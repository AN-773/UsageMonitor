package sau.odev.usagemonitor.appusagemanager

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log

data class AppUsageInfo(
    val packageName: String,
    val totalTimeInForeground: Long,
    val lastTimeUsed: Long,
    val firstTimeStamp: Long,
    val lastTimeStamp: Long,
    val launchCount: Int
)

class RunningAppManager(private val context: Context) {

    @SuppressLint("WrongConstant")
    private val runningAppManager: Any =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            context.getSystemService("usagestats") as UsageStatsManager
        } else {
            context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        }

    fun requestPermissionIntent(): Intent? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        }
        return null
    }

    fun isPermissionGranted(): Boolean {
        //https://stackoverflow.com/a/42390614
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), context.packageName
            )

            return if (mode == AppOpsManager.MODE_DEFAULT) {
                context.checkCallingOrSelfPermission("android.permission.PACKAGE_USAGE_STATS") == PackageManager.PERMISSION_GRANTED
            } else {
                mode == AppOpsManager.MODE_ALLOWED
            }
        }
        return true
    }

    fun getRunningApp(): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val usageStatsManager = runningAppManager as UsageStatsManager
            val usageEvents: UsageEvents =
                usageStatsManager.queryEvents(
                    (System.currentTimeMillis() - 3_600_000L),
                    System.currentTimeMillis()
                ) ?: return null

            var pkg: String? = null
            while (usageEvents.hasNextEvent()) {
                val event = UsageEvents.Event()
                usageEvents.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND)
                    pkg = event.packageName
            }
            return pkg
        } else {
            return try {
                val activityManager = runningAppManager as ActivityManager
                activityManager.getRunningTasks(1)[0].topActivity?.packageName
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * Get app usage statistics for a time range
     * @param startTime Start time in milliseconds
     * @param endTime End time in milliseconds
     * @param interval Interval type (e.g. UsageStatsManager.INTERVAL_DAILY)
     * @return Map of package name to AppUsageInfo
     */
    fun getAppsUsageStats(
        startTime: Long,
        endTime: Long,
        interval: Int = UsageStatsManager.INTERVAL_BEST
    ): Map<String, AppUsageInfo> {
        val result = mutableMapOf<String, AppUsageInfo>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                val usageStatsManager = runningAppManager as UsageStatsManager
                val usageStatsList = usageStatsManager.queryUsageStats(
                    interval,
                    startTime,
                    endTime
                )
                val whatsappUsage = usageStatsList?.filter { it.packageName == "com.whatsapp" }

                usageStatsList?.forEach { usageStats ->
                    if (usageStats.totalTimeInForeground > 0) {
                        // TODO We are using reflection to access hidden API getAppLaunchCount()
                        // This may not work on all devices and Android versions
                        var appLaunchCount = 0
                        try {
                            val klass = UsageStats::class.java
                            val method = klass.getDeclaredMethod("getAppLaunchCount")
                            method.isAccessible = true
                            appLaunchCount = method.invoke(usageStats) as Int
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        Log.d(
                            "usageStatsList",
                            "getAppsUsageStats: ${usageStats.packageName} launched $appLaunchCount times"
                        )
                        var oldValue = result[usageStats.packageName]
                        if (oldValue != null) {
                            val newValue = oldValue.copy(
                                totalTimeInForeground = maxOf(oldValue.totalTimeInForeground, usageStats.totalTimeVisible),
                                lastTimeUsed = maxOf(oldValue.lastTimeUsed, usageStats.lastTimeUsed),
                                firstTimeStamp = minOf(oldValue.firstTimeStamp, usageStats.firstTimeStamp),
                                lastTimeStamp = maxOf(oldValue.lastTimeStamp, usageStats.lastTimeStamp),
                                launchCount = oldValue.launchCount + appLaunchCount
                            )
                            result[usageStats.packageName] = newValue
                        } else {
                            result[usageStats.packageName] = AppUsageInfo(
                                packageName = usageStats.packageName,
                                totalTimeInForeground = usageStats.totalTimeVisible,
                                lastTimeUsed = usageStats.lastTimeUsed,
                                firstTimeStamp = usageStats.firstTimeStamp,
                                lastTimeStamp = usageStats.lastTimeStamp,
                                launchCount = appLaunchCount
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return result
    }

    /**
     * Get usage for a specific app in a time range
     * @param packageName Package name of the app
     * @param startTime Start time in milliseconds
     * @param endTime End time in milliseconds
     * @return Total foreground time in milliseconds, or 0 if not available
     */
    fun getAppUsageTime(packageName: String, startTime: Long, endTime: Long): Long {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                val usageStatsManager = runningAppManager as UsageStatsManager
                val usageStatsList = usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY,
                    startTime,
                    endTime
                )

                usageStatsList?.find { it.packageName == packageName }?.let {
                    return it.totalTimeInForeground
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return 0L
    }

    /**
     * Get usage events for detailed session tracking
     * @param startTime Start time in milliseconds
     * @param endTime End time in milliseconds
     * @return List of usage events
     */
    fun getUsageEvents(startTime: Long, endTime: Long): List<UsageEventInfo> {
        val events = mutableListOf<UsageEventInfo>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                val usageStatsManager = runningAppManager as UsageStatsManager
                val usageEvents = usageStatsManager.queryEvents(startTime, endTime)

                while (usageEvents.hasNextEvent()) {
                    val event = UsageEvents.Event()
                    usageEvents.getNextEvent(event)

                    if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                        event.eventType == UsageEvents.Event.MOVE_TO_BACKGROUND
                    ) {
                        events.add(
                            UsageEventInfo(
                                packageName = event.packageName,
                                timestamp = event.timeStamp,
                                eventType = event.eventType
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return events
    }
}

data class UsageEventInfo(
    val packageName: String,
    val timestamp: Long,
    val eventType: Int
)
