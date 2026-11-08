package io.github.danilodox.warbook.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.danilodox.warbook.api.PlayerDto
import io.github.danilodox.warbook.data.AppPreferences
import io.github.danilodox.warbook.data.FollowRepository
import io.github.danilodox.warbook.data.ProfileRepository
import io.github.danilodox.warbook.ui.common.FollowButton
import io.github.danilodox.warbook.ui.me.OfflineCard
import io.github.danilodox.warbook.ui.me.ProfileCardAction
import io.github.danilodox.warbook.ui.me.ProfileContent
import io.github.danilodox.warbook.ui.me.ProfileHeader
import io.github.danilodox.warbook.ui.me.ProfileSkeleton
import io.github.danilodox.warbook.ui.me.StaleBanner
import io.github.danilodox.warbook.ui.theme.Spacing
import org.koin.compose.koinInject

/** Tela Jogador (T1). [onOpenClan] abre a tela Clã do clã dele. */
@Composable
fun PlayerRoute(tag: String, fromSearch: Boolean, onOpenClan: (String) -> Unit, modifier: Modifier = Modifier) {
    val profiles: ProfileRepository = koinInject()
    val follows: FollowRepository = koinInject()
    val preferences: AppPreferences = koinInject()
    val viewModel = viewModel(key = "player-$tag") { PlayerViewModel(tag, profiles, follows, preferences, fromSearch) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    ProfileCardAction(state.profile?.player)
    PlayerScreen(state, viewModel::refresh, viewModel::toggleFollow, onOpenClan, modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    state: PlayerUiState,
    onRefresh: () -> Unit,
    onToggleFollow: () -> Unit,
    onOpenClan: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!state.loaded) return
    PullToRefreshBox(
        isRefreshing = state.refreshing && state.profile != null,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        val profile = state.profile
        val error = state.error
        when {
            profile != null -> Column(Modifier.fillMaxSize()) {
                if (error != null) StaleBanner(error, profile.fetchedAt, onRefresh)
                ProfileContent(
                    player = profile.player,
                    header = { PlayerHeader(profile.player, state, onToggleFollow, onOpenClan) },
                    modifier = Modifier.weight(1f),
                )
            }
            error != null -> OfflineCard(error, onRetry = onRefresh)
            else -> ProfileSkeleton()
        }
    }
}

/** Cabeçalho do perfil (o clã abre a tela Clã) e "Seguir" embaixo. "Comparar" chega no lote 9. */
@Composable
private fun PlayerHeader(
    player: PlayerDto,
    state: PlayerUiState,
    onToggleFollow: () -> Unit,
    onOpenClan: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        ProfileHeader(player, onOpenClan = onOpenClan)
        FollowButton(
            following = state.following,
            limitReached = state.followLimitReached,
            enabled = true,
            onToggle = onToggleFollow,
        )
    }
}
