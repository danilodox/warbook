package io.github.danilodox.warbook.coc

import io.github.danilodox.warbook.api.UnitLevelDto

/** Só a vila principal: a Base do Construtor fica fora do Warbook. */
internal fun List<CocUnit>.homeVillage(): List<CocUnit> = filter { it.village == CocUnit.VILLAGE_HOME }

internal fun List<CocUnit>.toDtos(): List<UnitLevelDto> = map { UnitLevelDto(it.name, it.level, it.maxLevel) }

/**
 * A API manda pets, máquinas de cerco e supertropas misturados em `troops`, sem campo que os diferencie.
 * A separação é pelo nome (em inglês, como a API devolve). Unidade nova do jogo que não estiver aqui
 * aparece como tropa comum até a lista ser atualizada.
 */
internal object CocUnits {
    val PETS = setOf(
        "L.A.S.S.I",
        "Electro Owl",
        "Mighty Yak",
        "Unicorn",
        "Frosty",
        "Diggy",
        "Poison Lizard",
        "Phoenix",
        "Spirit Fox",
        "Angry Jelly",
        "Sneezy",
    )

    val SIEGE_MACHINES = setOf(
        "Wall Wrecker",
        "Battle Blimp",
        "Stone Slammer",
        "Siege Barracks",
        "Log Launcher",
        "Flame Flinger",
        "Battle Drill",
        "Troop Launcher",
    )

    /** Supertropas cujo nome não começa com "Super ". O nível delas espelha o da tropa base, então ficam fora. */
    private val SUPER_TROOPS_WITHOUT_PREFIX = setOf("Sneaky Goblin", "Rocket Balloon", "Inferno Dragon", "Ice Hound")

    fun isSuperTroop(name: String): Boolean = name.startsWith("Super ") || name in SUPER_TROOPS_WITHOUT_PREFIX
}
