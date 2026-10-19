package com.taichiflow.app.ui.challenges

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.taichiflow.app.domain.engine.BalanceSnapshot
import com.taichiflow.app.domain.engine.BreathPhase
import com.taichiflow.app.domain.engine.BreathState
import com.taichiflow.app.ui.components.FlowBackdrop
import com.taichiflow.app.ui.components.FlowCard
import com.taichiflow.app.ui.components.IconBadge
import com.taichiflow.app.ui.components.Overline
import com.taichiflow.app.ui.components.PrimaryButton
import com.taichiflow.app.ui.components.ScreenHeader
import com.taichiflow.app.ui.theme.TC
import kotlin.math.hypot
import kotlin.math.min

enum class ChallengePhase { INTRO, COUNTDOWN, RUNNING, SAVING, RESULT }

/** Drives a challenge engine from the display's frame clock while [running]. */
@Composable
fun FrameLoop(running: Boolean, onFrame: (Float) -> Unit) {
    val callback by rememberUpdatedState(onFrame)
    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        var last = -1L
        while (true) {
            withFrameNanos { t ->
                if (last >= 0L) callback(((t - last) / 1_000_000_000f).coerceIn(0f, 0.1f))
                last = t
            }
        }
    }
}

@Composable
fun ChallengeIntro(
    overline: String,
    title: String,
    description: String,
    icon: ImageVector,
    rules: List<String>,
    best: Int?,
    duration: String,
    onStart: () -> Unit,
    onBack: () -> Unit,
    extra: @Composable () -> Unit = {}
) {
    Box(Modifier.fillMaxSize()) {
        FlowBackdrop()
        Column(Modifier.fillMaxSize()) {
            ScreenHeader(overline, onBack = onBack)
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                IconBadge(icon, size = 64, accent = true)
                Text(title.uppercase(), style = MaterialTheme.typography.displaySmall, color = TC.White)
                Text(description, style = MaterialTheme.typography.bodyLarge, color = TC.Soft)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FlowCard(Modifier.weight(1f)) {
                        Overline("Duration")
                        Text(duration, style = MaterialTheme.typography.headlineSmall, color = TC.White)
                    }
                    FlowCard(Modifier.weight(1f)) {
                        Overline("Personal best")
                        Text(best?.toString() ?: "—", style = MaterialTheme.typography.headlineSmall, color = TC.Ember)
                    }
                }
                FlowCard(Modifier.fillMaxWidth()) {
                    Overline("How it works", color = TC.Ember)
                    Spacer(Modifier.height(8.dp))
                    rules.forEachIndexed { i, r ->
                        Row(Modifier.padding(vertical = 4.dp)) {
                            Text("${i + 1}", style = MaterialTheme.typography.labelLarge, color = TC.Ember, modifier = Modifier.width(22.dp))
                            Text(r, style = MaterialTheme.typography.bodyMedium, color = TC.White)
                        }
                    }
                }
                extra()
                Spacer(Modifier.height(8.dp))
            }
            PrimaryButton("Start", onStart, Modifier.fillMaxWidth().padding(20.dp), icon = Icons.Filled.PlayArrow)
        }
    }
}

@Composable
fun CountdownView(value: Int, title: String, subtitle: String? = null) {
    val scale by animateFloatAsState(if (value % 2 == 0) 1f else 1.08f, tween(300), label = "count")
    Box(Modifier.fillMaxSize().background(TC.Black), contentAlignment = Alignment.Center) {
        FlowBackdrop()
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Overline("Get ready", color = TC.Ember)
            Text(title.uppercase(), style = MaterialTheme.typography.headlineLarge, color = TC.White, textAlign = TextAlign.Center)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TC.Soft, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text("$value", style = MaterialTheme.typography.displayLarge.copy(fontSize = MaterialTheme.typography.displayLarge.fontSize * 1.6f), color = TC.Crimson, modifier = Modifier.scale(scale))
        }
    }
}

@Composable
fun SavingView() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            androidx.compose.material3.CircularProgressIndicator(color = TC.Crimson)
            Spacer(Modifier.height(12.dp))
            Overline("Calculating results")
        }
    }
}

@Composable
fun LiveStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineMedium, color = TC.White, maxLines = 1)
        Overline(label)
    }
}

/**
 * Circular arena. Press and drag: the finger's offset from the centre pushes the indicator.
 * Coordinates are normalised to the arena radius (-1..1).
 */
@Composable
fun BalanceArena(snapshot: BalanceSnapshot?, onInput: (Float, Float) -> Unit, modifier: Modifier = Modifier) {
    val input by rememberUpdatedState(onInput)
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    fun send(p: Offset) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val r = min(size.width, size.height) / 2f * 0.92f
                        var x = (p.x - cx) / r
                        var y = (p.y - cy) / r
                        val len = hypot(x, y)
                        if (len > 1f) {
                            x /= len
                            y /= len
                        }
                        input(x, y)
                    }
                    send(down.position)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        send(change.position)
                        change.consume()
                    }
                    input(0f, 0f)
                }
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val r = min(size.width, size.height) / 2f * 0.92f
            drawCircle(Brush.radialGradient(listOf(TC.Blood, TC.Black), c, r), r, c)
            drawCircle(TC.Line, r, c, style = Stroke(2.dp.toPx()))
            for (k in 1..3) drawCircle(TC.Gray, r * k / 4f, c, style = Stroke(1.dp.toPx()))
            drawLine(TC.Gray, Offset(c.x - r, c.y), Offset(c.x + r, c.y), 1.dp.toPx())
            drawLine(TC.Gray, Offset(c.x, c.y - r), Offset(c.x, c.y + r), 1.dp.toPx())
            val s = snapshot ?: return@Canvas
            val zr = s.zoneRadius * r
            drawCircle(TC.Crimson.copy(alpha = if (s.inZone) 0.22f else 0.10f), zr, c)
            drawCircle(if (s.inZone) TC.Crimson else TC.DeepRed, zr, c, style = Stroke(3.dp.toPx()))
            if (s.inputX != 0f || s.inputY != 0f) {
                val target = Offset(c.x + s.inputX * r, c.y + s.inputY * r)
                drawLine(TC.Soft.copy(alpha = 0.35f), c, target, 2.dp.toPx(), cap = StrokeCap.Round)
                drawCircle(TC.Soft.copy(alpha = 0.35f), 10.dp.toPx(), target, style = Stroke(2.dp.toPx()))
            }
            s.trail.forEachIndexed { i, (tx, ty) ->
                val a = (i + 1f) / s.trail.size
                drawCircle(TC.Ember.copy(alpha = 0.25f * a), 6.dp.toPx() * a, Offset(c.x + tx * r, c.y + ty * r))
            }
            val p = Offset(c.x + s.x * r, c.y + s.y * r)
            drawCircle((if (s.inZone) TC.White else TC.Ember).copy(alpha = 0.25f), 20.dp.toPx(), p)
            drawCircle(if (s.inZone) TC.White else TC.Ember, 11.dp.toPx(), p)
        }
    }
}

/** Horizontal track with target zones; the indicator sweeps back and forth. */
@Composable
fun RhythmTrack(position: Float, zones: List<Float>, activeZone: Int?, pulse: Float, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxWidth().height(96.dp)) {
        val pad = 18.dp.toPx()
        val w = size.width - pad * 2
        val cy = size.height / 2f
        drawLine(TC.Gray, Offset(pad, cy), Offset(pad + w, cy), 6.dp.toPx(), cap = StrokeCap.Round)
        val zoneHalf = w / zones.size.coerceAtLeast(1) * 0.16f
        zones.forEachIndexed { i, z ->
            val x = pad + z * w
            val active = i == activeZone
            drawRoundRect(
                color = if (active) TC.Crimson.copy(alpha = 0.35f + 0.5f * pulse) else TC.DeepRed.copy(alpha = 0.35f),
                topLeft = Offset(x - zoneHalf, cy - 30.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(zoneHalf * 2, 60.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx())
            )
            drawLine(if (active) TC.White else TC.Line, Offset(x, cy - 30.dp.toPx()), Offset(x, cy + 30.dp.toPx()), 2.dp.toPx())
        }
        val ix = pad + position.coerceIn(0f, 1f) * w
        drawCircle(TC.White.copy(alpha = 0.2f), 22.dp.toPx(), Offset(ix, cy))
        drawCircle(TC.White, 12.dp.toPx(), Offset(ix, cy))
    }
}

/** Ring that fills until the next transition; used for movement phases. */
@Composable
fun BeatRing(fill: Float, pulse: Float, label: String, modifier: Modifier = Modifier) {
    Box(modifier.size(220.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = 10.dp.toPx()
            val r = size.minDimension / 2f - sw
            val c = Offset(size.width / 2f, size.height / 2f)
            drawCircle(TC.Crimson.copy(alpha = 0.12f + 0.3f * pulse), r * (0.82f + 0.08f * pulse), c)
            drawCircle(TC.Gray, r, c, style = Stroke(sw))
            drawArc(
                TC.Crimson, -90f, 360f * fill.coerceIn(0f, 1f), false,
                Offset(c.x - r, c.y - r), androidx.compose.ui.geometry.Size(r * 2, r * 2),
                style = Stroke(sw, cap = StrokeCap.Round)
            )
        }
        Text(label, style = MaterialTheme.typography.headlineMedium, color = TC.White, textAlign = TextAlign.Center)
    }
}

fun breathInstruction(phase: BreathPhase): String = when (phase) {
    BreathPhase.INHALE -> "Press & hold · breathe in"
    BreathPhase.HOLD -> "Keep holding"
    BreathPhase.EXHALE -> "Release · breathe out"
    BreathPhase.HOLD_OUT -> "Stay released"
}

/** Expanding breathing circle. Press and hold to inhale/hold, release to exhale. */
@Composable
fun BreathCircle(state: BreathState?, pressed: Boolean, onPress: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val press by rememberUpdatedState(onPress)
    val scale = state?.scale ?: 0.55f
    val expected = state != null && (state.phase == BreathPhase.INHALE || state.phase == BreathPhase.HOLD)
    val match = pressed == expected
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    press(true)
                    tryAwaitRelease()
                    press(false)
                })
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val maxR = size.minDimension / 2f * 0.92f
            drawCircle(TC.Gray, maxR, c, style = Stroke(1.dp.toPx()))
            drawCircle(TC.Gray, maxR * 0.55f, c, style = Stroke(1.dp.toPx()))
            val r = maxR * scale
            drawCircle(Brush.radialGradient(listOf(TC.Crimson.copy(alpha = 0.55f), TC.DeepRed.copy(alpha = 0.15f)), c, r), r, c)
            drawCircle(if (match) TC.Crimson else TC.Muted, r, c, style = Stroke(3.dp.toPx()))
            if (state != null) {
                drawArc(
                    TC.White.copy(alpha = 0.8f), -90f, 360f * state.phaseProgress, false,
                    Offset(c.x - maxR, c.y - maxR), androidx.compose.ui.geometry.Size(maxR * 2, maxR * 2),
                    style = Stroke(3.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text((state?.phase?.label ?: "Ready").uppercase(), style = MaterialTheme.typography.displaySmall, color = TC.White)
            if (state != null) {
                Text("${kotlin.math.ceil(state.remainingSec.toDouble()).toInt()}", style = MaterialTheme.typography.headlineMedium, color = TC.Soft)
            }
            Box(Modifier.padding(top = 6.dp).size(10.dp).clip(CircleShape).background(if (pressed) TC.Crimson else TC.Gray))
        }
    }
}

@Composable
fun FeedbackLabel(text: String?, key: Int, modifier: Modifier = Modifier) {
    val alpha by animateFloatAsState(if (text != null) 1f else 0f, tween(200), label = "fb")
    val scale by animateFloatAsState(if (key % 2 == 0) 1f else 1.12f, tween(160), label = "fbScale")
    Text(
        (text ?: " ").uppercase(),
        modifier = modifier.scale(scale),
        style = MaterialTheme.typography.headlineMedium,
        color = when (text) {
            "Perfect", "Great" -> TC.Ember.copy(alpha = alpha)
            "Good" -> TC.White.copy(alpha = alpha)
            else -> TC.Muted.copy(alpha = alpha)
        }
    )
}
