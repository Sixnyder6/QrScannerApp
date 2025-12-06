// Полное содержимое для ОБНОВЛЕННОГО файла SettingsManager.kt

package com.example.qrscannerapp
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first // <-- НОВЫЙ ИМПОРТ
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {
    companion object {
        val IS_SOUND_ENABLED = booleanPreferencesKey("is_sound_enabled")
        val IS_VIBRATION_ENABLED = booleanPreferencesKey("is_vibration_enabled")
        // V-- НОВЫЙ КЛЮЧ --V
        val IS_CATALOG_PRECACHED = booleanPreferencesKey("is_catalog_precached")
    }

    val isSoundEnabledFlow: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[IS_SOUND_ENABLED] ?: true
        }

    val isVibrationEnabledFlow: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[IS_VIBRATION_ENABLED] ?: true
        }

    suspend fun setSoundEnabled(isEnabled: Boolean) {
        context.dataStore.edit { settings ->
            settings[IS_SOUND_ENABLED] = isEnabled
        }
    }

    suspend fun setVibrationEnabled(isEnabled: Boolean) {
        context.dataStore.edit { settings ->
            settings[IS_VIBRATION_ENABLED] = isEnabled
        }
    }

    // V-- НОВЫЕ ФУНКЦИИ ДЛЯ РАБОТЫ С КЭШЕМ --V
    suspend fun isCatalogPrecached(): Boolean {
        return context.dataStore.data.map { preferences ->
            preferences[IS_CATALOG_PRECACHED] ?: false
        }.first() // .first() ждет первого значения и возвращает его
    }

    suspend fun setCatalogPrecached(isPrecached: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_CATALOG_PRECACHED] = isPrecached
        }
    }
    // ^-- КОНЕЦ НОВЫХ ФУНКЦИЙ --^
}