package com.example.qrscannerapp

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

// [НОВОЕ] Enum для всех доступных тем фона
enum class BackgroundTheme(val key: String, val displayName: String, val emoji: String) {
    ENGINE("engine", "Энергия", "⚡"),
    NEBULA("nebula", "Туманность", "🌌"),
    VORONOI("voronoi", "Биосфера", "🧬");

    companion object {
        fun fromKey(key: String?): BackgroundTheme =
            entries.firstOrNull { it.key == key } ?: ENGINE
    }
}

class SettingsManager(private val context: Context) {

    companion object {
        val IS_SOUND_ENABLED       = booleanPreferencesKey("is_sound_enabled")
        val IS_VIBRATION_ENABLED   = booleanPreferencesKey("is_vibration_enabled")
        val IS_CATALOG_PRECACHED   = booleanPreferencesKey("is_catalog_precached")
        // [НОВОЕ]
        val BACKGROUND_THEME       = stringPreferencesKey("background_theme")
    }

    val isSoundEnabledFlow: Flow<Boolean> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[IS_SOUND_ENABLED] ?: true }

    val isVibrationEnabledFlow: Flow<Boolean> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[IS_VIBRATION_ENABLED] ?: true }

    // [НОВОЕ] Flow для темы — реактивный, AppBackground будет подписываться
    val backgroundThemeFlow: Flow<BackgroundTheme> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { BackgroundTheme.fromKey(it[BACKGROUND_THEME]) }

    suspend fun setSoundEnabled(isEnabled: Boolean) {
        context.dataStore.edit { it[IS_SOUND_ENABLED] = isEnabled }
    }

    suspend fun setVibrationEnabled(isEnabled: Boolean) {
        context.dataStore.edit { it[IS_VIBRATION_ENABLED] = isEnabled }
    }

    // [НОВОЕ]
    suspend fun setBackgroundTheme(theme: BackgroundTheme) {
        context.dataStore.edit { it[BACKGROUND_THEME] = theme.key }
    }

    suspend fun isCatalogPrecached(): Boolean =
        context.dataStore.data.map { it[IS_CATALOG_PRECACHED] ?: false }.first()

    suspend fun setCatalogPrecached(isPrecached: Boolean) {
        context.dataStore.edit { it[IS_CATALOG_PRECACHED] = isPrecached }
    }
}