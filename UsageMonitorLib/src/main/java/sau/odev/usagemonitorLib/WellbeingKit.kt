package sau.odev.usagemonitorLib

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.annotation.MainThread
import sau.odev.usagemonitorLib.models.AppSessionStats
import sau.odev.usagemonitorLib.models.PackageStats
import sau.odev.usagemonitorLib.models.TimedPackageStats
import java.util.concurrent.atomic.AtomicBoolean

import sau.odev.usagemonitorLib.usage.UsageRepository
import sau.odev.usagemonitorLib.notifications.NotificationRepository
import sau.odev.usagemonitorLib.notifications.WbkTrackingForegroundService
import sau.odev.usagemonitorLib.time.TimeBucketGenerator
import sau.odev.usagemonitorLib.time.TimeWindows
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId


object WellbeingKit {

    private val initialized = AtomicBoolean(false)
    private lateinit var appContext: Context
    private val usageRepo by lazy { UsageRepository(requireInit()) }
    private val notifRepo by lazy { NotificationRepository(requireInit()) }


    /**
     * Call once from Application.onCreate().
     */
    @MainThread
    fun init(context: Context) {
        if (initialized.compareAndSet(false, true)) {
            appContext = context.applicationContext
        }
    }

    fun isInitialized(): Boolean = initialized.get()

    fun hasUsageStatsAccess(context: Context = requireInit()): Boolean =
        Permissions.hasUsageStatsAccess(context)

    fun hasNotificationAccess(context: Context = requireInit()): Boolean =
        Permissions.hasNotificationListenerAccess(context)

    fun usageAccessSettingsIntent(context: Context = requireInit()) =
        Permissions.usageAccessSettingsIntent(context)

    fun notificationAccessSettingsIntent(context: Context = requireInit()) =
        Permissions.notificationAccessSettingsIntent(context)

    suspend fun syncUsageIncremental(nowMs: Long = System.currentTimeMillis()) =
        usageRepo.syncIncremental(nowMs)

    suspend fun syncUsageRange(fromMs: Long, toMs: Long) =
        usageRepo.syncRange(fromMs, toMs)

    suspend fun getLaunchCounts(fromMs: Long, toMs: Long) =
        usageRepo.getLaunchCounts(fromMs, toMs)

    suspend fun getLaunchCount(packageName: String, fromMs: Long, toMs: Long) =
        usageRepo.getLaunchCount(packageName, fromMs, toMs)

    suspend fun getNotificationCounts(fromMs: Long, toMs: Long) =
        notifRepo.getPostedCounts(fromMs, toMs)

    suspend fun getNotificationCount(packageName: String, fromMs: Long, toMs: Long) =
        notifRepo.getPostedCount(packageName, fromMs, toMs)

    suspend fun getDailyUsage(date: LocalDate): Map<String, Long> {
        val r = TimeWindows.day(date)
        return usageRepo.getUsageDurations(r.fromMs, r.toMs)
    }

    suspend fun getDailyLaunchCounts(date: LocalDate): Map<String, Int> {
        val r = TimeWindows.day(date)
        return getLaunchCounts(r.fromMs, r.toMs)
    }


    suspend fun getDailyNotifications(date: LocalDate): Map<String, Int> {
        val r = TimeWindows.day(date)
        return notifRepo.getPostedCountsFast(r.fromMs, r.toMs)
    }

    suspend fun getPackageStats(
        packageName: String,
        fromMs: Long,
        toMs: Long
    ): PackageStats {

        val usageMs = usageRepo.getUsageDurations(fromMs, toMs)[packageName] ?: 0L
        val launches = usageRepo.getLaunchCount(packageName, fromMs, toMs)
        val notifications = notifRepo.getPostedCount(packageName, fromMs, toMs)

        return PackageStats(
            packageName = packageName,
            usageTimeMs = usageMs,
            launchCount = launches,
            notificationCount = notifications
        )
    }

    suspend fun getAllPackageStats(
        fromMs: Long,
        toMs: Long
    ): List<PackageStats> {

        val usageMap = usageRepo.getUsageDurations(fromMs, toMs)
        val launchMap = usageRepo.getLaunchCounts(fromMs, toMs)
        val notifMap = notifRepo.getPostedCountsFast(fromMs, toMs)

        // union of all seen packages
        val allPackages = HashSet<String>().apply {
            addAll(usageMap.keys)
            addAll(launchMap.keys)
            addAll(notifMap.keys)
        }

        return allPackages.map { pkg ->
            PackageStats(
                packageName = pkg,
                usageTimeMs = usageMap[pkg] ?: 0L,
                launchCount = launchMap[pkg] ?: 0,
                notificationCount = notifMap[pkg] ?: 0
            )
        }.sortedByDescending { it.usageTimeMs }
    }

    suspend fun getAppSessions(
        packageName: String,
        fromMs: Long,
        toMs: Long
    ): List<AppSessionStats> =
        usageRepo.getAppSessions(packageName, fromMs, toMs)

    suspend fun getDailyAppSessions(
        packageName: String,
        date: LocalDate
    ): List<AppSessionStats> {
        val r = TimeWindows.day(date)
        return getAppSessions(packageName, r.fromMs, r.toMs)
    }

    suspend fun getDailyPackageStats(date: LocalDate): List<PackageStats> {
        val r = TimeWindows.day(date)
        return getAllPackageStats(r.fromMs, r.toMs)
    }

    suspend fun getWeeklyPackageStats(date: LocalDate): List<PackageStats> {
        val r = TimeWindows.week(date)
        return getAllPackageStats(r.fromMs, r.toMs)
    }

    suspend fun getMonthlyPackageStats(date: LocalDate): List<PackageStats> {
        val r = TimeWindows.month(date)
        return getAllPackageStats(r.fromMs, r.toMs)
    }

    suspend fun getPackageStatsMonthly(
        packageName: String,
        from: LocalDate,
        to: LocalDate
    ): List<TimedPackageStats> {

        return TimeBucketGenerator.months(from, to).map { monthStart ->
            val r = TimeWindows.month(monthStart)

            TimedPackageStats(
                periodStart = monthStart,
                usageTimeMs =
                    usageRepo.getUsageDurations(r.fromMs, r.toMs)[packageName] ?: 0L,
                launchCount =
                    usageRepo.getLaunchCount(packageName, r.fromMs, r.toMs),
                notificationCount =
                    notifRepo.getPostedCount(packageName, r.fromMs, r.toMs)
            )
        }
    }

    suspend fun getPackageStatsWeekly(
        packageName: String,
        from: LocalDate,
        to: LocalDate
    ): List<TimedPackageStats> {

        return TimeBucketGenerator.weeks(from, to).map { weekStart ->
            val r = TimeWindows.week(weekStart)

            TimedPackageStats(
                periodStart = weekStart,
                usageTimeMs =
                    usageRepo.getUsageDurations(r.fromMs, r.toMs)[packageName] ?: 0L,
                launchCount =
                    usageRepo.getLaunchCount(packageName, r.fromMs, r.toMs),
                notificationCount =
                    notifRepo.getPostedCount(packageName, r.fromMs, r.toMs)
            )
        }
    }

    suspend fun getPackageStatsDaily(
        packageName: String,
        from: LocalDate,
        to: LocalDate
    ): List<TimedPackageStats> {

        return TimeBucketGenerator.days(from, to).map { day ->
            val r = TimeWindows.day(day)

            TimedPackageStats(
                periodStart = day,
                usageTimeMs =
                    usageRepo.getUsageDurations(r.fromMs, r.toMs)[packageName] ?: 0L,
                launchCount =
                    usageRepo.getLaunchCount(packageName, r.fromMs, r.toMs),
                notificationCount =
                    notifRepo.getPostedCount(packageName, r.fromMs, r.toMs)
            )
        }
    }

    /**
     * @return the oldest usage event timestamp currently stored in WellbeingKit's local DB,
     * or null if no usage events have been stored yet.
     */
    suspend fun getOldestUsageTimestampMsOrNull(): Long? =
        usageRepo.getOldestUsageTimestampMsOrNull()

    /**
     * @return the oldest LocalDate for which WellbeingKit has any stored usage events,
     * or null if the DB is empty.
     */
    suspend fun getOldestUsageDateOrNull(zoneId: ZoneId = ZoneId.systemDefault()): LocalDate? {
        val ts = getOldestUsageTimestampMsOrNull() ?: return null
        return Instant.ofEpochMilli(ts).atZone(zoneId).toLocalDate()
    }

    private fun requireInit(): Context {
        check(isInitialized()) { "WellbeingKit.init(context) must be called first." }
        return appContext
    }

    object TrackingUi {
        fun startForegroundIndicator(context: Context) {
            val i = Intent(context, WbkTrackingForegroundService::class.java)
            ContextCompat.startForegroundService(context, i)
        }

        fun stopForegroundIndicator(context: Context) {
            val i = Intent(context, WbkTrackingForegroundService::class.java)
            context.stopService(i)
        }
    }
}