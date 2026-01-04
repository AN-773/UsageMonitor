package sau.odev.usagemonitorLib.usage

import android.app.usage.UsageEvents
import sau.odev.usagemonitorLib.models.AppSessionStats
import sau.odev.usagemonitorLib.storage.entities.UsageEventEntity

internal object SessionExtractor {

    fun extractSessions(
        packageName: String,
        events: List<UsageEventEntity>,
        windowFromMs: Long,
        windowToMs: Long
    ): List<AppSessionStats> {

        val sessions = mutableListOf<AppSessionStats>()
        var sessionStart: Long? = null

        val sorted = events
            .asSequence()
//            .filter { it.packageName == packageName }
            .sortedBy { it.timestampMs }

        for (e in sorted) {
            when (e.eventType) {

                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    // Start session only if not already active
                    if (sessionStart == null && e.packageName == packageName) {
                        sessionStart = maxOf(e.timestampMs, windowFromMs)
                    }
                    val start = sessionStart
                    if (start != null && e.packageName != packageName) {
                        val end = minOf(e.timestampMs, windowToMs)
//                        if ((end - start) <= 60000L){
//                            continue
//                        }
                        if (end > start) {
                            sessions.add(
                                AppSessionStats(
                                    sessionStartMs = start,
                                    sessionEndMs = end,
                                    durationMs = end - start
                                )
                            )
                        }
                        sessionStart = null
                    }
                }

            }
        }

        // Session still open at window end
//        val start = sessionStart
//        if (start != null && windowToMs > start) {
//            if (windowToMs - start >= 3000L)
//                sessions.add(
//                    AppSessionStats(
//                        sessionStartMs = start,
//                        sessionEndMs = windowToMs,
//                        durationMs = windowToMs - start
//                    )
//                )
//        }

        return sessions
    }
}