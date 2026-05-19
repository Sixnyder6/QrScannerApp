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

enum class AppTheme(
    val backgroundKey: String,
    val backgroundName: String,
    val backgroundEmoji: String,
    val colorTheme: ColorThemeEngine.ColorPalette
) {
    ENGINE("engine",   "Энергия",   "⚡", ColorThemeEngine.PurpleTheme),
    NEBULA("nebula",   "Туманность","🌌", ColorThemeEngine.GreenTheme),
    VORONOI("voronoi", "Биосфера",  "🧬", ColorThemeEngine.RedTheme),
    WHITE("white",     "Белая",     "⚪", ColorThemeEngine.WhiteTheme),
    YELLOW("yellow",   "Желтая",    "💛", ColorThemeEngine.YellowTheme),
    EMBER("ember",     "Закат",     "🔥", ColorThemeEngine.RedTheme),
    AURORA("aurora",   "Аврора",    "🌌", ColorThemeEngine.GreenTheme),
    PLASMA("plasma",   "Плазма",    "⚡", ColorThemeEngine.PurpleTheme),
    RIVE("rive",       "Rive",      "✨", ColorThemeEngine.PurpleTheme);

    companion object {
        fun fromKey(key: String?): AppTheme =
            entries.firstOrNull { it.backgroundKey == key } ?: ENGINE
    }
}

// =============================================================================
// DIALOG ANIMATION — стиль появления окон, управляется из Spyder3000
// =============================================================================

enum class DialogAnimation(
    val key: String,
    val label: String,
    val description: String,
    val emoji: String
) {
    SCALE_BOUNCY(
        key         = "scale_bouncy",
        label       = "Bounce",
        description = "Выпрыгивает из центра с отскоком",
        emoji       = "⚡"
    ),
    SLIDE_BOTTOM(
        key         = "slide_bottom",
        label       = "Снизу",
        description = "Выезжает снизу как sheet",
        emoji       = "↑"
    ),
    FALL_TOP(
        key         = "fall_top",
        label       = "Сверху",
        description = "Падает сверху и приземляется",
        emoji       = "↓"
    ),
    FADE(
        key         = "fade",
        label       = "Fade",
        description = "Плавное растворение",
        emoji       = "○"
    ),
    FLIP(
        key         = "flip",
        label       = "Flip",
        description = "Переворот по оси X",
        emoji       = "↻"
    ),
    ZOOM_SOFT(
        key         = "zoom_soft",
        label       = "Zoom",
        description = "Мягкий zoom без отскока",
        emoji       = "◎"
    );

    companion object {
        fun fromKey(key: String?): DialogAnimation =
            entries.firstOrNull { it.key == key } ?: SCALE_BOUNCY
    }
}

class SettingsManager(private val context: Context) {

    companion object {
        val IS_SOUND_ENABLED     = booleanPreferencesKey("is_sound_enabled")
        val IS_VIBRATION_ENABLED = booleanPreferencesKey("is_vibration_enabled")
        val IS_CATALOG_PRECACHED = booleanPreferencesKey("is_catalog_precached")
        val APP_THEME            = stringPreferencesKey("app_theme")
        // Новый ключ для анимации диалогов
        val DIALOG_ANIMATION     = stringPreferencesKey("dialog_animation")
        // Новый ключ для авто-обновления Spyder3000
        val SPYDER_AUTO_UPDATE   = booleanPreferencesKey("spyder_auto_update")
    }

    val isSoundEnabledFlow: Flow<Boolean> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[IS_SOUND_ENABLED] ?: true }

    val isVibrationEnabledFlow: Flow<Boolean> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[IS_VIBRATION_ENABLED] ?: true }

    val appThemeFlow: Flow<AppTheme> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { AppTheme.fromKey(it[APP_THEME]) }

    // Поток выбранной анимации диалогов
    val dialogAnimationFlow: Flow<DialogAnimation> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { DialogAnimation.fromKey(it[DIALOG_ANIMATION]) }

    // Поток авто-обновления Spyder3000
    val spyderAutoUpdateFlow: Flow<Boolean> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[SPYDER_AUTO_UPDATE] ?: true }

    suspend fun setSoundEnabled(isEnabled: Boolean) {
        context.dataStore.edit { it[IS_SOUND_ENABLED] = isEnabled }
    }

    suspend fun setVibrationEnabled(isEnabled: Boolean) {
        context.dataStore.edit { it[IS_VIBRATION_ENABLED] = isEnabled }
    }

    suspend fun setAppTheme(theme: AppTheme) {
        context.dataStore.edit { it[APP_THEME] = theme.backgroundKey }
    }

    suspend fun setDialogAnimation(animation: DialogAnimation) {
        context.dataStore.edit { it[DIALOG_ANIMATION] = animation.key }
    }

    suspend fun setSpyderAutoUpdate(enabled: Boolean) {
        context.dataStore.edit { it[SPYDER_AUTO_UPDATE] = enabled }
    }

    fun getSpyderAutoUpdate(): Boolean = true   // дефолт

    suspend fun isCatalogPrecached(): Boolean =
        context.dataStore.data.map { it[IS_CATALOG_PRECACHED] ?: false }.first()

    suspend fun setCatalogPrecached(isPrecached: Boolean) {
        context.dataStore.edit { it[IS_CATALOG_PRECACHED] = isPrecached }
    }

    suspend fun getCurrentAppTheme(): AppTheme =
        context.dataStore.data.map { AppTheme.fromKey(it[APP_THEME]) }.first()

    suspend fun getCurrentDialogAnimation(): DialogAnimation =
        context.dataStore.data.map { DialogAnimation.fromKey(it[DIALOG_ANIMATION]) }.first()

    suspend fun getTotalCacheSize(): String = withContext(Dispatchers.IO) {
        val cacheSize = getDirSize(context.cacheDir) + getDirSize(context.codeCacheDir)
        val sizeInMB = cacheSize / (1024.0 * 1024.0)
        when {
            cacheSize == 0L -> "0 KB"
            sizeInMB < 1    -> "${cacheSize / 1024} KB"
            sizeInMB < 1024 -> String.format("%.1f MB", sizeInMB)
            else            -> String.format("%.1f GB", sizeInMB / 1024.0)
        }
    }

    suspend fun clearAppCache(): Boolean = withContext(Dispatchers.IO) {
        try { deleteRecursive(context.cacheDir); deleteRecursive(context.codeCacheDir); true }
        catch (e: Exception) { e.printStackTrace(); false }
    }

    suspend fun getScanHistorySize(): String = withContext(Dispatchers.IO) {
        val historyFile = File(context.filesDir, "scan_history.json")
        val sizeInKB = if (historyFile.exists()) historyFile.length() / 1024 else 0
        if (sizeInKB > 0) "${sizeInKB} KB" else "Нет данных"
    }

    suspend fun clearScanHistory(): Boolean = withContext(Dispatchers.IO) {
        try { File(context.filesDir, "scan_history.json").takeIf { it.exists() }?.delete(); true }
        catch (e: Exception) { false }
    }

    private fun getDirSize(dir: File): Long {
        if (!dir.exists() || !dir.isDirectory) return 0
        var size = 0L
        dir.listFiles()?.forEach { size += if (it.isDirectory) getDirSize(it) else it.length() }
        return size
    }

    private fun deleteRecursive(file: File) {
        if (file.isDirectory) file.listFiles()?.forEach { deleteRecursive(it) }
        file.delete()
    }
}