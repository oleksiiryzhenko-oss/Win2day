package com.taichiflow.app.domain.engine

import com.taichiflow.app.domain.model.Attribute
import com.taichiflow.app.domain.model.Difficulty
import com.taichiflow.app.domain.model.HistoryRecord
import com.taichiflow.app.domain.model.LevelInfo
import kotlin.math.pow
import kotlin.math.roundToInt

object ProgressEngine {
    const val BASELINE = 50
    const val MAX_LEVEL = 99
    private const val SAMPLE_WINDOW = 12
    private const val DECAY = 0.85

    val levelTitles: List<Pair<Int, String>> = listOf(
        1 to "Beginner",
        5 to "Apprentice",
        10 to "Practitioner",
        20 to "Flow Master",
        30 to "Advanced",
        50 to "Tai Chi Master"
    )

    /** XP needed to go from [level] to level + 1. */
    fun xpForNextLevel(level: Int): Long = 250L + 150L * level

    fun totalXpForLevel(level: Int): Long {
        var sum = 0L
        for (l in 1 until level) sum += xpForNextLevel(l)
        return sum
    }

    fun title(level: Int): String = levelTitles.last { level >= it.first }.second

    fun levelInfo(totalXp: Long): LevelInfo {
        var level = 1
        var remaining = totalXp.coerceAtLeast(0L)
        while (level < MAX_LEVEL && remaining >= xpForNextLevel(level)) {
            remaining -= xpForNextLevel(level)
            level++
        }
        val next = levelTitles.firstOrNull { it.first > level }
        return LevelInfo(
            level = level,
            title = title(level),
            xpInLevel = remaining,
            xpForNext = if (level >= MAX_LEVEL) 0L else xpForNextLevel(level),
            totalXp = totalXp,
            nextTitle = next?.second,
            nextTitleLevel = next?.first
        )
    }

    /**
     * Current attribute values: a recency-weighted average of the last samples that
     * trained each attribute. With few samples the value is blended toward a baseline.
     */
    fun attributes(history: List<HistoryRecord>): Map<Attribute, Int> {
        val sorted = history.sortedByDescending { it.timestamp }
        return Attribute.entries.associateWith { a ->
            val values = sorted.mapNotNull { r -> r.scores[a]?.takeIf { it > 0 } }.take(SAMPLE_WINDOW)
            if (values.isEmpty()) {
                BASELINE
            } else {
                var wSum = 0.0
                var vSum = 0.0
                values.forEachIndexed { i, v ->
                    val w = DECAY.pow(i)
                    wSum += w
                    vSum += v * w
                }
                val weighted = vSum / wSum
                val n = values.size
                val blended = if (n >= 3) weighted else (weighted * n + BASELINE * (3 - n)) / 3.0
                blended.roundToInt().coerceIn(0, 100)
            }
        }
    }

    fun flowScore(attributes: Map<Attribute, Int>): Int =
        if (attributes.isEmpty()) BASELINE else attributes.values.average().roundToInt()

    fun practiceXp(durationSec: Int, difficulty: Difficulty, score: Int): Int {
        val minutes = durationSec / 60
        val bonus = (score - 60).coerceAtLeast(0)
        return (40 + minutes * 10 + difficulty.rank * 30 + bonus).coerceAtMost(600)
    }

    fun challengeXp(score: Int, durationSec: Int): Int =
        (60 + score / 2 + (durationSec / 60) * 8).coerceAtMost(400)

    fun streakBonus(streak: Int): Int = 5 * streak.coerceIn(1, 20)

    /** Minutes practised on each of the last [days] days, oldest first. */
    fun dailyMinutes(history: List<HistoryRecord>, today: java.time.LocalDate, days: Int = 14): List<Pair<java.time.LocalDate, Int>> {
        val byDay = history.groupBy { it.date }.mapValues { e -> e.value.sumOf { it.durationSec } / 60 }
        return (days - 1 downTo 0).map { off ->
            val d = today.minusDays(off.toLong())
            d to (byDay[d] ?: 0)
        }
    }

    const val RECORD_XP = 50
    const val ACHIEVEMENT_XP = 100
}
