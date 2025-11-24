package com.example.qrscannerapp.data.repository

import android.util.Log
import com.example.qrscannerapp.EmployeeInfo
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private const val USERS_COLLECTION = "internal_users"
private const val TAG = "UserRepository"

@Singleton
class UserRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    suspend fun getAllUsers(): List<EmployeeInfo> {
        try {
            val snapshot = firestore.collection(USERS_COLLECTION).get().await()
            return snapshot.documents.mapNotNull { doc ->
                // Используем ID документа как ID пользователя
                val userId = doc.id
                // Поле name должно быть 'displayName'
                val name = doc.getString("displayName") ?: return@mapNotNull null
                // Извлекаем роль (важно для EmployeeInfo)
                val role = doc.getString("role") ?: "employee" // Указываем дефолтное значение

                EmployeeInfo(
                    id = userId, // <-- ИСПОЛЬЗУЕМ ID ДОКУМЕНТА
                    name = name,
                    role = role, // <-- ДОБАВЛЕНО
                    status = doc.getString("status") ?: "offline"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching users from Firestore", e)
            throw e
        }
    }
}