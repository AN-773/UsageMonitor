package sau.odev.usagemonitor.examples

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import sau.odev.usagemonitor.appusagemanager.AppsUsageManager
import sau.odev.usagemonitor.appusagemanager.notifications.NotificationTracker
import java.util.Calendar

/**
 * Example Activity demonstrating notification tracking usage
 *
 * This is a reference implementation showing how to:
 * - Check notification listener permission
 * - Request permission if needed
 * - Query notification counts
 * - Display notification statistics
 */
class NotificationTrackingExampleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if notification listener permission is granted
        if (!NotificationTracker.hasPermission(this)) {
            // Show explanation to user
            Toast.makeText(
                this,
                "Notification tracking requires special permission. Please enable it in the next screen.",
                Toast.LENGTH_LONG
            ).show()

            // Request permission
            requestNotificationListenerPermission()
        } else {
            // Permission granted, load notification data
            loadNotificationStatistics()
        }
    }

    private fun requestNotificationListenerPermission() {
        val notificationManager = AppsUsageManager.getInstance().getNotificationManager()
        val intent = notificationManager.getNotificationListenerPermissionIntent()
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()

        // Refresh data when returning from settings
        if (NotificationTracker.hasPermission(this)) {
            loadNotificationStatistics()
        }
    }

    private fun loadNotificationStatistics() {
        // Example: Get today's notification counts for all apps
        val todayStart = getTodayStartTimestamp()
        val now = System.currentTimeMillis()

        val notificationCounts = NotificationTracker.getAllNotificationCountsInTimeRange(
            this,
            todayStart,
            now
        )

        // Sort by count descending
        val topApps = notificationCounts.sortedByDescending { it.count }.take(10)

        // Display results (you would typically use RecyclerView here)
        topApps.forEach { notificationCount ->
            println("Package: ${notificationCount.package_name}")
            println("Count: ${notificationCount.count}")
            println("---")
        }
    }

    /**
     * Example: Get notification count for a specific app
     */
    private fun getAppNotificationCount(packageName: String) {
        // Get today's count
        val todayCount = NotificationTracker.getNotificationCountToday(this, packageName)

        // Get total count
        val totalCount = NotificationTracker.getTotalNotificationCount(this, packageName)

        // Get last 7 days count
        val weekStart = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
        val weekCount = NotificationTracker.getNotificationCountInTimeRange(
            this,
            packageName,
            weekStart,
            System.currentTimeMillis()
        )

        println("App: $packageName")
        println("Today: $todayCount notifications")
        println("This week: $weekCount notifications")
        println("All time: $totalCount notifications")
    }

    /**
     * Example: Get recent notifications
     */
    private fun showRecentNotifications() {
        val recentNotifications = NotificationTracker.getRecentNotifications(this, limit = 20)

        recentNotifications.forEach { notification ->
            println("Package: ${notification.packageName}")
            println("Title: ${notification.title}")
            println("Text: ${notification.text}")
            println("Time: ${formatTimestamp(notification.timestamp)}")
            println("---")
        }
    }

    /**
     * Example: Get notification details for specific app
     */
    private fun showAppNotificationHistory(packageName: String) {
        val notificationManager = AppsUsageManager.getInstance().getNotificationManager()
        val notifications = notificationManager.getNotificationsByPackage(packageName)

        notifications.forEach { notification ->
            println("Time: ${formatTimestamp(notification.timestamp)}")
            println("Title: ${notification.title}")
            println("Text: ${notification.text}")
            println("---")
        }
    }

    /**
     * Example: Clean up old data
     */
    private fun performMaintenance() {
        // Clean up notifications older than 30 days
        NotificationTracker.cleanupOldNotifications(this)

        Toast.makeText(this, "Old notifications cleaned up", Toast.LENGTH_SHORT).show()
    }

    /**
     * Example: Get notification statistics for different time periods
     */
    private fun getNotificationStats(packageName: String): NotificationStats {
        val now = System.currentTimeMillis()

        // Today
        val todayStart = getTodayStartTimestamp()
        val todayCount = NotificationTracker.getNotificationCountInTimeRange(
            this, packageName, todayStart, now
        )

        // Yesterday
        val yesterdayStart = todayStart - (24 * 60 * 60 * 1000L)
        val yesterdayCount = NotificationTracker.getNotificationCountInTimeRange(
            this, packageName, yesterdayStart, todayStart
        )

        // This week
        val weekStart = getWeekStartTimestamp()
        val weekCount = NotificationTracker.getNotificationCountInTimeRange(
            this, packageName, weekStart, now
        )

        // This month
        val monthStart = getMonthStartTimestamp()
        val monthCount = NotificationTracker.getNotificationCountInTimeRange(
            this, packageName, monthStart, now
        )

        return NotificationStats(
            today = todayCount,
            yesterday = yesterdayCount,
            thisWeek = weekCount,
            thisMonth = monthCount
        )
    }

    // Helper functions

    private fun getTodayStartTimestamp(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getWeekStartTimestamp(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getMonthStartTimestamp(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun formatTimestamp(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        return android.text.format.DateFormat.format("yyyy-MM-dd HH:mm:ss", calendar).toString()
    }

    // Data class for statistics
    data class NotificationStats(
        val today: Int,
        val yesterday: Int,
        val thisWeek: Int,
        val thisMonth: Int
    )
}

