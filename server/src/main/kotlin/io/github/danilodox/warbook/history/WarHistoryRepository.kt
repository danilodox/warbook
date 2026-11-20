package io.github.danilodox.warbook.history

import io.github.danilodox.warbook.api.WarState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.batchUpsert
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Clãs monitorados e histórico de guerras no Postgres. Tudo é idempotente: o mesmo retrato gravado duas
 * vezes não duplica nada. Nada é apagado.
 */
// Uma função por operação do monitoramento; dividir espalharia a conversão das linhas.
@Suppress("TooManyFunctions")
class WarHistoryRepository(private val database: Database) {

    /** Resultado de [recordVerifiedVisit]: [newlyActivated] é `true` só na primeira ativação do clã. */
    data class Visit(val clan: MonitoredClan, val newlyActivated: Boolean)

    /**
     * Um membro verificado ativou, reativou ou só abriu o app: liga o clã se for a primeira vez e renova o
     * [MonitoredClan.lastSeenAt] (que é o que tira da pausa). Quem ativou primeiro fica registrado.
     */
    suspend fun recordVerifiedVisit(clanTag: String, clanName: String, playerTag: String, now: Instant): Visit = io {
        val existing = findClan(clanTag)
        if (existing == null) {
            MonitoredClansTable.insert {
                it[MonitoredClansTable.clanTag] = clanTag
                it[MonitoredClansTable.clanName] = clanName
                it[activatedBy] = playerTag
                it[activatedAt] = now.utc()
                it[lastSeenAt] = now.utc()
            }
        } else {
            MonitoredClansTable.update({ MonitoredClansTable.clanTag eq clanTag }) {
                it[MonitoredClansTable.clanName] = clanName
                it[lastSeenAt] = now.utc()
            }
        }
        Visit(requireNotNull(findClan(clanTag)), newlyActivated = existing == null)
    }

    suspend fun monitoredClan(clanTag: String): MonitoredClan? = io { findClan(clanTag) }

    /** Clãs que o agendador acompanha: os que não estão pausados em [now]. */
    suspend fun activeClans(now: Instant): List<MonitoredClan> = io {
        val since = now.minusSeconds(MonitoredClan.PAUSE_AFTER.inWholeSeconds).utc()
        MonitoredClansTable.selectAll()
            .where { MonitoredClansTable.lastSeenAt greater since }
            .orderBy(MonitoredClansTable.clanTag)
            .map { it.toMonitoredClan() }
    }

    suspend fun markWarlogImported(clanTag: String, now: Instant) = io {
        MonitoredClansTable.update({ MonitoredClansTable.clanTag eq clanTag }) {
            it[warlogImportedAt] = now.utc()
        }
    }

    /**
     * Grava um retrato completo (guerra + escalação + ataques) e devolve o id. Um retrato de guerra
     * encerrada nunca é trocado por um mais antigo (de antes do fim); ataques e membros só se somam.
     */
    suspend fun saveSnapshot(clanTag: String, war: WarRecord): Long = io {
        require(war.source == WarSource.SNAPSHOT) { "saveSnapshot só grava retratos" }
        val existing = findWarRow(clanTag, war.endTime)
        val warId = when {
            existing == null -> insertWar(clanTag, war)
            existing.isEndedSnapshot() && war.state != WarState.WAR_ENDED -> existing[WarsTable.id]
            else -> existing[WarsTable.id].also { id ->
                WarsTable.update({ WarsTable.id eq id }) { it.fill(clanTag, war) }
            }
        }
        if (existing == null || !existing.isEndedSnapshot() || war.state == WarState.WAR_ENDED) {
            saveMembersAndAttacks(warId, war)
        }
        warId
    }

    /**
     * Grava o resultado de guerras do warlog. Não mexe em retrato de guerra encerrada (que já tem tudo);
     * num retrato de antes do fim, só troca o placar e o resultado, que no warlog já são os finais.
     */
    suspend fun saveWarlog(clanTag: String, wars: List<WarRecord>) = io {
        wars.forEach { war ->
            require(war.source == WarSource.WARLOG) { "saveWarlog só grava o warlog" }
            val existing = findWarRow(clanTag, war.endTime)
            when {
                existing == null -> insertWar(clanTag, war)
                existing[WarsTable.dataSource] == WarSource.WARLOG.name ->
                    WarsTable.update({ WarsTable.id eq existing[WarsTable.id] }) { it.fill(clanTag, war) }
                !existing.isEndedSnapshot() ->
                    WarsTable.update({ WarsTable.id eq existing[WarsTable.id] }) { it.fillScore(war) }
            }
        }
    }

    /** Guerras do clã, da mais recente para a mais antiga, sem membros nem ataques. */
    suspend fun recentWars(clanTag: String, limit: Int): List<WarRecord> = io {
        WarsTable.selectAll()
            .where { WarsTable.clanTag eq clanTag }
            .orderBy(WarsTable.endTime, SortOrder.DESC)
            .limit(limit)
            .map { it.toWarRecord() }
    }

    /** Uma guerra com a escalação e os ataques (em ordem), ou `null` se não for desse clã. */
    suspend fun war(clanTag: String, warId: Long): WarRecord? = io {
        val row = WarsTable.selectAll()
            .where { (WarsTable.id eq warId) and (WarsTable.clanTag eq clanTag) }
            .singleOrNull() ?: return@io null
        row.toWarRecord(withDetails = true)
    }

    /** warTags da CWL que já têm o retrato final: o agendador não precisa buscar de novo. */
    suspend fun endedWarTags(clanTag: String): Set<String> = io {
        WarsTable.select(WarsTable.warTag)
            .where {
                (WarsTable.clanTag eq clanTag) and
                    (WarsTable.dataSource eq WarSource.SNAPSHOT.name) and
                    (WarsTable.state eq WarState.WAR_ENDED.name)
            }
            .mapNotNull { it[WarsTable.warTag] }
            .toSet()
    }

    private suspend fun <T> io(block: JdbcTransaction.() -> T): T =
        withContext(Dispatchers.IO) { transaction(database) { block() } }

    private fun findClan(clanTag: String): MonitoredClan? = MonitoredClansTable.selectAll()
        .where { MonitoredClansTable.clanTag eq clanTag }
        .singleOrNull()
        ?.toMonitoredClan()

    private fun findWarRow(clanTag: String, endTime: Instant): ResultRow? = WarsTable.selectAll()
        .where { (WarsTable.clanTag eq clanTag) and (WarsTable.endTime eq endTime.utc()) }
        .singleOrNull()

    private fun ResultRow.isEndedSnapshot() =
        this[WarsTable.dataSource] == WarSource.SNAPSHOT.name && this[WarsTable.state] == WarState.WAR_ENDED.name

    private fun insertWar(clanTag: String, war: WarRecord): Long =
        WarsTable.insert { it.fill(clanTag, war) }[WarsTable.id]

    private fun UpdateBuilder<*>.fill(clanTag: String, war: WarRecord) {
        this[WarsTable.clanTag] = clanTag
        this[WarsTable.warType] = war.type.name
        this[WarsTable.dataSource] = war.source.name
        this[WarsTable.state] = war.state.name
        this[WarsTable.warTag] = war.warTag
        this[WarsTable.cwlSeason] = war.cwlSeason
        this[WarsTable.teamSize] = war.teamSize.toShort()
        this[WarsTable.attacksPerMember] = war.attacksPerMember.toShort()
        this[WarsTable.preparationStartTime] = war.preparationStartTime?.utc()
        this[WarsTable.startTime] = war.startTime?.utc()
        this[WarsTable.endTime] = war.endTime.utc()
        this[WarsTable.clanName] = war.clan.name.orEmpty()
        this[WarsTable.opponentTag] = war.opponent.tag
        this[WarsTable.opponentName] = war.opponent.name
        this[WarsTable.opponentBadgeUrl] = war.opponent.badgeUrl
        this[WarsTable.capturedAt] = war.capturedAt.utc()
        fillScore(war)
    }

    private fun UpdateBuilder<*>.fillScore(war: WarRecord) {
        this[WarsTable.clanStars] = war.clan.stars.toShort()
        this[WarsTable.clanDestruction] = war.clan.destruction
        this[WarsTable.clanAttacks] = war.clan.attacks?.toShort()
        this[WarsTable.opponentStars] = war.opponent.stars.toShort()
        this[WarsTable.opponentDestruction] = war.opponent.destruction
        this[WarsTable.opponentAttacks] = war.opponent.attacks?.toShort()
        this[WarsTable.result] = war.result?.name
    }

    private fun saveMembersAndAttacks(warId: Long, war: WarRecord) {
        // O upsert em lote do Postgres falha se a mesma chave vier duas vezes no mesmo comando.
        WarMembersTable.batchUpsert(war.members.distinctBy { it.tag }, shouldReturnGeneratedValues = false) { m ->
            this[WarMembersTable.warId] = warId
            this[WarMembersTable.tag] = m.tag
            this[WarMembersTable.isOpponent] = m.isOpponent
            this[WarMembersTable.name] = m.name
            this[WarMembersTable.townHall] = m.townHall.toShort()
            this[WarMembersTable.mapPosition] = m.mapPosition.toShort()
        }
        WarAttacksTable.batchUpsert(war.attacks.distinctBy { it.order }, shouldReturnGeneratedValues = false) { a ->
            this[WarAttacksTable.warId] = warId
            this[WarAttacksTable.order] = a.order.toShort()
            this[WarAttacksTable.attackerTag] = a.attackerTag
            this[WarAttacksTable.defenderTag] = a.defenderTag
            this[WarAttacksTable.stars] = a.stars.toShort()
            this[WarAttacksTable.destruction] = a.destruction.toShort()
            this[WarAttacksTable.duration] = a.duration?.toShort()
        }
    }

    private fun ResultRow.toMonitoredClan() = MonitoredClan(
        clanTag = this[MonitoredClansTable.clanTag],
        clanName = this[MonitoredClansTable.clanName],
        activatedBy = this[MonitoredClansTable.activatedBy],
        activatedAt = this[MonitoredClansTable.activatedAt].toInstant(),
        lastSeenAt = this[MonitoredClansTable.lastSeenAt].toInstant(),
        warlogImportedAt = this[MonitoredClansTable.warlogImportedAt]?.toInstant(),
    )

    private fun ResultRow.toWarRecord(withDetails: Boolean = false): WarRecord {
        val id = this[WarsTable.id]
        return WarRecord(
            id = id,
            type = WarType.valueOf(this[WarsTable.warType]),
            source = WarSource.valueOf(this[WarsTable.dataSource]),
            state = WarState.valueOf(this[WarsTable.state]),
            warTag = this[WarsTable.warTag],
            cwlSeason = this[WarsTable.cwlSeason],
            teamSize = this[WarsTable.teamSize].toInt(),
            attacksPerMember = this[WarsTable.attacksPerMember].toInt(),
            preparationStartTime = this[WarsTable.preparationStartTime]?.toInstant(),
            startTime = this[WarsTable.startTime]?.toInstant(),
            endTime = this[WarsTable.endTime].toInstant(),
            clan = WarSide(
                tag = this[WarsTable.clanTag],
                name = this[WarsTable.clanName],
                stars = this[WarsTable.clanStars].toInt(),
                destruction = this[WarsTable.clanDestruction],
                attacks = this[WarsTable.clanAttacks]?.toInt(),
            ),
            opponent = WarSide(
                tag = this[WarsTable.opponentTag],
                name = this[WarsTable.opponentName],
                badgeUrl = this[WarsTable.opponentBadgeUrl],
                stars = this[WarsTable.opponentStars].toInt(),
                destruction = this[WarsTable.opponentDestruction],
                attacks = this[WarsTable.opponentAttacks]?.toInt(),
            ),
            result = this[WarsTable.result]?.let(WarResult::valueOf),
            capturedAt = this[WarsTable.capturedAt].toInstant(),
            members = if (withDetails) members(id) else emptyList(),
            attacks = if (withDetails) attacks(id) else emptyList(),
        )
    }

    private fun members(warId: Long) = WarMembersTable.selectAll()
        .where { WarMembersTable.warId eq warId }
        .orderBy(WarMembersTable.isOpponent to SortOrder.ASC, WarMembersTable.mapPosition to SortOrder.ASC)
        .map {
            WarRecordMember(
                tag = it[WarMembersTable.tag],
                isOpponent = it[WarMembersTable.isOpponent],
                name = it[WarMembersTable.name],
                townHall = it[WarMembersTable.townHall].toInt(),
                mapPosition = it[WarMembersTable.mapPosition].toInt(),
            )
        }

    private fun attacks(warId: Long) = WarAttacksTable.selectAll()
        .where { WarAttacksTable.warId eq warId }
        .orderBy(WarAttacksTable.order)
        .map {
            WarRecordAttack(
                order = it[WarAttacksTable.order].toInt(),
                attackerTag = it[WarAttacksTable.attackerTag],
                defenderTag = it[WarAttacksTable.defenderTag],
                stars = it[WarAttacksTable.stars].toInt(),
                destruction = it[WarAttacksTable.destruction].toInt(),
                duration = it[WarAttacksTable.duration]?.toInt(),
            )
        }

    private fun Instant.utc(): OffsetDateTime = atOffset(ZoneOffset.UTC)
}
