package sau.odev.usagemonitor.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import sau.odev.usagemonitor.MainActivity
import sau.odev.usagemonitor.R
import sau.odev.usagemonitor.appusagemanager.AppsUsageManager

class UsageMonitorService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val syncRunnable = object : Runnable {
        override fun run() {
            syncUsageData()
            handler.postDelayed(this, SYNC_INTERVAL_MS)
        }
    }

    companion object {
        private const val CHANNEL_ID = "usage_monitor_channel"
        private const val NOTIFICATION_ID = 1001
        private const val SYNC_INTERVAL_MS = 5 * 60 * 1000L // 5 minutes
        const val ACTION_START = "sau.odev.usagemonitor.START_MONITORING"
        const val ACTION_STOP = "sau.odev.usagemonitor.STOP_MONITORING"

        fun startService(context: Context) {
            val intent = Intent(context, UsageMonitorService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, UsageMonitorService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForegroundService()
                startMonitoring()
            }
            ACTION_STOP -> {
                stopMonitoring()
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        val notification = createNotification(
            "Monitoring Active",
            "Usage Monitor is tracking your app usage"
        )
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Usage Monitor Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Persistent notification for usage monitoring"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(title: String, content: String): Notification {
        // Intent to open the main activity when notification is clicked
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Action to stop monitoring
        val stopIntent = Intent(this, UsageMonitorService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop",
                stopPendingIntent
            )
            .build()
    }

    private fun startMonitoring() {
        try {
            AppsUsageManager.getInstance().startObserving()
            // Start periodic sync
            handler.post(syncRunnable)
            updateNotification(
                "Monitoring Active",
                "Tracking app usage in background"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            updateNotification(
                "Monitoring Error",
                "Failed to start monitoring: ${e.message}"
            )
        }
    }

    private fun stopMonitoring() {
        try {
            // Stop periodic sync
            handler.removeCallbacks(syncRunnable)
            updateNotification(
                "Monitoring Stopped",
                "Usage monitoring has been stopped"
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun syncUsageData() {
        try {
            val usageStatsSync = AppsUsageManager.getInstance().getUsageStatsSync()
            // Sync today's usage data from UsageStatsManager
//            usageStatsSync.syncTodayUsage()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateNotification(title: String, content: String) {
        val notification = createNotification(title, content)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
    }
}

