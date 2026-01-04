package sau.odev.usagemonitorLib.usage

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import sau.odev.usagemonitorLib.storage.Storage
import sau.odev.usagemonitorLib.storage.entities.UsageEventEntity

internal class UsageIngestor(
    private val context: Context,
    private val storage: Storage,
) {

    private val usm: UsageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    /**
     * Ingest raw usage events from [fromMs, toMs) into Room.
     * Returns number of events written.
     */
    suspend fun ingestRange(fromMs: Long, toMs: Long): Int {
        if (toMs <= fromMs) return 0

        val events = usm.queryEvents(fromMs, toMs)
        val batch = ArrayList<UsageEventEntity>(4096)

        val e = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(e)

            val pkg = e.packageName ?: continue
            val ts = e.timeStamp

            // Some OEMs produce weird values outside requested range. Clamp by filter.
            if (ts < fromMs || ts >= toMs) continue

            // API 21+: eventType available.
            val type = e.eventType

            batch.add(
                UsageEventEntity(
                    packageName = pkg,
                    eventType = type,
                    timestampMs = ts
                )
            )

            // Write in chunks to avoid huge memory spikes.
            if (batch.size >= 2000) {
                storage.writeUsageEvents(batch)
                batch.clear()
            }
        }

        if (batch.isNotEmpty()) {
            storage.writeUsageEvents(batch)
        }

        // We don’t know exact inserted count (IGNORE strategy), so return read count approximation.
        return batch.size
    }
}