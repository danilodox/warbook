package io.github.danilodox.warbook.domain.model

data class Player(
    val tag: String,
    val name: String,
    val expLevel: Int,
    val trophies: Int,
    val warStars: Int,
    val donations: Int,
    val townHallLevel: Int,
    val clan: Clan?,
    val troops: List<Troop>,
    val heroes: List<Hero>,
    val spells: List<Spell>
)

data class Clan(
    val tag: String,
    val name: String,
    val clanLevel: Int
)

data class Troop(
    val name: String,
    val level: Int,
    val maxLevel: Int,
    val village: String,
    val iconUrl: String? // Supondo que a API forneça uma URL para o ícone da tropa
)

data class Hero(
    val name: String,
    val level: Int,
    val maxLevel: Int,
    val village: String,
    val iconUrl: String?
)

data class Spell(
    val name: String,
    val level: Int,
    val maxLevel: Int,
    val village: String,
    val iconUrl: String?
)