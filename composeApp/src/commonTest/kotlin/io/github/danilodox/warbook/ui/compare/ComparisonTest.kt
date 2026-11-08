package io.github.danilodox.warbook.ui.compare

import io.github.danilodox.warbook.api.PlayerDto
import io.github.danilodox.warbook.api.UnitLevelDto
import kotlin.test.Test
import kotlin.test.assertEquals

class ComparisonTest {

    private fun player(
        tag: String,
        townHall: Int = 16,
        level: Int = 200,
        trophies: Int = 5000,
        heroes: List<UnitLevelDto> = emptyList(),
    ) = PlayerDto(
        tag = tag,
        name = tag,
        townHallLevel = townHall,
        expLevel = level,
        trophies = trophies,
        warStars = 1000,
        attackWins = 10,
        donations = 500,
        donationsReceived = 0,
        heroes = heroes,
    )

    @Test
    fun contaQuemEstaAFrenteEOsEmpates() {
        val result = compare(player("EU", level = 247, trophies = 5312), player("ELE", level = 261, trophies = 5000))
        val general = result.groups.first { it.section == CompareSection.GENERAL }.rows
        assertEquals(listOf(0, -1, 1, 0), general.map { it.result })
        assertEquals(6, result.total)
        assertEquals(1, result.mineAhead)
        assertEquals(1, result.theirsAhead)
        assertEquals(4, result.ties)
    }

    @Test
    fun heroiQueSoUmTemEntraEContaComoZero() {
        val mine = player("EU", heroes = listOf(UnitLevelDto("Barbarian King", 92, 100)))
        val theirs = player(
            "ELE",
            heroes = listOf(UnitLevelDto("Barbarian King", 90, 100), UnitLevelDto("Minion Prince", 5, 90)),
        )
        val heroes = compare(mine, theirs).groups.first { it.section == CompareSection.HEROES }.rows
        assertEquals(
            listOf(CompareLabel.Hero("Barbarian King"), CompareLabel.Hero("Minion Prince")),
            heroes.map { it.label },
        )
        assertEquals(1, heroes[0].result)
        assertEquals(null, heroes[1].mine)
        assertEquals(-1, heroes[1].result)
    }

    @Test
    fun semHeroisASecaoSome() {
        val sections = compare(player("EU"), player("ELE")).groups.map { it.section }
        assertEquals(listOf(CompareSection.GENERAL, CompareSection.SEASON), sections)
    }
}
