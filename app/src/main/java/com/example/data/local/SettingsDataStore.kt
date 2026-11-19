package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "fiction_hike_settings")

data class UserSettings(
    val darkModeEnabled: Boolean = true,
    val offlineModeOnly: Boolean = false,
    val preferredScaleKm: Double = 3.0
)

class SettingsDataStore(private val context: Context) {
    private object Keys {
        val DARK_MODE = booleanPreferencesKey("dark_mode_enabled")
        val OFFLINE_MODE = booleanPreferencesKey("offline_mode_only")
        val SCALE_KM = doublePreferencesKey("preferred_scale_km")
    }

    val settingsFlow: Flow<UserSettings> = context.settingsDataStore.data.map { prefs ->
        UserSettings(
            darkModeEnabled = prefs[Keys.DARK_MODE] ?: true,
            offlineModeOnly = prefs[Keys.OFFLINE_MODE] ?: false,
            preferredScaleKm = prefs[Keys.SCALE_KM] ?: 3.0
        )
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.DARK_MODE] = enabled }
    }

    suspend fun setOfflineMode(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.OFFLINE_MODE] = enabled }
    }

    suspend fun setPreferredScaleKm(scaleKm: Double) {
        context.settingsDataStore.edit { it[Keys.SCALE_KM] = scaleKm }
    }
}
