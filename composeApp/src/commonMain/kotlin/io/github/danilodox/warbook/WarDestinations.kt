package io.github.danilodox.warbook

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import io.github.danilodox.warbook.navigation.Route
import io.github.danilodox.warbook.ui.history.WarHistoryRoute
import io.github.danilodox.warbook.ui.history.WarRecordRoute
import io.github.danilodox.warbook.ui.war.WarRoute

/** Aba Guerra e o histórico de guerras (G5): a lista do clã e uma guerra com a nota de cada membro. */
internal fun NavGraphBuilder.warDestinations(navController: NavHostController) {
    composable<Route.War> {
        WarRoute(
            onAddAccount = { navController.navigate(Route.Onboarding) },
            onOpenHistory = { clanTag -> navController.navigate(Route.WarHistory(clanTag)) },
        )
    }
    composable<Route.WarHistory> { entry ->
        val clanTag = entry.toRoute<Route.WarHistory>().clanTag
        WarHistoryRoute(clanTag, onOpenWar = { warId -> navController.navigate(Route.WarRecord(clanTag, warId)) })
    }
    composable<Route.WarRecord> { entry ->
        val route = entry.toRoute<Route.WarRecord>()
        WarRecordRoute(
            clanTag = route.clanTag,
            warId = route.warId,
            onOpenPlayer = { playerTag -> navController.navigate(Route.Player(playerTag)) },
        )
    }
}
