package io.github.danilodox.warbook.data.datastore

import android.content.Context
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey


class SecureTokenProvider(context: Context) {

    // Define keys in a companion object for safety and reusability, just like you did before.
    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_PLAYER_TAG = "player_tag"
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "secure_auth_prefs", // The name of the encrypted file
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // Your existing functions, now using the constant key
    fun saveToken(token: String) {
        encryptedPrefs.edit { putString(KEY_ACCESS_TOKEN, token) }
    }

    fun getToken(): String? = encryptedPrefs.getString(KEY_ACCESS_TOKEN, null)


    /**
     * Saves the player tag to the encrypted file.
     */
    fun saveTag(tag: String) {
        // It works the same way, just call 'edit' on your encryptedPrefs variable.
        encryptedPrefs.edit { putString(KEY_PLAYER_TAG, tag) }
    }

    /**
     * Retrieves the player tag from the encrypted file.
     */
    fun getSavedTag(): String? = encryptedPrefs.getString(KEY_PLAYER_TAG, null)

    fun saveCustomString(key: String, value: String) {
        encryptedPrefs.edit { putString(key, value) }
    }

    fun getCustomString(key: String): String? {
        return encryptedPrefs.getString(key, null)
    }
}
