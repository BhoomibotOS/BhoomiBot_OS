package com.bhoomibot.os.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.bhoomibot.os.connection.model.VideoQuality
import com.bhoomibot.os.connection.model.toTelemetry
import com.bhoomibot.os.connection.provideLiveLinkRepository
import com.bhoomibot.os.data.DevicePreferences
import com.bhoomibot.os.data.LiveLinkPreferencesStore
import com.bhoomibot.os.data.LocationTracker
import com.bhoomibot.os.feature.autonomous.AutonomyManager
import com.bhoomibot.os.model.DeviceRole
import com.bhoomibot.os.model.MockRobotData
import com.bhoomibot.os.repository.provideRobotRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first

/**
 * BhoomiBotService: The permanent heart of the robot operations.
 * Handles both Video Streaming and Hardware Command Forwarding.
 */
class BhoomiBotService : LifecycleService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var wakeLock: PowerManager.WakeLock? = null

    private var isHardwareLinkActive = false
    private var isBroadcastActive = false
    private var bridgeJob: Job? = null
    private var telemetryJob: Job? = null
    private var locationTracker: LocationTracker? = null

    private var currentQuality: VideoQuality? = null
    private var currentUseRear: Boolean? = null
    private var currentFps: Int = 12

    companion object {
        private const val CHANNEL_ID = "bhoomibot_service_channel"
        private const val NOTIFICATION_ID = 101

        var isRunning = false
            private set

        const val ACTION_START_HARDWARE = "ACTION_START_HARDWARE"
        const val ACTION_STOP_HARDWARE = "ACTION_STOP_HARDWARE"
        const val ACTION_START_BROADCAST = "ACTION_START_BROADCAST"
        const val ACTION_STOP_BROADCAST = "ACTION_STOP_BROADCAST"
        const val ACTION_SWITCH_CAMERA = "ACTION_SWITCH_CAMERA"

        fun start(context: Context) {
            val intent = Intent(context, BhoomiBotService::class.java).apply { action = ACTION_START_HARDWARE }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
            else context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, BhoomiBotService::class.java).apply { action = ACTION_STOP_HARDWARE }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BhoomiBot::RobotBrainLock")
        wakeLock?.acquire()

        locationTracker = LocationTracker(applicationContext)
        locationTracker?.startTracking()

        startCommandBridge()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        val action = intent?.action ?: ACTION_START_HARDWARE
        val qualityName = intent?.getStringExtra("EXTRA_QUALITY") ?: currentQuality?.name ?: "MEDIUM"
        val quality = runCatching { VideoQuality.valueOf(qualityName) }.getOrDefault(VideoQuality.MEDIUM)
        val useRear = intent?.getBooleanExtra("EXTRA_USE_REAR", currentUseRear ?: true) ?: (currentUseRear ?: true)
        val fps = intent?.getIntExtra("EXTRA_FPS", currentFps) ?: currentFps

        android.util.Log.d("BhoomiBotRelay", "[SERVICE] onStartCommand: action=$action quality=$qualityName useRear=$useRear fps=$fps")

        when (action) {
            ACTION_START_HARDWARE -> {
                isHardwareLinkActive = true
                startCommandBridge()
            }
            ACTION_START_BROADCAST -> {
                isBroadcastActive = true
                startVision(quality, useRear, fps)
            }
            ACTION_STOP_BROADCAST -> {
                isBroadcastActive = false
                stopVision()
                checkAndStopSelf()
            }
            ACTION_SWITCH_CAMERA -> {
                if (isBroadcastActive) {
                    startVision(quality, useRear, fps)
                }
            }
            ACTION_STOP_HARDWARE -> {
                isHardwareLinkActive = false
                checkAndStopSelf()
            }
        }
        return START_STICKY
    }

    private fun startCommandBridge() {
        if (bridgeJob?.isActive == true) return

        val liveRepo = provideLiveLinkRepository(application)
        val robotRepo = provideRobotRepository(application)
        val recordingEngine = AutonomyManager.getRecordingEngine(this)

        bridgeJob = serviceScope.launch(Dispatchers.Default) {
            val prefs = LiveLinkPreferencesStore.preferences(applicationContext).first()
            val role = DevicePreferences.role(applicationContext).first() ?: DeviceRole.ROBOT
            
            // SECURITY: Only a ROBOT device should act as a command bridge.
            if (role != DeviceRole.ROBOT) {
                android.util.Log.w("BhoomiBotRelay", "[SERVICE] Aborting bridge: Device is an OPERATOR.")
                return@launch
            }
            
            liveRepo.connect(com.bhoomibot.os.connection.model.ConnectionConfig(
                serverUrl = prefs.serverUrl,
                robotId = prefs.robotId,
                sessionCode = prefs.sessionCode,
                role = role
            ))

            // Start background telemetry loop (500ms heartbeat)
            startTelemetryLoop(liveRepo, robotRepo)

            liveRepo.incomingCommands.collect { cmd ->
                android.util.Log.d("BhoomiBotRelay", "[SERVICE] Forwarding CMD: ${cmd.drive} @ ${cmd.speedPercent}%")

                // 1. Forward to VCU (Bluetooth)
                if (cmd.emergencyStop) {
                    robotRepo.sendDriveCommand(com.bhoomibot.os.model.DriveCommand.EMERGENCY_STOP)
                } else {
                    /**
                     * CRITICAL SEQUENCE ALIGNMENT (VCU SYNC):
                     * We must send the Speed command BEFORE the Direction command.
                     */
                    robotRepo.updateSpeed(cmd.speedPercent)
                    robotRepo.sendDriveCommand(cmd.drive)
                }
                cmd.pto?.let { robotRepo.setPto(it) }
                cmd.lights?.let { robotRepo.setLights(it) }

                // 2. Camera Controls (Flip / Toggle)
                cmd.liveCamera?.let {
                    if (it) {
                        val startIntent = Intent(applicationContext, BhoomiBotService::class.java).apply {
                            action = ACTION_START_BROADCAST
                            putExtra("EXTRA_USE_REAR", cmd.useRearCamera ?: currentUseRear ?: true)
                        }
                        startService(startIntent)
                    } else {
                        val stopIntent = Intent(applicationContext, BhoomiBotService::class.java).apply {
                            action = ACTION_STOP_BROADCAST
                        }
                        startService(stopIntent)
                    }
                }

                if (isBroadcastActive && cmd.useRearCamera != null && currentUseRear != cmd.useRearCamera) {
                    val switchIntent = Intent(applicationContext, BhoomiBotService::class.java).apply {
                        action = ACTION_SWITCH_CAMERA
                        putExtra("EXTRA_USE_REAR", cmd.useRearCamera)
                    }
                    startService(switchIntent)
                }

                // 3. Handle Recording
                if (cmd.learningMode != null) {
                    if (cmd.learningMode == true) recordingEngine.startRecording()
                    else recordingEngine.stopRecording()
                }
                if (recordingEngine.isRecording()) {
                    recordingEngine.recordCommand(cmd)
                }
            }
        }
    }

    /**
     * Permanent 500ms telemetry heartbeat.
     * Ensures the Operator sees real-time VCU status even if the Robot screen is locked.
     */
    private fun startTelemetryLoop(liveRepo: com.bhoomibot.os.connection.repository.LiveLinkRepository, robotRepo: com.bhoomibot.os.repository.RobotRepository) {
        telemetryJob?.cancel()
        telemetryJob = serviceScope.launch(Dispatchers.Default) {
            while (isActive) {
                val hardwareOnline = robotRepo.isConnected.value
                val vcuBatteryLevel = robotRepo.vcuBattery.value
                val location = locationTracker?.currentLocation?.value

                // 1. Read Local Phone Battery
                val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
                    applicationContext.registerReceiver(null, filter)
                }
                val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                val phoneBatteryPercent = if (level != -1 && scale != -1) (level * 100 / scale.toFloat()).toInt() else 0
                
                // 2. FORMAT GPS STATUS: "Lat, Long" or "Searching..."
                val gpsStatusLabel = if (location != null) {
                    "${String.format("%.4f", location.latitude)}, ${String.format("%.4f", location.longitude)}"
                } else {
                    "Searching..."
                }

                val realStatus = MockRobotData.robotStatus.copy(
                    isOnline = hardwareOnline,
                    batteryPercent = phoneBatteryPercent,
                    vcuBattery = vcuBatteryLevel,
                    gpsStatus = gpsStatusLabel
                )
                liveRepo.publishTelemetry(realStatus.toTelemetry())
                delay(500)
            }
        }
    }

    private fun startVision(quality: VideoQuality, useRearCamera: Boolean, fps: Int) {
        val perception = AutonomyManager.getPerceptionEngine(applicationContext)
        val liveRepo = provideLiveLinkRepository(application)

        currentQuality = quality
        currentUseRear = useRearCamera
        currentFps = fps

        RobotCameraManager.stopCamera()
        RobotCameraManager.startCamera(
            context = this,
            lifecycleOwner = this,
            quality = quality,
            useRearCamera = useRearCamera,
            fps = fps,
            onFrame = { jpeg -> liveRepo.publishFrame(jpeg) },
            onBitmap = { bmp -> perception.analyzeFrame(bmp) }
        )
    }

    private fun stopVision() {
        RobotCameraManager.stopCamera()
    }

    private fun checkAndStopSelf() {
        if (!isHardwareLinkActive && !isBroadcastActive) stopSelf()
    }

    override fun onDestroy() {
        isRunning = false
        RobotCameraManager.stopCamera()
        wakeLock?.release()
        bridgeJob?.cancel()
        telemetryJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(CHANNEL_ID, "BhoomiBot Active Service", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("BhoomiBot Robot Active")
            .setContentText("Listening for Operator commands...")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setOngoing(true)
            .build()
    }
}
