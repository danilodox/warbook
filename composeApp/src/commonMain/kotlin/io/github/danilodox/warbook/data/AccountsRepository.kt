package io.github.danilodox.warbook.data

import io.github.danilodox.warbook.api.PlayerDto
import io.github.danilodox.warbook.data.local.AccountDao
import io.github.danilodox.warbook.data.local.AccountEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Conta do usuário no app. A verificação é opcional e fica na prova assinada pelo servidor. */
data class Account(
    val tag: String,
    val name: String,
    val townHallLevel: Int,
    val clanName: String?,
    val isVerified: Boolean,
    val isActive: Boolean,
)

interface AccountsRepository {
    /** Na ordem em que foram adicionadas. */
    val accounts: Flow<List<Account>>

    /** Adiciona (ou atualiza) a conta do [player] e deixa ela ativa. */
    suspend fun add(player: PlayerDto)

    suspend fun activate(tag: String)

    /** Remove a conta e o perfil salvo dela; se era a ativa, outra passa a ser. */
    suspend fun remove(tag: String)

    /** Verifica com o token do jogo e guarda a prova. O token do jogo não é guardado. */
    suspend fun verify(tag: String, gameToken: String): ApiResult<Unit>
}

class RoomAccountsRepository(
    private val dao: AccountDao,
    private val profiles: ProfileRepository,
    private val api: WarbookRepository,
    private val clock: () -> Long = ::currentTimeMillis,
) : AccountsRepository {

    override val accounts: Flow<List<Account>> = dao.observeAll().map { list -> list.map { it.toAccount() } }

    override suspend fun add(player: PlayerDto) {
        dao.addAndActivate(
            AccountEntity(
                tag = player.tag,
                name = player.name,
                townHallLevel = player.townHallLevel,
                clanName = player.clan?.name,
                verificationToken = null,
                isActive = true,
                addedAt = clock(),
            ),
        )
        profiles.save(player)
    }

    override suspend fun activate(tag: String) = dao.setActive(tag)

    override suspend fun remove(tag: String) {
        dao.removeAndPickNext(tag)
        profiles.forget(tag)
    }

    override suspend fun verify(tag: String, gameToken: String): ApiResult<Unit> =
        when (val result = api.verify(tag, gameToken)) {
            is ApiResult.Success -> {
                dao.setVerificationToken(tag, result.value)
                ApiResult.Success(Unit)
            }
            is ApiResult.Failure -> result
        }
}

private fun AccountEntity.toAccount() = Account(
    tag = tag,
    name = name,
    townHallLevel = townHallLevel,
    clanName = clanName,
    isVerified = verificationToken != null,
    isActive = isActive,
)
