package com.taichiflow.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.taichiflow.app.domain.model.DailyChallenge
import com.taichiflow.app.domain.model.DailyKind
import com.taichiflow.app.ui.challenges.BalanceChallengeScreen
import com.taichiflow.app.ui.challenges.BreathingChallengeScreen
import com.taichiflow.app.ui.challenges.ChallengesScreen
import com.taichiflow.app.ui.challenges.FlowChallengeScreen
import com.taichiflow.app.ui.challenges.RhythmChallengeScreen
import com.taichiflow.app.ui.components.LoadingScreen
import com.taichiflow.app.ui.home.HomeScreen
import com.taichiflow.app.ui.main.MainViewModel
import com.taichiflow.app.ui.practice.BuilderScreen
import com.taichiflow.app.ui.practice.MovementDetailScreen
import com.taichiflow.app.ui.practice.MovementLibraryScreen
import com.taichiflow.app.ui.practice.PracticeScreen
import com.taichiflow.app.ui.practice.SessionDetailScreen
import com.taichiflow.app.ui.profile.PrivacyScreen
import com.taichiflow.app.ui.profile.ProfileScreen
import com.taichiflow.app.ui.progress.AchievementsScreen
import com.taichiflow.app.ui.progress.HistoryDetailScreen
import com.taichiflow.app.ui.progress.HistoryScreen
import com.taichiflow.app.ui.progress.ProgressScreen
import com.taichiflow.app.ui.progress.RecordsScreen
import com.taichiflow.app.ui.session.GuidedSessionScreen
import com.taichiflow.app.ui.theme.TC
import com.taichiflow.app.ui.util.appViewModel

object Routes {
    const val HOME = "home"
    const val PRACTICE = "practice"
    const val CHALLENGES = "challenges"
    const val PROGRESS = "progress"
    const val PROFILE = "profile"
    const val SESSION = "session/{id}"
    const val GUIDED = "guided/{id}?daily={daily}"
    const val LIBRARY = "library"
    const val MOVEMENT = "movement/{id}"
    const val BUILDER = "builder"
    const val BALANCE = "balance?daily={daily}"
    const val RHYTHM = "rhythm?daily={daily}"
    const val BREATHING = "breathing?daily={daily}"
    const val FLOW = "flow/{mode}"
    const val HISTORY = "history"
    const val HISTORY_DETAIL = "history_detail/{id}"
    const val ACHIEVEMENTS = "achievements"
    const val RECORDS = "records"
    const val PRIVACY = "privacy"

    val tabs = listOf(HOME, PRACTICE, CHALLENGES, PROGRESS, PROFILE)
}

/** Typed navigation actions shared by all screens. */
class AppNavigator(private val nav: NavHostController) {
    fun back() {
        nav.popBackStack()
    }

    fun tab(route: String) {
        nav.navigate(route) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun session(id: String) = nav.navigate("session/$id")
    fun guided(id: String, dailyId: String? = null) =
        nav.navigate(if (dailyId != null) "guided/$id?daily=$dailyId" else "guided/$id")

    fun library() = nav.navigate(Routes.LIBRARY)
    fun movement(id: String) = nav.navigate("movement/$id")
    fun builder() = nav.navigate(Routes.BUILDER)
    fun balance(dailyId: String? = null) = nav.navigate(if (dailyId != null) "balance?daily=$dailyId" else "balance")
    fun rhythm(dailyId: String? = null) = nav.navigate(if (dailyId != null) "rhythm?daily=$dailyId" else "rhythm")
    fun breathing(dailyId: String? = null) = nav.navigate(if (dailyId != null) "breathing?daily=$dailyId" else "breathing")
    fun flowFive() = nav.navigate("flow/five")
    fun flowMaster() = nav.navigate("flow/master")
    fun history() = nav.navigate(Routes.HISTORY)
    fun historyDetail(id: Long) = nav.navigate("history_detail/$id")
    fun achievements() = nav.navigate(Routes.ACHIEVEMENTS)
    fun records() = nav.navigate(Routes.RECORDS)
    fun privacy() = nav.navigate(Routes.PRIVACY)

    fun daily(challenge: DailyChallenge) = when (challenge.kind) {
        DailyKind.GUIDED -> guided("daily_${challenge.id}", challenge.id)
        DailyKind.BALANCE -> balance(challenge.id)
        DailyKind.RHYTHM -> rhythm(challenge.id)
        DailyKind.BREATHING -> breathing(challenge.id)
    }
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabItems = listOf(
    Tab(Routes.HOME, "Home", Icons.Filled.Home),
    Tab(Routes.PRACTICE, "Practice", Icons.Filled.SelfImprovement),
    Tab(Routes.CHALLENGES, "Challenges", Icons.Filled.Bolt),
    Tab(Routes.PROGRESS, "Progress", Icons.Filled.Insights),
    Tab(Routes.PROFILE, "Profile", Icons.Filled.Person)
)

@Composable
fun AppRoot() {
    val nav = rememberNavController()
    val navigator = remember(nav) { AppNavigator(nav) }
    val main = appViewModel(key = "main") { MainViewModel(it.repository) }
    val snapshot by main.snapshot.collectAsStateWithLifecycle()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val showBar = route in Routes.tabs

    Scaffold(
        containerColor = TC.Black,
        bottomBar = {
            if (showBar) {
                NavigationBar(containerColor = TC.Charcoal, tonalElevation = 0.dp) {
                    tabItems.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = { navigator.tab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = TC.White,
                                selectedTextColor = TC.Ember,
                                indicatorColor = TC.DeepRed,
                                unselectedIconColor = TC.Muted,
                                unselectedTextColor = TC.Muted
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().background(TC.Black).padding(padding)) {
            val snap = snapshot
            if (snap == null) {
                LoadingScreen()
            } else {
                NavHost(navController = nav, startDestination = Routes.HOME) {
                    composable(Routes.HOME) { HomeScreen(snap, navigator) }
                    composable(Routes.PRACTICE) { PracticeScreen(snap, navigator) }
                    composable(Routes.CHALLENGES) { ChallengesScreen(snap, navigator) }
                    composable(Routes.PROGRESS) { ProgressScreen(snap, navigator) }
                    composable(Routes.PROFILE) { ProfileScreen(snap, main, navigator) }
                    composable(Routes.SESSION) { entry ->
                        SessionDetailScreen(entry.arguments?.getString("id").orEmpty(), snap, main, navigator)
                    }
                    composable(
                        Routes.GUIDED,
                        arguments = listOf(navArgument("daily") { type = NavType.StringType; nullable = true; defaultValue = null })
                    ) { entry ->
                        GuidedSessionScreen(
                            sessionId = entry.arguments?.getString("id").orEmpty(),
                            dailyId = entry.arguments?.getString("daily"),
                            snapshot = snap,
                            navigator = navigator
                        )
                    }
                    composable(Routes.LIBRARY) { MovementLibraryScreen(snap, navigator) }
                    composable(Routes.MOVEMENT) { entry ->
                        MovementDetailScreen(entry.arguments?.getString("id").orEmpty(), snap, navigator)
                    }
                    composable(Routes.BUILDER) { BuilderScreen(navigator) }
                    composable(
                        Routes.BALANCE,
                        arguments = listOf(navArgument("daily") { type = NavType.StringType; nullable = true; defaultValue = null })
                    ) { entry -> BalanceChallengeScreen(entry.arguments?.getString("daily"), snap, navigator) }
                    composable(
                        Routes.RHYTHM,
                        arguments = listOf(navArgument("daily") { type = NavType.StringType; nullable = true; defaultValue = null })
                    ) { entry -> RhythmChallengeScreen(entry.arguments?.getString("daily"), snap, navigator) }
                    composable(
                        Routes.BREATHING,
                        arguments = listOf(navArgument("daily") { type = NavType.StringType; nullable = true; defaultValue = null })
                    ) { entry -> BreathingChallengeScreen(entry.arguments?.getString("daily"), snap, navigator) }
                    composable(Routes.FLOW) { entry ->
                        FlowChallengeScreen(master = entry.arguments?.getString("mode") == "master", snapshot = snap, navigator = navigator)
                    }
                    composable(Routes.HISTORY) { HistoryScreen(snap, navigator) }
                    composable(
                        Routes.HISTORY_DETAIL,
                        arguments = listOf(navArgument("id") { type = NavType.LongType })
                    ) { entry -> HistoryDetailScreen(entry.arguments?.getLong("id") ?: 0L, snap, navigator) }
                    composable(Routes.ACHIEVEMENTS) { AchievementsScreen(snap, navigator) }
                    composable(Routes.RECORDS) { RecordsScreen(snap, navigator) }
                    composable(Routes.PRIVACY) { PrivacyScreen(navigator) }
                }
            }
        }
    }
}
