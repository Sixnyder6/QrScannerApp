package com.example.qrscannerapp.features.homescreen.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.qrscannerapp.features.homescreen.data.local.HomescreenBackgroundDao
import com.example.qrscannerapp.features.homescreen.data.local.HomescreenBackgroundEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the "what's behind the homescreen widgets" state.
 *
 * Important detail: when the user picks an image from the system picker we get a content://
 * URI which:
 *   1. May lose access on reboot (no persistable permission requested in this version)
 *   2. May be from a cloud provider that disappears offline
 *
 * To dodge both we COPY the picked image into our private filesDir/homescreen_bg.jpg and
 * store an absolute file path in Room. The path is stable, requires no permissions, and
 * survives every reboot.
 */
@Singleton
class HomescreenBackgroundRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: HomescreenBackgroundDao
) {

    companion object {
        private const val TAG = "HomescreenBgRepo"
        private const val BG_FILENAME_PREFIX = "homescreen_bg_"
        private const val BG_FILENAME_SUFFIX = ".jpg"
    }

    /** Hot flow — UI subscribes here and reflects changes immediately. */
    fun observeBackground(): Flow<HomescreenBackgroundEntity?> = dao.observe()

    /**
     * Copies [sourceUri] into our private storage, replaces the previous background entry,
     * and deletes the old file so we don't leak storage.
     *
     * Returns the new file path on success, or null if the copy failed.
     */
    suspend fun setBackgroundFromUri(sourceUri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            // Use a timestamped filename so Coil definitely sees a new resource and
            // doesn't serve a cached version under the old path.
            val newFile = File(
                context.filesDir,
                "$BG_FILENAME_PREFIX${System.currentTimeMillis()}$BG_FILENAME_SUFFIX"
            )

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(newFile).use { output ->
                    input.copyTo(output)
                }
            } ?: run {
                Log.w(TAG, "Could not open input stream for $sourceUri")
                return@withContext null
            }

            // Delete the previous file (if any) — only AFTER the new one is on disk.
            val previous = dao.get()?.imagePath
            if (previous != null && previous != newFile.absolutePath) {
                runCatching { File(previous).takeIf { it.exists() }?.delete() }
                    .onFailure { Log.w(TAG, "Failed to delete previous bg", it) }
            }

            val entity = HomescreenBackgroundEntity(
                id = 0,
                imagePath = newFile.absolutePath,
                updatedAt = System.currentTimeMillis()
            )
            dao.upsert(entity)
            newFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "setBackgroundFromUri failed", e)
            null
        }
    }

    /** Reset back to the shader/gradient default. Removes the local file too. */
    suspend fun clearBackground() = withContext(Dispatchers.IO) {
        val previous = dao.get()?.imagePath
        if (previous != null) {
            runCatching { File(previous).takeIf { it.exists() }?.delete() }
        }
        dao.clear()
    }

    /** Adjust how strong the blur and darkening are without changing the image. */
    suspend fun updateAppearance(blurAmount: Float, darkenAmount: Float) {
        val current = dao.get() ?: return
        dao.upsert(
            current.copy(
                blurAmount = blurAmount.coerceIn(0f, 60f),
                darkenAmount = darkenAmount.coerceIn(0f, 0.85f),
                updatedAt = System.currentTimeMillis()
            )
        )
    }
}