package io.github.danilodox.warbook.data.local.model
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import io.github.danilodox.warbook.domain.model.Clan
import io.github.danilodox.warbook.domain.model.Player

@Entity(
    tableName = "players",
    foreignKeys = [
        ForeignKey(
            entity = ClanEntity::class,
            parentColumns = ["tag"],
            childColumns = ["clanTag"],
            onDelete = ForeignKey.SET_NULL // Se um clan for deletado, o jogador não será
        )
    ],
    indices = [Index(value = ["clanTag"])]
)
data class PlayerEntity(
    @PrimaryKey val tag: String,
    val name: String,
    val expLevel: Int,
    val townHallLevel: Int,
    val clanTag: String? = null
)

/**
 * Classe de Relação que junta um PlayerEntity com seu ClanEntity correspondente.
 * O Room usará isso para buscar os dados de ambas as tabelas de uma só vez.
 */
data class PlayerWithClan(
    @Embedded val player: PlayerEntity,
    @Relation(
        parentColumn = "clanTag", // Campo na tabela 'players'
        entityColumn = "tag"      // Campo na tabela 'clans'
    )
    val clan: ClanEntity?
)


fun PlayerWithClan.toDomain(): Player {
    return Player(
        tag = this.player.tag,
        name = this.player.name,
        expLevel = this.player.expLevel,
        townHallLevel = this.player.townHallLevel,
        trophies = 0,
        warStars = 0,
        donations = 0,
        troops = emptyList(),
        heroes = emptyList(),
        spells = emptyList(),
        clan = this.clan?.let { clanEntity ->
            Clan(
                tag = clanEntity.tag,
                name = clanEntity.name,
                clanLevel = clanEntity.level
            )
        }
    )
}

fun Player.toEntity(): PlayerEntity {
    return PlayerEntity(
        tag = this.tag,
        name = this.name,
        expLevel = this.expLevel,
        townHallLevel = this.townHallLevel,
        clanTag = this.clan?.tag
    )
}



