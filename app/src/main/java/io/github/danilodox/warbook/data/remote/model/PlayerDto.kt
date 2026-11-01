package io.github.danilodox.warbook.data.remote.model

data class PlayerDto(
    val league: LeagueDto?,
    val builderBaseLeague: BuilderBaseLeagueDto?,
    val clan: ClanDto?,
    val role: String?,
    val warPreference: String?,
    val attackWins: Int?,
    val defenseWins: Int?,
    val townHallLevel: Int?,
    val townHallWeaponLevel: Int?,
    val legendStatistics: LegendStatisticsDto?,
    val troops: List<TroopDto>?,
    val heroes: List<HeroDto>?,
    val heroEquipment: List<HeroEquipmentDto>?,
    val spells: List<SpellDto>?,
    val labels: List<LabelDto>?,
    val tag: String?,
    val name: String?,
    val expLevel: Int?,
    val trophies: Int?,
    val bestTrophies: Int?,
    val donations: Int?,
    val donationsReceived: Int?,
    val builderHallLevel: Int?,
    val builderBaseTrophies: Int?,
    val bestBuilderBaseTrophies: Int?,
    val warStars: Int?,
    val achievements: List<AchievementDto>?,
    val clanCapitalContributions: Int?,
    val playerHouse: PlayerHouseDto?
)

data class LeagueDto(
    val name: String?,
    val id: Int?,
    val iconUrls: Map<String, String>?
)

data class BuilderBaseLeagueDto(
    val name: String?,
    val id: Int?
)

data class ClanDto(
    val tag: String?,
    val clanLevel: Int?,
    val name: String?,
    val badgeUrls: Map<String, String>?
)

data class LegendStatisticsDto(
    val legendTrophies: Int?,
    val currentSeason: SeasonDto?,
    val bestSeason: SeasonDto?,
    val bestBuilderBaseSeason: SeasonDto?,
    val previousSeason: SeasonDto?,
    val previousBuilderBaseSeason: SeasonDto?
)

data class SeasonDto(
    val trophies: Int?,
    val id: String?,
    val rank: Int?
)

data class TroopDto(
    val level: Int?,
    val name: String?,
    val maxLevel: Int?,
    val village: String?,
    val superTroopIsActive: Boolean?,
    val equipment: List<String>?
)

data class HeroDto(
    val level: Int?,
    val name: String?,
    val maxLevel: Int?,
    val village: String?,
    val superTroopIsActive: Boolean?,
    val equipment: List<HeroEquipmentDto>?
)

data class HeroEquipmentDto(
    val level: Int?,
    val name: String?,
    val maxLevel: Int?,
    val village: String?,
    val superTroopIsActive: Boolean?,
    val equipment: List<String>?
)

data class SpellDto(
    val level: Int?,
    val name: String?,
    val maxLevel: Int?,
    val village: String?,
    val superTroopIsActive: Boolean?,
    val equipment: List<String>?
)

data class LabelDto(
    val name: String?,
    val id: Int?,
    val iconUrls: Map<String, String>?
)

data class AchievementDto(
    val stars: Int?,
    val value: Int?,
    val name: String?,
    val target: Int?,
    val info: String?,
    val completionInfo: String?,
    val village: String?
)

data class PlayerHouseDto(
    val elements: List<PlayerHouseElementDto>?
)

data class PlayerHouseElementDto(
    val id: Int?,
    val type: String?
)