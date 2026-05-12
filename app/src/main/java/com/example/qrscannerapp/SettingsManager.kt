package com.example.qrscannerapp

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.qrscannerapp.common.ui.ColorThemeEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

// [НОВОЕ] Enum для всех доступных тем фона и цветов
enum class AppTheme(
    val backgroundKey: String,
    val backgroundName: String,
    val backgroundEmoji: String,
    val colorTheme: ColorThemeEngine.ColorPalette
) {
    // Фоновые темы
    ENGINE("engine", "Энергия", "⚡", ColorThemeEngine.PurpleTheme),
    NEBULA("nebula", "Туманность", "🌌", ColorThemeEngine.GreenTheme),
    VORONOI("voronoi", "Биосфера", "🧬", ColorThemeEngine.RedTheme),

    // Новые цветовые темы
    WHITE("white", "Белая", "⚪", ColorThemeEngine.WhiteTheme),
    YELLOW("yellow", "Желтая", "💛", ColorThemeEngine.YellowTheme),
    EMBER("ember", "Закат", "🔥", ColorThemeEngine.RedTheme),
    AURORA("aurora", "Аврора", "🌌", ColorThemeEngine.GreenTheme),
    PLASMA("plasma", "Плазма", "⚡", ColorThemeEngine.PurpleTheme),

    // Rive-анимация
    RIVE("rive", "Rive", "✨", ColorThemeEngine.PurpleTheme);

    companion object {
        fun fromKey(key: String?): AppTheme =
            entries.firstOrNull { it.backgroundKey == key } ?: ENGINE
    }
}

class SettingsManager(private val context: Context) {

    companion object {
        val IS_SOUND_ENABLED       = booleanPreferencesKey("is_sound_enabled")
        val IS_VIBRATION_ENABLED   = booleanPreferencesKey("is_vibration_enabled")
        val IS_CATALOG_PRECACHED   = booleanPreferencesKey("is_catalog_precached")
        // [НОВОЕ] Единый ключ для всех тем
        val APP_THEME              = stringPreferencesKey("app_theme")
    }

    val isSoundEnabledFlow: Flow<Boolean> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[IS_SOUND_ENABLED] ?: true }

    val isVibrationEnabledFlow: Flow<Boolean> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[IS_VIBRATION_ENABLED] ?: true }

    // [НОВОЕ] Поток для текущей темы приложения
    val appThemeFlow: Flow<AppTheme> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { AppTheme.fromKey(it[APP_THEME]) }

    suspend fun setSoundEnabled(isEnabled: Boolean) {
        context.dataStore.edit { it[IS_SOUND_ENABLED] = isEnabled }
    }

    suspend fun setVibrationEnabled(isEnabled: Boolean) {
        context.dataStore.edit { it[IS_VIBRATION_ENABLED] = isEnabled }
    }

    suspend fun setAppTheme(theme: AppTheme) {
        context.dataStore.edit { it[APP_THEME] = theme.backgroundKey }
    }

    suspend fun isCatalogPrecached(): Boolean =
        context.dataStore.data.map { it[IS_CATALOG_PRECACHED] ?: false }.first()

    suspend fun setCatalogPrecached(isPrecached: Boolean) {
        context.dataStore.edit { it[IS_CATALOG_PRECACHED] = isPrecached }
    }

    // Получение текущей темы (blocking)
    suspend fun getCurrentAppTheme(): AppTheme =
        context.dataStore.data
            .map { AppTheme.fromKey(it[APP_THEME]) }
            .first()

    // =================================================================================
    // НОВЫЕ МЕТОДЫ ДЛЯ УПРАВЛЕНИЯ КЭШЕМ
    // =================================================================================

    suspend fun getTotalCacheSize(): String {
        return withContext(Dispatchers.IO) {
            val cacheSize = getDirSize(context.cacheDir) + getDirSize(context.codeCacheDir)
            val sizeInMB = cacheSize / (1024.0 * 1024.0)
            when {
                cacheSize == 0L -> "0 KB"
                sizeInMB < 1 -> "${(cacheSize / 1024)} KB"
                sizeInMB < 1024 -> String.format("%.1f MB", sizeInMB)
                else -> String.format("%.1f GB", sizeInMB / 1024.0)
            }
        }
    }

    suspend fun clearAppCache(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                deleteRecursive(context.cacheDir)
                deleteRecursive(context.codeCacheDir)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun getScanHistorySize(): String {
        return withContext(Dispatchers.IO) {
            // Если есть локальная база данных с историей сканирований
            val historyFile = File(context.filesDir, "scan_history.json")
            val sizeInKB = if (historyFile.exists()) historyFile.length() / 1024 else 0
            if (sizeInKB > 0) "${sizeInKB} KB" else "Нет данных"
        }
    }

    suspend fun clearScanHistory(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val historyFile = File(context.filesDir, "scan_history.json")
                if (historyFile.exists()) historyFile.delete()
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    // Вспомогательные функции
    private fun getDirSize(dir: File): Long {
        if (!dir.exists() || !dir.isDirectory) return 0
        var size = 0L
        dir.listFiles()?.forEach { file ->
            size += if (file.isDirectory) getDirSize(file) else file.length()
        }
        return size
    }

    private fun deleteRecursive(file: File) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { deleteRecursive(it) }
        }
        file.delete()
    }
}