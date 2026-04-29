// Path: app/src/main/java/com/example/qrscannerapp/features/inventory/domain/model/StorageModels.kt

package com.example.qrscannerapp.features.inventory.domain.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.*

// ============================================================================================
// ТИПЫ ЯЧЕЕК (АКБ)
// ============================================================================================

/**
 * Типы содержимого ячейки.
 * Заменяет старое строковое поле `manufacturer` на типизированную систему.
 *
 * WIND 4.0 (старая модель, другие регионы):
 *   - FUJIAN  → префикс 4BB + 11 цифр
 *   - BYD     → префикс 4BZ + 11 цифр
 *
 * WIND 5.0 / Ninebot (текущая модель, СПб):
 *   - NINEBOT_NEW (5BB) → префикс 5BB + 11 цифр (новое поколение)
 *   - NINEBOT_OLD (SF)  → префикс SF + 2 буквы + 11 символов (старые, с 3-го поколения)
 */
enum class CellType(
    val displayName: String,
    val shortLabel: String,
    val prefix: String,
    val group: CellGroup
) {
    FUJIAN      ("WIND 4.0 (FUJIAN)",       "FUJIAN",       "4BB", CellGroup.WIND_40),
    BYD         ("WIND 4.0 (BYD)",          "BYD",          "4BZ", CellGroup.WIND_40),
    NINEBOT_NEW ("WIND 5.0 (Новый)",        "5BB",          "5BB", CellGroup.WIND_50),
    NINEBOT_OLD ("WIND 5.0 (Старый)",       "SF",           "SF",  CellGroup.WIND_50);

    companion object {
        /**
         * Маппинг из старого поля `manufacturer` в новый CellType.
         * Для обратной совместимости с существующими документами Firestore.
         */
        fun fromLegacyManufacturer(manufacturer: String?): CellType? {
            return when (manufacturer?.uppercase()?.trim()) {
                "FUJIAN"      -> FUJIAN
                "BYD"         -> BYD
                "NEW", "5BB"  -> NINEBOT_NEW
                "OLD", "SF"   -> NINEBOT_OLD
                else          -> null
            }
        }

        /**
         * Определяет тип ячейки по коду АКБ.
         */
        fun fromBatteryCode(code: String): CellType? {
            val upper = code.uppercase().trim()
            return when {
                upper.startsWith("4BB") -> FUJIAN
                upper.startsWith("4BZ") -> BYD
                upper.startsWith("5BB") -> NINEBOT_NEW
                upper.startsWith("SF")  -> NINEBOT_OLD
                else -> null
            }
        }
    }
}

/**
 * Группа ячеек — объединяет типы по модели самоката.
 */
enum class CellGroup(val displayName: String) {
    WIND_40("WIND 4.0"),
    WIND_50("WIND 5.0 / Ninebot")
}

// ============================================================================================
// СТАТУСЫ ЯЧЕЙКИ
// ============================================================================================

/**
 * Статус ячейки в жизненном цикле.
 */
enum class CellStatus(val displayName: String, val emoji: String) {
    ACTIVE    ("Хранение",  "📦"),   // Обычное хранение на складе
    RECEIVING ("Приёмка",   "📥"),   // Идёт приёмка АКБ в ячейку
    SHIPPING  ("Отправка",  "🚛"),   // Ячейка готовится к отправке
    REVISION  ("Ревизия",   "🔍"),   // Ячейка на ревизии / проверке
    ARCHIVED  ("Архив",     "🗄️");   // Ячейка закрыта / отправлена

    companion object {
        fun fromString(value: String?): CellStatus {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: ACTIVE
        }
    }
}

// ============================================================================================
// МОДЕЛЬ ЯЧЕЙКИ (БЫВШИЙ ПАЛЕТ)
// ============================================================================================

/**
 * Модель данных для ячейки хранения АКБ (бывший "Палет").
 * Хранится в Firestore коллекции `storage_pallets`.
 *
 * Обратная совместимость:
 * - Старые документы без новых полей десериализуются корректно (все дефолты безопасны)
 * - Поле `manufacturer` сохранено, `cellType` дублирует его с расширенной типизацией
 * - `palletNumber` сохранено как `cellNumber` с алиасом
 */
data class StoragePallet(
    val id: String = UUID.randomUUID().toString(),

    /** Номер ячейки (бывший palletNumber). Сохраняем имя поля для Firestore. */
    val palletNumber: Int = 0,

    /** Список кодов АКБ в ячейке */
    val items: List<String> = emptyList(),

    @ServerTimestamp
    val createdAt: Date? = null,

    val isFull: Boolean = false,

    // --- СТАРОЕ ПОЛЕ (обратная совместимость) ---
    /** @deprecated Используй cellType. Сохранено для чтения старых документов Firestore. */
    val manufacturer: String? = null,

    // --- НОВЫЕ ПОЛЯ ---

    /**
     * Тип содержимого ячейки (строковое представление CellType).
     * Хранится как String в Firestore для гибкости.
     * Примеры: "FUJIAN", "BYD", "NINEBOT_NEW", "NINEBOT_OLD"
     */
    val cellType: String? = null,

    /**
     * Максимальная ёмкость ячейки.
     * По умолчанию 500, но можно настроить индивидуально.
     */
    val capacity: Int = 500,

    /**
     * Адрес / локация склада.
     * Примеры: "Бестужевская 10", "Москва, склад №3"
     */
    val address: String? = null,

    /**
     * Текущий статус ячейки (строковое представление CellStatus).
     * Примеры: "ACTIVE", "RECEIVING", "SHIPPING", "REVISION", "ARCHIVED"
     */
    val status: String? = null,

    /**
     * Опциональное пользовательское имя ячейки.
     * Если задано, отображается вместо "Ячейка №X".
     * Пример: "Основная приёмка FUJIAN", "Отправка в Москву"
     */
    val displayName: String? = null,

    /** ID пользователя, создавшего ячейку */
    val creatorId: String? = null,

    /** Имя пользователя, создавшего ячейку */
    val creatorName: String? = null

) {
    // Пустой конструктор для Firebase десериализации
    constructor() : this(
        "", 0, emptyList(), null, false,
        null, null, 500, null, null, null, null, null
    )

    // --- ВЫЧИСЛЯЕМЫЕ СВОЙСТВА ---

    /** Номер ячейки (алиас для palletNumber) */
    val cellNumber: Int get() = palletNumber

    /**
     * Резолвит тип ячейки: сначала из нового поля `cellType`,
     * потом fallback на старое поле `manufacturer`.
     */
    val resolvedCellType: CellType?
        get() {
            // Сначала пробуем новое поле
            if (cellType != null) {
                val fromEnum = CellType.entries.find { it.name.equals(cellType, ignoreCase = true) }
                if (fromEnum != null) return fromEnum
            }
            // Fallback на старое поле manufacturer
            return CellType.fromLegacyManufacturer(manufacturer)
        }

    /** Резолвит статус ячейки */
    val resolvedStatus: CellStatus
        get() = CellStatus.fromString(status)

    /** Отображаемое имя: кастомное или "Ячейка №X" */
    val resolvedDisplayName: String
        get() = displayName ?: "Ячейка №$palletNumber"

    /** Прогресс заполнения (0.0 .. 1.0) */
    val fillProgress: Float
        get() = if (capacity > 0) items.size.toFloat() / capacity else 0f

    /** Ячейка заполнена */
    val isAtCapacity: Boolean
        get() = items.size >= capacity

    /**
     * Проверяет, принадлежит ли АКБ этой ячейке по типу.
     * Если тип ячейки не задан — принимает любые АКБ.
     */
    fun acceptsBattery(batteryCode: String): Boolean {
        val type = resolvedCellType ?: return true // нет типа = принимает всё
        val batteryType = CellType.fromBatteryCode(batteryCode) ?: return false
        return batteryType == type
    }
}

// ============================================================================================
// UI STATE
// ============================================================================================

/**
 * Модель данных, описывающая состояние UI для экрана распределения (бывший "Приёмка").
 */
data class PalletDistributionUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val pallets: List<StoragePallet> = emptyList(),
    val undistributedItemCount: Int = 0,
    val isDistributing: Boolean = false,
    val distributionResult: String? = null,
    val activityLog: List<PalletActivityLogEntry> = emptyList()
)

// ============================================================================================
// ACTIVITY LOG
// ============================================================================================

/**
 * Модель данных для записи активности по ячейкам (для истории операций).
 */
data class PalletActivityLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val userId: String? = null,
    val userName: String? = null,
    val action: String,
    val palletNumber: Int? = null,
    val itemCount: Int? = null,
    val palletId: String? = null
) {
    // Пустой конструктор для Firebase десериализации
    constructor() : this("", 0, null, null, "", null, null, null)
}