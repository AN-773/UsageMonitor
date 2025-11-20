package sau.odev.usagemonitor.appusagemanager.apps

import androidx.room.*
import sau.odev.usagemonitor.appusagemanager.groups.Group

@Entity(tableName = "apps",
        indices = [Index(value = ["group_id"])],
        foreignKeys = [
            ForeignKey(
                    entity = Group::class,
                    parentColumns = arrayOf("id"),
                    childColumns = arrayOf("group_id"))])
data class App constructor(
        @PrimaryKey(autoGenerate = true) var id: Long = 0,
        val name: String,
        @Volatile var category: String,
        val system: Boolean,
        @Volatile var installed: Boolean = true,
        @Volatile var ignored: Boolean = false,
        @Volatile @ColumnInfo(name = "floating_timer") var floatingTimer: Boolean = true,
        @Volatile @ColumnInfo(name = "ignore_max_usage_until") var ignoreMaxUsageUntil: Long = 0L,
        @Volatile @ColumnInfo(name = "strict_mode") var strictMode: Boolean = false,
        @Volatile @ColumnInfo(name = "max_usage_duration") var maxUsagePerDayInSeconds: Int = 0,
        @ColumnInfo(name = "package_name") val packageName: String,
        @Volatile @ColumnInfo(name = "group_id") var groupId: Long = 0
) {

    fun copyFrom(app: App) {
        if (app.id > 0L)
            id = app.id
        category = app.category
        installed = app.installed
        ignored = app.ignored
        floatingTimer = app.floatingTimer
        ignoreMaxUsageUntil = app.ignoreMaxUsageUntil
        strictMode = app.strictMode
        maxUsagePerDayInSeconds = app.maxUsagePerDayInSeconds
        groupId = app.groupId
    }
}