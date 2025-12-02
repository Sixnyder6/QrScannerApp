// --- Файл UpdateManager.kt ---
// --- Версия с добавленным полем apkSize ---

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

// НОВЫЙ КЛАСС для одного пункта в списке изменений
data class ReleaseItem(
    @SerializedName("type") val type: String, // "new", "fix", "beta"
    @SerializedName("text") val text: String  // "Описание изменения"
)

// ОБНОВЛЕННЫЙ КЛАСС с информацией об обновлении
data class UpdateInfo(
    @SerializedName("latestVersionCode") val latestVersionCode: Int,
    @SerializedName("latestVersionName") val latestVersionName: String,
    @SerializedName("apkUrl") val apkUrl: String,

    // --- НАЧАЛО ИЗМЕНЕНИЙ ---
    // НОВОЕ ПОЛЕ: Размер файла для отображения на кнопке
    @SerializedName("apkSize") val apkSize: String? = null,
    // --- КОНЕЦ ИЗМЕНЕНИЙ ---

    // Старое поле. Оставляем для совместимости и простых заметок.
    @SerializedName("releaseNotes") val releaseNotes: String,

    // НОВОЕ ПОЛЕ: Список ссылок на картинки для слайдера
    @SerializedName("imageUrls") val imageUrls: List<String>? = null,

    // НОВОЕ ПОЛЕ: Структурированный список изменений с тегами
    @SerializedName("releaseItems") val releaseItems: List<ReleaseItem>? = null
)

sealed interface UpdateState {
    object Idle : UpdateState
    object Checking : UpdateState
    data class UpdateAvailable(val info: UpdateInfo) : UpdateState
    object UpdateNotAvailable : UpdateState
    data class Downloading(val progress: Int) : UpdateState
    data class Error(val message: String) : UpdateState
}

@HiltViewModel
class UpdateManager @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val gson = Gson()
    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState = _updateState.asStateFlow()

    private val workManager = WorkManager.getInstance(context)

    companion object {
        private const val UPDATE_URL = "https://github.com/Sixnyder6/QrscannerApp/releases/latest/download/update.json"
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
                        resetState()
                    }
                    WorkInfo.State.FAILED -> {
                        _updateState.value = UpdateState.Error("Ошибка загрузки обновления")
                        workManager.pruneWork()
                    }
                    WorkInfo.State.CANCELLED -> {
                        resetState()
                    }
                    else -> { /* BLOCKED - ничего не делаем */ }
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

    fun resetState() {
        _updateState.value = UpdateState.Idle
        workManager.cancelUniqueWork(UPDATE_WORK_NAME)
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
}