package io.github.danilodox.warbook.api

/** Rotas do servidor, compartilhadas entre o app e o servidor. Tags vão sem `#` e já normalizadas. */
object ApiRoutes {
    const val HEALTH = "/health"
    const val INSTALLS = "/v1/installs"

    const val PARAM_TAG = "tag"
    const val PLAYER = "/v1/players/{$PARAM_TAG}"
    const val VERIFY = "/v1/players/{$PARAM_TAG}/verify"
    const val CLAN = "/v1/clans/{$PARAM_TAG}"
    const val CURRENT_WAR = "/v1/clans/{$PARAM_TAG}/war/current"
    const val CURRENT_CWL = "/v1/clans/{$PARAM_TAG}/cwl/current"

    fun player(tag: String): String = "/v1/players/$tag"

    fun verify(tag: String): String = "/v1/players/$tag/verify"

    fun clan(tag: String): String = "/v1/clans/$tag"

    fun currentWar(clanTag: String): String = "/v1/clans/$clanTag/war/current"

    fun currentCwl(clanTag: String): String = "/v1/clans/$clanTag/cwl/current"
}
