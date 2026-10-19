package com.taichiflow.app.data.seed

import com.taichiflow.app.data.catalog.AchievementCatalog
import com.taichiflow.app.data.catalog.DailyChallengeCatalog
import com.taichiflow.app.data.catalog.SessionCatalog
import com.taichiflow.app.data.catalog.WeeklyGoalCatalog
import com.taichiflow.app.data.local.AchievementEntity
import com.taichiflow.app.data.local.CustomSessionEntity
import com.taichiflow.app.data.local.DailyCompletionEntity
import com.taichiflow.app.data.local.HistoryEntity
import com.taichiflow.app.data.local.MovementProgressEntity
import com.taichiflow.app.data.local.TaiChiDao
import com.taichiflow.app.data.local.WeeklyClaimEntity
import com.taichiflow.app.data.local.historyEntity
import com.taichiflow.app.data.local.toDomain
import com.taichiflow.app.data.prefs.UserPreferences
import com.taichiflow.app.domain.engine.AchievementEngine
import com.taichiflow.app.domain.engine.GoalEngine
import com.taichiflow.app.domain.engine.ProgressEngine
import com.taichiflow.app.domain.engine.RecordsEngine
import com.taichiflow.app.domain.engine.StreakEngine
import com.taichiflow.app.domain.model.ActivityType
import com.taichiflow.app.domain.model.Attribute
import com.taichiflow.app.domain.model.BreathPattern
import com.taichiflow.app.domain.model.DailyChallenge
import com.taichiflow.app.domain.model.DailyKind
import com.taichiflow.app.domain.model.Difficulty
import com.taichiflow.app.domain.model.Goal
import com.taichiflow.app.domain.model.HistoryRecord
import com.taichiflow.app.domain.model.Metric
import com.taichiflow.app.domain.model.MetricCodec
import com.taichiflow.app.domain.model.PracticeCategory
import com.taichiflow.app.domain.model.PracticeSession
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Pre-populates a realistic practice history so the app looks lived-in on first launch:
 * an 8-day current streak, an 18-day longest streak, records, dailies, weekly claims,
 * a custom session, movement progress and achievements. Everything is ordinary local data
 * and can be cleared from Profile › Reset Data.
 */
object DemoSeeder {

    const val DEMO_XP = 14_130L

    private enum class Kind { PRACTICE, BALANCE, RHYTHM, BREATHING, FLOW, MASTER, CUSTOM, DAILY }

    private data class Plan(val offset: Int, val hour: Int, val minute: Int, val kind: Kind, val ref: String = "")

    private data class Draft(
        val type: ActivityType,
        val sessionId: String,
        val name: String,
        val category: PracticeCategory?,
        val durationSec: Int,
        val score: Int,
        val scores: Map<Attribute, Int>,
        val baseXp: Int,
        val metrics: List<Metric>,
        val movementIds: List<String>,
        val completion: Int
    )

    private val target = mapOf(
        Attribute.BALANCE to 90,
        Attribute.STABILITY to 87,
        Attribute.MOBILITY to 76,
        Attribute.COORDINATION to 88,
        Attribute.BREATHING to 82,
        Attribute.RHYTHM to 92,
        Attribute.FOCUS to 85,
        Attribute.FLOW to 88
    )

    private val plans = listOf(
        Plan(40, 18, 30, Kind.PRACTICE, "first_steps"),
        Plan(36, 18, 10, Kind.PRACTICE, "foundations"),
        Plan(33, 19, 0, Kind.PRACTICE, "grounded_roots"),
        Plan(29, 18, 40, Kind.PRACTICE, "cross_pattern"),
        Plan(29, 19, 10, Kind.BREATHING),
        Plan(28, 18, 20, Kind.BALANCE),
        Plan(28, 18, 30, Kind.PRACTICE, "balance_express"),
        Plan(27, 12, 30, Kind.PRACTICE, "lunch_flow"),
        Plan(26, 18, 0, Kind.RHYTHM),
        Plan(26, 18, 10, Kind.PRACTICE, "power_push"),
        Plan(25, 21, 0, Kind.PRACTICE, "evening_reset"),
        Plan(24, 18, 30, Kind.PRACTICE, "balance_flow"),
        Plan(23, 18, 0, Kind.BALANCE),
        Plan(23, 18, 5, Kind.RHYTHM),
        Plan(22, 18, 30, Kind.PRACTICE, "river_flow"),
        Plan(21, 12, 0, Kind.DAILY),
        Plan(21, 18, 30, Kind.PRACTICE, "calm_power"),
        Plan(20, 18, 30, Kind.MASTER),
        Plan(19, 18, 30, Kind.PRACTICE, "coordination_core"),
        Plan(18, 12, 15, Kind.RHYTHM),
        Plan(18, 12, 20, Kind.BREATHING),
        Plan(17, 18, 30, Kind.PRACTICE, "rooted_strength"),
        Plan(16, 18, 0, Kind.CUSTOM),
        Plan(16, 18, 30, Kind.DAILY),
        Plan(15, 10, 0, Kind.PRACTICE, "master_form"),
        Plan(14, 12, 0, Kind.DAILY),
        Plan(14, 18, 0, Kind.BALANCE),
        Plan(14, 18, 10, Kind.PRACTICE, "one_leg_lab"),
        Plan(13, 18, 30, Kind.PRACTICE, "continuous_form"),
        Plan(12, 7, 10, Kind.PRACTICE, "breath_foundation"),
        Plan(12, 18, 30, Kind.RHYTHM),
        Plan(8, 8, 15, Kind.PRACTICE, "morning_flow"),
        Plan(7, 12, 0, Kind.DAILY),
        Plan(7, 18, 30, Kind.PRACTICE, "tempo_shift"),
        Plan(6, 12, 10, Kind.DAILY),
        Plan(6, 18, 30, Kind.FLOW),
        Plan(5, 12, 0, Kind.DAILY),
        Plan(5, 18, 0, Kind.RHYTHM),
        Plan(5, 18, 10, Kind.PRACTICE, "sparrow_drill"),
        Plan(4, 12, 0, Kind.DAILY),
        Plan(4, 18, 30, Kind.PRACTICE, "river_flow"),
        Plan(3, 12, 0, Kind.DAILY),
        Plan(3, 18, 0, Kind.BALANCE),
        Plan(3, 18, 10, Kind.BREATHING),
        Plan(2, 12, 0, Kind.DAILY),
        Plan(2, 18, 30, Kind.CUSTOM),
        Plan(1, 12, 0, Kind.DAILY),
        Plan(1, 18, 0, Kind.PRACTICE, "balance_flow"),
        Plan(1, 18, 30, Kind.RHYTHM)
    )

    private val customMoves = listOf("wu_ji", "opening", "cloud_hands", "wild_horse", "white_crane", "closing")

    suspend fun seed(dao: TaiChiDao, prefs: UserPreferences) {
        val random = Random(20_260_930)
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val now = System.currentTimeMillis()

        fun ts(offset: Int, hour: Int, minute: Int): Long =
            today.minusDays(offset.toLong()).atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()

        // Custom session created three weeks ago
        val customCreated = ts(22, 20, 0)
        val customEntity = CustomSessionEntity(
            name = "My Morning Routine",
            movementIds = customMoves.joinToString(","),
            workSec = 90,
            restSec = 10,
            rounds = 1,
            inhale = 4,
            hold = 2,
            exhale = 6,
            holdOut = 0,
            createdAt = customCreated
        )
        val customId = dao.upsertCustomSession(customEntity)
        val customSession = customEntity.copy(id = customId).toDomain()

        val built = mutableListOf<HistoryRecord>()
        val entities = mutableListOf<HistoryEntity>()
        val cumulativeXp = mutableListOf<Int>()
        val dailyRows = mutableListOf<DailyCompletionEntity>()
        var runningXp = 0

        val ordered = plans.sortedWith(compareByDescending<Plan> { it.offset }.thenBy { it.hour * 60 + it.minute })
        for (plan in ordered) {
            val timestamp = ts(plan.offset, plan.hour, plan.minute)
            if (timestamp >= now) continue
            val date = today.minusDays(plan.offset.toLong())
            val trend = -plan.offset / 10f
            val value: (Attribute, Int) -> Int = { a, bonus ->
                (target.getValue(a) + trend + random.nextInt(-3, 4) + bonus).roundToInt().coerceIn(40, 99)
            }

            var daily: DailyChallenge? = null
            val draft: Draft = when (plan.kind) {
                Kind.PRACTICE -> practiceDraft(SessionCatalog.get(plan.ref) ?: SessionCatalog.all.first(), random, value)
                Kind.CUSTOM -> practiceDraft(customSession, random, value)
                Kind.BALANCE -> balanceDraft("Balance Flow", value)
                Kind.RHYTHM -> rhythmDraft("Flow Rhythm", random, value)
                Kind.BREATHING -> breathingDraft("Breathing · Flow 4-2-6", BreathPattern.FLOW, 6, value)
                Kind.FLOW -> flowDraft(random, value)
                Kind.MASTER -> masterDraft(random, value)
                Kind.DAILY -> {
                    val dc = DailyChallengeCatalog.forDate(date)
                    daily = dc
                    when (dc.kind) {
                        DailyKind.GUIDED -> practiceDraft(DailyChallengeCatalog.asSession(dc), random, value)
                        DailyKind.BALANCE -> balanceDraft(dc.name, value)
                        DailyKind.RHYTHM -> rhythmDraft(dc.name, random, value)
                        DailyKind.BREATHING -> breathingDraft(dc.name, dc.breath, dc.cycles, value)
                    }
                }
            }

            val records = RecordsEngine.newRecords(draft.type, draft.score, draft.durationSec, built)
            var xp = draft.baseXp + ProgressEngine.RECORD_XP * records.size
            val firstOfDay = built.none { it.date == date && StreakEngine.qualifies(it) }
            if (firstOfDay && draft.durationSec >= StreakEngine.QUALIFYING_SEC) {
                val days = StreakEngine.days(built) + date
                xp += ProgressEngine.streakBonus(StreakEngine.current(days, date))
            }
            if (daily != null) {
                xp += daily.xp
                dailyRows += DailyCompletionEntity(date.toEpochDay(), daily.id, timestamp, daily.xp)
            }

            val entity = historyEntity(
                sessionId = draft.sessionId,
                name = draft.name,
                type = draft.type,
                category = draft.category,
                timestamp = timestamp,
                durationSec = draft.durationSec,
                score = draft.score,
                xp = xp,
                scores = draft.scores,
                details = MetricCodec.encode(draft.metrics),
                movementIds = draft.movementIds,
                completion = draft.completion,
                isRecord = records.isNotEmpty(),
                recordLabel = records.joinToString(", "),
                daily = daily != null
            )
            entities += entity
            built += entity.toDomain()
            runningXp += xp
            cumulativeXp += runningXp
        }

        dao.insertHistoryAll(entities)
        dao.insertDailyAll(dailyRows)

        // Movement progress
        val moveTimes = mutableMapOf<String, Int>()
        val moveSec = mutableMapOf<String, Int>()
        val moveLast = mutableMapOf<String, Long>()
        built.forEach { r ->
            val ids = r.movementIds.distinct()
            if (ids.isEmpty()) return@forEach
            val per = r.durationSec / ids.size
            ids.forEach { id ->
                moveTimes[id] = (moveTimes[id] ?: 0) + 1
                moveSec[id] = (moveSec[id] ?: 0) + per
                moveLast[id] = maxOf(moveLast[id] ?: 0L, r.timestamp)
            }
        }
        dao.upsertMovementProgress(moveTimes.keys.map { id ->
            MovementProgressEntity(id, moveTimes.getValue(id), moveSec[id] ?: 0, moveLast[id] ?: now)
        })

        // Weekly goals: claims for past weeks plus any goal already completed this week
        val claims = mutableListOf<WeeklyClaimEntity>()
        for (w in 4 downTo 1) {
            val day = today.minusWeeks(w.toLong())
            val key = GoalEngine.weekKey(day)
            val claimedAt = StreakEngine.weekStart(day).plusDays(6).atTime(20, 0).atZone(zone).toInstant().toEpochMilli()
            WeeklyGoalCatalog.forWeek(key).take(if (w % 2 == 0) 3 else 2).forEach { g ->
                claims += WeeklyClaimEntity(key, g.id, claimedAt, g.xp)
            }
        }
        val weekKey = GoalEngine.weekKey(today)
        val weekStart = StreakEngine.weekStart(today).toEpochDay()
        val dailyThisWeek = dailyRows.count { it.epochDay in weekStart..today.toEpochDay() }
        val thisWeek = GoalEngine.progress(WeeklyGoalCatalog.forWeek(weekKey), built, dailyThisWeek, emptySet(), today)
        val lastThisWeek = built.filter { it.date.toEpochDay() >= weekStart }.maxOfOrNull { it.timestamp } ?: (now - 60_000L)
        thisWeek.filter { it.completed }.forEach { g ->
            claims += WeeklyClaimEntity(weekKey, g.goal.id, lastThisWeek, g.goal.xp)
        }
        claims.forEach { dao.insertWeeklyClaim(it) }

        // Achievements: replay history and stamp each unlock with the session that earned it
        val baselineXp = (DEMO_XP - runningXp - claims.sumOf { it.xp }).coerceAtLeast(0L)
        val unlocked = linkedMapOf<String, Long>()
        for (k in built.indices) {
            val prefix = built.subList(0, k + 1)
            val t = prefix.last().timestamp
            val attrs = ProgressEngine.attributes(prefix)
            val xpAt = baselineXp + cumulativeXp[k] + claims.filter { it.claimedAt <= t }.sumOf { it.xp }
            val stats = AchievementEngine.buildStats(
                history = prefix,
                attributes = attrs,
                flowScore = ProgressEngine.flowScore(attrs),
                level = ProgressEngine.levelInfo(xpAt).level,
                distinctMovements = prefix.flatMap { it.movementIds }.distinct().size,
                customCount = if (t >= customCreated) 1 else 0,
                dailyCount = dailyRows.count { it.completedAt <= t },
                weeklyClaims = claims.count { it.claimedAt <= t }
            )
            AchievementCatalog.all.forEach { d ->
                if (d.id !in unlocked && d.progress(stats) >= 1f) unlocked[d.id] = t
            }
        }
        dao.insertAchievements(unlocked.map { AchievementEntity(it.key, it.value) })

        prefs.updateProfile("Flow Athlete", Difficulty.INTERMEDIATE, Goal.BALANCE)
        prefs.setXp(DEMO_XP)
    }

    // ---------- Drafts ----------

    private fun practiceDraft(s: PracticeSession, random: Random, value: (Attribute, Int) -> Int): Draft {
        val completion = random.nextInt(90, 101)
        val duration = (s.totalSec * completion / 100) + random.nextInt(0, 30)
        val attrs = (s.focus + Attribute.FOCUS + Attribute.FLOW).distinct()
        val scores = attrs.associateWith { value(it, 0) }
        val score = scores.values.average().roundToInt()
        val done = (s.stepCount * completion / 100).coerceAtLeast(1)
        val pauses = random.nextInt(0, 3)
        return Draft(
            type = ActivityType.PRACTICE,
            sessionId = s.id,
            name = s.name,
            category = s.category,
            durationSec = duration,
            score = score,
            scores = scores,
            baseXp = ProgressEngine.practiceXp(duration, s.difficulty, score),
            metrics = listOf(
                Metric("Completion", "$completion%"),
                Metric("Movements", "$done / ${s.stepCount}"),
                Metric("Pauses", pauses.toString()),
                Metric("Skipped", "0"),
                Metric("Difficulty", s.difficulty.label),
                Metric("Breathing", s.breath.label)
            ),
            movementIds = s.movementIds.distinct(),
            completion = completion
        )
    }

    private fun balanceDraft(name: String, value: (Attribute, Int) -> Int): Draft {
        val balance = value(Attribute.BALANCE, 1)
        val stability = value(Attribute.STABILITY, 0)
        val control = value(Attribute.FOCUS, 4)
        val reaction = value(Attribute.COORDINATION, 0)
        val score = (balance * 0.4f + stability * 0.2f + control * 0.2f + reaction * 0.2f).roundToInt()
        return Draft(
            type = ActivityType.BALANCE,
            sessionId = "balance",
            name = name,
            category = PracticeCategory.BALANCE,
            durationSec = 45,
            score = score,
            scores = mapOf(
                Attribute.BALANCE to balance,
                Attribute.STABILITY to stability,
                Attribute.FOCUS to control,
                Attribute.COORDINATION to reaction
            ),
            baseXp = ProgressEngine.challengeXp(score, 45),
            metrics = listOf(
                Metric("Balance", "$balance"),
                Metric("Stability", "$stability"),
                Metric("Control", "$control"),
                Metric("Reaction", "$reaction"),
                Metric("Zone exits", "${((100 - balance) / 4).coerceAtLeast(0)}")
            ),
            movementIds = emptyList(),
            completion = 100
        )
    }

    private fun rhythmDraft(name: String, random: Random, value: (Attribute, Int) -> Int): Draft {
        val rhythm = value(Attribute.RHYTHM, 0)
        val accuracy = value(Attribute.COORDINATION, 2)
        val consistency = value(Attribute.FOCUS, 7)
        val hits = 32 - random.nextInt(0, 3)
        return Draft(
            type = ActivityType.RHYTHM,
            sessionId = "rhythm",
            name = name,
            category = PracticeCategory.COORDINATION,
            durationSec = 39,
            score = rhythm,
            scores = mapOf(Attribute.RHYTHM to rhythm, Attribute.COORDINATION to accuracy, Attribute.FOCUS to consistency),
            baseXp = ProgressEngine.challengeXp(rhythm, 39),
            metrics = listOf(
                Metric("Rhythm", "$rhythm%"),
                Metric("Accuracy", "$accuracy%"),
                Metric("Consistency", "$consistency%"),
                Metric("Beats hit", "$hits / 32")
            ),
            movementIds = emptyList(),
            completion = 100
        )
    }

    private fun breathingDraft(name: String, pattern: BreathPattern, cycles: Int, value: (Attribute, Int) -> Int): Draft {
        val breathing = value(Attribute.BREATHING, 0)
        val consistency = value(Attribute.FOCUS, 6)
        val duration = pattern.cycleSec * cycles
        return Draft(
            type = ActivityType.BREATHING,
            sessionId = "breathing",
            name = name,
            category = PracticeCategory.BREATHING,
            durationSec = duration,
            score = breathing,
            scores = mapOf(Attribute.BREATHING to breathing, Attribute.FOCUS to consistency),
            baseXp = ProgressEngine.challengeXp(breathing, duration),
            metrics = listOf(
                Metric("Cycles", "$cycles / $cycles"),
                Metric("Consistency", "$consistency%"),
                Metric("Pattern", pattern.label),
                Metric("Duration", "${duration / 60}:${(duration % 60).toString().padStart(2, '0')}")
            ),
            movementIds = emptyList(),
            completion = 100
        )
    }

    private fun flowDraft(random: Random, value: (Attribute, Int) -> Int): Draft {
        val scores = Attribute.entries.associateWith { value(it, 0) }
        val score = scores.values.average().roundToInt()
        val duration = 300 + random.nextInt(0, 30)
        return Draft(
            type = ActivityType.FLOW,
            sessionId = "flow_five",
            name = "5 Minute Flow",
            category = PracticeCategory.FLOW,
            durationSec = duration,
            score = score,
            scores = scores,
            baseXp = ProgressEngine.challengeXp(score, duration) + 60,
            metrics = listOf(
                Metric("Balance", "${scores[Attribute.BALANCE]}"),
                Metric("Cloud Hands", "${scores[Attribute.COORDINATION]}"),
                Metric("Weight Transfer", "${scores[Attribute.STABILITY]}"),
                Metric("Rhythm", "${scores[Attribute.RHYTHM]}"),
                Metric("Closing Form", "${scores[Attribute.BREATHING]}")
            ),
            movementIds = listOf("cloud_hands", "weight_transfer", "closing"),
            completion = 100
        )
    }

    private fun masterDraft(random: Random, value: (Attribute, Int) -> Int): Draft {
        val scores = Attribute.entries.associateWith { value(it, 0) }
        val score = scores.values.average().roundToInt()
        val duration = 870 + random.nextInt(0, 40)
        return Draft(
            type = ActivityType.MASTER,
            sessionId = "flow_master",
            name = "Flow Master",
            category = PracticeCategory.ADVANCED,
            durationSec = duration,
            score = score,
            scores = scores,
            baseXp = ProgressEngine.challengeXp(score, duration) + 150,
            metrics = listOf(
                Metric("Centering", "${scores[Attribute.BREATHING]}"),
                Metric("Weight Transfer", "${scores[Attribute.STABILITY]}"),
                Metric("Upper Body Flow", "${scores[Attribute.COORDINATION]}"),
                Metric("Lower Body Flow", "${scores[Attribute.MOBILITY]}"),
                Metric("Rhythm", "${scores[Attribute.RHYTHM]}"),
                Metric("Balance", "${scores[Attribute.BALANCE]}"),
                Metric("Closing", "${scores[Attribute.FOCUS]}")
            ),
            movementIds = listOf("weight_transfer", "cloud_hands", "ward_off", "silk_reeling", "bow_stance", "snake_creeps", "closing"),
            completion = 100
        )
    }
}
