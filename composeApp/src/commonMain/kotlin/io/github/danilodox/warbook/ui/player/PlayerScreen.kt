package io.github.danilodox.warbook.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.danilodox.warbook.api.PlayerDto
import io.github.danilodox.warbook.data.AppPreferences
import io.github.danilodox.warbook.data.FollowRepository
import io.github.danilodox.warbook.data.ProfileRepository
import io.github.danilodox.warbook.resources.Res
import io.github.danilodox.warbook.resources.player_clan_open
import io.github.danilodox.warbook.resources.player_clan_role
import io.github.danilodox.warbook.resources.player_no_clan
import io.github.danilodox.warbook.resources.player_tag
import io.github.danilodox.warbook.resources.screen_clan
import io.github.danilodox.warbook.ui.common.ClanBadge
import io.github.danilodox.warbook.ui.common.FollowButton
import io.github.danilodox.warbook.ui.me.LeagueLine
import io.github.danilodox.warbook.ui.me.OfflineCard
import io.github.danilodox.warbook.ui.me.ProfileContent
import io.github.danilodox.warbook.ui.me.ProfileSkeleton
import io.github.danilodox.warbook.ui.me.StaleBanner
import io.github.danilodox.warbook.ui.search.label
import io.github.danilodox.warbook.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/** Tela Jogador (T1). [onOpenClan] abre a tela Clã do clã dele. */
@Composable
fun PlayerRoute(tag: String, fromSearch: Boolean, onOpenClan: (String) -> Unit, modifier: Modifier = Modifier) {
    val profiles: ProfileRepository = koinInject()
    val follows: FollowRepository = koinInject()
    val preferences: AppPreferences = koinInject()
    val viewModel = viewModel(key = "player-$tag") { PlayerViewModel(tag, profiles, follows, preferences, fromSearch) }
    val state by viewModel.state.collectAsStateWithLifecycle()
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
                if (error != null) StaleBanner(error, profile.fetchedAt)
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

/** Nome, tag, clã (toca para abrir) e liga; "Seguir" ao lado. "Comparar" e cartão chegam na v0.6. */
@Composable
private fun PlayerHeader(
    player: PlayerDto,
    state: PlayerUiState,
    onToggleFollow: () -> Unit,
    onOpenClan: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(player.name, style = MaterialTheme.typography.titleLarge)
        Text(stringResource(Res.string.player_tag, player.tag), style = MaterialTheme.typography.bodySmall)
        val clan = player.clan
        if (clan == null) {
            Text(stringResource(Res.string.player_no_clan), style = MaterialTheme.typography.bodyMedium)
        } else {
            val text = player.role
                ?.let { stringResource(Res.string.player_clan_role, clan.name, stringResource(it.label)) }
                ?: clan.name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                modifier = Modifier.clickable(
                    onClickLabel = stringResource(Res.string.screen_clan),
                    role = Role.Button,
                ) { onOpenClan(clan.tag) },
            ) {
                ClanBadge(clan.badgeUrl, contentDescription = null, size = 24.dp)
                Text(
                    stringResource(Res.string.player_clan_open, text),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        LeagueLine(player)
        FollowButton(
            following = state.following,
            limitReached = state.followLimitReached,
            enabled = true,
            onToggle = onToggleFollow,
        )
    }
}
