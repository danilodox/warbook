package io.github.danilodox.warbook.navigation

import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController

internal fun NavDestination?.isIn(route: Route): Boolean = this?.hierarchy?.any { it.hasRoute(route::class) } == true

internal inline fun <reified T : Any> NavHostController.hasInBackStack(): Boolean =
    runCatching { getBackStackEntry<T>() }.isSuccess

/**
 * Troca de aba mantendo o estado de cada uma e sem empilhar abas repetidas. Volta até a aba Eu,
 * que é sempre a base das abas (o app pode ter começado no onboarding).
 */
internal fun NavHostController.navigateToTab(tab: TopLevelTab) {
    navigate(tab.route) {
        popUpTo<Route.Me> { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Fim do onboarding: aberto por "Adicionar conta", volta para onde estava; na primeira abertura, vira as abas. */
internal fun NavHostController.leaveOnboarding() {
    if (hasInBackStack<Route.Main>()) {
        popBackStack<Route.Onboarding>(inclusive = true)
    } else {
        navigate(Route.Main) { popUpTo<Route.Onboarding> { inclusive = true } }
    }
}

/** "Só explorar": as abas com a Buscar aberta, e a aba Eu embaixo (com o convite para adicionar conta). */
internal fun NavHostController.exploreFromOnboarding() {
    navigate(Route.Main) { popUpTo<Route.Onboarding> { inclusive = true } }
    navigateToTab(TopLevelTab.SEARCH)
}
