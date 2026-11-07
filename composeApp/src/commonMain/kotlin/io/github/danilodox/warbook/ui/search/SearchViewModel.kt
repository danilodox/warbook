package io.github.danilodox.warbook.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.danilodox.warbook.api.ClanSearch
import io.github.danilodox.warbook.api.ClanSearchResultDto
import io.github.danilodox.warbook.api.TagNormalizer
import io.github.danilodox.warbook.data.ApiResult
import io.github.danilodox.warbook.data.AppError
import io.github.danilodox.warbook.data.AppPreferences
import io.github.danilodox.warbook.data.ClanRepository
import io.github.danilodox.warbook.data.FollowKind
import io.github.danilodox.warbook.data.FollowRepository
import io.github.danilodox.warbook.data.FollowResult
import io.github.danilodox.warbook.data.Followed
import io.github.danilodox.warbook.data.RecentSearch
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SearchMode { PLAYER, CLAN }

/** O que a tela deve abrir: a tela Jogador ou a tela Clã. */
data class SearchTarget(val kind: FollowKind, val tag: String)

/** Um clã da busca por nome e se ele já é seguido (a estrela). */
data class ClanResult(val clan: ClanSearchResultDto, val following: Boolean)

/** O que mostrar abaixo do campo depois de buscar. */
sealed interface SearchOutcome {
    /** Nada buscado: mostra "Seguindo" e "Recentes". */
    data object None : SearchOutcome

    /** Texto que não é tag na busca de jogador: a busca por nick chega na v0.7. */
    data object NickNotice : SearchOutcome

    /** Nome de clã com menos de [ClanSearch.MIN_LENGTH] letras. */
    data object TooShort : SearchOutcome

    data object Searching : SearchOutcome

    data class Results(val clans: List<ClanResult>) : SearchOutcome

    data class Failed(val error: AppError) : SearchOutcome
}

data class SearchUiState(
    val mode: SearchMode = SearchMode.PLAYER,
    val outcome: SearchOutcome = SearchOutcome.None,
    val followed: List<Followed> = emptyList(),
    val recent: List<RecentSearch> = emptyList(),
    /** Lista "Seguindo" em modo de edição (botão remover em cada item). */
    val editing: Boolean = false,
    val followLimitReached: Boolean = false,
    /** Pedido de abrir uma tela; a tela chama [SearchViewModel.onOpened] depois de navegar. */
    val open: SearchTarget? = null,
)

private data class Local(
    val mode: SearchMode = SearchMode.PLAYER,
    val outcome: LocalOutcome = LocalOutcome.None,
    val editing: Boolean = false,
    val limitReached: Boolean = false,
    val open: SearchTarget? = null,
)

/** Igual a [SearchOutcome], mas com os clãs crus: a estrela vem da lista "Seguindo" atual. */
private sealed interface LocalOutcome {
    data object None : LocalOutcome

    data class Shown(val outcome: SearchOutcome) : LocalOutcome

    data class Results(val clans: List<ClanSearchResultDto>) : LocalOutcome
}

/**
 * Aba Buscar (B1–B2). Jogador: só por tag (com `#` ou só com os caracteres de tag); o resto avisa que o nick
 * chega na v0.7. Clã: por tag (com `#`, ou só caracteres de tag com algum número) ou por nome no servidor.
 * Abrir um jogador ou clã fica para a tela dele, que busca, guarda nas "Recentes" e trata os erros.
 */
@Suppress("TooManyFunctions") // Uma função por ação da tela.
class SearchViewModel(
    private val clans: ClanRepository,
    private val follows: FollowRepository,
    private val preferences: AppPreferences,
) : ViewModel() {
    private val local = MutableStateFlow(Local())
    private var searchJob: Job? = null

    val state: StateFlow<SearchUiState> = combine(
        local,
        follows.followed,
        preferences.recentSearches,
    ) { local, followed, recent ->
        val clanTags = followed.filter { it.kind == FollowKind.CLAN }.mapTo(mutableSetOf()) { it.tag }
        SearchUiState(
            mode = local.mode,
            outcome = when (val outcome = local.outcome) {
                LocalOutcome.None -> SearchOutcome.None
                is LocalOutcome.Shown -> outcome.outcome
                is LocalOutcome.Results -> SearchOutcome.Results(
                    outcome.clans.map {
                        ClanResult(it, it.tag in clanTags)
                    },
                )
            },
            followed = followed,
            recent = recent,
            editing = local.editing && followed.isNotEmpty(),
            followLimitReached = local.limitReached,
            open = local.open,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SearchUiState())

    fun selectMode(mode: SearchMode) {
        if (mode == local.value.mode) return
        searchJob?.cancel()
        local.update { it.copy(mode = mode, outcome = LocalOutcome.None, limitReached = false) }
    }

    /** O campo ficou vazio: volta a mostrar "Seguindo" e "Recentes". */
    fun clear() {
        searchJob?.cancel()
        local.update { it.copy(outcome = LocalOutcome.None, limitReached = false) }
    }

    fun submit(query: String) {
        if (query.isBlank()) return
        searchJob?.cancel()
        local.update { it.copy(limitReached = false) }
        when (local.value.mode) {
            SearchMode.PLAYER -> submitPlayer(query)
            SearchMode.CLAN -> submitClan(query)
        }
    }

    private fun submitPlayer(query: String) {
        val tag = TagNormalizer.normalize(query)
        when {
            tag != null -> open(SearchTarget(FollowKind.PLAYER, tag))
            query.trim().startsWith("#") -> show(SearchOutcome.Failed(AppError.INVALID_TAG))
            else -> show(SearchOutcome.NickNotice)
        }
    }

    private fun submitClan(query: String) {
        val trimmed = query.trim()
        val tag = TagNormalizer.normalize(trimmed)
        // Sem `#`, só vira tag se tiver número: um nome como "PURY" não é confundido com tag.
        val looksLikeTag = trimmed.startsWith("#") || (tag != null && tag.any { it.isDigit() })
        when {
            looksLikeTag && tag != null -> open(SearchTarget(FollowKind.CLAN, tag))
            trimmed.startsWith("#") -> show(SearchOutcome.Failed(AppError.INVALID_TAG))
            ClanSearch.normalize(trimmed) == null -> show(SearchOutcome.TooShort)
            else -> searchByName(trimmed)
        }
    }

    private fun searchByName(name: String) {
        show(SearchOutcome.Searching)
        searchJob = viewModelScope.launch {
            val outcome = when (val result = clans.search(name)) {
                is ApiResult.Success -> LocalOutcome.Results(result.value)
                is ApiResult.Failure -> LocalOutcome.Shown(SearchOutcome.Failed(result.error))
            }
            local.update { it.copy(outcome = outcome) }
        }
    }

    /** Abre um item de "Seguindo" ou "Recentes", ou um clã da lista de resultados. */
    fun open(target: SearchTarget) = local.update { it.copy(open = target) }

    fun onOpened() = local.update { it.copy(open = null) }

    /** A estrela de um resultado: segue sem abrir o clã, ou deixa de seguir. */
    fun toggleFollow(result: ClanResult) {
        viewModelScope.launch {
            if (result.following) {
                follows.unfollow(FollowKind.CLAN, result.clan.tag)
                local.update { it.copy(limitReached = false) }
            } else {
                val limit = follows.followClan(result.clan) == FollowResult.LIMIT_REACHED
                local.update { it.copy(limitReached = limit) }
            }
        }
    }

    fun toggleEditing() = local.update { it.copy(editing = !it.editing) }

    fun unfollow(item: Followed) {
        viewModelScope.launch { follows.unfollow(item.kind, item.tag) }
    }

    fun clearRecent() {
        viewModelScope.launch { preferences.clearRecentSearches() }
    }

    private fun show(outcome: SearchOutcome) = local.update { it.copy(outcome = LocalOutcome.Shown(outcome)) }
}
