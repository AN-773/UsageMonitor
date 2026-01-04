package sau.odev.usagemonitorLib.models

data class AppSessionStats(
    val sessionStartMs: Long,
    val sessionEndMs: Long,
    val durationMs: Long
)