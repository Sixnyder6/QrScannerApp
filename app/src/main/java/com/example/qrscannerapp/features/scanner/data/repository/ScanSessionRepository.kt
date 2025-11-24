package com.example.qrscannerapp.features.scanner.data.repository

import android.util.Log
import com.example.qrscannerapp.features.scanner.data.local.dao.ScanSessionDao
import com.example.qrscannerapp.features.scanner.data.local.entity.ScanSessionEntity
import com.example.qrscannerapp.features.scanner.domain.model.ScanSession
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

/**
Репозиторий для управления данными о сессиях сканирования.
...
@param scanSessionDao DAO для доступа к локальной таблице с сессиями.
 */
class ScanSessionRepository(private val scanSessionDao: ScanSessionDao) {
    private val firestore = Firebase.firestore
    /**
    Сохраняет сессию сканирования в локальную базу данных (оффлайн-режим).
    @param session Локальная модель сессии для сохранения.
     */
    suspend fun saveSessionLocally(session: ScanSessionEntity) { // <-- ИСПРАВЛЕНО
        scanSessionDao.insert(session)
        Log.d("ScanSessionRepository", "Session ${session.id} saved locally.")
    }
    /**
    Запускает процесс синхронизации локальных сессий с Firebase Firestore.
    ...
     */
    suspend fun syncPendingSessions() {
        val pendingSessions = scanSessionDao.getAllPending()
        if (pendingSessions.isEmpty()) {
            Log.d("ScanSessionRepository", "No pending sessions to sync.")
            return
        }
        Log.d("ScanSessionRepository", "Syncing ${pendingSessions.size} pending session(s)...")
        for (localSession in pendingSessions) {
            try {
// Создаем объект ScanSession, который совместим с Firestore
// (у него есть пустой конструктор и правильные типы данных)
                val sessionToUpload = ScanSession(
                    id = localSession.id,
                    name = localSession.name,
                    timestamp = localSession.timestamp,
                    items = localSession.items,
                    type = localSession.type,
                    creatorId = localSession.creatorId,
                    creatorName = localSession.creatorName
                )
// Отправляем данные в Firestore, используя ID сессии в качестве ID документа
                firestore.collection("scan_sessions").document(sessionToUpload.id)
                    .set(sessionToUpload)
                    .await()

                // Если отправка прошла успешно, удаляем запись из локальной базы
                scanSessionDao.delete(localSession)
                Log.d("ScanSessionRepository", "Session ${localSession.id} synced successfully.")

            } catch (e: Exception) {
                // Если произошла ошибка, логируем ее и прерываем цикл.
                // Повторная попытка будет при следующем запуске SyncManager.
                Log.e("ScanSessionRepository", "Failed to sync session ${localSession.id}", e)
                break
            }
        }
    }
}