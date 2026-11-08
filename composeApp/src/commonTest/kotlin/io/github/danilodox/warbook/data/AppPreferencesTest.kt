package io.github.danilodox.warbook.data

import io.github.danilodox.warbook.war.SummaryOptions
import io.github.danilodox.warbook.war.SummaryPart
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppPreferencesTest {

    @Test
    fun nadaSalvoEOPadrao() {
        assertEquals(SummaryOptions(), summaryOptionsOf(null))
    }

    @Test
    fun conjuntoVazioSalvoContinuaVazio() {
        assertEquals(SummaryOptions(emptySet()), summaryOptionsOf(emptySet()))
    }

    @Test
    fun nomeQueNaoExisteMaisEIgnorado() {
        assertEquals(
            SummaryOptions(setOf(SummaryPart.SCORE, SummaryPart.WEAK_ATTACKS)),
            summaryOptionsOf(setOf("SCORE", "WEAK_ATTACKS", "ALGO_ANTIGO")),
        )
    }

    @Test
    fun temaSemNadaSalvoEEscuro() {
        assertEquals(ThemeMode.DARK, themeModeOf(null))
        assertEquals(ThemeMode.DARK, themeModeOf("SEPIA"))
        assertEquals(ThemeMode.LIGHT, themeModeOf("LIGHT"))
        assertEquals(ThemeMode.SYSTEM, themeModeOf("SYSTEM"))
    }

    @Test
    fun temaDoSistemaSegueOAparelho() {
        assertTrue(ThemeMode.DARK.isDark(systemDark = false))
        assertFalse(ThemeMode.LIGHT.isDark(systemDark = true))
        assertTrue(ThemeMode.SYSTEM.isDark(systemDark = true))
        assertFalse(ThemeMode.SYSTEM.isDark(systemDark = false))
    }
}
