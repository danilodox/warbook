package io.github.danilodox.warbook.index

import java.text.Normalizer
import java.util.Locale

/**
 * Deixa o nick no formato em que a busca procura: minúsculas, sem acentos e com as letras "estilizadas"
 * trocadas pelas comuns. Quem busca "joao" acha "JoÃo", "ＪＯＡＯ", "𝓙𝓸𝓪𝓸" e "ᴊᴏᴀᴏ".
 *
 * Passos:
 * 1. NFKC: letras de largura total, matemáticas (𝐀, 𝓐, 𝔸), circuladas (Ⓐ) e ligaduras viram as comuns;
 * 2. versaletes (ᴀ ʙ ᴄ...), que o NFKC não troca, por uma tabela própria;
 * 3. NFD e fora as marcas combinantes das letras latinas e gregas (acentos, til, cedilha, "zalgo");
 * 4. minúsculas; o que não é letra nem número vira espaço; espaços repetidos viram um.
 *
 * Letras de outros alfabetos (cirílico, árabe, japonês...) ficam como estão: são o próprio nick.
 * Se não sobrar nada (nick só de símbolos, como "★★★"), fica o texto do passo 1 em minúsculas, para que
 * ainda dê para achar digitando os mesmos símbolos.
 */
object NickNormalizer {
    private val SMALL_CAPS: Map<Char, Char> = mapOf(
        'ᴀ' to 'a', 'ʙ' to 'b', 'ᴄ' to 'c', 'ᴅ' to 'd', 'ᴇ' to 'e', 'ғ' to 'f', 'ꜰ' to 'f', 'ɢ' to 'g',
        'ʜ' to 'h', 'ɪ' to 'i', 'ᴊ' to 'j', 'ᴋ' to 'k', 'ʟ' to 'l', 'ᴍ' to 'm', 'ɴ' to 'n', 'ᴏ' to 'o',
        'ᴘ' to 'p', 'ǫ' to 'q', 'ʀ' to 'r', 'ꜱ' to 's', 'ᴛ' to 't', 'ᴜ' to 'u', 'ᴠ' to 'v', 'ᴡ' to 'w',
        'ʏ' to 'y', 'ᴢ' to 'z',
    )
    private val NOT_LETTER_OR_DIGIT = Regex("[^\\p{L}\\p{N}]+")

    /**
     * Alfabetos em que a marca é "enfeite" (acento, zalgo). Em cirílico (й, ё), japonês (ド) ou hindi ela
     * forma outra letra, então fica.
     */
    private val SCRIPTS_WITHOUT_MARKS = setOf(Character.UnicodeScript.LATIN, Character.UnicodeScript.GREEK)

    fun normalize(nick: String): String {
        val compatible = Normalizer.normalize(nick, Normalizer.Form.NFKC)
        val plain = compatible.map { SMALL_CAPS[it] ?: it }.joinToString("")
        val withoutMarks = Normalizer.normalize(stripMarks(plain), Normalizer.Form.NFC)
        val words = withoutMarks.lowercase(Locale.ROOT).replace(NOT_LETTER_OR_DIGIT, " ").trim()
        return words.ifEmpty { compatible.lowercase(Locale.ROOT).trim() }
    }

    /** Tira as marcas combinantes que vêm depois de letra latina ou grega (e as soltas no começo). */
    private fun stripMarks(text: String): String {
        val decomposed = Normalizer.normalize(text, Normalizer.Form.NFD)
        val out = StringBuilder(decomposed.length)
        var baseScript: Character.UnicodeScript? = null
        var i = 0
        while (i < decomposed.length) {
            val codePoint = decomposed.codePointAt(i)
            val isMark = Character.getType(codePoint).toByte() in MARK_TYPES
            if (!isMark) baseScript = Character.UnicodeScript.of(codePoint)
            if (!isMark || (baseScript != null && baseScript !in SCRIPTS_WITHOUT_MARKS)) out.appendCodePoint(codePoint)
            i += Character.charCount(codePoint)
        }
        return out.toString()
    }

    private val MARK_TYPES = setOf(
        Character.NON_SPACING_MARK,
        Character.COMBINING_SPACING_MARK,
        Character.ENCLOSING_MARK,
    )
}
