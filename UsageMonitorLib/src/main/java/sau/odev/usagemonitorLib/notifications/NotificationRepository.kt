package sau.odev.usagemonitorLib.notifications

import android.content.Context
import sau.odev.usagemonitorLib.storage.Storage
import sau.odev.usagemonitorLib.storage.NotificationEventTypes

internal class NotificationRepository(context: Context) {

    private val storage = Storage(context.applicationContext)

    /**
     * Count notifications POSTED per package in [fromMs, toMs).
     */
    suspend fun getPostedCounts(fromMs: Long, toMs: Long): Map<String, Int> {
        val events = storage.readNotificationEvents(fromMs, toMs)
        val out = HashMap<String, Int>(128)

        for (e in events) {
            if (e.eventType == NotificationEventTypes.POSTED) {
                out[e.packageName] = (out[e.packageName] ?: 0) + 1
            }
        }
        return out
    }

    suspend fun getPostedCount(packageName: String, fromMs: Long, toMs: Long): Int {
        val events = storage.readNotificationEvents(fromMs, toMs)
        var c = 0
        for (e in events) {
            if (e.packageName == packageName && e.eventType == NotificationEventTypes.POSTED) c++
        }
        return c
    }

    suspend fun getPostedCountsFast(fromMs: Long, toMs: Long): Map<String, Int> =
        storage.notifDao
            .countPostedByPackage(fromMs, toMs)
            .associate { it.packageName to it.cnt }

    suspend fun getPackageNotificationCount(
        packageName: String,
        fromMs: Long,
        toMs: Long
    ): Int {
        return storage
            .readNotificationEvents(fromMs, toMs)
            .count { it.packageName == packageName && it.eventType == NotificationEventTypes.POSTED }
    }
}