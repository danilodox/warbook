package io.github.danilodox.warbook.api

/** Rotas do servidor, compartilhadas entre o app e o servidor. Tags vão sem `#` e já normalizadas. */
// Catálogo: uma função por rota com parâmetro; dividir só espalharia as rotas.
@Suppress("TooManyFunctions")
object ApiRoutes {
    const val HEALTH = "/health"
    const val INSTALLS = "/v1/installs"

    const val PARAM_TAG = "tag"
    const val PLAYER = "/v1/players/{$PARAM_TAG}"

    /** `?name=`. Como em [CLAN_SEARCH], `search` não é uma tag válida e o segmento fixo ganha de [PLAYER]. */
    const val PLAYER_SEARCH = "/v1/players/search"
    const val VERIFY = "/v1/players/{$PARAM_TAG}/verify"
    const val CLAN = "/v1/clans/{$PARAM_TAG}"

    /** `?name=`. Não colide com [CLAN]: `search` não é uma tag válida e o Ktor prefere o segmento fixo. */
    const val CLAN_SEARCH = "/v1/clans/search"
    const val PARAM_NAME = "name"
    const val CURRENT_WAR = "/v1/clans/{$PARAM_TAG}/war/current"
    const val CURRENT_CWL = "/v1/clans/{$PARAM_TAG}/cwl/current"
    const val CAPITAL_RAIDS = "/v1/clans/{$PARAM_TAG}/capitalraidseasons"

    /** `GET` estado; `POST` com [MonitoringRequest] ativa, reativa ou renova. */
    const val MONITORING = "/v1/clans/{$PARAM_TAG}/monitoring"

    /** `?limit=` (padrão [WarHistoryDto.DEFAULT_LIMIT], no máximo [WarHistoryDto.MAX_LIMIT]). */
    const val WARS = "/v1/clans/{$PARAM_TAG}/wars"
    const val PARAM_LIMIT = "limit"
    const val PARAM_WAR_ID = "warId"
    const val WAR = "/v1/clans/{$PARAM_TAG}/wars/{$PARAM_WAR_ID}"
    const val PARAM_PLAYER_TAG = "playerTag"
    const val PLAYER_HISTORY = "/v1/clans/{$PARAM_TAG}/players/{$PARAM_PLAYER_TAG}/history"
    const val ACTIVITY = "/v1/clans/{$PARAM_TAG}/activity"

    fun player(tag: String): String = "/v1/players/$tag"

    fun verify(tag: String): String = "/v1/players/$tag/verify"

    fun clan(tag: String): String = "/v1/clans/$tag"
    fun currentWar(clanTag: String): String = "/v1/clans/$clanTag/war/current"

    fun currentCwl(clanTag: String): String = "/v1/clans/$clanTag/cwl/current"

    fun capitalRaids(clanTag: String): String = "/v1/clans/$clanTag/capitalraidseasons"

    fun monitoring(clanTag: String): String = "/v1/clans/$clanTag/monitoring"

    fun wars(clanTag: String): String = "/v1/clans/$clanTag/wars"

    fun war(clanTag: String, warId: Long): String = "/v1/clans/$clanTag/wars/$warId"

    fun playerHistory(clanTag: String, playerTag: String): String = "/v1/clans/$clanTag/players/$playerTag/history"

    fun activity(clanTag: String): String = "/v1/clans/$clanTag/activity"
}
