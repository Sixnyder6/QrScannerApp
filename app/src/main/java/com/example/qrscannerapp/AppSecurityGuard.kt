package com.example.qrscannerapp

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Debug
import android.provider.Settings
import com.example.qrscannerapp.BuildConfig
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.Socket
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppSecurityGuard @Inject constructor(
    @ApplicationContext private val context: Context,
    private val telemetryManager: TelemetryManager? = null
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val securityPreferences = SecurityPreferences(context)

    // ====================================================================================
    // ПУБЛИЧНЫЕ МЕТОДЫ
    // ====================================================================================

    suspend fun runSecurityCheckAsync() = withContext(Dispatchers.IO) {
        if (BuildConfig.DEBUG) return@withContext

        val threats = mutableListOf<String>()

        if (isRootedOrDangerous())   threats.add("ROOT")
        if (isEmulatorAdvanced())    threats.add("EMULATOR")
        if (isDebuggerConnected())   threats.add("DEBUGGER")
        if (isFridaOrXposedActive()) threats.add("FRIDA_XPOSED")
        if (isVirtualApp())          threats.add("VIRTUAL_APP")
        if (!isSignatureValid())     threats.add("SIGNATURE")
        if (!isApkIntegrityOk())     threats.add("APK_TAMPERED")

        checkPlayIntegrityAsync()

        if (threats.isNotEmpty()) {
            saveSecurityEventLocally(threats)
            withContext(Dispatchers.Main) { crashApp() }
        }
    }

    fun runQuickCheck() {
        if (BuildConfig.DEBUG) return

        val threats = mutableListOf<String>()
        if (isDebuggerConnected()) threats.add("DEBUGGER")
        if (!isSignatureValid())   threats.add("SIGNATURE")
        if (threats.isNotEmpty()) {
            saveSecurityEventLocally(threats)
            crashApp()
        }
    }

    fun isDeviceSafe(): Boolean = !isRootedOrDangerous() && !isDebuggerConnected()
    fun getApkSignatureHash(): String = computeSignatureHash()

    // ====================================================================================
    // ДЕТЕКТОРЫ
    // ====================================================================================

    private fun isRootedOrDangerous(): Boolean {
        val suPaths = listOf(
            "/system/bin/su", "/system/xbin/su", "/sbin/su",
            "/data/local/xbin/su", "/data/local/bin/su", "/data/local/su",
            "/system/app/Superuser.apk"
        )
        if (suPaths.any { File(it).exists() }) return true

        val magiskPaths = listOf(
            "/sbin/.magisk", "/data/adb/magisk", "/data/adb/modules",
            "/cache/.disable_magisk"
        )
        if (magiskPaths.any { File(it).exists() }) return true

        if (getSystemProperty("ro.debuggable") == "1") return true
        if (getSystemProperty("ro.secure") == "0") return true

        val rootPackages = listOf(
            "com.topjohnwu.magisk",
            "de.robv.android.xposed.installer",
            "org.meowcat.LSPosed"
        )
        rootPackages.forEach { pkg ->
            try { context.packageManager.getPackageInfo(pkg, 0); return true }
            catch (_: Exception) {}
        }

        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val output = BufferedReader(InputStreamReader(process.inputStream)).readLine() ?: ""
            process.destroy()
            if (output.contains("uid=0")) return true
        } catch (_: Exception) {}

        return false
    }

    private fun isEmulatorAdvanced(): Boolean {
        var score = 0
        val fp = Build.FINGERPRINT.lowercase()
        if (fp.startsWith("generic") || fp.contains("unknown")) score++
        if (Build.MODEL.lowercase().let { it.contains("emulator") || it.contains("android sdk") }) score++
        if (Build.MANUFACTURER.lowercase().contains("genymotion")) score++
        if (Build.HARDWARE.lowercase().let { it.contains("goldfish") || it.contains("ranchu") || it.contains("vbox") }) score++
        if (Build.PRODUCT.lowercase().let { it.contains("sdk") || it.contains("vbox") }) score++
        if (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) score++

        val emuFiles = listOf("/dev/socket/qemud", "/dev/qemu_pipe", "/proc/tty/drivers")
        if (emuFiles.any { File(it).exists() }) score += 2

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? android.hardware.SensorManager
        val sensorCount = sensorManager?.getSensorList(android.hardware.Sensor.TYPE_ALL)?.size ?: 0
        if (sensorCount in 1..4) score++

        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        if (androidId == "000000000000000" || androidId == "0" || androidId.isNullOrBlank()) score++

        return score >= 3
    }

    private fun isDebuggerConnected(): Boolean =
        Debug.isDebuggerConnected() || Debug.waitingForDebugger()

    private fun isFridaOrXposedActive(): Boolean {
        try {
            val mapsContent = File("/proc/self/maps").readText()
            if (mapsContent.contains("frida", ignoreCase = true) ||
                mapsContent.contains("linjector", ignoreCase = true)) return true
        } catch (_: Exception) {}

        listOf(27042, 27043).forEach { port ->
            try {
                Socket().use { s -> s.connect(InetSocketAddress("127.0.0.1", port), 80); return true }
            } catch (_: Exception) {}
        }

        listOf("de.robv.android.xposed.XposedBridge", "org.lsposed.lspd.LSPosedBridge").forEach { cls ->
            try { Class.forName(cls); return true } catch (_: ClassNotFoundException) {}
        }

        listOf("de.robv.android.xposed.installer", "org.meowcat.LSPosed").forEach { pkg ->
            try { context.packageManager.getPackageInfo(pkg, 0); return true }
            catch (_: Exception) {}
        }
        return false
    }

    private fun isVirtualApp(): Boolean {
        val virtualPackages = listOf(
            "com.lbe.parallel.intl", "com.excelsior.virtualapp",
            "com.pspace.vandroid", "com.mapp.toolkit"
        )
        virtualPackages.forEach { pkg ->
            try { context.packageManager.getPackageInfo(pkg, 0); return true }
            catch (_: Exception) {}
        }
        listOf("/data/data/com.lbe.parallel.intl/", "/data/data/com.excelsior.virtualapp/").forEach { path ->
            if (File(path).exists()) return true
        }
        return false
    }

    // ====================================================================================
    // ПРОВЕРКА ПОДПИСИ
    // ====================================================================================

    private fun isSignatureValid(): Boolean {
        val storedHash = securityPreferences.getStoredSignatureHash()
        if (storedHash.isNullOrBlank()) {
            securityPreferences.storeSignatureHash(computeSignatureHash())
            return true
        }
        return computeSignatureHash() == storedHash
    }

    private fun computeSignatureHash(): String {
        return try {
            val pm = context.packageManager
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
            }
            val signature = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures?.firstOrNull()?.toByteArray()
            }
            signature?.let {
                MessageDigest.getInstance("SHA-256").digest(it)
                    .joinToString("") { byte -> "%02x".format(byte) }
            } ?: ""
        } catch (_: Exception) { "" }
    }

    // ====================================================================================
    // ПРОВЕРКА ЦЕЛОСТНОСТИ APK
    // ====================================================================================

    private fun isApkIntegrityOk(): Boolean {
        return try {
            val appInfo = context.packageManager
                .getPackageInfo(context.packageName, 0)
                .applicationInfo ?: return false
            val file = File(appInfo.publicSourceDir)
            file.exists() && file.length() > 0
        } catch (_: Exception) { false }
    }

    // ====================================================================================
    // PLAY INTEGRITY (асинхронно, только release)
    // ====================================================================================

    private fun checkPlayIntegrityAsync() {
        ioScope.launch {
            try {
                val integrityManager = IntegrityManagerFactory.create(context)
                val nonce = generateNonce()
                integrityManager.requestIntegrityToken(
                    com.google.android.play.core.integrity.IntegrityTokenRequest.builder()
                        .setNonce(nonce).build()
                ).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val token = task.result?.token()
                        if (token.isNullOrEmpty()) reportThreatAndCrash("PLAY_INTEGRITY")
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun generateNonce(): String {
        val bytes = ByteArray(24)
        kotlin.random.Random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // ====================================================================================
    // СОХРАНЕНИЕ СОБЫТИЙ + КРАШ
    // ====================================================================================

    private fun saveSecurityEventLocally(threats: List<String>) {
        val event = mapOf(
            "threats"    to threats.joinToString(","),
            "timestamp"  to System.currentTimeMillis().toString(),
            "device"     to "${Build.MANUFACTURER} ${Build.MODEL}",
            "android_id" to (Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown")
        )
        securityPreferences.addPendingSecurityEvent(event)
        ioScope.launch {
            try {
                firestore.collection("security_events").add(event)
                    .addOnSuccessListener { securityPreferences.removePendingSecurityEvents() }
            } catch (_: Exception) {}
        }
    }

    private fun reportThreatAndCrash(threat: String) {
        saveSecurityEventLocally(listOf(threat))
        crashApp()
    }

    private fun crashApp() {
        (context as? android.app.Activity)?.finishAffinity()
        android.os.Process.killProcess(android.os.Process.myPid())
        System.exit(0)
    }

    private fun getSystemProperty(prop: String): String? {
        return try {
            BufferedReader(InputStreamReader(Runtime.getRuntime().exec("getprop $prop").inputStream)).readLine()
        } catch (_: Exception) { null }
    }
}