package sau.odev.usagemonitor.appusagemanager.usagedirect;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.usage.UsageEvents;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.*;
import java.util.*;
import java.util.function.BiConsumer;

import sau.odev.usagemonitor.appusagemanager.usagestats.SimpleUsageStat;

/**
 * Wrapper class for <code>queryEvents(…)</code> calls to the UsageStatsManager class
 */
@SuppressLint("NewApi")

public class EventLogWrapper extends UsageStatsManagerWrapper {

    private final UnmatchedCloseEventGuardian guardian = new UnmatchedCloseEventGuardian(usageStatsManager);

    public EventLogWrapper(Context context) {
        super(context);

        SharedPreferences sharedPreferences = context.getSharedPreferences("timezone", Context.MODE_PRIVATE);

        // Migration to remove timezone from shared preferences
        if (sharedPreferences.contains("timezone")) {
            sharedPreferences
                    .edit()
                    .remove("timezone")
                    .apply();
        }

    }

    /**
     * Collects event information from system to calculate and aggregate precise
     * foreground time statistics for the specified period.
     *
     * Comments refer to the cases from
     * <a href="https://codeberg.org/fynngodau/usageDirect/wiki/Event-log-wrapper-scenarios">
     *     the documentation.</a>
     *
     * @param start First point in time to include in results
     * @param end   Last point in time to include in results
     * @return A list of foreground stats for the specified period
     */
    public List<ComponentForegroundStat> getForegroundStatsByTimestamps(long start, long end) {

        /*
         * Because sometimes, open events do not have close events when they should, as a hack / workaround,
         * we query the apps currently in the foreground and match them against the apps that are currently
         * in the foreground if the query start date is very recent or in the future. Thus, we are using this
         * to tell apart True from Faulty unmatched open events.
         *
         * We query processes in the beginning of this method call in case querying the event log takes a
         * little longer.
         */
        List<String> foregroundProcesses = new ArrayList<>();
        if (end >= System.currentTimeMillis() - 1500) {

            // Get foreground tasks
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
            for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
                if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND || appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE) {
                    foregroundProcesses.add(appProcess.processName);
                }
            }
        }


        // Assumption: events are ordered chronologically
        UsageEvents events = usageStatsManager.queryEvents(start, end);

        /* …except that sometimes, the events that are close to each other are swapped in a way that
         * breaks the assumption that all end times which do not have a matching start time have
         * started before start. We handle those as Duplicate close event and Duplicate open event.
         * Therefore, we keep null entries in our moveToForegroundMap instead of removing the entries
         * to prevent apps that had been opened previously in a period from being counted as "opened
         * before start" (as they are not a True unmatched close event).
        */

        // Map components to the last moveToForeground event
        Map<AppClass, Long> moveToForegroundMap = new HashMap<>();

        // Collect timespans during which components are in foreground
        ArrayList<ComponentForegroundStat> componentForegroundStats = new ArrayList<>();

        // Iterate over events
        UsageEvents.Event event = new UsageEvents.Event();
        AppClass appClass;

        while (events.hasNextEvent()) {
            events.getNextEvent(event);
            appClass = new AppClass(event.getPackageName(), event.getClassName());

            switch (event.getEventType()) {
                /*
                 * "An event type denoting that an android.app.Activity moved to the foreground."
                 * (old definition: "An event type denoting that a component moved to the foreground.")
                 */
                case UsageEvents.Event.ACTIVITY_RESUMED:
                /*
                 * public static final int android.app.usage.UsageEvents.Event.CONTINUE_PREVIOUS_DAY = 4;
                 * (annotated as @hide)
                 * "An event type denoting that a component was in the foreground the previous day.
                 * This is effectively treated as a MOVE_TO_FOREGROUND."
                 */
                case 4:
                    // Store open timestamp in map, overwriting earlier timestamps in case of Duplicate open event
                    moveToForegroundMap.put(appClass, event.getTimeStamp());

                    break;
                /*
                 * "An event type denoting that an android.app.Activity moved to the background."
                 * (old definition: "An event type denoting that a component moved to the background.")
                 */
                case UsageEvents.Event.ACTIVITY_PAUSED:
                /*
                 * "An activity becomes invisible on the UI, corresponding to Activity.onStop()
                 * of the activity's lifecycle."
                 */
                case UsageEvents.Event.ACTIVITY_STOPPED:
                /*
                 * public static final int android.app.usage.UsageEvents.Event.END_OF_DAY = 3;
                 * (annotated as @hide)
                 * "An event type denoting that a component was in the foreground when the stats
                 * rolled-over. This is effectively treated as a {@link #MOVE_TO_BACKGROUND}."
                 */
                case 3:
                    Long eventBeginTime = moveToForegroundMap.get(appClass);
                    if (eventBeginTime != null) {
                        // Open and close events in order
                        moveToForegroundMap.put(appClass, null);
                    } else if (
                            // App has not been in this query yet (test for Duplicate close event)
                            moveToForegroundMap.keySet().stream()
                                    .noneMatch(key -> event.getPackageName().equals(key.packageName)) &&
                            /*
                             * Test if this unmatched close event is True by asking the Guardian
                             * to scan for it
                             */
                            guardian.test(event, start)
                    ) {
                        // Identified as True unmatched close event
                        // Take start as a starting timestamp
                        eventBeginTime = start;
                    } else break; // Ignore Faulty unmatched close event

                    // Check if another of the app's components have moved to the foreground in the meantime
                    OptionalLong endTime =
                            moveToForegroundMap.entrySet().stream()
                                    .filter(entry -> event.getPackageName().equals(entry.getKey().packageName))
                                    .filter(entry -> entry.getValue() != null)
                                    .mapToLong(entry -> entry.getValue())
                                    .min();

                    componentForegroundStats.add(new ComponentForegroundStat(
                            eventBeginTime,
                            endTime.orElse(event.getTimeStamp()),
                            event.getPackageName(),
                            1
                    ));
                    break;
                /*
                 * "An event type denoting that the Android runtime underwent a shutdown process. A
                 * DEVICE_SHUTDOWN event should be treated as if all started activities and
                 * foreground services are now stopped and no explicit ACTIVITY_STOPPED and
                 * FOREGROUND_SERVICE_STOP events will be generated for them.
                 * [… A]ny open events without matching close events between DEVICE_SHUTDOWN and
                 * DEVICE_STARTUP should be ignored because the closing time is unknown."
                 */
                case UsageEvents.Event.DEVICE_SHUTDOWN:
                    // Per docs: iterate over remaining start events and treat them as closed
                    for (AppClass key : moveToForegroundMap.keySet()) {

                        if (moveToForegroundMap.get(key) == null) {
                            // Not a remaining start event
                            continue;
                        }

                        componentForegroundStats.add(new ComponentForegroundStat(
                                moveToForegroundMap.get(key),
                                event.getTimeStamp(),
                                key.packageName,
                                1
                        ));

                        // Set entire app to closed
                        moveToForegroundMap.keySet().stream()
                                .filter(key1 -> key.packageName.equals(key1.packageName))
                                .forEach(samePackageKey -> moveToForegroundMap.put(samePackageKey, null));
                    }
                    break;
                /*
                 * "An event type denoting that the Android runtime started up. This could be after
                 * a shutdown or a runtime restart. Any open events without matching close events
                 * between DEVICE_SHUTDOWN and DEVICE_STARTUP should be ignored because the
                 * closing time is unknown."
                 */
                case UsageEvents.Event.DEVICE_STARTUP:
                    // Per docs: remove pending open events
                    for (AppClass key : moveToForegroundMap.keySet()) {
                        // Overwrite all times with null
                        moveToForegroundMap.put(key, null);
                    }

                    /* No package could be open longer than a reboot. Thus, we set the `start`
                     * timestamp to the boot event's timestamp in case we later assume that a
                     * package has been open "since the start of the period". It is not logical
                     * that this would happen but we can never know with this API.
                     */
                    start = event.getTimeStamp();
                    break;

            }
        }

        // Iterate over remaining start events
        for (AppClass key : moveToForegroundMap.keySet()) {

            if (moveToForegroundMap.get(key) == null) {
                // Not a remaining start event
                continue;
            }

            // Test if foreground app
            for (String foregroundProcess : foregroundProcesses) {
                if (foregroundProcess.contains(key.packageName)) {

                    // Is a foreground app (True unmatched open event)
                    componentForegroundStats.add(new ComponentForegroundStat(
                            moveToForegroundMap.get(key),
                            Math.min(System.currentTimeMillis(), end),
                            key.packageName,
                            1
                    ));

                    break;
                }
            }

            // If app is not in foreground, drop event
            // Assume Faulty unmatched open event
        }

        /* If nothing happened during the timespan but there is an app in the foreground,
         * then this app was used the whole period time and there was No event for it.
         * Because the foreground applications API call is documented as not to be used
         * for purposes like this, we first query whether the process name is a valid
         * package name and if not, we drop it.
         */
        if (moveToForegroundMap.keySet().size() == 0) {
            PackageManager packageManager = context.getPackageManager();
            for (String foregroundProcess : foregroundProcesses) {
                if (packageManager.getLaunchIntentForPackage(foregroundProcess) != null) {
                    componentForegroundStats.add(
                            new ComponentForegroundStat(
                                    start,
                                    Math.min(System.currentTimeMillis(), end),
                                    foregroundProcess,
                                    1
                            )
                    );
                    Log.d("EventLogWrapper", "Assuming that application " + foregroundProcess + " has been used " +
                            "the whole query time");
                }
            }
        }


        return componentForegroundStats;
    }

    /**
     * Collects event information from system to calculate and aggregate precise
     * foreground time statistics for the specified relative day.
     *
     * @param offset Day to query back in time relative to today
     */
    public List<ComponentForegroundStat> getForegroundStatsByRelativeDay(int offset) {

        // Calculate timespan to query
        LocalDate queryDay = LocalDate.now()
                .minusDays(offset);

        long beginTime = queryDay
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();

        long endTime = queryDay
                .plusDays(1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();

        return getForegroundStatsByTimestamps(beginTime, endTime);
    }

    /**
     * Collects event information from system to calculate and aggregate precise
     * foreground time statistics starting at <code>start</code> and ending at
     * the end of the day that contains <code>start</code>.
     *
     * @param start Starting time of query and point in time in day to query
     */
    public List<ComponentForegroundStat> getForegroundStatsByPartialDay(long start) {
        ZoneId zone = ZoneId.systemDefault();
        long endTime = Instant.ofEpochMilli(start)
                .atZone(zone)
                .toLocalDate() // remove time (and zone) information
                .plusDays(1) // go one day ahead
                .atStartOfDay(zone)
                .toInstant()
                .toEpochMilli();

        return getForegroundStatsByTimestamps(start, endTime);
    }

    /**
     * Takes a list of foreground stats and aggregates them to usage stats.
     * <p>Assumes all provided usage stats to be on the same day.</p>
     */
    public List<SimpleUsageStat> aggregateForegroundStats(List<ComponentForegroundStat> foregroundStats) {
        return aggregateForegroundStats(foregroundStats, null);
    }

    /**
     * Takes a list of foreground stats and aggregates them to usage stats.
     * <p>Assumes all provided usage stats to be on the same day.</p>
     *
     * @param endConsumer Consumer that accepts ending times of component
     *                    foreground stats with their package name
     */
    public List<SimpleUsageStat> aggregateForegroundStats(List<ComponentForegroundStat> foregroundStats, @Nullable BiConsumer<String, Long> endConsumer) {

        List<SimpleUsageStat> usageStats = new ArrayList<>();

        if (foregroundStats.size() == 0) {
            return usageStats;
        }

        Map<String, Long> applicationTotalForegroundTime = new HashMap<>();

        for (ComponentForegroundStat foregroundStat : foregroundStats) {
            if (applicationTotalForegroundTime.containsKey(foregroundStat.packageName)) {

                long newTotal = applicationTotalForegroundTime.get(foregroundStat.packageName)
                        + (foregroundStat.endTime - foregroundStat.beginTime);

                applicationTotalForegroundTime.put(foregroundStat.packageName, newTotal);

            } else {

                applicationTotalForegroundTime.put(foregroundStat.packageName,
                        (foregroundStat.endTime - foregroundStat.beginTime)
                );

            }

            if (endConsumer != null) {
                endConsumer.accept(foregroundStat.packageName, foregroundStat.endTime);
            }
        }

        long day = Instant.ofEpochMilli(foregroundStats.get(0).beginTime)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .toEpochDay();

        for (String application : applicationTotalForegroundTime.keySet()) {
            usageStats.add(
                    new SimpleUsageStat(day, applicationTotalForegroundTime.get(application), application)
            );
        }

        return usageStats;

    }

    /**
     * Collects <b>all</b> event information from system to calculate and aggregate precise
     * foreground time statistics for the provided day and presents this information as
     * {@link SimpleUsageStat}s.
     *
     * @param day Day since epoch
     */
    private List<ComponentForegroundStat> getForegroundStatsByDay(long day) {
        LocalDate date = LocalDate.ofEpochDay(day);
        long start = date.atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli();
        long end = date.plusDays(1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli();

        return getForegroundStatsByTimestamps(start, end);
    }

    /**
     * Collects <b>all</b> event information from system to calculate and aggregate precise
     * foreground time statistics and presents this information as {@link SimpleUsageStat}s.
     * <p><b>This method call causes lag</b> if called with a low since value.
     *
     * @param daySince Return data from this day on
     * @param endConsumer Consumer that accepts ending times of component
     *                    foreground stats with their package name
     */
    public List<SimpleUsageStat> getAllSimpleUsageStats(long daySince, BiConsumer<String, Long> endConsumer) {
        List<SimpleUsageStat> usageStats = new ArrayList<>();
        List<ComponentForegroundStat> foregroundStats;

        long today = LocalDate.now().toEpochDay();

        // Maximum event log size
        daySince = Math.max(today - 10, daySince);

        while (daySince <= today) {
            foregroundStats = getForegroundStatsByDay(daySince);

            usageStats.addAll(
                    aggregateForegroundStats(foregroundStats, endConsumer)
            );

            daySince++;
        }

        return usageStats;
    }

    /**
     * Returns only usage statistics that have not been counted yet for
     * only the day that contains <code>timestamp</code>
     *
     * @param endConsumer Consumer that accepts ending times of component
     *                    foreground stats with their package name
     */
    public List<SimpleUsageStat> getIncrementalSimpleUsageStats(long timestamp, BiConsumer<String, Long> endConsumer) {

        List<ComponentForegroundStat> foregroundStats = getForegroundStatsByPartialDay(timestamp);
        return aggregateForegroundStats(foregroundStats, endConsumer);
    }

    /**
     * Stores a class name and its corresponding package.
     */
    private class AppClass {
        public @NonNull String packageName;
        public @NonNull String className;

        public AppClass(@NonNull String packageName, @NonNull String className) {
            this.packageName = packageName;
            this.className = className;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            AppClass appClass = (AppClass) o;

            if (!packageName.equals(appClass.packageName)) return false;
            return className.equals(appClass.className);
        }

        @Override
        public int hashCode() {
            int result = packageName.hashCode();
            result = 31 * result + className.hashCode();
            return result;
        }
    }
}
