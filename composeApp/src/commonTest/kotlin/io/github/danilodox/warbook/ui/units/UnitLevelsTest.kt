package io.github.danilodox.warbook.ui.units

import io.github.danilodox.warbook.api.UnitLevelDto
import io.github.danilodox.warbook.ui.me.toItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UnitLevelsTest {
    private val table = UnitLevelTable.parse(
        """
        {"source":"teste","generated":"2026-11-21",
         "units":{"Witch":[9,9,10,11,12,13,15,17,19],"Giant Gauntlet":[8,8,10,16]},
         "equipmentHero":{"Giant Gauntlet":"Barbarian King"}}
        """.trimIndent(),
    )

    @Test
    fun maxForTownHallCountsLevelsTheTownHallAllows() {
        assertEquals(0, table.maxForTownHall("Witch", 8))
        assertEquals(2, table.maxForTownHall("Witch", 9))
        assertEquals(8, table.maxForTownHall("Witch", 18))
        assertEquals(9, table.maxForTownHall("Witch", 19))
        assertNull(table.maxForTownHall("Unknown Troop", 19))
    }

    @Test
    fun equipmentKnowsItsHero() {
        assertEquals("Barbarian King", table.heroOf("Giant Gauntlet"))
        assertNull(table.heroOf("Witch"))
    }

    @Test
    fun itemIsFullForTheTownHallBelowTheGameMax() {
        val witch = UnitLevelDto("Witch", 8, 9).toItem(levels = table, townHall = 17)
        assertEquals(8, witch.townHallMax)
        assertTrue(witch.isMax)
        assertFalse(witch.isGameMax)
        assertEquals(1f, witch.progress)
    }

    @Test
    fun itemAtTopTownHallUsesTheGameMax() {
        val witch = UnitLevelDto("Witch", 8, 9).toItem(levels = table, townHall = 19)
        assertEquals(9, witch.townHallMax)
        assertFalse(witch.isMax)
    }

    @Test
    fun withoutTableOrUnitTheGameMaxIsUsed() {
        assertEquals(10, UnitLevelDto("Barbarian", 5, 10).toItem().townHallMax)
        assertEquals(10, UnitLevelDto("Unknown Troop", 5, 10).toItem(levels = table, townHall = 3).townHallMax)
    }

    @Test
    fun staleTableNeverShowsLevelAboveTheMax() {
        // A tabela diz 2 no CV9, mas o jogador já tem 3 (jogo mudou e a tabela não foi regerada).
        val witch = UnitLevelDto("Witch", 3, 9).toItem(levels = table, townHall = 9)
        assertEquals(3, witch.townHallMax)
        assertTrue(witch.isMax)
    }

    @Test
    fun tableNeverGoesAboveTheGameMax() {
        val gauntlet = UnitLevelDto("Giant Gauntlet", 2, 3).toItem(levels = table, townHall = 19)
        assertEquals(3, gauntlet.townHallMax)
    }
}
