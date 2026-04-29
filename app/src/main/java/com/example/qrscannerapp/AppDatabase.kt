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

// --- ИМПОРТЫ ДЛЯ INTERACTION ---
import com.example.qrscannerapp.features.interaction.data.local.entity.InteractionSessionEntity
import com.example.qrscannerapp.features.interaction.data.local.dao.InteractionDao
// ------------------------------------------

// --- НОВОЕ: ИМПОРТЫ ДЛЯ ТЕЛЕМЕТРИИ ---
import com.example.qrscannerapp.data.local.dao.TelemetryDao
import com.example.qrscannerapp.data.local.entity.TelemetryBuffer
// --------------------------------------

@Database(
    entities = [
        BatteryRepairLogEntity::class,
        ScanSessionEntity::class,
        TaskEntity::class,
        VehicleReportHistory::class,
        StorageCellEntity::class,
        StoragePalletEntity::class,
        InteractionSessionEntity::class,
        TelemetryBuffer::class  // <<< НОВОЕ
    ],
    version = 15, // <<< Увеличили с 14 до 15 (добавлена TelemetryBuffer)
    exportSchema = false
)
@TypeConverters(
    ScanConverters::class,
    TaskConverters::class,
    ElectricianConverters::class,
    StringListConverter::class
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun repairLogDao(): RepairLogDao
    abstract fun scanSessionDao(): ScanSessionDao
    abstract fun taskDao(): TaskDao
    abstract fun vehicleReportHistoryDao(): VehicleReportHistoryDao

    abstract fun storageCellDao(): StorageCellDao
    abstract fun storagePalletDao(): StoragePalletDao

    abstract fun interactionDao(): InteractionDao

    // <<< НОВОЕ: DAO для телеметрии
    abstract fun telemetryDao(): TelemetryDao

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