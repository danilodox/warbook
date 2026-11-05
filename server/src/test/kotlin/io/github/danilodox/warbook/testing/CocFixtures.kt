package io.github.danilodox.warbook.testing

/** Respostas de exemplo no formato da API oficial (com campos extras que o servidor ignora). */
object CocFixtures {
    const val PLAYER_TAG = "2PQ8LV9YC"
    const val CLAN_TAG = "2YQUC8J0P"

    const val PLAYER_PATH = "/v1/players/%23$PLAYER_TAG"
    const val CLAN_PATH = "/v1/clans/%23$CLAN_TAG"
    const val WAR_PATH = "/v1/clans/%23$CLAN_TAG/currentwar"

    val player = """
        {
          "tag": "#$PLAYER_TAG", "name": "Dantas", "townHallLevel": 15, "expLevel": 212,
          "trophies": 4812, "bestTrophies": 5000, "warStars": 1384, "attackWins": 47,
          "donations": 1240, "donationsReceived": 860, "role": "coLeader",
          "clan": {
            "tag": "#$CLAN_TAG", "name": "Picuí Raiz", "clanLevel": 18,
            "badgeUrls": { "small": "https://img/s.png", "medium": "https://img/m.png", "large": "https://img/l.png" }
          },
          "league": { "id": 29000022, "name": "Legend League" },
          "heroes": [
            { "name": "Barbarian King", "level": 90, "maxLevel": 100, "village": "home" },
            { "name": "Battle Machine", "level": 30, "maxLevel": 35, "village": "builderBase" }
          ],
          "troops": [
            { "name": "Barbarian", "level": 11, "maxLevel": 12, "village": "home" },
            { "name": "Super Barbarian", "level": 11, "maxLevel": 12, "village": "home", "superTroopIsActive": true },
            { "name": "Sneaky Goblin", "level": 8, "maxLevel": 9, "village": "home" },
            { "name": "Raged Barbarian", "level": 18, "maxLevel": 20, "village": "builderBase" },
            { "name": "Wall Wrecker", "level": 4, "maxLevel": 5, "village": "home" },
            { "name": "L.A.S.S.I", "level": 10, "maxLevel": 15, "village": "home" }
          ],
          "spells": [ { "name": "Lightning Spell", "level": 10, "maxLevel": 11, "village": "home" } ],
          "heroEquipment": [ { "name": "Earthquake Boots", "level": 15, "maxLevel": 18, "village": "home" } ]
        }
    """.trimIndent()

    const val VERIFY_PATH = "/v1/players/%23$PLAYER_TAG/verifytoken"

    /** Montado com repeat para não parecer segredo real ao Gitleaks. */
    val gameToken = "tk".repeat(4)

    // A API repete a tag e o token na resposta.
    val verifyOk = """{ "tag": "#$PLAYER_TAG", "token": "$gameToken", "status": "ok" }"""
    val verifyInvalid = """{ "tag": "#$PLAYER_TAG", "token": "$gameToken", "status": "invalid" }"""

    val clan = """
        {
          "tag": "#$CLAN_TAG", "name": "Picuí Raiz", "type": "inviteOnly",
          "description": "Clã brasileiro, guerra sempre.",
          "location": { "id": 32000038, "name": "Brazil" },
          "badgeUrls": { "medium": "https://img/m.png" },
          "clanLevel": 18, "members": 2, "warFrequency": "always", "warWinStreak": 7, "warWins": 312,
          "isWarLogPublic": true, "requiredTrophies": 3000, "requiredTownhallLevel": 12,
          "warLeague": { "id": 48000015, "name": "Crystal League I" },
          "capitalLeague": { "id": 85000015, "name": "Master League II" },
          "memberList": [
            { "tag": "#ABC", "name": "Maré Alta", "role": "leader", "townHallLevel": 16, "trophies": 5100,
              "clanRank": 1, "donations": 3412, "donationsReceived": 980 },
            { "tag": "#$PLAYER_TAG", "name": "Dantas", "role": "coLeader", "townHallLevel": 15, "trophies": 4812,
              "clanRank": 2, "donations": 1240, "donationsReceived": 860 }
          ]
        }
    """.trimIndent()

    val warInWar = """
        {
          "state": "inWar", "teamSize": 15, "attacksPerMember": 2,
          "preparationStartTime": "20261101T120000.000Z",
          "startTime": "20261102T120000.000Z",
          "endTime": "20261103T120000.000Z",
          "clan": {
            "tag": "#$CLAN_TAG", "name": "Picuí Raiz", "clanLevel": 18, "attacks": 24, "stars": 31,
            "destructionPercentage": 78.4,
            "members": [
              { "tag": "#$PLAYER_TAG", "name": "Dantas", "townhallLevel": 15, "mapPosition": 2,
                "attacks": [ { "attackerTag": "#$PLAYER_TAG", "defenderTag": "#DEF", "stars": 3,
                               "destructionPercentage": 100, "order": 5, "duration": 120 } ] },
              { "tag": "#ABC", "name": "Maré Alta", "townhallLevel": 16, "mapPosition": 1 }
            ]
          },
          "opponent": {
            "tag": "#OPP", "name": "Os Mito", "clanLevel": 20, "attacks": 27, "stars": 28,
            "destructionPercentage": 71.0, "members": []
          }
        }
    """.trimIndent()

    const val WAR_NOT_IN_WAR = """{ "state": "notInWar", "clan": { "badgeUrls": {} }, "opponent": {} }"""
    const val ERROR_ACCESS_DENIED = """{ "reason": "accessDenied", "message": "Access denied" }"""
    const val ERROR_INVALID_IP = """{ "reason": "accessDenied.invalidIp", "message": "Invalid authorization" }"""
    const val ERROR_MAINTENANCE = """{ "reason": "inMaintenance", "message": "Maintenance" }"""
    const val ERROR_THROTTLED = """{ "reason": "requestThrottled", "message": "Throttled" }"""
}
