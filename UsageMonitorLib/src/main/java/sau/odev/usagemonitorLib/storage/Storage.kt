package sau.odev.usagemonitorLib.storage

import android.content.Context
import sau.odev.usagemonitorLib.storage.entities.NotificationEventEntity
import sau.odev.usagemonitorLib.storage.entities.UsageEventEntity

internal class Storage(context: Context) {

    private val db = WellbeingDatabase.Companion.get(context)
    val usageDao = db.usageEventsDao()
    val notifDao = db.notificationEventsDao()

    suspend fun writeUsageEvents(events: List<UsageEventEntity>) {
        if (events.isNotEmpty()) usageDao.insertAll(events)
    }

    suspend fun writeNotificationEvents(events: List<NotificationEventEntity>) {
        if (events.isNotEmpty()) notifDao.insertAll(events)
    }

    suspend fun readUsageEvents(fromMs: Long, toMs: Long) =
        usageDao.getBetween(fromMs, toMs)

    suspend fun readNotificationEvents(fromMs: Long, toMs: Long) =
        notifDao.getBetween(fromMs, toMs)

    suspend fun pruneOlderThan(olderThanMs: Long): PruneResult {
        val u = usageDao.deleteOlderThan(olderThanMs)
        val n = notifDao.deleteOlderThan(olderThanMs)
        return PruneResult(usageDeleted = u, notificationDeleted = n)
    }

    suspend fun getOldestUsageTimestampMsOrNull(): Long? =
        usageDao.getOldestTimestampMsOrNull()
}

internal data class PruneResult(
    val usageDeleted: Int,
    val notificationDeleted: Int
)