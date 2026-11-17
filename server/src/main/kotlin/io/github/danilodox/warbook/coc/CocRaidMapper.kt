package io.github.danilodox.warbook.coc

import io.github.danilodox.warbook.api.CapitalRaidsDto
import io.github.danilodox.warbook.api.ClanMemberDto
import io.github.danilodox.warbook.api.RaidAbsentMemberDto
import io.github.danilodox.warbook.api.RaidClanDto
import io.github.danilodox.warbook.api.RaidMemberDto
import io.github.danilodox.warbook.api.RaidSeasonDto
import io.github.danilodox.warbook.api.RaidState
import kotlinx.serialization.Serializable

// Formato da Supercell para `GET /clans/{tag}/capitalraidseasons`. Só os campos usados.

@Serializable
internal data class CocRaidSeasonList(val items: List<CocRaidSeason> = emptyList())

@Serializable
internal data class CocRaidSeason(
    val state: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val capitalTotalLoot: Int = 0,
    val raidsCompleted: Int = 0,
    val totalAttacks: Int = 0,
    val enemyDistrictsDestroyed: Int = 0,
    val offensiveReward: Int = 0,
    val defensiveReward: Int = 0,
    val members: List<CocRaidMember> = emptyList(),
    val attackLog: List<CocRaidAttack> = emptyList(),
    val defenseLog: List<CocRaidDefense> = emptyList(),
)

@Serializable
internal data class CocRaidMember(
    val tag: String,
    val name: String,
    val attacks: Int = 0,
    val attackLimit: Int = 0,
    val bonusAttackLimit: Int = 0,
    val capitalResourcesLooted: Int = 0,
)

@Serializable
internal data class CocRaidClan(
    val tag: String,
    val name: String,
    val level: Int = 0,
    val badgeUrls: CocBadgeUrls? = null,
)

@Serializable
internal data class CocRaidAttack(
    val defender: CocRaidClan,
    val attackCount: Int = 0,
    val districtCount: Int = 0,
    val districtsDestroyed: Int = 0,
)

@Serializable
internal data class CocRaidDefense(
    val attacker: CocRaidClan,
    val attackCount: Int = 0,
    val districtCount: Int = 0,
    val districtsDestroyed: Int = 0,
)

internal fun cocRaidState(value: String?): RaidState = when (value) {
    "ongoing" -> RaidState.ONGOING
    "ended" -> RaidState.ENDED
    else -> RaidState.UNKNOWN
}

/**
 * [currentMembers] = membros atuais do clã: quem não está na lista de quem atacou entra em `notAttacked`,
 * só na temporada mais recente (a primeira da lista).
 */
internal fun CocRaidSeasonList.toDto(currentMembers: List<ClanMemberDto>): CapitalRaidsDto = CapitalRaidsDto(
    items.mapIndexed { index, season ->
        season.toDto(absent = if (index == 0) season.absentFrom(currentMembers) else emptyList())
    },
)

private fun CocRaidSeason.absentFrom(currentMembers: List<ClanMemberDto>): List<RaidAbsentMemberDto> {
    val attacked = members.mapTo(mutableSetOf()) { it.tag.withoutHash() }
    return currentMembers
        .filter { it.tag !in attacked }
        .map { RaidAbsentMemberDto(it.tag, it.name, it.townHallLevel) }
}

private fun CocRaidSeason.toDto(absent: List<RaidAbsentMemberDto>) = RaidSeasonDto(
    state = cocRaidState(state),
    startTime = parseCocTime(startTime),
    endTime = parseCocTime(endTime),
    capitalTotalLoot = capitalTotalLoot,
    raidsCompleted = raidsCompleted,
    totalAttacks = totalAttacks,
    enemyDistrictsDestroyed = enemyDistrictsDestroyed,
    offensiveReward = offensiveReward,
    defensiveReward = defensiveReward,
    members = members
        .map {
            RaidMemberDto(
                tag = it.tag.withoutHash(),
                name = it.name,
                attacks = it.attacks,
                attackLimit = it.attackLimit + it.bonusAttackLimit,
                capitalResourcesLooted = it.capitalResourcesLooted,
                // Conferido com a API real: offensiveReward ~150 por ataque → ~900 com 6 ataques.
                medals = if (it.attacks > 0) offensiveReward * it.attacks + defensiveReward else 0,
            )
        }
        .sortedByDescending { it.capitalResourcesLooted },
    notAttacked = absent,
    attackLog = attackLog.map { it.defender.toDto(it.attackCount, it.districtCount, it.districtsDestroyed) },
    defenseLog = defenseLog.map { it.attacker.toDto(it.attackCount, it.districtCount, it.districtsDestroyed) },
)

private fun CocRaidClan.toDto(attackCount: Int, districtCount: Int, districtsDestroyed: Int) = RaidClanDto(
    tag = tag.withoutHash(),
    name = name,
    level = level,
    badgeUrl = badgeUrls?.medium ?: badgeUrls?.small,
    attackCount = attackCount,
    districtCount = districtCount,
    districtsDestroyed = districtsDestroyed,
)
