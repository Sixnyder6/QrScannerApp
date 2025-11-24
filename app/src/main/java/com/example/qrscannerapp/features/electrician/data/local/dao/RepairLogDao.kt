package com.example.qrscannerapp.features.electrician.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.qrscannerapp.features.electrician.data.local.entity.BatteryRepairLogEntity // <-- ИМПОРТ ИСПРАВЛЕН
import kotlinx.coroutines.flow.Flow

/**
DAO (Data Access Object) для работы с таблицей pending_repair_logs.
Определяет методы для взаимодействия с локальной базой данных.
 */
@Dao
interface RepairLogDao {
    /**
    Вставляет одну запись о ремонте в локальную базу данных.
    Если запись уже существует, она будет заменена.
    @param log Объект лога для сохранения.
     */
    @Insert
    suspend fun insert(log: BatteryRepairLogEntity) // <-- ИСПРАВЛЕНО
    /**
    Возвращает все сохраненные логи ремонтов в виде потока (Flow).
    Flow будет автоматически эмитить новый список при любом изменении в таблице.
    Записи отсортированы по времени (от новых к старым).
    @return Flow со списком всех локальных логов.
     */
    @Query("SELECT * FROM pending_repair_logs ORDER BY timestamp DESC")
    fun getAll(): Flow<List<BatteryRepairLogEntity>> // <-- ИСПРАВЛЕНО
    /**
    Удаляет указанную запись о ремонте из локальной базы данных.
    Этот метод следует вызывать после успешной синхронизации лога с Firebase.
    @param log Объект лога для удаления.
     */
    @Delete
    suspend fun delete(log: BatteryRepairLogEntity) // <-- ИСПРАВЛЕНО
}