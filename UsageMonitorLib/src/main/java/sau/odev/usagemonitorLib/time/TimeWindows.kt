package sau.odev.usagemonitorLib.time

import java.time.*
import java.time.temporal.TemporalAdjusters

object TimeWindows {

    fun day(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): TimeRange {
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return TimeRange(start, end)
    }

    fun week(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): TimeRange {
        val startDate = date.with(DayOfWeek.MONDAY)
        val start = startDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = startDate.plusWeeks(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return TimeRange(start, end)
    }

    fun month(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): TimeRange {
        val startDate = date.withDayOfMonth(1)
        val start = startDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = startDate.plusMonths(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return TimeRange(start, end)
    }
}

data class TimeRange(
    val fromMs: Long,
    val toMs: Long
)