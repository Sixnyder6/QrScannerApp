package com.example.qrscannerapp
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.example.qrscannerapp.features.electrician.data.repository.RepairLogRepository
import com.example.qrscannerapp.features.scanner.data.repository.ScanSessionRepository // <-- ИСПРАВЛЕННЫЙ ИМПОРТ
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext context: Context,
    private val repairRepository: RepairLogRepository,
    private val sessionRepository: ScanSessionRepository
) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        // --- НАЧАЛО ДОБАВЛЕНИЙ: Логи для теста Hilt ---
        Log.d("HiltTest", "✅ SyncManager CREATED by Hilt!")
        // --- КОНЕЦ ДОБАВЛЕНИЙ ---

        Log.d("SyncManager", "Initial sync check...")
        triggerSync()

        startNetworkListener()
    }

    private fun startNetworkListener() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                Log.d("SyncManager", "Network is available. Triggering sync.")
                triggerSync()
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                Log.d("SyncManager", "Network is lost.")
            }
        }

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    private fun triggerSync() {
        Log.d("SyncManager", "Triggering sync for all repositories.")

        // --- НАЧАЛО ДОБАВЛЕНИЙ: Логи для теста Hilt ---
        Log.d("HiltTest", "▶️ SyncManager is starting sync tasks...")
        // --- КОНЕЦ ДОБАВЛЕНИЙ ---

        scope.launch {
            repairRepository.syncPendingLogs()
        }

        scope.launch {
            sessionRepository.syncPendingSessions()
        }
    }
}