package com.ksupatcher.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsRepository(
    private val context: Context
) {
    private val versionUrlKey = stringPreferencesKey("version_json_url")

    val versionUrlFlow: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[versionUrlKey] ?: UpdateConfig.versionJsonUrl
    }

    suspend fun setVersionUrl(url: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[versionUrlKey] = url
        }
    }
}
