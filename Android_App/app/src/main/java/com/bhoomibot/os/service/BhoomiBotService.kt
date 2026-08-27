package com.bhoomibot.os.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.LifecycleOwner
import com.bhoomibot.os.connection.model.VideoQuality
import com.bhoomibot.os.connection.provideLiveLinkRepository
import com.bhoomibot.os.data.DevicePreferences
import com.bhoomibot.os.feature.autonomous.AutonomyManager
import com.bhoomibot.os.model.DeviceRole
import com.bhoomibot.os.repository.provideRobotRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first

/**
 * BhoomiBotService: The permanent heart of the robot operations.
 */
class BhoomiBotService : LifecycleService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var wakeLock: PowerManager.WakeLock? = null
    
    private var isHardwareLinkActive = false
    private var isBroadcastActive = false
    
    private var currentQuality: VideoQuality? = null
    private var currentUseRear: Boolean? = null
    private var bridgeJob: Job? = null
    
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
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
                else context.startService(intent)
            } catch (e: Exception) {
                android.util.Log.e("RobotService", "FGS Start Failed: ${e.message}")
            }
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
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val type = ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE or ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            try {
                startForeground(NOTIFICATION_ID, createNotification(), type)
            } catch (e: Exception) {
                startForeground(NOTIFICATION_ID, createNotification())
            }
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
        }
        
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BhoomiBot::RobotBrainLock")
        wakeLock?.acquire()
        
        serviceScope.launch {
            DevicePreferences.role(applicationContext)
                .distinctUntilChanged()
                .collectLatest { role ->
                    if (role == DeviceRole.ROBOT) {
                        startCommandBridge()
                    } else {
                        bridgeJob?.cancel()
                    }
                }
        }
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
                updateForegroundType(camera = true)
                startVision(quality, useRear)
            }
            ACTION_STOP_BROADCAST -> {
                isBroadcastActive = false
                stopVision()
                updateForegroundType(camera = false)
                checkAndStopSelf()
            }
            ACTION_SWITCH_CAMERA -> {
                if (isBroadcastActive) startVision(quality, useRear)
            }
        }
        return START_STICKY
    }

    private fun updateForegroundType(camera: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            var type = ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE or ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            if (camera && ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
            }
            try {
                startForeground(NOTIFICATION_ID, createNotification(), type)
            } catch (e: Exception) {
                android.util.Log.e("RobotService", "Failed to update service type: ${e.message}")
            }
        }
    }

    private fun startCommandBridge() {
        bridgeJob?.cancel()
        val liveRepo = provideLiveLinkRepository(application)
        val robotRepo = provideRobotRepository(application)

        bridgeJob = serviceScope.launch(Dispatchers.Default) {
            // AI-Fix: Auto-connect the Robot to the Relay
            // Without this call, the Robot phone opens the repo but never opens the socket.
            val prefs = com.bhoomibot.os.data.LiveLinkPreferencesStore.preferences(applicationContext).first()
            liveRepo.connect(com.bhoomibot.os.connection.model.ConnectionConfig(
                serverUrl = prefs.serverUrl,
                robotId = prefs.robotId,
                sessionCode = prefs.sessionCode,
                role = DeviceRole.ROBOT
            ))

            // AI-Fix: Auto-start the camera broadcast on successful connection
            isBroadcastActive = true
            startVision(VideoQuality.MEDIUM, true)

            liveRepo.incomingCommands.collect { cmd ->
                if (cmd.emergencyStop) {
                    robotRepo.sendDriveCommand(com.bhoomibot.os.model.DriveCommand.EMERGENCY_STOP)
                } else {
                    robotRepo.sendDriveCommand(cmd.drive)
                    robotRepo.updateSpeed(cmd.speedPercent)
                }
                cmd.pto?.let { robotRepo.setPto(it) }
                cmd.lights?.let { robotRepo.setLights(it) }
                
                if (cmd.triggerOta) {
                    robotRepo.triggerOta()
                }

                if (cmd.liveCamera != null) {
                    val intent = Intent(applicationContext, BhoomiBotService::class.java).apply {
                        action = if (cmd.liveCamera!!) ACTION_START_BROADCAST else ACTION_STOP_BROADCAST
                        putExtra("EXTRA_USE_REAR", cmd.useRearCamera ?: true)
                    }
                    startService(intent)
                }
            }
        }
    }

    private fun startVision(quality: VideoQuality, useRearCamera: Boolean) {
        currentQuality = quality
        currentUseRear = useRearCamera
        val perception = AutonomyManager.getPerceptionEngine(applicationContext)
        val liveRepo = provideLiveLinkRepository(application)

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
            .setContentText("Hardware & Live systems synced.")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setOngoing(true)
            .build()
    }
}
