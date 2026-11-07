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
    val joinType: ClanJoinType = ClanJoinType.UNKNOWN,
    val requiredTrophies: Int = 0,
    val requiredTownHallLevel: Int? = null,
    val location: String? = null,
    val members: List<ClanMemberDto> = emptyList(),
)

/** [donations] e [donationsReceived] são da temporada atual (zeram a cada temporada). */
@Serializable
data class ClanMemberDto(
    val tag: String,
    val name: String,
    val role: ClanRole,
    val townHallLevel: Int,
    val expLevel: Int = 0,
    val trophies: Int,
    val league: LeagueDto? = null,
    val donations: Int,
    val donationsReceived: Int,
    val clanRank: Int,
)

/** Liga de troféus, com o ícone que a própria API entrega. */
@Serializable
data class LeagueDto(val name: String, val iconUrl: String? = null)

@Serializable
enum class WarFrequency { ALWAYS, MORE_THAN_ONCE_PER_WEEK, ONCE_PER_WEEK, LESS_THAN_ONCE_PER_WEEK, NEVER, UNKNOWN }

/** Como se entra no clã. */
@Serializable
enum class ClanJoinType { OPEN, INVITE_ONLY, CLOSED, UNKNOWN }

/** Resposta de `GET /v1/clans/search?name=`. */
@Serializable
data class ClanSearchResponse(val items: List<ClanSearchResultDto>)

/** Clã achado pelo nome (B2): só o que a lista mostra. */
@Serializable
data class ClanSearchResultDto(
    val tag: String,
    val name: String,
    val badgeUrl: String? = null,
    val clanLevel: Int,
    val memberCount: Int,
    val location: String? = null,
)

/** Regras da busca de clã por nome, iguais no app e no servidor. */
object ClanSearch {
    /** A API do jogo recusa nomes com menos de 3 letras. */
    const val MIN_LENGTH = 3
    const val MAX_LENGTH = 50
    const val MAX_RESULTS = 20

    /** Nome aparado, ou `null` se for curto ou longo demais. */
    fun normalize(raw: String): String? = raw.trim().takeIf { it.length in MIN_LENGTH..MAX_LENGTH }
}
