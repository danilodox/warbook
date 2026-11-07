package io.github.danilodox.warbook.ui.clan

import io.github.danilodox.warbook.api.ClanMemberDto

/** Ordem da lista de membros (C2). Cada uma tem direção fixa; empate cai na posição do clã. */
enum class MemberSort { POSITION, TOWN_HALL, TROPHIES, DONATIONS }

fun List<ClanMemberDto>.sortedBy(sort: MemberSort): List<ClanMemberDto> = when (sort) {
    MemberSort.POSITION -> sortedBy { it.clanRank }
    MemberSort.TOWN_HALL -> sortedWith(compareByDescending<ClanMemberDto> { it.townHallLevel }.thenBy { it.clanRank })
    MemberSort.TROPHIES -> sortedWith(compareByDescending<ClanMemberDto> { it.trophies }.thenBy { it.clanRank })
    MemberSort.DONATIONS -> sortedWith(
        compareByDescending<ClanMemberDto> { it.donations }.thenBy { it.donationsReceived }.thenBy { it.clanRank },
    )
}
