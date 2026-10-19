package com.taichiflow.app.ui.challenges

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.taichiflow.app.data.repository.TaiChiRepository
import com.taichiflow.app.domain.engine.BalanceEngine
import com.taichiflow.app.domain.engine.BalanceSnapshot
import com.taichiflow.app.domain.engine.BreathState
import com.taichiflow.app.domain.engine.BreathingEngine
import com.taichiflow.app.domain.engine.ProgressEngine
import com.taichiflow.app.domain.engine.RhythmEngine
import com.taichiflow.app.domain.model.ActivityResult
import com.taichiflow.app.domain.model.ActivityType
import com.taichiflow.app.domain.model.Attribute
import com.taichiflow.app.domain.model.BreathPattern
import com.taichiflow.app.domain.model.Metric
import com.taichiflow.app.domain.model.MetricCodec
import com.taichiflow.app.domain.model.PracticeCategory
import com.taichiflow.app.domain.model.RewardSummary
import com.taichiflow.app.domain.usecase.AppSnapshot
import com.taichiflow.app.ui.components.ConfirmDialog
import com.taichiflow.app.ui.components.FlowBackdrop
import com.taichiflow.app.ui.components.KeepScreenOn
import com.taichiflow.app.ui.components.LiveDot
import com.taichiflow.app.ui.components.Overline
import com.taichiflow.app.ui.components.ProgressLine
import com.taichiflow.app.ui.components.RewardContent
import com.taichiflow.app.ui.components.SelectChip
import com.taichiflow.app.ui.navigation.AppNavigator
import com.taichiflow.app.ui.theme.TC
import com.taichiflow.app.ui.util.Fmt
import com.taichiflow.app.ui.util.appViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// =================================================================== Balance

data class BalanceUi(
    val phase: ChallengePhase = ChallengePhase.INTRO,
    val countdown: Int = 3,
    val snap: BalanceSnapshot? = null,
    val reward: RewardSummary? = null
)

class BalanceViewModel(private val repo: TaiChiRepository, private val dailyId: String?) : ViewModel() {
    private val _ui = MutableStateFlow(BalanceUi())
    val ui: StateFlow<BalanceUi> = _ui.asStateFlow()
    private var engine = BalanceEngine(DURATION)
    private var ix = 0f
    private var iy = 0f
    val title: String = dailyId?.let { repo.dailyChallenge(it)?.name } ?: "Balance Flow"

    fun start() {
        if (_ui.value.phase == ChallengePhase.COUNTDOWN || _ui.value.phase == ChallengePhase.RUNNING) return
        engine = BalanceEngine(DURATION)
        ix = 0f
        iy = 0f
        _ui.value = BalanceUi(phase = ChallengePhase.COUNTDOWN, countdown = 3)
        viewModelScope.launch {
            for (i in 3 downTo 1) {
                _ui.update { it.copy(countdown = i) }
                delay(1000)
            }
            _ui.update { it.copy(phase = ChallengePhase.RUNNING, snap = engine.snapshot(0f, 0f)) }
        }
    }

    fun setInput(x: Float, y: Float) {
        ix = x
        iy = y
    }

    fun onFrame(dt: Float) {
        if (_ui.value.phase != ChallengePhase.RUNNING) return
        engine.update(dt, ix, iy)
        _ui.update { it.copy(snap = engine.snapshot(ix, iy)) }
        if (engine.finished) finish()
    }

    private fun finish() {
        _ui.update { it.copy(phase = ChallengePhase.SAVING) }
        val r = engine.result()
        val duration = DURATION.roundToInt()
        val metrics = listOf(
            Metric("Balance", "${r.balance}"),
            Metric("Stability", "${r.stability}"),
            Metric("Control", "${r.control}"),
            Metric("Reaction", "${r.reaction}"),
            Metric("Zone exits", "${r.exits}"),
            Metric("Duration", Fmt.clock(duration))
        )
        viewModelScope.launch {
            val reward = repo.recordActivity(
                ActivityResult(
                    type = ActivityType.BALANCE,
                    sessionId = "balance",
                    name = title,
                    category = PracticeCategory.BALANCE,
                    durationSec = duration,
                    score = r.score,
                    scores = mapOf(
                        Attribute.BALANCE to r.balance.coerceAtLeast(1),
                        Attribute.STABILITY to r.stability.coerceAtLeast(1),
                        Attribute.FOCUS to r.control.coerceAtLeast(1),
                        Attribute.COORDINATION to r.reaction.coerceAtLeast(1)
                    ),
                    baseXp = ProgressEngine.challengeXp(r.score, duration),
                    details = MetricCodec.encode(metrics),
                    metrics = metrics
                ),
                dailyId
            )
            _ui.update { it.copy(phase = ChallengePhase.RESULT, reward = reward) }
        }
    }

    fun abandon() {
        _ui.value = BalanceUi()
    }

    companion object {
        const val DURATION = 45f
    }
}

@Composable
fun BalanceChallengeScreen(dailyId: String?, snapshot: AppSnapshot, navigator: AppNavigator) {
    val vm = appViewModel { BalanceViewModel(it.repository, dailyId) }
    val ui by vm.ui.collectAsStateWithLifecycle()
    var confirmQuit by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val running = ui.phase == ChallengePhase.RUNNING || ui.phase == ChallengePhase.COUNTDOWN
    BackHandler(enabled = running) { confirmQuit = true }
    FrameLoop(ui.phase == ChallengePhase.RUNNING, vm::onFrame)

    val inZone = ui.snap?.inZone
    LaunchedEffect(inZone) {
        if (snapshot.profile.haptics && inZone == false && ui.phase == ChallengePhase.RUNNING) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    val reward = ui.reward
    when (ui.phase) {
        ChallengePhase.INTRO -> ChallengeIntro(
            overline = if (dailyId != null) "Daily Flow" else "Challenge",
            title = vm.title,
            description = "Test your control. Keep the flow indicator inside the target zone while it drifts.",
            icon = Icons.Filled.GpsFixed,
            rules = listOf(
                "Press and hold anywhere inside the circle.",
                "Your finger's offset from the centre pushes the indicator that way.",
                "Keep it inside the red zone. The zone shrinks and the drift grows stronger.",
                "Scored on Balance, Stability, Control and Reaction."
            ),
            best = snapshot.bestFor(ActivityType.BALANCE),
            duration = "${BalanceViewModel.DURATION.roundToInt()} sec",
            onStart = vm::start,
            onBack = navigator::back
        )
        ChallengePhase.COUNTDOWN -> CountdownView(ui.countdown, vm.title, "Press and hold inside the circle to steer.")
        ChallengePhase.RUNNING -> {
            KeepScreenOn()
            val s = ui.snap
            Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                ChallengeTopBar(vm.title, onQuit = { confirmQuit = true })
                ProgressLine(s?.progress ?: 0f, height = 4.dp, animate = false)
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    LiveStat("Time", Fmt.clock(kotlin.math.ceil((s?.remainingSec ?: 0f).toDouble()).toInt()))
                    LiveStat("Balance", "${s?.liveBalance ?: 100}%")
                    LiveStat("Zone", if (s?.inZone != false) "IN" else "OUT")
                }
                Spacer(Modifier.height(16.dp))
                BalanceArena(s, vm::setInput, Modifier.weight(1f, fill = false))
                Spacer(Modifier.height(12.dp))
                Text("Hold and steer toward the centre", style = MaterialTheme.typography.bodySmall, color = TC.Muted)
            }
        }
        ChallengePhase.SAVING -> SavingView()
        ChallengePhase.RESULT -> if (reward != null) {
            RewardContent(reward, onDone = navigator::back, onAgain = vm::start, scoreLabel = "BALANCE SCORE")
        }
    }

    if (confirmQuit) {
        ConfirmDialog(
            "Quit challenge?", "This run will not be saved.", "Quit",
            onConfirm = { confirmQuit = false; vm.abandon(); navigator.back() },
            onDismiss = { confirmQuit = false }
        )
    }
}

@Composable
fun ChallengeTopBar(title: String, onQuit: () -> Unit, trailing: String? = null) {
    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        LiveDot()
        Text("  " + title.uppercase(), style = MaterialTheme.typography.labelLarge, color = TC.White, modifier = Modifier.weight(1f))
        if (trailing != null) Text(trailing, style = MaterialTheme.typography.labelMedium, color = TC.Muted)
        IconButton(onClick = onQuit) { Icon(Icons.Filled.Close, "Quit", tint = TC.Soft) }
    }
}

// =================================================================== Rhythm

data class RhythmUi(
    val phase: ChallengePhase = ChallengePhase.INTRO,
    val countdown: Int = 3,
    val position: Float = 0f,
    val activeZone: Int? = null,
    val pulse: Float = 0f,
    val progress: Float = 0f,
    val hits: Int = 0,
    val judged: Int = 0,
    val accuracy: Int = 100,
    val feedback: String? = null,
    val feedbackId: Int = 0,
    val reward: RewardSummary? = null
)

class RhythmViewModel(private val repo: TaiChiRepository, private val dailyId: String?) : ViewModel() {
    private val _ui = MutableStateFlow(RhythmUi())
    val ui: StateFlow<RhythmUi> = _ui.asStateFlow()
    private var engine = RhythmEngine(BEAT_SEC, BEATS)
    val zones: List<Float> = engine.zonePositions()
    val title: String = dailyId?.let { repo.dailyChallenge(it)?.name } ?: "Flow Rhythm"

    fun start() {
        if (_ui.value.phase == ChallengePhase.COUNTDOWN || _ui.value.phase == ChallengePhase.RUNNING) return
        engine = RhythmEngine(BEAT_SEC, BEATS)
        _ui.value = RhythmUi(phase = ChallengePhase.COUNTDOWN)
        viewModelScope.launch {
            for (i in 3 downTo 1) {
                _ui.update { it.copy(countdown = i) }
                delay(1000)
            }
            _ui.update { it.copy(phase = ChallengePhase.RUNNING) }
        }
    }

    fun onFrame(dt: Float) {
        if (_ui.value.phase != ChallengePhase.RUNNING) return
        val missed = engine.update(dt)
        _ui.update {
            it.copy(
                position = engine.position(),
                activeZone = engine.nextBeatIndex()?.let { k -> zoneIndexFor(k) },
                pulse = engine.beatPulse(),
                progress = engine.progress,
                hits = engine.hits,
                judged = engine.beatsJudged,
                accuracy = engine.liveAccuracy(),
                feedback = if (missed > 0) "Miss" else it.feedback,
                feedbackId = if (missed > 0) it.feedbackId + 1 else it.feedbackId
            )
        }
        if (engine.finished) finish()
    }

    /** Zone index (0..beatsPerSweep-1) where beat k lands, accounting for ping-pong sweeps. */
    private fun zoneIndexFor(k: Int): Int {
        val pos = engine.beatPosition(k)
        return zones.indices.minByOrNull { kotlin.math.abs(zones[it] - pos) } ?: 0
    }

    fun tap(): Boolean {
        if (_ui.value.phase != ChallengePhase.RUNNING) return false
        val q = engine.tap()
        _ui.update { it.copy(feedback = q.label, feedbackId = it.feedbackId + 1, hits = engine.hits) }
        return q.isHit
    }

    private fun finish() {
        _ui.update { it.copy(phase = ChallengePhase.SAVING) }
        val r = engine.result()
        val duration = engine.elapsed.roundToInt()
        val metrics = listOf(
            Metric("Rhythm", "${r.rhythm}%"),
            Metric("Accuracy", "${r.accuracy}%"),
            Metric("Consistency", "${r.consistency}%"),
            Metric("Beats hit", "${r.hits} / ${r.total}")
        )
        viewModelScope.launch {
            val reward = repo.recordActivity(
                ActivityResult(
                    type = ActivityType.RHYTHM,
                    sessionId = "rhythm",
                    name = title,
                    category = PracticeCategory.COORDINATION,
                    durationSec = duration,
                    score = r.rhythm,
                    scores = mapOf(
                        Attribute.RHYTHM to r.rhythm.coerceAtLeast(1),
                        Attribute.COORDINATION to r.accuracy.coerceAtLeast(1),
                        Attribute.FOCUS to r.consistency.coerceAtLeast(1)
                    ),
                    baseXp = ProgressEngine.challengeXp(r.rhythm, duration),
                    details = MetricCodec.encode(metrics),
                    metrics = metrics
                ),
                dailyId
            )
            _ui.update { it.copy(phase = ChallengePhase.RESULT, reward = reward) }
        }
    }

    fun abandon() {
        _ui.value = RhythmUi()
    }

    companion object {
        const val BEAT_SEC = 1.2f
        const val BEATS = 32
    }
}

@Composable
fun RhythmChallengeScreen(dailyId: String?, snapshot: AppSnapshot, navigator: AppNavigator) {
    val vm = appViewModel { RhythmViewModel(it.repository, dailyId) }
    val ui by vm.ui.collectAsStateWithLifecycle()
    var confirmQuit by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val hapticsOn = snapshot.profile.haptics
    val running = ui.phase == ChallengePhase.RUNNING || ui.phase == ChallengePhase.COUNTDOWN
    BackHandler(enabled = running) { confirmQuit = true }
    FrameLoop(ui.phase == ChallengePhase.RUNNING, vm::onFrame)
    val onTap by rememberUpdatedState {
        val hit = vm.tap()
        if (hapticsOn && hit) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    val reward = ui.reward
    when (ui.phase) {
        ChallengePhase.INTRO -> ChallengeIntro(
            overline = if (dailyId != null) "Daily Flow" else "Challenge",
            title = vm.title,
            description = "A timing drill for smooth, even transitions. Tap exactly when the indicator reaches each target zone.",
            icon = Icons.Filled.GraphicEq,
            rules = listOf(
                "The white indicator sweeps back and forth across the track.",
                "Tap anywhere on the screen when it crosses the centre of the highlighted zone.",
                "${RhythmViewModel.BEATS} beats. Missed and stray taps lower your score.",
                "Scored on Rhythm, Accuracy and Consistency."
            ),
            best = snapshot.bestFor(ActivityType.RHYTHM),
            duration = "${(RhythmViewModel.BEATS * RhythmViewModel.BEAT_SEC).roundToInt()} sec",
            onStart = vm::start,
            onBack = navigator::back
        )
        ChallengePhase.COUNTDOWN -> CountdownView(ui.countdown, vm.title, "Tap the screen on every beat.")
        ChallengePhase.RUNNING -> {
            KeepScreenOn()
            Box(
                Modifier.fillMaxSize().pointerInput(Unit) {
                    detectTapGestures(onPress = { onTap() })
                }
            ) {
                FlowBackdrop(intensity = 0.5f + ui.pulse * 0.6f)
                Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    ChallengeTopBar(vm.title, onQuit = { confirmQuit = true }, trailing = "${ui.judged}/${RhythmViewModel.BEATS}")
                    ProgressLine(ui.progress, height = 4.dp, animate = false)
                    Spacer(Modifier.height(24.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        LiveStat("Hits", "${ui.hits}")
                        LiveStat("Accuracy", "${ui.accuracy}%")
                    }
                    Spacer(Modifier.weight(1f))
                    FeedbackLabel(ui.feedback, ui.feedbackId)
                    Spacer(Modifier.height(24.dp))
                    RhythmTrack(ui.position, vm.zones, ui.activeZone, ui.pulse)
                    Spacer(Modifier.weight(1f))
                    Text("TAP ANYWHERE ON THE BEAT", style = MaterialTheme.typography.labelLarge, color = TC.Muted)
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
        ChallengePhase.SAVING -> SavingView()
        ChallengePhase.RESULT -> if (reward != null) {
            RewardContent(reward, onDone = navigator::back, onAgain = vm::start, scoreLabel = "RHYTHM SCORE")
        }
    }

    if (confirmQuit) {
        ConfirmDialog(
            "Quit challenge?", "This run will not be saved.", "Quit",
            onConfirm = { confirmQuit = false; vm.abandon(); navigator.back() },
            onDismiss = { confirmQuit = false }
        )
    }
}

// =================================================================== Breathing

data class BreathingUi(
    val phase: ChallengePhase = ChallengePhase.INTRO,
    val countdown: Int = 3,
    val patternIndex: Int = 0,
    val cycles: Int = 6,
    val state: BreathState? = null,
    val pressed: Boolean = false,
    val completedCycles: Int = 0,
    val consistency: Int = 100,
    val progress: Float = 0f,
    val reward: RewardSummary? = null
) {
    val pattern: BreathPattern get() = BreathPattern.presets[patternIndex].second
}

class BreathingViewModel(private val repo: TaiChiRepository, private val dailyId: String?) : ViewModel() {
    private val daily = dailyId?.let { repo.dailyChallenge(it) }
    val fixed: Boolean = daily != null
    val title: String = daily?.name ?: "Breathing Challenge"
    private val _ui = MutableStateFlow(
        BreathingUi(
            patternIndex = daily?.let { d -> BreathPattern.presets.indexOfFirst { it.second == d.breath }.coerceAtLeast(0) } ?: 0,
            cycles = daily?.cycles ?: 6
        )
    )
    val ui: StateFlow<BreathingUi> = _ui.asStateFlow()
    private var engine: BreathingEngine? = null
    private var pressed = false

    fun setPattern(i: Int) = _ui.update { if (fixed) it else it.copy(patternIndex = i) }
    fun setCycles(n: Int) = _ui.update { if (fixed) it else it.copy(cycles = n) }

    fun start() {
        if (_ui.value.phase == ChallengePhase.COUNTDOWN || _ui.value.phase == ChallengePhase.RUNNING) return
        val current = _ui.value
        engine = BreathingEngine(current.pattern, current.cycles)
        pressed = false
        _ui.value = current.copy(phase = ChallengePhase.COUNTDOWN, countdown = 3, state = null, reward = null, progress = 0f, completedCycles = 0, consistency = 100)
        viewModelScope.launch {
            for (i in 3 downTo 1) {
                _ui.update { it.copy(countdown = i) }
                delay(1000)
            }
            _ui.update { it.copy(phase = ChallengePhase.RUNNING, state = engine?.state()) }
        }
    }

    fun setPressed(value: Boolean) {
        pressed = value
        _ui.update { it.copy(pressed = value) }
    }

    fun onFrame(dt: Float) {
        val e = engine ?: return
        if (_ui.value.phase != ChallengePhase.RUNNING) return
        e.update(dt, pressed)
        _ui.update {
            it.copy(state = e.state(), completedCycles = e.completedCycles, consistency = e.liveConsistency, progress = e.progress)
        }
        if (e.finished) finish(e)
    }

    private fun finish(e: BreathingEngine) {
        _ui.update { it.copy(phase = ChallengePhase.SAVING) }
        val r = e.result()
        val pattern = e.pattern
        val metrics = listOf(
            Metric("Cycles", "${r.cycles} / ${r.targetCycles}"),
            Metric("Consistency", "${r.consistency}%"),
            Metric("Pattern", "${BreathPattern.nameOf(pattern)} ${pattern.label}"),
            Metric("Duration", Fmt.clock(r.durationSec))
        )
        viewModelScope.launch {
            val reward = repo.recordActivity(
                ActivityResult(
                    type = ActivityType.BREATHING,
                    sessionId = "breathing",
                    name = if (daily != null) title else "Breathing · ${BreathPattern.nameOf(pattern)} ${pattern.label}",
                    category = PracticeCategory.BREATHING,
                    durationSec = r.durationSec,
                    score = r.score,
                    scores = mapOf(Attribute.BREATHING to r.score.coerceAtLeast(1), Attribute.FOCUS to r.consistency.coerceAtLeast(1)),
                    baseXp = ProgressEngine.challengeXp(r.score, r.durationSec),
                    details = MetricCodec.encode(metrics),
                    metrics = metrics
                ),
                dailyId
            )
            _ui.update { it.copy(phase = ChallengePhase.RESULT, reward = reward) }
        }
    }

    fun abandon() {
        engine = null
        _ui.update { it.copy(phase = ChallengePhase.INTRO) }
    }
}

@Composable
fun BreathingChallengeScreen(dailyId: String?, snapshot: AppSnapshot, navigator: AppNavigator) {
    val vm = appViewModel { BreathingViewModel(it.repository, dailyId) }
    val ui by vm.ui.collectAsStateWithLifecycle()
    var confirmQuit by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val running = ui.phase == ChallengePhase.RUNNING || ui.phase == ChallengePhase.COUNTDOWN
    BackHandler(enabled = running) { confirmQuit = true }
    FrameLoop(ui.phase == ChallengePhase.RUNNING, vm::onFrame)

    val breathPhase = ui.state?.phase
    LaunchedEffect(breathPhase) {
        if (snapshot.profile.haptics && breathPhase != null && ui.phase == ChallengePhase.RUNNING) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val reward = ui.reward
    when (ui.phase) {
        ChallengePhase.INTRO -> ChallengeIntro(
            overline = if (dailyId != null) "Daily Flow" else "Challenge",
            title = vm.title,
            description = "Follow the expanding circle and keep your breath even. A pacing exercise for calm, controlled movement.",
            icon = Icons.Filled.Air,
            rules = listOf(
                "Press and hold the circle while it expands (inhale) and during the hold.",
                "Release as it contracts (exhale).",
                "Consistency measures how closely your touch follows the guide.",
                "Breathe comfortably. Stop if you feel light-headed."
            ),
            best = snapshot.bestFor(ActivityType.BREATHING),
            duration = Fmt.clock(ui.pattern.cycleSec * ui.cycles),
            onStart = vm::start,
            onBack = navigator::back,
            extra = {
                if (!vm.fixed) {
                    Column {
                        Overline("Pattern")
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            BreathPattern.presets.forEachIndexed { i, (name, p) ->
                                SelectChip("$name ${p.label}", ui.patternIndex == i, { vm.setPattern(i) })
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Overline("Cycles")
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(4, 6, 8, 10).forEach { n -> SelectChip("$n", ui.cycles == n, { vm.setCycles(n) }) }
                        }
                    }
                } else {
                    Text(
                        "Daily pattern: ${ui.pattern.label} · ${ui.cycles} cycles",
                        style = MaterialTheme.typography.titleSmall, color = TC.Ember
                    )
                }
            }
        )
        ChallengePhase.COUNTDOWN -> CountdownView(ui.countdown, vm.title, "Pattern ${ui.pattern.label}. Press and hold to inhale.")
        ChallengePhase.RUNNING -> {
            KeepScreenOn()
            Column(Modifier.fillMaxSize().background(TC.Black).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                ChallengeTopBar(vm.title, onQuit = { confirmQuit = true }, trailing = ui.pattern.label)
                ProgressLine(ui.progress, height = 4.dp, animate = false)
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    LiveStat("Cycles", "${ui.completedCycles}/${ui.cycles}")
                    LiveStat("Consistency", "${ui.consistency}%")
                }
                Spacer(Modifier.weight(1f))
                BreathCircle(ui.state, ui.pressed, vm::setPressed)
                Spacer(Modifier.height(16.dp))
                Text(
                    ui.state?.let { breathInstruction(it.phase) }?.uppercase() ?: "",
                    style = MaterialTheme.typography.labelLarge, color = TC.Ember, textAlign = TextAlign.Center
                )
                Spacer(Modifier.weight(1f))
            }
        }
        ChallengePhase.SAVING -> SavingView()
        ChallengePhase.RESULT -> if (reward != null) {
            RewardContent(reward, onDone = navigator::back, onAgain = vm::start, scoreLabel = "BREATH SCORE")
        }
    }

    if (confirmQuit) {
        ConfirmDialog(
            "Quit challenge?", "This session will not be saved.", "Quit",
            onConfirm = { confirmQuit = false; vm.abandon(); navigator.back() },
            onDismiss = { confirmQuit = false }
        )
    }
}
