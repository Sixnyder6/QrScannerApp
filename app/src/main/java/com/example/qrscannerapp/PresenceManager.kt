package com.example.qrscannerapp

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val TAG = "PresenceManager"

/**
 * Единственный источник правды для онлайн-статуса (lastSeen в internal_users).
 *
 * startTracking(uid)  — вызывается при логине
 * stopTracking()      — вызывается при логауте
 * pingNow()           — вызывается при сканировании, начале/конце смены, foreground приложения
 *
 * Порог онлайн/офлайн определяется в EmployeeInfo.ONLINE_THRESHOLD_MS (3 мин).
 */
class PresenceManager(private val firestore: FirebaseFirestore) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var currentUid: String? = null

    fun startTracking(uid: String) {
        currentUid = uid
        pingNow()
    }

    fun pingNow() {
        val uid = currentUid ?: return
        scope.launch {
            try {
                firestore.collection("internal_users").document(uid)
                    .update("lastSeen", System.currentTimeMillis())
                    .await()
                Log.d(TAG, "pingNow OK for $uid")
            } catch (e: Exception) {
                Log.e(TAG, "pingNow failed for $uid", e)
            }
        }
    }

    fun stopTracking() {
        val uid = currentUid ?: return
        currentUid = null
        scope.launch {
            try {
                firestore.collection("internal_users").document(uid)
                    .update("lastSeen", 0L)
                    .await()
                Log.d(TAG, "stopTracking OK for $uid")
            } catch (e: Exception) {
                Log.e(TAG, "stopTracking failed for $uid", e)
            }
        }
    }
}