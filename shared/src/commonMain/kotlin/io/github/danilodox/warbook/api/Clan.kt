package io.github.danilodox.warbook.api

import kotlinx.serialization.Serializable

/** Clã (telas C1, C2, T2). Tags sem `#`. */
@Serializable
data class ClanDto(
    val tag: String,
    val name: String,
    val badgeUrl: String? = null,
    val clanLevel: Int,
    val memberCount: Int,
    val description: String = "",
    val warLeague: String? = null,
    val capitalLeague: String? = null,
    val warWins: Int = 0,
    val warWinStreak: Int = 0,
    val isWarLogPublic: Boolean,
    val warFrequency: WarFrequency = WarFrequency.UNKNOWN,
    val requiredTrophies: Int = 0,
    val requiredTownHallLevel: Int? = null,
    val location: String? = null,
    val members: List<ClanMemberDto> = emptyList(),
)

@Serializable
data class ClanMemberDto(
    val tag: String,
    val name: String,
    val role: ClanRole,
    val townHallLevel: Int,
    val trophies: Int,
    val donations: Int,
    val donationsReceived: Int,
    val clanRank: Int,
)

@Serializable
enum class WarFrequency { ALWAYS, MORE_THAN_ONCE_PER_WEEK, ONCE_PER_WEEK, LESS_THAN_ONCE_PER_WEEK, NEVER, UNKNOWN }
