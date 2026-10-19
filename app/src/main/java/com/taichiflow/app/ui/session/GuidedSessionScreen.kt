package com.taichiflow.app.ui.session

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.taichiflow.app.data.catalog.MovementCatalog
import com.taichiflow.app.data.repository.TaiChiRepository
import com.taichiflow.app.domain.engine.BreathState
import com.taichiflow.app.domain.engine.BreathingEngine
import com.taichiflow.app.domain.engine.PracticeEngine
import com.taichiflow.app.domain.engine.PracticeState
import com.taichiflow.app.domain.engine.ProgressEngine
import com.taichiflow.app.domain.engine.StepPhase
import com.taichiflow.app.domain.model.ActivityResult
import com.taichiflow.app.domain.model.ActivityType
import com.taichiflow.app.domain.model.Metric
import com.taichiflow.app.domain.model.MetricCodec
import com.taichiflow.app.domain.model.PracticeSession
import com.taichiflow.app.domain.model.RewardSummary
import com.taichiflow.app.domain.usecase.AppSnapshot
import com.taichiflow.app.ui.challenges.SavingView
import com.taichiflow.app.ui.components.ConfirmDialog
import com.taichiflow.app.ui.components.EmptyNote
import com.taichiflow.app.ui.components.FlowBackdrop
import com.taichiflow.app.ui.components.FlowCard
import com.taichiflow.app.ui.components.KeepScreenOn
import com.taichiflow.app.ui.components.LoadingScreen
import com.taichiflow.app.ui.components.Overline
import com.taichiflow.app.ui.components.PrimaryButton
import com.taichiflow.app.ui.components.ProgressLine
import com.taichiflow.app.ui.components.RewardContent
import com.taichiflow.app.ui.components.RoundIconButton
import com.taichiflow.app.ui.components.ScoreRing
import com.taichiflow.app.ui.components.ScreenHeader
import com.taichiflow.app.ui.components.SecondaryButton
import com.taichiflow.app.ui.navigation.AppNavigator
import com.taichiflow.app.ui.theme.TC
import com.taichiflow.app.ui.util.Fmt
import com.taichiflow.app.ui.util.appViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class GuidedUi(
    val loading: Boolean = true,
    val notFound: Boolean = false,
    val session: PracticeSession? = null,
    val practice: PracticeState? = null,
    val breath: BreathState? = null,
    val focusMode: Boolean = false,
    val saving: Boolean = false,
    val tooShort: Boolean = false,
    val reward: RewardSummary? = null
)

class GuidedSessionViewModel(
    private val repo: TaiChiRepository,
    private val sessionId: String,
    private val dailyId: String?,
    focusDefault: Boolean
) : ViewModel() {

    private val _ui = MutableStateFlow(GuidedUi(focusMode = focusDefault))
    val ui: StateFlow<GuidedUi> = _ui.asStateFlow()
    private var engine: PracticeEngine? = null
    private var completed = false

    init {
        viewModelScope.launch {
            val session = repo.session(sessionId)
            val movements = session?.let { MovementCatalog.resolve(it.movementIds) }.orEmpty()
            if (session == null || movements.isEmpty()) {
                _ui.update { it.copy(loading = false, notFound = true) }
                return@launch
            }
            val e = PracticeEngine(session, movements, viewModelScope)
            engine = e
            _ui.update { it.copy(loading = false, session = session, practice = e.state.value) }
            launch {
                e.state.collect { s ->
                    val breath = if (s.phase == StepPhase.WORK) BreathingEngine.phaseAt(session.breath, s.phaseElapsedMs / 1000f) else null
                    _ui.update { it.copy(practice = s, breath = breath) }
                }
            }
            e.start()
            e.state.first { it.finished }
            complete()
        }
    }

    fun togglePause() { engine?.togglePause() }
    fun next() { engine?.next() }
    fun previous() { engine?.previous() }
    fun finish() { engine?.finish() }
    fun toggleFocus() = _ui.update { it.copy(focusMode = !it.focusMode) }
    fun pauseForDialog() { engine?.pause() }
    fun resume() { engine?.resume() }
    fun abandon() {
        completed = true
        engine?.stop()
    }

    private fun complete() {
        if (completed) return
        completed = true
        val e = engine ?: return
        val session = _ui.value.session ?: return
        val state = e.state.value
        if (state.totalElapsedMs < MIN_SCORED_MS) {
            _ui.update { it.copy(tooShort = true) }
            return
        }
        _ui.update { it.copy(saving = true) }
        val focus = _ui.value.focusMode
        val o = e.outcome(focus)
        val completionPct = (o.completion * 100).roundToInt()
        val metrics = listOf(
            Metric("Completion", "$completionPct%"),
            Metric("Movements", "${state.completedSteps.size} / ${state.steps.size}"),
            Metric("Duration", Fmt.clock(o.durationSec)),
            Metric("Pauses", "${o.pauses}"),
            Metric("Skipped", "${o.skips}"),
            Metric("Focus mode", if (focus) "On" else "Off")
        )
        viewModelScope.launch {
            val reward = repo.recordActivity(
                ActivityResult(
                    type = ActivityType.PRACTICE,
                    sessionId = session.id,
                    name = session.name,
                    category = session.category,
                    durationSec = o.durationSec,
                    score = o.score,
                    scores = o.scores,
                    baseXp = ProgressEngine.practiceXp(o.durationSec, session.difficulty, o.score),
                    details = MetricCodec.encode(metrics),
                    metrics = metrics,
                    movementIds = o.completedMovementIds,
                    completion = completionPct
                ),
                dailyId
            )
            _ui.update { it.copy(saving = false, reward = reward) }
        }
    }

    override fun onCleared() {
        engine?.stop()
        super.onCleared()
    }

    companion object {
        const val MIN_SCORED_MS = 30_000L
    }
}

@Composable
fun GuidedSessionScreen(sessionId: String, dailyId: String?, snapshot: AppSnapshot, navigator: AppNavigator) {
    val vm = appViewModel { GuidedSessionViewModel(it.repository, sessionId, dailyId, snapshot.profile.focusModeDefault) }
    val ui by vm.ui.collectAsStateWithLifecycle()
    var confirmExit by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    val active = ui.reward == null && !ui.tooShort && !ui.notFound && !ui.saving
    BackHandler(enabled = active) {
        vm.pauseForDialog()
        confirmExit = true
    }

    val p = ui.practice
    LaunchedEffect(p?.index, p?.phase) {
        if (snapshot.profile.haptics && p != null && p.phase != StepPhase.READY) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val reward = ui.reward
    when {
        ui.loading -> LoadingScreen()
        ui.notFound -> Column(Modifier.fillMaxSize()) {
            ScreenHeader("Session", onBack = navigator::back)
            EmptyNote("This session could not be loaded.", Modifier.padding(20.dp))
        }
        reward != null -> RewardContent(reward, onDone = navigator::back)
        ui.saving -> SavingView()
        ui.tooShort -> TooShortView(onExit = navigator::back)
        p != null && ui.session != null -> {
            KeepScreenOn()
            if (ui.focusMode) {
                FocusModeView(ui, p, onPause = vm::togglePause, onNext = vm::next, onExitFocus = vm::toggleFocus)
            } else {
                SessionView(
                    ui = ui, p = p,
                    onClose = { vm.pauseForDialog(); confirmExit = true },
                    onPause = vm::togglePause, onNext = vm::next, onPrevious = vm::previous,
                    onFinish = vm::finish, onFocus = vm::toggleFocus
                )
            }
        }
    }

    if (confirmExit) {
        ConfirmDialog(
            title = "End session?",
            text = "Leaving now discards this session. Use Finish to save your progress instead.",
            confirmText = "Discard",
            dismissText = "Keep going",
            onConfirm = {
                confirmExit = false
                vm.abandon()
                navigator.back()
            },
            onDismiss = {
                confirmExit = false
                vm.resume()
            }
        )
    }
}

@Composable
private fun SessionView(
    ui: GuidedUi,
    p: PracticeState,
    onClose: () -> Unit,
    onPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onFinish: () -> Unit,
    onFocus: () -> Unit
) {
    val session = ui.session ?: return
    val step = p.current
    Box(Modifier.fillMaxSize()) {
        FlowBackdrop(intensity = 0.8f)
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Filled.Close, "Exit session", tint = TC.White) }
                Column(Modifier.weight(1f)) {
                    Text(session.name.uppercase(), style = MaterialTheme.typography.labelLarge, color = TC.White)
                    Text(
                        "Round ${step?.round ?: 1}/${p.rounds} · Movement ${(step?.indexInRound ?: 0) + 1}/${p.movementsPerRound}",
                        style = MaterialTheme.typography.bodySmall, color = TC.Muted
                    )
                }
                IconButton(onClick = onFocus) { Icon(Icons.Filled.CenterFocusStrong, "Focus mode", tint = TC.Soft) }
            }
            ProgressLine(p.overallProgress, Modifier.padding(horizontal = 20.dp), height = 4.dp, animate = false)

            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(16.dp))
                val phaseLabel = when (p.phase) {
                    StepPhase.READY -> "Get ready"
                    StepPhase.WORK -> if (p.paused) "Paused" else "Current movement"
                    StepPhase.REST -> "Rest"
                    StepPhase.DONE -> "Complete"
                }
                Overline(phaseLabel, color = if (p.paused) TC.Muted else TC.Ember)
                AnimatedContent(
                    targetState = if (p.phase == StepPhase.REST) "REST" else (step?.movement?.name ?: "").uppercase(),
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "move"
                ) { name ->
                    Text(name, style = MaterialTheme.typography.displaySmall, color = TC.White, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(16.dp))
                val secLeft = ((p.remainingMs + 999) / 1000).toInt()
                ScoreRing(
                    value = ((1f - p.phaseProgress) * 100).roundToInt(),
                    size = 200.dp, stroke = 12.dp
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(Fmt.clock(secLeft), style = MaterialTheme.typography.displayMedium, color = TC.White)
                        Overline(if (p.phase == StepPhase.WORK) "remaining" else if (p.phase == StepPhase.REST) "rest" else "starting")
                    }
                }
                Spacer(Modifier.height(16.dp))
                val breath = ui.breath
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    InfoTile("Breathing", breath?.let { "${it.phase.label} ${kotlin.math.ceil(it.remainingSec.toDouble()).toInt()}" } ?: session.breath.label, Modifier.weight(1f), highlight = breath != null)
                    InfoTile("Tempo", step?.movement?.tempo ?: "—", Modifier.weight(1f))
                    InfoTile("Step", "${p.index + 1}/${p.steps.size}", Modifier.weight(1f))
                }
                Spacer(Modifier.height(14.dp))
                if (step != null) {
                    FlowCard(Modifier.fillMaxWidth()) {
                        Overline("Instructions", color = TC.Ember)
                        Spacer(Modifier.height(4.dp))
                        Text(step.movement.instructions, style = MaterialTheme.typography.bodyMedium, color = TC.White)
                        Spacer(Modifier.height(10.dp))
                        Overline("Breath")
                        Text(step.movement.breathing, style = MaterialTheme.typography.bodySmall, color = TC.Soft)
                    }
                }
                Spacer(Modifier.height(10.dp))
                val next = if (p.phase == StepPhase.READY) p.current else p.next
                Text(
                    if (next != null) "NEXT · ${next.movement.name.uppercase()}" else "FINAL MOVEMENT",
                    style = MaterialTheme.typography.labelMedium, color = TC.Muted
                )
                Spacer(Modifier.height(16.dp))
            }

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoundIconButton(Icons.Filled.SkipPrevious, "Previous", onPrevious)
                RoundIconButton(if (p.paused) Icons.Filled.PlayArrow else Icons.Filled.Pause, if (p.paused) "Resume" else "Pause", onPause, size = 76.dp, primary = true)
                RoundIconButton(Icons.Filled.SkipNext, "Next", onNext)
            }
            SecondaryButton("Finish session", onFinish, Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 16.dp, top = 4.dp), icon = Icons.Filled.Flag)
        }
    }
}

@Composable
private fun InfoTile(label: String, value: String, modifier: Modifier = Modifier, highlight: Boolean = false) {
    Column(
        modifier.background(if (highlight) TC.Blood else TC.DarkGray, MaterialTheme.shapes.medium).padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Overline(label)
        Text(value, style = MaterialTheme.typography.titleSmall, color = TC.White, maxLines = 1, textAlign = TextAlign.Center)
    }
}

/** Minimal distraction-free display: movement, time, breath and what's next. */
@Composable
private fun FocusModeView(ui: GuidedUi, p: PracticeState, onPause: () -> Unit, onNext: () -> Unit, onExitFocus: () -> Unit) {
    val step = p.current
    Box(Modifier.fillMaxSize().background(TC.Black)) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Overline("Focus mode", Modifier.weight(1f), color = TC.DeepRed)
                IconButton(onClick = onExitFocus) { Icon(Icons.Filled.CenterFocusStrong, "Exit focus mode", tint = TC.Muted) }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (p.phase == StepPhase.REST) "REST" else (step?.movement?.name ?: "").uppercase(),
                    style = MaterialTheme.typography.headlineLarge, color = TC.White, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                Text(Fmt.clockMs(p.remainingMs), style = MaterialTheme.typography.displayLarge, color = if (p.paused) TC.Muted else TC.White)
                Spacer(Modifier.height(20.dp))
                Text(
                    (ui.breath?.phase?.label ?: if (p.phase == StepPhase.READY) "Get ready" else "Breathe").uppercase(),
                    style = MaterialTheme.typography.headlineMedium, color = TC.Crimson
                )
                Spacer(Modifier.height(8.dp))
                ProgressLine(p.phaseProgress, Modifier.width(160.dp), height = 2.dp, animate = false)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val next = if (p.phase == StepPhase.READY) p.current else p.next
                Text(
                    if (next != null) "NEXT · ${next.movement.name.uppercase()}" else "FINAL MOVEMENT",
                    style = MaterialTheme.typography.labelMedium, color = TC.Muted
                )
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    RoundIconButton(if (p.paused) Icons.Filled.PlayArrow else Icons.Filled.Pause, if (p.paused) "Resume" else "Pause", onPause, size = 64.dp)
                    RoundIconButton(Icons.Filled.SkipNext, "Next", onNext, size = 48.dp)
                }
            }
        }
    }
}

@Composable
private fun TooShortView(onExit: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        FlowBackdrop()
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Overline("Session ended", color = TC.Ember)
            Text("TOO SHORT TO SCORE", style = MaterialTheme.typography.headlineLarge, color = TC.White, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                "Practise for at least 30 seconds to record a session and earn XP.",
                style = MaterialTheme.typography.bodyMedium, color = TC.Soft, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            PrimaryButton("Back", onExit, Modifier.fillMaxWidth())
        }
    }
}
