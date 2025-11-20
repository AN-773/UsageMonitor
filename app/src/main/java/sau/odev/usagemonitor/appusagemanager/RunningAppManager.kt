package sau.odev.usagemonitor.appusagemanager

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings

class RunningAppManager(private val context: Context) {

    @SuppressLint("WrongConstant")
    private val runningAppManager: Any = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
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
                    usageStatsManager.queryEvents((System.currentTimeMillis() - 3_600_000L), System.currentTimeMillis()) ?: return null

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

}