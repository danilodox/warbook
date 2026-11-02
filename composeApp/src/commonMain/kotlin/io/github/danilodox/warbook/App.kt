package io.github.danilodox.warbook

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.danilodox.warbook.data.WarbookHttpClients
import io.github.danilodox.warbook.navigation.Route
import io.github.danilodox.warbook.navigation.TopLevelTab
import io.github.danilodox.warbook.resources.Res
import io.github.danilodox.warbook.resources.app_name
import io.github.danilodox.warbook.resources.back
import io.github.danilodox.warbook.resources.ic_arrow_back
import io.github.danilodox.warbook.resources.ic_settings
import io.github.danilodox.warbook.resources.settings
import io.github.danilodox.warbook.resources.tab_clan
import io.github.danilodox.warbook.resources.tab_me
import io.github.danilodox.warbook.resources.tab_war
import io.github.danilodox.warbook.ui.screens.PlaceholderScreen
import io.github.danilodox.warbook.ui.screens.SettingsScreen
import io.github.danilodox.warbook.ui.search.SearchRoute
import io.github.danilodox.warbook.ui.theme.WarbookTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun App() {
    val httpClients: WarbookHttpClients = koinInject()
    // Primeira abertura: já pede o token da instalação, sem esperar a primeira busca.
    LaunchedEffect(Unit) { httpClients.tokens.warmUp() }

    WarbookTheme {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val destination = backStackEntry?.destination
        val currentTab = TopLevelTab.entries.firstOrNull { destination.isIn(it.route) }
        val onSettings = destination?.hasRoute(Route.Settings::class) == true

        Scaffold(
            topBar = {
                WarbookTopBar(
                    onSettings = onSettings,
                    currentTab = currentTab,
                    onBack = { navController.popBackStack() },
                    onOpenSettings = { navController.navigate(Route.Settings) { launchSingleTop = true } },
                )
            },
            bottomBar = {
                if (!onSettings) {
                    NavigationBar {
                        TopLevelTab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = tab == currentTab,
                                onClick = { navController.navigateToTab(tab) },
                                icon = { Icon(painterResource(tab.icon), contentDescription = null) },
                                label = { Text(stringResource(tab.label)) },
                            )
                        }
                    }
                }
            },
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Route.Me,
                modifier = Modifier.padding(padding),
            ) {
                composable<Route.Me> { PlaceholderScreen(Res.string.tab_me) }
                composable<Route.Clan> { PlaceholderScreen(Res.string.tab_clan) }
                composable<Route.War> { PlaceholderScreen(Res.string.tab_war) }
                composable<Route.Search> { SearchRoute() }
                composable<Route.Settings> { SettingsScreen() }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WarbookTopBar(
    onSettings: Boolean,
    currentTab: TopLevelTab?,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val title = when {
        onSettings -> Res.string.settings
        currentTab != null -> currentTab.label
        else -> Res.string.app_name
    }
    TopAppBar(
        title = { Text(stringResource(title)) },
        navigationIcon = {
            if (onSettings) {
                IconButton(onClick = onBack) {
                    Icon(painterResource(Res.drawable.ic_arrow_back), stringResource(Res.string.back))
                }
            }
        },
        actions = {
            if (!onSettings) {
                IconButton(onClick = onOpenSettings) {
                    Icon(painterResource(Res.drawable.ic_settings), stringResource(Res.string.settings))
                }
            }
        },
    )
}

private fun NavDestination?.isIn(route: Route): Boolean = this?.hierarchy?.any { it.hasRoute(route::class) } == true

/** Troca de aba mantendo o estado de cada uma e sem empilhar abas repetidas. */
private fun NavHostController.navigateToTab(tab: TopLevelTab) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
