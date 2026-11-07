package io.github.danilodox.warbook.war

import io.github.danilodox.warbook.api.CurrentWarDto

// Blocos "Melhores ataques" e "Ataques ruins" do resumo do WhatsApp.

/** Linhas por bloco de ataques; o resto vira "+N". */
private const val MAX_ATTACK_LINES = 5
private const val MAX_STARS = 3

/** Os blocos de ataques ligados em [options]; um bloco sem nenhum ataque não aparece. */
internal fun attackBlocks(war: CurrentWarDto, texts: SummaryTexts, options: SummaryOptions): List<List<String>> =
    listOfNotNull(
        attackBlock("🔥 *${texts.bestAttacksTitle}*", war.bestAttacks(), texts)
            .takeIf { SummaryPart.BEST_ATTACKS in options },
        attackBlock("👎 *${texts.weakAttacksTitle}*", war.weakAttacks(), texts)
            .takeIf { SummaryPart.WEAK_ATTACKS in options },
    )

/** `• Dantas → #4 (CV 15) ★★★ 100%`, no máximo [MAX_ATTACK_LINES] linhas e um "+N" com o resto. */
private fun attackBlock(title: String, attacks: List<AttackLine>, texts: SummaryTexts): List<String> {
    if (attacks.isEmpty()) return emptyList()
    val lines = attacks.take(MAX_ATTACK_LINES).map { attack ->
        val target = attack.defenderPosition?.let { position ->
            " → #$position" + (attack.defenderTownHall?.let { " (${texts.townHall(it)})" } ?: "")
        } ?: ""
        "• ${WhatsAppText.safeName(attack.attackerName)}$target ${stars(attack.stars)} ${attack.destructionPercentage}%"
    }
    val more = attacks.size - MAX_ATTACK_LINES
    return listOf(title) + lines + if (more > 0) listOf("• ${texts.more(more)}") else emptyList()
}

private fun stars(count: Int) = "★".repeat(count) + "☆".repeat((MAX_STARS - count).coerceAtLeast(0))
