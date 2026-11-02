package io.github.danilodox.warbook.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TagNormalizerTest {

    @Test
    fun tiraCerquilhaEPassaParaMaiusculas() {
        assertEquals("2PQ8LV9YC", TagNormalizer.normalize("#2pq8lv9yc"))
    }

    @Test
    fun aceitaTagSemCerquilha() {
        assertEquals("2PQ8LV9YC", TagNormalizer.normalize("2PQ8LV9YC"))
    }

    @Test
    fun trocaLetraOPorZero() {
        assertEquals("2YQUC8J0P", TagNormalizer.normalize("#2yquc8jop"))
    }

    @Test
    fun ignoraEspacosNasPontas() {
        assertEquals("2PQ8LV9YC", TagNormalizer.normalize("  #2PQ8LV9YC \n"))
    }

    @Test
    fun recusaCaracteresQueOJogoNaoUsa() {
        assertNull(TagNormalizer.normalize("#2PQ8LV9YA"))
        assertNull(TagNormalizer.normalize("#2PQ 8LV"))
        assertNull(TagNormalizer.normalize("##2PQ8LV9YC"))
    }

    @Test
    fun recusaTagVaziaCurtaOuLongaDemais() {
        assertNull(TagNormalizer.normalize(""))
        assertNull(TagNormalizer.normalize("#"))
        assertNull(TagNormalizer.normalize("#2P"))
        assertNull(TagNormalizer.normalize("#2PQ8LV9YC2PQ8"))
    }
}
