package io.github.danilodox.warbook.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ClanSearchTest {

    @Test
    fun aparaOsEspacos() {
        assertEquals("Picuí Raiz", ClanSearch.normalize("  Picuí Raiz "))
    }

    @Test
    fun recusaMenosDeTresLetrasDepoisDeAparar() {
        assertNull(ClanSearch.normalize(" ab "))
        assertEquals("abc", ClanSearch.normalize("abc"))
    }

    @Test
    fun recusaNomeLongoDemais() {
        assertNull(ClanSearch.normalize("a".repeat(ClanSearch.MAX_LENGTH + 1)))
    }
}
