package io.github.danilodox.warbook.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.danilodox.warbook.api.ClanRole
import io.github.danilodox.warbook.api.PlayerDto
import io.github.danilodox.warbook.data.AppError
import io.github.danilodox.warbook.data.WarbookRepository
import io.github.danilodox.warbook.resources.Res
import io.github.danilodox.warbook.resources.error_api_maintenance
import io.github.danilodox.warbook.resources.error_invalid_query
import io.github.danilodox.warbook.resources.error_invalid_tag
import io.github.danilodox.warbook.resources.error_invalid_verify_token
import io.github.danilodox.warbook.resources.error_network
import io.github.danilodox.warbook.resources.error_not_found
import io.github.danilodox.warbook.resources.error_rate_limited
import io.github.danilodox.warbook.resources.error_server
import io.github.danilodox.warbook.resources.error_war_log_private
import io.github.danilodox.warbook.resources.player_clan_role
import io.github.danilodox.warbook.resources.player_level
import io.github.danilodox.warbook.resources.player_no_clan
import io.github.danilodox.warbook.resources.player_tag
import io.github.danilodox.warbook.resources.player_town_hall
import io.github.danilodox.warbook.resources.player_trophies
import io.github.danilodox.warbook.resources.player_war_stars
import io.github.danilodox.warbook.resources.retry
import io.github.danilodox.warbook.resources.role_co_leader
import io.github.danilodox.warbook.resources.role_elder
import io.github.danilodox.warbook.resources.role_leader
import io.github.danilodox.warbook.resources.role_member
import io.github.danilodox.warbook.resources.search_button
import io.github.danilodox.warbook.resources.search_hint
import io.github.danilodox.warbook.resources.search_loading
import io.github.danilodox.warbook.resources.search_tag_label
import io.github.danilodox.warbook.ui.theme.Spacing
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun SearchRoute(modifier: Modifier = Modifier) {
    val repository: WarbookRepository = koinInject()
    val viewModel = viewModel { SearchViewModel(repository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    SearchScreen(state = state, onSearch = viewModel::search, modifier = modifier)
}

@Composable
fun SearchScreen(state: SearchState, onSearch: (String) -> Unit, modifier: Modifier = Modifier) {
    var tag by rememberSaveable { mutableStateOf("") }
    val submit = { if (tag.isNotBlank()) onSearch(tag) }

    Column(
        modifier = modifier.fillMaxSize().padding(Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = tag,
                onValueChange = { tag = it },
                label = { Text(stringResource(Res.string.search_tag_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Search,
                ),
                keyboardActions = KeyboardActions(onSearch = { submit() }),
                modifier = Modifier.weight(1f),
            )
            Button(onClick = submit, enabled = tag.isNotBlank() && state != SearchState.Loading) {
                Text(stringResource(Res.string.search_button))
            }
        }
        when (state) {
            SearchState.Idle -> Text(
                stringResource(Res.string.search_hint),
                style = MaterialTheme.typography.bodyMedium,
            )
            SearchState.Loading -> LoadingRow()
            is SearchState.Found -> PlayerCard(state.player)
            is SearchState.Failed -> ErrorCard(state.error, onRetry = submit)
        }
    }
}

@Composable
private fun LoadingRow() {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small), verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator()
        Text(stringResource(Res.string.search_loading))
    }
}

@Composable
private fun PlayerCard(player: PlayerDto) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.medium), verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
            Text(player.name, style = MaterialTheme.typography.titleLarge)
            Text(stringResource(Res.string.player_tag, player.tag), style = MaterialTheme.typography.bodySmall)
            val clan = player.clan
            val role = player.role
            Text(
                when {
                    clan == null -> stringResource(Res.string.player_no_clan)
                    role == null -> clan.name
                    else -> stringResource(Res.string.player_clan_role, clan.name, stringResource(role.label))
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.large)) {
                Stat(Res.string.player_town_hall, player.townHallLevel)
                Stat(Res.string.player_level, player.expLevel)
                Stat(Res.string.player_trophies, player.trophies)
                Stat(Res.string.player_war_stars, player.warStars)
            }
        }
    }
}

@Composable
private fun Stat(label: StringResource, value: Int) {
    Column {
        Text(stringResource(label), style = MaterialTheme.typography.labelSmall)
        Text(value.toString(), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun ErrorCard(error: AppError, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Column(Modifier.padding(Spacing.medium), verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
            Text(
                stringResource(error.message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            if (error.canRetry) {
                OutlinedButton(onClick = onRetry) { Text(stringResource(Res.string.retry)) }
            }
        }
    }
}

internal val ClanRole.label: StringResource
    get() = when (this) {
        ClanRole.MEMBER -> Res.string.role_member
        ClanRole.ELDER -> Res.string.role_elder
        ClanRole.CO_LEADER -> Res.string.role_co_leader
        ClanRole.LEADER -> Res.string.role_leader
    }

internal val AppError.message: StringResource
    get() = when (this) {
        AppError.INVALID_TAG -> Res.string.error_invalid_tag
        AppError.NOT_FOUND -> Res.string.error_not_found
        AppError.WAR_LOG_PRIVATE -> Res.string.error_war_log_private
        AppError.API_MAINTENANCE -> Res.string.error_api_maintenance
        AppError.RATE_LIMITED -> Res.string.error_rate_limited
        AppError.INVALID_VERIFY_TOKEN -> Res.string.error_invalid_verify_token
        AppError.INVALID_QUERY -> Res.string.error_invalid_query
        AppError.NETWORK -> Res.string.error_network
        AppError.SERVER -> Res.string.error_server
    }

/** Tag errada ou inexistente não muda tentando de novo; o resto pode passar. */
internal val AppError.canRetry: Boolean
    get() = this != AppError.INVALID_TAG && this != AppError.NOT_FOUND && this != AppError.WAR_LOG_PRIVATE
