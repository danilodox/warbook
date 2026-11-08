package io.github.danilodox.warbook.ui.units

import kotlin.test.Test
import kotlin.test.assertEquals

class UnitNamesTest {

    @Test
    fun emPortuguesUsaATabela() {
        assertEquals("Rei Bárbaro", UnitNames.display("Barbarian King", "pt"))
        assertEquals("Quebra-Muros", UnitNames.display("Wall Breaker", "pt"))
        assertEquals("Feitiço de Relâmpago", UnitNames.display("Lightning Spell", "pt"))
        assertEquals("Iaque Poderoso", UnitNames.display("Mighty Yak", "pt"))
    }

    @Test
    fun foraDaTabelaFicaONomeDaApi() {
        assertEquals("Giant Gauntlet", UnitNames.display("Giant Gauntlet", "pt"))
        assertEquals("Unidade Nova", UnitNames.display("Unidade Nova", "pt"))
    }

    @Test
    fun emOutroIdiomaFicaONomeDaApi() {
        assertEquals("Barbarian King", UnitNames.display("Barbarian King", "en"))
        assertEquals("Wall Breaker", UnitNames.display("Wall Breaker", "es"))
    }
}
