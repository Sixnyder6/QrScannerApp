package com.example.qrscannerapp.features.street_doctor.ui

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Создаём singleton DataStore для этого модуля
private val Context.importDataStore: DataStore<Preferences> by preferencesDataStore(name = "import_history_prefs")

object ImportPreferencesManager {

    // Ключи для хранения данных
    private val LAST_IMPORTED_AT_KEY = longPreferencesKey("last_imported_at_timestamp")
    private val LAST_IMPORT_COUNT_KEY = intPreferencesKey("last_import_count")
    private val LAST_SESSION_ID_KEY = stringPreferencesKey("last_session_id")

    /**
     * Поток для получения последнего времени импорта
     */
    fun getLastImportedAt(context: Context): Flow<Long?> {
        return context.importDataStore.data.map { preferences ->
            preferences[LAST_IMPORTED_AT_KEY]
        }
    }

    /**
     * Поток для получения количества загруженных заданий
     */
    fun getLastImportCount(context: Context): Flow<Int> {
        return context.importDataStore.data.map { preferences ->
            preferences[LAST_IMPORT_COUNT_KEY] ?: 0
        }
    }

    /**
     * Поток для получения ID последней сессии
     */
    fun getLastSessionId(context: Context): Flow<String?> {
        return context.importDataStore.data.map { preferences ->
            preferences[LAST_SESSION_ID_KEY]
        }
    }

    /**
     * Сохраняет информацию о последнем импорте
     */
    suspend fun saveLastImportInfo(
        context: Context,
        timestamp: Long,
        count: Int,
        sessionId: String
    ) {
        context.importDataStore.edit { preferences ->
            preferences[LAST_IMPORTED_AT_KEY] = timestamp
            preferences[LAST_IMPORT_COUNT_KEY] = count
            preferences[LAST_SESSION_ID_KEY] = sessionId
        }
    }
}