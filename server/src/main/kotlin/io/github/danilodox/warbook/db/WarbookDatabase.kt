package io.github.danilodox.warbook.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.danilodox.warbook.config.DatabaseConfig
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import java.sql.SQLException
import javax.sql.DataSource

/**
 * Postgres do servidor: pool de conexões (Hikari), migrations versionadas (Flyway, em
 * `resources/db/migration`) e o [Database] do Exposed. As migrations rodam antes de qualquer consulta.
 */
class WarbookDatabase private constructor(private val dataSource: HikariDataSource) : AutoCloseable {
    val exposed: Database = Database.connect(dataSource)

    override fun close() = dataSource.close()

    companion object {
        private const val MAX_POOL_SIZE = 5
        private const val CONNECTION_TIMEOUT_MS = 5_000L

        /**
         * Conecta e aplica as migrations pendentes. Se o banco não responder ou recusar o usuário,
         * falha com [DatabaseUnavailableException] dizendo o que conferir (sem a senha).
         */
        fun connect(config: DatabaseConfig): WarbookDatabase {
            val hikari = HikariConfig().apply {
                jdbcUrl = config.url
                username = config.user
                password = config.password
                maximumPoolSize = MAX_POOL_SIZE
                connectionTimeout = CONNECTION_TIMEOUT_MS
                poolName = "warbook-db"
            }
            val dataSource = try {
                HikariDataSource(hikari)
            } catch (@Suppress("TooGenericExceptionCaught") e: RuntimeException) {
                throw DatabaseUnavailableException(config, e)
            }
            try {
                migrate(dataSource)
            } catch (@Suppress("TooGenericExceptionCaught") e: RuntimeException) {
                dataSource.close()
                throw DatabaseUnavailableException(config, e)
            }
            return WarbookDatabase(dataSource)
        }

        fun migrate(dataSource: DataSource) {
            Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate()
        }
    }
}

/** O banco não respondeu ou recusou a conexão. A mensagem diz o que conferir e nunca mostra a senha. */
class DatabaseUnavailableException(config: DatabaseConfig, cause: Throwable) :
    IllegalStateException(
        "Não consegui usar o Postgres em ${config.url} com o usuário ${config.user}: ${cause.rootMessage()}. " +
            "Confira se o serviço do PostgreSQL está rodando, se o banco e o usuário existem e se " +
            "DATABASE_URL, DATABASE_USER e DATABASE_PASSWORD no server/.env estão certos.",
        cause,
    )

private fun Throwable.rootMessage(): String {
    var current: Throwable = this
    while (current.cause != null && current.cause !== current) current = current.cause!!
    val sqlState = (current as? SQLException)?.sqlState?.let { " (SQLSTATE $it)" }.orEmpty()
    return (current.message ?: current::class.simpleName.orEmpty()) + sqlState
}
