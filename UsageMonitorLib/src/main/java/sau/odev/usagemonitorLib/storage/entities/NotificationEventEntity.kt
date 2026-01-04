package sau.odev.usagemonitorLib.storage.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "notification_events",
  indices = [
    Index(value = ["packageName", "timestampMs"]),
    Index(value = ["timestampMs"]),
    Index(value = ["notificationKeyHash"])
  ]
)
internal data class NotificationEventEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val packageName: String,
  val eventType: Int,              // POSTED / REMOVED
  val timestampMs: Long,

  // Helps de-dup / correlate posted vs removed without storing PII-like content.
  val notificationKeyHash: String? = null,

  // Optional metadata (safe-ish, still avoid titles/text)
  val channelId: String? = null,
  val category: String? = null
)