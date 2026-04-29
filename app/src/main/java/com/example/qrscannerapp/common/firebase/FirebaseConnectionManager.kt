package com.example.qrscannerapp.common.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseConnectionManager @Inject constructor() {

    private val firestore = FirebaseFirestore.getInstance()

    private val _isConnected = MutableStateFlow(true)
    val isConnected: StateFlow<Boolean> = _isConnected

    private var monitoringJob: Job? = null

    fun startMonitoring() {
        if (monitoringJob != null) return

        monitoringJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                checkConnection()
                delay(8000) // проверка каждые 8 секунд
            }
        }
    }

    private suspend fun checkConnection() {
        try {
            firestore.collection("ping")
                .limit(1)
                .get(Source.SERVER)
                .await()

            _isConnected.value = true
        } catch (e: Exception) {
            _isConnected.value = false
        }
    }
}
