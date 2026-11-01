package io.github.danilodox.warbook.data.remote.model

data class ClanDetailsDto(
    val tag: String,
    val name: String,
    val description: String,
    val clanLevel: Int,
    val clanPoints: Int,
    val clanBuilderBasePoints: Int,
    val clanCapitalPoints: Int,
    val warWinStreak: Int,
    val warWins: Int,
    val warTies: Int?,
    val warLosses: Int?,
    val warFrequency: String,
    val isWarLogPublic: Boolean,
    val isFamilyFriendly: Boolean,
    val requiredTrophies: Int,
    val requiredBuilderBaseTrophies: Int?,
    val requiredTownhallLevel: Int,
    val type: String,
    val members: Int,
    val badgeUrls: BadgeUrls?,
    val location: Location?,
    val chatLanguage: ChatLanguage?,
    val warLeague: League?,
    val capitalLeague: League?,
    val labels: List<Label>?,
    val memberList: List<ClanMemberDto>?,
    val clanCapital: ClanCapital?
)

data class BadgeUrls(
    val small: String?,
    val medium: String?,
    val large: String?
)

data class Location(
    val localizedName: String?,
    val id: Int,
    val name: String?,
    val isCountry: Boolean?,
    val countryCode: String?
)

data class ChatLanguage(
    val name: String?,
    val id: Int,
    val languageCode: String?
)

data class League(
    val id: Int,
    val name: String?
)

data class Label(
    val id: Int,
    val name: String?,
    val iconUrls: BadgeUrls?
)

data class ClanMemberDto(
    val tag: String,
    val name: String,
    val role: String,
    val townHallLevel: Int,
    val expLevel: Int,
    val clanRank: Int,
    val previousClanRank: Int,
    val donations: Int,
    val donationsReceived: Int,
    val trophies: Int,
    val builderBaseTrophies: Int,
    val league: League?,
    val builderBaseLeague: League?,
    val playerHouse: PlayerHouse?
)

data class PlayerHouse(
    val elements: List<HouseElement>?
)

data class HouseElement(
    val id: Int,
    val type: String
)

data class ClanCapital(
    val capitalHallLevel: Int,
    val districts: List<District>?
)

data class District(
    val id: Int,
    val name: String?,
    val districtHallLevel: Int
)