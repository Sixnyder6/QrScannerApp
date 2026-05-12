package com.example.qrscannerapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.URL
import javax.inject.Inject

data class ReleaseItem(
    @SerializedName("type") val type: String,
    @SerializedName("text") val text: String
)

data class UpdateInfo(
    @SerializedName("latestVersionCode") val latestVersionCode: Int,
    @SerializedName("latestVersionName") val latestVersionName: String,
    @SerializedName("apkUrl") val apkUrl: String,
    @SerializedName("apkSize") val apkSize: String? = null,
    @SerializedName("releaseNotes") val releaseNotes: String,
    @SerializedName("imageUrls") val imageUrls: List<String>? = null,
    @SerializedName("releaseItems") val releaseItems: List<ReleaseItem>? = null,

    // НОВЫЕ ПОЛЯ ДЛЯ ЗАЩИТЫ ОТ СТАРЫХ ВЕРСИЙ
    @SerializedName("minRequiredVersionCode") val minRequiredVersionCode: Int = 0,
    @SerializedName("minRequiredVersionName") val minRequiredVersionName: String = "0"
)

sealed interface UpdateState {
    object Idle : UpdateState
    object Checking : UpdateState
    data class UpdateAvailable(val info: UpdateInfo) : UpdateState
    object UpdateNotAvailable : UpdateState
    data class Downloading(val progress: Int) : UpdateState
    data class ReadyToInstall(val uri: Uri) : UpdateState
    data class Error(val message: String) : UpdateState
}

sealed interface VersionCheckResult {
    object Ok : VersionCheckResult
    data class Outdated(val requiredVersionCode: Int, val requiredVersionName: String, val currentVersionCode: Int, val currentVersionName: String) : VersionCheckResult
    data class Error(val message: String) : VersionCheckResult
}

@HiltViewModel
class UpdateManager @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val gson = Gson()
    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState = _updateState.asStateFlow()

    private val _minRequiredVersion = MutableStateFlow<VersionCheckResult?>(null)
    val minRequiredVersion = _minRequiredVersion.asStateFlow()

    private val workManager = WorkManager.getInstance(context)

    companion object {
        private const val UPDATE_URL = "https://github.com/Sixnyder6/QrScannerApp/releases/latest/download/update.json"
        private const val UPDATE_WORK_NAME = "app_update_work"
    }

    init {
        observeUpdateWork()
    }

    private fun observeUpdateWork() {
        viewModelScope.launch {
            workManager.getWorkInfosForUniqueWorkFlow(UPDATE_WORK_NAME).collect { workInfos ->
                val workInfo = workInfos.firstOrNull() ?: return@collect

                when (workInfo.state) {
                    WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING -> {
                        val progress = workInfo.progress.getInt(UpdateWorker.KEY_PROGRESS, 0)
                        _updateState.value = UpdateState.Downloading(progress)
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        val uriString = workInfo.outputData.getString("apk_uri")
                        if (uriString != null) {
                            _updateState.value = UpdateState.ReadyToInstall(Uri.parse(uriString))
                        } else {
                            resetState()
                        }
                    }
                    WorkInfo.State.FAILED -> {
                        _updateState.value = UpdateState.Error("Ошибка загрузки обновления")
                        workManager.pruneWork()
                    }
                    WorkInfo.State.CANCELLED -> {
                        resetState()
                    }
                    else -> { }
                }
            }
        }
    }

    fun startUpdate(info: UpdateInfo) {
        if (!hasInstallPermission()) {
            requestInstallPermission()
            _updateState.value = UpdateState.UpdateAvailable(info)
            return
        }

        val workData = workDataOf(
            UpdateWorker.KEY_URL to info.apkUrl,
            UpdateWorker.KEY_VERSION_NAME to info.latestVersionName
        )

        val updateWorkRequest = OneTimeWorkRequestBuilder<UpdateWorker>()
            .setInputData(workData)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        workManager.enqueueUniqueWork(
            UPDATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            updateWorkRequest
        )
    }

    suspend fun checkForUpdates() {
        val workInfo = workManager.getWorkInfosForUniqueWork(UPDATE_WORK_NAME).get().firstOrNull()
        if (workInfo?.state == WorkInfo.State.RUNNING || workInfo?.state == WorkInfo.State.ENQUEUED) {
            return
        }

        _updateState.value = UpdateState.Checking

        withContext(Dispatchers.IO) {
            try {
                val jsonStr = URL(UPDATE_URL).readText()
                val updateInfo = gson.fromJson(jsonStr, UpdateInfo::class.java)
                val currentVersionCode = getCurrentVersionCode()

                if (updateInfo.latestVersionCode > currentVersionCode) {
                    _updateState.value = UpdateState.UpdateAvailable(updateInfo)
                } else {
                    _updateState.value = UpdateState.UpdateNotAvailable
                }
            } catch (e: Exception) {
                Log.e("UpdateManager", "Check failed", e)
                _updateState.value = UpdateState.Error("Ошибка проверки обновлений: ${e.message}")
            }
        }
    }

    // НОВЫЙ МЕТОД: Проверка минимальной версии с сервера
    suspend fun checkMinRequiredVersion(): VersionCheckResult {
        return withContext(Dispatchers.IO) {
            try {
                val jsonStr = URL(UPDATE_URL).readText()
                val updateInfo = gson.fromJson(jsonStr, UpdateInfo::class.java)
                val currentVersionCode = getCurrentVersionCode()
                val currentVersionName = getCurrentVersionName()

                if (currentVersionCode < updateInfo.minRequiredVersionCode) {
                    VersionCheckResult.Outdated(
                        requiredVersionCode = updateInfo.minRequiredVersionCode,
                        requiredVersionName = updateInfo.minRequiredVersionName,
                        currentVersionCode = currentVersionCode,
                        currentVersionName = currentVersionName
                    )
                } else {
                    VersionCheckResult.Ok
                }
            } catch (e: Exception) {
                Log.e("UpdateManager", "Min version check failed", e)
                VersionCheckResult.Error("Ошибка проверки версии: ${e.message}")
            }
        }
    }

    // НОВЫЙ МЕТОД: Быстрая проверка без интернета (использует последние известные данные)
    fun isAppOutdatedCached(minRequiredVersionCode: Int): Boolean {
        return getCurrentVersionCode() < minRequiredVersionCode
    }

    // НОВЫЙ МЕТОД: Получить текущую версию для отправки на сервер
    fun getCurrentVersionForServer(): Map<String, Any> {
        return mapOf(
            "versionCode" to getCurrentVersionCode(),
            "versionName" to getCurrentVersionName(),
            "androidSdk" to Build.VERSION.SDK_INT,
            "deviceInfo" to "${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE}"
        )
    }

    fun installApk(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }

    fun resetState() {
        _updateState.value = UpdateState.Idle
        workManager.cancelUniqueWork(UPDATE_WORK_NAME)
    }

    fun startCheckForUpdates() {
        viewModelScope.launch { checkForUpdates() }
    }

    private fun hasInstallPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else { true }
    }

    private fun requestInstallPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    private fun getCurrentVersionCode(): Int {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).versionCode
            }
        } catch (e: Exception) { -1 }
    }

    private fun getCurrentVersionName(): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0"
        } catch (e: Exception) { "0" }
    }
}