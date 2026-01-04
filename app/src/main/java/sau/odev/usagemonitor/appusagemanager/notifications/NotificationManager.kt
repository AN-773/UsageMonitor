package sau.odev.usagemonitor.appusagemanager.notifications

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import java.util.Calendar

class NotificationManager(
    private val context: Context,
    private val notificationsDao: NotificationsDao
) {

    /**
     * Record a notification for a specific app
     */
    fun recordNotification(packageName: String, timestamp: Long, title: String?, text: String?, notificationKey: String) {
        // Check if notification already exists
        if (notificationsDao.notificationExists(notificationKey) > 0) {
            // Notification already recorded, skip duplicate
            // Whats app sends multiple updates for the same notification
            // So we will not skip duplicates
//            return
        }

        val notification = NotificationData(
            packageName = packageName,
            timestamp = timestamp,
            title = title,
            text = text,
            notificationKey = notificationKey
        )
        notificationsDao.insertNotification(notification)
    }

    /**
     * Get total notification count for a package
     */
    fun getNotificationCount(packageName: String): Int {
        return notificationsDao.getNotificationCountByPackage(packageName)
    }

    /**
     * Get notification count for a package within a time range
     */
    fun getNotificationCountInTimeRange(packageName: String, startTime: Long, endTime: Long): Int {
        return notificationsDao.getNotificationCountByPackageInTimeRange(packageName, startTime, endTime)
    }

    /**
     * Get notification count for a package today
     */
    fun getNotificationCountToday(packageName: String): Int {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis
        val endOfDay = System.currentTimeMillis()

        return getNotificationCountInTimeRange(packageName, startOfDay, endOfDay)
    }

    /**
     * Get all notifications for a package
     */
    fun getNotificationsByPackage(packageName: String): List<NotificationData> {
        return notificationsDao.getNotificationsByPackage(packageName)
    }

    /**
     * Get all notifications within a time range
     */
    fun getNotificationsInTimeRange(startTime: Long, endTime: Long): List<NotificationData> {
        return notificationsDao.getNotificationsInTimeRange(startTime, endTime)
    }

    /**
     * Get notification counts by package within a time range
     */
    fun getNotificationCountsByPackageInTimeRange(startTime: Long, endTime: Long): List<NotificationCount> {
        return notificationsDao.getNotificationCountsByPackageInTimeRange(startTime, endTime)
    }

    /**
     * Get recent notifications with limit
     */
    fun getRecentNotifications(limit: Int = 50): List<NotificationData> {
        return notificationsDao.getRecentNotifications(limit)
    }

    /**
     * Clean up old notifications (older than specified timestamp)
     */
    fun cleanupOldNotifications(olderThanTimestamp: Long) {
        notificationsDao.deleteNotificationsOlderThan(olderThanTimestamp)
    }

    /**
     * Clean up notifications older than 30 days
     */
    fun cleanupOldNotifications() {
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        cleanupOldNotifications(thirtyDaysAgo)
    }

    /**
     * Check if notification listener permission is granted
     */
    fun isNotificationListenerPermissionGranted(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
        return enabledListeners?.contains(context.packageName) ?: false
    }

    /**
     * Get intent to request notification listener permission
     */
    fun getNotificationListenerPermissionIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        } else {
            Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
        }
    }
}

