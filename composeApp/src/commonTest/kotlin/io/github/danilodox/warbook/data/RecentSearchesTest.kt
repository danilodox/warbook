package io.github.danilodox.warbook.data

import kotlin.test.Test
import kotlin.test.assertEquals

class RecentSearchesTest {

    private fun player(tag: String) = RecentSearch(FollowKind.PLAYER, tag, "Jogador $tag")

    @Test
    fun novoVaiParaOTopoSemRepetir() {
        val list = RecentSearches.add(listOf(player("A"), player("B")), player("B"))

        assertEquals(listOf("B", "A"), list.map { it.tag })
    }

    @Test
    fun mesmaTagDeTipoDiferenteNaoEARepetida() {
        val clan = RecentSearch(FollowKind.CLAN, "A", "Clã A")

        assertEquals(2, RecentSearches.add(listOf(player("A")), clan).size)
    }

    @Test
    fun guardaSoOsDezMaisRecentes() {
        val list = (1..12).fold(emptyList<RecentSearch>()) { acc, i -> RecentSearches.add(acc, player("T$i")) }

        assertEquals(RecentSearches.LIMIT, list.size)
        assertEquals("T12", list.first().tag)
    }

    @Test
    fun idaEVoltaEFormatoAntigoViraVazio() {
        val list = listOf(player("A"), RecentSearch(FollowKind.CLAN, "C", "Clã", 18, "https://img/m.png"))

        assertEquals(list, RecentSearches.decode(RecentSearches.encode(list)))
        assertEquals(emptyList(), RecentSearches.decode("""{"formato":"antigo"}"""))
        assertEquals(emptyList(), RecentSearches.decode(null))
    }
}
