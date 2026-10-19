package com.taichiflow.app.ui.practice

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import com.taichiflow.app.data.catalog.MovementCatalog
import com.taichiflow.app.data.catalog.SessionCatalog
import com.taichiflow.app.domain.model.BreathPattern
import com.taichiflow.app.domain.model.PracticeCategory
import com.taichiflow.app.domain.model.PracticeSession
import com.taichiflow.app.domain.usecase.AppSnapshot
import com.taichiflow.app.ui.components.ConfirmDialog
import com.taichiflow.app.ui.components.EmptyNote
import com.taichiflow.app.ui.components.FlowBackdrop
import com.taichiflow.app.ui.components.FlowCard
import com.taichiflow.app.ui.components.IconBadge
import com.taichiflow.app.ui.components.MetricGrid
import com.taichiflow.app.ui.components.Overline
import com.taichiflow.app.ui.components.Pill
import com.taichiflow.app.ui.components.PrimaryButton
import com.taichiflow.app.ui.components.ScreenHeader
import com.taichiflow.app.ui.components.SectionTitle
import com.taichiflow.app.ui.components.SelectChip
import com.taichiflow.app.ui.components.SessionRow
import com.taichiflow.app.ui.components.difficultyColor
import com.taichiflow.app.ui.main.MainViewModel
import com.taichiflow.app.ui.navigation.AppNavigator
import com.taichiflow.app.ui.theme.TC

@Composable
fun PracticeScreen(snapshot: AppSnapshot, navigator: AppNavigator) {
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = PracticeCategory.entries.firstOrNull { it.name == category }
    val sessions = SessionCatalog.all.filter { selected == null || it.category == selected }
    val recommendedId = snapshot.recommendation.session?.id

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(Modifier.padding(top = 20.dp)) {
                Overline("Structured training", color = TC.Ember)
                Text("PRACTICE", style = MaterialTheme.typography.displaySmall, color = TC.White)
                Text("${SessionCatalog.all.size} guided sessions · ${MovementCatalog.all.size} movements", style = MaterialTheme.typography.bodyMedium, color = TC.Muted)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickAction("Movement library", "${MovementCatalog.all.size} movements", Icons.AutoMirrored.Filled.MenuBook, Modifier.weight(1f)) { navigator.library() }
                QuickAction("Build session", "Your own flow", Icons.Filled.Add, Modifier.weight(1f)) { navigator.builder() }
            }
        }
        if (snapshot.customSessions.isNotEmpty()) {
            item { SectionTitle("My sessions") }
            items(snapshot.customSessions, key = { it.id }) { s ->
                SessionRow(s, onClick = { navigator.session(s.id) })
            }
        }
        item { SectionTitle("Categories") }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { SelectChip("All", selected == null, { category = null }) }
                items(PracticeCategory.entries.toList()) { c ->
                    SelectChip(c.label, selected == c, { category = c.name })
                }
            }
        }
        if (selected != null) {
            item { Text(selected.blurb, style = MaterialTheme.typography.bodyMedium, color = TC.Soft) }
        }
        items(sessions, key = { it.id }) { s ->
            SessionRow(s, onClick = { navigator.session(s.id) }, recommended = s.id == recommendedId)
        }
    }
}

@Composable
private fun QuickAction(title: String, sub: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    FlowCard(modifier, onClick = onClick, padding = PaddingValues(14.dp)) {
        IconBadge(icon, size = 38)
        Spacer(Modifier.height(10.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, color = TC.White)
        Text(sub, style = MaterialTheme.typography.bodySmall, color = TC.Muted)
    }
}

/** Finds a session by id from the catalogs or the user's custom sessions. */
fun findSession(id: String, snapshot: AppSnapshot): PracticeSession? = when {
    id.startsWith("custom_") -> snapshot.customSessions.firstOrNull { it.id == id }
    id.startsWith("move_") -> SessionCatalog.single(id.removePrefix("move_"))
    else -> SessionCatalog.get(id)
}

@Composable
fun SessionDetailScreen(sessionId: String, snapshot: AppSnapshot, main: MainViewModel, navigator: AppNavigator) {
    val session = findSession(sessionId, snapshot)
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    if (session == null) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader("Session", onBack = navigator::back)
            EmptyNote("This session is no longer available.", Modifier.padding(20.dp))
        }
        return
    }
    val completed = snapshot.history.filter { it.sessionId == session.id }
    val best = completed.maxOfOrNull { it.score }
    Box(Modifier.fillMaxSize()) {
        FlowBackdrop(intensity = 0.7f)
        Column(Modifier.fillMaxSize()) {
            ScreenHeader(
                "Session", onBack = navigator::back,
                actions = {
                    if (session.isCustom) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete session", tint = TC.Soft)
                        }
                    }
                }
            )
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column {
                    Overline(session.category.label, color = TC.Ember)
                    Text(session.name.uppercase(), style = MaterialTheme.typography.displaySmall, color = TC.White)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Pill(session.difficulty.label, color = difficultyColor(session.difficulty))
                        Pill(session.focusLabel, color = TC.Soft)
                    }
                    if (session.description.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(session.description, style = MaterialTheme.typography.bodyLarge, color = TC.Soft)
                    }
                }
                MetricGrid(
                    listOf(
                        "Duration" to "${session.minutes} min",
                        "Difficulty" to session.difficulty.label,
                        "Rounds" to "${session.rounds}",
                        "Work / Rest" to "${session.workSec}s / ${session.restSec}s",
                        "Breathing" to "${BreathPattern.nameOf(session.breath)} ${session.breath.label}",
                        "Completed" to if (best != null) "${completed.size}× · best $best" else "Not yet"
                    )
                )
                SectionTitle("Exercises")
                MovementCatalog.resolve(session.movementIds).forEachIndexed { i, m ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(TC.Charcoal)
                            .clickable { navigator.movement(m.id) }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(30.dp).clip(CircleShape).background(TC.Crimson.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                            Text("${i + 1}", style = MaterialTheme.typography.labelLarge, color = TC.Ember)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(m.name, style = MaterialTheme.typography.titleSmall, color = TC.White)
                            Text("${m.primary.label} · ${m.tempo}", style = MaterialTheme.typography.bodySmall, color = TC.Muted)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = TC.Muted)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            PrimaryButton(
                "Start", { navigator.guided(session.id) },
                Modifier.fillMaxWidth().padding(20.dp), icon = Icons.Filled.PlayArrow
            )
        }
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete session?",
            text = "\"${session.name}\" will be removed from your sessions. Your practice history is kept.",
            confirmText = "Delete",
            onConfirm = {
                confirmDelete = false
                main.deleteCustomSession(session.id) { }
                navigator.back()
            },
            onDismiss = { confirmDelete = false }
        )
    }
}
