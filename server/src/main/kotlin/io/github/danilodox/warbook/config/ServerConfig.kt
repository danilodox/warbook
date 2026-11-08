package io.github.danilodox.warbook.config

/**
 * Configuração do servidor, lida de variáveis de ambiente.
 * Os segredos nunca aparecem em [toString] nem em mensagens de erro.
 */
class ServerConfig(
    val cocApiKey: String,
    val jwtSecret: String,
    val database: DatabaseConfig,
    val cocBaseUrl: String = DEFAULT_COC_BASE_URL,
) {
    override fun toString(): String =
        "ServerConfig(cocBaseUrl=$cocBaseUrl, cocApiKey=***, jwtSecret=***, database=$database)"

    companion object {
        const val ENV_COC_API_KEY = "COC_API_KEY"
        const val ENV_JWT_SECRET = "JWT_SECRET"
        const val ENV_DATABASE_URL = "DATABASE_URL"
        const val ENV_DATABASE_USER = "DATABASE_USER"
        const val ENV_DATABASE_PASSWORD = "DATABASE_PASSWORD"
        const val DEFAULT_COC_BASE_URL = "https://api.clashofclans.com/v1"
        const val MIN_JWT_SECRET_LENGTH = 32
        private const val JDBC_POSTGRES_PREFIX = "jdbc:postgresql://"

        /** Lê a configuração; falha com uma mensagem clara (com tudo o que falta de uma vez) se faltar algo. */
        fun fromEnv(env: (String) -> String? = System::getenv): ServerConfig {
            fun read(name: String) = env(name)?.trim().orEmpty()
            val apiKey = read(ENV_COC_API_KEY)
            val jwtSecret = read(ENV_JWT_SECRET)
            val dbUrl = read(ENV_DATABASE_URL)
            val dbUser = read(ENV_DATABASE_USER)
            val dbPassword = read(ENV_DATABASE_PASSWORD)
            val problems = buildList {
                if (apiKey.isEmpty()) add("$ENV_COC_API_KEY não definida")
                if (jwtSecret.isEmpty()) {
                    add("$ENV_JWT_SECRET não definida")
                } else if (jwtSecret.length < MIN_JWT_SECRET_LENGTH) {
                    add("$ENV_JWT_SECRET precisa de pelo menos $MIN_JWT_SECRET_LENGTH caracteres")
                }
                if (dbUrl.isEmpty()) {
                    add("$ENV_DATABASE_URL não definida")
                } else if (!dbUrl.startsWith(JDBC_POSTGRES_PREFIX)) {
                    add("$ENV_DATABASE_URL precisa começar com $JDBC_POSTGRES_PREFIX")
                }
                if (dbUser.isEmpty()) add("$ENV_DATABASE_USER não definida")
                if (dbPassword.isEmpty()) add("$ENV_DATABASE_PASSWORD não definida")
            }
            if (problems.isNotEmpty()) {
                throw InvalidConfigException(
                    problems.joinToString(separator = "; ") +
                        ". Copie server/.env.example para server/.env e preencha os valores.",
                )
            }
            return ServerConfig(
                cocApiKey = apiKey,
                jwtSecret = jwtSecret,
                database = DatabaseConfig(url = dbUrl, user = dbUser, password = dbPassword),
            )
        }
    }
}

/** Conexão com o Postgres. A senha nunca aparece em [toString]. */
class DatabaseConfig(val url: String, val user: String, val password: String) {
    override fun toString(): String = "DatabaseConfig(url=$url, user=$user, password=***)"
}

class InvalidConfigException(message: String) : IllegalStateException(message)
