package io.github.danilodox.warbook.history

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

// Tabelas criadas pela migration `V2__monitoramento_de_guerras.sql`.

internal object MonitoredClansTable : Table("monitored_clans") {
    val clanTag = text("clan_tag")
    val clanName = text("clan_name")
    val activatedBy = text("activated_by")
    val activatedAt = timestampWithTimeZone("activated_at")
    val lastSeenAt = timestampWithTimeZone("last_seen_at")
    val warlogImportedAt = timestampWithTimeZone("warlog_imported_at").nullable()
    override val primaryKey = PrimaryKey(clanTag)
}

internal object WarsTable : Table("wars") {
    val id = long("id").autoIncrement()
    val clanTag = text("clan_tag")
    val warType = text("war_type")
    val dataSource = text("source")
    val state = text("state")
    val warTag = text("war_tag").nullable()
    val cwlSeason = text("cwl_season").nullable()
    val teamSize = short("team_size")
    val attacksPerMember = short("attacks_per_member")
    val preparationStartTime = timestampWithTimeZone("preparation_start_time").nullable()
    val startTime = timestampWithTimeZone("start_time").nullable()
    val endTime = timestampWithTimeZone("end_time")
    val clanName = text("clan_name")
    val clanStars = short("clan_stars")
    val clanDestruction = double("clan_destruction")
    val clanAttacks = short("clan_attacks").nullable()
    val opponentTag = text("opponent_tag").nullable()
    val opponentName = text("opponent_name").nullable()
    val opponentBadgeUrl = text("opponent_badge_url").nullable()
    val opponentStars = short("opponent_stars")
    val opponentDestruction = double("opponent_destruction")
    val opponentAttacks = short("opponent_attacks").nullable()
    val result = text("result").nullable()
    val capturedAt = timestampWithTimeZone("captured_at")
    override val primaryKey = PrimaryKey(id)
}

internal object WarMembersTable : Table("war_members") {
    val warId = long("war_id")
    val tag = text("tag")
    val isOpponent = bool("is_opponent")
    val name = text("name")
    val townHall = short("town_hall")
    val mapPosition = short("map_position")
    override val primaryKey = PrimaryKey(warId, tag)
}

internal object WarAttacksTable : Table("war_attacks") {
    val warId = long("war_id")
    val order = short("attack_order")
    val attackerTag = text("attacker_tag")
    val defenderTag = text("defender_tag")
    val stars = short("stars")
    val destruction = short("destruction")
    val duration = short("duration").nullable()
    override val primaryKey = PrimaryKey(warId, order)
}
