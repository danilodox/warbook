package io.github.danilodox.warbook.ui.clan

import io.github.danilodox.warbook.api.ClanMemberDto
import io.github.danilodox.warbook.api.ClanRole
import kotlin.test.Test
import kotlin.test.assertEquals

class MemberSortTest {

    @Suppress("LongParameterList")
    private fun member(name: String, rank: Int, townHall: Int, trophies: Int, donated: Int, received: Int = 0) =
        ClanMemberDto(name, name, ClanRole.MEMBER, townHall, 100, trophies, null, donated, received, rank)

    private val members = listOf(
        member("C", rank = 3, townHall = 16, trophies = 4000, donated = 100),
        member("A", rank = 1, townHall = 15, trophies = 5000, donated = 100, received = 50),
        member("B", rank = 2, townHall = 16, trophies = 4500, donated = 900),
    )

    private fun List<ClanMemberDto>.names() = map { it.name }

    @Test
    fun posicaoDoClaCrescente() {
        assertEquals(listOf("A", "B", "C"), members.sortedBy(MemberSort.POSITION).names())
    }

    @Test
    fun cvDecrescenteComEmpatePelaPosicao() {
        assertEquals(listOf("B", "C", "A"), members.sortedBy(MemberSort.TOWN_HALL).names())
    }

    @Test
    fun trofeusDecrescentes() {
        assertEquals(listOf("A", "B", "C"), members.sortedBy(MemberSort.TROPHIES).names())
    }

    @Test
    fun doacoesDecrescentesEmpateQuemRecebeuMenos() {
        assertEquals(listOf("B", "C", "A"), members.sortedBy(MemberSort.DONATIONS).names())
    }
}
