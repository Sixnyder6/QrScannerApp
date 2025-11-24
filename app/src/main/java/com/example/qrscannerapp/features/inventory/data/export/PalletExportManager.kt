// Полная и измененная версия файла: features/inventory/data/export/PalletExportManager.kt

package com.example.qrscannerapp.features.inventory.data.export

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.qrscannerapp.features.inventory.domain.model.StoragePallet // <-- Добавлен импорт
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

/**
Класс, отвечающий за логику генерации Excel-файлов для палетов.
Использует Apache POI.
 */
class PalletExportManager(private val context: Context) {

    // --- V ИЗМЕНЕНА ВСЯ ФУНКЦИЯ V ---
    /**
     * Генерирует книгу Excel на основе списка объектов StoragePallet.
     * @param pallets Список палетов для экспорта.
     */
    private fun generateWorkbook(pallets: List<StoragePallet>): XSSFWorkbook {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Сводный складской отчет")

        // Стили для заголовков
        val headerStyle = workbook.createCellStyle().apply {
            val font = workbook.createFont().apply {
                this.bold = true
                this.color = IndexedColors.WHITE.index
            }
            this.setFont(font)
            this.fillForegroundColor = IndexedColors.GREY_50_PERCENT.index
            this.fillPattern = FillPatternType.SOLID_FOREGROUND
        }

        // Определяем максимальное количество строк (ID аккумуляторов)
        val maxItemsCount = pallets.maxOfOrNull { it.items.size } ?: 0

        // 1. Создаем строки заголовков (Палет №1, Палет №2 и т.д.)
        val headerRow = sheet.createRow(0)
        pallets.forEachIndexed { colIndex, pallet ->
            val cell = headerRow.createCell(colIndex)
            cell.setCellValue("Палет №${pallet.palletNumber}")
            cell.cellStyle = headerStyle
            sheet.setColumnWidth(colIndex, 20 * 256) // Устанавливаем ширину колонки
        }

        // 2. Создаем строку для производителей
        val manufacturerRow = sheet.createRow(1)
        pallets.forEachIndexed { colIndex, pallet ->
            pallet.manufacturer?.let {
                manufacturerRow.createCell(colIndex).setCellValue(it)
            }
        }

        // 3. Заполняем данными (ID аккумуляторов), начиная с 3-й строки
        for (rowIndex in 0 until maxItemsCount) {
            val row = sheet.createRow(rowIndex + 3) // +3, чтобы пропустить заголовки и строку производителей
            pallets.forEachIndexed { colIndex, pallet ->
                if (rowIndex < pallet.items.size) {
                    row.createCell(colIndex).setCellValue(pallet.items[rowIndex])
                }
            }
        }
        return workbook
    }
    // --- ^ КОНЕЦ ИЗМЕНЕНИЙ ^ ---


    // --- V ИЗМЕНЕНА СИГНАТУРА ФУНКЦИИ V ---
    /**
    Экспорт в поток (используется для сохранения через SAF).
    @param pallets Список палетов для экспорта.
    @param outputStream Поток, куда нужно записать Excel-файл.
     */
    fun writeMasterListToStream(pallets: List<StoragePallet>, outputStream: OutputStream) {
        val workbook = generateWorkbook(pallets)
        try {
            outputStream.use {
                workbook.write(it)
            }
        } finally {
            workbook.close()
        }
    }
    // --- ^ КОНЕЦ ИЗМЕНЕНИЙ ^ ---


    // --- V ИЗМЕНЕНА СИГНАТУРА ФУНКЦИИ V ---
    /**
    Создает файл во временной папке и запускает меню "Поделиться".
     */
    fun shareMasterList(pallets: List<StoragePallet>) {
        if (pallets.all { it.items.isEmpty() }) {
            Toast.makeText(context, "Нет АКБ для экспорта.", Toast.LENGTH_SHORT).show()
            return
        }
        val workbook = generateWorkbook(pallets)
        val fileName = "master_pallet_export.xlsx"
        val file = File(context.cacheDir, fileName)
        try {
            FileOutputStream(file).use {
                workbook.write(it)
            }
            shareFile(file)
        } catch (e: Exception) {
            Toast.makeText(context, "Ошибка экспорта: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        } finally {
            workbook.close()
        }
    }
    // --- ^ КОНЕЦ ИЗМЕНЕНИЙ ^ ---

    private fun shareFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Экспорт склада"))
        } catch (e: IOException) {
            Toast.makeText(context, "Не удалось запустить меню 'Поделиться'.", Toast.LENGTH_LONG).show()
        }
    }
}