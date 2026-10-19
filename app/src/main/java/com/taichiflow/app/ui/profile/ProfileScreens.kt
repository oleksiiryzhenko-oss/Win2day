package com.taichiflow.app.ui.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.taichiflow.app.BuildConfig
import com.taichiflow.app.domain.model.Difficulty
import com.taichiflow.app.domain.model.Goal
import com.taichiflow.app.domain.usecase.AppSnapshot
import com.taichiflow.app.ui.components.ConfirmDialog
import com.taichiflow.app.ui.components.FlowBackdrop
import com.taichiflow.app.ui.components.FlowCard
import com.taichiflow.app.ui.components.MetricGrid
import com.taichiflow.app.ui.components.Overline
import com.taichiflow.app.ui.components.Pill
import com.taichiflow.app.ui.components.ProgressLine
import com.taichiflow.app.ui.components.ScreenHeader
import com.taichiflow.app.ui.components.SectionTitle
import com.taichiflow.app.ui.components.SelectChip
import com.taichiflow.app.ui.main.MainViewModel
import com.taichiflow.app.ui.navigation.AppNavigator
import com.taichiflow.app.ui.practice.appTextFieldColors
import com.taichiflow.app.ui.theme.TC
import com.taichiflow.app.ui.util.Fmt

@Composable
fun ProfileScreen(snapshot: AppSnapshot, main: MainViewModel, navigator: AppNavigator) {
    val profile = snapshot.profile
    val context = LocalContext.current
    var editing by rememberSaveable { mutableStateOf(false) }
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    var confirmDemo by rememberSaveable { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        FlowBackdrop(intensity = 0.6f)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(Modifier.padding(top = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(72.dp).clip(CircleShape).background(TC.redGradient).border(2.dp, TC.Ember, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(initials(profile.name), style = MaterialTheme.typography.headlineLarge, color = TC.White)
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(profile.name.uppercase(), style = MaterialTheme.typography.headlineLarge, color = TC.White)
                    Text("${profile.experience.label} · Goal: ${profile.goal.label}", style = MaterialTheme.typography.bodyMedium, color = TC.Soft)
                }
                IconButton(onClick = { editing = true }) { Icon(Icons.Filled.Edit, "Edit profile", tint = TC.White) }
            }

            FlowCard(Modifier.fillMaxWidth(), highlight = true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Overline("Flow level")
                        Text("LEVEL ${snapshot.level.level}", style = MaterialTheme.typography.headlineLarge, color = TC.White)
                        Text(snapshot.level.title.uppercase(), style = MaterialTheme.typography.labelMedium, color = TC.Ember)
                    }
                    Pill(snapshot.archetype.name, color = TC.Ember)
                }
                Spacer(Modifier.height(10.dp))
                ProgressLine(snapshot.level.progress, height = 8.dp)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${Fmt.number(snapshot.level.xpInLevel)} / ${Fmt.number(snapshot.level.xpForNext)} XP · ${Fmt.number(snapshot.level.totalXp)} total",
                    style = MaterialTheme.typography.bodySmall, color = TC.Muted
                )
            }

            MetricGrid(
                listOf(
                    "Best score" to "${snapshot.bestScore}",
                    "Flow score" to "${snapshot.flowScore}",
                    "Current streak" to "${snapshot.streak.current} days",
                    "Longest streak" to "${snapshot.streak.longest} days",
                    "Achievements" to "${snapshot.unlockedCount}/${snapshot.achievements.size}",
                    "Sessions" to "${snapshot.history.size}",
                    "Total time" to Fmt.minutes(snapshot.totalPracticeSec),
                    "Daily Flows" to "${snapshot.stats.dailyCount}"
                )
            )

            SectionTitle("Your practice")
            LinkRow(Icons.Filled.EmojiEvents, "Achievements", "${snapshot.unlockedCount} unlocked") { navigator.achievements() }
            LinkRow(Icons.Filled.MilitaryTech, "Personal records", "Bests with dates") { navigator.records() }
            LinkRow(Icons.Filled.History, "Practice history", "${snapshot.history.size} sessions") { navigator.history() }

            SectionTitle("Settings")
            FlowCard(Modifier.fillMaxWidth()) {
                SettingSwitch("Focus Mode by default", "Start guided sessions in the minimal dark display.", profile.focusModeDefault, main::setFocusModeDefault)
                Spacer(Modifier.height(12.dp))
                SettingSwitch("Haptic feedback", "Vibration cues for steps, beats and zone exits.", profile.haptics, main::setHaptics)
            }

            SectionTitle("Data & privacy")
            LinkRow(Icons.Filled.PrivacyTip, "Privacy Policy", "All data stays on this device") { navigator.privacy() }
            LinkRow(Icons.Filled.Restore, "Load demo data", "Replace your data with example practice") { confirmDemo = true }
            LinkRow(Icons.Filled.DeleteForever, "Reset data", "Delete all history, XP and settings", danger = true) { confirmReset = true }

            Text(
                "Tai Chi Flow ${BuildConfig.VERSION_NAME} · Offline · No account",
                style = MaterialTheme.typography.bodySmall, color = TC.Muted,
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
            )
        }
    }

    if (editing) {
        EditProfileDialog(
            name = profile.name, experience = profile.experience, goal = profile.goal,
            onSave = { n, e, g ->
                main.updateProfile(n, e, g)
                editing = false
            },
            onDismiss = { editing = false }
        )
    }
    if (confirmReset) {
        ConfirmDialog(
            title = "Reset all data?",
            text = "This permanently deletes your history, records, achievements, custom sessions, XP and settings on this device. This cannot be undone.",
            confirmText = "Reset",
            onConfirm = {
                confirmReset = false
                main.resetAll { Toast.makeText(context, "All data deleted", Toast.LENGTH_SHORT).show() }
            },
            onDismiss = { confirmReset = false }
        )
    }
    if (confirmDemo) {
        ConfirmDialog(
            title = "Load demo data?",
            text = "Your current history and custom sessions will be replaced with example practice data.",
            confirmText = "Load",
            onConfirm = {
                confirmDemo = false
                main.loadDemoData { Toast.makeText(context, "Demo data loaded", Toast.LENGTH_SHORT).show() }
            },
            onDismiss = { confirmDemo = false }
        )
    }
}

private fun initials(name: String): String =
    name.trim().split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }.ifBlank { "TC" }

@Composable
private fun LinkRow(icon: ImageVector, title: String, sub: String, danger: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(TC.Charcoal)
            .clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (danger) TC.Ember else TC.Soft, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = if (danger) TC.Ember else TC.White)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = TC.Muted)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = TC.Muted)
    }
}

@Composable
private fun SettingSwitch(title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = TC.White)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = TC.Muted)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = TC.White,
                checkedTrackColor = TC.Crimson,
                uncheckedThumbColor = TC.Muted,
                uncheckedTrackColor = TC.Gray,
                uncheckedBorderColor = TC.Line
            )
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditProfileDialog(
    name: String,
    experience: Difficulty,
    goal: Goal,
    onSave: (String, Difficulty, Goal) -> Unit,
    onDismiss: () -> Unit
) {
    var n by rememberSaveable { mutableStateOf(name) }
    var e by rememberSaveable { mutableStateOf(experience.name) }
    var g by rememberSaveable { mutableStateOf(goal.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TC.DarkGray,
        title = { Text("EDIT PROFILE", style = MaterialTheme.typography.headlineSmall, color = TC.White) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = n, onValueChange = { n = it.take(24) },
                    label = { Text("Display name") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), colors = appTextFieldColors()
                )
                Spacer(Modifier.height(14.dp))
                Overline("Experience level")
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Difficulty.entries.forEach { d -> SelectChip(d.label, e == d.name, { e = d.name }) }
                }
                Spacer(Modifier.height(14.dp))
                Overline("Training goal")
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Goal.entries.forEach { goalOption -> SelectChip(goalOption.label, g == goalOption.name, { g = goalOption.name }) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    n,
                    Difficulty.entries.firstOrNull { it.name == e } ?: experience,
                    Goal.entries.firstOrNull { it.name == g } ?: goal
                )
            }) { Text("SAVE", color = TC.Ember, style = MaterialTheme.typography.labelLarge) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", color = TC.Soft, style = MaterialTheme.typography.labelLarge) }
        }
    )
}

@Composable
fun PrivacyScreen(navigator: AppNavigator) {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Privacy Policy", subtitle = "Tai Chi Flow", onBack = navigator::back)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            PolicySection("Everything stays on your device", "Tai Chi Flow works completely offline. Your profile, practice history, scores, records, achievements, custom sessions and settings are stored locally on this device only.")
            PolicySection("No account", "There is no sign-up, login or user account. The app does not ask for your email address or any contact details.")
            PolicySection("No data collection", "The app contains no analytics, advertising or tracking SDKs and does not send your data to any server. It does not request internet, location, camera, microphone or contacts permissions.")
            PolicySection("No payments", "The app contains no purchases, subscriptions, gambling or real-money features.")
            PolicySection("Deleting your data", "You can delete everything at any time from Profile › Reset data. Uninstalling the app also removes all of its data from your device.")
            PolicySection("Health note", "Tai Chi Flow is a training and practice tool. Scores reflect in-app exercises and session completion, not medical measurements. It does not provide medical advice. Practise within a comfortable range and consult a professional if you have health concerns.")
            PolicySection("Contact", "Questions about this policy can be sent through the developer contact listed on the app's store page.")
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PolicySection(title: String, text: String) {
    FlowCard(Modifier.fillMaxWidth()) {
        Overline(title, color = TC.Ember)
        Spacer(Modifier.height(6.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = TC.White)
    }
}
