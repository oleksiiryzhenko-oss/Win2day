package com.taichiflow.app.domain.engine

import com.taichiflow.app.domain.model.Attribute
import com.taichiflow.app.domain.model.Difficulty
import com.taichiflow.app.domain.model.FlowArchetype
import com.taichiflow.app.domain.model.HistoryRecord
import com.taichiflow.app.domain.model.PracticeSession
import com.taichiflow.app.domain.model.Recommendation
import com.taichiflow.app.domain.model.TrainingGroup
import java.time.LocalDate
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

object RecommendationEngine {

    /**
     * Share of recent training time per group (last [days] days). Each activity's time is
     * split evenly between the groups of the attributes it trained.
     */
    fun trainingBalance(history: List<HistoryRecord>, today: LocalDate, days: Long = 30): Map<TrainingGroup, Float> {
        val from = today.minusDays(days)
        val totals = TrainingGroup.entries.associateWith { 0f }.toMutableMap()
        history.filter { !it.date.isBefore(from) }.forEach { r ->
            val groups = r.scores.keys.map { it.group }.distinct()
            if (groups.isEmpty()) return@forEach
            val share = r.durationSec.toFloat() / groups.size
            groups.forEach { g -> totals[g] = (totals[g] ?: 0f) + share }
        }
        val sum = totals.values.sum()
        if (sum <= 0f) return emptyMap()
        return totals.mapValues { it.value / sum }
    }

    fun neglectedGroup(balance: Map<TrainingGroup, Float>): TrainingGroup? {
        if (balance.isEmpty()) return null
        val even = 1f / TrainingGroup.entries.size
        val lowest = balance.minByOrNull { it.value } ?: return null
        return if (lowest.value < even * 0.55f) lowest.key else null
    }

    fun levelDifficulty(level: Int): Difficulty = when {
        level < 8 -> Difficulty.BEGINNER
        level < 20 -> Difficulty.INTERMEDIATE
        else -> Difficulty.ADVANCED
    }

    fun recommend(
        attributes: Map<Attribute, Int>,
        balance: Map<TrainingGroup, Float>,
        sessions: List<PracticeSession>,
        level: Int
    ): Recommendation {
        val strongest = attributes.maxByOrNull { it.value } ?: attributes.entries.first()
        val weakest = attributes.minByOrNull { it.value } ?: attributes.entries.first()
        val neglected = neglectedGroup(balance)
        val targetGroup = neglected ?: weakest.key.group

        val maxDifficulty = levelDifficulty(level)
        val candidates = sessions.filter { !it.isCustom }.let { all ->
            val byCategory = all.filter { it.category.primary.group == targetGroup }
            val byFocus = all.filter { s -> s.focus.any { it.group == targetGroup } }
            byCategory.ifEmpty { byFocus }.ifEmpty { all }
        }
        val allowed = candidates.filter { it.difficulty.rank <= maxDifficulty.rank }.ifEmpty { candidates }
        val session = allowed.sortedWith(
            compareByDescending<PracticeSession> { it.difficulty.rank }
                .thenBy { abs(it.minutes - 15) }
        ).firstOrNull()

        val groupLabel = targetGroup.label.lowercase(Locale.US)
        val top = balance.maxByOrNull { it.value }
        val targetShare = balance[targetGroup] ?: 0f
        val reason = if (top != null && top.key != targetGroup && top.value >= targetShare * 2f && top.value > 0f) {
            "Your recent sessions contain significantly more ${top.key.label.lowercase(Locale.US)} training than $groupLabel work."
        } else {
            "${weakest.key.label} is currently your lowest attribute (${weakest.value}). A focused session lifts it fastest."
        }
        val neglectedMessage = neglected?.let {
            "Your ${it.label.lowercase(Locale.US)} practice is lower than your other areas."
        }
        return Recommendation(
            strongest = strongest.key,
            strongestValue = strongest.value,
            weakest = weakest.key,
            weakestValue = weakest.value,
            session = session,
            reason = reason,
            neglected = neglected,
            neglectedMessage = neglectedMessage
        )
    }

    private fun avg(attrs: Map<Attribute, Int>, vararg keys: Attribute): Float =
        keys.map { (attrs[it] ?: 0).toFloat() }.average().toFloat()

    fun archetype(attrs: Map<Attribute, Int>): FlowArchetype {
        val sorted = attrs.entries.sortedByDescending { it.value }
        val strengths = sorted.take(2).map { it.key to it.value }
        val weakest = sorted.last()
        val spread = (sorted.first().value - sorted.last().value)
        val mean = attrs.values.average()
        val focusLine = "${weakest.key.label}-focused practices."

        if (mean >= 85 && spread <= 12) {
            return FlowArchetype(
                "COMPLETE FLOW", "Every dimension in harmony",
                "You combine balance, rhythm, breath and mobility at a high level. Keep refining the details.",
                strengths, weakest.key, "Flow Master sessions and advanced forms."
            )
        }
        if (spread <= 10) {
            return FlowArchetype(
                "BALANCED FLOW", "Even across the board",
                "No single area dominates your practice. A strong, well-rounded base to build on.",
                strengths, weakest.key, focusLine
            )
        }
        val candidates = listOf(
            Triple("POWER FLOW", avg(attrs, Attribute.STABILITY, Attribute.BALANCE),
                "Rooted and strong. Your stances hold and your weight stays centred." to "Grounded strength"),
            Triple("PRECISION FLOW", avg(attrs, Attribute.RHYTHM, Attribute.COORDINATION),
                "Exact timing and clean coordination define your movement." to "Timing and control"),
            Triple("MOBILITY FLOW", avg(attrs, Attribute.MOBILITY, Attribute.FLOW),
                "Open joints and long, spiralling lines make your forms expansive." to "Range and release"),
            Triple("CALM FLOW", avg(attrs, Attribute.BREATHING, Attribute.FOCUS),
                "Steady breath and a quiet mind carry your practice." to "Breath-led control"),
            Triple("DYNAMIC FLOW", avg(attrs, Attribute.FLOW, Attribute.RHYTHM, Attribute.MOBILITY),
                "Continuous, energetic transitions with strong momentum." to "Momentum and transitions")
        )
        val best = candidates.maxBy { it.second }
        return FlowArchetype(
            name = best.first,
            tagline = best.third.second,
            description = best.third.first,
            strengths = strengths,
            focusArea = weakest.key,
            recommendation = focusLine
        )
    }

    fun percent(value: Float): Int = (value * 100f).roundToInt()
}
