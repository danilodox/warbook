package io.github.danilodox.warbook.ui.me

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.danilodox.warbook.data.Account
import io.github.danilodox.warbook.data.AccountsRepository
import io.github.danilodox.warbook.data.ApiResult
import io.github.danilodox.warbook.data.AppError
import io.github.danilodox.warbook.data.CachedProfile
import io.github.danilodox.warbook.data.ProfileRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MeState(
    /** `false` só até a primeira leitura do banco. */
    val loaded: Boolean = false,
    val accounts: List<Account> = emptyList(),
    val active: Account? = null,
    /** Último perfil salvo da conta ativa. */
    val profile: CachedProfile? = null,
    val refreshing: Boolean = false,
    /** Erro da última atualização; com [profile] salvo, a tela mostra a faixa de offline. */
    val refreshError: AppError? = null,
)

/** Atualização em andamento ou o erro dela, sempre de uma conta: trocar de conta não mostra o erro da outra. */
private data class Refresh(val tag: String? = null, val running: Boolean = false, val error: AppError? = null)

/** Aba Eu (E1 e E2): perfil da conta ativa, salvo para abrir sem rede, e a troca de conta. */
@OptIn(ExperimentalCoroutinesApi::class)
class MeViewModel(private val accounts: AccountsRepository, private val profiles: ProfileRepository) : ViewModel() {
    private val refresh = MutableStateFlow(Refresh())

    private val activeTag = accounts.accounts
        .map { list -> list.activeOrFirst()?.tag }
        .distinctUntilChanged()

    private val profile = activeTag.flatMapLatest { tag -> if (tag == null) flowOf(null) else profiles.observe(tag) }

    val state: StateFlow<MeState> = combine(accounts.accounts, profile, refresh) { list, cached, status ->
        val active = list.activeOrFirst()
        val current = status.takeIf { it.tag == active?.tag }
        MeState(
            loaded = true,
            accounts = list,
            active = active,
            // Na troca de conta, o perfil antigo ainda pode chegar um instante; não mostra.
            profile = cached?.takeIf { it.player.tag == active?.tag },
            refreshing = current?.running == true,
            refreshError = current?.error,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, MeState())

    init {
        // Abriu a aba ou trocou de conta: busca o perfil novo (o salvo aparece enquanto isso).
        viewModelScope.launch { activeTag.filterNotNull().collect { refresh(it) } }
    }

    fun refresh() {
        state.value.active?.let { refresh(it.tag) }
    }

    fun switchTo(tag: String) {
        viewModelScope.launch { accounts.activate(tag) }
    }

    fun remove(tag: String) {
        viewModelScope.launch { accounts.remove(tag) }
    }

    private fun refresh(tag: String) {
        if (refresh.value.tag == tag && refresh.value.running) return
        refresh.value = Refresh(tag, running = true)
        viewModelScope.launch {
            val error = (profiles.refresh(tag) as? ApiResult.Failure)?.error
            refresh.update { if (it.tag == tag) Refresh(tag, running = false, error = error) else it }
        }
    }
}

private fun List<Account>.activeOrFirst(): Account? = firstOrNull { it.isActive } ?: firstOrNull()

/** Quanto tempo faz que o perfil salvo veio do servidor. */
sealed interface Age {
    data object JustNow : Age

    data class Minutes(val value: Long) : Age

    data class Hours(val value: Long) : Age

    data class Days(val value: Long) : Age
}

private const val MINUTE = 60_000L
private const val HOUR = 60 * MINUTE
private const val DAY = 24 * HOUR

fun ageOf(fetchedAt: Long, now: Long): Age {
    val elapsed = (now - fetchedAt).coerceAtLeast(0)
    return when {
        elapsed < MINUTE -> Age.JustNow
        elapsed < HOUR -> Age.Minutes(elapsed / MINUTE)
        elapsed < DAY -> Age.Hours(elapsed / HOUR)
        else -> Age.Days(elapsed / DAY)
    }
}
