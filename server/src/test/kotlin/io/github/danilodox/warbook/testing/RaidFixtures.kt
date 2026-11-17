package io.github.danilodox.warbook.testing

/** Respostas da API do jogo para `GET /clans/{tag}/capitalraidseasons`, no formato da Supercell. */
object RaidFixtures {
    const val RAID_PATH = "/v1/clans/%23${CocFixtures.CLAN_TAG}/capitalraidseasons"

    /**
     * Dois fins de semana. No mais recente (em andamento) só o `#ABC` atacou, com o bônus; o `#2PQ8LV9YC`
     * (membro do clã em [CocFixtures.clan]) não aparece, então não atacou.
     */
    val seasons = """
        {
          "items": [
            {
              "state": "ongoing", "startTime": "20261113T070000.000Z", "endTime": "20261116T070000.000Z",
              "capitalTotalLoot": 52000, "raidsCompleted": 1, "totalAttacks": 9, "enemyDistrictsDestroyed": 7,
              "offensiveReward": 0, "defensiveReward": 0,
              "members": [
                { "tag": "#LOW", "name": "Fraco", "attacks": 3, "attackLimit": 5, "bonusAttackLimit": 0,
                  "capitalResourcesLooted": 9000 },
                { "tag": "#ABC", "name": "Maré Alta", "attacks": 6, "attackLimit": 5, "bonusAttackLimit": 1,
                  "capitalResourcesLooted": 43000 }
              ],
              "attackLog": [
                { "defender": { "tag": "#DEF1", "name": "Alvo", "level": 9,
                                "badgeUrls": { "small": "https://img/s.png", "medium": "https://img/m.png" } },
                  "attackCount": 9, "districtCount": 8, "districtsDestroyed": 7 }
              ],
              "defenseLog": [
                { "attacker": { "tag": "#ATK1", "name": "Invasor", "level": 12, "badgeUrls": {} },
                  "attackCount": 20, "districtCount": 9, "districtsDestroyed": 9 }
              ]
            },
            {
              "state": "ended", "startTime": "20261106T070000.000Z", "endTime": "20261109T070000.000Z",
              "capitalTotalLoot": 185110, "raidsCompleted": 2, "totalAttacks": 65, "enemyDistrictsDestroyed": 23,
              "offensiveReward": 148, "defensiveReward": 176,
              "members": [
                { "tag": "#ABC", "name": "Maré Alta", "attacks": 6, "attackLimit": 5, "bonusAttackLimit": 1,
                  "capitalResourcesLooted": 21019 }
              ],
              "attackLog": [], "defenseLog": []
            }
          ]
        }
    """.trimIndent()

    const val EMPTY = """{ "items": [] }"""
}
