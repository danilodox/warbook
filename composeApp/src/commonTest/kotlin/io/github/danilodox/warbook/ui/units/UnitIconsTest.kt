package io.github.danilodox.warbook.ui.units

import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class UnitIconsTest {

    @Test
    fun unidadesDoKitTemIcone() {
        listOf(
            "Barbarian", "P.E.K.K.A", "Archer Queen", "Bag of Frostmites", "L.A.S.S.I", "Battle Drill",
            "Minion Prince", "Dragon Duke", "Thrower", "Druid", "Stone Slammer", "Troop Launcher", "Angry Jelly",
            "Ruin Witch", "Meteor Golem", "Sky Wagon", "Barch Squad", "Greedy Raven", "Angry Spell",
            "Giant Gauntlet", "Spiky Ball", "Snake Bracelet", "Fire Heart", "Revenge Deck",
        ).forEach { assertNotNull(UnitIcons.forName(it), it) }
    }

    @Test
    fun foraDoKitFicaSemIcone() {
        assertNull(UnitIcons.forName("Noble Iron"))
        assertNull(UnitIcons.forName("Party Wizard"))
        assertNull(UnitIcons.forName("barbarian"))
    }

    @Test
    fun todaUnidadeComIconeTemNomeEmPortugues() {
        listOf("Barbarian", "Healing Spell", "Royal Champion", "Siege Barracks", "Spirit Fox").forEach {
            assertNotNull(UnitIcons.forName(it), it)
            assertNotEquals(it, UnitNames.display(it, "pt"))
        }
    }
}
