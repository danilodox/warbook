package io.github.danilodox.warbook.data.local

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * Conta do usuário. Os dados do cabeçalho (nome, CV, clã) são uma cópia do último perfil, para a troca de
 * conta aparecer sem rede. [verificationToken] é a prova assinada pelo servidor (não é o token do jogo,
 * que nunca é guardado).
 */
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val tag: String,
    val name: String,
    val townHallLevel: Int,
    val clanName: String?,
    val verificationToken: String?,
    val isActive: Boolean,
    val addedAt: Long,
)

/** Último perfil visto de um jogador, em JSON do `PlayerDto`, para o modo offline. */
@Entity(tableName = "player_cache")
data class PlayerCacheEntity(@PrimaryKey val tag: String, val json: String, val fetchedAt: Long)

/**
 * Último resultado da aba Guerra de um clã, para o modo offline. [cacheKey] é `war:<tag>` (JSON do
 * `CurrentWarDto`) ou `cwl:<tag>` (JSON do `CwlDto`).
 */
@Entity(tableName = "war_cache")
data class WarCacheEntity(@PrimaryKey val cacheKey: String, val json: String, val fetchedAt: Long)
