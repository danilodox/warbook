package io.github.danilodox.warbook.testing

import io.github.danilodox.warbook.db.WarbookDatabase
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * Postgres de verdade (embedded-postgres, sem Docker), um por execução dos testes: sobe na primeira vez
 * que é usado (~2 s) e desce quando a JVM termina. As migrations rodam uma vez, como no servidor.
 */
object TestDatabase {
    private val postgres: EmbeddedPostgres by lazy {
        EmbeddedPostgres.builder().start().also { pg ->
            Runtime.getRuntime().addShutdownHook(Thread { pg.close() })
        }
    }

    val dataSource by lazy { postgres.postgresDatabase.also(WarbookDatabase::migrate) }

    val database: Database by lazy { Database.connect(dataSource) }

    val jdbcUrl: String get() = postgres.getJdbcUrl("postgres", "postgres")

    /** Esvazia as tabelas entre um teste e outro (o schema continua). */
    fun clean() {
        transaction(database) { exec("TRUNCATE player_index") }
    }
}
