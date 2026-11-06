package io.github.danilodox.warbook.coc

import io.github.danilodox.warbook.api.ClanDto
import io.github.danilodox.warbook.api.ClanMemberDto
import io.github.danilodox.warbook.api.ClanRole
import io.github.danilodox.warbook.api.ClanSummaryDto
import io.github.danilodox.warbook.api.PlayerDto
import io.github.danilodox.warbook.api.WarFrequency
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

// Converte o formato da Supercell para os DTOs próprios. Tags saem sem `#`.

private val cocTimeFormat = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss.SSS'Z'")

/** "20261102T120000.000Z" → milissegundos desde a época; `null` se vier vazio ou fora do formato. */
internal fun parseCocTime(value: String?): Long? = value
    ?.let { runCatching { LocalDateTime.parse(it, cocTimeFormat) }.getOrNull() }
    ?.toInstant(ZoneOffset.UTC)
    ?.toEpochMilli()

internal fun String.withoutHash(): String = removePrefix("#")

internal fun cocRole(value: String?): ClanRole? = when (value) {
    "member" -> ClanRole.MEMBER
    "admin" -> ClanRole.ELDER
    "coLeader" -> ClanRole.CO_LEADER
    "leader" -> ClanRole.LEADER
    else -> null
}

internal fun cocWarFrequency(value: String?): WarFrequency = when (value) {
    "always" -> WarFrequency.ALWAYS
    "moreThanOncePerWeek" -> WarFrequency.MORE_THAN_ONCE_PER_WEEK
    "oncePerWeek" -> WarFrequency.ONCE_PER_WEEK
    "lessThanOncePerWeek" -> WarFrequency.LESS_THAN_ONCE_PER_WEEK
    "never" -> WarFrequency.NEVER
    else -> WarFrequency.UNKNOWN
}

internal fun CocPlayer.toDto(): PlayerDto {
    val homeTroops = troops.homeVillage().filterNot { CocUnits.isSuperTroop(it.name) }
    val (pets, nonPets) = homeTroops.partition { it.name in CocUnits.PETS }
    val (siegeMachines, regularTroops) = nonPets.partition { it.name in CocUnits.SIEGE_MACHINES }
    return PlayerDto(
        tag = tag.withoutHash(),
        name = name,
        townHallLevel = townHallLevel,
        expLevel = expLevel,
        trophies = trophies,
        warStars = warStars,
        attackWins = attackWins,
        donations = donations,
        donationsReceived = donationsReceived,
        clan = clan?.let { ClanSummaryDto(it.tag.withoutHash(), it.name, it.badgeUrls?.medium) },
        role = cocRole(role),
        league = league?.name,
        heroes = heroes.homeVillage().toDtos(),
        troops = regularTroops.toDtos(),
        spells = spells.homeVillage().toDtos(),
        siegeMachines = siegeMachines.toDtos(),
        pets = pets.toDtos(),
        heroEquipment = heroEquipment.homeVillage().toDtos(),
    )
}

internal fun CocClan.toDto() = ClanDto(
    tag = tag.withoutHash(),
    name = name,
    badgeUrl = badgeUrls?.medium,
    clanLevel = clanLevel,
    memberCount = members,
    description = description.orEmpty(),
    warLeague = warLeague?.name,
    capitalLeague = capitalLeague?.name,
    warWins = warWins,
    warWinStreak = warWinStreak,
    isWarLogPublic = isWarLogPublic,
    warFrequency = cocWarFrequency(warFrequency),
    requiredTrophies = requiredTrophies,
    requiredTownHallLevel = requiredTownhallLevel,
    location = location?.name,
    members = memberList.map {
        ClanMemberDto(
            tag = it.tag.withoutHash(),
            name = it.name,
            role = cocRole(it.role) ?: ClanRole.MEMBER,
            townHallLevel = it.townHallLevel,
            trophies = it.trophies,
            donations = it.donations,
            donationsReceived = it.donationsReceived,
            clanRank = it.clanRank,
        )
    },
)
