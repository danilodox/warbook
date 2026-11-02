package io.github.danilodox.warbook.api

import kotlinx.serialization.Serializable

/** Jogador (telas O2, E1, T1, T3). Tags sem `#`. */
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
    val heroes: List<UnitLevelDto> = emptyList(),
    val troops: List<UnitLevelDto> = emptyList(),
    val spells: List<UnitLevelDto> = emptyList(),
)

@Serializable
data class ClanSummaryDto(val tag: String, val name: String, val badgeUrl: String? = null)

/** Herói, tropa ou feitiço com o nível atual e o máximo do jogo. */
@Serializable
data class UnitLevelDto(val name: String, val level: Int, val maxLevel: Int)

@Serializable
enum class ClanRole { MEMBER, ELDER, CO_LEADER, LEADER }
