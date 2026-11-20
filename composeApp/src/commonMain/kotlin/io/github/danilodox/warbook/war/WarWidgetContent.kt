package io.github.danilodox.warbook.war

import io.github.danilodox.warbook.api.CurrentWarDto
import io.github.danilodox.warbook.api.WarState
import io.github.danilodox.warbook.data.AccountsRepository
import io.github.danilodox.warbook.data.ApiResult
import io.github.danilodox.warbook.data.ProfileRepository
import io.github.danilodox.warbook.data.WarRepository
import io.github.danilodox.warbook.ui.me.activeOrFirst
import kotlinx.coroutines.flow.first

/** O que o widget mostra: [title] e [status] sempre; [score] e [pending] só com guerra. */
data class WarWidgetData(val title: String, val status: String, val score: String? = null, val pending: String? = null)

/** Textos só do widget; o resto vem do [SummaryTexts] do resumo da guerra. */
interface WarWidgetTexts {
    val appName: String
    val noAccount: String
    val noClan: String
    val noWar: String
    val matchmaking: String
    val allAttacked: String
}

object PortugueseWarWidgetTexts : WarWidgetTexts {
    override val appName = "Warbook"
    override val noAccount = "Abra o Warbook e escolha uma conta"
    override val noClan = "A conta ativa não está em um clã"
    override val noWar = "Sem guerra agora"
    override val matchmaking = "Procurando adversário"
    override val allAttacked = "Todo mundo atacou"
}

object EnglishWarWidgetTexts : WarWidgetTexts {
    override val appName = "Warbook"
    override val noAccount = "Open Warbook and pick an account"
    override val noClan = "The active account isn't in a clan"
    override val noWar = "No war right now"
    override val matchmaking = "Searching for an opponent"
    override val allAttacked = "Everyone attacked"
}

fun warWidgetTextsFor(languageCode: String): WarWidgetTexts =
    if (languageCode.lowercase().startsWith("pt")) PortugueseWarWidgetTexts else EnglishWarWidgetTexts

/** O que se sabe da conta ativa para o widget. */
sealed interface WidgetWar {
    data object NoAccount : WidgetWar
    data object NoClan : WidgetWar

    /** [war] nulo: nunca veio do servidor nem ficou salva. */
    data class Clan(val war: CurrentWarDto?) : WidgetWar
}

object WarWidgetContent {

    fun of(widget: WidgetWar, now: Long, texts: WarWidgetTexts, summary: SummaryTexts): WarWidgetData = when (widget) {
        WidgetWar.NoAccount -> WarWidgetData(texts.appName, texts.noAccount)
        WidgetWar.NoClan -> WarWidgetData(texts.appName, texts.noClan)
        is WidgetWar.Clan -> war(widget.war, now, texts, summary)
    }

    private fun war(war: CurrentWarDto?, now: Long, texts: WarWidgetTexts, summary: SummaryTexts): WarWidgetData {
        val ours = war?.clan
        val theirs = war?.opponent
        if (war == null || ours == null || theirs == null) {
            val status = if (war?.state == WarState.MATCHMAKING) texts.matchmaking else texts.noWar
            return WarWidgetData(texts.appName, status)
        }
        val title = "${ours.name} × ${theirs.name}"
        val score = "⭐ ${ours.stars} × ${theirs.stars} · " +
            "${summary.decimal(ours.destructionPercentage)}% × ${summary.decimal(theirs.destructionPercentage)}%"
        return when (war.state) {
            WarState.PREPARATION ->
                WarWidgetData(title, summary.preparation(summary.duration((war.startTime ?: now) - now)))
            WarState.IN_WAR -> {
                val pending = war.pendingAttackers()
                WarWidgetData(
                    title = title,
                    status = summary.endsIn(summary.duration((war.endTime ?: now) - now)),
                    score = score,
                    pending = if (pending.isEmpty()) {
                        texts.allAttacked
                    } else {
                        summary.pendingTitle(pending.sumOf { it.remaining }, pending.size)
                    },
                )
            }
            WarState.WAR_ENDED -> WarWidgetData(title, war.result()?.let(summary::result).orEmpty(), score)
            WarState.NOT_IN_WAR, WarState.MATCHMAKING -> WarWidgetData(texts.appName, texts.noWar)
        }
    }
}

/**
 * Guerra do clã da conta ativa para o widget: tenta o servidor e, se falhar, fica com a salva. Sem perfil
 * salvo, busca o perfil para saber o clã.
 */
class WarWidgetLoader(
    private val accounts: AccountsRepository,
    private val profiles: ProfileRepository,
    private val wars: WarRepository,
) {
    suspend fun load(): WidgetWar {
        val tag = accounts.accounts.first().activeOrFirst()?.tag ?: return WidgetWar.NoAccount
        val player = profiles.observe(tag).first()?.player ?: (profiles.refresh(tag) as? ApiResult.Success)?.value
        val clanTag = player?.clan?.tag
        return when {
            // Sem perfil salvo e sem rede não dá para saber o clã: "sem guerra", não "sem clã".
            player == null -> WidgetWar.Clan(null)
            clanTag == null -> WidgetWar.NoClan
            else -> {
                wars.refreshWar(clanTag)
                WidgetWar.Clan(wars.observeWar(clanTag).first()?.value)
            }
        }
    }
}
