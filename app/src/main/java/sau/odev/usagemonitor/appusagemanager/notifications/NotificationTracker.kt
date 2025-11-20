package sau.odev.usagemonitor.appusagemanager.notifications

import android.content.Context
import sau.odev.usagemonitor.appusagemanager.AppsUsageManager

/**
 * Helper class to easily access notification tracking features
 */
object NotificationTracker {

    /**
     * Get the notification manager instance
     */
    fun getManager(context: Context): NotificationManager {
        return AppsUsageManager.getInstance(context).getNotificationManager()
    }

    /**
     * Check if notification listener permission is granted
     */
    fun hasPermission(context: Context): Boolean {
        return getManager(context).isNotificationListenerPermissionGranted()
    }

    /**
     * Get notification count for an app today
     */
    fun getNotificationCountToday(context: Context, packageName: String): Int {
        return getManager(context).getNotificationCountToday(packageName)
    }

    /**
     * Get total notification count for an app
     */
    fun getTotalNotificationCount(context: Context, packageName: String): Int {
        return getManager(context).getNotificationCount(packageName)
    }

    /**
     * Get notification count for an app in a time range
     */
    fun getNotificationCountInTimeRange(
        context: Context,
        packageName: String,
        startTime: Long,
        endTime: Long
    ): Int {
        return getManager(context).getNotificationCountInTimeRange(packageName, startTime, endTime)
    }

    /**
     * Get notification counts for all apps in a time range
     */
    fun getAllNotificationCountsInTimeRange(
        context: Context,
        startTime: Long,
        endTime: Long
    ): List<NotificationCount> {
        return getManager(context).getNotificationCountsByPackageInTimeRange(startTime, endTime)
    }

    /**
     * Get recent notifications
     */
    fun getRecentNotifications(context: Context, limit: Int = 50): List<NotificationData> {
        return getManager(context).getRecentNotifications(limit)
    }

    /**
     * Clean up old notifications (older than 30 days)
     */
    fun cleanupOldNotifications(context: Context) {
        getManager(context).cleanupOldNotifications()
    }
}

