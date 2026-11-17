package io.github.danilodox.warbook.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlayerSearchTest {

    @Test
    fun aparaOsEspacos() {
        assertEquals("Danilo", PlayerSearch.normalize("  Danilo "))
    }

    @Test
    fun recusaMenosDeTresLetrasDepoisDeAparar() {
        assertNull(PlayerSearch.normalize(" ab "))
        assertEquals("abc", PlayerSearch.normalize("abc"))
    }

    @Test
    fun recusaNickLongoDemais() {
        assertNull(PlayerSearch.normalize("a".repeat(PlayerSearch.MAX_LENGTH + 1)))
        assertEquals(50, PlayerSearch.normalize("a".repeat(PlayerSearch.MAX_LENGTH))?.length)
    }
}
