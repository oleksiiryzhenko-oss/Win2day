package com.taichiflow.app.data.catalog

import com.taichiflow.app.domain.model.AchievementDef
import com.taichiflow.app.domain.model.Attribute
import com.taichiflow.app.domain.model.BreathPattern
import com.taichiflow.app.domain.model.DailyChallenge
import com.taichiflow.app.domain.model.DailyKind
import com.taichiflow.app.domain.model.Difficulty
import com.taichiflow.app.domain.model.PracticeCategory
import com.taichiflow.app.domain.model.PracticeSession
import com.taichiflow.app.domain.model.WeeklyGoal
import com.taichiflow.app.domain.model.WeeklyKind
import java.time.LocalDate

object SessionCatalog {
    private fun s(
        id: String, name: String, cat: PracticeCategory, d: Difficulty, focus: List<Attribute>,
        moves: List<String>, work: Int, rest: Int, rounds: Int = 1,
        breath: BreathPattern = BreathPattern.FLOW, desc: String
    ) = PracticeSession(id, name, cat, d, focus, moves, work, rest, rounds, breath, desc)

    val all: List<PracticeSession> = listOf(
        s("morning_flow", "Morning Flow", PracticeCategory.BEGINNER, Difficulty.BEGINNER,
            listOf(Attribute.MOBILITY, Attribute.BALANCE),
            listOf("wu_ji", "opening", "cloud_hands", "brush_knee", "ward_off", "closing"), 110, 10,
            desc = "Wake up the joints and settle your balance with six foundational movements."),
        s("first_steps", "First Steps", PracticeCategory.BEGINNER, Difficulty.BEGINNER,
            listOf(Attribute.BALANCE, Attribute.BREATHING),
            listOf("wu_ji", "holding_ball", "weight_transfer", "shoulder_circles", "waist_turn", "closing"), 60, 15,
            desc = "A short first session: stance, breath and the weight shift behind every form."),
        s("foundations", "Foundations", PracticeCategory.BEGINNER, Difficulty.BEGINNER,
            listOf(Attribute.STABILITY, Attribute.COORDINATION),
            listOf("wu_ji", "bow_stance", "weight_transfer", "tai_chi_walk", "cloud_hands", "closing"), 90, 10,
            desc = "Stances, stepping and Cloud Hands. The building blocks of every form."),
        s("balance_flow", "Balance Flow", PracticeCategory.BALANCE, Difficulty.INTERMEDIATE,
            listOf(Attribute.BALANCE, Attribute.STABILITY),
            listOf("wu_ji", "weight_transfer", "white_crane", "golden_rooster", "repulse_monkey", "high_pat", "closing"), 140, 15,
            desc = "Single-leg holds and backward stepping for a rock-solid root."),
        s("one_leg_lab", "One-Leg Lab", PracticeCategory.BALANCE, Difficulty.ADVANCED,
            listOf(Attribute.BALANCE, Attribute.STABILITY),
            listOf("golden_rooster", "heel_kick", "turn_kick", "white_crane", "lotus_kick"), 75, 20, 2,
            desc = "Two rounds of kicks and one-leg stands. Control over height."),
        s("grounded_roots", "Grounded Roots", PracticeCategory.BALANCE, Difficulty.BEGINNER,
            listOf(Attribute.BALANCE, Attribute.FOCUS),
            listOf("wu_ji", "standing_post", "weight_transfer", "tai_chi_walk", "closing"), 120, 10,
            desc = "Slow stepping and stillness to feel the ground through the feet."),
        s("balance_express", "Balance Express", PracticeCategory.BALANCE, Difficulty.BEGINNER,
            listOf(Attribute.BALANCE, Attribute.STABILITY),
            listOf("weight_transfer", "white_crane", "high_pat", "closing"), 75, 10,
            desc = "Five quick minutes of balance work for busy days."),
        s("mobility_flow", "Mobility Flow", PracticeCategory.MOBILITY, Difficulty.BEGINNER,
            listOf(Attribute.MOBILITY, Attribute.FLOW),
            listOf("shoulder_circles", "waist_turn", "silk_reeling", "cloud_hands", "wild_horse", "closing"), 140, 10,
            desc = "Spirals and circles that open the shoulders, spine and hips."),
        s("open_joints", "Open Joints", PracticeCategory.MOBILITY, Difficulty.BEGINNER,
            listOf(Attribute.MOBILITY, Attribute.BREATHING),
            listOf("shoulder_circles", "lift_sky", "waist_turn", "bow_stance", "closing"), 90, 10,
            desc = "Gentle range-of-motion work paced by the breath."),
        s("deep_reach", "Deep Reach", PracticeCategory.MOBILITY, Difficulty.ADVANCED,
            listOf(Attribute.MOBILITY, Attribute.STABILITY),
            listOf("silk_reeling", "needle_sea", "snake_creeps", "single_whip", "carry_tiger", "closing"), 120, 15,
            desc = "Low postures and long lines for advanced range."),
        s("river_flow", "River Flow", PracticeCategory.FLOW, Difficulty.INTERMEDIATE,
            listOf(Attribute.FLOW, Attribute.COORDINATION),
            listOf("opening", "ward_off", "roll_back", "press", "push", "single_whip", "closing"), 100, 10,
            desc = "The four energies linked into one continuous current."),
        s("continuous_form", "Continuous Form", PracticeCategory.FLOW, Difficulty.INTERMEDIATE,
            listOf(Attribute.FLOW, Attribute.BALANCE),
            listOf("opening", "wild_horse", "white_crane", "brush_knee", "play_pipa", "repulse_monkey", "grasp_sparrow", "closing"), 90, 5,
            desc = "Eight movements with minimal rest. Keep the thread unbroken."),
        s("swimming_dragon_flow", "Swimming Dragon", PracticeCategory.FLOW, Difficulty.ADVANCED,
            listOf(Attribute.FLOW, Attribute.RHYTHM),
            listOf("silk_reeling", "swimming_dragon", "fair_lady", "shoot_tiger", "closing"), 150, 10,
            desc = "Serpentine, full-body flow for experienced practitioners."),
        s("lunch_flow", "Lunch Flow", PracticeCategory.FLOW, Difficulty.BEGINNER,
            listOf(Attribute.FLOW, Attribute.FOCUS),
            listOf("opening", "ward_off", "cloud_hands", "push", "closing"), 80, 10,
            desc = "A seven-minute reset in the middle of the day."),
        s("breath_foundation", "Breath Foundation", PracticeCategory.BREATHING, Difficulty.BEGINNER,
            listOf(Attribute.BREATHING, Attribute.FOCUS),
            listOf("wu_ji", "holding_ball", "lift_sky", "opening", "closing"), 90, 10,
            desc = "Link every movement to a slow 4-2-6 breath."),
        s("calm_power", "Calm Power", PracticeCategory.BREATHING, Difficulty.INTERMEDIATE,
            listOf(Attribute.BREATHING, Attribute.STABILITY),
            listOf("standing_post", "embrace_moon", "pushing_mountain", "cross_hands", "closing"), 120, 10,
            breath = BreathPattern.BOX, desc = "Box breathing under load. Strong legs, quiet breath."),
        s("coordination_core", "Coordination Core", PracticeCategory.COORDINATION, Difficulty.INTERMEDIATE,
            listOf(Attribute.COORDINATION, Attribute.RHYTHM),
            listOf("cloud_hands", "brush_knee", "deflect_parry", "fan_back", "strike_ears", "closing"), 110, 10,
            desc = "Hands, feet and waist arriving together."),
        s("cross_pattern", "Cross Pattern", PracticeCategory.COORDINATION, Difficulty.BEGINNER,
            listOf(Attribute.COORDINATION, Attribute.BALANCE),
            listOf("tai_chi_walk", "cloud_hands", "brush_knee", "repulse_monkey", "closing"), 90, 10,
            desc = "Opposite hand, opposite foot. Simple patterns, clean timing."),
        s("sparrow_drill", "Sparrow Drill", PracticeCategory.COORDINATION, Difficulty.ADVANCED,
            listOf(Attribute.COORDINATION, Attribute.FLOW),
            listOf("grasp_sparrow", "seven_stars", "punch_under_elbow", "fair_lady"), 100, 15, 2,
            desc = "Two rounds of complex linked techniques."),
        s("rooted_strength", "Rooted Strength", PracticeCategory.STRENGTH, Difficulty.INTERMEDIATE,
            listOf(Attribute.STABILITY, Attribute.BALANCE),
            listOf("horse_stance", "bow_stance", "press", "push", "standing_post", "closing"), 120, 15,
            desc = "Low stances and slow pushes that build leg endurance."),
        s("iron_legs", "Iron Legs", PracticeCategory.STRENGTH, Difficulty.ADVANCED,
            listOf(Attribute.STABILITY, Attribute.MOBILITY),
            listOf("horse_stance", "snake_creeps", "golden_rooster", "heel_kick", "standing_post"), 90, 20, 2,
            desc = "Deep holds and controlled kicks. Demanding on the legs."),
        s("power_push", "Power Push", PracticeCategory.STRENGTH, Difficulty.BEGINNER,
            listOf(Attribute.STABILITY, Attribute.COORDINATION),
            listOf("bow_stance", "ward_off", "press", "push", "apparent_close", "closing"), 80, 15,
            desc = "Whole-body pushing mechanics from the ground up."),
        s("evening_reset", "Evening Reset", PracticeCategory.RECOVERY, Difficulty.BEGINNER,
            listOf(Attribute.FOCUS, Attribute.MOBILITY),
            listOf("shoulder_circles", "waist_turn", "lift_sky", "holding_ball", "cross_hands", "closing"), 90, 10,
            desc = "Unwind the shoulders and back at the end of the day."),
        s("recovery_flow", "Recovery Flow", PracticeCategory.RECOVERY, Difficulty.BEGINNER,
            listOf(Attribute.FOCUS, Attribute.BREATHING),
            listOf("wu_ji", "cloud_hands", "silk_reeling", "lift_sky", "closing"), 120, 10,
            desc = "Easy circles and slow breath after a demanding day."),
        s("desk_break", "Desk Break", PracticeCategory.RECOVERY, Difficulty.BEGINNER,
            listOf(Attribute.MOBILITY, Attribute.FOCUS),
            listOf("shoulder_circles", "lift_sky", "waist_turn", "closing"), 60, 5,
            desc = "Four minutes to undo hours of sitting."),
        s("master_form", "Master Form 24", PracticeCategory.ADVANCED, Difficulty.ADVANCED,
            listOf(Attribute.FLOW, Attribute.COORDINATION),
            listOf(
                "opening", "wild_horse", "white_crane", "brush_knee", "play_pipa", "repulse_monkey",
                "grasp_sparrow", "single_whip", "cloud_hands", "high_pat", "heel_kick", "strike_ears",
                "snake_creeps", "golden_rooster", "fair_lady", "needle_sea", "fan_back", "deflect_parry",
                "apparent_close", "cross_hands", "closing"
            ), 60, 5,
            desc = "A full 21-posture form inspired by the classic short sequence."),
        s("warrior_flow", "Warrior Flow", PracticeCategory.ADVANCED, Difficulty.ADVANCED,
            listOf(Attribute.COORDINATION, Attribute.BALANCE),
            listOf("deflect_parry", "shoot_tiger", "seven_stars", "lotus_kick", "turn_kick", "closing"), 120, 15,
            desc = "The martial side of the form: strikes, kicks and turns."),
        s("tempo_shift", "Tempo Shift", PracticeCategory.ADVANCED, Difficulty.ADVANCED,
            listOf(Attribute.RHYTHM, Attribute.FLOW),
            listOf("cloud_hands", "swimming_dragon", "fair_lady", "carry_tiger", "closing"), 150, 10,
            desc = "Long flowing sets that demand steady, unchanging tempo.")
    )

    private val byId = all.associateBy { it.id }

    fun get(id: String): PracticeSession? = byId[id]

    /** Curated rotation for the Home screen "Today's Practice". */
    private val todayRotation = listOf(
        "morning_flow", "balance_flow", "mobility_flow", "river_flow",
        "coordination_core", "breath_foundation", "evening_reset"
    )

    fun todays(date: LocalDate): PracticeSession =
        byId.getValue(todayRotation[Math.floorMod(date.toEpochDay(), todayRotation.size.toLong()).toInt()])

    /** A short solo session for practising one movement from the library. */
    fun single(movementId: String): PracticeSession? {
        val m = MovementCatalog.get(movementId) ?: return null
        return PracticeSession(
            id = "move_$movementId",
            name = m.name,
            category = PracticeCategory.BEGINNER,
            difficulty = m.difficulty,
            focus = listOf(m.primary, m.secondary).distinct(),
            movementIds = listOf(m.id),
            workSec = m.suggestedSec,
            restSec = 10,
            rounds = 2,
            description = m.description
        )
    }
}

object DailyChallengeCatalog {
    val all: List<DailyChallenge> = listOf(
        DailyChallenge("d_cloud", "Cloud Hands Challenge", "Five minutes of continuous Cloud Hands.",
            "Maintain consistent rhythm.", 5, 150, DailyKind.GUIDED, listOf("cloud_hands"), 300),
        DailyChallenge("d_rooster", "Rooster Hold", "A Balance Flow round with a shrinking zone.",
            "Stay inside the zone as long as you can.", 1, 150, DailyKind.BALANCE),
        DailyChallenge("d_breath", "Steady Breath", "Six cycles of 4-2-6 breathing.",
            "Match the guide on every phase.", 2, 120, DailyKind.BREATHING, breath = BreathPattern.FLOW, cycles = 6),
        DailyChallenge("d_tempo", "Tempo Tap", "One round of Flow Rhythm.",
            "Tap every beat with even timing.", 1, 150, DailyKind.RHYTHM),
        DailyChallenge("d_horse", "Wild Horse Waves", "Parting Wild Horse's Mane, both sides.",
            "Keep the weight shift smooth.", 4, 150, DailyKind.GUIDED, listOf("wild_horse"), 240),
        DailyChallenge("d_crane", "Crane & Rooster", "Two single-leg postures back to back.",
            "No hopping, no rushing.", 5, 160, DailyKind.GUIDED, listOf("white_crane", "golden_rooster"), 150),
        DailyChallenge("d_silk", "Silk Thread", "Five minutes of Silk Reeling.",
            "Never let the circle stop.", 5, 150, DailyKind.GUIDED, listOf("silk_reeling"), 300),
        DailyChallenge("d_box", "Box Breath", "Six cycles of 4-4-4-4 breathing.",
            "Hold each side of the box evenly.", 2, 130, DailyKind.BREATHING, breath = BreathPattern.BOX, cycles = 6),
        DailyChallenge("d_walk", "Brush Knee Walk", "Brush Knee and Tai Chi Walking.",
            "Heel first, then shift.", 5, 150, DailyKind.GUIDED, listOf("brush_knee", "tai_chi_walk"), 150),
        DailyChallenge("d_post", "Still Mountain", "Five minutes of Standing Post.",
            "Stay still and relaxed.", 5, 170, DailyKind.GUIDED, listOf("standing_post"), 300),
        DailyChallenge("d_center", "Center Line", "A Balance Flow round.",
            "Recover quickly whenever you drift.", 1, 150, DailyKind.BALANCE),
        DailyChallenge("d_pulse", "Pulse Keeper", "One round of Flow Rhythm.",
            "Keep your taps consistent from start to finish.", 1, 150, DailyKind.RHYTHM),
        DailyChallenge("d_sparrow", "Sparrow's Tail", "Four minutes of Grasp Sparrow's Tail.",
            "Link all four parts without pausing.", 4, 170, DailyKind.GUIDED, listOf("grasp_sparrow"), 240),
        DailyChallenge("d_waist", "Waist Wheel", "Waist Turning into Cloud Hands.",
            "Lead every movement from the centre.", 5, 140, DailyKind.GUIDED, listOf("waist_turn", "cloud_hands"), 150),
        DailyChallenge("d_even", "Even Breath", "Eight cycles of 5-5 breathing.",
            "Make the inhale and exhale equal.", 2, 120, DailyKind.BREATHING, breath = BreathPattern.EVEN, cycles = 8),
        DailyChallenge("d_monkey", "Repulse Monkey Steps", "Four minutes stepping backward.",
            "Place the toe first, then settle.", 4, 150, DailyKind.GUIDED, listOf("repulse_monkey"), 240),
        DailyChallenge("d_push", "Mountain Push", "Pushing the Mountain and Press.",
            "Move from the legs, not the arms.", 4, 150, DailyKind.GUIDED, listOf("pushing_mountain", "press"), 120)
    )

    private val byId = all.associateBy { it.id }

    fun get(id: String): DailyChallenge? = byId[id]

    fun forDate(date: LocalDate): DailyChallenge =
        all[Math.floorMod(date.toEpochDay(), all.size.toLong()).toInt()]

    fun asSession(challenge: DailyChallenge): PracticeSession = PracticeSession(
        id = "daily_${challenge.id}",
        name = challenge.name,
        category = PracticeCategory.FLOW,
        difficulty = Difficulty.BEGINNER,
        focus = challenge.movementIds.mapNotNull { MovementCatalog.get(it)?.primary }.distinct()
            .ifEmpty { listOf(Attribute.FLOW) },
        movementIds = challenge.movementIds,
        workSec = challenge.workSec,
        restSec = 5,
        rounds = 1,
        description = challenge.description
    )
}

object WeeklyGoalCatalog {
    val all: List<WeeklyGoal> = listOf(
        WeeklyGoal("w_practices4", "Complete 4 practices", 4, WeeklyKind.PRACTICES, 200),
        WeeklyGoal("w_balance2", "Complete 2 balance challenges", 2, WeeklyKind.BALANCE_CHALLENGES, 150),
        WeeklyGoal("w_minutes60", "Practice for 60 total minutes", 60, WeeklyKind.MINUTES, 200),
        WeeklyGoal("w_categories3", "Complete 3 different movement categories", 3, WeeklyKind.CATEGORIES, 180),
        WeeklyGoal("w_record1", "Beat one personal record", 1, WeeklyKind.RECORDS, 200),
        WeeklyGoal("w_rhythm2", "Complete 2 rhythm challenges", 2, WeeklyKind.RHYTHM_CHALLENGES, 150),
        WeeklyGoal("w_breath3", "Complete 3 breathing sessions", 3, WeeklyKind.BREATHING_CHALLENGES, 150),
        WeeklyGoal("w_daily5", "Finish 5 Daily Flows", 5, WeeklyKind.DAILY, 220),
        WeeklyGoal("w_days5", "Practice on 5 different days", 5, WeeklyKind.ACTIVE_DAYS, 220),
        WeeklyGoal("w_flow1", "Complete a Flow challenge", 1, WeeklyKind.FLOW_CHALLENGES, 180),
        WeeklyGoal("w_high3", "Score 85+ three times", 3, WeeklyKind.HIGH_SCORES, 200),
        WeeklyGoal("w_challenges5", "Complete 5 challenges of any type", 5, WeeklyKind.CHALLENGES_ANY, 180),
        WeeklyGoal("w_minutes90", "Practice for 90 total minutes", 90, WeeklyKind.MINUTES, 260)
    )

    /** Five goals per week. The first five rotate in a fixed pattern based on the week number. */
    fun forWeek(weekKey: Long): List<WeeklyGoal> {
        val weekIndex = Math.floorDiv(weekKey, 7L)
        val start = Math.floorMod(weekIndex * 3, all.size.toLong()).toInt()
        return (0 until 5).map { all[(start + it) % all.size] }
    }
}

object AchievementCatalog {
    private fun ratio(value: Int, target: Int): Float = value.toFloat() / target

    val all: List<AchievementDef> = listOf(
        AchievementDef("first_flow", "FIRST FLOW", "Complete your first practice.") { ratio(it.practices, 1) },
        AchievementDef("balanced", "BALANCED", "Reach 90 Balance.") { ratio(it.attributes[Attribute.BALANCE] ?: 0, 90) },
        AchievementDef("rhythm_master", "RHYTHM MASTER", "Reach 95 Rhythm.") {
            ratio(maxOf(it.attributes[Attribute.RHYTHM] ?: 0, it.bestRhythm), 95)
        },
        AchievementDef("flow_state", "FLOW STATE", "Reach a Flow Score of 90.") { ratio(it.flowScore, 90) },
        AchievementDef("consistent", "CONSISTENT", "Practise 7 consecutive days.") { ratio(it.longestStreak, 7) },
        AchievementDef("dedicated", "DEDICATED", "Practise for 10 total hours.") { ratio(it.totalMinutes, 600) },
        AchievementDef("movement_master", "MOVEMENT MASTER", "Complete 30 different movements.") { ratio(it.distinctMovements, 30) },
        AchievementDef("journey", "TAI CHI JOURNEY", "Reach Flow Level 25.") { ratio(it.level, 25) },
        AchievementDef("practitioner", "PRACTITIONER", "Reach Flow Level 10.") { ratio(it.level, 10) },
        AchievementDef("steady_start", "STEADY START", "Practise 3 days in a row.") { ratio(it.longestStreak, 3) },
        AchievementDef("unshakable", "UNSHAKABLE", "Score 90 in Balance Flow.") { ratio(it.bestBalance, 90) },
        AchievementDef("metronome", "METRONOME", "Score 90 in Flow Rhythm.") { ratio(it.bestRhythm, 90) },
        AchievementDef("deep_breath", "DEEP BREATH", "Complete 5 breathing sessions.") { ratio(it.breathingCount, 5) },
        AchievementDef("five_minute", "FIVE MINUTE FLOW", "Complete the 5 Minute Flow challenge.") { ratio(it.flowCount, 1) },
        AchievementDef("flow_master", "FLOW MASTER", "Complete a Flow Master session.") { ratio(it.masterCount, 1) },
        AchievementDef("master_class", "MASTER CLASS", "Score 85 in Flow Master.") { ratio(it.bestMaster, 85) },
        AchievementDef("architect", "ARCHITECT", "Create a custom session.") { ratio(it.customCount, 1) },
        AchievementDef("daily_devotion", "DAILY DEVOTION", "Complete 7 Daily Flows.") { ratio(it.dailyCount, 7) },
        AchievementDef("weekly_warrior", "WEEKLY WARRIOR", "Complete 5 weekly goals.") { ratio(it.weeklyClaims, 5) },
        AchievementDef("explorer", "EXPLORER", "Practise in all 9 categories.") { ratio(it.categoriesPracticed, 9) },
        AchievementDef("century", "CENTURY", "Log 100 sessions.") { ratio(it.totalSessions, 100) },
        AchievementDef("early_bird", "EARLY BIRD", "Practise before 8 AM.") { if (it.earlyBird) 1f else 0f },
        AchievementDef("marathon", "LONG FORM", "Complete a single session of 30 minutes.") { ratio(it.longestSessionMin, 30) },
        AchievementDef("record_breaker", "RECORD BREAKER", "Set 5 personal records.") { ratio(it.recordsCount, 5) },
        AchievementDef("half_century", "HALF CENTURY", "Complete 50 practices.") { ratio(it.practices, 50) }
    )
}
