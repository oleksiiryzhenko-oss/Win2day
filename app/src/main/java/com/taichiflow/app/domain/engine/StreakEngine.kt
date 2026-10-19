package com.taichiflow.app.domain.engine

import com.taichiflow.app.domain.model.HistoryRecord
import com.taichiflow.app.domain.model.StreakInfo
import com.taichiflow.app.domain.model.WeekDayStatus
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

object StreakEngine {
    /** A day counts toward the streak when it contains at least one activity of 30 s or more. */
    const val QUALIFYING_SEC = 30

    fun qualifies(record: HistoryRecord): Boolean = record.durationSec >= QUALIFYING_SEC

    fun days(history: List<HistoryRecord>): Set<LocalDate> =
        history.filter { qualifies(it) }.map { it.date }.toSet()

    fun current(days: Set<LocalDate>, today: LocalDate): Int {
        var cursor = when {
            today in days -> today
            today.minusDays(1) in days -> today.minusDays(1)
            else -> return 0
        }
        var count = 0
        while (cursor in days) {
            count++
            cursor = cursor.minusDays(1)
        }
        return count
    }

    fun longest(days: Set<LocalDate>): Int {
        if (days.isEmpty()) return 0
        val sorted = days.sorted()
        var best = 1
        var run = 1
        for (i in 1 until sorted.size) {
            run = if (sorted[i - 1].plusDays(1) == sorted[i]) run + 1 else 1
            if (run > best) best = run
        }
        return best
    }

    /** Last day of the longest streak (used as the "achieved" date for the record). */
    fun longestEnd(days: Set<LocalDate>): LocalDate? {
        if (days.isEmpty()) return null
        val sorted = days.sorted()
        var best = 1
        var run = 1
        var end = sorted.first()
        for (i in 1 until sorted.size) {
            run = if (sorted[i - 1].plusDays(1) == sorted[i]) run + 1 else 1
            if (run >= best) {
                best = run
                end = sorted[i]
            }
        }
        return end
    }

    fun weekStart(date: LocalDate): LocalDate = date.minusDays((date.dayOfWeek.value - 1).toLong())

    fun info(history: List<HistoryRecord>, today: LocalDate): StreakInfo {
        val d = days(history)
        val start = weekStart(today)
        val week = (0L until 7L).map { offset ->
            val day = start.plusDays(offset)
            WeekDayStatus(
                label = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US).uppercase(Locale.US),
                done = day in d,
                isToday = day == today,
                isFuture = day.isAfter(today)
            )
        }
        return StreakInfo(
            current = current(d, today),
            longest = longest(d),
            week = week,
            practicedToday = today in d
        )
    }

    fun dayLabel(day: DayOfWeek): String = day.getDisplayName(TextStyle.SHORT, Locale.US).uppercase(Locale.US)
}
