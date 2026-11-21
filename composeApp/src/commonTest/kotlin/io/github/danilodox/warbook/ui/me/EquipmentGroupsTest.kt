package io.github.danilodox.warbook.ui.me

import io.github.danilodox.warbook.ui.components.UnitItem
import io.github.danilodox.warbook.ui.units.UnitLevelTable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EquipmentGroupsTest {
    private val levels = UnitLevelTable(
        equipmentHero = mapOf(
            "Giant Gauntlet" to "Barbarian King",
            "Spiky Ball" to "Barbarian King",
            "Magic Mirror" to "Archer Queen",
            "Fire Heart" to "Dragon Duke",
        ),
    )

    private fun item(name: String) = UnitItem(name, name, 1, 10)

    @Test
    fun groupsByHeroInThePlayersHeroOrder() {
        val heroes = listOf(item("Archer Queen"), item("Barbarian King"))
        val equipment = listOf(item("Giant Gauntlet"), item("Magic Mirror"), item("Spiky Ball"))

        val groups = groupEquipment(equipment, heroes, levels) { item(it) }

        assertEquals(listOf("Archer Queen", "Barbarian King"), groups.map { it.hero?.key })
        assertEquals(listOf("Giant Gauntlet", "Spiky Ball"), groups[1].items.map { it.key })
    }

    @Test
    fun heroTheTableKnowsButThePlayerLacksComesAfterAndUnknownLast() {
        val heroes = listOf(item("Barbarian King"))
        val equipment = listOf(item("New Gear"), item("Fire Heart"), item("Spiky Ball"))

        val groups = groupEquipment(equipment, heroes, levels) { UnitItem(it, "nome $it", 0, 0) }

        assertEquals(listOf("Barbarian King", "Dragon Duke", null), groups.map { it.hero?.key })
        assertEquals("nome Dragon Duke", groups[1].hero?.name)
        assertEquals(listOf("New Gear"), groups[2].items.map { it.key })
    }

    @Test
    fun withoutTableEverythingIsUngrouped() {
        val groups = groupEquipment(listOf(item("Spiky Ball")), listOf(item("Barbarian King")), null) { item(it) }
        assertEquals(1, groups.size)
        assertNull(groups.single().hero)
    }
}
