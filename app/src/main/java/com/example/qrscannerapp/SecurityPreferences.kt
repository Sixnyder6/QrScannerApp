package com.example.qrscannerapp

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SecurityPreferences(context: Context) {

    private val prefs: SharedPreferences

    init {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        prefs = EncryptedSharedPreferences.create(
            "secure_security_prefs",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun storeSignatureHash(hash: String) {
        prefs.edit().putString("signature_hash", hash).apply()
    }

    fun getStoredSignatureHash(): String? {
        return prefs.getString("signature_hash", null)
    }

    fun addPendingSecurityEvent(event: Map<String, String>) {
        val json = prefs.getString("pending_events", "[]")
        val list: MutableList<Map<String, String>> = Gson().fromJson(
            json,
            object : TypeToken<List<Map<String, String>>>() {}.type
        ) ?: mutableListOf()
        list.add(event)
        prefs.edit().putString("pending_events", Gson().toJson(list)).apply()
    }

    fun removePendingSecurityEvents() {
        prefs.edit().remove("pending_events").apply()
    }

    fun getPendingEvents(): List<Map<String, String>> {
        val json = prefs.getString("pending_events", "[]")
        return Gson().fromJson(
            json,
            object : TypeToken<List<Map<String, String>>>() {}.type
        ) ?: emptyList()
    }
}