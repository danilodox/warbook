package io.github.danilodox.warbook.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Preferências simples do app, no mesmo DataStore do token da instalação. */
interface AppPreferences {
    /** O usuário escolheu "Só explorar, sem conta" no onboarding. */
    val exploreMode: Flow<Boolean>

    suspend fun setExploreMode(enabled: Boolean)
}

class DataStoreAppPreferences(private val dataStore: DataStore<Preferences>) : AppPreferences {
    private val exploreKey = booleanPreferencesKey("explore_mode")

    override val exploreMode: Flow<Boolean> = dataStore.data.map { it[exploreKey] ?: false }

    override suspend fun setExploreMode(enabled: Boolean) {
        dataStore.edit { it[exploreKey] = enabled }
    }
}
