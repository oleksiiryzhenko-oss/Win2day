package com.taichiflow.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.taichiflow.app.ui.theme.TC
import kotlin.math.PI
import kotlin.math.sin

// ---------------------------------------------------------------- Backdrop

/** Slowly turning rings and flowing lines: the app's motion signature. */
@Composable
fun FlowBackdrop(modifier: Modifier = Modifier, intensity: Float = 1f) {
    val transition = rememberInfiniteTransition(label = "backdrop")
    val turn by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(60_000, easing = LinearEasing)), label = "turn"
    )
    val wave by transition.animateFloat(
        initialValue = 0f, targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(14_000, easing = LinearEasing)), label = "wave"
    )
    Canvas(modifier = modifier.fillMaxSize()) {
        val c = Offset(size.width * 0.86f, size.height * 0.12f)
        val base = size.minDimension
        val radii = listOf(0.22f, 0.34f, 0.47f, 0.62f)
        radii.forEachIndexed { i, r ->
            val rad = base * r
            val start = turn * (if (i % 2 == 0) 1f else -0.7f) + i * 40f
            drawArc(
                color = TC.Crimson.copy(alpha = (0.18f - i * 0.03f) * intensity),
                startAngle = start, sweepAngle = 220f - i * 30f, useCenter = false,
                topLeft = Offset(c.x - rad, c.y - rad), size = Size(rad * 2, rad * 2),
                style = Stroke(width = (2.5f - i * 0.4f).dp.toPx(), cap = StrokeCap.Round)
            )
        }
        for (k in 0 until 3) {
            val path = Path()
            val yBase = size.height * (0.55f + k * 0.12f)
            val amp = size.height * 0.03f
            path.moveTo(0f, yBase)
            var x = 0f
            while (x <= size.width) {
                val y = yBase + sin(x / size.width * 2f * PI.toFloat() + wave + k) * amp
                path.lineTo(x, y)
                x += 12f
            }
            drawPath(path, TC.DeepRed.copy(alpha = (0.10f - k * 0.025f) * intensity), style = Stroke(width = 1.2.dp.toPx()))
        }
    }
}

// ---------------------------------------------------------------- Layout helpers

@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(start = if (onBack != null) 4.dp else 20.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TC.White)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(title.uppercase(), style = MaterialTheme.typography.headlineMedium, color = TC.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TC.Muted, maxLines = 2)
            }
        }
        actions()
    }
}

@Composable
fun Overline(text: String, modifier: Modifier = Modifier, color: Color = TC.Muted) {
    Text(text.uppercase(), modifier = modifier, style = MaterialTheme.typography.labelMedium, color = color)
}

@Composable
fun SectionTitle(title: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(3.dp).height(16.dp).background(TC.Crimson, RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(8.dp))
        Text(title.uppercase(), style = MaterialTheme.typography.labelLarge, color = TC.White, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            Text(
                action.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = TC.Ember,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onAction).padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
fun FlowCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    highlight: Boolean = false,
    padding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    val border = if (highlight) BorderStroke(1.dp, Brush.linearGradient(listOf(TC.Crimson, TC.DeepRed.copy(alpha = 0.3f))))
    else BorderStroke(1.dp, TC.Gray)
    Column(
        modifier = modifier
            .clip(shape)
            .background(if (highlight) Brush.verticalGradient(listOf(TC.Blood, TC.Charcoal)) else TC.cardGradient)
            .border(border, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content
    )
}

@Composable
fun Pill(text: String, modifier: Modifier = Modifier, color: Color = TC.Crimson, filled: Boolean = false) {
    Text(
        text.uppercase(),
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (filled) color else color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelSmall,
        color = if (filled) TC.White else color,
        maxLines = 1
    )
}

/** Selectable chip (own implementation to avoid experimental APIs). */
@Composable
fun SelectChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(50)
    Text(
        text,
        modifier = modifier
            .clip(shape)
            .background(if (selected) TC.Crimson else TC.DarkGray)
            .border(1.dp, if (selected) TC.Crimson else TC.Line, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        style = MaterialTheme.typography.labelMedium,
        color = if (selected) TC.White else TC.Soft
    )
}

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier, sub: String? = null, accent: Boolean = false) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (accent) TC.Blood else TC.DarkGray)
            .border(1.dp, if (accent) TC.DeepRed else TC.Gray, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Overline(label)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.headlineSmall, color = TC.White, maxLines = 1)
        if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall, color = TC.Muted, maxLines = 1)
    }
}

// ---------------------------------------------------------------- Buttons

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .height(56.dp)
            .clip(shape)
            .background(if (enabled) TC.redGradient else Brush.linearGradient(listOf(TC.Gray, TC.Gray)))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = TC.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text.uppercase(), style = MaterialTheme.typography.labelLarge, color = if (enabled) TC.White else TC.Muted, maxLines = 1)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .height(56.dp)
            .clip(shape)
            .background(TC.DarkGray)
            .border(1.dp, if (enabled) TC.Line else TC.Gray, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = if (enabled) TC.White else TC.Muted, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text.uppercase(), style = MaterialTheme.typography.labelLarge, color = if (enabled) TC.White else TC.Muted, maxLines = 1)
    }
}

@Composable
fun RoundIconButton(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 56.dp, primary: Boolean = false, enabled: Boolean = true) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (primary) TC.redGradient else Brush.linearGradient(listOf(TC.DarkGray, TC.DarkGray)))
            .border(1.dp, if (primary) TC.Crimson else TC.Line, CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = if (enabled) TC.White else TC.Muted, modifier = Modifier.size(size * 0.42f))
    }
}

// ---------------------------------------------------------------- Scores & progress

@Composable
fun AnimatedNumber(value: Int, style: TextStyle, modifier: Modifier = Modifier, color: Color = TC.White, suffix: String = "", durationMs: Int = 900) {
    val shown by animateIntAsState(value, animationSpec = tween(durationMs, easing = FastOutSlowInEasing), label = "number")
    Text("$shown$suffix", style = style, color = color, modifier = modifier, maxLines = 1)
}

/** Circular score indicator with an animated sweep and count-up. */
@Composable
fun ScoreRing(
    value: Int,
    modifier: Modifier = Modifier,
    size: Dp = 150.dp,
    stroke: Dp = 10.dp,
    label: String? = null,
    max: Int = 100,
    numberStyle: TextStyle = MaterialTheme.typography.displayMedium,
    color: Color = TC.Crimson,
    content: (@Composable BoxScope.() -> Unit)? = null
) {
    val sweep by animateFloatAsState((value.toFloat() / max).coerceIn(0f, 1f), tween(1100, easing = FastOutSlowInEasing), label = "ring")
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = stroke.toPx()
            val inset = sw / 2
            val arcSize = Size(this.size.width - sw, this.size.height - sw)
            drawArc(TC.Gray, 135f, 270f, false, Offset(inset, inset), arcSize, style = Stroke(sw, cap = StrokeCap.Round))
            drawArc(
                Brush.sweepGradient(listOf(TC.DeepRed, color, TC.Ember, TC.DeepRed)),
                135f, 270f * sweep, false, Offset(inset, inset), arcSize, style = Stroke(sw, cap = StrokeCap.Round)
            )
        }
        if (content != null) {
            content()
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedNumber(value, numberStyle)
                if (label != null) Overline(label)
            }
        }
    }
}

@Composable
fun MiniRing(progress: Float, modifier: Modifier = Modifier, size: Dp = 44.dp, stroke: Dp = 4.dp, text: String? = null, color: Color = TC.Crimson) {
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), tween(900), label = "mini")
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = stroke.toPx()
            val s = Size(this.size.width - sw, this.size.height - sw)
            drawArc(TC.Gray, -90f, 360f, false, Offset(sw / 2, sw / 2), s, style = Stroke(sw))
            drawArc(color, -90f, 360f * p, false, Offset(sw / 2, sw / 2), s, style = Stroke(sw, cap = StrokeCap.Round))
        }
        if (text != null) Text(text, style = MaterialTheme.typography.labelMedium, color = TC.White)
    }
}

@Composable
fun ProgressLine(progress: Float, modifier: Modifier = Modifier, height: Dp = 6.dp, color: Color = TC.Crimson, track: Color = TC.Gray, animate: Boolean = true) {
    val p = if (animate) animateFloatAsState(progress.coerceIn(0f, 1f), tween(900, easing = FastOutSlowInEasing), label = "line").value
    else progress.coerceIn(0f, 1f)
    Box(modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(50)).background(track)) {
        Box(
            Modifier.fillMaxWidth(p).height(height).clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(listOf(TC.DeepRed, color)))
        )
    }
}

@Composable
fun AttributeBar(label: String, value: Int, modifier: Modifier = Modifier, highlight: Boolean = false, caption: String? = null) {
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, color = if (highlight) TC.Ember else TC.Soft, modifier = Modifier.weight(1f))
            if (caption != null) {
                Text(caption, style = MaterialTheme.typography.bodySmall, color = TC.Muted)
                Spacer(Modifier.width(8.dp))
            }
            AnimatedNumber(value, MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }
        Spacer(Modifier.height(6.dp))
        ProgressLine(value / 100f, color = if (highlight) TC.Ember else TC.Crimson)
    }
}

@Composable
fun MetricGrid(metrics: List<Pair<String, String>>, modifier: Modifier = Modifier, columns: Int = 2) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        metrics.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (l, v) -> StatTile(l, v, Modifier.weight(1f)) }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

// ---------------------------------------------------------------- Misc

@Composable
fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}

@Composable
fun LoadingScreen(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(TC.Black), contentAlignment = Alignment.Center) {
        val t = rememberInfiniteTransition(label = "load")
        val r by t.animateFloat(0f, 360f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "spin")
        Box(Modifier.size(64.dp).rotate(r)) {
            CircularProgressIndicator(color = TC.Crimson, strokeWidth = 3.dp, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
fun EmptyNote(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(TC.Charcoal).padding(20.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = TC.Muted,
        textAlign = TextAlign.Center
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissText: String = "Cancel"
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TC.DarkGray,
        titleContentColor = TC.White,
        textContentColor = TC.Soft,
        title = { Text(title, style = MaterialTheme.typography.headlineSmall) },
        text = { Text(text, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmText.uppercase(), color = TC.Ember, style = MaterialTheme.typography.labelLarge) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(dismissText.uppercase(), color = TC.Soft, style = MaterialTheme.typography.labelLarge) }
        }
    )
}

/** Pulsing dot used to signal "live" state in challenges. */
@Composable
fun LiveDot(modifier: Modifier = Modifier, color: Color = TC.Crimson) {
    val t = rememberInfiniteTransition(label = "live")
    val a by t.animateFloat(0.3f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "alpha")
    Box(modifier.size(8.dp).clip(CircleShape).background(color.copy(alpha = a)))
}
