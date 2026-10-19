package com.taichiflow.app.ui.practice

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.taichiflow.app.domain.model.Difficulty
import com.taichiflow.app.domain.model.Movement
import com.taichiflow.app.domain.usecase.AppSnapshot
import com.taichiflow.app.ui.components.EmptyNote
import com.taichiflow.app.ui.components.FlowBackdrop
import com.taichiflow.app.ui.components.FlowCard
import com.taichiflow.app.ui.components.MetricGrid
import com.taichiflow.app.ui.components.Overline
import com.taichiflow.app.ui.components.Pill
import com.taichiflow.app.ui.components.PrimaryButton
import com.taichiflow.app.ui.components.ScreenHeader
import com.taichiflow.app.ui.components.SectionTitle
import com.taichiflow.app.ui.components.SelectChip
import com.taichiflow.app.ui.components.difficultyColor
import com.taichiflow.app.ui.navigation.AppNavigator
import com.taichiflow.app.ui.theme.TC
import com.taichiflow.app.ui.util.Fmt

@Composable
fun appTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = TC.Crimson,
    unfocusedBorderColor = TC.Line,
    focusedTextColor = TC.White,
    unfocusedTextColor = TC.White,
    cursorColor = TC.Crimson,
    focusedLabelColor = TC.Ember,
    unfocusedLabelColor = TC.Muted,
    focusedContainerColor = TC.Charcoal,
    unfocusedContainerColor = TC.Charcoal
)

@Composable
fun MovementLibraryScreen(snapshot: AppSnapshot, navigator: AppNavigator) {
    var query by rememberSaveable { mutableStateOf("") }
    var level by rememberSaveable { mutableStateOf<String?>(null) }
    val q = query.trim().lowercase()
    val list = MovementCatalog.all.filter { m ->
        (level == null || m.difficulty.name == level) &&
            (q.isEmpty() || m.name.lowercase().contains(q) || m.primary.label.lowercase().contains(q) || m.secondary.label.lowercase().contains(q))
    }
    val practised = snapshot.movementProgress.values.count { it.times > 0 }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Movement library", subtitle = "${MovementCatalog.all.size} movements · $practised practised", onBack = navigator::back)
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Search name or benefit", color = TC.Muted) },
                    leadingIcon = { Icon(Icons.Filled.Search, null, tint = TC.Muted) },
                    shape = MaterialTheme.shapes.medium,
                    colors = appTextFieldColors()
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SelectChip("All", level == null, { level = null })
                    Difficulty.entries.forEach { d -> SelectChip(d.label, level == d.name, { level = d.name }) }
                }
            }
            if (list.isEmpty()) item { EmptyNote("No movements match your search.") }
            items(list, key = { it.id }) { m ->
                MovementRow(m, snapshot.movementProgress[m.id]?.times ?: 0) { navigator.movement(m.id) }
            }
        }
    }
}

@Composable
private fun MovementRow(m: Movement, times: Int, onClick: () -> Unit) {
    FlowCard(Modifier.fillMaxWidth(), onClick = onClick, padding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(m.name, style = MaterialTheme.typography.titleMedium, color = TC.White)
                Text("${m.primary.label} · ${m.secondary.label} · ${m.suggestedSec}s", style = MaterialTheme.typography.bodySmall, color = TC.Muted)
                Spacer(Modifier.height(6.dp))
                Pill(m.difficulty.label, color = difficultyColor(m.difficulty))
            }
            if (times > 0) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.CheckCircle, "Practised", tint = TC.Crimson, modifier = Modifier.size(22.dp))
                    Text("${times}×", style = MaterialTheme.typography.labelSmall, color = TC.Muted)
                }
            }
        }
    }
}

@Composable
fun MovementDetailScreen(movementId: String, snapshot: AppSnapshot, navigator: AppNavigator) {
    val m = MovementCatalog.get(movementId)
    if (m == null) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader("Movement", onBack = navigator::back)
            EmptyNote("Movement not found.", Modifier.padding(20.dp))
        }
        return
    }
    val progress = snapshot.movementProgress[m.id]
    Box(Modifier.fillMaxSize()) {
        FlowBackdrop(intensity = 0.6f)
        Column(Modifier.fillMaxSize()) {
            ScreenHeader("Movement", onBack = navigator::back)
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column {
                    Pill(m.difficulty.label, color = difficultyColor(m.difficulty))
                    Spacer(Modifier.height(6.dp))
                    Text(m.name.uppercase(), style = MaterialTheme.typography.displaySmall, color = TC.White)
                    Spacer(Modifier.height(6.dp))
                    Text(m.description, style = MaterialTheme.typography.bodyLarge, color = TC.Soft)
                }
                MetricGrid(
                    listOf(
                        "Primary benefit" to m.primary.label,
                        "Secondary benefit" to m.secondary.label,
                        "Suggested duration" to Fmt.clock(m.suggestedSec),
                        "Tempo" to m.tempo
                    )
                )
                FlowCard(Modifier.fillMaxWidth()) {
                    Overline("How to practise", color = TC.Ember)
                    Spacer(Modifier.height(6.dp))
                    Text(m.instructions, style = MaterialTheme.typography.bodyMedium, color = TC.White)
                    Spacer(Modifier.height(14.dp))
                    Overline("Breathing guidance", color = TC.Ember)
                    Spacer(Modifier.height(6.dp))
                    Text(m.breathing, style = MaterialTheme.typography.bodyMedium, color = TC.White)
                }
                SectionTitle("Common mistakes")
                m.mistakes.forEach { mistake ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(TC.Crimson))
                        Spacer(Modifier.width(10.dp))
                        Text(mistake, style = MaterialTheme.typography.bodyMedium, color = TC.Soft)
                    }
                }
                SectionTitle("Your progress")
                if (progress == null || progress.times == 0) {
                    EmptyNote("Not practised yet. Start a solo session below.")
                } else {
                    MetricGrid(
                        listOf(
                            "Completed" to "${progress.times}×",
                            "Total time" to Fmt.minutes(progress.totalSec),
                            "Last practised" to Fmt.day(progress.lastPracticed)
                        ),
                        columns = 3
                    )
                }
                Text(
                    "Instructional text is general demo guidance. Move within a comfortable range and stop if anything hurts.",
                    style = MaterialTheme.typography.bodySmall, color = TC.Muted
                )
                Spacer(Modifier.height(8.dp))
            }
            PrimaryButton(
                "Practise this movement", { navigator.guided("move_${m.id}") },
                Modifier.fillMaxWidth().padding(20.dp), icon = Icons.Filled.PlayArrow
            )
        }
    }
}
