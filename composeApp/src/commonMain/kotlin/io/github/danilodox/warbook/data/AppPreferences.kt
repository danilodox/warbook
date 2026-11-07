package io.github.danilodox.warbook.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import io.github.danilodox.warbook.war.SummaryOptions
import io.github.danilodox.warbook.war.SummaryPart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Preferências simples do app, no mesmo DataStore do token da instalação. */
interface AppPreferences {
    /** O usuário escolheu "Só explorar, sem conta" no onboarding. */
    val exploreMode: Flow<Boolean>

    /** Partes do resumo do WhatsApp; a mesma escolha vale para a guerra e a CWL. */
    val summaryOptions: Flow<SummaryOptions>

    /** Jogadores e clãs abertos pela busca, o mais recente primeiro (até [RecentSearches.LIMIT]). */
    val recentSearches: Flow<List<RecentSearch>>

    suspend fun setExploreMode(enabled: Boolean)

    /** Lê e grava numa só edição, para dois toques rápidos não se perderem. */
    suspend fun updateSummaryOptions(transform: (SummaryOptions) -> SummaryOptions)

    suspend fun addRecentSearch(item: RecentSearch)

    suspend fun clearRecentSearches()
}

class DataStoreAppPreferences(private val dataStore: DataStore<Preferences>) : AppPreferences {
    private val exploreKey = booleanPreferencesKey("explore_mode")
    private val summaryKey = stringSetPreferencesKey("summary_parts")
    private val recentKey = stringPreferencesKey("recent_searches")

    override val exploreMode: Flow<Boolean> = dataStore.data.map { it[exploreKey] ?: false }

    override val summaryOptions: Flow<SummaryOptions> = dataStore.data.map { summaryOptionsOf(it[summaryKey]) }

    override val recentSearches: Flow<List<RecentSearch>> = dataStore.data.map { RecentSearches.decode(it[recentKey]) }

    override suspend fun setExploreMode(enabled: Boolean) {
        dataStore.edit { it[exploreKey] = enabled }
    }

    override suspend fun updateSummaryOptions(transform: (SummaryOptions) -> SummaryOptions) {
        dataStore.edit { prefs ->
            prefs[summaryKey] = transform(summaryOptionsOf(prefs[summaryKey])).parts.mapTo(mutableSetOf()) { it.name }
        }
    }

    override suspend fun addRecentSearch(item: RecentSearch) {
        dataStore.edit { prefs ->
            prefs[recentKey] = RecentSearches.encode(RecentSearches.add(RecentSearches.decode(prefs[recentKey]), item))
        }
    }

    override suspend fun clearRecentSearches() {
        dataStore.edit { it.remove(recentKey) }
    }
}

/** Nada salvo = o padrão; um nome que não existe mais (de outra versão) é ignorado. */
internal fun summaryOptionsOf(names: Set<String>?): SummaryOptions = if (names == null) {
    SummaryOptions()
} else {
    SummaryOptions(SummaryPart.entries.filterTo(mutableSetOf()) { it.name in names })
}
