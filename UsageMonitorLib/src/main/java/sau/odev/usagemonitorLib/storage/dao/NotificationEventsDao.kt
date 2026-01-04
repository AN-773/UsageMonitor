package sau.odev.usagemonitorLib.storage.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import sau.odev.usagemonitorLib.storage.entities.NotificationEventEntity

@Dao
internal interface NotificationEventsDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(events: List<NotificationEventEntity>)

    @Query(
        """
    SELECT * FROM notification_events
    WHERE timestampMs >= :fromMs AND timestampMs < :toMs
    ORDER BY timestampMs ASC
  """
    )
    suspend fun getBetween(fromMs: Long, toMs: Long): List<NotificationEventEntity>

    @Query(
        """
    SELECT DISTINCT packageName FROM notification_events
    WHERE timestampMs >= :fromMs AND timestampMs < :toMs
    ORDER BY packageName ASC
  """
    )
    suspend fun getDistinctPackagesBetween(fromMs: Long, toMs: Long): List<String>

    @Query("DELETE FROM notification_events WHERE timestampMs < :olderThanMs")
    suspend fun deleteOlderThan(olderThanMs: Long): Int

    @Query("""
  SELECT packageName, COUNT(*) AS cnt
  FROM notification_events
  WHERE eventType = :postedType
    AND timestampMs >= :fromMs AND timestampMs < :toMs
  GROUP BY packageName
""")
    suspend fun countPostedByPackage(
        fromMs: Long,
        toMs: Long,
        postedType: Int = 1
    ): List<PackageCount>
}