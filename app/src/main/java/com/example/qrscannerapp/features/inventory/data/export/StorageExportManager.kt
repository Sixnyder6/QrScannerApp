package com.example.qrscannerapp.features.inventory.data.export

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.qrscannerapp.StorageCell
import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StorageExportManager(private val context: Context) {

    // ========================================================================================
    // РЕЖИМ БЕЗ СОРТИРОВКИ
    // Экспортирует одну ячейку в одну колонку, порядок как хранится (новые сверху)
    // ========================================================================================

    fun exportCellAsIs(cell: StorageCell) {
        if (cell.items.isEmpty()) {
            Toast.makeText(context, "Ячейка пуста, нечего экспортировать.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val workbook = XSSFWorkbook()
            val sheetName = sanitizeSheetName(cell.name)
            val sheet = workbook.createSheet(sheetName)

            val orderedItems = cell.items.reversed()

            val headerFont = workbook.createFont().apply {
                bold = true
                fontHeightInPoints = 13.toShort()
                color = IndexedColors.WHITE.index
            }
            val headerStyle = workbook.createCellStyle().apply {
                setFont(headerFont)
                alignment = HorizontalAlignment.CENTER
                borderBottom = BorderStyle.THICK
                fillForegroundColor = IndexedColors.DARK_BLUE.index
                fillPattern = FillPatternType.SOLID_FOREGROUND
            }
            val directionHeaderStyle = workbook.createCellStyle().apply {
                setFont(headerFont)
                alignment = HorizontalAlignment.CENTER
                borderBottom = BorderStyle.THICK
                fillForegroundColor = IndexedColors.GREY_50_PERCENT.index
                fillPattern = FillPatternType.SOLID_FOREGROUND
            }
            val itemStyle = workbook.createCellStyle().apply {
                alignment = HorizontalAlignment.CENTER
                borderBottom = BorderStyle.THIN
                borderLeft = BorderStyle.THIN
                borderRight = BorderStyle.THIN
                borderTop = BorderStyle.THIN
            }
            val directionStyle = workbook.createCellStyle().apply {
                alignment = HorizontalAlignment.CENTER
                borderBottom = BorderStyle.THIN
                borderLeft = BorderStyle.THIN
                borderRight = BorderStyle.THIN
                borderTop = BorderStyle.THIN
            }

            sheet.setColumnWidth(0, 22 * 256)
            sheet.setColumnWidth(1, 10 * 256)

            val headerRow = sheet.createRow(0)
            val headerCell = headerRow.createCell(0)
            headerCell.setCellValue("${cell.name} — ${cell.description}")
            headerCell.cellStyle = headerStyle
            val dirHeaderCell = headerRow.createCell(1)
            dirHeaderCell.setCellValue("Напр.")
            dirHeaderCell.cellStyle = directionHeaderStyle

            orderedItems.forEachIndexed { index, scooterId ->
                val row = sheet.createRow(index + 1)
                val dataCell = row.createCell(0)
                dataCell.setCellValue(scooterId)
                dataCell.cellStyle = itemStyle
                val dirCell = row.createCell(1)
                dirCell.setCellValue(getDirectionArrows(cell, scooterId))
                dirCell.cellStyle = directionStyle
            }

            shareExcelFile(workbook, "raw_${sanitizeFileName(cell.name)}")

        } catch (e: Exception) {
            handleError(e)
        }
    }

    // ========================================================================================
    // РЕЖИМ ПЕЧАТИ
    // Одна ячейка, список разбит на столбцы по 50 штук. Данные сортируются.
    // Столбцы через один (A, C, E) — удобно резать бумагу
    // ========================================================================================

    fun exportCellForPrinting(cell: StorageCell) {
        if (cell.items.isEmpty()) {
            Toast.makeText(context, "Ячейка пуста, нечего печатать.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val workbook = XSSFWorkbook()
            val sheetName = sanitizeSheetName(cell.name)
            val sheet = workbook.createSheet(sheetName)

            val sortedItems = cell.items.sorted()
            val rowsPerColumn = 50
            val chunks = sortedItems.chunked(rowsPerColumn)

            val headerFont = workbook.createFont().apply {
                bold = true
                fontHeightInPoints = 12.toShort()
                color = IndexedColors.WHITE.index
            }
            val headerStyle = workbook.createCellStyle().apply {
                setFont(headerFont)
                alignment = HorizontalAlignment.CENTER
                borderBottom = BorderStyle.THICK
                fillForegroundColor = IndexedColors.DARK_BLUE.index
                fillPattern = FillPatternType.SOLID_FOREGROUND
            }
            val directionHeaderStyle = workbook.createCellStyle().apply {
                setFont(headerFont)
                alignment = HorizontalAlignment.CENTER
                borderBottom = BorderStyle.THICK
                fillForegroundColor = IndexedColors.GREY_50_PERCENT.index
                fillPattern = FillPatternType.SOLID_FOREGROUND
            }
            val itemStyle = workbook.createCellStyle().apply {
                alignment = HorizontalAlignment.CENTER
                borderBottom = BorderStyle.THIN
                borderLeft = BorderStyle.THIN
                borderRight = BorderStyle.THIN
                borderTop = BorderStyle.THIN
            }
            // Чётные строки — лёгкая заливка для читаемости при печати
            val itemStyleAlt = workbook.createCellStyle().apply {
                alignment = HorizontalAlignment.CENTER
                borderBottom = BorderStyle.THIN
                borderLeft = BorderStyle.THIN
                borderRight = BorderStyle.THIN
                borderTop = BorderStyle.THIN
                fillForegroundColor = IndexedColors.LIGHT_CORNFLOWER_BLUE.index
                fillPattern = FillPatternType.SOLID_FOREGROUND
            }

            val directionStyle = workbook.createCellStyle().apply {
                alignment = HorizontalAlignment.CENTER
                borderBottom = BorderStyle.THIN
                borderLeft = BorderStyle.THIN
                borderRight = BorderStyle.THIN
                borderTop = BorderStyle.THIN
            }

            chunks.forEachIndexed { chunkIndex, chunkItems ->
                val colIndex = chunkIndex * 3  // было *2, теперь *3: код | направление | разделитель
                sheet.setColumnWidth(colIndex, 16 * 256)
                sheet.setColumnWidth(colIndex + 1, 7 * 256)  // столбец направления (узкий)
                if (chunkIndex > 0) {
                    // Разделитель между группами
                    sheet.setColumnWidth(colIndex - 1, 2 * 256)
                }

                val headerRow = sheet.getRow(0) ?: sheet.createRow(0)
                val headerCell = headerRow.createCell(colIndex)
                headerCell.setCellValue("${cell.name} (${chunkIndex + 1}/${chunks.size})")
                headerCell.cellStyle = headerStyle
                val dirHeaderCell = headerRow.createCell(colIndex + 1)
                dirHeaderCell.setCellValue("→")
                dirHeaderCell.cellStyle = directionHeaderStyle

                chunkItems.forEachIndexed { itemIndex, scooterId ->
                    val rowIndex = itemIndex + 1
                    val row = sheet.getRow(rowIndex) ?: sheet.createRow(rowIndex)
                    val dataCell = row.createCell(colIndex)
                    dataCell.setCellValue(scooterId)
                    dataCell.cellStyle = if (itemIndex % 2 == 0) itemStyle else itemStyleAlt
                    val dirCell = row.createCell(colIndex + 1)
                    dirCell.setCellValue(getDirectionArrows(cell, scooterId))
                    dirCell.cellStyle = directionStyle
                }
            }

            shareExcelFile(workbook, "print_${sanitizeFileName(cell.name)}")

        } catch (e: Exception) {
            handleError(e)
        }
    }

    // ========================================================================================
    // ОБЩИЙ ЭКСПОРТ (БЭКАП)
    // Все ячейки, каждая в своём столбце
    // ========================================================================================

    fun exportAllCellsToExcel(cells: List<StorageCell>) {
        if (cells.isEmpty()) {
            Toast.makeText(context, "Нет ячеек для экспорта.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("Весь склад")

            val headerFont = workbook.createFont().apply {
                bold = true
                color = IndexedColors.WHITE.index
            }
            val headerStyle = workbook.createCellStyle().apply {
                setFont(headerFont)
                alignment = HorizontalAlignment.CENTER
                fillForegroundColor = IndexedColors.DARK_BLUE.index
                fillPattern = FillPatternType.SOLID_FOREGROUND
                borderBottom = BorderStyle.THICK
            }
            val directionHeaderStyle = workbook.createCellStyle().apply {
                setFont(headerFont)
                alignment = HorizontalAlignment.CENTER
                fillForegroundColor = IndexedColors.GREY_50_PERCENT.index
                fillPattern = FillPatternType.SOLID_FOREGROUND
                borderBottom = BorderStyle.THICK
            }
            val descStyle = workbook.createCellStyle().apply {
                val font = workbook.createFont().apply {
                    italic = true
                    color = IndexedColors.GREY_50_PERCENT.index
                }
                setFont(font)
                alignment = HorizontalAlignment.CENTER
            }
            val itemStyle = workbook.createCellStyle().apply {
                alignment = HorizontalAlignment.CENTER
                borderBottom = BorderStyle.THIN
                borderLeft = BorderStyle.THIN
                borderRight = BorderStyle.THIN
                borderTop = BorderStyle.THIN
            }
            val directionStyle = workbook.createCellStyle().apply {
                alignment = HorizontalAlignment.CENTER
                borderBottom = BorderStyle.THIN
                borderLeft = BorderStyle.THIN
                borderRight = BorderStyle.THIN
                borderTop = BorderStyle.THIN
            }

            cells.forEachIndexed { cellIndex, cell ->
                val codeCol = cellIndex * 2      // столбец кода
                val dirCol = codeCol + 1         // столбец направления
                sheet.setColumnWidth(codeCol, 22 * 256)
                sheet.setColumnWidth(dirCol, 10 * 256)

                val row0 = sheet.getRow(0) ?: sheet.createRow(0)
                row0.createCell(codeCol).apply {
                    setCellValue(cell.name)
                    cellStyle = headerStyle
                }
                row0.createCell(dirCol).apply {
                    setCellValue("Напр.")
                    cellStyle = directionHeaderStyle
                }

                val row1 = sheet.getRow(1) ?: sheet.createRow(1)
                row1.createCell(codeCol).apply {
                    setCellValue(cell.description)
                    cellStyle = descStyle
                }
                row1.createCell(dirCol).apply {
                    setCellValue("")
                    cellStyle = descStyle
                }

                // Счётчик: "12 / 600"
                val row2 = sheet.getRow(2) ?: sheet.createRow(2)
                row2.createCell(codeCol).apply {
                    setCellValue("${cell.items.size} / ${cell.capacity}")
                    cellStyle = descStyle
                }
                row2.createCell(dirCol).apply {
                    setCellValue("")
                    cellStyle = descStyle
                }

                val sortedItems = cell.items.sorted()
                sortedItems.forEachIndexed { itemIndex, scooterId ->
                    val rIndex = itemIndex + 3
                    val row = sheet.getRow(rIndex) ?: sheet.createRow(rIndex)
                    row.createCell(codeCol).apply {
                        setCellValue(scooterId)
                        cellStyle = itemStyle
                    }
                    row.createCell(dirCol).apply {
                        setCellValue(getDirectionArrows(cell, scooterId))
                        cellStyle = directionStyle
                    }
                }
            }

            shareExcelFile(workbook, "full_storage_backup")

        } catch (e: Exception) {
            handleError(e)
        }
    }

    // ========================================================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // ========================================================================================

    /**
     * Санирует имя листа Excel:
     * - убирает запрещённые символы: : \ / * ? [ ] '
     * - обрезает до 31 символа (лимит Excel)
     */
    private fun sanitizeSheetName(name: String): String {
        return name
            .replace(Regex("[:\\\\/*?\\[\\]']"), " ")
            .trim()
            .take(31)
            .ifBlank { "Лист" }
    }

    /**
     * Санирует имя файла для файловой системы:
     * - убирает все символы кроме букв, цифр, дефиса, подчёркивания
     * - обрезает до 50 символов
     */
    private fun sanitizeFileName(name: String): String {
        return name
            .replace(Regex("[^a-zA-Z0-9а-яА-ЯёЁ_\\-]"), "_")
            .trim('_')
            .take(50)
            .ifBlank { "cell" }
    }

    private fun shareExcelFile(workbook: XSSFWorkbook, fileNamePrefix: String) {
        val sdf = SimpleDateFormat("ddMM_HHmm", Locale.getDefault())
        val timestamp = sdf.format(Date())
        val safePrefix = sanitizeFileName(fileNamePrefix)
        val fileName = "${safePrefix}_$timestamp.xlsx"

        val outputDir = File(context.cacheDir, "exports").also { it.mkdirs() }
        val file = File(outputDir, fileName)

        FileOutputStream(file).use { fos ->
            workbook.write(fos)
            fos.flush()
        }
        workbook.close()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, fileName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Сохранить / Отправить"))
    }

    /**
     * Возвращает строку со стрелочками направления для кода самоката.
     * Пример: "↑←" для UP и LEFT, "↓" для DOWN, "" если направлений нет.
     */
    private fun getDirectionArrows(cell: StorageCell, scooterId: String): String {
        val directions = cell.stickerDirections?.get(scooterId) ?: return ""
        return directions.joinToString("") { dir ->
            when (dir.uppercase()) {
                "UP" -> "\u2191"      // ↑
                "DOWN" -> "\u2193"    // ↓
                "LEFT" -> "\u2190"    // ←
                "RIGHT" -> "\u2192"   // →
                else -> ""
            }
        }
    }

    private fun handleError(e: Exception) {
        e.printStackTrace()
        val msg = when {
            e.message?.contains("ENOSPC") == true -> "Недостаточно места на устройстве"
            e.message?.contains("permission") == true -> "Нет прав доступа к файлу"
            else -> "Ошибка экспорта: ${e.message}"
        }
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
    }
}