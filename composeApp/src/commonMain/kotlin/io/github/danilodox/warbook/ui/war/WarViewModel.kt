package io.github.danilodox.warbook.ui.war

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.danilodox.warbook.api.ClanSummaryDto
import io.github.danilodox.warbook.api.CurrentWarDto
import io.github.danilodox.warbook.api.CwlDto
import io.github.danilodox.warbook.data.AccountsRepository
import io.github.danilodox.warbook.data.ApiResult
import io.github.danilodox.warbook.data.AppError
import io.github.danilodox.warbook.data.AppPreferences
import io.github.danilodox.warbook.data.Cached
import io.github.danilodox.warbook.data.CachedProfile
import io.github.danilodox.warbook.data.ProfileRepository
import io.github.danilodox.warbook.data.WarRepository
import io.github.danilodox.warbook.ui.me.activeOrFirst
import io.github.danilodox.warbook.war.SummaryOptions
import io.github.danilodox.warbook.war.SummaryPart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class WarTab { CURRENT, CWL }

/** O salvo de uma sub-aba e a atualização dela. Com [cached], [error] vira a faixa de offline. */
data class Section<T>(val cached: Cached<T>? = null, val refreshing: Boolean = false, val error: AppError? = null)

data class WarUiState(
    /** `false` só até a primeira leitura do banco. */
    val loaded: Boolean = false,
    /** Conta ativa; `null` = sem conta ou "só explorar" (a tela convida a adicionar). */
    val accountTag: String? = null,
    /** `false` enquanto o perfil da conta nunca foi salvo: ainda não se sabe o clã. */
    val profileKnown: Boolean = false,
    val clan: ClanSummaryDto? = null,
    /** Atualização do perfil quando ele ainda não existe (só então a aba Guerra busca o perfil). */
    val profile: Section<Unit> = Section(),
    val tab: WarTab = WarTab.CURRENT,
    val war: Section<CurrentWarDto> = Section(),
    val cwl: Section<CwlDto> = Section(),
    /** Rodada da CWL escolhida nas fichas; `null` = a que o servidor indica. */
    val selectedRound: Int? = null,
    /** Partes do resumo do WhatsApp, salvas no aparelho. */
    val summaryOptions: SummaryOptions = SummaryOptions(),
    /** Guerra de um clã escolhido (tela Clã), não o da conta: não convida a adicionar conta nem olha o perfil. */
    val fixedClan: Boolean = false,
) {
    val round: Int? get() = selectedRound ?: cwl.cached?.value?.currentRound
}

/** Atualização em andamento ou o erro dela, sempre de uma [key] (conta ou clã): trocar não mostra o erro do outro. */
private data class Load(val key: String? = null, val running: Boolean = false, val error: AppError? = null) {
    fun forKey(current: String?) = takeIf { it.key == current && current != null }
}

private data class Loads(val profile: Load = Load(), val war: Load = Load(), val cwl: Load = Load())

private data class Selection(val tab: WarTab = WarTab.CURRENT, val round: Int? = null)

/** Perfil salvo de [tag] (que pode nem existir ainda). */
private data class ProfileLookup(val tag: String?, val cached: CachedProfile?)

/**
 * Aba Guerra (G1–G4): guerra atual e CWL do clã da conta ativa, sempre com o último resultado salvo.
 * Com [fixedClanTag] (sub-aba Guerra da tela Clã), mostra a guerra desse clã; "você" continua sendo a conta ativa.
 * Só busca no servidor quando a aba abre ([onOpen]), ao trocar de sub-aba pela primeira vez e ao puxar.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WarViewModel(
    private val accounts: AccountsRepository,
    private val profiles: ProfileRepository,
    private val wars: WarRepository,
    private val preferences: AppPreferences,
    private val fixedClanTag: String? = null,
) : ViewModel() {
    private val selection = MutableStateFlow(Selection())
    private val loads = MutableStateFlow(Loads())

    /** Sub-abas já atualizadas desde que a aba abriu. */
    private val visited = mutableSetOf<WarTab>()

    private val activeTag: Flow<String?> = accounts.accounts.map { it.activeOrFirst()?.tag }.distinctUntilChanged()

    private val profile: Flow<ProfileLookup> = activeTag.flatMapLatest { tag ->
        if (tag == null) flowOf(ProfileLookup(null, null)) else profiles.observe(tag).map { ProfileLookup(tag, it) }
    }

    private val clanTag: Flow<String?> = fixedClanTag?.let { flowOf(it) }
        ?: profile.map { it.cached?.player?.clan?.tag }.distinctUntilChanged()

    private val war = clanTag.flatMapLatest { tag -> if (tag == null) flowOf(null) else wars.observeWar(tag) }

    private val cwl = clanTag.flatMapLatest { tag -> if (tag == null) flowOf(null) else wars.observeCwl(tag) }

    val state: StateFlow<WarUiState> = combine(profile, war, cwl, selection, loads) { lookup, war, cwl, sel, loads ->
        // Na troca de conta, o perfil antigo ainda pode chegar um instante; não mostra.
        val cached = lookup.cached?.takeIf { it.player.tag == lookup.tag }
        // O nome do clã fixo não importa aqui: a tela usa só a tag (o nome vem na própria guerra).
        val clan = if (fixedClanTag != null) ClanSummaryDto(fixedClanTag, "") else cached?.player?.clan
        val profileLoad = loads.profile.forKey(lookup.tag)
        val warLoad = loads.war.forKey(clan?.tag)
        val cwlLoad = loads.cwl.forKey(clan?.tag)
        WarUiState(
            loaded = true,
            accountTag = lookup.tag,
            profileKnown = fixedClanTag != null || cached != null,
            clan = clan,
            profile = Section(null, profileLoad?.running == true, profileLoad?.error),
            tab = sel.tab,
            war = Section(war, warLoad?.running == true, warLoad?.error),
            cwl = Section(cwl, cwlLoad?.running == true, cwlLoad?.error),
            selectedRound = sel.round,
            fixedClan = fixedClanTag != null,
        )
    }.combine(preferences.summaryOptions) { state, options -> state.copy(summaryOptions = options) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, WarUiState())

    /** A aba ficou visível: atualiza a sub-aba aberta. */
    fun onOpen() {
        visited.clear()
        selection.update { it.copy(round = null) }
        refresh(selection.value.tab)
    }

    fun selectTab(tab: WarTab) {
        selection.update { it.copy(tab = tab) }
        if (tab !in visited) refresh(tab)
    }

    fun selectRound(round: Int) {
        selection.update { it.copy(round = round) }
    }

    /** Liga ou desliga uma parte do resumo; a escolha fica salva para a próxima guerra. */
    fun toggleSummaryPart(part: SummaryPart) {
        viewModelScope.launch { preferences.updateSummaryOptions { it.toggle(part) } }
    }

    /** Puxar para atualizar. */
    fun refresh() = refresh(selection.value.tab)

    private fun refresh(tab: WarTab) {
        visited += tab
        viewModelScope.launch {
            val clanTag = fixedClanTag ?: activeTag.first()?.let { clanOf(it) } ?: return@launch
            // Clã da conta ativa: sempre salvo, mesmo antes de a conta saber a tag do clã (banco v3).
            // Clã fixo: salvo só se for de uma conta ou seguido (o repositório decide).
            val keep = fixedClanTag == null
            when (tab) {
                WarTab.CURRENT -> track(clanTag, { copy(war = it) }, { war }) { wars.refreshWar(clanTag, keep) }
                WarTab.CWL -> track(clanTag, { copy(cwl = it) }, { cwl }) { wars.refreshCwl(clanTag, keep) }
            }
        }
    }

    /** Clã do perfil salvo; sem perfil salvo, busca o perfil primeiro. `null` = sem clã ou falhou. */
    private suspend fun clanOf(accountTag: String): String? {
        profiles.observe(accountTag).first()?.let { return it.player.clan?.tag }
        var clanTag: String? = null
        track(accountTag, { copy(profile = it) }, { profile }) {
            profiles.refresh(accountTag).also { result ->
                clanTag = (result as? ApiResult.Success)?.value?.clan?.tag
            }
        }
        return clanTag
    }

    /** Roda [fetch] marcando a atualização de [key]; não repete se a mesma já está rodando. */
    private suspend fun track(
        key: String,
        set: Loads.(Load) -> Loads,
        get: Loads.() -> Load,
        fetch: suspend () -> ApiResult<*>,
    ) {
        val current = loads.value.get()
        if (current.key == key && current.running) return
        loads.update { it.set(Load(key, running = true)) }
        val error = (fetch() as? ApiResult.Failure)?.error
        loads.update { if (it.get().key == key) it.set(Load(key, running = false, error = error)) else it }
    }
}
