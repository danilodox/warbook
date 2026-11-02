package io.github.danilodox.warbook.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import okio.Path.Companion.toPath

/** Guarda o token da instalação. Não é um segredo forte: só identifica a instalação para o limite do servidor. */
interface InstallTokenStore {
    suspend fun get(): String?

    suspend fun save(token: String)
}

class DataStoreInstallTokenStore(private val dataStore: DataStore<Preferences>) : InstallTokenStore {
    private val key = stringPreferencesKey("install_token")

    override suspend fun get(): String? = dataStore.data.first()[key]

    override suspend fun save(token: String) {
        dataStore.edit { it[key] = token }
    }
}

const val DATA_STORE_FILE = "warbook.preferences_pb"

/** [path] vem de cada plataforma (pasta de arquivos do app). */
fun createDataStore(path: String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(produceFile = { path.toPath() })
