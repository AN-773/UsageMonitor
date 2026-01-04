package sau.odev.usagemonitor.appusagemanager

import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log
import sau.odev.usagemonitor.appusagemanager.alerts.IAlertsManager
import sau.odev.usagemonitor.appusagemanager.apps.App
import sau.odev.usagemonitor.appusagemanager.apps.InstalledApps
import sau.odev.usagemonitor.appusagemanager.challenges.ChallengesManager
import sau.odev.usagemonitor.appusagemanager.challenges.CreatedChallenges
import sau.odev.usagemonitor.appusagemanager.challenges.pojos.AppChallenge
import sau.odev.usagemonitor.appusagemanager.challenges.pojos.DeviceFastChallenge
import sau.odev.usagemonitor.appusagemanager.challenges.pojos.GroupChallenge
import sau.odev.usagemonitor.appusagemanager.groups.CreatedGroups
import sau.odev.usagemonitor.appusagemanager.groups.Group
import sau.odev.usagemonitor.appusagemanager.sessions.BaseSession
import sau.odev.usagemonitor.appusagemanager.sessions.ScreenSession
import sau.odev.usagemonitor.appusagemanager.sessions.SessionsProxy
import sau.odev.usagemonitor.appusagemanager.settings.Settings
import sau.odev.usagemonitor.appusagemanager.utils.MyExecutor
//import com.crashlytics.android.Crashlytics
import khronos.Dates
import khronos.beginningOfDay
import java.util.*
import kotlin.math.max

internal var sDelayScan = 1000L

class RunningAppsObserver internal constructor(
    private val context: Context,
    private val mRunningAppManager: RunningAppManager,
    private val mInstalledApps: InstalledApps,
    private val mCreatedGroups: CreatedGroups,
    private val mSessionsProxy: SessionsProxy,
    private val mSettings: Settings,
    private val mCreatedChallenges: CreatedChallenges,
    private val mChallengesManager: ChallengesManager,
    private val mAlertsManager: IAlertsManager
) {

    @Volatile
    private var isRunning = false

    private var mToday = Dates.today.time

    private val LAST_SCREEN_USAGE_KEY = "lastScreenTime"
    private val LAST_APP_USAGE_KEY = "lastAppTime"
    private val mExecutor = MyExecutor.getExecutor()
    private val mPreferences =
        context.getSharedPreferences(javaClass.simpleName, Context.MODE_PRIVATE)
    private var mOnScreenSession: ScreenSession? = null
    private var mRunningAppSession: RunningAppSession? = null

    private val mKeyguardManager =
        context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    private val mPowerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    private val mAppsConstraints = ArrayList<AppConstraint>()
    private val permissionObservers = ArrayList<(Intent?) -> Unit>()
    private val mAppsObservers = ArrayList<AppsObserver>()
    private val mDeviceObservers = ArrayList<DeviceObserver>()

    private var lastExecuteTime = 0L
    private val mRunnable = object : Runnable {
        override fun run() {
            try {
                observe()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                val timeConsumed = System.currentTimeMillis() - lastExecuteTime
                val delay = sDelayScan - timeConsumed
                mExecutor.executeDelayed(this, max(0, delay))
            }
        }
    }

    internal fun startObserving() {
        synchronized(this) {
            if (isRunning) return
            isRunning = true
        }
        mExecutor.execute {
            // Sync usage stats on startup

            val screenSessionObject = mPreferences.getString(LAST_SCREEN_USAGE_KEY, null)
            if (screenSessionObject != null) {
                val values = screenSessionObject.split(":")
                val screenSession =
                    ScreenSession(0, values[0].toLong(), values[1].toLong(), 1, values[2].toLong())

                try {
                    mSessionsProxy.addScreenSession(screenSession)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            mPreferences.edit().remove(LAST_SCREEN_USAGE_KEY).apply()
            val appSessionObject = mPreferences.getString(LAST_APP_USAGE_KEY, null)
            if (appSessionObject != null) {
                val values = appSessionObject.split(":")
                val app = mInstalledApps.getApp(values[0].toLong())
                if (app?.ignored == false) {
                    val appId = values[0].toLong()
                    val groupId = values[1].toLong()
                    val launchTime = values[2].toLong()
                    val session =
                        BaseSession(
                            0,
                            appId,
                            groupId,
                            Dates.from(launchTime).beginningOfDay.time,
                            launchTime,
                            1,
                            values[3].toLong()
                        )
                    try {
                        mSessionsProxy.addSession(session)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            mPreferences.edit().remove(LAST_APP_USAGE_KEY).apply()
            mExecutor.execute(mRunnable)
        }
    }


    private fun observe() {
        // TODO This is overwhelming getting all usage stats every second for the whole day but for now we have no other fast option
        val usageStatsSync = AppsUsageManager.getInstance(context).getUsageStatsSync()
        usageStatsSync.syncTodayUsage()
        lastExecuteTime = System.currentTimeMillis()
        val today = Dates.today.time
        if (mToday != today) {
            onNewDay()
            mToday = today
        }
        if (isDeviceInteractive() && isDeviceUnlocked()) {
            if (mOnScreenSession == null) {
                mOnScreenSession = ScreenSession(
                    launchDay = Dates.today.time,
                    launchTime = System.currentTimeMillis(),
                    usageDuration = mSessionsProxy.getDeviceUsageDuration(Dates.today.time)
                )
                synchronized(mDeviceObservers) {
                    mDeviceObservers.forEach {
                        it.onDeviceUnlocked(mOnScreenSession!!)
                    }
                }
            } else {
                val sc = mOnScreenSession as ScreenSession
                sc.usageDuration += sDelayScan
                mPreferences.edit()
                    .putString(
                        LAST_SCREEN_USAGE_KEY,
                        "${sc.launchDay}:${sc.launchTime}:${System.currentTimeMillis() - sc.launchTime}"
                    )
                    .apply()
            }

            val runningPackage =
                null; // Disable getting running app for now we are going to use UsageStatsManager
//            val runningPackage = mRunningAppManager.getRunningApp()
            if (runningPackage == null) {
                if (!mRunningAppManager.isPermissionGranted())
                    notifyUsageStatsPermissionObservers()
                if (mRunningAppSession != null) {
                    onAppClosed()
                }
                return
            }
            if (mRunningAppSession?.app?.packageName == runningPackage) {
                checkAppValid(mRunningAppSession as RunningAppSession)
                if (mRunningAppSession?.stopped == false)
                    updateRunningSession()
            } else {
                val startTime = System.currentTimeMillis()
                if (mRunningAppSession != null)
                    onAppClosed()
                onAppLaunched(runningPackage, startTime)
                checkAppValid(mRunningAppSession as RunningAppSession)
            }
        } else {
            onDeviceClosed()
        }
    }

    @SuppressLint("ApplySharedPref")
    private fun updateRunningSession() {
        val rp = mRunningAppSession as RunningAppSession
        rp.sessionUsage += sDelayScan
        rp.appTodayUsage += sDelayScan
        rp.groupTodayUsage += sDelayScan
        Log.d(
            "updateRunningSession",
            "updateRunningSession: ${rp.app.name} , sessionUsage: ${rp.sessionUsage}, appTodayUsage: ${rp.appTodayUsage}, groupTodayUsage: ${rp.groupTodayUsage}"
        )
        synchronized(mAppsObservers) {
            mAppsObservers.forEach {
                it.onNext(rp, sDelayScan)
            }
        }
        mPreferences.edit()
            .putString(
                LAST_APP_USAGE_KEY,
                "${rp.app.id}:${rp.group.id}:${rp.launchTime}:${System.currentTimeMillis() - rp.launchTime}"
            )
            .commit()
    }

    private fun onNewDay() {
        onDeviceClosed()
    }

    private fun onDeviceClosed() {
        mOnScreenSession ?: return
        onAppClosed()
        println("On device closed")
        try {
            val screenSession = mOnScreenSession!!.copy()
            screenSession.usageDuration = System.currentTimeMillis() - screenSession.launchTime
            println("Total usage is: ${screenSession.usageDuration}")
            mSessionsProxy.addScreenSession(screenSession)
            mPreferences.edit().remove(LAST_SCREEN_USAGE_KEY).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val session = mOnScreenSession!!
        mOnScreenSession = null
        synchronized(mDeviceObservers) {
            mDeviceObservers.forEach {
                it.onDeviceClosed(session)
            }
        }
    }

    private fun onAppLaunched(packageName: String, startTime: Long) {
//        mAlertsManager.hideAlert()
        val startOfDay = Dates.today.time
        val app = mInstalledApps.getApp(packageName) ?: return
        if (!app.installed)
            app.installed = true
        val group = mCreatedGroups.getGroup(app.groupId) ?: return
        mRunningAppSession = RunningAppSession(
            app, group, startTime,
            mSessionsProxy.getAppUsageDuration(app.id, startOfDay, System.currentTimeMillis()),
            mSessionsProxy.getGroupUsageDuration(
                app.groupId,
                startOfDay,
                System.currentTimeMillis()
            )
        )
        synchronized(mAppsObservers) {
            mAppsObservers.forEach {
                it.onAppLaunched(mRunningAppSession!!)
            }
        }

    }

    private fun onAppClosed() {
        Log.d("onAppClosed", "onAppClosed: clalled")
        mAlertsManager.hideAlert()
        if (mRunningAppSession == null)
            return
        val rp = mRunningAppSession as RunningAppSession
        mRunningAppSession = null
        synchronized(mAppsObservers) {
            mAppsObservers.forEach {
                it.onAppClosed(rp)
            }
        }
        if (rp.app.packageName in IGNORED_PACKAGES || rp.app.ignored || rp.sessionUsage < 3000) return
        val session = BaseSession(
            appId = rp.app.id,
            groupId = rp.app.groupId,
            launchDay = Dates.from(rp.launchTime).beginningOfDay.time,
            launchTime = rp.launchTime,
            usageDuration = rp.sessionUsage
        )
        try {
            mSessionsProxy.addSession(session)
            mPreferences.edit().remove(LAST_APP_USAGE_KEY).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun checkAppValid(rp: RunningAppSession) {
        synchronized(mAppsConstraints) {
            for (constraint in mAppsConstraints) {
                if (!checkConstraint(constraint, rp))
                    return
            }
        }
    }

    private fun checkConstraint(constraint: AppConstraint, rp: RunningAppSession): Boolean {
        val pairResult = constraint.isSessionValid(rp, mOnScreenSession as ScreenSession)

        if (pairResult.first) {
            if (rp.stopped) {
                rp.stopped = false
                if (constraint is DefaultAppConstraint)
                    mAlertsManager.hideAlert()
            }
            return true
        }
        rp.stopped = true
        synchronized(mAppsObservers) {
            mAppsObservers.forEach {
                it.onAppStopped(rp)
            }
        }
        when (pairResult.second) {
            AppConstraintType.TYPE_APP_USAGE -> {
                mAlertsManager.showAppUsageAlert(rp.app, rp.launchTime, mInstalledApps)
            }

            AppConstraintType.TYPE_GROUP_USAGE -> {
                mAlertsManager.showGroupUsageAlert(
                    rp.app,
                    rp.launchTime,
                    rp.group,
                    mCreatedGroups,
                    mInstalledApps
                )
            }

            AppConstraintType.TYPE_DEVICE_USAGE -> {
                mAlertsManager.showDeviceUsageAlert(mSettings)
            }

            AppConstraintType.TYPE_GROUP_FAST_CHALLENGE -> {
                mAlertsManager.showGroupFastChallengeAlert(
                    mChallengesManager.getInvalidChallengeOfThisSession(rp) as GroupChallenge,
                    rp.app,
                    rp.group,
                    mInstalledApps,
                    mCreatedGroups,
                    mCreatedChallenges
                )
            }

            AppConstraintType.TYPE_GROUP_LIMIT_USAGE_CHALLENGE -> {
                mAlertsManager.showGroupLimitChallengeAlert(
                    mChallengesManager.getInvalidChallengeOfThisSession(rp) as GroupChallenge,
                    rp.app,
                    rp.group,
                    mInstalledApps,
                    mCreatedGroups,
                    mCreatedChallenges
                )
            }

            AppConstraintType.TYPE_APP_FAST_CHALLENGE -> {
                mAlertsManager.showAppFastChallengeAlert(
                    mChallengesManager.getInvalidChallengeOfThisSession(rp) as AppChallenge,
                    rp.app,
                    mInstalledApps,
                    mCreatedChallenges
                )
            }

            AppConstraintType.TYPE_APP_LIMIT_USAGE_CHALLENGE -> {
                mAlertsManager.showAppLimitChallengeAlert(
                    mChallengesManager.getInvalidChallengeOfThisSession(rp) as AppChallenge,
                    rp.app,
                    mInstalledApps,
                    mCreatedChallenges
                )
            }

            AppConstraintType.TYPE_DEVICE_FAST_CHALLENGE -> {
                mAlertsManager.showDeviceFastChallenge(
                    mChallengesManager.getInvalidChallengeOfThisSession(rp) as DeviceFastChallenge,
                    mCreatedChallenges,
                    mSettings,
                    mInstalledApps.getDialPackages()[0]
                )
            }

            else -> {
            }
        }
        return false
    }

    private fun notifyUsageStatsPermissionObservers() {
        synchronized(permissionObservers) {
            for (a in permissionObservers) {
                a(mRunningAppManager.requestPermissionIntent())
            }
        }
    }

    internal fun addUsageStatsPermissionObserver(observer: (Intent?) -> Unit) {
        synchronized(permissionObservers) {
            permissionObservers.add(observer)
        }
    }

    internal fun removeUsageStatsPermissionObserver(observer: (Intent?) -> Unit) {
        synchronized(permissionObservers) {
            permissionObservers.remove(observer)
        }
    }

    internal fun addAppsObserver(observer: AppsObserver) {
        synchronized(mAppsObservers) {
            mAppsObservers.add(observer)
        }
    }

    internal fun removeAppsObserver(observer: AppsObserver) {
        synchronized(mAppsObservers) {
            mAppsObservers.remove(observer)
        }
    }

    internal fun addDeviceObserver(observer: DeviceObserver) {
        synchronized(mDeviceObservers) {
            mDeviceObservers.add(observer)
        }
    }

    internal fun removeDeviceObserver(observer: DeviceObserver) {
        synchronized(mDeviceObservers) {
            mDeviceObservers.remove(observer)
        }
    }

    internal fun addAppConstraint(constraint: AppConstraint) {
        synchronized(mAppsConstraints) {
            for (i in 0 until mAppsConstraints.size) {
                val o = mAppsConstraints[i]
                if (o.getPriority() < constraint.getPriority()) {
                    mAppsConstraints.add(i, constraint)
                    return
                }
            }
            mAppsConstraints.add(constraint)
        }
    }

    internal fun removeAppConstraint(constraint: AppConstraint) {
        synchronized(mAppsConstraints) {
            mAppsConstraints.remove(constraint)
        }
    }

    private fun isDeviceInteractive(): Boolean = mPowerManager.isInteractive

    private fun isDeviceUnlocked(): Boolean = !mKeyguardManager.isKeyguardLocked

}

class RunningAppSession(
    val app: App,
    val group: Group,
    val launchTime: Long,
    var appTodayUsage: Long,
    var groupTodayUsage: Long,
    var sessionUsage: Long = 0L,
    var stopped: Boolean = false
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RunningAppSession) return false

        if (app != other.app) return false
        if (group != other.group) return false
        if (launchTime != other.launchTime) return false
        if (appTodayUsage != other.appTodayUsage) return false
        if (groupTodayUsage != other.groupTodayUsage) return false
        if (stopped != other.stopped) return false

        return true
    }

    override fun hashCode(): Int {
        var result = app.hashCode()
        result = 31 * result + group.hashCode()
        result = 31 * result + launchTime.hashCode()
        result = 31 * result + appTodayUsage.hashCode()
        result = 31 * result + groupTodayUsage.hashCode()
        result = 31 * result + stopped.hashCode()
        return result
    }

    override fun toString(): String {
        return "RunningAppSession(app=${app.name}: ${app.packageName}, ignoreUntil=${app.ignoreMaxUsageUntil}, launchTime=$launchTime, appTodayUsage=$appTodayUsage, groupTodayUsage=$groupTodayUsage, stopped=$stopped)"
    }
}

interface AppsObserver {

    fun onAppLaunched(rp: RunningAppSession)

    fun onAppClosed(rp: RunningAppSession)

    fun onNext(rp: RunningAppSession, delay: Long)

    fun onAppStopped(rp: RunningAppSession)
}

interface DeviceObserver {

    fun onDeviceUnlocked(screenSession: ScreenSession)

    fun onDeviceClosed(screenSession: ScreenSession)
}

enum class AppConstraintType {
    VALID,
    TYPE_DEVICE_USAGE,
    TYPE_APP_USAGE,
    TYPE_GROUP_USAGE,
    TYPE_DEVICE_FAST_CHALLENGE,
    TYPE_APP_FAST_CHALLENGE,
    TYPE_APP_LIMIT_USAGE_CHALLENGE,
    TYPE_GROUP_FAST_CHALLENGE,
    TYPE_GROUP_LIMIT_USAGE_CHALLENGE
}

interface AppConstraint {

    fun isSessionValid(
        appSession: RunningAppSession,
        screenSession: ScreenSession
    ): Pair<Boolean, AppConstraintType>

    fun getPriority(): Int

}