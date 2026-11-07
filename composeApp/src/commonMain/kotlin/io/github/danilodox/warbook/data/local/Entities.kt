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
    /** Desde a v0.5.0 (banco v3): diz se o clã é "de uma conta" e o salvo dele fica. Antes disso, `null`. */
    val clanTag: String? = null,
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

/** Último clã visto, em JSON do `ClanDto`. Só clãs das contas e clãs seguidos (banco v3). */
@Entity(tableName = "clan_cache")
data class ClanCacheEntity(@PrimaryKey val tag: String, val json: String, val fetchedAt: Long)

/**
 * Jogador ou clã seguido (banco v3). [kind] é `PLAYER` ou `CLAN`. [name], [level] (CV do jogador, nível do
 * clã) e [imageUrl] (brasão do clã) são uma cópia para a lista "Seguindo" aparecer sem rede.
 */
@Entity(tableName = "followed", primaryKeys = ["kind", "tag"])
data class FollowedEntity(
    val kind: String,
    val tag: String,
    val name: String,
    val level: Int?,
    val imageUrl: String?,
    val followedAt: Long,
)
