package io.github.danilodox.warbook.domain.model

data class ClanDetails(
    val tag: String,
    val name: String,
    val level: Int,
    val badgeUrl: String?,
    val membersCount: Int,
    val warWins: Int,
    val warLosses: Int,
    val warTies: Int,
    val members: List<ClanMember>
)

data class ClanMember(
    val tag: String,
    val name: String,
    val townHallLevel: Int,
    val expLevel: Int,
    val trophies: Int
)