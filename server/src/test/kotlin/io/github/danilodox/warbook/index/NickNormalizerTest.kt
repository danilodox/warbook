package io.github.danilodox.warbook.index

import kotlin.test.Test
import kotlin.test.assertEquals

class NickNormalizerTest {

    private fun assertNormalizes(expected: String, nick: String) =
        assertEquals(expected, NickNormalizer.normalize(nick))

    @Test
    fun minusculasESemAcentos() {
        assertNormalizes("joao", "JoÃo")
        assertNormalizes("ceu azul", "Céu Azul")
        assertNormalizes("muller", "Müller")
    }

    @Test
    fun letrasEstilizadasViramAsComuns() {
        assertNormalizes("joao", "ＪＯＡＯ") // largura total
        assertNormalizes("joao", "𝓙𝓸𝓪𝓸") // matemática cursiva
        assertNormalizes("joao", "𝕁𝕆𝔸𝕆") // matemática dupla
        assertNormalizes("joao", "ⒿⓄⒶⓄ") // circuladas
        assertNormalizes("joao", "ᴊᴏᴀᴏ") // versaletes
    }

    @Test
    fun simbolosViramEspacoEEspacosRepetidosViramUm() {
        assertNormalizes("rei do clash", "★ Rei_do--Clash ★")
        assertNormalizes("x pro x", "x•PRO•x")
        assertNormalizes("danilo", "  ~Danilo~  ")
    }

    @Test
    fun zalgoPerdeAsMarcasEmpilhadas() {
        assertNormalizes("zalgo", "Z̷̢a̵̛l̶g̸o̴")
    }

    @Test
    fun outrosAlfabetosFicam() {
        assertNormalizes("дмитрий", "Дмитрий") // o й não vira и
        assertNormalizes("αθηνα", "Αθηνά")
        assertNormalizes("ドラゴン", "ドラゴン")
        assertNormalizes("ドラゴン", "ﾄﾞﾗｺﾞﾝ") // meia largura vira a normal
    }

    @Test
    fun numerosFicam() {
        assertNormalizes("clash2026", "Clash²⁰²⁶") // sobrescritos viram números comuns
        assertNormalizes("th17 boss", "TH17_Boss")
    }

    @Test
    fun soSimbolosGuardaOsSimbolos() {
        assertNormalizes("★★★", "★★★")
    }
}
