package io.github.danilodox.warbook.data

import io.github.danilodox.warbook.war.SummaryOptions
import io.github.danilodox.warbook.war.SummaryPart
import kotlin.test.Test
import kotlin.test.assertEquals

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
}
