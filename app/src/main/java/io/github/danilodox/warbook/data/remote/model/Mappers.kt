package io.github.danilodox.warbook.data.remote.model

import io.github.danilodox.warbook.domain.model.Clan
import io.github.danilodox.warbook.domain.model.ClanDetails
import io.github.danilodox.warbook.domain.model.ClanMember
import io.github.danilodox.warbook.domain.model.Hero
import io.github.danilodox.warbook.domain.model.Player
import io.github.danilodox.warbook.domain.model.Spell
import io.github.danilodox.warbook.domain.model.Troop

fun PlayerDto.toDomain(): Player {
    return Player(
        tag = this.tag ?: "",
        name = this.name ?: "Unknown Player",
        expLevel = this.expLevel ?: 0,
        trophies = this.trophies ?: 0,
        warStars = this.warStars ?: 0,
        donations = this.donations ?: 0,
        townHallLevel = this.townHallLevel ?: 0,
        clan = this.clan?.toDomain(),
        troops = this.troops?.map { it.toDomain() } ?: emptyList(),
        heroes = this.heroes?.map { it.toDomain() } ?: emptyList(),
        spells = this.spells?.map { it.toDomain() } ?: emptyList()
    )
}

/**
 * Converte o DTO 'ClanDto' da API para o modelo de Domínio 'Clan'.
 */
fun ClanDto.toDomain(): Clan {
    return Clan(
        tag = this.tag ?: "",
        name = this.name ?: "Unknown Clan",
        clanLevel = this.clanLevel ?: 0
    )
}

fun ClanDetailsDto.toDomain(): ClanDetails {
    return ClanDetails(
        tag = this.tag,
        name = this.name,
        level = this.clanLevel,
        badgeUrl = this.badgeUrls?.medium,
        membersCount = this.members,
        warWins = this.warWins,
        warLosses = this.warLosses ?: 0,
        warTies = this.warTies ?: 0,
        members = this.memberList?.map { it.toDomain() } ?: emptyList()
    )
}

/**
 * Converte o DTO 'ClanMemberDto' da API para o modelo de Domínio 'ClanMember'.
 */
fun ClanMemberDto.toDomain(): ClanMember {
    return ClanMember(
        tag = this.tag,
        name = this.name,
        townHallLevel = this.townHallLevel,
        expLevel = this.expLevel,
        trophies = this.trophies,
    )
}

fun TroopDto.toDomain(): Troop { // Use o nome do seu DTO de tropa
    return Troop(
        name = this.name ?: "",
        level = this.level ?: 0,
        maxLevel = this.maxLevel ?: 0,
        village = this.village ?: "",
        iconUrl = null // A API do CoC não fornece URLs de ícones para tropas, infelizmente.
    )
}
fun HeroDto.toDomain(): Hero { // Use o nome do seu DTO de herói
    return Hero(
        name = this.name ?: "",
        level = this.level ?: 0,
        maxLevel = this.maxLevel ?: 0,
        village = this.village ?: "",
        iconUrl = null // A API do CoC também não fornece URLs de ícones para heróis.
    )
}

fun SpellDto.toDomain(): Spell { // Use o nome do seu DTO de feitiço
    return Spell(
        name = this.name ?: "",
        level = this.level ?: 0,
        maxLevel = this.maxLevel ?: 0,
        village = this.village ?: "",
        iconUrl = null
    )
}
