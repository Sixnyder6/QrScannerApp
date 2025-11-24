package com.example.qrscannerapp.features.vehicle_report.data.repository

import android.util.Log
import com.example.qrscannerapp.AuthManager
import com.example.qrscannerapp.features.vehicle_report.data.local.dao.VehicleReportHistoryDao
import com.example.qrscannerapp.features.vehicle_report.domain.model.VehicleReportHistory
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "VehicleReportRepository"
private const val REPORTS_COLLECTION = "vehicle_reports"

@Singleton
class VehicleReportRepository @Inject constructor(
    private val historyDao: VehicleReportHistoryDao,
    private val firestore: FirebaseFirestore,
    private val authManager: AuthManager
) {

    val allReports = historyDao.getAllReports()

    suspend fun uploadAndSaveReport(report: VehicleReportHistory) {
        val creatorId = authManager.currentUserId
        val creatorName = authManager.authState.value.userName

        if (creatorId == null || creatorName == null) {
            Log.e(TAG, "Ошибка: Не удалось загрузить отчет. Пользователь не авторизован.")
            historyDao.insertReport(report)
            return
        }

        val reportForFirestore = createFirestoreReportMap(report, creatorId, creatorName)

        try {
            val documentReference = firestore.collection(REPORTS_COLLECTION)
                .add(reportForFirestore)
                .await()

            val firestoreId = documentReference.id
            Log.d(TAG, "Отчет успешно загружен в Firestore с ID: $firestoreId")

            report.firestoreDocumentId = firestoreId

        } catch (e: Exception) {
            Log.e(TAG, "Ошибка загрузки отчета в Firestore. Сохраняем только локально.", e)
        }

        historyDao.insertReport(report)
        Log.d(TAG, "Отчет сохранен в локальной базе данных.")
    }

    suspend fun deleteReport(report: VehicleReportHistory) {
        val docId = report.firestoreDocumentId

        if (!docId.isNullOrBlank()) {
            try {
                firestore.collection(REPORTS_COLLECTION).document(docId).delete().await()
                Log.d(TAG, "Отчет с ID $docId успешно удален из Firestore.")
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка удаления отчета $docId из Firestore. Он может остаться в облаке.", e)
            }
        }

        historyDao.deleteReport(report)
        Log.d(TAG, "Отчет удален из локальной базы данных.")
    }

    // V-- НАЧАЛО НОВОГО, "ОПАСНОГО" МЕТОДА --V
    /**
     * ВНИМАНИЕ: Удаляет АБСОЛЮТНО ВСЕ отчеты из коллекции в Firestore,
     * а также все отчеты из локальной базы данных.
     */
    suspend fun deleteAllReports() {
        // --- Шаг 1: Полное удаление из Firestore ---
        try {
            // 1. Получаем ВСЕ документы из коллекции
            val querySnapshot = firestore.collection(REPORTS_COLLECTION).get().await()

            if (querySnapshot.isEmpty) {
                Log.d(TAG, "В коллекции Firestore нет отчетов для удаления.")
            } else {
                // 2. Используем пакетную запись (batch) для эффективного удаления всех документов
                val batch: WriteBatch = firestore.batch()
                for (document in querySnapshot.documents) {
                    batch.delete(document.reference)
                }
                // 3. Выполняем необратимую операцию удаления
                batch.commit().await()
                Log.w(TAG, "!!! УСПЕШНО УДАЛЕНЫ ВСЕ ${querySnapshot.size()} ОТЧЕТОВ ИЗ FIRESTORE !!!")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Критическая ошибка при полном удалении отчетов из Firestore.", e)
            // Если в облаке произошла ошибка, мы НЕ продолжаем, чтобы избежать рассинхронизации.
            throw e // Пробрасываем исключение
        }

        // --- Шаг 2: Удаление из локальной базы (выполняется только если в облаке все прошло успешно) ---
        historyDao.deleteAll()
        Log.d(TAG, "Все отчеты успешно удалены из локальной базы данных.")
    }
    // ^-- КОНЕЦ НОВОГО МЕТОДА --^

    private fun createFirestoreReportMap(report: VehicleReportHistory, creatorId: String, creatorName: String): Map<String, Any> {
        return mapOf(
            "creatorId" to creatorId,
            "creatorName" to creatorName,
            "timestamp" to report.timestamp,
            "fileName" to report.fileName,
            "readyForExportCount" to report.readyForExportCount,
            "storageCount" to report.storageCount,
            "awaitingTestingCount" to report.awaitingTestingCount,
            "testingChargedCount" to report.testingChargedCount,
            "testingDischargedCount" to report.testingDischargedCount,
            "awaitingRepairCount" to report.awaitingRepairCount
        )
    }
}
// ---
// На чем мы остановились: Выполнили Шаг 2 из 4. Предоставлена ПОЛНАЯ версия файла VehicleReportRepository.kt с "опасной" логикой полного удаления всех отчетов, согласно вашему требованию.
// Следующий шаг: Реализация Шага 3. Я жду от вас ПОЛНУЮ версию файла VehicleReportHistoryViewModel.kt, чтобы мы могли добавить вызов этого нового метода.