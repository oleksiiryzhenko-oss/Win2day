package com.taichiflow.app.ui.challenges

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.taichiflow.app.domain.engine.FlowPrograms
import com.taichiflow.app.domain.model.ActivityType
import com.taichiflow.app.domain.usecase.AppSnapshot
import com.taichiflow.app.ui.components.DailyFlowCard
import com.taichiflow.app.ui.components.FlowBackdrop
import com.taichiflow.app.ui.components.FlowCard
import com.taichiflow.app.ui.components.IconBadge
import com.taichiflow.app.ui.components.Overline
import com.taichiflow.app.ui.components.Pill
import com.taichiflow.app.ui.components.SectionTitle
import com.taichiflow.app.ui.components.WeeklyGoalsCard
import com.taichiflow.app.ui.navigation.AppNavigator
import com.taichiflow.app.ui.theme.TC

@Composable
fun ChallengesScreen(snapshot: AppSnapshot, navigator: AppNavigator) {
    Box(Modifier.fillMaxSize()) {
        FlowBackdrop(intensity = 0.7f)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(Modifier.padding(top = 20.dp)) {
                Overline("Test your control", color = TC.Ember)
                Text("CHALLENGES", style = MaterialTheme.typography.displaySmall, color = TC.White)
                Text("Scored drills that feed your attributes and records.", style = MaterialTheme.typography.bodyMedium, color = TC.Muted)
            }
            DailyFlowCard(snapshot.daily, snapshot.dailyDone, onStart = { navigator.daily(snapshot.daily) })

            SectionTitle("Skill challenges")
            ChallengeCard(
                "Balance Flow", "Keep the flow indicator inside a shrinking target zone.",
                Icons.Filled.GpsFixed, "45 sec", snapshot.bestFor(ActivityType.BALANCE), "Balance · Stability · Control"
            ) { navigator.balance() }
            ChallengeCard(
                "Flow Rhythm", "Tap on every beat as the indicator crosses each zone.",
                Icons.Filled.GraphicEq, "38 sec", snapshot.bestFor(ActivityType.RHYTHM), "Rhythm · Accuracy · Consistency"
            ) { navigator.rhythm() }
            ChallengeCard(
                "Breathing Challenge", "Follow the expanding circle with a steady 4-2-6 breath.",
                Icons.Filled.Air, "1–3 min", snapshot.bestFor(ActivityType.BREATHING), "Breathing · Focus"
            ) { navigator.breathing() }

            SectionTitle("Flow tests")
            ChallengeCard(
                "5 Minute Flow", "Balance, Cloud Hands, Weight Transfer, Rhythm and Closing Form in one run.",
                Icons.Filled.Waves, "${(FlowPrograms.totalSec(FlowPrograms.five) + 59) / 60} min",
                snapshot.bestFor(ActivityType.FLOW), "Complete Flow Score"
            ) { navigator.flowFive() }
            ChallengeCard(
                "Flow Master", "Seven stages, about 15 minutes. The ultimate test of your practice.",
                Icons.Filled.MilitaryTech, "15 min", snapshot.bestFor(ActivityType.MASTER), "Master Flow Score", highlight = true
            ) { navigator.flowMaster() }

            SectionTitle("This week")
            WeeklyGoalsCard(snapshot.weekly)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ChallengeCard(
    title: String,
    description: String,
    icon: ImageVector,
    duration: String,
    best: Int?,
    trains: String,
    highlight: Boolean = false,
    onClick: () -> Unit
) {
    FlowCard(Modifier.fillMaxWidth(), onClick = onClick, highlight = highlight) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, size = 48, accent = highlight)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title.uppercase(), style = MaterialTheme.typography.headlineSmall, color = TC.White)
                Text(description, style = MaterialTheme.typography.bodySmall, color = TC.Soft)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = TC.Muted)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Pill(duration, color = TC.Soft)
            Pill(if (best != null) "Best $best" else "No score yet", color = TC.Ember)
            Text(trains, style = MaterialTheme.typography.bodySmall, color = TC.Muted, maxLines = 1)
        }
    }
}
