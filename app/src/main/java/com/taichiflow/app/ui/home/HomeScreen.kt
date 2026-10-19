package com.taichiflow.app.ui.home

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.taichiflow.app.domain.model.Attribute
import com.taichiflow.app.domain.usecase.AppSnapshot
import com.taichiflow.app.ui.components.AnimatedNumber
import com.taichiflow.app.ui.components.AttributeBar
import com.taichiflow.app.ui.components.DailyFlowCard
import com.taichiflow.app.ui.components.FlowBackdrop
import com.taichiflow.app.ui.components.FlowCard
import com.taichiflow.app.ui.components.HistoryRow
import com.taichiflow.app.ui.components.LabeledValue
import com.taichiflow.app.ui.components.Overline
import com.taichiflow.app.ui.components.Pill
import com.taichiflow.app.ui.components.PrimaryButton
import com.taichiflow.app.ui.components.ProgressLine
import com.taichiflow.app.ui.components.RecommendationCard
import com.taichiflow.app.ui.components.ScoreRing
import com.taichiflow.app.ui.components.SecondaryButton
import com.taichiflow.app.ui.components.SectionTitle
import com.taichiflow.app.ui.components.StreakWeekRow
import com.taichiflow.app.ui.components.WeeklyGoalsCard
import com.taichiflow.app.ui.components.difficultyColor
import com.taichiflow.app.ui.navigation.AppNavigator
import com.taichiflow.app.ui.navigation.Routes
import com.taichiflow.app.ui.theme.TC
import com.taichiflow.app.ui.util.Fmt

@Composable
fun HomeScreen(snapshot: AppSnapshot, navigator: AppNavigator) {
    val level = snapshot.level
    val today = snapshot.todaysPractice
    Box(Modifier.fillMaxSize()) {
        FlowBackdrop()
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero
            Column(Modifier.padding(top = 20.dp)) {
                Overline("Welcome back, ${snapshot.profile.name}", color = TC.Ember)
                Text("TAI CHI FLOW", style = MaterialTheme.typography.displaySmall, color = TC.White)
                Text("Move with control. Build your balance.", style = MaterialTheme.typography.bodyLarge, color = TC.Soft)
            }

            // Level + streak
            FlowCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Overline("Current level")
                        Text("FLOW LEVEL ${level.level}", style = MaterialTheme.typography.headlineMedium, color = TC.White)
                        Text(level.title.uppercase(), style = MaterialTheme.typography.labelMedium, color = TC.Ember)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.LocalFireDepartment, null, tint = TC.Ember, modifier = Modifier.size(22.dp))
                            AnimatedNumber(snapshot.streak.current, MaterialTheme.typography.headlineMedium)
                        }
                        Overline(if (snapshot.streak.current == 1) "Day streak" else "Days streak")
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row {
                    Overline("XP", Modifier.weight(1f))
                    Text("${Fmt.number(level.xpInLevel)} / ${Fmt.number(level.xpForNext)}", style = MaterialTheme.typography.labelMedium, color = TC.Soft)
                }
                Spacer(Modifier.height(6.dp))
                ProgressLine(level.progress, height = 8.dp)
                if (level.nextTitle != null) {
                    Spacer(Modifier.height(6.dp))
                    Text("${level.nextTitle} unlocks at Level ${level.nextTitleLevel}", style = MaterialTheme.typography.bodySmall, color = TC.Muted)
                }
                Spacer(Modifier.height(14.dp))
                StreakWeekRow(snapshot.streak)
            }

            // Today's practice
            FlowCard(Modifier.fillMaxWidth(), highlight = true) {
                Overline("Today's practice", color = TC.Ember)
                Text(today.name.uppercase(), style = MaterialTheme.typography.headlineLarge, color = TC.White)
                Text(today.description, style = MaterialTheme.typography.bodyMedium, color = TC.Soft)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    LabeledValue("Duration", "${today.minutes} minutes")
                    Column {
                        Overline("Difficulty")
                        Spacer(Modifier.height(2.dp))
                        Pill(today.difficulty.label, color = difficultyColor(today.difficulty))
                    }
                    LabeledValue("Focus", today.focusLabel, Modifier.weight(1f))
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PrimaryButton("Start practice", { navigator.guided(today.id) }, Modifier.weight(1f), icon = Icons.Filled.PlayArrow)
                    SecondaryButton("Details", { navigator.session(today.id) })
                }
            }

            // Flow score
            FlowCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ScoreRing(snapshot.flowScore, size = 132.dp, stroke = 9.dp, label = "/ 100")
                    Spacer(Modifier.width(18.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Overline("Flow score", color = TC.Ember)
                        AttributeBar("Balance", snapshot.attributes[Attribute.BALANCE] ?: 0)
                        AttributeBar("Mobility", snapshot.attributes[Attribute.MOBILITY] ?: 0)
                        AttributeBar("Focus", snapshot.attributes[Attribute.FOCUS] ?: 0)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "YOUR STYLE · ${snapshot.archetype.name}",
                    style = MaterialTheme.typography.labelLarge, color = TC.White,
                    modifier = Modifier.fillMaxWidth().background(TC.Blood, MaterialTheme.shapes.small).padding(10.dp)
                )
                Spacer(Modifier.height(10.dp))
                SecondaryButton("View progress", { navigator.tab(Routes.PROGRESS) }, Modifier.fillMaxWidth())
            }

            DailyFlowCard(snapshot.daily, snapshot.dailyDone, onStart = { navigator.daily(snapshot.daily) })

            WeeklyGoalsCard(snapshot.weekly, compact = true)

            SectionTitle("Smart recommendation")
            RecommendationCard(snapshot.recommendation, onOpen = { navigator.session(it.id) })

            SectionTitle("Recent sessions", action = "History", onAction = { navigator.history() })
            if (snapshot.history.isEmpty()) {
                Text(
                    "No sessions yet. Start today's practice above and your history will appear here.",
                    style = MaterialTheme.typography.bodyMedium, color = TC.Muted
                )
            } else {
                snapshot.history.take(3).forEach { r ->
                    HistoryRow(r, onClick = { navigator.historyDetail(r.id) })
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
