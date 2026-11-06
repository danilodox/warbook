package io.github.danilodox.warbook.api

import kotlinx.serialization.Serializable

/**
 * Liga de Guerra de Clãs (CWL) atual do clã (tela G3). Fora da CWL só vem [state].
 * [rounds] tem todas as rodadas do grupo, sorteadas ou não.
 */
@Serializable
data class CwlDto(
    val state: CwlState,
    /** Temporada no formato `AAAA-MM`. */
    val season: String? = null,
    /** Rodada que a tela abre (1 = primeira): a em guerra, senão a em preparação, senão a última sorteada. */
    val currentRound: Int? = null,
    /** Classificação do grupo, do primeiro ao último. */
    val clans: List<CwlClanDto> = emptyList(),
    val rounds: List<CwlRoundDto> = emptyList(),
)

@Serializable
enum class CwlState { NOT_IN_CWL, PREPARATION, IN_WAR, ENDED }

/**
 * Clã do grupo com o total da temporada até agora. [stars] soma as estrelas das guerras mais
 * 10 por vitória (regra do jogo); [destructionPercentage] soma a destruição de cada guerra.
 */
@Serializable
data class CwlClanDto(
    val tag: String,
    val name: String,
    val badgeUrl: String? = null,
    val clanLevel: Int,
    val stars: Int,
    val destructionPercentage: Double,
    val wins: Int,
)

/** [war] é a guerra do clã consultado na rodada, já com ele em `clan`; `null` se a rodada ainda não foi sorteada. */
@Serializable
data class CwlRoundDto(val round: Int, val war: CurrentWarDto? = null)
