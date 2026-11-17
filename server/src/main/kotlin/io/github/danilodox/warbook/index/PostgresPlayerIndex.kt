package io.github.danilodox.warbook.index

import io.github.danilodox.warbook.api.PlayerSearch
import io.github.danilodox.warbook.api.PlayerSearchResultDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.IntegerColumnType
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.TextColumnType
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.batchUpsert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.coroutines.cancellation.CancellationException

/** Tabela criada pela migration `V1__indice_de_nicks.sql`. */
internal object PlayerIndexTable : Table("player_index") {
    val tag = text("tag")
    val name = text("name")
    val nameNormalized = text("name_normalized")
    val townHall = short("town_hall")
    val clanTag = text("clan_tag").nullable()
    val clanName = text("clan_name").nullable()
    val seenAt = timestampWithTimeZone("seen_at")
    override val primaryKey = PrimaryKey(tag)
}

/**
 * [PlayerIndex] no Postgres. A busca compara o nick normalizado ([NickNormalizer]) por semelhança de
 * trigramas (`pg_trgm`) e também por trecho, porque com 3 ou 4 letras a semelhança sozinha erra muito.
 * Ordem: igual ao buscado, depois começa com ele, depois o mais parecido, depois o visto mais recente.
 *
 * [scope] é onde as gravações rodam (o servidor cancela ao parar); [clock] é trocado nos testes.
 */
class PostgresPlayerIndex(
    private val database: Database,
    private val scope: CoroutineScope,
    private val clock: Clock = Clock.systemUTC(),
) : PlayerIndex {
    private val log = LoggerFactory.getLogger(PostgresPlayerIndex::class.java)

    override fun record(players: List<IndexedPlayer>) {
        if (players.isEmpty()) return
        scope.launch {
            try {
                upsert(players)
            } catch (e: CancellationException) {
                throw e
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                // ExposedSQLException (consulta) ou SQLException do Hikari (banco fora do ar): o índice é extra,
                // a resposta ao app já foi.
                log.warn("Não gravei {} jogador(es) no índice de nicks: {}", players.size, e.message)
            }
        }
    }

    /** Grava já, numa transação só. Público para os testes; o servidor usa [record]. */
    suspend fun upsert(players: List<IndexedPlayer>) = withContext(Dispatchers.IO) {
        val now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
        // O upsert em lote do Postgres falha se a mesma tag vier duas vezes no mesmo comando.
        val unique = players.associateBy { it.tag }.values
        transaction(database) {
            PlayerIndexTable.batchUpsert(unique, shouldReturnGeneratedValues = false) { player ->
                this[PlayerIndexTable.tag] = player.tag
                this[PlayerIndexTable.name] = player.name
                this[PlayerIndexTable.nameNormalized] = NickNormalizer.normalize(player.name)
                this[PlayerIndexTable.townHall] = player.townHallLevel.toShort()
                this[PlayerIndexTable.clanTag] = player.clanTag
                this[PlayerIndexTable.clanName] = player.clanName
                this[PlayerIndexTable.seenAt] = now
            }
        }
    }

    override suspend fun search(name: String): List<PlayerSearchResultDto> = withContext(Dispatchers.IO) {
        val query = NickNormalizer.normalize(name)
        val contains = "%${query.escapeLike()}%"
        val prefix = "${query.escapeLike()}%"
        val text = TextColumnType()
        transaction(database) {
            exec(
                SEARCH_SQL,
                args = listOf(
                    text to query,
                    text to contains,
                    text to query,
                    text to prefix,
                    text to query,
                    IntegerColumnType() to PlayerSearch.MAX_RESULTS,
                ),
            ) { rs ->
                buildList {
                    while (rs.next()) {
                        add(
                            PlayerSearchResultDto(
                                tag = rs.getString("tag"),
                                name = rs.getString("name"),
                                townHallLevel = rs.getInt("town_hall"),
                                clanTag = rs.getString("clan_tag"),
                                clanName = rs.getString("clan_name"),
                                seenAt = rs.getTimestamp("seen_at").time,
                            ),
                        )
                    }
                }
            }.orEmpty()
        }
    }

    private companion object {
        /** `%` é o operador de semelhança do pg_trgm (limite padrão 0,3); os dois filtros usam o índice GIN. */
        const val SEARCH_SQL = """
            SELECT tag, name, town_hall, clan_tag, clan_name, seen_at
            FROM player_index
            WHERE name_normalized % ? OR name_normalized LIKE ?
            ORDER BY name_normalized = ? DESC,
                     name_normalized LIKE ? DESC,
                     similarity(name_normalized, ?) DESC,
                     seen_at DESC
            LIMIT ?
        """

        /** `%`, `_` e `\` no nick são letras, não curingas do LIKE. */
        fun String.escapeLike(): String = replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
    }
}
