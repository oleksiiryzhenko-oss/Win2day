package com.taichiflow.app.domain.engine

import com.taichiflow.app.domain.model.Attribute
import com.taichiflow.app.domain.model.BreathPattern

enum class PhaseKind { BALANCE, RHYTHM, MOVEMENT, BREATHING }

/** One stage of a multi-part flow challenge. */
data class FlowPhase(
    val title: String,
    val cue: String,
    val kind: PhaseKind,
    val attributes: List<Attribute>,
    val seconds: Int,
    val movementId: String? = null,
    val beatSec: Float = 1.2f,
    val pattern: BreathPattern = BreathPattern.FLOW
)

/** The fixed programmes for 5 Minute Flow and Flow Master (about 15 minutes). */
object FlowPrograms {
    val five: List<FlowPhase> = listOf(
        FlowPhase("Balance", "Keep the indicator inside the zone.", PhaseKind.BALANCE,
            listOf(Attribute.BALANCE, Attribute.STABILITY), 60),
        FlowPhase("Cloud Hands", "Flow side to side. Tap on every transition.", PhaseKind.MOVEMENT,
            listOf(Attribute.COORDINATION, Attribute.MOBILITY), 60, movementId = "cloud_hands", beatSec = 3f),
        FlowPhase("Weight Transfer", "Shift your weight fully. Tap as you arrive on each side.", PhaseKind.MOVEMENT,
            listOf(Attribute.STABILITY, Attribute.MOBILITY), 60, movementId = "weight_transfer", beatSec = 3f),
        FlowPhase("Rhythm", "Tap when the indicator hits a zone.", PhaseKind.RHYTHM,
            listOf(Attribute.RHYTHM, Attribute.COORDINATION), 44, beatSec = 1.2f),
        FlowPhase("Closing Form", "Hold while inhaling, release to exhale.", PhaseKind.BREATHING,
            listOf(Attribute.BREATHING, Attribute.FOCUS), 60, movementId = "closing", pattern = BreathPattern.FLOW)
    )

    val master: List<FlowPhase> = listOf(
        FlowPhase("Centering", "Settle the breath. Hold while inhaling, release to exhale.", PhaseKind.BREATHING,
            listOf(Attribute.BREATHING, Attribute.FOCUS), 72, movementId = "wu_ji", pattern = BreathPattern.FLOW),
        FlowPhase("Weight Transfer", "Slow, complete shifts. Tap as you arrive on each side.", PhaseKind.MOVEMENT,
            listOf(Attribute.STABILITY, Attribute.BALANCE), 150, movementId = "weight_transfer", beatSec = 3f),
        FlowPhase("Upper Body Flow", "Cloud Hands led by the waist. Tap on every transition.", PhaseKind.MOVEMENT,
            listOf(Attribute.COORDINATION, Attribute.FLOW), 180, movementId = "cloud_hands", beatSec = 3f),
        FlowPhase("Lower Body Flow", "Tai Chi Walking, heel first. Tap on every step.", PhaseKind.MOVEMENT,
            listOf(Attribute.MOBILITY, Attribute.STABILITY), 180, movementId = "tai_chi_walk", beatSec = 3.5f),
        FlowPhase("Rhythm", "Tap when the indicator hits a zone.", PhaseKind.RHYTHM,
            listOf(Attribute.RHYTHM, Attribute.COORDINATION), 88, beatSec = 1.0f),
        FlowPhase("Balance", "Keep the indicator inside the shrinking zone.", PhaseKind.BALANCE,
            listOf(Attribute.BALANCE, Attribute.STABILITY), 90),
        FlowPhase("Closing", "Even breathing to finish. Hold while inhaling.", PhaseKind.BREATHING,
            listOf(Attribute.BREATHING, Attribute.FOCUS), 90, movementId = "closing", pattern = BreathPattern.EVEN)
    )

    fun totalSec(phases: List<FlowPhase>): Int = phases.sumOf { it.seconds }
}
