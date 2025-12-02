// Файл: features/profile/data/repository/EmployeeProfileRepository.kt
package com.example.qrscannerapp.features.profile.data.repository

import com.example.qrscannerapp.TelemetryManager
import com.example.qrscannerapp.features.profile.domain.model.DevicePerformanceDetails
import com.example.qrscannerapp.features.profile.domain.model.PerformanceClass
import com.example.qrscannerapp.features.profile.domain.model.UserActivityLog
import com.example.qrscannerapp.features.profile.domain.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmployeeProfileRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val telemetryManager: TelemetryManager // Инжектим наш сканер
) {

    // --- КОНСТАНТЫ ДЛЯ КОЛЛЕКЦИЙ FIREBASE ---
    private companion object {
        const val USERS_COLLECTION = "internal_users"
        const val ACTIVITY_LOG_COLLECTION = "activity_log"
    }

    /**
     * Загружает основную информацию о профиле пользователя один раз.
     * @param userId ID пользователя.
     * @return Result, содержащий UserProfile или ошибку.
     */
    suspend fun getUserProfile(userId: String): Result<UserProfile> {
        return try {
            val userDoc = firestore.collection(USERS_COLLECTION).document(userId).get().await()
            if (!userDoc.exists()) {
                return Result.failure(Exception("Документ пользователя не найден."))
            }

            val dateOfBirthStr = userDoc.getString("dateOfBirth")
            val age = if (dateOfBirthStr != null) calculateAge(dateOfBirthStr) else 0

            val profile = UserProfile(
                name = userDoc.getString("displayName") ?: "Без имени",
                username = userDoc.getString("username") ?: "N/A",
                role = if (userDoc.getBoolean("isAdmin") == true) "Администратор" else "Сотрудник",
                age = age,
                deviceInfo = userDoc.getString("deviceInfo") ?: "Нет данных",
                appVersion = userDoc.getString("appVersion") ?: "-",
                lastBatteryLevel = userDoc.getLong("lastBatteryLevel")?.toInt() ?: -1
            )
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Подписывается на последнее событие в логе активности пользователя.
     * @param userId ID пользователя.
     * @return Flow, который эмитит Result с последним логом или ошибкой.
     */
    fun listenForLastActivity(userId: String): Flow<Result<UserActivityLog?>> = callbackFlow {
        val listener = firestore.collection(ACTIVITY_LOG_COLLECTION)
            .whereEqualTo("creatorId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val doc = snapshot.documents[0]
                    val lastActivity = UserActivityLog(
                        id = doc.id,
                        timestamp = doc.getLong("timestamp") ?: 0,
                        activityType = doc.getString("activityType") ?: "UNKNOWN",
                        itemCount = doc.getLong("itemCount")?.toInt() ?: 0,
                        manualEntryCount = doc.getLong("manualEntryCount")?.toInt() ?: 0,
                        durationSeconds = doc.getLong("durationSeconds") ?: 0,
                        appVersion = doc.getString("appVersion") ?: "N/A",
                        lastBatteryLevel = doc.getLong("lastBatteryLevel")?.toInt() ?: -1,
                        isCharging = doc.getBoolean("isCharging") ?: false,
                        networkState = doc.getString("networkState") ?: "N/A",
                        freeRam = doc.getString("freeRam") ?: "N/A",
                        freeStorage = doc.getString("freeStorage") ?: "N/A",
                        deviceUptime = doc.getString("deviceUptime") ?: "N/A",
                        batteryHealth = doc.getString("batteryHealth") ?: "N/A",
                        isPowerSaveMode = doc.getBoolean("isPowerSaveMode") ?: false,
                        networkPing = doc.getString("networkPing") ?: "N/A",
                        totalRamInGb = doc.getDouble("totalRamInGb")
                    )
                    trySend(Result.success(lastActivity))
                } else {
                    // Пользователь еще ничего не делал
                    trySend(Result.success(null))
                }
            }
        // Этот блок выполнится, когда Flow будет закрыт (например, ViewModel уничтожится)
        awaitClose { listener.remove() }
    }

    /**
     * Определяет класс производительности на основе данных из лога.
     * Если в логе нет данных о RAM, использует сканер для определения по текущему устройству.
     * @param log Лог активности, может быть null.
     * @return DevicePerformanceDetails с классом и объемом RAM.
     */
    fun getPerformanceDetails(log: UserActivityLog?): DevicePerformanceDetails {
        val totalRam = log?.totalRamInGb
        if (totalRam != null && totalRam > 0) {
            // Если в логе есть инфа о RAM, вычисляем класс по ней
            val performanceClass = when {
                totalRam < 3.5 -> PerformanceClass.LOW
                totalRam < 7.5 -> PerformanceClass.MEDIUM
                else -> PerformanceClass.HIGH
            }
            return DevicePerformanceDetails(performanceClass, totalRam)
        } else {
            // Если в логе инфы нет (старые записи), возвращаем UNKNOWN.
            // В реальном времени мы бы могли использовать TelemetryManager, но для исторических
            // данных это будет некорректно. Показываем, что данных просто нет.
            return DevicePerformanceDetails(PerformanceClass.UNKNOWN, 0.0)
        }
    }

    private fun calculateAge(dateOfBirthString: String): Int {
        return try {
            val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            val birthDate = sdf.parse(dateOfBirthString)!!

            val birthCal = Calendar.getInstance().apply { time = birthDate }
            val todayCal = Calendar.getInstance()

            var age = todayCal.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR)
            if (todayCal.get(Calendar.DAY_OF_YEAR) < birthCal.get(Calendar.DAY_OF_YEAR)) {
                age--
            }
            age
        } catch (e: Exception) {
            0
        }
    }
}