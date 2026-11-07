package io.github.danilodox.warbook.war

/**
 * O WhatsApp não tem escape: `*` e `_` num nome ligam negrito e itálico e quebram o texto em volta.
 * Troca pelos parecidos que ele não interpreta (`∗` U+2217 e `＿` U+FF3F). Só no texto gerado; no app o
 * nome aparece como veio do jogo.
 */
internal object WhatsAppText {
    private const val ASTERISK = '∗'
    private const val UNDERSCORE = '＿'

    fun safeName(name: String): String = name.replace('*', ASTERISK).replace('_', UNDERSCORE)
}
