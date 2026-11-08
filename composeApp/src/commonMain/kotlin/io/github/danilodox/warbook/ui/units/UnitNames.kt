package io.github.danilodox.warbook.ui.units

/**
 * Nome em português de cada unidade, a partir do nome que a API do jogo entrega (sempre em inglês).
 * Fora da tabela (unidade nova, equipamento de herói) fica o nome da API.
 */
object UnitNames {
    fun display(apiName: String, language: String): String =
        if (language == "pt") portuguese[apiName] ?: apiName else apiName

    private val troops = mapOf(
        "Barbarian" to "Bárbaro",
        "Archer" to "Arqueira",
        "Giant" to "Gigante",
        "Goblin" to "Goblin",
        "Wall Breaker" to "Quebra-Muros",
        "Balloon" to "Balão",
        "Wizard" to "Mago",
        "Healer" to "Curandeira",
        "Dragon" to "Dragão",
        "P.E.K.K.A" to "P.E.K.K.A",
        "Baby Dragon" to "Dragão Bebê",
        "Miner" to "Mineiro",
        "Electro Dragon" to "Dragão Elétrico",
        "Yeti" to "Yeti",
        "Dragon Rider" to "Cavaleiro do Dragão",
        "Electro Titan" to "Titã Elétrica",
        "Root Rider" to "Cavaleira das Raízes",
        "Thrower" to "Arremessador",
        "Minion" to "Servo",
        "Hog Rider" to "Corredor",
        "Valkyrie" to "Valquíria",
        "Golem" to "Golem",
        "Witch" to "Bruxa",
        "Lava Hound" to "Cão de Lava",
        "Bowler" to "Lançador",
        "Ice Golem" to "Golem de Gelo",
        "Headhunter" to "Caçadora de Heróis",
        "Apprentice Warden" to "Guardião Aprendiz",
        "Druid" to "Druida",
        "Furnace" to "Fornalha",
    )

    private val superTroops = mapOf(
        "Super Barbarian" to "Super Bárbaro",
        "Super Archer" to "Super Arqueira",
        "Super Giant" to "Super Gigante",
        "Sneaky Goblin" to "Goblin Furtivo",
        "Super Wall Breaker" to "Super Quebra-Muros",
        "Rocket Balloon" to "Balão Foguete",
        "Super Wizard" to "Super Mago",
        "Super Dragon" to "Super Dragão",
        "Inferno Dragon" to "Dragão Infernal",
        "Super Minion" to "Super Servo",
        "Super Valkyrie" to "Super Valquíria",
        "Super Witch" to "Super Bruxa",
        "Ice Hound" to "Cão de Gelo",
        "Super Bowler" to "Super Lançador",
        "Super Miner" to "Super Mineiro",
        "Super Hog Rider" to "Super Corredor",
    )

    private val spells = mapOf(
        "Lightning Spell" to "Feitiço de Relâmpago",
        "Healing Spell" to "Feitiço de Cura",
        "Rage Spell" to "Feitiço de Fúria",
        "Jump Spell" to "Feitiço de Salto",
        "Freeze Spell" to "Feitiço de Congelamento",
        "Clone Spell" to "Feitiço de Clonagem",
        "Invisibility Spell" to "Feitiço de Invisibilidade",
        "Recall Spell" to "Feitiço de Retorno",
        "Revive Spell" to "Feitiço de Reviver",
        "Totem Spell" to "Feitiço de Totem",
        "Poison Spell" to "Feitiço de Veneno",
        "Earthquake Spell" to "Feitiço de Terremoto",
        "Haste Spell" to "Feitiço de Pressa",
        "Skeleton Spell" to "Feitiço de Esqueletos",
        "Bat Spell" to "Feitiço de Morcegos",
        "Overgrowth Spell" to "Feitiço de Supercrescimento",
        "Ice Block Spell" to "Feitiço de Bloco de Gelo",
        "Bag of Frostmites" to "Saco de Ácaros Gélidos",
    )

    private val heroes = mapOf(
        "Barbarian King" to "Rei Bárbaro",
        "Archer Queen" to "Rainha Arqueira",
        "Grand Warden" to "Grande Guardião",
        "Royal Champion" to "Campeã Real",
        "Minion Prince" to "Príncipe Servo",
        "Dragon Duke" to "Duque Dragão",
    )

    private val sieges = mapOf(
        "Wall Wrecker" to "Destruidor de Muros",
        "Battle Blimp" to "Dirigível de Batalha",
        "Stone Slammer" to "Esmagador de Pedras",
        "Siege Barracks" to "Quartel de Cerco",
        "Log Launcher" to "Lançador de Troncos",
        "Flame Flinger" to "Lança-Chamas",
        "Battle Drill" to "Broca de Batalha",
        "Troop Launcher" to "Lançador de Tropas",
    )

    private val pets = mapOf(
        "L.A.S.S.I" to "L.A.S.S.I",
        "Electro Owl" to "Coruja Elétrica",
        "Mighty Yak" to "Iaque Poderoso",
        "Unicorn" to "Unicórnio",
        "Frosty" to "Frosty",
        "Diggy" to "Diggy",
        "Poison Lizard" to "Lagarto Venenoso",
        "Phoenix" to "Fênix",
        "Spirit Fox" to "Raposa Espiritual",
        "Angry Jelly" to "Água-viva Brava",
        "Sneezy" to "Sneezy",
    )

    private val portuguese: Map<String, String> = troops + superTroops + spells + heroes + sieges + pets
}
