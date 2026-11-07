package io.github.danilodox.warbook.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer

/** Jogador ou clã aberto pela busca (B1, "Recentes"). [level] = CV do jogador ou nível do clã. */
@Serializable
data class RecentSearch(
    val kind: FollowKind,
    val tag: String,
    val name: String,
    val level: Int? = null,
    val imageUrl: String? = null,
)

internal object RecentSearches {
    const val LIMIT = 10
    private val serializer = ListSerializer(RecentSearch.serializer())

    /** O novo vai para o topo; o mesmo jogador ou clã não se repete. */
    fun add(list: List<RecentSearch>, item: RecentSearch): List<RecentSearch> =
        (listOf(item) + list.filterNot { it.kind == item.kind && it.tag == item.tag }).take(LIMIT)

    fun encode(list: List<RecentSearch>): String = warbookJson.encodeToString(serializer, list)

    /** Nada salvo ou formato de outra versão = lista vazia. */
    fun decode(json: String?): List<RecentSearch> = if (json == null) {
        emptyList()
    } else {
        try {
            warbookJson.decodeFromString(serializer, json)
        } catch (_: SerializationException) {
            emptyList()
        } catch (_: IllegalArgumentException) {
            emptyList()
        }
    }
}
