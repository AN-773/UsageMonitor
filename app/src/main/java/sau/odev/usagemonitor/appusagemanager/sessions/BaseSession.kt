package sau.odev.usagemonitor.appusagemanager.sessions

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class BaseSession internal constructor(
        @PrimaryKey(autoGenerate = true) val id: Long = 0,
        @ColumnInfo(name = "app_id") val appId: Long,
        @ColumnInfo(name = "group_id") val groupId: Long,
        @ColumnInfo(name = "launch_day") val launchDay: Long,
        @ColumnInfo(name = "launch_time") val launchTime: Long,
        @ColumnInfo(name = "visit_count") val visitCount: Int = 1,
        @ColumnInfo(name = "usage_duration") val usageDuration: Long
)