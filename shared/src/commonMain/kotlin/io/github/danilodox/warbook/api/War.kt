package io.github.danilodox.warbook.api

import kotlinx.serialization.Serializable

/**
 * Guerra atual de um clã (telas G1, G2, G4). Fora de guerra só vem [state].
 * Horários em milissegundos desde a época (UTC).
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
    val members: List<WarMemberDto> = emptyList(),
)

@Serializable
data class WarMemberDto(
    val tag: String,
    val name: String,
    val townHallLevel: Int,
    val mapPosition: Int,
    val attacks: List<WarAttackDto> = emptyList(),
)

@Serializable
data class WarAttackDto(val defenderTag: String, val stars: Int, val destructionPercentage: Int, val order: Int)
