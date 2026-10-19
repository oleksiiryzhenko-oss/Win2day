package com.taichiflow.app.domain.engine

import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

data class BalanceSnapshot(
    val x: Float,
    val y: Float,
    val zoneRadius: Float,
    val inZone: Boolean,
    val progress: Float,
    val remainingSec: Float,
    val liveBalance: Int,
    val trail: List<Pair<Float, Float>>,
    val inputX: Float,
    val inputY: Float
)

data class BalanceResult(
    val balance: Int,
    val stability: Int,
    val control: Int,
    val reaction: Int,
    val score: Int,
    val exits: Int
)

/**
 * Balance Flow: an indicator drifts inside a circular arena (unit radius). The player
 * presses and holds on the arena; the offset of the finger from the centre pushes the
 * indicator in that direction. Keep it inside the shrinking target zone.
 */
class BalanceEngine(
    val durationSec: Float = 45f,
    seed: Long = System.nanoTime()
) {
    private val random = Random(seed)
    private val p1 = random.nextFloat() * 6.28f
    private val p2 = random.nextFloat() * 6.28f
    private val p3 = random.nextFloat() * 6.28f
    private val p4 = random.nextFloat() * 6.28f

    var x = 0f
        private set
    var y = 0f
        private set
    private var vx = 0f
    private var vy = 0f
    var elapsed = 0f
        private set

    private var noiseX = 0f
    private var noiseY = 0f
    private var targetNoiseX = 0f
    private var targetNoiseY = 0f
    private var noiseTimer = 0f

    private var inZoneTime = 0f
    private var speedSum = 0f
    private var distSum = 0f
    private var inputChangeSum = 0f
    private var samples = 0
    private var lastIx = 0f
    private var lastIy = 0f
    private var outSince = -1f
    private val recoveries = mutableListOf<Float>()
    private var exits = 0
    private val trail = ArrayDeque<Pair<Float, Float>>()

    val progress: Float get() = (elapsed / durationSec).coerceIn(0f, 1f)
    val zoneRadius: Float get() = 0.40f - 0.10f * progress
    val finished: Boolean get() = elapsed >= durationSec
    val inZone: Boolean get() = hypot(x, y) <= zoneRadius

    fun update(dtRaw: Float, ix: Float, iy: Float) {
        if (finished) return
        val dt = dtRaw.coerceIn(0.001f, 0.05f)
        elapsed += dt

        noiseTimer -= dt
        if (noiseTimer <= 0f) {
            targetNoiseX = random.nextFloat() * 2f - 1f
            targetNoiseY = random.nextFloat() * 2f - 1f
            noiseTimer = 0.6f + random.nextFloat() * 0.8f
        }
        val blend = min(1f, dt * 2f)
        noiseX += (targetNoiseX - noiseX) * blend
        noiseY += (targetNoiseY - noiseY) * blend

        val t = elapsed
        val amp = 0.55f + 0.55f * progress
        val driftX = (sin(t * 0.8f + p1) + 0.5f * sin(t * 1.9f + p2) + noiseX) * amp
        val driftY = (cos(t * 0.7f + p3) + 0.5f * sin(t * 2.2f + p4) + noiseY) * amp

        val ax = driftX + ix * 3.2f - vx * 1.8f - x * 0.4f
        val ay = driftY + iy * 3.2f - vy * 1.8f - y * 0.4f
        vx += ax * dt
        vy += ay * dt
        x += vx * dt
        y += vy * dt

        val r = hypot(x, y)
        if (r > 1f) {
            x /= r
            y /= r
            val dot = vx * x + vy * y
            if (dot > 0f) {
                vx -= dot * x
                vy -= dot * y
            }
        }

        samples++
        speedSum += hypot(vx, vy)
        distSum += hypot(x, y)
        inputChangeSum += min(20f, hypot(ix - lastIx, iy - lastIy) / dt)
        lastIx = ix
        lastIy = iy

        if (inZone) {
            inZoneTime += dt
            if (outSince >= 0f) {
                recoveries += elapsed - outSince
                outSince = -1f
            }
        } else if (outSince < 0f) {
            outSince = elapsed
            exits++
        }

        trail.addLast(x to y)
        while (trail.size > 18) trail.removeFirst()
    }

    fun snapshot(ix: Float, iy: Float): BalanceSnapshot = BalanceSnapshot(
        x = x,
        y = y,
        zoneRadius = zoneRadius,
        inZone = inZone,
        progress = progress,
        remainingSec = max(0f, durationSec - elapsed),
        liveBalance = if (elapsed <= 0f) 100 else (inZoneTime / elapsed * 100f).roundToInt(),
        trail = trail.toList(),
        inputX = ix,
        inputY = iy
    )

    fun result(): BalanceResult {
        val e = elapsed.coerceAtLeast(0.001f)
        val n = max(1, samples)
        val ratio = (inZoneTime / e).coerceIn(0f, 1f)
        val balance = (ratio * 100f).roundToInt()
        val avgSpeed = speedSum / n
        val avgDist = distSum / n
        val stability = (100f - avgSpeed * 40f - avgDist * 40f).roundToInt().coerceIn(0, 100)
        val smooth = (100f - (inputChangeSum / n) * 9f).coerceIn(0f, 100f)
        val control = (smooth * 0.5f + ratio * 50f).roundToInt().coerceIn(0, 100)
        val pending = if (outSince >= 0f) elapsed - outSince else null
        val allRecoveries = recoveries + listOfNotNull(pending)
        val reaction = if (allRecoveries.isEmpty()) {
            if (ratio > 0.5f) 96 else 50
        } else {
            (100f - allRecoveries.average().toFloat() * 22f).roundToInt().coerceIn(0, 100)
        }
        val score = (balance * 0.4f + stability * 0.2f + control * 0.2f + reaction * 0.2f).roundToInt().coerceIn(0, 100)
        return BalanceResult(balance, stability, control, reaction, score, exits)
    }
}
