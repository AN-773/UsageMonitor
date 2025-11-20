package sau.odev.usagemonitor.appusagemanager.notifications

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notifications",
    indices = [
        Index(value = ["package_name"]),
        Index(value = ["notification_key"], unique = true)
    ]
)
data class NotificationData(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "title") val title: String? = null,
    @ColumnInfo(name = "text") val text: String? = null,
    @ColumnInfo(name = "notification_key") val notificationKey: String
)

