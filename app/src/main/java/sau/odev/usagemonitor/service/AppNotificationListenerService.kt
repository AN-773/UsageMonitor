package sau.odev.usagemonitor.service

import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import sau.odev.usagemonitor.appusagemanager.AppsUsageManager
import sau.odev.usagemonitor.appusagemanager.IGNORED_PACKAGES

class AppNotificationListenerService : NotificationListenerService() {

    private val TAG = "NotificationListener"
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val lastCalledPerPackage = mutableMapOf<String, Long>()

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "NotificationListenerService created")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        try {
            super.onNotificationPosted(sbn)
            Log.d(TAG, "onNotificationPosted called")

            sbn?.let {
                if (lastCalledPerPackage[it.packageName]?.let { lastTime ->
                        System.currentTimeMillis() - lastTime < 100
                    } == true) {
                    Log.d(TAG, "Ignoring duplicate notification for package: ${it.packageName}")
                    return
                }
                lastCalledPerPackage[it.packageName] = System.currentTimeMillis()
                val packageName = it.packageName
                val notificationKey = it.key // Unique system identifier for this notification
                Log.d(TAG, "onNotificationPosted: Package: $packageName, Key: $notificationKey")

                // Ignore system and our own package
                if (packageName == this.packageName || IGNORED_PACKAGES.contains(packageName)) {
                    return
                }

                val notification = it.notification ?: return
                val extras = notification.extras ?: return
                val title = extras.getCharSequence("android.title")?.toString()
                val text = extras.getCharSequence("android.text")?.toString()
                val timestamp = it.postTime

                Log.d(TAG, "Notification posted - Package: $packageName, Title: $title, Time: $timestamp")

                // Save to database asynchronously to avoid blocking the binder thread
                serviceScope.launch {
                    try {
                        val usageManager = AppsUsageManager.getInstance()
                        val notificationManager = usageManager.getNotificationManager()
                        notificationManager.recordNotification(packageName, timestamp, title, text, notificationKey)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error recording notification", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onNotificationPosted", e)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        try {
            super.onNotificationRemoved(sbn)
            sbn?.let {
                Log.d(TAG, "onNotificationRemoved: Package: ${it.packageName}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onNotificationRemoved", e)
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "NotificationListenerService connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "NotificationListenerService disconnected")
        // Request rebind when disconnected
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            requestRebind(android.content.ComponentName(this, AppNotificationListenerService::class.java))
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "NotificationListenerService destroyed")
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        fun hasPermission(context: android.content.Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                val enabledListeners = Settings.Secure.getString(
                    context.contentResolver,
                    "enabled_notification_listeners"
                )
                enabledListeners?.contains(context.packageName) ?: false
            } else {
                true
            }
        }

        fun getPermissionIntent(): Intent {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            } else {
                Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            }
        }
    }
}

