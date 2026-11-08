package io.github.danilodox.warbook.ui.compare

import io.github.danilodox.warbook.api.PlayerDto

/** Números gerais e da temporada que entram no Comparar. Em todos, mais é melhor. */
enum class CompareStat { TOWN_HALL, LEVEL, TROPHIES, WAR_STARS, DONATIONS, ATTACK_WINS }

enum class CompareSection { GENERAL, HEROES, SEASON }

/** O nome da linha: um número do perfil ou um herói (pelo nome da API). */
sealed interface CompareLabel {
    data class Stat(val stat: CompareStat) : CompareLabel
    data class Hero(val apiName: String) : CompareLabel
}

/** Uma linha: o meu valor e o do outro. `null` = herói que a pessoa ainda não tem (conta como 0). */
data class CompareRow(val label: CompareLabel, val mine: Int?, val theirs: Int?) {
    /** Positivo: eu à frente; negativo: o outro; 0: empate. */
    val result: Int get() = (mine ?: 0).compareTo(theirs ?: 0)
}

data class CompareGroup(val section: CompareSection, val rows: List<CompareRow>)

data class Comparison(val groups: List<CompareGroup>) {
    private val rows get() = groups.flatMap { it.rows }
    val total: Int get() = rows.size
    val mineAhead: Int get() = rows.count { it.result > 0 }
    val theirsAhead: Int get() = rows.count { it.result < 0 }
    val ties: Int get() = rows.count { it.result == 0 }
}

/**
 * Compara [mine] (a conta ativa) com [theirs], na ordem do design: Geral, Heróis e Temporada.
 * Os heróis seguem a ordem do jogo; um herói que só um dos dois tem também entra.
 */
fun compare(mine: PlayerDto, theirs: PlayerDto): Comparison {
    fun stat(stat: CompareStat, value: PlayerDto.() -> Int) =
        CompareRow(CompareLabel.Stat(stat), mine.value(), theirs.value())

    val general = listOf(
        stat(CompareStat.TOWN_HALL) { townHallLevel },
        stat(CompareStat.LEVEL) { expLevel },
        stat(CompareStat.TROPHIES) { trophies },
        stat(CompareStat.WAR_STARS) { warStars },
    )
    val myHeroes = mine.heroes.associate { it.name to it.level }
    val theirHeroes = theirs.heroes.associate { it.name to it.level }
    val heroes = (mine.heroes.map { it.name } + theirs.heroes.map { it.name }).distinct()
        .map { CompareRow(CompareLabel.Hero(it), myHeroes[it], theirHeroes[it]) }
    val season = listOf(
        stat(CompareStat.DONATIONS) { donations },
        stat(CompareStat.ATTACK_WINS) { attackWins },
    )
    return Comparison(
        listOf(
            CompareGroup(CompareSection.GENERAL, general),
            CompareGroup(CompareSection.HEROES, heroes),
            CompareGroup(CompareSection.SEASON, season),
        ).filter { it.rows.isNotEmpty() },
    )
}
