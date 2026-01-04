package sau.odev.usagemonitorLib.usage

import android.app.usage.UsageEvents
import sau.odev.usagemonitorLib.storage.entities.UsageEventEntity

internal object LaunchEstimator {

    private const val COOLDOWN_MS = 7_000L


    // Primary signal: ACTIVITY_RESUMED (closest to real user opening app screen)
    private val primaryTypes = setOf(
        UsageEvents.Event.ACTIVITY_RESUMED
    )

    // Fallback signal: MOVE_TO_FOREGROUND (some devices don’t emit ACTIVITY_RESUMED reliably)
    private val fallbackTypes = setOf(
        UsageEvents.Event.MOVE_TO_FOREGROUND
    )

    fun countLaunches(events: List<UsageEventEntity>): Map<String, Int> {
        val countsPrimary = HashMap<String, Int>(128)
        for (e in events) {
            if (e.eventType in primaryTypes) {
                countsPrimary[e.packageName] = (countsPrimary[e.packageName] ?: 0) + 1
            }
        }
        if (countsPrimary.isNotEmpty()) return countsPrimary

        val countsFallback = HashMap<String, Int>(128)
        for (e in events) {
            if (e.eventType in fallbackTypes) {
                countsFallback[e.packageName] = (countsFallback[e.packageName] ?: 0) + 1
            }
        }
        return countsFallback
    }

    fun countLaunchesForPackage(events: List<UsageEventEntity>, packageName: String): Int {
        val filteredEvents = events.filter { it.packageName == packageName }
        return estimateLaunches(events)[packageName] ?: 0
    }

    fun estimateLaunches(
        events: List<UsageEventEntity>
    ): Map<String, Int> {

        val lastResume = HashMap<String, Long>()
        val active = HashSet<String>()
        var lastActive = ""
        val launches = HashMap<String, Int>()

        for (e in events.sortedBy { it.timestampMs }) {
            when (e.eventType) {

                UsageEvents.Event.MOVE_TO_FOREGROUND,
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    val last = lastResume[e.packageName]
                    val now = e.timestampMs

                    val isNewLaunch =
                        (lastActive != e.packageName)

                    if (isNewLaunch) {
                        launches[e.packageName] =
                            (launches[e.packageName] ?: 0) + 1
                    }

                    active.add(e.packageName)
                    lastActive = e.packageName
                    lastResume[e.packageName] = now
                }

                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED -> {
                    active.remove(e.packageName)
                }
            }

        }

        return launches
    }
}