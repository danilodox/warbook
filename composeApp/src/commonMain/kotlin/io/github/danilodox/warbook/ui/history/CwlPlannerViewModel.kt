package io.github.danilodox.warbook.ui.history

import androidx.lifecycle.viewModelScope
import io.github.danilodox.warbook.data.ApiResult
import io.github.danilodox.warbook.data.WarbookRepository
import io.github.danilodox.warbook.score.CwlPlanner
import io.github.danilodox.warbook.score.PlannerCandidate
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Escolha do planejador: o tamanho da escalação, quem está nela e se o usuário já mexeu na sugestão. */
data class PlannerSelection(
    val size: Int = CwlPlanner.SIZES.first(),
    val selected: Set<String> = emptySet(),
    val edited: Boolean = false,
)

/**
 * Planejador da CWL: os membros atuais do clã (com o que se sabe deles nas guerras guardadas), já na ordem
 * da sugestão. A escolha fica só aqui, enquanto a tela está aberta. Atualizar refaz a sugestão se o usuário
 * não mexeu; se mexeu, mantém a escolha e só tira quem saiu do clã.
 */
class CwlPlannerViewModel(clanTag: String, api: WarbookRepository) :
    LoadViewModel<List<PlannerCandidate>>({ plannerCandidates(clanTag, api) }) {

    private val mutableSelection = MutableStateFlow(PlannerSelection())
    val selection: StateFlow<PlannerSelection> = mutableSelection.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            state.collect { loaded ->
                val candidates = loaded.value ?: return@collect
                val tags = candidates.map { it.tag }.toSet()
                mutableSelection.update {
                    if (it.edited) {
                        it.copy(selected = it.selected intersect tags)
                    } else {
                        it.copy(selected = CwlPlanner.suggest(candidates, it.size))
                    }
                }
            }
        }
    }

    /** Muda o tamanho e volta para a sugestão desse tamanho. */
    fun setSize(size: Int) {
        val candidates = state.value.value.orEmpty()
        mutableSelection.value = PlannerSelection(size, CwlPlanner.suggest(candidates, size))
    }

    fun toggle(tag: String) = mutableSelection.update {
        val selected = if (tag in it.selected) it.selected - tag else it.selected + tag
        it.copy(selected = selected, edited = true)
    }

    fun restore() = setSize(selection.value.size)
}

/** Clã e estatísticas em paralelo; os dois precisam chegar. Na ordem da sugestão. */
internal suspend fun plannerCandidates(clanTag: String, api: WarbookRepository): ApiResult<List<PlannerCandidate>> =
    coroutineScope {
        val clan = async { api.clan(clanTag) }
        val planner = async { api.cwlPlanner(clanTag) }
        val clanResult = clan.await()
        val plannerResult = planner.await()
        when {
            clanResult is ApiResult.Failure -> clanResult
            plannerResult is ApiResult.Failure -> plannerResult
            else -> {
                val members = (clanResult as ApiResult.Success).value.members
                val stats = (plannerResult as ApiResult.Success).value.members.associateBy { it.tag }
                val candidates = members.map { PlannerCandidate(it.tag, it.name, it.townHallLevel, stats[it.tag]) }
                ApiResult.Success(CwlPlanner.rank(candidates))
            }
        }
    }
