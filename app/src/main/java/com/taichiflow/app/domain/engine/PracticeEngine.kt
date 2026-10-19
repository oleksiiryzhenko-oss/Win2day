package com.taichiflow.app.domain.engine

import com.taichiflow.app.domain.model.Attribute
import com.taichiflow.app.domain.model.Movement
import com.taichiflow.app.domain.model.PracticeSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.math.roundToInt

enum class StepPhase { READY, WORK, REST, DONE }

const val PRACTICE_READY_MS = 5_000L
private const val TICK_MS = 100L

data class PracticeStep(val movement: Movement, val round: Int, val indexInRound: Int)

data class PracticeState(
    val steps: List<PracticeStep> = emptyList(),
    val rounds: Int = 1,
    val movementsPerRound: Int = 0,
    val index: Int = 0,
    val phase: StepPhase = StepPhase.READY,
    val phaseElapsedMs: Long = 0L,
    val phaseDurationMs: Long = PRACTICE_READY_MS,
    val paused: Boolean = false,
    val totalElapsedMs: Long = 0L,
    val workDoneMs: Long = 0L,
    val pauses: Int = 0,
    val skips: Int = 0,
    val completedSteps: Set<Int> = emptySet(),
    val finished: Boolean = false
) {
    val current: PracticeStep? get() = steps.getOrNull(index)
    val next: PracticeStep? get() = steps.getOrNull(index + 1)
    val remainingMs: Long get() = (phaseDurationMs - phaseElapsedMs).coerceAtLeast(0L)
    val phaseProgress: Float
        get() = if (phaseDurationMs <= 0L) 1f else (phaseElapsedMs.toFloat() / phaseDurationMs).coerceIn(0f, 1f)
    val overallProgress: Float
        get() = if (steps.isEmpty()) 0f else ((index + if (phase == StepPhase.WORK) phaseProgress else 0f) / steps.size).coerceIn(0f, 1f)
}

data class PracticeOutcome(
    val durationSec: Int,
    val completion: Float,
    val scores: Map<Attribute, Int>,
    val score: Int,
    val completedMovementIds: List<String>,
    val pauses: Int,
    val skips: Int
)

/**
 * Runs a guided session: a READY countdown, then WORK/REST phases for each movement × round.
 * Owns its timer; UI only observes [state] and calls the control functions.
 */
class PracticeEngine(
    private val session: PracticeSession,
    movements: List<Movement>,
    private val scope: CoroutineScope
) {
    private val workMs = session.workSec * 1000L
    private val restMs = session.restSec * 1000L

    private val _state = MutableStateFlow(
        PracticeState(
            steps = buildList {
                for (round in 1..session.rounds) {
                    movements.forEachIndexed { i, m -> add(PracticeStep(m, round, i)) }
                }
            },
            rounds = session.rounds,
            movementsPerRound = movements.size
        )
    )
    val state: StateFlow<PracticeState> = _state.asStateFlow()

    private var job: Job? = null

    fun start() {
        if (job != null || _state.value.steps.isEmpty()) return
        job = scope.launch {
            var last = System.nanoTime()
            while (isActive) {
                delay(TICK_MS)
                val now = System.nanoTime()
                val dt = ((now - last) / 1_000_000L).coerceIn(0L, 1_000L)
                last = now
                _state.update { s -> if (s.paused || s.finished) s else advance(s, dt) }
                if (_state.value.finished) break
            }
        }
    }

    private fun advance(s: PracticeState, dt: Long): PracticeState {
        val effective = min(dt, s.remainingMs)
        var st = s.copy(
            phaseElapsedMs = s.phaseElapsedMs + dt,
            totalElapsedMs = if (s.phase == StepPhase.READY) s.totalElapsedMs else s.totalElapsedMs + dt,
            workDoneMs = if (s.phase == StepPhase.WORK) s.workDoneMs + effective else s.workDoneMs
        )
        if (st.phaseElapsedMs < st.phaseDurationMs) return st
        st = when (st.phase) {
            StepPhase.READY -> st.copy(phase = StepPhase.WORK, phaseElapsedMs = 0L, phaseDurationMs = workMs)
            StepPhase.WORK -> completeCurrent(st)
            StepPhase.REST -> st.copy(index = st.index + 1, phase = StepPhase.WORK, phaseElapsedMs = 0L, phaseDurationMs = workMs)
            StepPhase.DONE -> st
        }
        return st
    }

    private fun completeCurrent(st: PracticeState): PracticeState {
        val completed = st.completedSteps + st.index
        return when {
            st.index >= st.steps.lastIndex ->
                st.copy(completedSteps = completed, phase = StepPhase.DONE, finished = true, phaseElapsedMs = st.phaseDurationMs)
            restMs > 0 ->
                st.copy(completedSteps = completed, phase = StepPhase.REST, phaseElapsedMs = 0L, phaseDurationMs = restMs)
            else ->
                st.copy(completedSteps = completed, index = st.index + 1, phase = StepPhase.WORK, phaseElapsedMs = 0L, phaseDurationMs = workMs)
        }
    }

    fun pause() = _state.update { s ->
        if (s.paused || s.finished) s else s.copy(paused = true, pauses = s.pauses + 1)
    }

    fun resume() = _state.update { s -> s.copy(paused = false) }

    fun togglePause() {
        if (_state.value.paused) resume() else pause()
    }

    fun next() = _state.update { s ->
        if (s.finished) return@update s
        when (s.phase) {
            StepPhase.READY -> s.copy(phase = StepPhase.WORK, phaseElapsedMs = 0L, phaseDurationMs = workMs)
            StepPhase.WORK -> {
                val skipped = s.phaseElapsedMs < workMs / 2
                if (skipped) {
                    val moved = s.copy(skips = s.skips + 1)
                    if (s.index >= s.steps.lastIndex) {
                        moved.copy(phase = StepPhase.DONE, finished = true)
                    } else {
                        moved.copy(index = s.index + 1, phase = StepPhase.WORK, phaseElapsedMs = 0L, phaseDurationMs = workMs)
                    }
                } else {
                    val done = s.copy(completedSteps = s.completedSteps + s.index)
                    if (s.index >= s.steps.lastIndex) done.copy(phase = StepPhase.DONE, finished = true)
                    else done.copy(index = s.index + 1, phase = StepPhase.WORK, phaseElapsedMs = 0L, phaseDurationMs = workMs)
                }
            }
            StepPhase.REST -> s.copy(index = s.index + 1, phase = StepPhase.WORK, phaseElapsedMs = 0L, phaseDurationMs = workMs)
            StepPhase.DONE -> s
        }
    }

    fun previous() = _state.update { s ->
        if (s.finished || s.phase == StepPhase.READY) return@update s
        if (s.phase == StepPhase.WORK && s.phaseElapsedMs > 3_000L) {
            s.copy(phaseElapsedMs = 0L)
        } else if (s.index > 0) {
            s.copy(index = s.index - 1, phase = StepPhase.WORK, phaseElapsedMs = 0L, phaseDurationMs = workMs)
        } else {
            s.copy(phase = StepPhase.WORK, phaseElapsedMs = 0L, phaseDurationMs = workMs)
        }
    }

    fun finish() {
        _state.update { s -> if (s.finished) s else s.copy(finished = true, phase = StepPhase.DONE, paused = false) }
        job?.cancel()
    }

    fun stop() {
        job?.cancel()
    }

    /**
     * Scores reflect how completely and how steadily the session was followed
     * (completion, pauses, skips, difficulty). No motion sensors are used.
     */
    fun outcome(focusMode: Boolean): PracticeOutcome {
        val s = _state.value
        val totalWork = (s.steps.size * workMs).coerceAtLeast(1L)
        val completion = (s.workDoneMs.toFloat() / totalWork).coerceIn(0f, 1f)
        val diff = session.difficulty.rank
        val base = 60f + completion * 30f + diff * 2.5f
        val pausePenalty = min(s.pauses * 2f, 10f)
        val skipPenalty = min(s.skips * 4f, 16f)
        val attrs = (session.focus + Attribute.FOCUS + Attribute.FLOW).distinct()
        val scores = attrs.associateWith { a ->
            val v = when (a) {
                Attribute.FOCUS -> 70f + completion * 22f - s.pauses * 3f - s.skips * 2f + if (focusMode) 5f else 0f
                Attribute.FLOW -> 62f + completion * 30f + diff * 2f - skipPenalty
                else -> base - pausePenalty * 0.6f - skipPenalty * 0.6f + variation(a)
            }
            v.roundToInt().coerceIn(10, 100)
        }
        val score = scores.values.average().roundToInt()
        val completedIds = s.completedSteps.mapNotNull { s.steps.getOrNull(it)?.movement?.id }.distinct()
        return PracticeOutcome(
            durationSec = (s.totalElapsedMs / 1000L).toInt(),
            completion = completion,
            scores = scores,
            score = score,
            completedMovementIds = completedIds,
            pauses = s.pauses,
            skips = s.skips
        )
    }

    private fun variation(a: Attribute): Float =
        (((a.ordinal * 7 + session.movementIds.size * 3 + session.workSec / 10) % 7) - 3).toFloat()
}
