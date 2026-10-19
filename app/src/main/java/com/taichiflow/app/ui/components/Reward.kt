package com.taichiflow.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.taichiflow.app.domain.model.RewardSummary
import com.taichiflow.app.ui.theme.TC
import com.taichiflow.app.ui.util.Fmt
import kotlinx.coroutines.delay

/** Full-screen results with staged animations: score, XP, level, records, streak, achievements. */
@Composable
fun RewardContent(
    summary: RewardSummary,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    onAgain: (() -> Unit)? = null,
    scoreLabel: String = "SCORE"
) {
    var started by remember { mutableStateOf(false) }
    var stage by remember { mutableIntStateOf(0) }
    LaunchedEffect(summary) {
        delay(150)
        started = true
        repeat(summary.xpItems.size + 6) {
            delay(260)
            stage++
        }
    }
    val xpShown = summary.xpItems.take((stage).coerceAtMost(summary.xpItems.size)).sumOf { it.amount }
    val levelStage = stage > summary.xpItems.size

    Box(modifier.fillMaxSize().background(TC.Black)) {
        FlowBackdrop()
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(12.dp))
            Overline(summary.title, color = TC.Ember)
            Text(
                summary.activityName.uppercase(),
                style = MaterialTheme.typography.headlineLarge,
                color = TC.White,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            ScoreRing(if (started) summary.score else 0, size = 190.dp, stroke = 12.dp, label = scoreLabel, numberStyle = MaterialTheme.typography.displayLarge)

            if (summary.dailyCompleted) {
                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier.clip(MaterialTheme.shapes.medium).background(TC.redGradient).padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.TaskAlt, null, tint = TC.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    val dailyXp = summary.xpItems.firstOrNull { it.label == "Daily Flow" }?.amount ?: 0
                    Text("DAILY FLOW  +$dailyXp XP  +1 STREAK", style = MaterialTheme.typography.labelLarge, color = TC.White)
                }
            }

            if (summary.metrics.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                MetricGrid(summary.metrics.map { it.label to it.value }, Modifier.fillMaxWidth())
            }

            Spacer(Modifier.height(20.dp))
            FlowCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Overline("XP earned", Modifier.weight(1f))
                    AnimatedNumber(xpShown, MaterialTheme.typography.headlineMedium, color = TC.Ember, suffix = " XP", durationMs = 400)
                }
                Spacer(Modifier.height(8.dp))
                summary.xpItems.forEachIndexed { i, item ->
                    AnimatedVisibility(visible = stage > i, enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 2 }) {
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text(item.label, style = MaterialTheme.typography.bodyMedium, color = TC.Soft, modifier = Modifier.weight(1f))
                            Text("+${item.amount}", style = MaterialTheme.typography.titleSmall, color = TC.White)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                val lvl = if (levelStage) summary.levelAfter else summary.levelBefore
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("FLOW LEVEL ${lvl.level}", style = MaterialTheme.typography.labelLarge, color = TC.White, modifier = Modifier.weight(1f))
                    Text("${Fmt.number(lvl.xpInLevel)} / ${Fmt.number(lvl.xpForNext)}", style = MaterialTheme.typography.bodySmall, color = TC.Muted)
                }
                Spacer(Modifier.height(6.dp))
                ProgressLine(lvl.progress, height = 8.dp)
                AnimatedVisibility(visible = levelStage && summary.leveledUp, enter = fadeIn(tween(400))) {
                    LevelUpBanner(summary.levelAfter.level, summary.levelAfter.title)
                }
            }

            AnimatedVisibility(visible = stage > summary.xpItems.size + 1, enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 3 }) {
                Column(Modifier.fillMaxWidth().padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FlowCard(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Overline("Flow Score")
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text("${summary.flowBefore}", style = MaterialTheme.typography.headlineSmall, color = TC.Muted)
                                    Text("  →  ", style = MaterialTheme.typography.titleMedium, color = TC.Muted)
                                    AnimatedNumber(summary.flowAfter, MaterialTheme.typography.headlineMedium)
                                }
                            }
                            val diff = summary.flowAfter - summary.flowBefore
                            Pill(if (diff >= 0) "+$diff" else "$diff", color = if (diff >= 0) TC.Ember else TC.Muted)
                        }
                    }
                    if (summary.streakIncreased) {
                        RewardLine(Icons.Filled.LocalFireDepartment, "${summary.streak} DAY FLOW STREAK", "+1 streak day")
                    }
                    summary.newRecords.forEach { r ->
                        RewardLine(Icons.Filled.MilitaryTech, "NEW PERSONAL RECORD", r)
                    }
                    summary.achievements.forEach { a ->
                        RewardLine(Icons.Filled.EmojiEvents, "ACHIEVEMENT · ${a.title}", a.description)
                    }
                    summary.weeklyCompleted.forEach { w ->
                        RewardLine(Icons.Filled.TaskAlt, "WEEKLY GOAL COMPLETE", w)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            PrimaryButton("Done", onDone, Modifier.fillMaxWidth())
            if (onAgain != null) {
                Spacer(Modifier.height(10.dp))
                SecondaryButton("Try again", onAgain, Modifier.fillMaxWidth(), icon = Icons.Filled.Replay)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LevelUpBanner(level: Int, title: String) {
    val t = rememberInfiniteTransition(label = "lvl")
    val s by t.animateFloat(0.97f, 1.03f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "scale")
    Column(
        Modifier.fillMaxWidth().padding(top = 14.dp).scale(s).clip(MaterialTheme.shapes.medium)
            .background(TC.redGradient).padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("LEVEL UP", style = MaterialTheme.typography.labelLarge, color = TC.White)
        Text("FLOW LEVEL $level", style = MaterialTheme.typography.headlineLarge, color = TC.White)
        Text(title.uppercase(), style = MaterialTheme.typography.labelMedium, color = TC.White.copy(alpha = 0.8f))
    }
}

@Composable
private fun RewardLine(icon: ImageVector, title: String, text: String) {
    FlowCard(Modifier.fillMaxWidth(), highlight = true, padding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(TC.Crimson.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = TC.Ember, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.labelLarge, color = TC.White)
                Text(text, style = MaterialTheme.typography.bodySmall, color = TC.Soft)
            }
        }
    }
}
