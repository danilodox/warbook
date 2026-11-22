package io.github.danilodox.warbook.ui.units

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import io.github.danilodox.warbook.resources.Res
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Níveis por CV (`files/unit_levels.json`, gerado por `tools/gerar_niveis_cv.py` a partir do clashy.py).
 * A API só dá o máximo do jogo; com esta tabela sai o máximo que o CV do jogador permite.
 */
@Serializable
data class UnitLevelTable(
    /** Nome da API → CV exigido por nível (índice 0 = nível 1), sem descer. */
    val units: Map<String, List<Int>> = emptyMap(),
    /** Equipamento → herói dono (nome da API). */
    val equipmentHero: Map<String, String> = emptyMap(),
) {
    /** Maior nível que o CV [townHall] permite; `null` se a unidade não está na tabela. */
    fun maxForTownHall(name: String, townHall: Int): Int? = units[name]?.count { it <= townHall }

    fun heroOf(equipment: String): String? = equipmentHero[equipment]

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun parse(text: String): UnitLevelTable = json.decodeFromString(serializer(), text)
    }
}

/** Lê a tabela uma vez só; até lá (ou se falhar) a tela usa o máximo do jogo. */
object UnitLevels {
    private val mutex = Mutex()
    private var cached: UnitLevelTable? = null

    suspend fun load(): UnitLevelTable = mutex.withLock {
        cached ?: runCatching { UnitLevelTable.parse(Res.readBytes(FILE).decodeToString()) }
            .getOrDefault(UnitLevelTable())
            .also { cached = it }
    }

    private const val FILE = "files/unit_levels.json"
}

@Composable
fun rememberUnitLevels(): State<UnitLevelTable?> = produceState<UnitLevelTable?>(null) { value = UnitLevels.load() }
