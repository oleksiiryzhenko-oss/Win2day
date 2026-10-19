package com.taichiflow.app.domain.engine

import com.taichiflow.app.domain.model.AchievementDef
import com.taichiflow.app.domain.model.AchievementState
import com.taichiflow.app.domain.model.AchievementStats
import com.taichiflow.app.domain.model.ActivityType
import com.taichiflow.app.domain.model.Attribute
import com.taichiflow.app.domain.model.HistoryRecord
import com.taichiflow.app.domain.model.PersonalRecord
import com.taichiflow.app.domain.model.WeeklyGoal
import com.taichiflow.app.domain.model.WeeklyGoalProgress
import com.taichiflow.app.domain.model.WeeklyKind
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

object AchievementEngine {

    fun buildStats(
        history: List<HistoryRecord>,
        attributes: Map<Attribute, Int>,
        flowScore: Int,
        level: Int,
        distinctMovements: Int,
        customCount: Int,
        dailyCount: Int,
        weeklyClaims: Int
    ): AchievementStats {
        val zone = ZoneId.systemDefault()
        val practices = history.filter { it.type == ActivityType.PRACTICE }
        return AchievementStats(
            practices = practices.size,
            totalSessions = history.size,
            attributes = attributes,
            flowScore = flowScore,
            longestStreak = StreakEngine.longest(StreakEngine.days(history)),
            totalMinutes = history.sumOf { it.durationSec } / 60,
            distinctMovements = distinctMovements,
            level = level,
            bestBalance = history.filter { it.type == ActivityType.BALANCE }.maxOfOrNull { it.score } ?: 0,
            bestRhythm = history.filter { it.type == ActivityType.RHYTHM }.maxOfOrNull { it.score } ?: 0,
            breathingCount = history.count { it.type == ActivityType.BREATHING },
            flowCount = history.count { it.type == ActivityType.FLOW },
            masterCount = history.count { it.type == ActivityType.MASTER },
            bestMaster = history.filter { it.type == ActivityType.MASTER }.maxOfOrNull { it.score } ?: 0,
            customCount = customCount,
            dailyCount = dailyCount,
            weeklyClaims = weeklyClaims,
            categoriesPracticed = practices.mapNotNull { it.category }.distinct().size,
            earlyBird = history.any { Instant.ofEpochMilli(it.timestamp).atZone(zone).hour < 8 },
            longestSessionMin = (history.maxOfOrNull { it.durationSec } ?: 0) / 60,
            recordsCount = history.count { it.isRecord }
        )
    }

    fun states(defs: List<AchievementDef>, stats: AchievementStats, unlocked: Map<String, Long>): List<AchievementState> =
        defs.map { d ->
            AchievementState(d, unlocked[d.id], if (unlocked.containsKey(d.id)) 1f else d.progress(stats).coerceIn(0f, 1f))
        }

    fun newlyUnlocked(defs: List<AchievementDef>, stats: AchievementStats, unlockedIds: Set<String>): List<AchievementDef> =
        defs.filter { it.id !in unlockedIds && it.progress(stats) >= 1f }
}

object GoalEngine {
    fun weekKey(date: LocalDate): Long = StreakEngine.weekStart(date).toEpochDay()

    fun progress(
        goals: List<WeeklyGoal>,
        history: List<HistoryRecord>,
        dailyDaysThisWeek: Int,
        claimedIds: Set<String>,
        today: LocalDate
    ): List<WeeklyGoalProgress> {
        val start = StreakEngine.weekStart(today)
        val week = history.filter { !it.date.isBefore(start) && !it.date.isAfter(today) }
        return goals.map { g ->
            val current = when (g.kind) {
                WeeklyKind.PRACTICES -> week.count { it.type == ActivityType.PRACTICE }
                WeeklyKind.BALANCE_CHALLENGES -> week.count { it.type == ActivityType.BALANCE }
                WeeklyKind.RHYTHM_CHALLENGES -> week.count { it.type == ActivityType.RHYTHM }
                WeeklyKind.BREATHING_CHALLENGES -> week.count { it.type == ActivityType.BREATHING }
                WeeklyKind.FLOW_CHALLENGES -> week.count { it.type == ActivityType.FLOW || it.type == ActivityType.MASTER }
                WeeklyKind.MINUTES -> week.sumOf { it.durationSec } / 60
                WeeklyKind.CATEGORIES -> week.mapNotNull { it.category }.distinct().size
                WeeklyKind.RECORDS -> week.count { it.isRecord }
                WeeklyKind.DAILY -> dailyDaysThisWeek
                WeeklyKind.ACTIVE_DAYS -> week.filter { StreakEngine.qualifies(it) }.map { it.date }.distinct().size
                WeeklyKind.HIGH_SCORES -> week.count { it.score >= 85 }
                WeeklyKind.CHALLENGES_ANY -> week.count { it.type != ActivityType.PRACTICE }
            }
            WeeklyGoalProgress(g, current.coerceAtMost(g.target), g.id in claimedIds)
        }
    }
}

object RecordsEngine {
    const val BEST_BALANCE = "Best Balance"
    const val BEST_RHYTHM = "Best Rhythm"
    const val BEST_BREATHING = "Best Breathing"
    const val BEST_FLOW = "Best Flow"
    const val BEST_MASTER = "Best Master Flow"
    const val LONGEST_SESSION = "Longest Session"
    const val LONGEST_STREAK = "Longest Streak"

    private val flowTypes = setOf(ActivityType.PRACTICE, ActivityType.FLOW, ActivityType.MASTER)

    private fun scoreLabel(type: ActivityType): List<String> = when (type) {
        ActivityType.BALANCE -> listOf(BEST_BALANCE)
        ActivityType.RHYTHM -> listOf(BEST_RHYTHM)
        ActivityType.BREATHING -> listOf(BEST_BREATHING)
        ActivityType.MASTER -> listOf(BEST_MASTER, BEST_FLOW)
        ActivityType.FLOW, ActivityType.PRACTICE -> listOf(BEST_FLOW)
    }

    /** Which records a new result would break, given the previous history. */
    fun newRecords(type: ActivityType, score: Int, durationSec: Int, previous: List<HistoryRecord>): List<String> {
        val labels = mutableListOf<String>()
        for (label in scoreLabel(type)) {
            val prevBest = previous.filter { typeMatches(label, it.type) }.maxOfOrNull { it.score }
            if (prevBest != null && score > prevBest) labels += label
        }
        val prevLongest = previous.maxOfOrNull { it.durationSec }
        if (prevLongest != null && durationSec > prevLongest && durationSec >= 300) labels += LONGEST_SESSION
        return labels
    }

    private fun typeMatches(label: String, type: ActivityType): Boolean = when (label) {
        BEST_BALANCE -> type == ActivityType.BALANCE
        BEST_RHYTHM -> type == ActivityType.RHYTHM
        BEST_BREATHING -> type == ActivityType.BREATHING
        BEST_MASTER -> type == ActivityType.MASTER
        BEST_FLOW -> type in flowTypes
        else -> false
    }

    fun records(history: List<HistoryRecord>): List<PersonalRecord> {
        fun best(label: String): PersonalRecord {
            val r = history.filter { typeMatches(label, it.type) }.maxByOrNull { it.score }
            return PersonalRecord(label, r?.score?.toString() ?: "—", r?.timestamp)
        }
        val longest = history.maxByOrNull { it.durationSec }
        val days = StreakEngine.days(history)
        val streak = StreakEngine.longest(days)
        val streakEnd = StreakEngine.longestEnd(days)
        val total = history.sumOf { it.durationSec }
        return listOf(
            best(BEST_BALANCE),
            best(BEST_RHYTHM),
            best(BEST_FLOW),
            best(BEST_BREATHING),
            best(BEST_MASTER),
            PersonalRecord(LONGEST_SESSION, longest?.let { "${it.durationSec / 60} min" } ?: "—", longest?.timestamp),
            PersonalRecord(
                LONGEST_STREAK,
                if (streak > 0) "$streak days" else "—",
                streakEnd?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
            ),
            PersonalRecord("Total Practice", formatHours(total), history.maxOfOrNull { it.timestamp })
        )
    }

    fun formatHours(totalSec: Int): String {
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        return if (h > 0) String.format(Locale.US, "%dh %02dm", h, m) else "${m}m"
    }
}
