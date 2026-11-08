package io.github.danilodox.warbook.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.danilodox.warbook.data.AppPreferences
import io.github.danilodox.warbook.data.ClanRepository
import io.github.danilodox.warbook.data.FollowKind
import io.github.danilodox.warbook.data.FollowRepository
import io.github.danilodox.warbook.resources.Res
import io.github.danilodox.warbook.resources.ic_tab_search
import io.github.danilodox.warbook.resources.search_button
import io.github.danilodox.warbook.resources.search_clan_label
import io.github.danilodox.warbook.resources.search_mode_clan
import io.github.danilodox.warbook.resources.search_mode_player
import io.github.danilodox.warbook.resources.search_player_label
import io.github.danilodox.warbook.ui.components.SegmentedTabs
import io.github.danilodox.warbook.ui.theme.Spacing
import io.github.danilodox.warbook.ui.theme.WarbookTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/**
 * Aba Buscar. [openClanMode] = pedido (vindo da aba Clã sem clã) para abrir no modo Clã; [onClanModeOpened]
 * avisa que já foi atendido. [onOpenPlayer] e [onOpenClan] abrem as telas Jogador e Clã.
 */
@Composable
fun SearchRoute(
    openClanMode: Boolean,
    onClanModeOpened: () -> Unit,
    onOpenPlayer: (String) -> Unit,
    onOpenClan: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val clans: ClanRepository = koinInject()
    val follows: FollowRepository = koinInject()
    val preferences: AppPreferences = koinInject()
    val viewModel = viewModel { SearchViewModel(clans, follows, preferences) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(openClanMode) {
        if (openClanMode) {
            viewModel.selectMode(SearchMode.CLAN)
            onClanModeOpened()
        }
    }
    LaunchedEffect(state.open) {
        val target = state.open ?: return@LaunchedEffect
        viewModel.onOpened()
        when (target.kind) {
            FollowKind.PLAYER -> onOpenPlayer(target.tag)
            FollowKind.CLAN -> onOpenClan(target.tag)
        }
    }

    SearchScreen(state, viewModel, modifier)
}

@Composable
private fun SearchScreen(state: SearchUiState, viewModel: SearchViewModel, modifier: Modifier) {
    // O texto fica por modo: trocar de Jogador para Clã não leva a tag junto.
    var query by rememberSaveable(state.mode) { mutableStateOf("") }
    val submit = { viewModel.submit(query) }

    Column(modifier.fillMaxSize()) {
        SegmentedTabs(
            listOf(SearchMode.PLAYER to Res.string.search_mode_player, SearchMode.CLAN to Res.string.search_mode_clan),
            state.mode,
            viewModel::selectMode,
        )
        SearchField(query, state.mode, onChange = {
            query = it
            if (it.isBlank()) viewModel.clear()
        }, onSubmit = submit)
        LazyColumn(
            contentPadding = PaddingValues(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            modifier = Modifier.weight(1f),
        ) {
            outcome(state, viewModel, onRetry = submit)
            if (state.outcome == SearchOutcome.None) {
                followedSection(state, viewModel)
                recentSection(state.recent, viewModel)
            }
        }
    }
}

/** Campo em pílula com a lupa. No modo Jogador, a tag sai em monoespaçada. */
@Composable
private fun SearchField(query: String, mode: SearchMode, onChange: (String) -> Unit, onSubmit: () -> Unit) {
    val player = mode == SearchMode.PLAYER
    OutlinedTextField(
        value = query,
        onValueChange = onChange,
        placeholder = {
            Text(stringResource(if (player) Res.string.search_player_label else Res.string.search_clan_label))
        },
        leadingIcon = {
            Icon(painterResource(Res.drawable.ic_tab_search), contentDescription = null, Modifier.size(20.dp))
        },
        trailingIcon = {
            TextButton(onClick = onSubmit, enabled = query.isNotBlank()) {
                Text(stringResource(Res.string.search_button))
            }
        },
        textStyle = if (player) WarbookTheme.text.tag.copy(fontSize = 16.sp) else MaterialTheme.typography.bodyLarge,
        shape = CircleShape,
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        keyboardOptions = KeyboardOptions(
            capitalization = if (player) KeyboardCapitalization.Characters else KeyboardCapitalization.Words,
            autoCorrectEnabled = false,
            imeAction = ImeAction.Search,
        ),
        keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.xs),
    )
}
