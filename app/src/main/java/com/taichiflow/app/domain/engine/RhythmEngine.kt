package com.taichiflow.app.domain.engine

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

enum class TapQuality(val label: String, val isHit: Boolean) {
    PERFECT("Perfect", true),
    GREAT("Great", true),
    GOOD("Good", true),
    EARLY("Too early", false),
    LATE("Too late", false),
    MISS("Miss", false)
}

data class RhythmResult(
    val rhythm: Int,
    val accuracy: Int,
    val consistency: Int,
    val timing: Int,
    val hits: Int,
    val total: Int
)

/**
 * Flow Rhythm: an indicator sweeps back and forth across a track. Each sweep crosses
 * [beatsPerSweep] target zones; a beat happens when the indicator is at the centre of a
 * zone. The player taps on each beat.
 */
class RhythmEngine(
    val beatSec: Float = 1.2f,
    val totalBeats: Int = 32,
    val beatsPerSweep: Int = 4
) {
    var elapsed = 0f
        private set
    private val errors = arrayOfNulls<Float>(totalBeats)
    private val judged = BooleanArray(totalBeats)
    var hits = 0
        private set
    var misses = 0
        private set
    var strayTaps = 0
        private set

    val window: Float = min(0.32f, beatSec * 0.3f)

    fun beatTime(k: Int): Float = (k + 0.5f) * beatSec

    val finished: Boolean get() = elapsed >= beatTime(totalBeats - 1) + window + 0.2f

    val progress: Float get() = (elapsed / (beatTime(totalBeats - 1) + window + 0.2f)).coerceIn(0f, 1f)

    /** Advances time. Returns how many beats were missed during this step. */
    fun update(dt: Float): Int {
        if (finished) return 0
        elapsed += dt.coerceIn(0f, 0.1f)
        var newMisses = 0
        for (k in 0 until totalBeats) {
            if (!judged[k] && elapsed > beatTime(k) + window) {
                judged[k] = true
                misses++
                newMisses++
            }
        }
        return newMisses
    }

    fun tap(): TapQuality {
        val k = ((elapsed / beatSec) - 0.5f).roundToInt().coerceIn(0, totalBeats - 1)
        val err = elapsed - beatTime(k)
        if (judged[k] || abs(err) > window) {
            strayTaps++
            return if (err < 0f) TapQuality.EARLY else TapQuality.LATE
        }
        judged[k] = true
        errors[k] = err
        hits++
        val a = abs(err)
        return when {
            a < window * 0.3f -> TapQuality.PERFECT
            a < window * 0.6f -> TapQuality.GREAT
            else -> TapQuality.GOOD
        }
    }

    /** Indicator position on the track in 0..1 (ping-pong sweeps). */
    fun position(): Float {
        val sweepLen = beatsPerSweep * beatSec
        val sweep = floor(elapsed / sweepLen).toInt()
        val frac = (elapsed - sweep * sweepLen) / sweepLen
        return if (sweep % 2 == 0) frac else 1f - frac
    }

    fun zonePositions(): List<Float> = (0 until beatsPerSweep).map { (it + 0.5f) / beatsPerSweep }

    fun beatPosition(k: Int): Float {
        val sweep = k / beatsPerSweep
        val frac = ((k % beatsPerSweep) + 0.5f) / beatsPerSweep
        return if (sweep % 2 == 0) frac else 1f - frac
    }

    fun nextBeatIndex(): Int? = (0 until totalBeats).firstOrNull { !judged[it] }

    /** 1.0 exactly on a beat, falling to 0 half a beat away. */
    fun beatPulse(): Float {
        val k = ((elapsed / beatSec) - 0.5f).roundToInt().coerceIn(0, totalBeats - 1)
        val d = abs(elapsed - beatTime(k))
        return (1f - d / (beatSec * 0.5f)).coerceIn(0f, 1f)
    }

    val beatsJudged: Int get() = judged.count { it }

    fun liveAccuracy(): Int {
        val done = beatsJudged
        if (done == 0) return 100
        val sum = errors.filterNotNull().sumOf { (1.0 - 0.7 * abs(it) / window) }
        return (sum / done * 100).roundToInt().coerceIn(0, 100)
    }

    fun result(): RhythmResult {
        val errs = errors.filterNotNull()
        val timing = hits * 100 / totalBeats
        val accSum = errs.sumOf { (1.0 - 0.7 * abs(it) / window) }
        val accuracy = ((accSum / totalBeats) * 100).roundToInt().minus(strayTaps * 2).coerceIn(0, 100)
        val consistency = if (errs.size < 2) {
            timing / 2
        } else {
            val mean = errs.average()
            val sd = sqrt(errs.sumOf { (it - mean) * (it - mean) } / errs.size)
            val steadiness = (1.0 - sd / window * 1.2).coerceIn(0.0, 1.0)
            (steadiness * 100 * (0.4 + 0.6 * hits.toDouble() / totalBeats)).roundToInt().coerceIn(0, 100)
        }
        val rhythm = (timing * 0.35 + accuracy * 0.4 + consistency * 0.25).roundToInt().coerceIn(0, 100)
        return RhythmResult(rhythm, accuracy, consistency, timing, hits, totalBeats)
    }
}
