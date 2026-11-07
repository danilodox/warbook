package io.github.danilodox.warbook.war

import io.github.danilodox.warbook.api.CurrentWarDto
import io.github.danilodox.warbook.api.WarAttackDto
import io.github.danilodox.warbook.api.WarClanDto
import io.github.danilodox.warbook.api.WarMemberDto
import io.github.danilodox.warbook.api.WarState

const val NOW = 1_800_000_000_000L
const val MINUTE = 60_000L
const val HOUR = 60 * MINUTE

fun member(name: String, townHall: Int, position: Int, attacks: Int = 0) = WarMemberDto(
    tag = name.uppercase(),
    name = name,
    townHallLevel = townHall,
    mapPosition = position,
    attacks = List(attacks) { WarAttackDto("ALVO", stars = 3, destructionPercentage = 100, order = it + 1) },
)

/** Picuí Raiz (Maré Alta CV 16, Dantas CV 15, Rapadura CV 13) × Os Mito (CV 16, 16, 14). */
@Suppress("LongParameterList") // Cada teste muda só o que importa para ele.
fun testWar(
    state: WarState = WarState.IN_WAR,
    attacksPerMember: Int = 2,
    ourAttacks: List<Int> = listOf(2, 1, 0),
    ourStars: Int = 31,
    ourDestruction: Double = 78.4,
    theirStars: Int = 28,
    theirDestruction: Double = 71.0,
    endsIn: Long = 5 * HOUR + 12 * MINUTE,
    startsIn: Long = 14 * HOUR + 3 * MINUTE,
    isCwl: Boolean = false,
) = CurrentWarDto(
    state = state,
    teamSize = 3,
    attacksPerMember = attacksPerMember,
    startTime = NOW + startsIn,
    endTime = NOW + endsIn,
    isCwl = isCwl,
    clan = WarClanDto(
        tag = "2YQUC8J0P",
        name = "Picuí Raiz",
        clanLevel = 18,
        stars = ourStars,
        destructionPercentage = ourDestruction,
        attacks = ourAttacks.sum(),
        members = listOf(
            member("Maré Alta", 16, 1, ourAttacks[0]),
            member("Dantas", 15, 2, ourAttacks[1]),
            member("Rapadura", 13, 3, ourAttacks[2]),
        ),
    ),
    opponent = WarClanDto(
        tag = "OPP",
        name = "Os Mito",
        clanLevel = 20,
        stars = theirStars,
        destructionPercentage = theirDestruction,
        attacks = 0,
        members = listOf(member("Mito01", 16, 1), member("Brabo", 16, 2), member("Gordin", 14, 3)),
    ),
)
