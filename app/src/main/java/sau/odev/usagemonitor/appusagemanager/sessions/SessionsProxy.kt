package sau.odev.usagemonitor.appusagemanager.sessions

import khronos.Dates
import java.util.concurrent.Executors

class SessionsProxy internal constructor(private val mSessionsDao: SessionsDao,
                                         private val mScreenSessionsDao: ScreenSessionsDao,
                                         private val mCurrentScreenSessionsTracker: CurrentScreenSessionsTracker) : ISessionsDaosProxy {

    private val executor = Executors.newSingleThreadExecutor()

/*
    @SuppressLint("WrongConstant")
    private fun loadSystemSessions() {
        if (mRunningAppManager.isPermissionGranted() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                && mPreferences.getBoolean("loadSysSessions", true)) {
            mPreferences.edit().putBoolean("loadSysSessions", false).apply()
            val usageStatsManager: UsageStatsManager = context.getSystemService("usagestats") as UsageStatsManager
            val list = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, 0, Date().time)
            for (stats in list) {
                val app = mInstalledApps.getApp(stats.packageName)
                if (app != null) {
                    mCalendar.time = Date(stats.lastTimeStamp)
                    if (stats.lastTimeStamp < 1262293200000L || stats.totalTimeInForeground == 0L) {
                        break
                    }
                    val session = BaseSession(0, app.id, app.groupId, stats.lastTimeStamp, 1, stats.totalTimeInForeground)
                    mSessionsDao.addSession(session)
                }
            }
        }
    }
*/

    override fun getAppsUsageOrderByUsageTime(from: Long, to: Long, callback: (List<Session>) -> Unit) =
            executor.execute {
                val list = mSessionsDao.getAppsUsageOrderByUsageTime(from, to)
                callback(list)
            }

    override fun getGroupsUsageOrderByUsageTime(from: Long, to: Long, callback: (List<Session>) -> Unit) =
            executor.execute {
                val list = mSessionsDao.getGroupsUsageOrderByUsageTime(from, to)
                callback(list)
            }

    override fun getGroupSessions(groupId: Long, from: Long, to: Long, callback: (List<Session>) -> Unit) =
            executor.execute {
                val list = mSessionsDao.getGroupSessions(groupId, from, to)
                callback(list)
            }

    override fun getAppSessions(appId: Long, from: Long, to: Long, callback: (List<BaseSession>) -> Unit) =
            executor.execute {
                val list = mSessionsDao.getAppSessions(appId, from, to)
                callback(list)
            }

    override fun getAppSessionsByDay(appId: Long, from: Long, to: Long, callback: (List<BaseSession>) -> Unit) =
            executor.execute {
                val list = mSessionsDao.getAppSessionsByDay(appId, from, to)
                callback(list)
            }

    override fun getTotalUsageByDay(day: Long, callback: (ScreenSession?) -> Unit) =
            executor.execute {
                val session = mScreenSessionsDao.getUsageByDay(day)
                if (day == Dates.today.time)
                    if (session == null) {
                        val s = ScreenSession(0, Dates.today.time, System.currentTimeMillis(), 1, mCurrentScreenSessionsTracker.getCurrentSessionDuration())
                        callback(s)
                        return@execute
                    } else {
                        session.apply {
                            usageDuration += mCurrentScreenSessionsTracker.getCurrentSessionDuration()
                        }
                    }
                callback(session)
            }

    override fun getScreenSessions(from: Long, to: Long, callback: (List<ScreenSession>) -> Unit) =
            executor.execute {
                val result = mScreenSessionsDao.getScreenSessions(from, to)
                callback(result)
            }

    override fun getScreenSessionsByDay(from: Long, to: Long, callback: (List<ScreenSession>) -> Unit) =
            executor.execute {
                val result = mScreenSessionsDao.getScreenSessionsByDay(from, to)
                if (to > Dates.today.time) {
                    val session = result.lastOrNull()
                    if (session == null || session.launchDay != Dates.today.time) {
                        val s = ScreenSession(0, Dates.today.time, System.currentTimeMillis(), 1, mCurrentScreenSessionsTracker.getCurrentSessionDuration())
                        val list = ArrayList(result)
                        list.add(s)
                        callback(list)
                        return@execute
                    }else {
                        session.apply {
                            usageDuration += mCurrentScreenSessionsTracker.getCurrentSessionDuration()
                        }
                    }
                }
                callback(result)
            }

    internal fun getTopUsedApps(from: Long, to: Long, max: Int): List<BaseSession> = mSessionsDao.getTopUsedAppsSessions(from, to, max)

    internal fun getTopVisitedApps(from: Long, to: Long, max: Int): List<BaseSession> = mSessionsDao.getTopVisitedAppsSessions(from, to, max)

    internal fun getTopUsedGroups(from: Long, to: Long, max: Int): List<BaseSession> = mSessionsDao.getTopUsedGroupsSessions(from, to, max)

    internal fun getTopVisitedGroups(from: Long, to: Long, max: Int): List<BaseSession> = mSessionsDao.getTopVisitedGroupsSessions(from, to, max)

    internal fun getDeviceAverageUsage(from: Long, to: Long): Long {
        val list = mScreenSessionsDao.getScreenSessions(from, to)
        return list.sumByDouble {
            it.usageDuration.toDouble()
        }.toLong() / list.size
    }

    internal fun getScreenSessions(from: Long, to: Long)  = mScreenSessionsDao.getScreenSessions(from, to)

    internal fun addScreenSession(screenSession: ScreenSession) = mScreenSessionsDao.addSession(screenSession)

    internal fun addSession(session: BaseSession) {
        if (session.usageDuration > 0)
            mSessionsDao.addSession(session)
    }

    internal fun getDeviceUsageDuration(day: Long): Long = mScreenSessionsDao.getUsageDurationByDay(day)

    internal fun getAppUsageDuration(appId: Long, startOfDay: Long, endOfDay: Long): Long = mSessionsDao.getAppUsageDuration(appId, startOfDay, endOfDay)

    internal fun getGroupUsageDuration(groupId: Long, startOfDay: Long, endOfDay: Long): Long = mSessionsDao.getGroupUsageDuration(groupId, startOfDay, endOfDay)

    internal fun getAppSessionCount(appId: Long, from: Long, to: Long): Int = mSessionsDao.getAppSessionCount(appId, from, to)


}
