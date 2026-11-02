package io.github.danilodox.warbook.api

/**
 * Normaliza tags de jogador e clã: tira o `#`, passa para maiúsculas e troca `O` por `0`
 * (o jogo não usa a letra O). Só aceita os caracteres que o jogo gera.
 */
object TagNormalizer {
    const val VALID_CHARS = "0289PYLQGRJCUV"
    private val validTag = Regex("^[$VALID_CHARS]{3,12}$")

    /** Tag normalizada, sem `#`, ou `null` se não for uma tag possível. */
    fun normalize(raw: String): String? {
        val tag = raw.trim().removePrefix("#").uppercase().replace('O', '0')
        return tag.takeIf { validTag.matches(it) }
    }
}
