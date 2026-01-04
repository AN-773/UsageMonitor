package sau.odev.usagemonitorLib.notifications

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import sau.odev.usagemonitorLib.storage.Storage
import sau.odev.usagemonitorLib.storage.entities.NotificationEventEntity
import sau.odev.usagemonitorLib.storage.NotificationEventTypes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WbkNotificationListenerService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName ?: return
        val ts = sbn.postTime.takeIf { it > 0 } ?: System.currentTimeMillis()

        // sbn.key is stable per notification instance; hash it to avoid storing raw key.
        val keyHash = runCatching { Hashing.sha256Hex(sbn.key ?: "") }.getOrNull()

        val channelId = runCatching { sbn.notification?.channelId }.getOrNull()
        val category = runCatching { sbn.notification?.category }.getOrNull()

        val entity = NotificationEventEntity(
            packageName = pkg,
            eventType = NotificationEventTypes.POSTED,
            timestampMs = ts,
            notificationKeyHash = keyHash,
            channelId = channelId,
            category = category
        )

        scope.launch {
            Storage(applicationContext).writeNotificationEvents(listOf(entity))
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        val pkg = sbn.packageName ?: return
        val ts = System.currentTimeMillis()
        val keyHash = runCatching { Hashing.sha256Hex(sbn.key ?: "") }.getOrNull()

        val entity = NotificationEventEntity(
            packageName = pkg,
            eventType = NotificationEventTypes.REMOVED,
            timestampMs = ts,
            notificationKeyHash = keyHash
        )

        scope.launch {
            Storage(applicationContext).writeNotificationEvents(listOf(entity))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // scope cancels automatically when process dies; explicit cancel is optional
    }
}