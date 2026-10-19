package com.taichiflow.app.ui.progress

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.taichiflow.app.data.catalog.MovementCatalog
import com.taichiflow.app.domain.engine.ProgressEngine
import com.taichiflow.app.domain.engine.RecommendationEngine
import com.taichiflow.app.domain.model.AchievementState
import com.taichiflow.app.domain.model.ActivityType
import com.taichiflow.app.domain.model.Attribute
import com.taichiflow.app.domain.model.HistoryRecord
import com.taichiflow.app.domain.model.MetricCodec
import com.taichiflow.app.domain.model.PersonalRecord
import com.taichiflow.app.domain.model.TrainingGroup
import com.taichiflow.app.domain.usecase.AppSnapshot
import com.taichiflow.app.ui.components.AttributeBar
import com.taichiflow.app.ui.components.EmptyNote
import com.taichiflow.app.ui.components.FlowBackdrop
import com.taichiflow.app.ui.components.FlowCard
import com.taichiflow.app.ui.components.HistoryRow
import com.taichiflow.app.ui.components.IconBadge
import com.taichiflow.app.ui.components.MetricGrid
import com.taichiflow.app.ui.components.MiniRing
import com.taichiflow.app.ui.components.Overline
import com.taichiflow.app.ui.components.Pill
import com.taichiflow.app.ui.components.PrimaryButton
import com.taichiflow.app.ui.components.ProgressLine
import com.taichiflow.app.ui.components.RecommendationCard
import com.taichiflow.app.ui.components.ScoreRing
import com.taichiflow.app.ui.components.ScreenHeader
import com.taichiflow.app.ui.components.SectionTitle
import com.taichiflow.app.ui.components.SelectChip
import com.taichiflow.app.ui.components.StreakCard
import com.taichiflow.app.ui.components.activityIcon
import com.taichiflow.app.ui.navigation.AppNavigator
import com.taichiflow.app.ui.practice.findSession
import com.taichiflow.app.ui.theme.TC
import com.taichiflow.app.ui.util.Fmt

@Composable
fun ProgressScreen(snapshot: AppSnapshot, navigator: AppNavigator) {
    val attrs = snapshot.attributes
    Box(Modifier.fillMaxSize()) {
        FlowBackdrop(intensity = 0.6f)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(Modifier.padding(top = 20.dp)) {
                Overline("Your development", color = TC.Ember)
                Text("PROGRESS", style = MaterialTheme.typography.displaySmall, color = TC.White)
            }

            FlowCard(Modifier.fillMaxWidth(), highlight = true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ScoreRing(snapshot.flowScore, size = 140.dp, label = "Flow score")
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Overline("Level ${snapshot.level.level}")
                        Text(snapshot.level.title.uppercase(), style = MaterialTheme.typography.headlineSmall, color = TC.White)
                        Spacer(Modifier.height(6.dp))
                        ProgressLine(snapshot.level.progress)
                        Spacer(Modifier.height(4.dp))
                        Text("${Fmt.number(snapshot.level.totalXp)} total XP", style = MaterialTheme.typography.bodySmall, color = TC.Muted)
                        Spacer(Modifier.height(8.dp))
                        Pill(snapshot.archetype.name, color = TC.Ember)
                    }
                }
            }

            SectionTitle("Attributes")
            FlowCard(Modifier.fillMaxWidth()) {
                Attribute.entries.chunked(4).forEach { row ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        row.forEach { a ->
                            val v = attrs[a] ?: 0
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                MiniRing(
                                    v / 100f, size = 58.dp, stroke = 5.dp, text = "$v",
                                    color = if (a == snapshot.recommendation.weakest) TC.Soft else TC.Crimson
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(a.label.uppercase(), style = MaterialTheme.typography.labelSmall, color = TC.Muted, maxLines = 1)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Attributes are a recency-weighted average of your recent sessions and challenges.",
                    style = MaterialTheme.typography.bodySmall, color = TC.Muted
                )
            }

            ActivityChart(snapshot)

            SectionTitle("Flow profile")
            val arch = snapshot.archetype
            FlowCard(Modifier.fillMaxWidth()) {
                Overline("Your style", color = TC.Ember)
                Text(arch.name, style = MaterialTheme.typography.headlineLarge, color = TC.White)
                Text(arch.tagline, style = MaterialTheme.typography.titleSmall, color = TC.Soft)
                Spacer(Modifier.height(8.dp))
                Text(arch.description, style = MaterialTheme.typography.bodyMedium, color = TC.Soft)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(Modifier.weight(1f)) {
                        Overline("Strengths")
                        arch.strengths.forEach { (a, v) ->
                            Text("${a.label} $v", style = MaterialTheme.typography.titleSmall, color = TC.White)
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Overline("Focus area")
                        Text(arch.focusArea.label, style = MaterialTheme.typography.titleSmall, color = TC.White)
                        Spacer(Modifier.height(4.dp))
                        Overline("Recommended")
                        Text(arch.recommendation, style = MaterialTheme.typography.bodySmall, color = TC.Soft)
                    }
                }
            }

            SectionTitle("Practice balance · 30 days")
            PracticeBalanceCard(snapshot, onOpen = { id -> navigator.session(id) })

            SectionTitle("Smart recommendations")
            RecommendationCard(snapshot.recommendation, onOpen = { navigator.session(it.id) })

            SectionTitle("Consistency")
            StreakCard(snapshot.streak)

            SectionTitle("Statistics")
            val practices = snapshot.history.count { it.type == ActivityType.PRACTICE }
            val avg = if (snapshot.history.isEmpty()) 0 else snapshot.history.map { it.score }.average().toInt()
            MetricGrid(
                listOf(
                    "Sessions" to "${snapshot.history.size}",
                    "Total time" to Fmt.minutes(snapshot.totalPracticeSec),
                    "Practices" to "$practices",
                    "Challenges" to "${snapshot.history.size - practices}",
                    "Average score" to "$avg",
                    "Movements" to "${snapshot.movementProgress.values.count { it.times > 0 }} / ${MovementCatalog.all.size}"
                )
            )

            SectionTitle("Personal records", action = "All", onAction = { navigator.records() })
            snapshot.records.take(4).forEach { RecordRow(it) }

            SectionTitle("Achievements ${snapshot.unlockedCount}/${snapshot.achievements.size}", action = "All", onAction = { navigator.achievements() })
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(snapshot.achievements.take(8)) { a -> AchievementBadge(a) }
            }

            SectionTitle("History", action = "All", onAction = { navigator.history() })
            if (snapshot.history.isEmpty()) EmptyNote("Your completed sessions will appear here.")
            snapshot.history.take(4).forEach { r -> HistoryRow(r, onClick = { navigator.historyDetail(r.id) }) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ActivityChart(snapshot: AppSnapshot) {
    val days = ProgressEngine.dailyMinutes(snapshot.history, snapshot.today, 14)
    val max = (days.maxOfOrNull { it.second } ?: 0).coerceAtLeast(10)
    FlowCard(Modifier.fillMaxWidth()) {
        Row {
            Overline("Minutes · last 14 days", Modifier.weight(1f))
            Text("${days.sumOf { it.second }} min", style = MaterialTheme.typography.labelMedium, color = TC.Soft)
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().height(110.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
            days.forEach { (date, minutes) ->
                val h by animateFloatAsState(minutes.toFloat() / max, tween(900), label = "bar")
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.fillMaxWidth().fillMaxHeight(h.coerceIn(0.02f, 1f) * 0.85f)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(if (date == snapshot.today) TC.Ember else if (minutes > 0) TC.Crimson else TC.Gray)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.labelSmall, color = TC.Muted)
                }
            }
        }
    }
}

@Composable
private fun PracticeBalanceCard(snapshot: AppSnapshot, onOpen: (String) -> Unit) {
    val balance = snapshot.trainingBalance
    val rec = snapshot.recommendation
    FlowCard(Modifier.fillMaxWidth()) {
        if (balance.isEmpty()) {
            Text("Complete a few sessions to see how your training is distributed.", style = MaterialTheme.typography.bodyMedium, color = TC.Muted)
        } else {
            TrainingGroup.entries.forEach { g ->
                val pct = RecommendationEngine.percent(balance[g] ?: 0f)
                AttributeBar(g.label + " %", pct, highlight = g == rec.neglected)
                Spacer(Modifier.height(10.dp))
            }
        }
        val message = rec.neglectedMessage
        val session = rec.session
        if (message != null) {
            Spacer(Modifier.height(4.dp))
            Text(message, style = MaterialTheme.typography.titleSmall, color = TC.Ember)
        }
        if (session != null) {
            Spacer(Modifier.height(6.dp))
            Overline("Suggested session")
            Spacer(Modifier.height(6.dp))
            PrimaryButton("${session.name} · ${session.minutes} min", { onOpen(session.id) }, Modifier.fillMaxWidth(), icon = Icons.Filled.PlayArrow)
        }
    }
}

@Composable
fun RecordRow(record: PersonalRecord) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(TC.Charcoal).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(Icons.Filled.MilitaryTech, size = 38)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(record.label.uppercase(), style = MaterialTheme.typography.labelLarge, color = TC.White)
            Text(record.timestamp?.let { "Achieved ${Fmt.date(it)}" } ?: "Not set yet", style = MaterialTheme.typography.bodySmall, color = TC.Muted)
        }
        Text(record.value, style = MaterialTheme.typography.headlineSmall, color = TC.Ember)
    }
}

@Composable
private fun AchievementBadge(a: AchievementState) {
    Column(
        Modifier.width(96.dp).clip(RoundedCornerShape(16.dp)).background(if (a.unlocked) TC.Blood else TC.Charcoal).padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(if (a.unlocked) TC.Crimson else TC.Gray),
            contentAlignment = Alignment.Center
        ) {
            Icon(if (a.unlocked) Icons.Filled.EmojiEvents else Icons.Filled.Lock, null, tint = TC.White, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(a.def.title, style = MaterialTheme.typography.labelSmall, color = if (a.unlocked) TC.White else TC.Muted, textAlign = TextAlign.Center, maxLines = 2)
    }
}

// =================================================================== History

@Composable
fun HistoryScreen(snapshot: AppSnapshot, navigator: AppNavigator) {
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    val list = snapshot.history.filter { filter == null || it.type.name == filter }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            "Practice history",
            subtitle = "${snapshot.history.size} sessions · ${Fmt.minutes(snapshot.totalPracticeSec)}",
            onBack = navigator::back
        )
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { SelectChip("All", filter == null, { filter = null }) }
                    items(ActivityType.entries.toList()) { t -> SelectChip(t.label, filter == t.name, { filter = t.name }) }
                }
            }
            if (list.isEmpty()) item { EmptyNote("No sessions recorded yet.") }
            items(list, key = { it.id }) { r -> HistoryRow(r, onClick = { navigator.historyDetail(r.id) }) }
        }
    }
}

@Composable
fun HistoryDetailScreen(id: Long, snapshot: AppSnapshot, navigator: AppNavigator) {
    val r = snapshot.history.firstOrNull { it.id == id }
    if (r == null) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader("Session", onBack = navigator::back)
            EmptyNote("This record no longer exists.", Modifier.padding(20.dp))
        }
        return
    }
    val metrics = MetricCodec.decode(r.details)
    Box(Modifier.fillMaxSize()) {
        FlowBackdrop(intensity = 0.6f)
        Column(Modifier.fillMaxSize()) {
            ScreenHeader("Session detail", onBack = navigator::back)
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(activityIcon(r.type), size = 48)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Overline(r.type.label, color = TC.Ember)
                        Text(r.name.uppercase(), style = MaterialTheme.typography.headlineMedium, color = TC.White)
                        Text(Fmt.full(r.timestamp), style = MaterialTheme.typography.bodySmall, color = TC.Muted)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (r.isRecord) Pill("Record · ${r.recordLabel}", color = TC.Ember)
                    if (r.daily) Pill("Daily Flow", color = TC.Soft)
                }
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    ScoreRing(r.score, size = 170.dp, label = "Score")
                }
                MetricGrid(
                    listOf(
                        "Duration" to Fmt.clock(r.durationSec),
                        "XP earned" to "+${r.xp}",
                        "Completion" to "${r.completion}%",
                        "Category" to (r.category?.label ?: r.type.label)
                    )
                )
                if (metrics.isNotEmpty()) {
                    SectionTitle("Details")
                    MetricGrid(metrics.map { it.label to it.value })
                }
                if (r.scores.isNotEmpty()) {
                    SectionTitle("Attribute scores")
                    FlowCard(Modifier.fillMaxWidth()) {
                        r.scores.entries.sortedByDescending { it.value }.forEach { (a, v) ->
                            AttributeBar(a.label, v)
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                }
                val moves = MovementCatalog.resolve(r.movementIds)
                if (moves.isNotEmpty()) {
                    SectionTitle("Movements")
                    moves.forEach { m ->
                        Text(
                            m.name,
                            style = MaterialTheme.typography.titleSmall, color = TC.White,
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(TC.Charcoal)
                                .clickable { navigator.movement(m.id) }.padding(12.dp)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            val repeat: (() -> Unit)? = repeatAction(r, snapshot, navigator)
            if (repeat != null) {
                PrimaryButton("Repeat", repeat, Modifier.fillMaxWidth().padding(20.dp), icon = Icons.Filled.Replay)
            }
        }
    }
}

private fun repeatAction(r: HistoryRecord, snapshot: AppSnapshot, navigator: AppNavigator): (() -> Unit)? = when (r.type) {
    ActivityType.BALANCE -> ({ navigator.balance() })
    ActivityType.RHYTHM -> ({ navigator.rhythm() })
    ActivityType.BREATHING -> ({ navigator.breathing() })
    ActivityType.FLOW -> ({ navigator.flowFive() })
    ActivityType.MASTER -> ({ navigator.flowMaster() })
    ActivityType.PRACTICE -> {
        val s = findSession(r.sessionId, snapshot)
        if (s != null) ({ navigator.guided(s.id) }) else null
    }
}

// =================================================================== Records

@Composable
fun RecordsScreen(snapshot: AppSnapshot, navigator: AppNavigator) {
    val broken = snapshot.history.filter { it.isRecord }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { ScreenHeader("Personal records", subtitle = "Your best results with dates", onBack = navigator::back) }
        items(snapshot.records) { rec -> Box(Modifier.padding(horizontal = 20.dp)) { RecordRow(rec) } }
        item { Box(Modifier.padding(horizontal = 20.dp)) { SectionTitle("Record timeline") } }
        if (broken.isEmpty()) item { EmptyNote("Beat a previous best to set your first record.", Modifier.padding(horizontal = 20.dp)) }
        items(broken, key = { it.id }) { r ->
            Row(
                Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(TC.Charcoal)
                    .clickable { navigator.historyDetail(r.id) }.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(r.recordLabel, style = MaterialTheme.typography.titleSmall, color = TC.White)
                    Text("${r.name} · ${Fmt.date(r.timestamp)}", style = MaterialTheme.typography.bodySmall, color = TC.Muted)
                }
                Text("${r.score}", style = MaterialTheme.typography.headlineSmall, color = TC.Ember)
            }
        }
    }
}

// =================================================================== Achievements

@Composable
fun AchievementsScreen(snapshot: AppSnapshot, navigator: AppNavigator) {
    Column(Modifier.fillMaxSize()) {
    ScreenHeader(
        "Achievements",
        subtitle = "${snapshot.unlockedCount} of ${snapshot.achievements.size} unlocked · +100 XP each",
        onBack = navigator::back
    )
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { ProgressLine(if (snapshot.achievements.isEmpty()) 0f else snapshot.unlockedCount.toFloat() / snapshot.achievements.size, height = 8.dp) }
        items(snapshot.achievements, key = { it.def.id }) { a ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(if (a.unlocked) TC.Blood else TC.Charcoal)
                    .border(1.dp, if (a.unlocked) TC.DeepRed else TC.Gray, RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(48.dp).clip(CircleShape).background(if (a.unlocked) TC.Crimson else TC.Gray),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(if (a.unlocked) Icons.Filled.EmojiEvents else Icons.Filled.Lock, null, tint = TC.White)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(a.def.title, style = MaterialTheme.typography.labelLarge, color = if (a.unlocked) TC.White else TC.Soft)
                    Text(a.def.description, style = MaterialTheme.typography.bodySmall, color = TC.Muted)
                    Spacer(Modifier.height(6.dp))
                    val unlockedAt = a.unlockedAt
                    if (unlockedAt != null) {
                        Text("Unlocked ${Fmt.date(unlockedAt)}", style = MaterialTheme.typography.labelSmall, color = TC.Ember)
                    } else {
                        ProgressLine(a.progress, height = 4.dp)
                        Spacer(Modifier.height(2.dp))
                        Text("${(a.progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = TC.Muted)
                    }
                }
            }
        }
    }
}
}
