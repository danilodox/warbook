package io.github.danilodox.warbook.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.danilodox.warbook.api.PlayerDto
import io.github.danilodox.warbook.data.AccountsRepository
import io.github.danilodox.warbook.data.AppPreferences
import io.github.danilodox.warbook.data.FollowRepository
import io.github.danilodox.warbook.data.ProfileRepository
import io.github.danilodox.warbook.resources.Res
import io.github.danilodox.warbook.resources.compare_with_me
import io.github.danilodox.warbook.ui.common.FollowButton
import io.github.danilodox.warbook.ui.me.OfflineCard
import io.github.danilodox.warbook.ui.me.ProfileCardAction
import io.github.danilodox.warbook.ui.me.ProfileContent
import io.github.danilodox.warbook.ui.me.ProfileHeader
import io.github.danilodox.warbook.ui.me.ProfileSkeleton
import io.github.danilodox.warbook.ui.me.StaleBanner
import io.github.danilodox.warbook.ui.theme.Spacing
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/**
 * Tela Jogador (T1). [onOpenClan] abre a tela Clã do clã dele; [onCompare] abre o Comparar,
 * que só aparece com uma conta ativa e quando o jogador não é ela.
 */
@Composable
fun PlayerRoute(
    tag: String,
    fromSearch: Boolean,
    onOpenClan: (String) -> Unit,
    onCompare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val profiles: ProfileRepository = koinInject()
    val follows: FollowRepository = koinInject()
    val preferences: AppPreferences = koinInject()
    val accounts: AccountsRepository = koinInject()
    val viewModel = viewModel(key = "player-$tag") { PlayerViewModel(tag, profiles, follows, preferences, fromSearch) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val activeTagFlow = remember(accounts) { accounts.accounts.map { list -> list.firstOrNull { it.isActive }?.tag } }
    val activeTag by activeTagFlow.collectAsStateWithLifecycle(initialValue = null)
    ProfileCardAction(state.profile?.player)
    PlayerScreen(
        state = state,
        onRefresh = viewModel::refresh,
        onToggleFollow = viewModel::toggleFollow,
        onOpenClan = onOpenClan,
        onCompare = onCompare.takeIf { activeTag != null && activeTag != tag },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    state: PlayerUiState,
    onRefresh: () -> Unit,
    onToggleFollow: () -> Unit,
    onOpenClan: (String) -> Unit,
    onCompare: (() -> Unit)?,
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
                    header = { PlayerHeader(profile.player, state, onToggleFollow, onOpenClan, onCompare) },
                    modifier = Modifier.weight(1f),
                )
            }
            error != null -> OfflineCard(error, onRetry = onRefresh)
            else -> ProfileSkeleton()
        }
    }
}

/** Cabeçalho do perfil (o clã abre a tela Clã) e, embaixo, "Seguir" e "Comparar comigo" lado a lado. */
@Composable
private fun PlayerHeader(
    player: PlayerDto,
    state: PlayerUiState,
    onToggleFollow: () -> Unit,
    onOpenClan: (String) -> Unit,
    onCompare: (() -> Unit)?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        ProfileHeader(player, onOpenClan = onOpenClan)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.Top,
        ) {
            FollowButton(
                following = state.following,
                limitReached = state.followLimitReached,
                enabled = true,
                onToggle = onToggleFollow,
                fillWidth = true,
                modifier = Modifier.weight(1f),
            )
            if (onCompare != null) {
                Button(onClick = onCompare, modifier = Modifier.weight(1f)) {
                    Text(stringResource(Res.string.compare_with_me))
                }
            }
        }
    }
}
