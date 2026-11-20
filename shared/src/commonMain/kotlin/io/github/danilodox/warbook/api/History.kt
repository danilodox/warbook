package io.github.danilodox.warbook.api

import io.github.danilodox.warbook.score.AttackMark
import kotlinx.serialization.Serializable

// Histórico de guerras dos clãs monitorados (G5). Horários em epoch ms; tags sem `#`.

@Serializable
enum class WarKind { WAR, CWL }

@Serializable
enum class WarOutcome { WIN, LOSE, TIE }

/**
 * Uma guerra no histórico, do ponto de vista do clã. [detailed]: tem escalação e ataques (retrato do
 * agendador), então tem nota; `false` = só o placar (veio do warlog).
 */
@Serializable
data class WarSummaryDto(
    val id: Long,
    val kind: WarKind,
    val state: WarState,
    val endTime: Long,
    val teamSize: Int,
    val attacksPerMember: Int,
    val stars: Int,
    val destruction: Double,
    val opponentStars: Int,
    val opponentDestruction: Double,
    val detailed: Boolean,
    val opponentTag: String? = null,
    val opponentName: String? = null,
    val opponentBadgeUrl: String? = null,
    val result: WarOutcome? = null,
    val cwlSeason: String? = null,
)

/** `GET /v1/clans/{tag}/wars`: da mais recente para a mais antiga. Vazio se o clã nunca foi monitorado. */
@Serializable
data class WarHistoryDto(val wars: List<WarSummaryDto>) {
    companion object {
        const val DEFAULT_LIMIT = 50
        const val MAX_LIMIT = 100
    }
}

/** Um ataque com a nota. O alvo vem com nome, posição e CV da escalação. */
@Serializable
data class ScoredAttackDto(
    val order: Int,
    val defenderTag: String,
    val stars: Int,
    val destruction: Int,
    val factor: Double,
    val points: Double,
    val mark: AttackMark? = null,
    val defenderName: String? = null,
    val defenderPosition: Int? = null,
    val defenderTownHall: Int? = null,
)

/** Nota do membro na guerra. [missed] e [penalty] só contam depois do fim. */
@Serializable
data class MemberWarScoreDto(
    val tag: String,
    val name: String,
    val townHall: Int,
    val mapPosition: Int,
    val attacks: List<ScoredAttackDto>,
    val missed: Int,
    val penalty: Double,
    val total: Double,
)

/** `GET /v1/clans/{tag}/wars/{id}`. [members] vazio quando a guerra não é [WarSummaryDto.detailed]. */
@Serializable
data class WarDetailDto(val war: WarSummaryDto, val members: List<MemberWarScoreDto>)

/**
 * Atividade de um membro: nas [ClanActivityDto.WARS] últimas guerras comuns encerradas, em quantas estava
 * escalado e quantos ataques usou; na última temporada da CWL guardada, em quantas rodadas estava e em
 * quantas atacou.
 */
@Serializable
data class MemberActivityDto(
    val tag: String,
    val name: String,
    val townHall: Int,
    val wars: Int,
    val attacksUsed: Int,
    val attacksAvailable: Int,
    val cwlRounds: Int,
    val cwlAttacksUsed: Int,
)

/**
 * `GET /v1/clans/{tag}/activity`. Os membros são os que aparecem nessas guerras (com o nome mais recente),
 * de quem mais deixou de atacar para quem menos. [warsCounted]: quantas guerras comuns entraram (até
 * [WARS]); [cwlRounds]: rodadas guardadas da temporada [cwlSeason].
 */
@Serializable
data class ClanActivityDto(
    val warsCounted: Int,
    val members: List<MemberActivityDto>,
    val cwlRounds: Int = 0,
    val cwlSeason: String? = null,
) {
    companion object {
        const val WARS = 10

        /** Rodadas de uma temporada da CWL. */
        const val CWL_ROUNDS = 7
    }
}

/** Uma guerra encerrada do jogador. */
@Serializable
data class PlayerWarEntryDto(
    val warId: Long,
    val kind: WarKind,
    val endTime: Long,
    val townHall: Int,
    val mapPosition: Int,
    val attacksUsed: Int,
    val attacksAvailable: Int,
    val total: Double,
    val easyTargets: Int,
    val cleanups: Int,
    val opponentName: String? = null,
)

/**
 * `GET /v1/clans/{tag}/players/{playerTag}/history`: as guerras encerradas (com nota) do jogador nesse clã,
 * da mais recente para a mais antiga. [average]: média da nota. [trend]: média das [TREND_WINDOW] mais
 * recentes menos a das [TREND_WINDOW] anteriores (positivo = melhorando); nula sem guerras para comparar.
 */
@Serializable
data class PlayerHistoryDto(
    val tag: String,
    val wars: List<PlayerWarEntryDto>,
    val attacksUsed: Int,
    val attacksAvailable: Int,
    val easyTargets: Int,
    val cleanups: Int,
    val name: String? = null,
    val average: Double? = null,
    val trend: Double? = null,
) {
    companion object {
        const val TREND_WINDOW = 5

        /** Guerras com nota que entram no histórico do jogador. */
        const val MAX_WARS = 30
    }
}
