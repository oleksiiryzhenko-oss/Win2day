package com.taichiflow.app.domain.usecase

import com.taichiflow.app.data.catalog.AchievementCatalog
import com.taichiflow.app.data.catalog.DailyChallengeCatalog
import com.taichiflow.app.data.catalog.SessionCatalog
import com.taichiflow.app.data.catalog.WeeklyGoalCatalog
import com.taichiflow.app.domain.engine.AchievementEngine
import com.taichiflow.app.domain.engine.GoalEngine
import com.taichiflow.app.domain.engine.ProgressEngine
import com.taichiflow.app.domain.engine.RecommendationEngine
import com.taichiflow.app.domain.engine.RecordsEngine
import com.taichiflow.app.domain.engine.StreakEngine
import com.taichiflow.app.domain.model.AchievementState
import com.taichiflow.app.domain.model.AchievementStats
import com.taichiflow.app.domain.model.Attribute
import com.taichiflow.app.domain.model.DailyChallenge
import com.taichiflow.app.domain.model.FlowArchetype
import com.taichiflow.app.domain.model.HistoryRecord
import com.taichiflow.app.domain.model.LevelInfo
import com.taichiflow.app.domain.model.MovementProgress
import com.taichiflow.app.domain.model.PersonalRecord
import com.taichiflow.app.domain.model.PracticeSession
import com.taichiflow.app.domain.model.Recommendation
import com.taichiflow.app.domain.model.StreakInfo
import com.taichiflow.app.domain.model.TrainingGroup
import com.taichiflow.app.domain.model.UserProfile
import com.taichiflow.app.domain.model.WeeklyGoalProgress
import java.time.LocalDate

/** Everything the screens need, derived from stored data in one place. */
data class AppSnapshot(
    val today: LocalDate,
    val profile: UserProfile,
    val level: LevelInfo,
    val attributes: Map<Attribute, Int>,
    val flowScore: Int,
    val bestScore: Int,
    val history: List<HistoryRecord>,
    val streak: StreakInfo,
    val records: List<PersonalRecord>,
    val trainingBalance: Map<TrainingGroup, Float>,
    val recommendation: Recommendation,
    val archetype: FlowArchetype,
    val weekly: List<WeeklyGoalProgress>,
    val daily: DailyChallenge,
    val dailyDone: Boolean,
    val achievements: List<AchievementState>,
    val customSessions: List<PracticeSession>,
    val movementProgress: Map<String, MovementProgress>,
    val todaysPractice: PracticeSession,
    val stats: AchievementStats,
    val totalPracticeSec: Int
) {
    val weeklyDone: Int get() = weekly.count { it.completed }
    val unlockedCount: Int get() = achievements.count { it.unlocked }
    fun bestFor(type: com.taichiflow.app.domain.model.ActivityType): Int? =
        history.filter { it.type == type }.maxOfOrNull { it.score }
}

object SnapshotBuilder {

    fun build(
        profile: UserProfile,
        history: List<HistoryRecord>,
        unlocked: Map<String, Long>,
        custom: List<PracticeSession>,
        movementProgress: List<MovementProgress>,
        dailyDays: Map<Long, String>,
        weeklyClaims: Set<Pair<Long, String>>,
        today: LocalDate = LocalDate.now()
    ): AppSnapshot {
        val level = ProgressEngine.levelInfo(profile.totalXp)
        val attributes = ProgressEngine.attributes(history)
        val flow = ProgressEngine.flowScore(attributes)
        val streak = StreakEngine.info(history, today)
        val balance = RecommendationEngine.trainingBalance(history, today)
        val recommendation = RecommendationEngine.recommend(attributes, balance, SessionCatalog.all, level.level)
        val archetype = RecommendationEngine.archetype(attributes)

        val weekKey = GoalEngine.weekKey(today)
        val weekGoals = WeeklyGoalCatalog.forWeek(weekKey)
        val weekStart = StreakEngine.weekStart(today).toEpochDay()
        val dailyThisWeek = dailyDays.keys.count { it in weekStart..today.toEpochDay() }
        val claimedIds = weeklyClaims.filter { it.first == weekKey }.map { it.second }.toSet()
        val weekly = GoalEngine.progress(weekGoals, history, dailyThisWeek, claimedIds, today)

        val daily = DailyChallengeCatalog.forDate(today)
        val dailyDone = dailyDays.containsKey(today.toEpochDay())

        val stats = AchievementEngine.buildStats(
            history = history,
            attributes = attributes,
            flowScore = flow,
            level = level.level,
            distinctMovements = movementProgress.count { it.times > 0 },
            customCount = custom.size,
            dailyCount = dailyDays.size,
            weeklyClaims = weeklyClaims.size
        )
        val achievements = AchievementEngine.states(AchievementCatalog.all, stats, unlocked)

        return AppSnapshot(
            today = today,
            profile = profile,
            level = level,
            attributes = attributes,
            flowScore = flow,
            bestScore = history.maxOfOrNull { it.score } ?: 0,
            history = history.sortedByDescending { it.timestamp },
            streak = streak,
            records = RecordsEngine.records(history),
            trainingBalance = balance,
            recommendation = recommendation,
            archetype = archetype,
            weekly = weekly,
            daily = daily,
            dailyDone = dailyDone,
            achievements = achievements.sortedWith(
                compareByDescending<AchievementState> { it.unlocked }.thenByDescending { it.progress }
            ),
            customSessions = custom,
            movementProgress = movementProgress.associateBy { it.movementId },
            todaysPractice = SessionCatalog.todays(today),
            stats = stats,
            totalPracticeSec = history.sumOf { it.durationSec }
        )
    }
}
