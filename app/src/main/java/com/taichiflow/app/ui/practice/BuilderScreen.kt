package com.taichiflow.app.ui.practice

import android.widget.Toast
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.taichiflow.app.data.catalog.MovementCatalog
import com.taichiflow.app.data.repository.TaiChiRepository
import com.taichiflow.app.domain.model.BreathPattern
import com.taichiflow.app.ui.components.EmptyNote
import com.taichiflow.app.ui.components.FlowCard
import com.taichiflow.app.ui.components.Overline
import com.taichiflow.app.ui.components.PrimaryButton
import com.taichiflow.app.ui.components.ScreenHeader
import com.taichiflow.app.ui.components.SectionTitle
import com.taichiflow.app.ui.components.SelectChip
import com.taichiflow.app.ui.navigation.AppNavigator
import com.taichiflow.app.ui.theme.TC
import com.taichiflow.app.ui.util.Fmt
import com.taichiflow.app.ui.util.appViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class BuilderState(
    val name: String = "",
    val selected: List<String> = emptyList(),
    val workSec: Int = 90,
    val restSec: Int = 10,
    val rounds: Int = 1,
    val breathIndex: Int = 0,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null
) {
    val steps: Int get() = selected.size * rounds
    val totalSec: Int get() = steps * workSec + (steps - 1).coerceAtLeast(0) * restSec
    val breath: BreathPattern get() = BreathPattern.presets[breathIndex].second
}

class BuilderViewModel(private val repo: TaiChiRepository) : ViewModel() {
    private val _state = MutableStateFlow(BuilderState())
    val state: StateFlow<BuilderState> = _state.asStateFlow()

    fun setName(v: String) = _state.update { it.copy(name = v.take(40), error = null) }

    fun toggle(id: String) = _state.update { s ->
        if (id in s.selected) s.copy(selected = s.selected - id)
        else if (s.selected.size >= 12) s.copy(error = "A session can hold up to 12 movements.")
        else s.copy(selected = s.selected + id, error = null)
    }

    fun remove(index: Int) = _state.update { s -> s.copy(selected = s.selected.filterIndexed { i, _ -> i != index }) }

    fun moveUp(index: Int) = _state.update { s ->
        if (index <= 0 || index >= s.selected.size) s else {
            val l = s.selected.toMutableList()
            val tmp = l[index - 1]
            l[index - 1] = l[index]
            l[index] = tmp
            s.copy(selected = l)
        }
    }

    fun setWork(v: Int) = _state.update { it.copy(workSec = v) }
    fun setRest(v: Int) = _state.update { it.copy(restSec = v) }
    fun setRounds(v: Int) = _state.update { it.copy(rounds = v) }
    fun setBreath(i: Int) = _state.update { it.copy(breathIndex = i) }

    fun save() {
        val s = _state.value
        when {
            s.name.isBlank() -> _state.update { it.copy(error = "Give your session a name.") }
            s.selected.isEmpty() -> _state.update { it.copy(error = "Add at least one movement.") }
            s.saving -> Unit
            else -> {
                _state.update { it.copy(saving = true, error = null) }
                viewModelScope.launch {
                    repo.saveCustomSession(s.name, s.selected, s.workSec, s.restSec, s.rounds, s.breath)
                    _state.update { it.copy(saving = false, saved = true) }
                }
            }
        }
    }
}

@Composable
fun BuilderScreen(navigator: AppNavigator) {
    val vm = appViewModel { BuilderViewModel(it.repository) }
    val s by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(s.saved) {
        if (s.saved) {
            Toast.makeText(context, "Session saved", Toast.LENGTH_SHORT).show()
            navigator.back()
        }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Build session", subtitle = "Create your own practice", onBack = navigator::back)
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                OutlinedTextField(
                    value = s.name,
                    onValueChange = vm::setName,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Session name") },
                    shape = MaterialTheme.shapes.medium,
                    colors = appTextFieldColors()
                )
            }
            item {
                FlowCard(Modifier.fillMaxWidth()) {
                    StepSlider("Duration per movement", Fmt.clock(s.workSec), s.workSec, 30, 300, 15, vm::setWork)
                    StepSlider("Rest between movements", "${s.restSec}s", s.restSec, 0, 60, 5, vm::setRest)
                    StepSlider("Rounds", "${s.rounds}", s.rounds, 1, 6, 1, vm::setRounds)
                    Spacer(Modifier.height(8.dp))
                    Overline("Breathing pattern")
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BreathPattern.presets.forEachIndexed { i, (name, p) ->
                            SelectChip("$name ${p.label}", s.breathIndex == i, { vm.setBreath(i) })
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row {
                        Overline("Total", Modifier.weight(1f))
                        Text(
                            "${Fmt.clock(s.totalSec)} · ${s.steps} steps",
                            style = MaterialTheme.typography.titleMedium, color = TC.White
                        )
                    }
                }
            }
            item { SectionTitle("Movements (${s.selected.size})") }
            if (s.selected.isEmpty()) {
                item { EmptyNote("Tap + on movements below to add them in order.") }
            }
            itemsIndexed(s.selected, key = { i, id -> "sel_${id}_$i" }) { i, id ->
                val m = MovementCatalog.get(id)
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(TC.Blood).padding(start = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${i + 1}", style = MaterialTheme.typography.labelLarge, color = TC.Ember, modifier = Modifier.width(24.dp))
                    Text(m?.name ?: id, style = MaterialTheme.typography.titleSmall, color = TC.White, modifier = Modifier.weight(1f))
                    IconButton(onClick = { vm.moveUp(i) }, enabled = i > 0) {
                        Icon(Icons.Filled.KeyboardArrowUp, "Move up", tint = if (i > 0) TC.Soft else TC.Gray)
                    }
                    IconButton(onClick = { vm.remove(i) }) { Icon(Icons.Filled.Close, "Remove", tint = TC.Soft) }
                }
            }
            item { SectionTitle("Add movements") }
            items(MovementCatalog.all, key = { "all_${it.id}" }) { m ->
                val chosen = m.id in s.selected
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(TC.Charcoal)
                        .clickable { vm.toggle(m.id) }.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(m.name, style = MaterialTheme.typography.titleSmall, color = TC.White)
                        Text("${m.difficulty.label} · ${m.primary.label}", style = MaterialTheme.typography.bodySmall, color = TC.Muted)
                    }
                    Box(
                        Modifier.size(32.dp).clip(CircleShape).background(if (chosen) TC.Crimson else TC.DarkGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(if (chosen) Icons.Filled.Check else Icons.Filled.Add, if (chosen) "Selected" else "Add", tint = TC.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
        Column(Modifier.fillMaxWidth().background(TC.Charcoal).padding(horizontal = 20.dp, vertical = 12.dp)) {
            val error = s.error
            if (error != null) {
                Text(error, style = MaterialTheme.typography.bodySmall, color = TC.Ember)
                Spacer(Modifier.height(8.dp))
            }
            PrimaryButton(if (s.saving) "Saving…" else "Save session", vm::save, Modifier.fillMaxWidth(), icon = Icons.Filled.Save, enabled = !s.saving)
        }
    }
}

@Composable
fun StepSlider(label: String, valueText: String, value: Int, min: Int, max: Int, step: Int, onChange: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row {
            Overline(label, Modifier.weight(1f))
            Text(valueText, style = MaterialTheme.typography.titleSmall, color = TC.White)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { v -> onChange(((v - min) / step).roundToInt() * step + min) },
            valueRange = min.toFloat()..max.toFloat(),
            steps = ((max - min) / step - 1).coerceAtLeast(0),
            colors = SliderDefaults.colors(
                thumbColor = TC.Crimson,
                activeTrackColor = TC.Crimson,
                inactiveTrackColor = TC.Gray,
                activeTickColor = TC.Ember.copy(alpha = 0.4f),
                inactiveTickColor = TC.Line
            )
        )
    }
}
