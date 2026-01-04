package sau.odev.usagemonitorLib.storage.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "usage_events",
  indices = [
    Index(value = ["packageName", "timestampMs"]),
    Index(value = ["timestampMs"])
  ]
)
internal data class UsageEventEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val packageName: String,
  val eventType: Int,
  val timestampMs: Long
)