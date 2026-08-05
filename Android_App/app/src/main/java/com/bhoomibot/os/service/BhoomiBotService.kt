package com.bhoomibot.os.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.bhoomibot.os.connection.model.VideoQuality
import com.bhoomibot.os.connection.provideLiveLinkRepository
import com.bhoomibot.os.data.DevicePreferences
import com.bhoomibot.os.feature.autonomous.AutonomyManager
import com.bhoomibot.os.model.DeviceRole
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
    
    private var currentQuality: VideoQuality? = null
    private var currentUseRear: Boolean? = null
    
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
            context.stopService(intent)
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
        
        // AI-Fix: Always start command bridge for robot devices
        startCommandBridge()
        
        android.util.Log.i("RobotService", "BhoomiBot Background Service Started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        
        val qualityName = intent?.getStringExtra("EXTRA_QUALITY") ?: currentQuality?.name ?: "MEDIUM"
        val quality = runCatching { VideoQuality.valueOf(qualityName) }.getOrDefault(VideoQuality.MEDIUM)
        val useRear = intent?.getBooleanExtra("EXTRA_USE_REAR", currentUseRear ?: true) ?: (currentUseRear ?: true)

        when (intent?.action) {
            ACTION_START_HARDWARE -> isHardwareLinkActive = true
            ACTION_STOP_HARDWARE -> {
                isHardwareLinkActive = false
                checkAndStopSelf()
            }
            ACTION_START_BROADCAST -> {
                isBroadcastActive = true
                startVision(quality, useRear)
            }
            ACTION_STOP_BROADCAST -> {
                isBroadcastActive = false
                stopVision()
                checkAndStopSelf()
            }
            ACTION_SWITCH_CAMERA -> {
                if (isBroadcastActive) {
                    startVision(quality, useRear)
                }
            }
        }
        return START_STICKY
    }

    /**
     * PERMANENT COMMAND BRIDGE
     * This loop runs for the entire life of the service. 
     * It receives Operator Wi-Fi commands and pushes them to VCU Bluetooth.
     */
    private fun startCommandBridge() {
        val liveRepo = provideLiveLinkRepository(application)
        val robotRepo = provideRobotRepository(application)
        val recordingEngine = AutonomyManager.getRecordingEngine(this)

        // AI-Fix: Move to Default dispatcher for high-priority computation
        serviceScope.launch(Dispatchers.Default) {
            liveRepo.incomingCommands.collect { cmd ->
                android.util.Log.d("RobotService", "Forwarding Command to VCU: ${cmd.drive} @ ${cmd.speedPercent}%")
                
                // 1. Forward to VCU (Bluetooth)
                if (cmd.emergencyStop) {
                    robotRepo.sendDriveCommand(com.bhoomibot.os.model.DriveCommand.EMERGENCY_STOP)
                } else {
                    robotRepo.sendDriveCommand(cmd.drive)
                    robotRepo.updateSpeed(cmd.speedPercent)
                }
                cmd.pto?.let { robotRepo.setPto(it) }
                cmd.lights?.let { robotRepo.setLights(it) }

                // 2. Handle Recording (If learning is enabled)
                if (cmd.learningMode != null) {
                    if (cmd.learningMode == true) recordingEngine.startRecording()
                    else recordingEngine.stopRecording()
                }
                if (recordingEngine.isRecording()) {
                    recordingEngine.recordCommand(cmd)
                }
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

                // Only switch if explicitly requested via useRearCamera field
                if (isBroadcastActive && cmd.useRearCamera != null && currentUseRear != cmd.useRearCamera) {
                    val switchIntent = Intent(applicationContext, BhoomiBotService::class.java).apply {
                        action = ACTION_SWITCH_CAMERA
                        putExtra("EXTRA_USE_REAR", cmd.useRearCamera)
                    }
                    startService(switchIntent)
                }
            }
        }
    }

    private fun startVision(quality: VideoQuality, useRearCamera: Boolean) {
        val perception = AutonomyManager.getPerceptionEngine(applicationContext)
        val liveRepo = provideLiveLinkRepository(application)

        // Reset state
        currentQuality = quality
        currentUseRear = useRearCamera

        RobotCameraManager.stopCamera()
        RobotCameraManager.startCamera(
            context = this,
            lifecycleOwner = this,
            quality = quality,
            useRearCamera = useRearCamera,
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
            .setContentTitle("BhoomiBot is Active")
            .setContentText("Robot brain is alive. Controlling VCU & Camera.")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setOngoing(true)
            .build()
    }
}
