package sau.odev.usagemonitor.appusagemanager.sessions

import androidx.room.ColumnInfo
import androidx.room.PrimaryKey

data class Session internal constructor(
        @PrimaryKey(autoGenerate = true) val id: Long = 0,
        @ColumnInfo(name = "app_id") val appId: Long,
        @ColumnInfo(name = "group_id") val groupId: Long,
        @ColumnInfo(name = "app_name") var appName: String? = "",
        @ColumnInfo(name = "group_name") var groupName: String? = "",
        @ColumnInfo(name = "package_name") var packageName: String? = "",
        @ColumnInfo(name = "launch_time") val launchTime: Long,
        @ColumnInfo(name = "visit_count") val visitCount: Int = 1,
        @ColumnInfo(name = "usage_duration") val usageDuration: Long
)