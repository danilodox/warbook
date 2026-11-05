package io.github.danilodox.warbook.api

import kotlinx.serialization.Serializable

/**
 * Jogador (telas O2, E1, T1, T3), só da vila principal. Tags sem `#`.
 * [donations], [donationsReceived] e [attackWins] são da temporada atual (zeram a cada temporada).
 */
@Serializable
data class PlayerDto(
    val tag: String,
    val name: String,
    val townHallLevel: Int,
    val expLevel: Int,
    val trophies: Int,
    val warStars: Int,
    val attackWins: Int,
    val donations: Int,
    val donationsReceived: Int,
    val clan: ClanSummaryDto? = null,
    val role: ClanRole? = null,
    val league: String? = null,
    val heroes: List<UnitLevelDto> = emptyList(),
    val troops: List<UnitLevelDto> = emptyList(),
    val spells: List<UnitLevelDto> = emptyList(),
    val siegeMachines: List<UnitLevelDto> = emptyList(),
    val pets: List<UnitLevelDto> = emptyList(),
    val heroEquipment: List<UnitLevelDto> = emptyList(),
)

@Serializable
data class ClanSummaryDto(val tag: String, val name: String, val badgeUrl: String? = null)

/** Herói, tropa, feitiço, máquina, pet ou equipamento com o nível atual e o máximo do jogo. */
@Serializable
data class UnitLevelDto(val name: String, val level: Int, val maxLevel: Int)

@Serializable
enum class ClanRole { MEMBER, ELDER, CO_LEADER, LEADER }
