package io.github.danilodox.warbook.coc

import io.github.danilodox.warbook.api.WarState
import io.github.danilodox.warbook.history.WarRecord
import io.github.danilodox.warbook.history.WarResult
import io.github.danilodox.warbook.history.WarSide
import io.github.danilodox.warbook.history.WarSource
import io.github.danilodox.warbook.history.WarType
import java.time.Instant

/**
 * Entrada do warlog → [WarRecord] só com o placar. As entradas da CWL (sem clã adversário) ficam de fora:
 * a CWL vem completa pelo `warTag` enquanto a temporada está aberta.
 */
internal fun CocWarLogEntry.toWarRecord(capturedAt: Instant): WarRecord? {
    val end = parseCocTime(endTime)
    val ours = clan
    val theirs = opponent?.takeIf { it.tag != null }
    if (end == null || ours == null || theirs == null) return null
    return WarRecord(
        type = WarType.WAR,
        source = WarSource.WARLOG,
        state = WarState.WAR_ENDED,
        teamSize = teamSize,
        attacksPerMember = attacksPerMember,
        endTime = Instant.ofEpochMilli(end),
        clan = ours.toSide(),
        opponent = theirs.toSide(),
        capturedAt = capturedAt,
        result = cocWarResult(result) ?: warResult(ours, theirs),
    )
}

internal fun cocWarResult(value: String?): WarResult? = when (value) {
    "win" -> WarResult.WIN
    "lose" -> WarResult.LOSE
    "tie" -> WarResult.TIE
    else -> null
}

/** Mesma regra do jogo: mais estrelas vence; empate de estrelas, mais destruição. */
internal fun warResult(ours: CocWarClan, theirs: CocWarClan): WarResult = when {
    ours.stars != theirs.stars -> if (ours.stars > theirs.stars) WarResult.WIN else WarResult.LOSE
    ours.destructionPercentage != theirs.destructionPercentage ->
        if (ours.destructionPercentage > theirs.destructionPercentage) WarResult.WIN else WarResult.LOSE
    else -> WarResult.TIE
}

private fun CocWarClan.toSide() = WarSide(
    tag = tag?.withoutHash(),
    name = name,
    badgeUrl = badgeUrls?.medium,
    stars = stars,
    destruction = destructionPercentage,
    // No warlog o adversário vem sem ataques: 0 aqui é "não sei", não "nenhum".
    attacks = attacks.takeIf { it > 0 },
)
