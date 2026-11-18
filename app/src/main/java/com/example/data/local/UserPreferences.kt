package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "deiplay_prefs")

enum class ThemeMode {
    SYSTEM, DARK, LIGHT
}

data class UserPrefsState(
    val currentUserId: String? = null,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val autoPlayNext: Boolean = true,
    val preferredLanguage: String = "Todos",
    val preferredCountry: String = "Global",
    val youtubeApiKey: String = "",
    val youtubeOAuthClientId: String = "",
    val youtubeAccessToken: String = ""
)

class UserPreferencesRepository(private val context: Context) {
    private object Keys {
        val CURRENT_USER_ID = stringPreferencesKey("current_user_id")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val AUTO_PLAY_NEXT = booleanPreferencesKey("auto_play_next")
        val PREFERRED_LANGUAGE = stringPreferencesKey("preferred_language")
        val PREFERRED_COUNTRY = stringPreferencesKey("preferred_country")
        val YOUTUBE_API_KEY = stringPreferencesKey("youtube_api_key")
        val YOUTUBE_OAUTH_CLIENT_ID = stringPreferencesKey("youtube_oauth_client_id")
        val YOUTUBE_ACCESS_TOKEN = stringPreferencesKey("youtube_access_token")
    }

    val prefsFlow: Flow<UserPrefsState> = context.dataStore.data.map { prefs ->
        val themeStr = prefs[Keys.THEME_MODE] ?: ThemeMode.DARK.name
        val theme = ThemeMode.entries.firstOrNull { it.name == themeStr } ?: ThemeMode.DARK
        UserPrefsState(
            currentUserId = prefs[Keys.CURRENT_USER_ID],
            themeMode = theme,
            autoPlayNext = prefs[Keys.AUTO_PLAY_NEXT] ?: true,
            preferredLanguage = prefs[Keys.PREFERRED_LANGUAGE] ?: "Todos",
            preferredCountry = prefs[Keys.PREFERRED_COUNTRY] ?: "Global",
            youtubeApiKey = prefs[Keys.YOUTUBE_API_KEY] ?: "",
            youtubeOAuthClientId = prefs[Keys.YOUTUBE_OAUTH_CLIENT_ID] ?: "",
            youtubeAccessToken = prefs[Keys.YOUTUBE_ACCESS_TOKEN] ?: ""
        )
    }

    suspend fun setCurrentUserId(userId: String?) {
        context.dataStore.edit { prefs ->
            if (userId == null) prefs.remove(Keys.CURRENT_USER_ID)
            else prefs[Keys.CURRENT_USER_ID] = userId
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setAutoPlayNext(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_PLAY_NEXT] = enabled }
    }

    suspend fun setDiscoveryFilters(language: String, country: String) {
        context.dataStore.edit {
            it[Keys.PREFERRED_LANGUAGE] = language
            it[Keys.PREFERRED_COUNTRY] = country
        }
    }

    suspend fun saveYouTubeCredentials(apiKey: String, clientId: String, accessToken: String) {
        context.dataStore.edit {
            it[Keys.YOUTUBE_API_KEY] = apiKey.trim()
            it[Keys.YOUTUBE_OAUTH_CLIENT_ID] = clientId.trim()
            it[Keys.YOUTUBE_ACCESS_TOKEN] = accessToken.trim()
        }
    }

    suspend fun clearYouTubeToken() {
        context.dataStore.edit {
            it.remove(Keys.YOUTUBE_ACCESS_TOKEN)
        }
    }
}
