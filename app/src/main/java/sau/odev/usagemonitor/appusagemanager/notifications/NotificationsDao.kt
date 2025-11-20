package sau.odev.usagemonitor.appusagemanager.notifications

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface NotificationsDao {

    @Insert
    fun insertNotification(notification: NotificationData): Long

    @Query("SELECT COUNT(*) FROM notifications WHERE notification_key = :notificationKey")
    fun notificationExists(notificationKey: String): Int

    @Query("SELECT COUNT(*) FROM notifications WHERE package_name = :packageName")
    fun getNotificationCountByPackage(packageName: String): Int

    @Query("SELECT COUNT(*) FROM notifications WHERE package_name = :packageName AND timestamp >= :startTime AND timestamp <= :endTime")
    fun getNotificationCountByPackageInTimeRange(packageName: String, startTime: Long, endTime: Long): Int

    @Query("SELECT * FROM notifications WHERE package_name = :packageName ORDER BY timestamp DESC")
    fun getNotificationsByPackage(packageName: String): List<NotificationData>

    @Query("SELECT * FROM notifications WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getNotificationsInTimeRange(startTime: Long, endTime: Long): List<NotificationData>

    @Query("SELECT package_name, COUNT(*) as count FROM notifications WHERE timestamp >= :startTime AND timestamp <= :endTime GROUP BY package_name")
    fun getNotificationCountsByPackageInTimeRange(startTime: Long, endTime: Long): List<NotificationCount>

    @Query("DELETE FROM notifications WHERE timestamp < :timestamp")
    fun deleteNotificationsOlderThan(timestamp: Long)

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentNotifications(limit: Int): List<NotificationData>
}

data class NotificationCount(
    val package_name: String,
    val count: Int
)

