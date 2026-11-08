package io.github.danilodox.warbook.ui.units

import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class UnitIconsTest {

    @Test
    fun unidadesDoKitTemIcone() {
        listOf("Barbarian", "P.E.K.K.A", "Archer Queen", "Bag of Frostmites", "L.A.S.S.I", "Battle Drill")
            .forEach { assertNotNull(UnitIcons.forName(it), it) }
    }

    @Test
    fun foraDoKitFicaSemIcone() {
        assertNull(UnitIcons.forName("Giant Gauntlet"))
        assertNull(UnitIcons.forName("Thrower"))
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
