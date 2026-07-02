package com.watchocr.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "watchocr_settings")

data class AppSettings(
    val directoryUri: String? = null,
    val apiKey: String = "",
    val model: String = "gemini-3.1-flash-lite",
    /** True once the monitor has baselined the current directory's pre-existing files. */
    val baselineDone: Boolean = false
)

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val DIRECTORY_URI = stringPreferencesKey("directory_uri")
        val API_KEY = stringPreferencesKey("api_key")
        val MODEL = stringPreferencesKey("model")
        val BASELINE_DONE = booleanPreferencesKey("baseline_done")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            directoryUri = prefs[Keys.DIRECTORY_URI],
            apiKey = prefs[Keys.API_KEY] ?: "",
            model = prefs[Keys.MODEL] ?: "gemini-3.1-flash-lite",
            baselineDone = prefs[Keys.BASELINE_DONE] ?: false
        )
    }

    suspend fun setDirectoryUri(uri: String) {
        context.dataStore.edit {
            it[Keys.DIRECTORY_URI] = uri
            // A (possibly) different directory needs a fresh baseline pass.
            it[Keys.BASELINE_DONE] = false
        }
    }

    suspend fun setBaselineDone(done: Boolean) {
        context.dataStore.edit { it[Keys.BASELINE_DONE] = done }
    }

    suspend fun setApiKey(key: String) {
        context.dataStore.edit { it[Keys.API_KEY] = key }
    }

    suspend fun setModel(model: String) {
        context.dataStore.edit { it[Keys.MODEL] = model }
    }
}
