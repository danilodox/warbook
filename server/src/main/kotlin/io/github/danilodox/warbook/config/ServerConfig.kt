package io.github.danilodox.warbook.config

/**
 * Configuração do servidor, lida de variáveis de ambiente.
 * Os segredos nunca aparecem em [toString] nem em mensagens de erro.
 */
class ServerConfig(val cocApiKey: String, val jwtSecret: String, val cocBaseUrl: String = DEFAULT_COC_BASE_URL) {
    override fun toString(): String = "ServerConfig(cocBaseUrl=$cocBaseUrl, cocApiKey=***, jwtSecret=***)"

    companion object {
        const val ENV_COC_API_KEY = "COC_API_KEY"
        const val ENV_JWT_SECRET = "JWT_SECRET"
        const val DEFAULT_COC_BASE_URL = "https://api.clashofclans.com/v1"
        const val MIN_JWT_SECRET_LENGTH = 32

        /** Lê a configuração; falha com uma mensagem clara se faltar algo. */
        fun fromEnv(env: (String) -> String? = System::getenv): ServerConfig {
            val apiKey = env(ENV_COC_API_KEY)?.trim().orEmpty()
            val jwtSecret = env(ENV_JWT_SECRET)?.trim().orEmpty()
            val problems = buildList {
                if (apiKey.isEmpty()) add("$ENV_COC_API_KEY não definida")
                if (jwtSecret.isEmpty()) {
                    add("$ENV_JWT_SECRET não definida")
                } else if (jwtSecret.length < MIN_JWT_SECRET_LENGTH) {
                    add("$ENV_JWT_SECRET precisa de pelo menos $MIN_JWT_SECRET_LENGTH caracteres")
                }
            }
            if (problems.isNotEmpty()) {
                throw InvalidConfigException(
                    problems.joinToString(separator = "; ") +
                        ". Copie server/.env.example para server/.env e preencha os valores.",
                )
            }
            return ServerConfig(cocApiKey = apiKey, jwtSecret = jwtSecret)
        }
    }
}

class InvalidConfigException(message: String) : IllegalStateException(message)
