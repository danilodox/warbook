package io.github.danilodox.warbook.data.remote.interceptor

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class TokenProvider(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val PREF_KEY_ACCESS_TOKEN = "access_token" // apenas o nome da chave
        private const val KEY_PLAYER_TAG = "player_tag"
    }

    fun saveToken(token: String) {
        prefs.edit { putString(PREF_KEY_ACCESS_TOKEN, token) }
    }

    fun getToken(): String? {
        return prefs.getString(PREF_KEY_ACCESS_TOKEN, null)
    }

    fun saveTag(tag: String) {
        prefs.edit { putString(KEY_PLAYER_TAG, tag) }
    }

    fun getSavedTag(): String? = prefs.getString(KEY_PLAYER_TAG, null)
}