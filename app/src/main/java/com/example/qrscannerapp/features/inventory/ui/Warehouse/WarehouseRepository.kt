package com.example.qrscannerapp.features.inventory.data

import com.example.qrscannerapp.features.inventory.ui.Warehouse.Employee
import com.example.qrscannerapp.features.inventory.ui.Warehouse.components.DemoCatalogItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Locale

// --- Модель для представления состояния склада из Firestore ---
data class WarehouseState(
    val employeeOnShiftId: String? = null,
    val employeeStatus: String? = null
)

class WarehouseRepository {

    private val GITHUB_IMAGE_BASE_URL = "https://raw.githubusercontent.com/Sixnyder6/QrScannerApp/master/images/"

    private val db = FirebaseFirestore.getInstance()
    private val itemsCollection = db.collection("warehouse_items")
    private val logsCollection = db.collection("warehouse_logs")
    private val newsCollection = db.collection("warehouse_news")
    private val stateCollection = db.collection("warehouse_state")
    // --- НОВОЕ: Коллекция для сотрудников ---
    private val employeesCollection = db.collection("warehouse_employees")


    // --- НОВЫЙ БЛОК: ПОЛУЧЕНИЕ СПИСКА СОТРУДНИКОВ ---
    fun getEmployeesStream(): Flow<List<Employee>> = callbackFlow {
        val listener = employeesCollection
            .orderBy("name", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    // Преобразуем каждый документ в объект Employee.
                    // Важно: id документа мы присваиваем полю id в объекте.
                    val employees = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Employee::class.java)?.apply { id = doc.id }
                    }
                    trySend(employees)
                }
            }
        awaitClose { listener.remove() }
    }
    // --- КОНЕЦ НОВОГО БЛОКА ---


    // --- БЛОК: УПРАВЛЕНИЕ СОСТОЯНИЕМ СМЕНЫ ---
    fun getShiftState(): Flow<WarehouseState> = callbackFlow {
        val docRef = stateCollection.document("global")
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val state = snapshot?.toObject(WarehouseState::class.java) ?: WarehouseState()
            trySend(state)
        }
        awaitClose { listener.remove() }
    }

    suspend fun updateEmployeeOnShift(employeeId: String): Result<Unit> {
        return try {
            val docRef = stateCollection.document("global")
            docRef.update("employeeOnShiftId", employeeId).await()
            Result.success(Unit)
        } catch (e: Exception) {
            try {
                stateCollection.document("global").set(mapOf("employeeOnShiftId" to employeeId)).await()
                Result.success(Unit)
            } catch (setException: Exception) {
                Result.failure(setException)
            }
        }
    }

    suspend fun updateEmployeeStatus(status: String): Result<Unit> {
        return try {
            val docRef = stateCollection.document("global")
            docRef.update("employeeStatus", status).await()
            Result.success(Unit)
        } catch (e: Exception) {
            try {
                stateCollection.document("global").set(mapOf("employeeStatus" to status)).await()
                Result.success(Unit)
            } catch (setException: Exception) {
                Result.failure(setException)
            }
        }
    }
    // --- КОНЕЦ БЛОКА ---


    // --- БЛОК: РАБОТА С НОВОСТЯМИ ---
    fun getNewsStream(): Flow<List<NewsItem>> = callbackFlow {
        val listener = newsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val news = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(NewsItem::class.java)?.apply { id = doc.id }
                }
                trySend(news)
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun addNewsItem(newsItem: NewsItem): Result<Unit> {
        return try {
            newsCollection.document().set(newsItem).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateNewsItem(newsItem: NewsItem): Result<Unit> {
        return try {
            if (newsItem.id.isBlank()) throw IllegalArgumentException("NewsItem ID cannot be blank for update.")
            newsCollection.document(newsItem.id).set(newsItem).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteNewsItem(newsId: String): Result<Unit> {
        return try {
            newsCollection.document(newsId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    // 1. ПОЛУЧЕНИЕ СПИСКА ТОВАРОВ (Realtime)
    fun getWarehouseItems(): Flow<List<WarehouseItem>> = callbackFlow {
        val listener = itemsCollection
            .orderBy("shortName", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val items = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(WarehouseItem::class.java)?.apply {
                            id = doc.id
                        }
                    }
                    trySend(items)
                }
            }
        awaitClose { listener.remove() }
    }

    // --- ФУНКЦИЯ: ПОЛУЧЕНИЕ ЖУРНАЛА ОПЕРАЦИЙ ---
    fun getWarehouseLogs(): Flow<List<WarehouseLog>> = callbackFlow {
        val listener = logsCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val logs = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(WarehouseLog::class.java)?.apply {
                            id = doc.id
                        }
                    }
                    trySend(logs)
                }
            }
        awaitClose { listener.remove() }
    }


    // 2. ДОБАВЛЕНИЕ НОВОГО ТОВАРА
    suspend fun addNewItem(item: WarehouseItem): Result<Unit> {
        return try {
            val newDocRef = itemsCollection.document()
            val searchKeywords = generateKeywords(item.fullName, item.shortName, item.sku)
            val itemToSave = item.copy(keywords = searchKeywords)
            newDocRef.set(itemToSave).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- НОВЫЕ ФУНКЦИИ ДЛЯ РЕДАКТИРОВАНИЯ И УДАЛЕНИЯ ---
    suspend fun updateItem(item: WarehouseItem): Result<Unit> {
        return try {
            // Генерируем новые ключевые слова для поиска, если название изменилось
            val searchKeywords = generateKeywords(item.fullName, item.shortName, item.sku)
            val itemToSave = item.copy(keywords = searchKeywords)
            itemsCollection.document(item.id).set(itemToSave).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteItem(itemId: String): Result<Unit> {
        return try {
            itemsCollection.document(itemId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    // --- КОНЕЦ НОВЫХ ФУНКЦИЙ ---


    // 3. ВЗЯТИЕ ТОВАРА (ТРАНЗАКЦИЯ)
    suspend fun takeItem(itemId: String, quantityToTake: Int, userName: String): Result<Unit> {
        return try {
            db.runTransaction { transaction ->
                val docRef = itemsCollection.document(itemId)
                val snapshot = transaction.get(docRef)

                val currentStock = snapshot.getLong("stockCount")?.toInt() ?: 0
                val itemName = snapshot.getString("shortName") ?: "Неизвестно"
                val itemUnit = snapshot.getString("unit") ?: "шт."

                if (currentStock < quantityToTake) {
                    throw IllegalStateException("Мало товара! Остаток: $currentStock")
                }

                val newStock = currentStock - quantityToTake
                transaction.update(docRef, "stockCount", newStock)

                val newLogRef = logsCollection.document()
                val logEntry = WarehouseLog(
                    itemId = itemId,
                    itemName = itemName,
                    userName = userName,
                    quantityChange = -quantityToTake
                )
                transaction.set(newLogRef, logEntry)
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Функция для обновления URL изображения товара ---
    suspend fun updateItemImageUrl(itemId: String, newUrl: String?): Result<Unit> {
        return try {
            itemsCollection.document(itemId).update("imageUrl", newUrl).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    // --- "УМНАЯ СИНХРОНИЗАЦИЯ" (UPSERT) ---
    suspend fun uploadDemoData(demoItems: List<DemoCatalogItem>): Result<Int> {
        return try {
            val batch = db.batch()
            var count = 0

            demoItems.forEach { demo ->
                val docRef = itemsCollection.document(demo.id)

                val fullImageUrl = demo.imageName?.let { imageName ->
                    val imageNameWithExt = if (imageName.endsWith(".jpg") || imageName.endsWith(".png") || imageName.endsWith(".jpeg")) imageName else "$imageName.jpg"
                    GITHUB_IMAGE_BASE_URL + imageNameWithExt
                }

                val newItem = WarehouseItem(
                    fullName = demo.fullName,
                    shortName = demo.shortName,
                    sku = demo.sku,
                    category = demo.category,
                    unit = demo.unit,
                    stockCount = demo.stockCount,
                    totalStock = demo.totalStock,
                    imageUrl = fullImageUrl,
                    keywords = generateKeywords(demo.fullName, demo.shortName, demo.sku),
                    lowStockThreshold = (demo.totalStock * 0.15).toInt().coerceAtLeast(5)
                )

                batch.set(docRef, newItem)
                count++
            }

            batch.commit().await()
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun generateKeywords(vararg strings: String?): List<String> {
        val keywords = mutableListOf<String>()
        strings.filterNotNull().forEach { str ->
            val cleanStr = str.lowercase(Locale.getDefault()).trim()
            cleanStr.split(" ", "-", ".", "_", "/").forEach { word ->
                if (word.isNotEmpty()) keywords.add(word)
            }
            var temp = ""
            cleanStr.forEach { char ->
                temp += char
                keywords.add(temp)
            }
        }
        return keywords.distinct()
    }
}