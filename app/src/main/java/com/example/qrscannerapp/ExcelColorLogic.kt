// Файл: ExcelColorLogic.kt
package com.example.qrscannerapp

import org.apache.poi.ss.usermodel.IndexedColors

/**
 * Синглтон-объект, содержащий логику для определения цвета ячейки Excel
 * на основе ключевых слов в описании ячейки хранения.
 */
object ExcelColorLogic {

    // Определяем уровни приоритета для цветов
    private const val PRIORITY_RED = 3
    private const val PRIORITY_YELLOW = 2
    private const val PRIORITY_GREEN = 1

    /**
     * Словарь ключевых слов. Поиск нечувствителен к регистру.
     * Ключи - это приоритеты, значения - списки ключевых слов.
     */
    private val keywordMap = mapOf(
        // Красный уровень (самый высокий приоритет)
        PRIORITY_RED to listOf(
            "стойки", "стойка",
            "мотор", "матор", "моторы",
            "покрышки", "покрышка", "колесо", "колеса",
            "электрика", "iot", "айот", "проводка"
        ),
        // Желтый уровень
        PRIORITY_YELLOW to listOf(
            "легкий ремонт", "легкий", "мелкий"
        ),
        // Зеленый уровень
        PRIORITY_GREEN to listOf(
            "после ремонта", "готов", "готовы", "обслужен"
        )
    )

    /**
     * Анализирует описание и возвращает индекс цвета для Apache POI.
     * @param description Текст описания ячейки.
     * @return Индекс цвета (Short) из IndexedColors или null, если цвет не определен.
     */
    fun getColorIndexForDescription(description: String): Short? {
        val lowerCaseDescription = description.lowercase() // Приводим к нижнему регистру для поиска

        // Проверяем с самого высокого приоритета (Красный)
        if (keywordMap[PRIORITY_RED]!!.any { keyword -> lowerCaseDescription.contains(keyword) }) {
            return IndexedColors.RED.index
        }

        // Если красных слов нет, проверяем желтые
        if (keywordMap[PRIORITY_YELLOW]!!.any { keyword -> lowerCaseDescription.contains(keyword) }) {
            return IndexedColors.YELLOW.index
        }

        // Если и желтых нет, проверяем зеленые
        if (keywordMap[PRIORITY_GREEN]!!.any { keyword -> lowerCaseDescription.contains(keyword) }) {
            return IndexedColors.GREEN.index
        }

        // Если ни одно ключевое слово не найдено, возвращаем null (без цвета)
        return null
    }
}