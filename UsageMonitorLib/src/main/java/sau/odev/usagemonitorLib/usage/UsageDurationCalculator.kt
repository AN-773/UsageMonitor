package sau.odev.usagemonitorLib.usage

import android.app.usage.UsageEvents
import sau.odev.usagemonitorLib.storage.entities.UsageEventEntity

internal object UsageDurationCalculator {

    fun computeForegroundDurations(
        events: List<UsageEventEntity>,
        windowEndMs: Long
    ): Map<String, Long> {

        val activeStart = HashMap<String, Long>()
        val durations = HashMap<String, Long>()

        for (e in events.sortedBy { it.timestampMs }) {
            when (e.eventType) {

                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    // Start session only if not already active
                    activeStart.putIfAbsent(e.packageName, e.timestampMs)
                }

                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    val start = activeStart.remove(e.packageName)
                    if (start != null && e.timestampMs > start) {
                        durations[e.packageName] =
                            (durations[e.packageName] ?: 0L) + (e.timestampMs - start)
                    }
                }
            }
        }

        // Close any open sessions at window end
        for ((pkg, start) in activeStart) {
            if (windowEndMs > start) {
                durations[pkg] =
                    (durations[pkg] ?: 0L) + (windowEndMs - start)
            }
        }

        return durations
    }
}