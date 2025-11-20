package sau.odev.usagemonitor.appusagemanager.groups

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "groups")
data class Group constructor(
        @Volatile var name: String,
        @Volatile @ColumnInfo(name = "max_usage_duration") var maxUsagePerDayInSeconds: Int = 0,
     /*   @Ignore @Volatile var ignored: Boolean = false,
        @Ignore @Volatile @ColumnInfo(name = "floating_timer") var floatingTimer: Boolean = true,
        @Ignore @Volatile @ColumnInfo(name = "ignore_max_usage_until") var ignoreMaxUsageUntil: Long = 0L,
        @Ignore @Volatile @ColumnInfo(name = "strict_mode") var strictMode: Boolean = false,*/
        @Volatile var icon: Int = 0,
        @Volatile @PrimaryKey(autoGenerate = true) var id: Long = 0
) {
    internal fun copyFrom(group: Group) {
        if (group.id > 0L)
            id = group.id
        name = group.name
        icon = group.icon
      /*  floatingTimer = group.floatingTimer
        ignored = group.ignored
        ignoreMaxUsageUntil = group.ignoreMaxUsageUntil
        strictMode = group.strictMode*/
        maxUsagePerDayInSeconds = group.maxUsagePerDayInSeconds
    }
}