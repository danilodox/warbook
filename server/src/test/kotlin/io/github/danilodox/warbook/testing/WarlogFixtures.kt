package io.github.danilodox.warbook.testing

/** Warlog no formato da API do jogo: vitória, entrada da CWL (sem adversário), derrota e uma sem `result`. */
object WarlogFixtures {
    const val PATH = "/v1/clans/%23${CocFixtures.CLAN_TAG}/warlog"

    val warLog = """
        {
          "items": [
            { "result": "win", "endTime": "20261118T200000.000Z", "teamSize": 15, "attacksPerMember": 2,
              "clan": { "tag": "#${CocFixtures.CLAN_TAG}", "name": "Picuí Raiz", "clanLevel": 18,
                        "attacks": 28, "stars": 40, "destructionPercentage": 91.5, "expEarned": 300 },
              "opponent": { "tag": "#OPP1", "name": "Os Mito", "badgeUrls": { "medium": "https://img/o1.png" },
                            "clanLevel": 17, "stars": 33, "destructionPercentage": 80.2 } },
            { "result": null, "endTime": "20261110T200000.000Z", "teamSize": 15, "attacksPerMember": 1,
              "clan": { "tag": "#${CocFixtures.CLAN_TAG}", "name": "Picuí Raiz", "clanLevel": 18,
                        "attacks": 98, "stars": 240, "destructionPercentage": 0 },
              "opponent": { "badgeUrls": {}, "clanLevel": 0, "stars": 0, "destructionPercentage": 0 } },
            { "result": "lose", "endTime": "20261105T200000.000Z", "teamSize": 10, "attacksPerMember": 2,
              "clan": { "tag": "#${CocFixtures.CLAN_TAG}", "name": "Picuí Raiz", "clanLevel": 18,
                        "attacks": 18, "stars": 22, "destructionPercentage": 70.0 },
              "opponent": { "tag": "#OPP2", "name": "Base", "clanLevel": 20, "stars": 27,
                            "destructionPercentage": 88.0 } },
            { "endTime": "20261101T200000.000Z", "teamSize": 5, "attacksPerMember": 2,
              "clan": { "tag": "#${CocFixtures.CLAN_TAG}", "name": "Picuí Raiz", "clanLevel": 18,
                        "attacks": 10, "stars": 12, "destructionPercentage": 77.0 },
              "opponent": { "tag": "#OPP3", "name": "Empate", "clanLevel": 9, "stars": 12,
                            "destructionPercentage": 77.0 } }
          ],
          "paging": { "cursors": {} }
        }
    """.trimIndent()
}
