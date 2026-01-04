package sau.odev.usagemonitorLib.usage

import android.app.usage.UsageEvents
import android.content.Context
import sau.odev.usagemonitorLib.models.AppSessionStats
import sau.odev.usagemonitorLib.storage.Storage
import kotlin.math.max
import kotlin.math.min

internal class UsageRepository(
    context: Context
) {
    private val appContext = context.applicationContext
    private val storage = Storage(appContext)
    private val syncState = UsageSyncState(appContext)
    private val ingestor = UsageIngestor(appContext, storage)

    /**
     * Incremental sync up to [nowMs]. Uses last stored cursor.
     * - If this is the first sync, it ingests only the last [defaultBackfillMs].
     */
    suspend fun syncIncremental(
        nowMs: Long,
        defaultBackfillMs: Long = 120L * 24 * 60 * 60 * 1000 // 120 days
    ): UsageSyncResult {
        val last = syncState.getLastUsageIngestMs()
        val from = if (last > 0L) last else max(0L, nowMs - defaultBackfillMs)
        return syncRange(fromMs = from, toMs = nowMs)
    }

    /**
     * Sync a specific range. Internally chunks into 6-hour windows to avoid OEM weirdness.
     */
    suspend fun syncRange(fromMs: Long, toMs: Long): UsageSyncResult {
        if (toMs <= fromMs) return UsageSyncResult(0, fromMs, toMs)

        val chunkMs = 6L * 60 * 60 * 1000 // 6 hours
        var cursor = fromMs
        var wrote = 0

        while (cursor < toMs) {
            val end = min(toMs, cursor + chunkMs)
            ingestor.ingestRange(cursor, end)
            // We can’t know exact inserted count with IGNORE; treat as “processed”.
            wrote += 1
            cursor = end
        }

        // Advance cursor to toMs (idempotent-ish; duplicates ignored in future once we add de-dup keys).
        syncState.setLastUsageIngestMs(toMs)
        return UsageSyncResult(processedChunks = wrote, fromMs = fromMs, toMs = toMs)
    }

    suspend fun getLaunchCounts(fromMs: Long, toMs: Long): Map<String, Int> {
        val events = storage.readUsageEvents(fromMs, toMs)
        return LaunchEstimator.estimateLaunches(events)
    }

    suspend fun getLaunchCount(packageName: String, fromMs: Long, toMs: Long): Int {
        val events = storage.readUsageEvents(fromMs, toMs)
        return LaunchEstimator.countLaunchesForPackage(events, packageName)
    }

    suspend fun getUsageDurations(
        fromMs: Long,
        toMs: Long
    ): Map<String, Long> {
        val events = storage.readUsageEvents(fromMs, toMs)
        return UsageDurationCalculator.computeForegroundDurations(events, toMs)
    }

    suspend fun getAppSessions(
        packageName: String,
        fromMs: Long,
        toMs: Long
    ): List<AppSessionStats> {

        val events = storage.readUsageEvents(fromMs, toMs)

        return SessionExtractor.extractSessions(
            packageName = packageName,
            events = events,
            windowFromMs = fromMs,
            windowToMs = toMs
        )
    }

    suspend fun getPackageUsageTime(
        packageName: String,
        fromMs: Long,
        toMs: Long
    ): Long {
        val events = storage.readUsageEvents(fromMs, toMs)
        return UsageDurationCalculator
            .computeForegroundDurations(events, toMs)[packageName] ?: 0L
    }

    suspend fun getOldestUsageTimestampMsOrNull(): Long? =
        storage.getOldestUsageTimestampMsOrNull()

}

data class UsageSyncResult(
    val processedChunks: Int,
    val fromMs: Long,
    val toMs: Long
)