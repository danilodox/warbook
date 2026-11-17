package io.github.danilodox.warbook.api

import kotlinx.serialization.Serializable

/**
 * Resposta de `GET /v1/clans/{tag}/capitalraidseasons`: os fins de semana de ataque à capital que a API
 * devolve, do mais recente para o mais antigo. Nada fica salvo no servidor.
 */
@Serializable
data class CapitalRaidsDto(val seasons: List<RaidSeasonDto>)

@Serializable
enum class RaidState { ONGOING, ENDED, UNKNOWN }

/**
 * Um fim de semana de raid. **Medalhas de raid**: [offensiveReward] é por ataque e [defensiveReward] é igual
 * para todos que atacaram (os dois ficam zero enquanto o raid não acaba); o total de cada um já vem em
 * [RaidMemberDto.medals]. [capitalTotalLoot] é **Ouro da Capital**: o raid não dá ouro comum.
 */
@Serializable
data class RaidSeasonDto(
    val state: RaidState,
    /** Epoch ms. */
    val startTime: Long? = null,
    val endTime: Long? = null,
    val capitalTotalLoot: Int = 0,
    val raidsCompleted: Int = 0,
    val totalAttacks: Int = 0,
    val enemyDistrictsDestroyed: Int = 0,
    val offensiveReward: Int = 0,
    val defensiveReward: Int = 0,
    /** Quem atacou, quem mais saqueou primeiro. */
    val members: List<RaidMemberDto> = emptyList(),
    /**
     * Membros atuais do clã que não aparecem em [members] (não atacaram). Só no raid mais recente: para os
     * anteriores não dá para saber quem estava no clã.
     */
    val notAttacked: List<RaidAbsentMemberDto> = emptyList(),
    /** Clãs que atacamos. */
    val attackLog: List<RaidClanDto> = emptyList(),
    /** Clãs que atacaram a nossa capital. */
    val defenseLog: List<RaidClanDto> = emptyList(),
)

/**
 * [attackLimit] já soma o ataque bônus (5 + 1 quando ganho). [medals] = medalhas por ataque × ataques +
 * medalhas de defesa (zero até o raid acabar).
 */
@Serializable
data class RaidMemberDto(
    val tag: String,
    val name: String,
    val attacks: Int,
    val attackLimit: Int,
    val capitalResourcesLooted: Int,
    val medals: Int = 0,
)

@Serializable
data class RaidAbsentMemberDto(val tag: String, val name: String, val townHallLevel: Int)

/** Um clã atacado ou que atacou: quantos ataques e quantos distritos caíram. */
@Serializable
data class RaidClanDto(
    val tag: String,
    val name: String,
    val level: Int = 0,
    val badgeUrl: String? = null,
    val attackCount: Int,
    val districtCount: Int,
    val districtsDestroyed: Int,
)
