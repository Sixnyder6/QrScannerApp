// Полная и исправленная версия файла: AppDatabase.kt

package com.example.qrscannerapp

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.qrscannerapp.features.electrician.data.local.dao.RepairLogDao
import com.example.qrscannerapp.features.electrician.data.local.converter.ElectricianConverters
import com.example.qrscannerapp.features.electrician.data.local.entity.BatteryRepairLogEntity
import com.example.qrscannerapp.features.inventory.data.local.dao.StorageCellDao
import com.example.qrscannerapp.features.inventory.data.local.dao.StoragePalletDao
import com.example.qrscannerapp.features.inventory.data.local.entity.StorageCellEntity
import com.example.qrscannerapp.features.inventory.data.local.entity.StoragePalletEntity
import com.example.qrscannerapp.features.scanner.data.local.converter.ScanConverters
import com.example.qrscannerapp.features.scanner.data.local.dao.ScanSessionDao
import com.example.qrscannerapp.features.scanner.data.local.entity.ScanSessionEntity
import com.example.qrscannerapp.features.tasks.data.local.converter.TaskConverters
import com.example.qrscannerapp.features.tasks.data.local.dao.TaskDao
import com.example.qrscannerapp.features.tasks.data.local.entity.TaskEntity
import com.example.qrscannerapp.features.vehicle_report.data.local.dao.VehicleReportHistoryDao
import com.example.qrscannerapp.features.vehicle_report.data.local.converters.StringListConverter
import com.example.qrscannerapp.features.vehicle_report.domain.model.VehicleReportHistory

@Database(
    entities = [
        BatteryRepairLogEntity::class,
        ScanSessionEntity::class,
        TaskEntity::class,
        VehicleReportHistory::class,
        // Добавляем наши новые сущности
        StorageCellEntity::class,
        StoragePalletEntity::class
    ],
    version = 11, // Версия увеличена для миграции
    exportSchema = false
)
@TypeConverters(
    ScanConverters::class,
    TaskConverters::class,
    ElectricianConverters::class,
    StringListConverter::class
    // Я убрал отсюда конфликтный InventoryConverters::class
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun repairLogDao(): RepairLogDao
    abstract fun scanSessionDao(): ScanSessionDao
    abstract fun taskDao(): TaskDao
    abstract fun vehicleReportHistoryDao(): VehicleReportHistoryDao

    // Добавляем наши новые DAO
    abstract fun storageCellDao(): StorageCellDao
    abstract fun storagePalletDao(): StoragePalletDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "qr_scanner_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}