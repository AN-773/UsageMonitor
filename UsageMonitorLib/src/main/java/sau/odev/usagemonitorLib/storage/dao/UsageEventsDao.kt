package sau.odev.usagemonitorLib.storage.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import sau.odev.usagemonitorLib.storage.entities.UsageEventEntity

@Dao
internal interface UsageEventsDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(events: List<UsageEventEntity>)

    @Query(
        """
    SELECT * FROM usage_events
    WHERE timestampMs >= :fromMs AND timestampMs < :toMs
    ORDER BY timestampMs ASC
  """
    )
    suspend fun getBetween(fromMs: Long, toMs: Long): List<UsageEventEntity>

    @Query(
        """
    SELECT DISTINCT packageName FROM usage_events
    WHERE timestampMs >= :fromMs AND timestampMs < :toMs
    ORDER BY packageName ASC
  """
    )
    suspend fun getDistinctPackagesBetween(fromMs: Long, toMs: Long): List<String>

    @Query("DELETE FROM usage_events WHERE timestampMs < :olderThanMs")
    suspend fun deleteOlderThan(olderThanMs: Long): Int

    @Query(
        """
  SELECT packageName, COUNT(*) AS cnt
  FROM usage_events
  WHERE eventType IN (:primaryTypes)
    AND timestampMs >= :fromMs AND timestampMs < :toMs
  GROUP BY packageName
"""
    )
    suspend fun countLaunchesPrimary(
        fromMs: Long,
        toMs: Long,
        primaryTypes: List<Int>
    ): List<PackageCount>

    @Query("SELECT MIN(timestampMs) FROM usage_events")
    suspend fun getOldestTimestampMsOrNull(): Long?
}