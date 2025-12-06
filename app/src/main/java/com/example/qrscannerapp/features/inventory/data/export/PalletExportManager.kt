// Полная и исправленная версия файла: features/inventory/data/export/PalletExportManager.kt

package com.example.qrscannerapp.features.inventory.data.export

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.qrscannerapp.features.inventory.domain.model.StoragePallet
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

/**
 * Класс, отвечающий за логику генерации Excel-файлов для палетов.
 * Исправлено: Убрано дублирующее закрытие потока (double-close), что могло бить файлы.
 */
class PalletExportManager(private val context: Context) {

    /**
     * Генерирует книгу Excel на основе списка объектов StoragePallet.
     */
    private fun generateWorkbook(pallets: List<StoragePallet>): XSSFWorkbook {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Сводный складской отчет")

        // --- 1. СТИЛИ ---
        val boldFont = workbook.createFont().apply {
            this.bold = true
            this.fontHeightInPoints = 12.toShort()
        }

        val headerFont = workbook.createFont().apply {
            this.bold = true
            this.color = IndexedColors.WHITE.index
        }

        val titleStyle = workbook.createCellStyle().apply {
            this.setFont(boldFont)
        }

        val headerStyle = workbook.createCellStyle().apply {
            this.setFont(headerFont)
            this.fillForegroundColor = IndexedColors.GREY_50_PERCENT.index
            this.fillPattern = FillPatternType.SOLID_FOREGROUND
        }

        // --- 2. ПОДСЧЕТ СТАТИСТИКИ ---
        val totalCount = pallets.sumOf { it.items.size }
        val fujianCount = pallets.filter { it.manufacturer?.uppercase() == "FUJIAN" }.sumOf { it.items.size }
        val bydCount = pallets.filter { it.manufacturer?.uppercase() == "BYD" }.sumOf { it.items.size }
        val otherCount = totalCount - (fujianCount + bydCount)
        val maxItemsCount = pallets.maxOfOrNull { it.items.size } ?: 0

        // --- 3. РИСУЕМ ШАПКУ (ДАШБОРД) ---
        // Строка 0: Заголовок
        val rowTitle = sheet.createRow(0)
        rowTitle.createCell(0).apply {
            setCellValue("СВОДНЫЙ ОТЧЕТ ПО СКЛАДУ")
            cellStyle = titleStyle
        }

        // Строка 1: Всего
        val rowTotal = sheet.createRow(1)
        rowTotal.createCell(0).setCellValue("Всего АКБ:")
        rowTotal.createCell(1).apply {
            setCellValue(totalCount.toString())
            cellStyle = titleStyle
        }

        // Строка 2: Fujian
        val rowFujian = sheet.createRow(2)
        rowFujian.createCell(0).setCellValue("FUJIAN:")
        rowFujian.createCell(1).apply {
            setCellValue(fujianCount.toString())
            cellStyle = titleStyle
        }

        // Строка 3: BYD
        val rowByd = sheet.createRow(3)
        rowByd.createCell(0).setCellValue("BYD:")
        rowByd.createCell(1).apply {
            setCellValue(bydCount.toString())
            cellStyle = titleStyle
        }

        // Если есть "другие"
        if (otherCount > 0) {
            val rowOther = sheet.createRow(4)
            rowOther.createCell(0).setCellValue("Без маркировки:")
            rowOther.createCell(1).setCellValue(otherCount.toString())
        }

        // Отступ перед таблицей (строк)
        val tableStartRow = 6

        // --- 4. ЗАГОЛОВКИ ТАБЛИЦЫ ---
        val headerRow = sheet.createRow(tableStartRow)
        pallets.forEachIndexed { colIndex, pallet ->
            val cell = headerRow.createCell(colIndex)
            cell.setCellValue("Палет №${pallet.palletNumber}")
            cell.cellStyle = headerStyle
            sheet.setColumnWidth(colIndex, 20 * 256)
        }

        // --- 5. ПРОИЗВОДИТЕЛИ ---
        val manufacturerRow = sheet.createRow(tableStartRow + 1)
        pallets.forEachIndexed { colIndex, pallet ->
            pallet.manufacturer?.let {
                manufacturerRow.createCell(colIndex).setCellValue(it)
            }
        }

        // --- 6. ЗАПОЛНЕНИЕ ID ---
        for (itemIndex in 0 until maxItemsCount) {
            val rowIndex = tableStartRow + 2 + itemIndex
            val row = sheet.createRow(rowIndex)

            pallets.forEachIndexed { colIndex, pallet ->
                if (itemIndex < pallet.items.size) {
                    row.createCell(colIndex).setCellValue(pallet.items[itemIndex])
                }
            }
        }

        return workbook
    }

    /**
    Экспорт в поток (Сохранить как файл).
    ИСПРАВЛЕНО: Убран блок .use{}, чтобы поток не закрывался раньше времени.
     */
    fun writeMasterListToStream(pallets: List<StoragePallet>, outputStream: OutputStream) {
        val workbook = generateWorkbook(pallets)
        // Не используем здесь outputStream.use, так как поток закрывается уровнем выше (в Screen)
        workbook.write(outputStream)
        workbook.close()
    }

    /**
    Создает файл в кэше и запускает меню "Поделиться".
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