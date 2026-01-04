package sau.odev.usagemonitorLib.models

import java.time.LocalDate

data class TimedPackageStats(
    val periodStart: LocalDate,   // day / week start / month start
    val usageTimeMs: Long,
    val launchCount: Int,
    val notificationCount: Int
)