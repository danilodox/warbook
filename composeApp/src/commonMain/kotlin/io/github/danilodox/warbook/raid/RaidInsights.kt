package io.github.danilodox.warbook.raid

import io.github.danilodox.warbook.api.RaidMemberDto
import io.github.danilodox.warbook.api.RaidSeasonDto

/** Quem atacou mas não usou todos os ataques (ex.: 3 de 5), quem atacou menos primeiro. */
fun RaidSeasonDto.attackedLess(): List<RaidMemberDto> =
    members.filter { it.attacks < it.attackLimit }.sortedBy { it.attacks }

/** Ataques que sobraram no clã: os que não usou quem atacou + 5 de cada um que não atacou. */
fun RaidSeasonDto.unusedAttacks(): Int =
    attackedLess().sumOf { it.attackLimit - it.attacks } + notAttacked.size * BASE_ATTACKS

/** Todo membro tem 5 ataques; o 6º é bônus. */
const val BASE_ATTACKS = 5
