package com.example.qrscannerapp.features.electrician.data.repository

import android.util.Log
import com.example.qrscannerapp.features.electrician.data.local.entity.BatteryRepairLogEntity // <-- ИСПРАВЛЕНО
import com.example.qrscannerapp.features.electrician.data.local.dao.RepairLogDao
import com.example.qrscannerapp.features.electrician.domain.model.BatteryRepairLog
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.map // <-- Добавил, т.к. понадобится для конвертации

/**
 * Репозиторий для управления данными о ремонтах.
 * Является единой точкой доступа к данным, абстрагируя источники данных
 * (локальная база Room и удаленная Firebase Firestore).
 *
 * @param repairLogDao DAO для доступа к локальной таблице с логами ремонтов.
 */
class RepairLogRepository(private val repairLogDao: RepairLogDao) {

    /**
     * Поток (Flow) со списком всех логов, ожидающих отправки.
     * Позволяет UI реактивно обновляться при изменении локальных данных.
     */
    val pendingLogs: Flow<List<BatteryRepairLogEntity>> = repairLogDao.getAll() // <-- ИСПРАВЛЕНО

    private val firestore = Firebase.firestore

    // --- НАЧАЛО ИЗМЕНЕНИЙ: Новый метод для прямой отправки в облако ---
    /**
     * Пытается немедленно отправить лог ремонта в Firebase Firestore.
     * Этот метод предназначен для основного "онлайн" сценария.
     * В случае ошибки он выбросит исключение, которое нужно обработать в ViewModel.
     *
     * @param log Объект BatteryRepairLog для отправки.
     */
    suspend fun uploadRepairLogNow(log: BatteryRepairLog) {
        // Создаем Map для отправки в Firestore, используя ID из лога
        val logData = mapOf(
            "id" to log.id,
            "batteryId" to log.batteryId,
            "electricianId" to log.electricianId,
            "electricianName" to log.electricianName,
            "timestamp" to log.timestamp,
            "repairs" to log.repairs,
            "manufacturer" to log.manufacturer
        )

        // Отправляем данные и ждем завершения. Если будет ошибка сети, .await() выбросит исключение.
        firestore.collection("battery_repair_log").document(log.id).set(logData).await()
    }
    // --- КОНЕЦ ИЗМЕНЕНИЙ ---


    /**
     * Сохраняет отчет о ремонте в локальную базу данных.
     * Этот метод работает в "офлайн-режиме", обеспечивая быстрое сохранение
     * без ожидания ответа от сети.
     *
     * @param log Локальная модель отчета о ремонте.
     */
    suspend fun saveRepairLog(log: BatteryRepairLogEntity) { // <-- ИСПРАВЛЕНО
        repairLogDao.insert(log)
    }

    /**
     * Запускает процесс синхронизации локальных данных с Firebase Firestore.
     * Получает все ожидающие логи, пытается отправить каждый из них,
     * и в случае успеха удаляет из локальной базы.
     */
    suspend fun syncPendingLogs() {
        // .first() получает текущее значение из Flow и завершает его
        val pending = pendingLogs.first()

        if (pending.isEmpty()) {
            Log.d("RepairLogRepository", "No pending logs to sync.")
            return
        }

        Log.d("RepairLogRepository", "Syncing ${pending.size} pending log(s)...")

        for (log in pending) {
            try {
                // Создаем Map для отправки в Firestore
                val logData = mapOf(
                    "batteryId" to log.batteryId,
                    "electricianId" to log.electricianId,
                    "electricianName" to log.electricianName,
                    "timestamp" to log.timestamp,
                    "repairs" to log.repairs,
                    "manufacturer" to log.manufacturer
                )

                // Отправляем данные и ждем завершения с помощью .await()
                firestore.collection("battery_repair_log").add(logData).await()

                // Если отправка прошла успешно, удаляем запись из локальной базы
                repairLogDao.delete(log)
                Log.d("RepairLogRepository", "Log for battery ${log.batteryId} synced successfully.")

            } catch (e: Exception) {
                // Если произошла ошибка (например, нет интернета), логируем ее
                // и прерываем цикл, чтобы не пытаться отправить остальные записи.
                // Мы попробуем снова при следующем вызове syncPendingLogs().
                Log.e("RepairLogRepository", "Failed to sync log for battery ${log.batteryId}", e)
                break
            }
        }
    }
}