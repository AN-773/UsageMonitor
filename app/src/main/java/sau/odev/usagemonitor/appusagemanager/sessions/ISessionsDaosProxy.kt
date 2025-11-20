package sau.odev.usagemonitor.appusagemanager.sessions

interface ISessionsDaosProxy {

    fun getAppsUsageOrderByUsageTime(from: Long, to: Long, callback: (List<Session>) -> Unit)

    fun getGroupsUsageOrderByUsageTime(from: Long, to: Long, callback: (List<Session>) -> Unit)

    fun getGroupSessions(groupId: Long, from: Long, to: Long, callback: (List<Session>) -> Unit)

    fun getAppSessions(appId: Long, from: Long, to: Long, callback: (List<BaseSession>) -> Unit)

    fun getAppSessionsByDay(appId: Long, from: Long, to: Long, callback: (List<BaseSession>) -> Unit)

    fun getTotalUsageByDay(day: Long, callback: (ScreenSession?) -> Unit)

    fun getScreenSessions(from: Long, to: Long, callback: (List<ScreenSession>) -> Unit)

    fun getScreenSessionsByDay(from: Long, to: Long, callback: (List<ScreenSession>) -> Unit)

}