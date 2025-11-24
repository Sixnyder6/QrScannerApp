package com.example.qrscannerapp
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
object ImageUtils {
    /**
     * Сохраняет Bitmap в галерею устройства, используя MediaStore API.
     * Этот метод является предпочтительным для Android 10 (Q) и выше.
     * @param context Контекст приложения.
     * @param bitmap Изображение для сохранения.
     * @param displayName Имя файла, которое будет видно в галерее (без расширения).
     */
    suspend fun saveBitmapToGallery(context: Context, bitmap: Bitmap, displayName: String) {
        withContext(Dispatchers.IO) { // Выполняем файловые операции в фоновом потоке
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$displayName.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            var uri = resolver.insert(collection, contentValues)

            try {
                uri?.let {
                    resolver.openOutputStream(it).use { outputStream ->
                        if (outputStream == null) {
                            throw Exception("Failed to get output stream.")
                        }
                        if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)) {
                            throw Exception("Failed to save bitmap.")
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                        resolver.update(it, contentValues, null, null)
                    }
                    // Показываем сообщение об успехе в главном потоке
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "QR-код сохранен в галерею", Toast.LENGTH_SHORT).show()
                    }
                } ?: throw Exception("MediaStore returned null URI.")
            } catch (e: Exception) {
                e.printStackTrace()
                // Если произошла ошибка, удаляем запись, если она была создана
                uri?.let { resolver.delete(it, null, null) }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Ошибка сохранения: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * Сохраняет Bitmap во временный кеш и создает Intent для "Поделиться".
     * @param context Контекст приложения.
     * @param bitmap Изображение для отправки.
     * @param fileName Имя временного файла.
     */
    suspend fun shareBitmap(context: Context, bitmap: Bitmap, fileName: String) {
        withContext(Dispatchers.IO) {
            try {
                // Создаем директорию в кеше, если ее нет
                val cachePath = File(context.cacheDir, "images")
                cachePath.mkdirs()

                // Создаем временный файл
                val file = File(cachePath, "$fileName.png")
                val fileOutputStream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
                fileOutputStream.close()

                // Получаем URI через FileProvider
                val authority = "${context.packageName}.provider"
                val contentUri = FileProvider.getUriForFile(context, authority, file)

                // Создаем Intent
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Даем временное разрешение
                    setDataAndType(contentUri, context.contentResolver.getType(contentUri))
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(Intent.EXTRA_TEXT, "Сгенерированный QR-код: $fileName")
                }

                // Запускаем системный диалог "Поделиться" в главном потоке
                withContext(Dispatchers.Main) {
                    context.startActivity(Intent.createChooser(shareIntent, "Поделиться QR-кодом"))
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Ошибка подготовки файла: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}