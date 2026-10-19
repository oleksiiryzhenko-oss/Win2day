package com.taichiflow.app.ui.challenges

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.taichiflow.app.data.catalog.MovementCatalog
import com.taichiflow.app.data.repository.TaiChiRepository
import com.taichiflow.app.domain.engine.BalanceEngine
import com.taichiflow.app.domain.engine.BalanceSnapshot
import com.taichiflow.app.domain.engine.BreathState
import com.taichiflow.app.domain.engine.BreathingEngine
import com.taichiflow.app.domain.engine.FlowPhase
import com.taichiflow.app.domain.engine.FlowPrograms
import com.taichiflow.app.domain.engine.PhaseKind
import com.taichiflow.app.domain.engine.ProgressEngine
import com.taichiflow.app.domain.engine.RhythmEngine
import com.taichiflow.app.domain.model.ActivityResult
import com.taichiflow.app.domain.model.ActivityType
import com.taichiflow.app.domain.model.Attribute
import com.taichiflow.app.domain.model.Metric
import com.taichiflow.app.domain.model.MetricCodec
import com.taichiflow.app.domain.model.PracticeCategory
import com.taichiflow.app.domain.model.RewardSummary
import com.taichiflow.app.domain.usecase.AppSnapshot
import com.taichiflow.app.ui.components.ConfirmDialog
import com.taichiflow.app.ui.components.FlowCard
import com.taichiflow.app.ui.components.KeepScreenOn
import com.taichiflow.app.ui.components.Overline
import com.taichiflow.app.ui.components.ProgressLine
import com.taichiflow.app.ui.components.RewardContent
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

data class FlowUi(
    val phase: ChallengePhase = ChallengePhase.INTRO,
    val index: Int = 0,
    val countdown: Int = 3,
    val balance: BalanceSnapshot? = null,
    val rhythmPos: Float = 0f,
    val activeZone: Int? = null,
    val pulse: Float = 0f,
    val beatFill: Float = 0f,
    val breath: BreathState? = null,
    val pressed: Boolean = false,
    val phaseProgress: Float = 0f,
    val remainingSec: Int = 0,
    val live: Int = 100,
    val feedback: String? = null,
    val feedbackId: Int = 0,
    val phaseScores: List<Int> = emptyList(),
    val reward: RewardSummary? = null
)

/** Runs a sequence of phases, reusing the Balance, Rhythm and Breathing engines. */
class FlowChallengeViewModel(private val repo: TaiChiRepository, val master: Boolean) : ViewModel() {
    val phases: List<FlowPhase> = if (master) FlowPrograms.master else FlowPrograms.five
    val title: String = if (master) "Flow Master" else "5 Minute Flow"

    private val _ui = MutableStateFlow(FlowUi())
    val ui: StateFlow<FlowUi> = _ui.asStateFlow()

    private var balance: BalanceEngine? = null
    private var rhythm: RhythmEngine? = null
    private var breathing: BreathingEngine? = null
    private var ix = 0f
    private var iy = 0f
    private var pressed = false
    private var activeSec = 0f
    private val scores = mutableListOf<Int>()

    val zones: List<Float> = RhythmEngine().zonePositions()

    fun start() {
        if (_ui.value.phase == ChallengePhase.COUNTDOWN || _ui.value.phase == ChallengePhase.RUNNING) return
        scores.clear()
        activeSec = 0f
        _ui.value = FlowUi()
        beginPhase(0)
    }

    private fun beginPhase(index: Int) {
        val p = phases[index]
        ix = 0f
        iy = 0f
        pressed = false
        balance = null
        rhythm = null
        breathing = null
        when (p.kind) {
            PhaseKind.BALANCE -> balance = BalanceEngine(p.seconds.toFloat())
            PhaseKind.RHYTHM, PhaseKind.MOVEMENT ->
                rhythm = RhythmEngine(p.beatSec, (p.seconds / p.beatSec).toInt().coerceAtLeast(4))
            PhaseKind.BREATHING ->
                breathing = BreathingEngine(p.pattern, (p.seconds / p.pattern.cycleSec.coerceAtLeast(1)).coerceAtLeast(1))
        }
        _ui.update {
            it.copy(
                phase = ChallengePhase.COUNTDOWN, index = index, countdown = 3, balance = null, breath = null,
                feedback = null, phaseProgress = 0f, remainingSec = p.seconds, live = 100, pressed = false,
                phaseScores = scores.toList()
            )
        }
        viewModelScope.launch {
            for (i in 3 downTo 1) {
                _ui.update { it.copy(countdown = i) }
                delay(1000)
            }
            _ui.update {
                it.copy(
                    phase = ChallengePhase.RUNNING,
                    balance = balance?.snapshot(0f, 0f),
                    breath = breathing?.state()
                )
            }
        }
    }

    fun setInput(x: Float, y: Float) {
        ix = x
        iy = y
    }

    fun setPressed(v: Boolean) {
        pressed = v
        _ui.update { it.copy(pressed = v) }
    }

    fun tap(): Boolean {
        val r = rhythm ?: return false
        if (_ui.value.phase != ChallengePhase.RUNNING) return false
        val q = r.tap()
        _ui.update { it.copy(feedback = q.label, feedbackId = it.feedbackId + 1) }
        return q.isHit
    }

    fun onFrame(dt: Float) {
        if (_ui.value.phase != ChallengePhase.RUNNING) return
        val p = phases[_ui.value.index]
        activeSec += dt
        var done = false
        var phaseScore = 0
        when (p.kind) {
            PhaseKind.BALANCE -> {
                val e = balance ?: return
                e.update(dt, ix, iy)
                val s = e.snapshot(ix, iy)
                _ui.update { it.copy(balance = s, phaseProgress = s.progress, remainingSec = kotlin.math.ceil(s.remainingSec.toDouble()).toInt(), live = s.liveBalance) }
                if (e.finished) {
                    done = true
                    phaseScore = e.result().score
                }
            }
            PhaseKind.RHYTHM, PhaseKind.MOVEMENT -> {
                val e = rhythm ?: return
                val missed = e.update(dt)
                val beats = e.elapsed / e.beatSec - 0.5f
                val fill = ((beats % 1f) + 1f) % 1f
                val total = e.beatTime(e.totalBeats - 1) + e.window + 0.2f
                _ui.update {
                    it.copy(
                        rhythmPos = e.position(),
                        activeZone = e.nextBeatIndex()?.let { k -> zoneFor(e, k) },
                        pulse = e.beatPulse(),
                        beatFill = if (e.elapsed < e.beatSec * 0.5f) e.elapsed / (e.beatSec * 0.5f) else fill,
                        phaseProgress = e.progress,
                        remainingSec = kotlin.math.ceil((total - e.elapsed).toDouble()).toInt().coerceAtLeast(0),
                        live = e.liveAccuracy(),
                        feedback = if (missed > 0) "Miss" else it.feedback,
                        feedbackId = if (missed > 0) it.feedbackId + 1 else it.feedbackId
                    )
                }
                if (e.finished) {
                    done = true
                    phaseScore = e.result().rhythm
                }
            }
            PhaseKind.BREATHING -> {
                val e = breathing ?: return
                e.update(dt, pressed)
                _ui.update {
                    it.copy(
                        breath = e.state(), phaseProgress = e.progress,
                        remainingSec = kotlin.math.ceil((e.totalSec - e.elapsed).toDouble()).toInt(),
                        live = e.liveConsistency
                    )
                }
                if (e.finished) {
                    done = true
                    phaseScore = e.result().score
                }
            }
        }
        if (done) {
            scores += phaseScore
            val next = _ui.value.index + 1
            if (next < phases.size) beginPhase(next) else finish()
        }
    }

    private fun zoneFor(e: RhythmEngine, k: Int): Int {
        val pos = e.beatPosition(k)
        return zones.indices.minByOrNull { kotlin.math.abs(zones[it] - pos) } ?: 0
    }

    private fun finish() {
        _ui.update { it.copy(phase = ChallengePhase.SAVING, phaseScores = scores.toList()) }
        val perAttr = mutableMapOf<Attribute, MutableList<Int>>()
        phases.forEachIndexed { i, p ->
            val s = scores.getOrElse(i) { 0 }
            p.attributes.forEach { a -> perAttr.getOrPut(a) { mutableListOf() }.add(s) }
        }
        val overall = scores.average().roundToInt()
        val attrScores = perAttr.mapValues { it.value.average().roundToInt().coerceAtLeast(1) }.toMutableMap()
        attrScores[Attribute.FLOW] = overall.coerceAtLeast(1)
        val duration = activeSec.roundToInt()
        val metrics = phases.mapIndexed { i, p -> Metric(p.title, "${scores.getOrElse(i) { 0 }}") }
        viewModelScope.launch {
            val reward = repo.recordActivity(
                ActivityResult(
                    type = if (master) ActivityType.MASTER else ActivityType.FLOW,
                    sessionId = if (master) "flow_master" else "flow_five",
                    name = title,
                    category = if (master) PracticeCategory.ADVANCED else PracticeCategory.FLOW,
                    durationSec = duration,
                    score = overall,
                    scores = attrScores,
                    baseXp = ProgressEngine.challengeXp(overall, duration) + if (master) 150 else 60,
                    details = MetricCodec.encode(metrics),
                    metrics = metrics,
                    movementIds = phases.mapNotNull { it.movementId }.distinct()
                ),
                null
            )
            _ui.update { it.copy(phase = ChallengePhase.RESULT, reward = reward) }
        }
    }

    fun abandon() {
        balance = null
        rhythm = null
        breathing = null
        _ui.value = FlowUi()
    }
}

@Composable
fun FlowChallengeScreen(master: Boolean, snapshot: AppSnapshot, navigator: AppNavigator) {
    val vm = appViewModel(key = if (master) "flow_master" else "flow_five") { FlowChallengeViewModel(it.repository, master) }
    val ui by vm.ui.collectAsStateWithLifecycle()
    var confirmQuit by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val hapticsOn = snapshot.profile.haptics
    val running = ui.phase == ChallengePhase.RUNNING || ui.phase == ChallengePhase.COUNTDOWN
    BackHandler(enabled = running) { confirmQuit = true }
    FrameLoop(ui.phase == ChallengePhase.RUNNING, vm::onFrame)
    val onTap by rememberUpdatedState {
        if (vm.tap() && hapticsOn) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    val reward = ui.reward
    val phase = vm.phases.getOrNull(ui.index)
    when (ui.phase) {
        ChallengePhase.INTRO -> ChallengeIntro(
            overline = "Challenge",
            title = vm.title,
            description = if (master) "The complete test: seven stages covering centering, weight transfer, upper and lower body flow, rhythm, balance and a closing breath. Earn your MASTER FLOW SCORE."
            else "A complete flow in five stages. Your stage results combine into one FLOW SCORE.",
            icon = if (master) Icons.Filled.MilitaryTech else Icons.Filled.Waves,
            rules = vm.phases.map { "${it.title} · ${Fmt.clock(it.seconds)} · ${it.cue}" },
            best = snapshot.bestFor(if (master) ActivityType.MASTER else ActivityType.FLOW),
            duration = "${(FlowPrograms.totalSec(vm.phases) + vm.phases.size * 3 + 59) / 60} min",
            onStart = vm::start,
            onBack = navigator::back
        )
        ChallengePhase.COUNTDOWN -> if (phase != null) {
            CountdownView(ui.countdown, "Stage ${ui.index + 1}/${vm.phases.size} · ${phase.title}", phase.cue)
        }
        ChallengePhase.RUNNING -> if (phase != null) {
            KeepScreenOn()
            Column(Modifier.fillMaxSize().background(TC.Black).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                ChallengeTopBar(vm.title, onQuit = { confirmQuit = true }, trailing = Fmt.clock(ui.remainingSec))
                StageDots(vm.phases.size, ui.index)
                Spacer(Modifier.height(8.dp))
                ProgressLine(ui.phaseProgress, height = 4.dp, animate = false)
                Spacer(Modifier.height(12.dp))
                Overline("Stage ${ui.index + 1} · ${phase.title}", color = TC.Ember)
                Text(phase.cue, style = MaterialTheme.typography.bodyMedium, color = TC.Soft, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                when (phase.kind) {
                    PhaseKind.BALANCE -> {
                        LiveStat("Balance", "${ui.live}%")
                        Spacer(Modifier.height(12.dp))
                        BalanceArena(ui.balance, vm::setInput, Modifier.weight(1f, fill = false))
                    }
                    PhaseKind.RHYTHM -> Box(
                        Modifier.fillMaxWidth().weight(1f).pointerInput(Unit) { detectTapGestures(onPress = { onTap() }) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            LiveStat("Accuracy", "${ui.live}%")
                            Spacer(Modifier.height(20.dp))
                            FeedbackLabel(ui.feedback, ui.feedbackId)
                            Spacer(Modifier.height(20.dp))
                            RhythmTrack(ui.rhythmPos, vm.zones, ui.activeZone, ui.pulse)
                            Spacer(Modifier.height(20.dp))
                            Text("TAP ANYWHERE ON THE BEAT", style = MaterialTheme.typography.labelLarge, color = TC.Muted)
                        }
                    }
                    PhaseKind.MOVEMENT -> Box(
                        Modifier.fillMaxWidth().weight(1f).pointerInput(Unit) { detectTapGestures(onPress = { onTap() }) },
                        contentAlignment = Alignment.Center
                    ) {
                        val move = phase.movementId?.let { MovementCatalog.get(it) }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text((move?.name ?: phase.title).uppercase(), style = MaterialTheme.typography.headlineLarge, color = TC.White, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(16.dp))
                            BeatRing(ui.beatFill, ui.pulse, "${ui.live}%")
                            Spacer(Modifier.height(12.dp))
                            FeedbackLabel(ui.feedback, ui.feedbackId)
                            if (move != null) {
                                Spacer(Modifier.height(8.dp))
                                Text(move.instructions, style = MaterialTheme.typography.bodySmall, color = TC.Muted, textAlign = TextAlign.Center)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("TAP AS THE RING COMPLETES", style = MaterialTheme.typography.labelLarge, color = TC.Muted)
                        }
                    }
                    PhaseKind.BREATHING -> {
                        LiveStat("Consistency", "${ui.live}%")
                        Spacer(Modifier.height(12.dp))
                        BreathCircle(ui.breath, ui.pressed, vm::setPressed, Modifier.weight(1f, fill = false))
                        Spacer(Modifier.height(8.dp))
                        Text(ui.breath?.let { breathInstruction(it.phase) }?.uppercase() ?: "", style = MaterialTheme.typography.labelLarge, color = TC.Ember)
                    }
                }
                if (ui.phaseScores.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ui.phaseScores.forEachIndexed { i, s ->
                            Text("${vm.phases[i].title.take(10)} $s", style = MaterialTheme.typography.labelSmall, color = TC.Muted)
                        }
                    }
                }
            }
        }
        ChallengePhase.SAVING -> SavingView()
        ChallengePhase.RESULT -> if (reward != null) {
            RewardContent(reward, onDone = navigator::back, onAgain = vm::start, scoreLabel = if (master) "MASTER FLOW SCORE" else "FLOW SCORE")
        }
    }

    if (confirmQuit) {
        ConfirmDialog(
            "Quit challenge?", "Completed stages will not be saved.", "Quit",
            onConfirm = { confirmQuit = false; vm.abandon(); navigator.back() },
            onDismiss = { confirmQuit = false }
        )
    }
}

@Composable
private fun StageDots(count: Int, current: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { i ->
            Box(
                Modifier.size(if (i == current) 10.dp else 7.dp).clip(CircleShape)
                    .background(if (i < current) TC.Crimson else if (i == current) TC.White else TC.Gray)
            )
        }
    }
}

@Suppress("unused")
@Composable
private fun StageSummary(titles: List<String>, scores: List<Int>) {
    FlowCard(Modifier.fillMaxWidth()) {
        titles.forEachIndexed { i, t ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                Text(t, style = MaterialTheme.typography.bodyMedium, color = TC.Soft, modifier = Modifier.weight(1f))
                Text(scores.getOrNull(i)?.toString() ?: "—", style = MaterialTheme.typography.titleSmall, color = TC.White)
                Spacer(Modifier.width(4.dp))
            }
        }
    }
}
