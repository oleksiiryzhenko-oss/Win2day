package com.taichiflow.app.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class Attribute(val label: String) {
    BALANCE("Balance"),
    STABILITY("Stability"),
    MOBILITY("Mobility"),
    COORDINATION("Coordination"),
    BREATHING("Breathing"),
    RHYTHM("Rhythm"),
    FOCUS("Focus"),
    FLOW("Flow");

    val group: TrainingGroup
        get() = when (this) {
            BALANCE, STABILITY -> TrainingGroup.BALANCE
            MOBILITY -> TrainingGroup.MOBILITY
            COORDINATION -> TrainingGroup.COORDINATION
            BREATHING, FOCUS -> TrainingGroup.BREATHING
            RHYTHM -> TrainingGroup.RHYTHM
            FLOW -> TrainingGroup.FLOW
        }
}

/** Buckets used by the Practice Balance analysis. */
enum class TrainingGroup(val label: String) {
    BALANCE("Balance"),
    MOBILITY("Mobility"),
    FLOW("Flow"),
    BREATHING("Breathing"),
    COORDINATION("Coordination"),
    RHYTHM("Rhythm")
}

enum class Difficulty(val label: String, val rank: Int) {
    BEGINNER("Beginner", 0),
    INTERMEDIATE("Intermediate", 1),
    ADVANCED("Advanced", 2)
}

enum class PracticeCategory(val label: String, val primary: Attribute, val blurb: String) {
    BEGINNER("Beginner", Attribute.FLOW, "Foundations and first forms"),
    BALANCE("Balance", Attribute.BALANCE, "Single-leg control and rooting"),
    MOBILITY("Mobility", Attribute.MOBILITY, "Open the joints, spiral the spine"),
    FLOW("Flow", Attribute.FLOW, "Continuous linked forms"),
    BREATHING("Breathing", Attribute.BREATHING, "Movement paced by the breath"),
    COORDINATION("Coordination", Attribute.COORDINATION, "Hands, feet and waist as one"),
    STRENGTH("Strength", Attribute.STABILITY, "Low stances, strong roots"),
    RECOVERY("Recovery", Attribute.FOCUS, "Gentle resets and cool-downs"),
    ADVANCED("Advanced", Attribute.FLOW, "Long forms, kicks and transitions")
}

enum class Goal(val label: String) {
    BALANCE("Balance"),
    MOBILITY("Mobility"),
    COORDINATION("Coordination"),
    FITNESS("Fitness"),
    RELAXATION("Relaxation"),
    FLOW("Flow")
}

enum class ActivityType(val label: String) {
    PRACTICE("Practice"),
    BALANCE("Balance Flow"),
    RHYTHM("Flow Rhythm"),
    BREATHING("Breathing"),
    FLOW("5 Minute Flow"),
    MASTER("Flow Master")
}

data class BreathPattern(val inhale: Int, val hold: Int, val exhale: Int, val holdOut: Int = 0) {
    val cycleSec: Int get() = inhale + hold + exhale + holdOut
    val label: String get() = if (holdOut > 0) "$inhale-$hold-$exhale-$holdOut" else "$inhale-$hold-$exhale"

    companion object {
        val FLOW = BreathPattern(4, 2, 6)
        val BOX = BreathPattern(4, 4, 4, 4)
        val EVEN = BreathPattern(5, 0, 5)
        val presets: List<Pair<String, BreathPattern>> = listOf("Flow" to FLOW, "Box" to BOX, "Even" to EVEN)

        fun nameOf(pattern: BreathPattern): String =
            presets.firstOrNull { it.second == pattern }?.first ?: "Custom"
    }
}

data class Movement(
    val id: String,
    val name: String,
    val difficulty: Difficulty,
    val description: String,
    val instructions: String,
    val primary: Attribute,
    val secondary: Attribute,
    val suggestedSec: Int,
    val breathing: String,
    val tempo: String,
    val mistakes: List<String>
)

data class PracticeSession(
    val id: String,
    val name: String,
    val category: PracticeCategory,
    val difficulty: Difficulty,
    val focus: List<Attribute>,
    val movementIds: List<String>,
    val workSec: Int,
    val restSec: Int,
    val rounds: Int = 1,
    val breath: BreathPattern = BreathPattern.FLOW,
    val description: String = "",
    val isCustom: Boolean = false
) {
    val stepCount: Int get() = movementIds.size * rounds
    val totalSec: Int get() = stepCount * workSec + (stepCount - 1).coerceAtLeast(0) * restSec
    val minutes: Int get() = ((totalSec + 30) / 60).coerceAtLeast(1)
    val focusLabel: String get() = focus.joinToString(" + ") { it.label }
}

data class UserProfile(
    val name: String,
    val experience: Difficulty,
    val goal: Goal,
    val totalXp: Long,
    val seeded: Boolean,
    val focusModeDefault: Boolean,
    val haptics: Boolean
)

data class HistoryRecord(
    val id: Long,
    val sessionId: String,
    val name: String,
    val type: ActivityType,
    val category: PracticeCategory?,
    val timestamp: Long,
    val durationSec: Int,
    val score: Int,
    val xp: Int,
    val scores: Map<Attribute, Int>,
    val details: String,
    val movementIds: List<String>,
    val completion: Int,
    val isRecord: Boolean,
    val recordLabel: String,
    val daily: Boolean
) {
    val date: LocalDate
        get() = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
}

data class LevelInfo(
    val level: Int,
    val title: String,
    val xpInLevel: Long,
    val xpForNext: Long,
    val totalXp: Long,
    val nextTitle: String?,
    val nextTitleLevel: Int?
) {
    val progress: Float get() = if (xpForNext <= 0L) 1f else (xpInLevel.toFloat() / xpForNext).coerceIn(0f, 1f)
}

data class WeekDayStatus(val label: String, val done: Boolean, val isToday: Boolean, val isFuture: Boolean)

data class StreakInfo(
    val current: Int,
    val longest: Int,
    val week: List<WeekDayStatus>,
    val practicedToday: Boolean
)

data class PersonalRecord(val label: String, val value: String, val timestamp: Long?)

data class Recommendation(
    val strongest: Attribute,
    val strongestValue: Int,
    val weakest: Attribute,
    val weakestValue: Int,
    val session: PracticeSession?,
    val reason: String,
    val neglected: TrainingGroup?,
    val neglectedMessage: String?
)

data class FlowArchetype(
    val name: String,
    val tagline: String,
    val description: String,
    val strengths: List<Pair<Attribute, Int>>,
    val focusArea: Attribute,
    val recommendation: String
)

enum class DailyKind { GUIDED, BALANCE, RHYTHM, BREATHING }

data class DailyChallenge(
    val id: String,
    val name: String,
    val description: String,
    val goal: String,
    val durationMin: Int,
    val xp: Int,
    val kind: DailyKind,
    val movementIds: List<String> = emptyList(),
    val workSec: Int = 0,
    val breath: BreathPattern = BreathPattern.FLOW,
    val cycles: Int = 6
)

enum class WeeklyKind {
    PRACTICES, BALANCE_CHALLENGES, RHYTHM_CHALLENGES, BREATHING_CHALLENGES, FLOW_CHALLENGES,
    MINUTES, CATEGORIES, RECORDS, DAILY, ACTIVE_DAYS, HIGH_SCORES, CHALLENGES_ANY
}

data class WeeklyGoal(val id: String, val title: String, val target: Int, val kind: WeeklyKind, val xp: Int)

data class WeeklyGoalProgress(val goal: WeeklyGoal, val current: Int, val claimed: Boolean) {
    val completed: Boolean get() = current >= goal.target
    val fraction: Float get() = (current.toFloat() / goal.target).coerceIn(0f, 1f)
}

data class AchievementStats(
    val practices: Int,
    val totalSessions: Int,
    val attributes: Map<Attribute, Int>,
    val flowScore: Int,
    val longestStreak: Int,
    val totalMinutes: Int,
    val distinctMovements: Int,
    val level: Int,
    val bestBalance: Int,
    val bestRhythm: Int,
    val breathingCount: Int,
    val flowCount: Int,
    val masterCount: Int,
    val bestMaster: Int,
    val customCount: Int,
    val dailyCount: Int,
    val weeklyClaims: Int,
    val categoriesPracticed: Int,
    val earlyBird: Boolean,
    val longestSessionMin: Int,
    val recordsCount: Int
)

data class AchievementDef(
    val id: String,
    val title: String,
    val description: String,
    val progress: (AchievementStats) -> Float
)

data class AchievementState(val def: AchievementDef, val unlockedAt: Long?, val progress: Float) {
    val unlocked: Boolean get() = unlockedAt != null
}

data class MovementProgress(val movementId: String, val times: Int, val totalSec: Int, val lastPracticed: Long)

data class Metric(val label: String, val value: String)

/** Everything an activity reports when it finishes. */
data class ActivityResult(
    val type: ActivityType,
    val sessionId: String,
    val name: String,
    val category: PracticeCategory?,
    val durationSec: Int,
    val score: Int,
    val scores: Map<Attribute, Int>,
    val baseXp: Int,
    val details: String,
    val metrics: List<Metric>,
    val movementIds: List<String> = emptyList(),
    val completion: Int = 100
)

data class XpItem(val label: String, val amount: Int)

data class RewardSummary(
    val title: String,
    val activityName: String,
    val score: Int,
    val metrics: List<Metric>,
    val xpItems: List<XpItem>,
    val levelBefore: LevelInfo,
    val levelAfter: LevelInfo,
    val flowBefore: Int,
    val flowAfter: Int,
    val newRecords: List<String>,
    val streak: Int,
    val streakIncreased: Boolean,
    val achievements: List<AchievementDef>,
    val weeklyCompleted: List<String>,
    val dailyCompleted: Boolean
) {
    val totalXp: Int get() = xpItems.sumOf { it.amount }
    val leveledUp: Boolean get() = levelAfter.level > levelBefore.level
}
