package sau.odev.usagemonitor.appusagemanager

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import sau.odev.usagemonitor.appusagemanager.alerts.AlertsManagerWrapper
import sau.odev.usagemonitor.appusagemanager.alerts.IAlertsManager
import sau.odev.usagemonitor.appusagemanager.apps.InstalledApps
import sau.odev.usagemonitor.appusagemanager.challenges.ChallengesManager
import sau.odev.usagemonitor.appusagemanager.challenges.CreatedChallenges
import sau.odev.usagemonitor.appusagemanager.groups.CreatedGroups
import sau.odev.usagemonitor.appusagemanager.notifications.NotificationTracker
import sau.odev.usagemonitor.appusagemanager.sessions.CurrentScreenSessionsTracker
import sau.odev.usagemonitor.appusagemanager.sessions.SessionsProxy
import sau.odev.usagemonitor.appusagemanager.settings.IDeviceUsageSettings
import sau.odev.usagemonitor.appusagemanager.settings.Settings
import sau.odev.usagemonitor.appusagemanager.utils.MyExecutor

internal lateinit var PACKAGE_NAME: String
internal val IGNORED_PACKAGES = ArrayList<String>()
@SuppressLint("StaticFieldLeak")

class AppsUsageManager private constructor(private val context: Context,
                                           db: UsageDatabase) {

    private val mExecutor = MyExecutor.getExecutor()
    private val installedApps: InstalledApps = InstalledApps(context, db.getAppsDao())
    private val createdGroups: CreatedGroups = CreatedGroups(db.getGroupsDao())
    private val currentScreenSessionsTracker = CurrentScreenSessionsTracker()
    private val mSessionsProxy: SessionsProxy = SessionsProxy(db.getSessionDao(), db.getScreenSessionDao(), currentScreenSessionsTracker)
    private val challengeManager: ChallengesManager = ChallengesManager(db.getChallengesDao(), mSessionsProxy, installedApps)
    private val createdChallenges: CreatedChallenges = CreatedChallenges(context, challengeManager, installedApps, createdGroups)
    private val mRunningAppManager: RunningAppManager = RunningAppManager(context)
    private val alertsManagerWrapper = AlertsManagerWrapper()
    private val settings: Settings = Settings(context)
    private val notificationManager: sau.odev.usagemonitor.appusagemanager.notifications.NotificationManager =
        sau.odev.usagemonitor.appusagemanager.notifications.NotificationManager(context, db.getNotificationsDao())
    private lateinit var runningAppsObserver: RunningAppsObserver
    private val TAG = "AppsUsageManager"

    private fun init() {
        Thread {
            Looper.prepare()
            val l = Looper.myLooper() as Looper
            l.thread.name = "UsageManager"
            val h = object : Handler(Looper.myLooper()!!) {
                override fun dispatchMessage(msg: Message) {
                    try {
                        Log.d(TAG, "dispatchMessage: ")
                        super.dispatchMessage(msg)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            installedApps.init()
            createdGroups.init()
            challengeManager.init()
            runningAppsObserver = RunningAppsObserver(context,
                    RunningAppManager(context),
                    installedApps,
                    createdGroups,
                    mSessionsProxy,
                    settings,
                    createdChallenges,
                    challengeManager,
                    alertsManagerWrapper)
            runningAppsObserver.addAppConstraint(challengeManager)
            runningAppsObserver.addAppConstraint(DefaultAppConstraint(context, settings))
            runningAppsObserver.addDeviceObserver(currentScreenSessionsTracker)
            mExecutor.handler = h
            Looper.loop()
        }.start()

    }

    //Settings
    fun getDeviceSettings(): IDeviceUsageSettings = settings

    //ChallengeManager
    fun getCreatedChallenges(): CreatedChallenges = createdChallenges

    //InstalledApps
    fun getInstalledApps(): InstalledApps = installedApps

    //Created groups
    fun getCreatedGroups(): CreatedGroups = createdGroups

    fun getSessionsDaosProxy(): SessionsProxy = mSessionsProxy

    fun isUsageStatesPermissionGranted(): Boolean = mRunningAppManager.isPermissionGranted()

    //NotificationManager
    fun getNotificationManager(): sau.odev.usagemonitor.appusagemanager.notifications.NotificationManager = notificationManager

    //RunningAppObserver Proxy
    fun startObserving() {
        mExecutor.execute {
            runningAppsObserver.startObserving()
        }
    }

    fun addAppConstraint(constraint: AppConstraint) {
        mExecutor.execute {
            runningAppsObserver.addAppConstraint(constraint)
        }
    }

    fun addAppsObserver(appsObserver: AppsObserver) {
        mExecutor.execute {
            runningAppsObserver.addAppsObserver(appsObserver)
        }
    }

    fun addUsageStatsPermissionObserver(observer: (Intent?) -> Unit) = mExecutor.execute { runningAppsObserver.addUsageStatsPermissionObserver(observer) }

    fun removeUsageStatsPermissionObserver(observer: (Intent?) -> Unit) = mExecutor.execute { runningAppsObserver.removeUsageStatsPermissionObserver(observer) }

    //AlertsWrapper
    fun setAlertManager(alertManager: IAlertsManager) {
        alertsManagerWrapper.alertManager = alertManager
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: AppsUsageManager? = null

        @JvmStatic
        fun init(ctx: Context, packageName: String) {
            PACKAGE_NAME = packageName
            IGNORED_PACKAGES.add(PACKAGE_NAME)
            IGNORED_PACKAGES.add("com.android.systemui")
            val context = ctx.applicationContext
            if (instance == null) {
                instance = AppsUsageManager(context, UsageDatabase.getInstance(context))
                instance!!.init()
            }
        }

        @Synchronized
        @JvmStatic
        fun getInstance(): AppsUsageManager {
            require(instance != null) {"Must call init(ctx: Context, packageName: String) when application starts"}
            return instance as AppsUsageManager
        }

        @Synchronized
        @JvmStatic
        fun getInstance(ctx: Context): AppsUsageManager {
            if (instance == null) {
                init(ctx, ctx.packageName)
            }
            return instance as AppsUsageManager
        }
    }
}