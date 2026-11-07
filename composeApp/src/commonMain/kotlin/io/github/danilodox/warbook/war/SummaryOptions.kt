package io.github.danilodox.warbook.war

/** Partes do resumo do WhatsApp que o usuário liga e desliga. O cabeçalho (e a rodada na CWL) sempre vai. */
enum class SummaryPart {
    /** Placar; na guerra encerrada, também o resultado. */
    SCORE,

    /** Tempo até começar (preparação) ou até acabar (em guerra). */
    TIME,

    /** Quem falta atacar; na guerra encerrada, quem não atacou. */
    ATTACKERS,

    /** Força por CV, só na preparação. */
    STRENGTH,

    /** Ataques de 3 estrelas (ou os melhores, se não houver nenhum). */
    BEST_ATTACKS,

    /** Ataques de 0 ou 1 estrela. */
    WEAK_ATTACKS,
}

data class SummaryOptions(val parts: Set<SummaryPart> = DEFAULT_PARTS) {
    operator fun contains(part: SummaryPart) = part in parts

    fun toggle(part: SummaryPart) = SummaryOptions(if (part in parts) parts - part else parts + part)

    companion object {
        /** O modelo fixo da v0.4.0: quem não mexer recebe o mesmo texto. */
        val DEFAULT_PARTS = setOf(SummaryPart.SCORE, SummaryPart.TIME, SummaryPart.ATTACKERS, SummaryPart.STRENGTH)
    }
}
