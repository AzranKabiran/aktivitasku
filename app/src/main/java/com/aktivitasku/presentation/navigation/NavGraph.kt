package com.aktivitasku.presentation.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.*
import androidx.navigation.NavType
import androidx.navigation.compose.*
import com.aktivitasku.presentation.add.AddActivityScreen
import com.aktivitasku.presentation.detail.DetailScreen
import com.aktivitasku.presentation.home.HomeScreen
import com.aktivitasku.presentation.statistics.StatisticsScreen
import com.aktivitasku.presentation.settings.SettingsScreen
import com.aktivitasku.presentation.theme.Blue700

sealed class Screen(val route: String) {
    object Home       : Screen("home")
    object Statistics : Screen("statistics")
    object Settings   : Screen("settings")
    object Add        : Screen("add?activityId={activityId}") {
        fun route(id: Long? = null) = if (id != null) "add?activityId=$id" else "add"
    }
    object Detail     : Screen("detail/{activityId}") {
        fun route(id: Long) = "detail/$id"
    }
}

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home,       "Beranda",   Icons.Rounded.HomeWork,  Icons.Rounded.Home),
    BottomNavItem(Screen.Statistics, "Statistik", Icons.Rounded.BarChart,  Icons.Rounded.BarChart),
    BottomNavItem(Screen.Settings,   "Setelan",   Icons.Rounded.TuneOutlined, Icons.Rounded.Tune)
)

@Composable
fun AktivitasKuNavGraph() {
    val navController = rememberNavController()
    val navBackStack  by navController.currentBackStackEntryAsState()
    val currentRoute  = navBackStack?.destination?.route
    val showBottomBar = currentRoute in listOf(Screen.Home.route, Screen.Statistics.route, Screen.Settings.route)

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick  = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(Screen.Home.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState    = true
                                }
                            },
                            icon  = { Icon(if (selected) item.selectedIcon else item.icon, item.label) },
                            label = { Text(item.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor   = Blue700,
                                selectedTextColor   = Blue700,
                                indicatorColor      = Blue700.copy(alpha = 0.12f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = Screen.Home.route,
            modifier         = Modifier.padding(innerPadding),
            enterTransition  = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = androidx.compose.animation.core.tween(280))
            },
            exitTransition   = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = androidx.compose.animation.core.tween(280))
            },
            popEnterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = androidx.compose.animation.core.tween(280))
            },
            popExitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = androidx.compose.animation.core.tween(280))
            }
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onAddActivity   = { navController.navigate(Screen.Add.route()) },
                    onActivityClick = { id -> navController.navigate(Screen.Detail.route(id)) }
                )
            }

            composable(Screen.Statistics.route) { StatisticsScreen() }

            composable(Screen.Settings.route) { SettingsScreen() }

            composable(
                route     = Screen.Add.route,
                arguments = listOf(navArgument("activityId") {
                    type = NavType.LongType; defaultValue = -1L
                })
            ) {
                AddActivityScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable(
                route     = Screen.Detail.route,
                arguments = listOf(navArgument("activityId") { type = NavType.LongType })
            ) { back ->
                val id = back.arguments?.getLong("activityId") ?: return@composable
                DetailScreen(
                    activityId     = id,
                    onNavigateBack = { navController.popBackStack() },
                    onEdit         = { navController.navigate(Screen.Add.route(id)) }
                )
            }
        }
    }
}
