package com.taichiflow.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.taichiflow.app.domain.model.ActivityType
import com.taichiflow.app.domain.model.DailyChallenge
import com.taichiflow.app.domain.model.Difficulty
import com.taichiflow.app.domain.model.HistoryRecord
import com.taichiflow.app.domain.model.PracticeSession
import com.taichiflow.app.domain.model.Recommendation
import com.taichiflow.app.domain.model.StreakInfo
import com.taichiflow.app.domain.model.WeeklyGoalProgress
import com.taichiflow.app.ui.theme.TC
import com.taichiflow.app.ui.util.Fmt

fun difficultyColor(d: Difficulty): Color = when (d) {
    Difficulty.BEGINNER -> TC.Soft
    Difficulty.INTERMEDIATE -> TC.Crimson
    Difficulty.ADVANCED -> TC.Ember
}

fun activityIcon(type: ActivityType): ImageVector = when (type) {
    ActivityType.PRACTICE -> Icons.Filled.SelfImprovement
    ActivityType.BALANCE -> Icons.Filled.GpsFixed
    ActivityType.RHYTHM -> Icons.Filled.GraphicEq
    ActivityType.BREATHING -> Icons.Filled.Air
    ActivityType.FLOW -> Icons.Filled.Waves
    ActivityType.MASTER -> Icons.Filled.MilitaryTech
}

@Composable
fun IconBadge(icon: ImageVector, modifier: Modifier = Modifier, size: Int = 44, accent: Boolean = false) {
    Box(
        modifier.size(size.dp).clip(RoundedCornerShape(14.dp))
            .background(if (accent) TC.Crimson else TC.Crimson.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = if (accent) TC.White else TC.Ember, modifier = Modifier.size((size * 0.5f).dp))
    }
}

@Composable
fun StreakWeekRow(streak: StreakInfo, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        streak.week.forEach { day ->
            val scale by animateFloatAsState(if (day.done) 1f else 0.86f, tween(500), label = "day")
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(day.label, style = MaterialTheme.typography.labelSmall, color = if (day.isToday) TC.Ember else TC.Muted)
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier.size(34.dp).scale(scale).clip(CircleShape)
                        .background(if (day.done) TC.Crimson else TC.DarkGray)
                        .border(1.dp, if (day.isToday) TC.Ember else if (day.done) TC.Crimson else TC.Line, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (day.done) Icon(Icons.Filled.Check, contentDescription = "${day.label} done", tint = TC.White, modifier = Modifier.size(18.dp))
                    else if (!day.isFuture) Box(Modifier.size(4.dp).clip(CircleShape).background(TC.Muted))
                }
            }
        }
    }
}

@Composable
fun StreakCard(streak: StreakInfo, modifier: Modifier = Modifier) {
    FlowCard(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.LocalFireDepartment, null, tint = TC.Ember, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                AnimatedNumber(streak.current, MaterialTheme.typography.headlineMedium, suffix = " DAY FLOW STREAK")
                Text(
                    if (streak.practicedToday) "Today counts. Keep it going tomorrow."
                    else if (streak.current > 0) "Practise today to extend your streak."
                    else "Complete any session of 30 s or more to start a streak.",
                    style = MaterialTheme.typography.bodySmall, color = TC.Muted
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Overline("Best")
                Text("${streak.longest}d", style = MaterialTheme.typography.titleMedium, color = TC.White)
            }
        }
        Spacer(Modifier.height(14.dp))
        StreakWeekRow(streak)
    }
}

@Composable
fun DailyFlowCard(daily: DailyChallenge, done: Boolean, onStart: () -> Unit, modifier: Modifier = Modifier) {
    FlowCard(modifier.fillMaxWidth(), highlight = !done) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Overline("Daily Flow", color = TC.Ember, modifier = Modifier.weight(1f))
            Pill(if (done) "Completed" else "+${daily.xp} XP", color = if (done) TC.Soft else TC.Ember)
        }
        Spacer(Modifier.height(6.dp))
        Text(daily.name.uppercase(), style = MaterialTheme.typography.headlineSmall, color = TC.White)
        Text(daily.description, style = MaterialTheme.typography.bodyMedium, color = TC.Soft)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LabeledValue("Duration", "${daily.durationMin} min")
            LabeledValue("Goal", daily.goal, Modifier.weight(1f))
        }
        Spacer(Modifier.height(14.dp))
        if (done) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.TaskAlt, null, tint = TC.Ember, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("FLOW COMPLETED · +${daily.xp} XP · +1 STREAK", style = MaterialTheme.typography.labelLarge, color = TC.White, modifier = Modifier.weight(1f))
                Text(
                    "REPLAY", style = MaterialTheme.typography.labelMedium, color = TC.Muted,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onStart).padding(8.dp)
                )
            }
        } else {
            PrimaryButton("Start daily flow", onStart, Modifier.fillMaxWidth(), icon = Icons.Filled.Bolt)
        }
    }
}

@Composable
fun LabeledValue(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Overline(label)
        Text(value, style = MaterialTheme.typography.titleSmall, color = TC.White, maxLines = 2)
    }
}

@Composable
fun WeeklyGoalsCard(weekly: List<WeeklyGoalProgress>, modifier: Modifier = Modifier, compact: Boolean = false) {
    val done = weekly.count { it.completed }
    FlowCard(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Overline("Weekly progress")
                AnimatedNumber(done, MaterialTheme.typography.headlineLarge, suffix = " / ${weekly.size}")
                Text("Goals reset every Monday", style = MaterialTheme.typography.bodySmall, color = TC.Muted)
            }
            MiniRing(if (weekly.isEmpty()) 0f else done.toFloat() / weekly.size, size = 64.dp, stroke = 6.dp, text = "${if (weekly.isEmpty()) 0 else done * 100 / weekly.size}%")
        }
        Spacer(Modifier.height(12.dp))
        val list = if (compact) weekly.sortedBy { it.completed }.take(3) else weekly
        list.forEach { g ->
            Column(Modifier.padding(vertical = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (g.completed) {
                        Icon(Icons.Filled.Check, null, tint = TC.Ember, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(g.goal.title, style = MaterialTheme.typography.bodyMedium, color = if (g.completed) TC.Soft else TC.White, modifier = Modifier.weight(1f))
                    Text("${g.current.coerceAtMost(g.goal.target)}/${g.goal.target}", style = MaterialTheme.typography.labelMedium, color = TC.Muted)
                    Spacer(Modifier.width(8.dp))
                    Text("+${g.goal.xp}", style = MaterialTheme.typography.labelMedium, color = if (g.completed) TC.Ember else TC.Muted)
                }
                Spacer(Modifier.height(6.dp))
                ProgressLine(g.fraction, height = 4.dp)
            }
        }
    }
}

@Composable
fun SessionRow(session: PracticeSession, onClick: () -> Unit, modifier: Modifier = Modifier, recommended: Boolean = false) {
    FlowCard(modifier.fillMaxWidth(), onClick = onClick, highlight = recommended, padding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(TC.Crimson.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${session.minutes}", style = MaterialTheme.typography.headlineSmall, color = TC.White)
                    Text("MIN", style = MaterialTheme.typography.labelSmall, color = TC.Ember)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(session.name, style = MaterialTheme.typography.titleMedium, color = TC.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(session.focusLabel, style = MaterialTheme.typography.bodySmall, color = TC.Muted, maxLines = 1)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Pill(session.difficulty.label, color = difficultyColor(session.difficulty))
                    if (session.isCustom) Pill("Custom", color = TC.Soft)
                    if (recommended) Pill("Recommended", color = TC.Ember)
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = TC.Muted)
        }
    }
}

@Composable
fun HistoryRow(record: HistoryRecord, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(TC.Charcoal)
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(activityIcon(record.type), size = 40)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(record.name, style = MaterialTheme.typography.titleSmall, color = TC.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (record.isRecord) {
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Filled.MilitaryTech, "Personal record", tint = TC.Ember, modifier = Modifier.size(16.dp))
                }
            }
            Text("${Fmt.day(record.timestamp)} · ${Fmt.clock(record.durationSec)} · +${record.xp} XP", style = MaterialTheme.typography.bodySmall, color = TC.Muted)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${record.score}", style = MaterialTheme.typography.headlineSmall, color = TC.White)
            Text("SCORE", style = MaterialTheme.typography.labelSmall, color = TC.Muted)
        }
    }
}

@Composable
fun RecommendationCard(rec: Recommendation, onOpen: (PracticeSession) -> Unit, modifier: Modifier = Modifier) {
    FlowCard(modifier.fillMaxWidth()) {
        Row {
            Column(Modifier.weight(1f)) {
                Overline("Your strongest area")
                Text(rec.strongest.label.uppercase(), style = MaterialTheme.typography.headlineSmall, color = TC.White)
                AnimatedNumber(rec.strongestValue, MaterialTheme.typography.headlineMedium, color = TC.Ember)
            }
            Box(Modifier.width(1.dp).height(80.dp).background(TC.Gray))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Overline("Your weakest area")
                Text(rec.weakest.label.uppercase(), style = MaterialTheme.typography.headlineSmall, color = TC.White)
                AnimatedNumber(rec.weakestValue, MaterialTheme.typography.headlineMedium, color = TC.Soft)
            }
        }
        val s = rec.session
        if (s != null) {
            Spacer(Modifier.height(14.dp))
            Overline("Recommended session")
            Spacer(Modifier.height(4.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(TC.Blood)
                    .clickable { onOpen(s) }.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("${s.name} · ${s.minutes} min", style = MaterialTheme.typography.titleMedium, color = TC.White)
                    Text(s.focusLabel, style = MaterialTheme.typography.bodySmall, color = TC.Soft)
                }
                Icon(Icons.Filled.PlayArrow, "Open", tint = TC.White)
            }
            Spacer(Modifier.height(10.dp))
            Overline("Reason")
            Text(rec.reason, style = MaterialTheme.typography.bodyMedium, color = TC.Soft)
        }
    }
}
