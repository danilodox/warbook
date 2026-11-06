package io.github.danilodox.warbook.testing

/**
 * CWL de exemplo: grupo de 4 clãs (o nosso é [CocFixtures.CLAN_TAG]) e 3 rodadas.
 * R1 encerrada (W1: nós 20 × 15 BBB; W2: CCC 18 × 18 DDD, CCC vence na destruição).
 * R2 em guerra (W3: BBB 5 × 8 nós, com o nosso clã do lado `opponent`; W4: CCC 3 × 4 DDD). R3 não sorteada.
 */
object CwlFixtures {
    private const val OURS = "#${CocFixtures.CLAN_TAG}"

    const val GROUP_PATH = "/v1/clans/%23${CocFixtures.CLAN_TAG}/currentwar/leaguegroup"

    fun warPath(warTag: String) = "/v1/clanwarleagues/wars/%23$warTag"

    val group = """
        {
          "state": "inWar", "season": "2026-11",
          "clans": [
            { "tag": "$OURS", "name": "Picuí Raiz", "clanLevel": 18, "badgeUrls": { "medium": "https://img/a.png" },
              "members": [ { "tag": "#${CocFixtures.PLAYER_TAG}", "name": "Dantas", "townHallLevel": 15 } ] },
            { "tag": "#BBB", "name": "Os Mito", "clanLevel": 20 },
            { "tag": "#CCC", "name": "Clã do Norte", "clanLevel": 19 },
            { "tag": "#DDD", "name": "Sertão War", "clanLevel": 15 }
          ],
          "rounds": [
            { "warTags": [ "#W1", "#W2" ] },
            { "warTags": [ "#W3", "#W4" ] },
            { "warTags": [ "#0", "#0" ] }
          ]
        }
    """.trimIndent()

    val wars = mapOf(
        "W1" to war("warEnded", side(OURS, "Picuí Raiz", 20, 80.0, true), side("#BBB", "Os Mito", 15, 70.0)),
        "W2" to war("warEnded", side("#CCC", "Clã do Norte", 18, 75.0), side("#DDD", "Sertão War", 18, 60.0)),
        "W3" to war("inWar", side("#BBB", "Os Mito", 5, 30.0), side(OURS, "Picuí Raiz", 8, 40.0, true)),
        "W4" to war("inWar", side("#CCC", "Clã do Norte", 3, 10.0), side("#DDD", "Sertão War", 4, 12.0)),
    )

    /** Guerra de CWL: sem `attacksPerMember`, como na API. */
    private fun war(state: String, clan: String, opponent: String) = """
        {
          "state": "$state", "teamSize": 15,
          "preparationStartTime": "20261101T120000.000Z", "startTime": "20261102T120000.000Z",
          "endTime": "20261103T120000.000Z", "warStartTime": "20261102T120000.000Z",
          "clan": $clan, "opponent": $opponent
        }
    """.trimIndent()

    private fun side(tag: String, name: String, stars: Int, destruction: Double, withMember: Boolean = false): String {
        val members = if (withMember) {
            """[ { "tag": "#${CocFixtures.PLAYER_TAG}", "name": "Dantas", "townhallLevel": 15, "mapPosition": 1 } ]"""
        } else {
            "[]"
        }
        return """
            { "tag": "$tag", "name": "$name", "clanLevel": 10, "attacks": 0, "stars": $stars,
              "destructionPercentage": $destruction, "members": $members }
        """.trimIndent()
    }
}
