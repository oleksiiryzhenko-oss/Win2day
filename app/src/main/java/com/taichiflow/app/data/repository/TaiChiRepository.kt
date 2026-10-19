package com.taichiflow.app.data.repository

import com.taichiflow.app.data.catalog.AchievementCatalog
import com.taichiflow.app.data.catalog.DailyChallengeCatalog
import com.taichiflow.app.data.catalog.SessionCatalog
import com.taichiflow.app.data.catalog.WeeklyGoalCatalog
import com.taichiflow.app.data.local.AchievementEntity
import com.taichiflow.app.data.local.AppDatabase
import com.taichiflow.app.data.local.CustomSessionEntity
import com.taichiflow.app.data.local.DailyCompletionEntity
import com.taichiflow.app.data.local.MovementProgressEntity
import com.taichiflow.app.data.local.WeeklyClaimEntity
import com.taichiflow.app.data.local.historyEntity
import com.taichiflow.app.data.local.toDomain
import com.taichiflow.app.data.prefs.UserPreferences
import com.taichiflow.app.data.seed.DemoSeeder
import com.taichiflow.app.domain.engine.AchievementEngine
import com.taichiflow.app.domain.engine.GoalEngine
import com.taichiflow.app.domain.engine.ProgressEngine
import com.taichiflow.app.domain.engine.RecordsEngine
import com.taichiflow.app.domain.engine.StreakEngine
import com.taichiflow.app.domain.model.ActivityResult
import com.taichiflow.app.domain.model.BreathPattern
import com.taichiflow.app.domain.model.DailyChallenge
import com.taichiflow.app.domain.model.Difficulty
import com.taichiflow.app.domain.model.Goal
import com.taichiflow.app.domain.model.PracticeSession
import com.taichiflow.app.domain.model.RewardSummary
import com.taichiflow.app.domain.model.XpItem
import com.taichiflow.app.domain.usecase.AppSnapshot
import com.taichiflow.app.domain.usecase.SnapshotBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDate

class TaiChiRepository(
    private val db: AppDatabase,
    private val prefs: UserPreferences,
    appScope: CoroutineScope
) {
    private val dao = db.dao()
    private val writeLock = Mutex()

    private data class Partial(
        val profile: com.taichiflow.app.domain.model.UserProfile,
        val history: List<com.taichiflow.app.domain.model.HistoryRecord>,
        val unlocked: Map<String, Long>,
        val custom: List<PracticeSession>,
        val moves: List<com.taichiflow.app.domain.model.MovementProgress>
    )

    val snapshot: Flow<AppSnapshot> = combine(
        prefs.profile.filter { it.seeded },
        dao.history(),
        dao.achievements(),
        dao.customSessions(),
        dao.movementProgress()
    ) { profile, history, achievements, custom, moves ->
        Partial(
            profile = profile,
            history = history.map { it.toDomain() },
            unlocked = achievements.associate { it.id to it.unlockedAt },
            custom = custom.map { it.toDomain() },
            moves = moves.map { it.toDomain() }
        )
    }.combine(combine(dao.dailyCompletions(), dao.weeklyClaims()) { d, w -> d to w }) { p, (daily, weekly) ->
        SnapshotBuilder.build(
            profile = p.profile,
            history = p.history,
            unlocked = p.unlocked,
            custom = p.custom,
            movementProgress = p.moves,
            dailyDays = daily.associate { it.epochDay to it.challengeId },
            weeklyClaims = weekly.map { it.weekKey to it.goalId }.toSet()
        )
    }
        .flowOn(Dispatchers.Default)
        .shareIn(appScope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    /** Seeds demo content on the very first launch. */
    suspend fun ensureSeeded() = writeLock.withLock {
        withContext(Dispatchers.IO) {
            if (!prefs.current().seeded) {
                DemoSeeder.seed(dao, prefs)
                prefs.setSeeded(true)
            }
        }
    }

    // ---------- Sessions ----------

    suspend fun session(id: String): PracticeSession? = withContext(Dispatchers.IO) {
        when {
            id.startsWith("custom_") -> id.removePrefix("custom_").toLongOrNull()?.let { dao.customSession(it)?.toDomain() }
            id.startsWith("daily_") -> DailyChallengeCatalog.get(id.removePrefix("daily_"))?.let { DailyChallengeCatalog.asSession(it) }
            id.startsWith("move_") -> SessionCatalog.single(id.removePrefix("move_"))
            else -> SessionCatalog.get(id)
        }
    }

    fun dailyChallenge(id: String): DailyChallenge? = DailyChallengeCatalog.get(id)

    suspend fun saveCustomSession(
        name: String,
        movementIds: List<String>,
        workSec: Int,
        restSec: Int,
        rounds: Int,
        breath: BreathPattern
    ): Long = withContext(Dispatchers.IO) {
        dao.upsertCustomSession(
            CustomSessionEntity(
                name = name.trim(),
                movementIds = movementIds.joinToString(","),
                workSec = workSec,
                restSec = restSec,
                rounds = rounds,
                inhale = breath.inhale,
                hold = breath.hold,
                exhale = breath.exhale,
                holdOut = breath.holdOut,
                createdAt = System.currentTimeMillis()
            )
        ).also { evaluateAchievementsOnly() }
    }

    suspend fun deleteCustomSession(sessionId: String) = withContext(Dispatchers.IO) {
        sessionId.removePrefix("custom_").toLongOrNull()?.let { dao.deleteCustomSession(it) }
    }

    // ---------- Profile & settings ----------

    suspend fun updateProfile(name: String, experience: Difficulty, goal: Goal) = prefs.updateProfile(name, experience, goal)
    suspend fun setFocusModeDefault(value: Boolean) = prefs.setFocusModeDefault(value)
    suspend fun setHaptics(value: Boolean) = prefs.setHaptics(value)

    suspend fun resetAll() = writeLock.withLock {
        withContext(Dispatchers.IO) {
            db.clearAllTables()
            prefs.reset()
        }
    }

    suspend fun loadDemoData() = writeLock.withLock {
        withContext(Dispatchers.IO) {
            db.clearAllTables()
            DemoSeeder.seed(dao, prefs)
            prefs.setSeeded(true)
        }
    }

    private suspend fun evaluateAchievementsOnly() {
        val profile = prefs.current()
        val history = dao.historyOnce().map { it.toDomain() }
        val attrs = ProgressEngine.attributes(history)
        val stats = AchievementEngine.buildStats(
            history, attrs, ProgressEngine.flowScore(attrs), ProgressEngine.levelInfo(profile.totalXp).level,
            dao.movementProgressOnce().count { it.timesCompleted > 0 },
            dao.customSessionsOnce().size,
            dao.dailyCompletionsOnce().size,
            dao.weeklyClaimsOnce().size
        )
        val unlockedIds = dao.achievementsOnce().map { it.id }.toSet()
        val fresh = AchievementEngine.newlyUnlocked(AchievementCatalog.all, stats, unlockedIds)
        if (fresh.isNotEmpty()) {
            val now = System.currentTimeMillis()
            dao.insertAchievements(fresh.map { AchievementEntity(it.id, now) })
            prefs.addXp(ProgressEngine.ACHIEVEMENT_XP.toLong() * fresh.size)
        }
    }

    // ---------- Recording results ----------

    /**
     * Saves a finished activity and applies every reward rule: records, streak, daily flow,
     * weekly goals, XP/levels and achievements. Returns a summary for the results screen.
     */
    suspend fun recordActivity(result: ActivityResult, dailyId: String?): RewardSummary = writeLock.withLock {
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val today = LocalDate.now()
            val profileBefore = prefs.current()
            val historyBefore = dao.historyOnce().map { it.toDomain() }
            val levelBefore = ProgressEngine.levelInfo(profileBefore.totalXp)
            val flowBefore = ProgressEngine.flowScore(ProgressEngine.attributes(historyBefore))

            val daysBefore = StreakEngine.days(historyBefore)
            val streakBefore = StreakEngine.current(daysBefore, today)
            val longestBefore = StreakEngine.longest(daysBefore)
            val qualifies = result.durationSec >= StreakEngine.QUALIFYING_SEC
            val daysAfter = if (qualifies) daysBefore + today else daysBefore
            val streakAfter = StreakEngine.current(daysAfter, today)
            val streakIncreased = streakAfter > streakBefore

            val records = RecordsEngine.newRecords(result.type, result.score, result.durationSec, historyBefore).toMutableList()
            if (streakIncreased && streakAfter > longestBefore && longestBefore > 0) records += RecordsEngine.LONGEST_STREAK

            val xpItems = mutableListOf(XpItem(result.type.label, result.baseXp))
            if (records.isNotEmpty()) xpItems += XpItem("Personal record", ProgressEngine.RECORD_XP * records.size)
            if (streakIncreased) xpItems += XpItem("Streak day $streakAfter", ProgressEngine.streakBonus(streakAfter))

            var dailyCompleted = false
            val todaysDaily = DailyChallengeCatalog.forDate(today)
            if (dailyId != null && dailyId == todaysDaily.id) {
                val inserted = dao.insertDaily(DailyCompletionEntity(today.toEpochDay(), dailyId, now, todaysDaily.xp))
                if (inserted != -1L) {
                    dailyCompleted = true
                    xpItems += XpItem("Daily Flow", todaysDaily.xp)
                }
            }

            val sessionXp = xpItems.sumOf { it.amount }
            dao.insertHistory(
                historyEntity(
                    sessionId = result.sessionId,
                    name = result.name,
                    type = result.type,
                    category = result.category,
                    timestamp = now,
                    durationSec = result.durationSec,
                    score = result.score,
                    xp = sessionXp,
                    scores = result.scores,
                    details = result.details,
                    movementIds = result.movementIds,
                    completion = result.completion,
                    isRecord = records.isNotEmpty(),
                    recordLabel = records.joinToString(", "),
                    daily = dailyCompleted
                )
            )

            if (result.movementIds.isNotEmpty()) {
                val existing = dao.movementProgressOnce().associateBy { it.movementId }
                val perMove = if (result.movementIds.isEmpty()) 0 else result.durationSec / result.movementIds.size
                dao.upsertMovementProgress(result.movementIds.distinct().map { id ->
                    val e = existing[id]
                    MovementProgressEntity(id, (e?.timesCompleted ?: 0) + 1, (e?.totalSec ?: 0) + perMove, now)
                })
            }

            // Weekly goals
            val historyAfter = dao.historyOnce().map { it.toDomain() }
            val weekKey = GoalEngine.weekKey(today)
            val claims = dao.weeklyClaimsOnce()
            val claimed = claims.filter { it.weekKey == weekKey }.map { it.goalId }.toSet()
            val dailyAll = dao.dailyCompletionsOnce()
            val weekStart = StreakEngine.weekStart(today).toEpochDay()
            val dailyThisWeek = dailyAll.count { it.epochDay in weekStart..today.toEpochDay() }
            val weekly = GoalEngine.progress(WeeklyGoalCatalog.forWeek(weekKey), historyAfter, dailyThisWeek, claimed, today)
            val weeklyCompleted = mutableListOf<String>()
            weekly.filter { it.completed && !it.claimed }.forEach { g ->
                dao.insertWeeklyClaim(WeeklyClaimEntity(weekKey, g.goal.id, now, g.goal.xp))
                weeklyCompleted += g.goal.title
                xpItems += XpItem("Weekly goal", g.goal.xp)
            }

            prefs.addXp(xpItems.sumOf { it.amount }.toLong())

            // Achievements (evaluated with the new level)
            val attrsAfter = ProgressEngine.attributes(historyAfter)
            val flowAfter = ProgressEngine.flowScore(attrsAfter)
            val xpNow = prefs.current().totalXp
            val stats = AchievementEngine.buildStats(
                historyAfter, attrsAfter, flowAfter, ProgressEngine.levelInfo(xpNow).level,
                dao.movementProgressOnce().count { it.timesCompleted > 0 },
                dao.customSessionsOnce().size,
                dailyAll.size,
                claims.size + weeklyCompleted.size
            )
            val unlockedIds = dao.achievementsOnce().map { it.id }.toSet()
            val fresh = AchievementEngine.newlyUnlocked(AchievementCatalog.all, stats, unlockedIds)
            if (fresh.isNotEmpty()) {
                dao.insertAchievements(fresh.map { AchievementEntity(it.id, now) })
                val bonus = ProgressEngine.ACHIEVEMENT_XP * fresh.size
                xpItems += XpItem("Achievements", bonus)
                prefs.addXp(bonus.toLong())
            }

            val levelAfter = ProgressEngine.levelInfo(prefs.current().totalXp)
            RewardSummary(
                title = if (dailyCompleted) "FLOW COMPLETED" else "SESSION COMPLETE",
                activityName = result.name,
                score = result.score,
                metrics = result.metrics,
                xpItems = xpItems,
                levelBefore = levelBefore,
                levelAfter = levelAfter,
                flowBefore = flowBefore,
                flowAfter = flowAfter,
                newRecords = records,
                streak = streakAfter,
                streakIncreased = streakIncreased,
                achievements = fresh,
                weeklyCompleted = weeklyCompleted,
                dailyCompleted = dailyCompleted
            )
        }
    }
}
