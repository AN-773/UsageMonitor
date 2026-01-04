package sau.odev.usagemonitor.appusagemanager.usagedirect;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * Used for displaying clock pie charts, not for precise usage time calculations
 *
 * @see #EventLogWrapper
 */
public class HarmonizedEventLogWrapper extends EventLogWrapper {

    /**
     * Events that are closer than 30s should be merged
     */
    private static final int HARMONY_INTERVAL = 30 * 1000;

    public HarmonizedEventLogWrapper(Context context) {
        super(context);
    }

    /**
     * @return A harmonized version of the result of parent's method
     */
    @Override
    public List<ComponentForegroundStat> getForegroundStatsByTimestamps(long start, long end) {
        List<ComponentForegroundStat> eventList = super.getForegroundStatsByTimestamps(start, end);

        if (eventList.size() <= 1) return eventList;

        // Harmonize

        List<ComponentForegroundStat> harmonizedList = new ArrayList<>();

        ComponentForegroundStat pendingEvent = eventList.get(0);
        for (int i = 1; i < eventList.size(); i++) {

            ComponentForegroundStat currentEvent = eventList.get(i);

            // Merge equal package name events if less than `HARMONY_INTERVAL` between usages
            if (currentEvent.packageName.equals(pendingEvent.packageName)
                    && Math.abs(currentEvent.beginTime - pendingEvent.endTime) < HARMONY_INTERVAL
            ) {
                // Merge current with pending event
                pendingEvent = new ComponentForegroundStat(
                        pendingEvent.beginTime, currentEvent.endTime, currentEvent.packageName, 1
                );
            } else {

                // Harmonize start time if less than `HARMONY_INTERVAL` between usages
                if (Math.abs(currentEvent.beginTime - pendingEvent.endTime) < HARMONY_INTERVAL) {
                    // Harmonize end and start times
                    currentEvent = new ComponentForegroundStat(
                            pendingEvent.endTime, currentEvent.endTime, currentEvent.packageName, 1
                    );
                }

                // Commit pending event
                harmonizedList.add(pendingEvent);
                // Current event is new pending event
                pendingEvent = currentEvent;
            }
        }

        // Commit last pending event
        harmonizedList.add(pendingEvent);

        return harmonizedList;
    }
}
