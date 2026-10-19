package com.taichiflow.app.domain.engine

import com.taichiflow.app.domain.model.BreathPattern
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt

enum class BreathPhase(val label: String, val short: String) {
    INHALE("Inhale", "IN"),
    HOLD("Hold", "HOLD"),
    EXHALE("Exhale", "OUT"),
    HOLD_OUT("Hold", "HOLD")
}

data class BreathState(
    val phase: BreathPhase,
    val remainingSec: Float,
    val phaseProgress: Float,
    val scale: Float,
    val phaseElapsed: Float
)

data class BreathingResult(val cycles: Int, val targetCycles: Int, val consistency: Int, val durationSec: Int, val score: Int)

/**
 * Guided breathing: the player presses and holds during INHALE and HOLD and releases
 * during EXHALE. Consistency is the share of time the touch matched the guide
 * (a short grace window after every phase change is not judged).
 */
class BreathingEngine(val pattern: BreathPattern, val cycles: Int) {
    var elapsed = 0f
        private set
    private var matched = 0f
    private var judgedTime = 0f

    val totalSec: Float get() = pattern.cycleSec.toFloat() * cycles
    val finished: Boolean get() = elapsed >= totalSec
    val completedCycles: Int
        get() = if (pattern.cycleSec <= 0) 0 else min(cycles, (elapsed / pattern.cycleSec).toInt())
    val progress: Float get() = if (totalSec <= 0f) 1f else (elapsed / totalSec).coerceIn(0f, 1f)

    fun state(): BreathState = phaseAt(pattern, elapsed)

    fun update(dt: Float, pressed: Boolean) {
        if (finished) return
        val step = dt.coerceIn(0f, 0.1f)
        elapsed = min(totalSec, elapsed + step)
        val st = phaseAt(pattern, elapsed)
        if (st.phaseElapsed > GRACE_SEC) {
            judgedTime += step
            val expected = st.phase == BreathPhase.INHALE || st.phase == BreathPhase.HOLD
            if (pressed == expected) matched += step
        }
    }

    val liveConsistency: Int
        get() = if (judgedTime <= 0f) 100 else (matched / judgedTime * 100f).roundToInt().coerceIn(0, 100)

    fun result(): BreathingResult {
        val consistency = liveConsistency
        val completionPct = if (cycles == 0) 0 else completedCycles * 100 / cycles
        val score = (consistency * 0.75f + completionPct * 0.25f).roundToInt().coerceIn(0, 100)
        return BreathingResult(completedCycles, cycles, consistency, elapsed.roundToInt(), score)
    }

    companion object {
        const val GRACE_SEC = 0.6f

        fun phaseAt(pattern: BreathPattern, t: Float): BreathState {
            val parts = listOf(
                BreathPhase.INHALE to pattern.inhale,
                BreathPhase.HOLD to pattern.hold,
                BreathPhase.EXHALE to pattern.exhale,
                BreathPhase.HOLD_OUT to pattern.holdOut
            ).filter { it.second > 0 }
            if (parts.isEmpty()) return BreathState(BreathPhase.INHALE, 0f, 0f, 0.55f, 0f)
            val cycle = pattern.cycleSec.toFloat()
            var tt = t % cycle
            for ((phase, d) in parts) {
                if (tt < d) {
                    val p = tt / d
                    return BreathState(phase, d - tt, p, scaleFor(phase, p), tt)
                }
                tt -= d
            }
            val last = parts.last()
            return BreathState(last.first, 0f, 1f, scaleFor(last.first, 1f), last.second.toFloat())
        }

        private fun ease(p: Float): Float = ((1 - cos(p * PI)) / 2).toFloat()

        private fun scaleFor(phase: BreathPhase, p: Float): Float = when (phase) {
            BreathPhase.INHALE -> 0.55f + 0.45f * ease(p)
            BreathPhase.HOLD -> 1f
            BreathPhase.EXHALE -> 1f - 0.45f * ease(p)
            BreathPhase.HOLD_OUT -> 0.55f
        }
    }
}
