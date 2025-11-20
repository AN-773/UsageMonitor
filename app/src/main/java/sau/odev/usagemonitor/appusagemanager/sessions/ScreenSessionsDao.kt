package sau.odev.usagemonitor.appusagemanager.sessions

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ScreenSessionsDao {

    @Insert
    fun addSession(session: ScreenSession)

    @Query("SELECT * FROM screen_sessions WHERE launch_time >= :from AND launch_time < :to")
    fun getScreenSessions(from: Long, to: Long): List<ScreenSession>

    @Query("SELECT id, " +
            "launch_time, " +
            "launch_day, " +
            "sum(visit_count) AS visit_count, " +
            "sum(usage_duration ) AS usage_duration " +
            "FROM screen_sessions " +
            "WHERE launch_day = :day " +
            "GROUP BY launch_day")
    fun getUsageByDay(day: Long): ScreenSession?

    @Query("SELECT id, " +
            "launch_time, " +
            "launch_day, " +
            "sum(visit_count) AS visit_count, " +
            "sum(usage_duration ) AS usage_duration " +
            "FROM screen_sessions " +
            "WHERE launch_time >= :from AND launch_time < :to " +
            "GROUP BY launch_day")
    fun getScreenSessionsByDay(from: Long, to: Long): List<ScreenSession>

    @Query("SELECT sum(usage_duration ) AS usage_duration " +
            "FROM screen_sessions " +
            "WHERE launch_day = :day " +
            "GROUP BY launch_day")
    fun getUsageDurationByDay(day: Long): Long

    @Query("DELETE FROM screen_sessions WHERE launch_day = :dayStart")
    fun deleteTodaySessions(dayStart: Long)
}