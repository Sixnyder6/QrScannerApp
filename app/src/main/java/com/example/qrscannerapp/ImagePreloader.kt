// Полное содержимое для НОВОГО файла ImagePreloader.kt

package com.example.qrscannerapp.core.image

import android.content.Context
import coil.ImageLoader
import coil.request.ImageRequest
import com.example.qrscannerapp.features.inventory.ui.Warehouse.components.getImageUrl
import com.example.qrscannerapp.features.inventory.ui.Warehouse.components.warehouseCatalogItems
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImagePreloader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageLoader: ImageLoader
) {

    suspend fun preloadCatalogImages() {
        withContext(Dispatchers.IO) {
            val itemsToPreload = warehouseCatalogItems
            itemsToPreload.forEach { item ->
                val imageUrl = getImageUrl(item)
                if (imageUrl != null) {
                    val request = ImageRequest.Builder(context)
                        .data(imageUrl)
                        .build()
                    imageLoader.enqueue(request)
                }
            }
        }
    }
}