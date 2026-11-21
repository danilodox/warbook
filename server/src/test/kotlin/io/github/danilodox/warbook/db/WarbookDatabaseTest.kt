package io.github.danilodox.warbook.db

import io.github.danilodox.warbook.config.DatabaseConfig
import io.github.danilodox.warbook.testing.TestDatabase
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WarbookDatabaseTest {

    @Test
    fun migrationCriaATabelaEOPgTrgm() {
        val (extension, table) = transaction(TestDatabase.database) {
            val ext = exec("SELECT count(*) FROM pg_extension WHERE extname = 'pg_trgm'") { rs ->
                rs.next()
                rs.getInt(1)
            }
            val tbl = exec("SELECT count(*) FROM information_schema.tables WHERE table_name = 'player_index'") { rs ->
                rs.next()
                rs.getInt(1)
            }
            ext to tbl
        }

        assertEquals(1, extension)
        assertEquals(1, table)
    }

    @Test
    fun migrationRodadaDuasVezesNaoMudaNada() {
        val before = appliedMigrations()

        WarbookDatabase.migrate(TestDatabase.dataSource)

        assertEquals(before, appliedMigrations())
    }

    private fun appliedMigrations(): Int? = transaction(TestDatabase.database) {
        exec("SELECT count(*) FROM flyway_schema_history WHERE success") { rs ->
            rs.next()
            rs.getInt(1)
        }
    }

    @Test
    fun connectAplicaAsMigrationsEConsulta() {
        val config = DatabaseConfig(url = TestDatabase.jdbcUrl, user = "postgres", password = "postgres")

        WarbookDatabase.connect(config).use { db ->
            val one = transaction(db.exposed) {
                exec("SELECT 1") { rs ->
                    rs.next()
                    rs.getInt(1)
                }
            }
            assertEquals(1, one)
        }
    }

    @Test
    fun bancoForaDoArDizOQueConferirSemMostrarASenha() {
        val password = "senha".repeat(3)
        // Porta 1: ninguém escuta, a conexão é recusada na hora.
        val config = DatabaseConfig("jdbc:postgresql://localhost:1/warbook", user = "warbook", password = password)

        val error = assertFailsWith<DatabaseUnavailableException> { WarbookDatabase.connect(config) }

        assertTrue("localhost:1/warbook" in error.message!!)
        assertTrue("serviço do PostgreSQL" in error.message!!)
        assertTrue("DATABASE_PASSWORD" in error.message!!)
        assertFalse(password in error.message!!)
    }
}
