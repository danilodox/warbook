package io.github.danilodox.warbook.api

import kotlinx.serialization.Serializable

/**
 * Guerra atual de um clã (telas G1, G2, G4), ou a guerra do clã numa rodada da CWL ([isCwl]).
 * Fora de guerra só vem [state]. [clan] é sempre o clã consultado. Horários em milissegundos desde a época (UTC).
 */
@Serializable
data class CurrentWarDto(
    val state: WarState,
    val teamSize: Int = 0,
    val attacksPerMember: Int = 0,
    val preparationStartTime: Long? = null,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val clan: WarClanDto? = null,
    val opponent: WarClanDto? = null,
    val isCwl: Boolean = false,
)

@Serializable
enum class WarState { NOT_IN_WAR, MATCHMAKING, PREPARATION, IN_WAR, WAR_ENDED }

@Serializable
data class WarClanDto(
    val tag: String,
    val name: String,
    val badgeUrl: String? = null,
    val clanLevel: Int,
    val stars: Int,
    val destructionPercentage: Double,
    val attacks: Int,
    /** Ordenados por [WarMemberDto.mapPosition]. */
    val members: List<WarMemberDto> = emptyList(),
)

@Serializable
data class WarMemberDto(
    val tag: String,
    val name: String,
    val townHallLevel: Int,
    /** Posição no mapa, de 1 a [CurrentWarDto.teamSize], sem buracos. */
    val mapPosition: Int,
    /** Ataques feitos, em ordem. */
    val attacks: List<WarAttackDto> = emptyList(),
    /** Quantas vezes foi atacado. */
    val opponentAttacks: Int = 0,
    /** Melhor ataque sofrido (mais estrelas, depois mais destruição). */
    val bestOpponentAttack: WarAttackDto? = null,
)

/**
 * Um ataque. [order] é a ordem do ataque na guerra (1 = primeiro dos dois clãs).
 * [defenderPosition] é a posição do alvo no mapa; `null` se o alvo não veio na resposta.
 */
@Serializable
data class WarAttackDto(
    val defenderTag: String,
    val stars: Int,
    val destructionPercentage: Int,
    val order: Int,
    val attackerTag: String = "",
    val defenderPosition: Int? = null,
)
