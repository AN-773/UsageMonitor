package sau.odev.usagemonitorLib.models

data class PackageStats(
    val packageName: String,
    val usageTimeMs: Long,
    val launchCount: Int,
    val notificationCount: Int
)