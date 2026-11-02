package io.github.danilodox.warbook.api

/** Rotas do servidor, compartilhadas entre o app e o servidor. Tags vão sem `#` e já normalizadas. */
object ApiRoutes {
    const val HEALTH = "/health"
    const val INSTALLS = "/v1/installs"

    const val PARAM_TAG = "tag"
    const val PLAYER = "/v1/players/{$PARAM_TAG}"
    const val CLAN = "/v1/clans/{$PARAM_TAG}"
    const val CURRENT_WAR = "/v1/clans/{$PARAM_TAG}/war/current"

    fun player(tag: String): String = "/v1/players/$tag"

    fun clan(tag: String): String = "/v1/clans/$tag"

    fun currentWar(clanTag: String): String = "/v1/clans/$clanTag/war/current"
}
