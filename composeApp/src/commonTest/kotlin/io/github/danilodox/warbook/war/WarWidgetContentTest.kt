package io.github.danilodox.warbook.war

import io.github.danilodox.warbook.api.CurrentWarDto
import io.github.danilodox.warbook.api.WarState
import kotlin.test.Test
import kotlin.test.assertEquals

class WarWidgetContentTest {

    private val pt = summaryTextsFor("pt")
    private val texts = PortugueseWarWidgetTexts

    private fun widget(war: CurrentWarDto?) = WarWidgetContent.of(WidgetWar.Clan(war), NOW, texts, pt)

    @Test
    fun emGuerraMostraPlacarTempoEQuemFalta() {
        val data = widget(testWar())

        assertEquals("Picuí Raiz × Os Mito", data.title)
        assertEquals("⭐ 31 × 28 · 78,4% × 71,0%", data.score)
        assertEquals(pt.endsIn(pt.duration(5 * HOUR + 12 * MINUTE)), data.status)
        assertEquals("Faltam 3 ataques (2 membros)", data.pending)
    }

    @Test
    fun todoMundoAtacou() {
        assertEquals("Todo mundo atacou", widget(testWar(ourAttacks = listOf(2, 2, 2))).pending)
    }

    @Test
    fun preparacaoSemPlacar() {
        val data = widget(testWar(state = WarState.PREPARATION, ourAttacks = listOf(0, 0, 0)))

        assertEquals(pt.preparation(pt.duration(14 * HOUR + 3 * MINUTE)), data.status)
        assertEquals(null, data.score)
        assertEquals(null, data.pending)
    }

    @Test
    fun previaTemOFormatoDeUmaGuerraEmAndamento() {
        val data = WarWidgetContent.preview(texts, pt)

        assertEquals("Seu clã × Adversário", data.title)
        assertEquals("⭐ 24 × 21 · 82,5% × 74,0%", data.score)
        assertEquals(pt.endsIn(pt.duration(5 * HOUR + 20 * MINUTE)), data.status)
        assertEquals("Faltam 7 ataques (4 membros)", data.pending)
    }

    @Test
    fun previaEmIngles() {
        val data = WarWidgetContent.preview(EnglishWarWidgetTexts, summaryTextsFor("en"))

        assertEquals("Your clan × Opponent", data.title)
        assertEquals("⭐ 24 × 21 · 82.5% × 74.0%", data.score)
        assertEquals("7 attacks left (4 members)", data.pending)
    }

    @Test
    fun encerradaMostraOResultado() {
        val data = widget(testWar(state = WarState.WAR_ENDED))

        assertEquals("Vitória!", data.status)
        assertEquals(null, data.pending)
    }

    @Test
    fun semGuerraSemContaESemCla() {
        assertEquals(WarWidgetData("Warbook", "Sem guerra agora"), widget(null))
        assertEquals(WarWidgetData("Warbook", "Sem guerra agora"), widget(CurrentWarDto(WarState.NOT_IN_WAR)))
        assertEquals("Procurando adversário", widget(CurrentWarDto(WarState.MATCHMAKING)).status)
        assertEquals(
            "Abra o Warbook e escolha uma conta",
            WarWidgetContent.of(WidgetWar.NoAccount, NOW, texts, pt).status,
        )
        assertEquals(
            "No war right now",
            WarWidgetContent.of(WidgetWar.Clan(null), NOW, EnglishWarWidgetTexts, summaryTextsFor("en")).status,
        )
        assertEquals("A conta ativa não está em um clã", WarWidgetContent.of(WidgetWar.NoClan, NOW, texts, pt).status)
    }
}
