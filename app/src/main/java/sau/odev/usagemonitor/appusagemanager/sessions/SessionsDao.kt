package sau.odev.usagemonitor.appusagemanager.sessions

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SessionsDao {

    @Insert
    fun addSession(session: BaseSession)

    @Query(
            "SELECT sessions.id, " +
                    "sessions.app_id, " +
                    "sessions.group_id, " +
                    "sessions.launch_day, " +
                    "sessions.launch_time, " +
                    "sum(sessions.usage_duration) AS usage_duration, " +
                    "sum(sessions.visit_count) AS visit_count, " +
                    "apps.package_name AS package_name, " +
                    "apps.name AS app_name, " +
                    "groups.name AS group_name " +
                    "FROM sessions " +
                    "INNER JOIN apps " +
                    "ON sessions.app_id = apps.id " +
                    "INNER JOIN groups " +
                    "ON sessions.group_id = groups.id " +
                    "WHERE sessions.launch_time >= :from " +
                    "AND sessions.launch_time < :to " +
                    "GROUP BY app_id " +
                    "ORDER BY usage_duration DESC"

    )
    fun getAppsUsageOrderByUsageTime(from: Long, to: Long): List<Session>

    @Query(
            "SELECT sessions.id, " +
                    "sessions.app_id, " +
                    "sessions.group_id, " +
                    "sessions.launch_day, " +
                    "sessions.launch_time, " +
                    "sum(sessions.usage_duration) AS usage_duration, " +
                    "sum(sessions.visit_count) AS visit_count, " +
                    "apps.package_name AS package_name, " +
                    "apps.name AS app_name, " +
                    "groups.name AS group_name " +
                    "FROM sessions " +
                    "INNER JOIN apps ON sessions.app_id = apps.id " +
                    "INNER JOIN groups ON sessions.group_id = groups.id " +
                    "WHERE sessions.launch_time >= :from " +
                    "AND sessions.launch_time < :to " +
                    "GROUP BY sessions.group_id " +
                    "ORDER BY usage_duration DESC"

    )
    fun getGroupsUsageOrderByUsageTime(from: Long, to: Long): List<Session>

    @Query("SELECT sum(se.usage_duration) as usage_duration FROM sessions as se " +
            "WHERE se.app_id = :appId " +
            "AND se.launch_time >= :from " +
            "AND se.launch_time < :to")
    fun getAppUsageDuration(appId: Long, from: Long, to: Long): Long

    @Query("SELECT COUNT(*) FROM sessions as se " +
            "WHERE se.app_id = :appId " +
            "AND se.launch_time >= :from " +
            "AND se.launch_time < :to")
    fun getAppSessionCount(appId: Long, from: Long, to: Long): Int

    @Query("SELECT sum(se.usage_duration) as usage_duration FROM sessions as se " +
            "WHERE se.group_id = :groupId " +
            "AND se.launch_time >= :from " +
            "AND se.launch_time < :to")

    fun getGroupUsageDuration(groupId: Long, from: Long, to: Long): Long

    @Query("SELECT * " +
            "FROM sessions AS se " +
            "WHERE se.app_id = :appId " +
            "AND se.launch_time >= :from " +
            "AND se.launch_time < :to " +
            "ORDER BY se.launch_time ASC")
    fun getAppSessions(appId: Long, from: Long, to: Long): List<BaseSession>

    @Query("SELECT se.id, se.app_id, se.group_id, se.launch_day, se.launch_time, sum(se.visit_count) as visit_count, sum(se.usage_duration) as usage_duration FROM sessions AS se " +
            "WHERE se.app_id = :appId " +
            "AND se.launch_time >= :from " +
            "AND se.launch_time < :to " +
            "GROUP BY se.launch_day " +
            "ORDER BY se.launch_time DESC")
    fun getAppSessionsByDay(appId: Long, from: Long, to: Long): List<BaseSession>

    @Query("SELECT se.*, " +
            "apps.name AS app_name, " +
            "groups.name AS group_name, " +
            "apps.package_name AS package_name " +
            "FROM sessions AS se " +
            "INNER JOIN apps ON se.app_id = apps.id " +
            "INNER JOIN groups ON se.group_id = groups.id " +
            "WHERE se.group_id = :groupId " +
            "AND se.launch_time >= :from " +
            "AND se.launch_time < :to " +
            "ORDER BY se.launch_time ASC")
    fun getGroupSessions(groupId: Long, from: Long, to: Long): List<Session>

    @Query("SELECT se.id, se.app_id, se.group_id, se.launch_day, se.launch_time, sum(se.visit_count) as visit_count, sum(se.usage_duration) as usage_duration FROM sessions AS se WHERE se.launch_time >= :from AND se.launch_time < :to AND se.group_id != 0 GROUP BY se.group_id ORDER BY usage_duration DESC LIMIT :max")
    fun getTopUsedGroupsSessions(from: Long, to: Long, max: Int): List<BaseSession>

    @Query("SELECT se.id, se.app_id, se.group_id, se.launch_day, se.launch_time, sum(se.visit_count) as visit_count, sum(se.usage_duration) as usage_duration FROM sessions AS se WHERE se.launch_time >= :from AND se.launch_time < :to AND se.group_id != 0 GROUP BY se.group_id ORDER BY visit_count DESC LIMIT :max")
    fun getTopVisitedGroupsSessions(from: Long, to: Long, max: Int): List<BaseSession>

    @Query("SELECT se.id, se.app_id, se.group_id, se.launch_day, se.launch_time, sum(se.visit_count) as visit_count, sum(se.usage_duration) as usage_duration FROM sessions AS se WHERE se.launch_time >= :from AND se.launch_time < :to GROUP BY se.app_id ORDER BY usage_duration DESC LIMIT :max")
    fun getTopUsedAppsSessions(from: Long, to: Long, max: Int): List<BaseSession>

    @Query("SELECT se.id, se.app_id, se.group_id, se.launch_day, se.launch_time, sum(se.visit_count) as visit_count, sum(se.usage_duration) as usage_duration FROM sessions AS se WHERE se.launch_time >= :from AND se.launch_time < :to GROUP BY se.app_id ORDER BY visit_count DESC LIMIT :max")
    fun getTopVisitedAppsSessions(from: Long, to: Long, max: Int): List<BaseSession>
}