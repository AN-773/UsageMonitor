package sau.odev.usagemonitorLib.time

import java.time.DayOfWeek
import java.time.LocalDate

internal object TimeBucketGenerator {

    fun days(from: LocalDate, to: LocalDate): List<LocalDate> =
        generateSequence(from) { it.plusDays(1) }
            .takeWhile { !it.isAfter(to) }
            .toList()

    fun weeks(from: LocalDate, to: LocalDate): List<LocalDate> =
        generateSequence(from.with(DayOfWeek.MONDAY)) { it.plusWeeks(1) }
            .takeWhile { !it.isAfter(to) }
            .toList()

    fun months(from: LocalDate, to: LocalDate): List<LocalDate> =
        generateSequence(from.withDayOfMonth(1)) { it.plusMonths(1) }
            .takeWhile { !it.isAfter(to) }
            .toList()
}