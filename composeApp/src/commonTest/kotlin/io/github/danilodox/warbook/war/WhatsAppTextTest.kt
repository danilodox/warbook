package io.github.danilodox.warbook.war

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WhatsAppTextTest {

    @Test
    fun trocaAsteriscoESublinhado() {
        assertEquals("∗Rei＿do＿Norte∗", WhatsAppText.safeName("*Rei_do_Norte*"))
    }

    @Test
    fun nomeSemCaractereEspecialNaoMuda() {
        assertEquals("Maré Alta ✦", WhatsAppText.safeName("Maré Alta ✦"))
    }

    @Test
    fun resumoEscapaNomesDeClasEJogadoresMasNaoONegrito() {
        val base = testWar()
        val clan = requireNotNull(base.clan)
        val renamed = mapOf("Maré Alta" to "*Maré_Alta*", "Rapadura" to "Ra*pa")
        val members = clan.members.map { it.copy(name = renamed[it.name] ?: it.name) }
        val war = base.copy(
            clan = clan.copy(name = "Picuí_Raiz", members = members),
            opponent = base.opponent?.copy(name = "*Os Mito*"),
        )
        val options = SummaryOptions(SummaryOptions().parts + SummaryPart.BEST_ATTACKS)

        val text = requireNotNull(WarSummary.war(war, NOW, PortugueseSummaryTexts, options))

        assertTrue(text.startsWith("*Picuí＿Raiz × ∗Os Mito∗* (3 × 3)"), text)
        assertTrue("• Ra∗pa (2)" in text, "pendentes")
        assertTrue("• ∗Maré＿Alta∗" in text, "melhores ataques")
        assertFalse('_' in text, "nenhum sublinhado sobra")
    }
}
