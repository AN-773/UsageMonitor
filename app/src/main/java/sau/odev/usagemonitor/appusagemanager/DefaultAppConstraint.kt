package sau.odev.usagemonitor.appusagemanager

import android.content.Context
import sau.odev.usagemonitor.appusagemanager.sessions.ScreenSession
import sau.odev.usagemonitor.appusagemanager.settings.IDeviceUsageSettings

class DefaultAppConstraint(val context: Context, val settings: IDeviceUsageSettings) : AppConstraint {

    override fun isSessionValid(appSession: RunningAppSession, screenSession: ScreenSession): Pair<Boolean, AppConstraintType> {
        val maxUsagePerDayInSeconds = settings.getDeviceMaxUsageDurationInSeconds(0)
        val ignoreMaxUsagePerDayUntil = settings.getDeviceIgnoreUsageDurationUntil(0L)
        //screenSessions.usageDuration = today overall usage duration
        if (appSession.app.packageName != PACKAGE_NAME && maxUsagePerDayInSeconds != 0 && maxUsagePerDayInSeconds < (screenSession.usageDuration / 1000) && ignoreMaxUsagePerDayUntil < System.currentTimeMillis()) {
            return Pair(false, AppConstraintType.TYPE_DEVICE_USAGE)
        }
        val app = appSession.app
        return if (app.ignored) {
            Pair(true, AppConstraintType.VALID)
        } else if (app.ignoreMaxUsageUntil == appSession.launchTime) {
            Pair(true, AppConstraintType.VALID)
        }else if (app.ignoreMaxUsageUntil > System.currentTimeMillis()) {
            Pair(true, AppConstraintType.VALID)
        } else if (app.maxUsagePerDayInSeconds != 0 && (appSession.appTodayUsage / 1000) >= app.maxUsagePerDayInSeconds) {
            Pair(false, AppConstraintType.TYPE_APP_USAGE)
        } else if (appSession.group.maxUsagePerDayInSeconds != 0 && (appSession.groupTodayUsage / 1000) >= appSession.group.maxUsagePerDayInSeconds) {
            Pair(false, AppConstraintType.TYPE_GROUP_USAGE)

        } else
            Pair(true, AppConstraintType.VALID)
    }

    override fun getPriority(): Int = 1

}