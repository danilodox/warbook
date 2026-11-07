package io.github.danilodox.warbook.coc

import kotlinx.serialization.Serializable

// Formato da API oficial do Clash of Clans. Fica só no servidor: o app recebe os DTOs do `shared`.
// Só os campos usados; o resto é ignorado (ignoreUnknownKeys).

@Serializable
internal data class CocPlayer(
    val tag: String,
    val name: String,
    val townHallLevel: Int,
    val expLevel: Int,
    val trophies: Int,
    val warStars: Int = 0,
    val attackWins: Int = 0,
    val donations: Int = 0,
    val donationsReceived: Int = 0,
    val clan: CocClanRef? = null,
    val role: String? = null,
    val league: CocLeague? = null,
    /** Liga do ranqueado novo; quando vem, vale mais que [league]. */
    val leagueTier: CocLeague? = null,
    val heroes: List<CocUnit> = emptyList(),
    /** Na API, `troops` também traz pets, máquinas de cerco e supertropas. */
    val troops: List<CocUnit> = emptyList(),
    val spells: List<CocUnit> = emptyList(),
    val heroEquipment: List<CocUnit> = emptyList(),
)

/** Corpo de `POST /players/{tag}/verifytoken`. */
@Serializable
internal data class CocVerifyTokenRequest(val token: String) {
    override fun toString(): String = "CocVerifyTokenRequest(token=***)"
}

/** A resposta também repete a tag e o token; só o [status] é lido (o resto é ignorado e descartado). */
@Serializable
internal data class CocVerifyTokenResponse(val status: String) {
    companion object {
        const val STATUS_OK = "ok"
    }
}

@Serializable
internal data class CocClanRef(val tag: String, val name: String, val badgeUrls: CocBadgeUrls? = null)

@Serializable
internal data class CocBadgeUrls(val small: String? = null, val medium: String? = null, val large: String? = null)

@Serializable
internal data class CocUnit(val name: String, val level: Int, val maxLevel: Int, val village: String = VILLAGE_HOME) {
    companion object {
        const val VILLAGE_HOME = "home"
    }
}

@Serializable
internal data class CocNamed(val name: String)

@Serializable
internal data class CocLeague(val name: String, val iconUrls: CocIconUrls? = null)

@Serializable
internal data class CocIconUrls(
    val tiny: String? = null,
    val small: String? = null,
    val medium: String? = null,
    val large: String? = null,
)

/** `clans?name=`: a lista vem em `items`, cada um no formato do clã sem `memberList`. */
@Serializable
internal data class CocClanList(val items: List<CocClan> = emptyList())

@Serializable
internal data class CocClan(
    val tag: String,
    val name: String,
    val type: String? = null,
    val badgeUrls: CocBadgeUrls? = null,
    val clanLevel: Int,
    val members: Int,
    val description: String? = null,
    val warLeague: CocNamed? = null,
    val capitalLeague: CocNamed? = null,
    val warWins: Int = 0,
    val warWinStreak: Int = 0,
    val isWarLogPublic: Boolean = false,
    val warFrequency: String? = null,
    val requiredTrophies: Int = 0,
    val requiredTownhallLevel: Int? = null,
    val location: CocNamed? = null,
    val memberList: List<CocClanMember> = emptyList(),
)

@Serializable
internal data class CocClanMember(
    val tag: String,
    val name: String,
    val role: String,
    val townHallLevel: Int,
    val expLevel: Int = 0,
    val trophies: Int,
    val league: CocLeague? = null,
    val leagueTier: CocLeague? = null,
    val donations: Int = 0,
    val donationsReceived: Int = 0,
    val clanRank: Int,
)

@Serializable
internal data class CocWar(
    val state: String,
    val teamSize: Int = 0,
    val attacksPerMember: Int = 0,
    val preparationStartTime: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val clan: CocWarClan? = null,
    val opponent: CocWarClan? = null,
)

@Serializable
internal data class CocWarClan(
    val tag: String? = null,
    val name: String? = null,
    val badgeUrls: CocBadgeUrls? = null,
    val clanLevel: Int = 0,
    val stars: Int = 0,
    val destructionPercentage: Double = 0.0,
    val attacks: Int = 0,
    val members: List<CocWarMember> = emptyList(),
)

/** Nos membros da guerra a API escreve `townhallLevel` (h minúsculo). */
@Serializable
internal data class CocWarMember(
    val tag: String,
    val name: String,
    val townhallLevel: Int,
    val mapPosition: Int,
    val attacks: List<CocWarAttack> = emptyList(),
    val opponentAttacks: Int = 0,
    val bestOpponentAttack: CocWarAttack? = null,
)

@Serializable
internal data class CocWarAttack(
    val attackerTag: String = "",
    val defenderTag: String,
    val stars: Int,
    val destructionPercentage: Int,
    val order: Int,
)

/** `clans/{tag}/currentwar/leaguegroup`: o grupo da CWL. As guerras de cada rodada vêm por `warTags`. */
@Serializable
internal data class CocLeagueGroup(
    val state: String,
    val season: String? = null,
    val clans: List<CocLeagueClan> = emptyList(),
    val rounds: List<CocLeagueRound> = emptyList(),
)

@Serializable
internal data class CocLeagueClan(
    val tag: String,
    val name: String,
    val clanLevel: Int = 0,
    val badgeUrls: CocBadgeUrls? = null,
)

/** Rodada ainda não sorteada vem com `#0` em todas as posições. */
@Serializable
internal data class CocLeagueRound(val warTags: List<String> = emptyList())

/** Corpo de erro da API oficial. */
@Serializable
internal data class CocClientError(val reason: String? = null, val message: String? = null)
