package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.domain.model.TransactionItem
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility for exporting transaction history to RFC 4180 compliant CSV format,
 * optimized for review in spreadsheet applications such as Microsoft Excel,
 * Google Sheets, and LibreOffice Calc.
 */
object TransactionCsvExporter {

    private const val CSV_HEADER = "ID,Tanggal,Waktu,Tipe Transaksi,Kategori,Jumlah (IDR),Rekening Sumber,Rekening Tujuan,Catatan"

    /**
     * Serializes a list of TransactionItem into an RFC 4180 compliant CSV string.
     * Amounts are formatted with period decimals and without thousand separators,
     * allowing spreadsheet software to immediately interpret them as numeric values.
     */
    fun exportToCsv(transactions: List<TransactionItem>): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        val builder = StringBuilder()
        builder.append(CSV_HEADER).append("\r\n")

        for (item in transactions) {
            val dateObj = Date(item.dateMillis)
            val dateStr = dateFormat.format(dateObj)
            val timeStr = timeFormat.format(dateObj)
            val typeStr = item.type.label
            val categoryStr = escapeCsv(item.category)
            val amountStr = String.format(Locale.US, "%.2f", item.amount)
            val sourceStr = escapeCsv(item.sourceAccountName ?: "-")
            val destStr = escapeCsv(item.destinationAccountName ?: "-")
            val noteStr = escapeCsv(item.note)

            builder.append(item.id).append(",")
                .append(dateStr).append(",")
                .append(timeStr).append(",")
                .append(escapeCsv(typeStr)).append(",")
                .append(categoryStr).append(",")
                .append(amountStr).append(",")
                .append(sourceStr).append(",")
                .append(destStr).append(",")
                .append(noteStr)
                .append("\r\n")
        }

        return builder.toString()
    }

    /**
     * Escapes a string for CSV format according to RFC 4180:
     * - Double quotes are escaped with two double quotes ("")
     * - If the field contains commas, double quotes, or newlines, it is enclosed in double quotes.
     */
    fun escapeCsv(value: String): String {
        val trimmed = value.trim()
        val needsQuotes = trimmed.contains(',') ||
                trimmed.contains('"') ||
                trimmed.contains('\n') ||
                trimmed.contains('\r') ||
                trimmed.contains(';')

        return if (needsQuotes) {
            "\"" + trimmed.replace("\"", "\"\"") + "\""
        } else {
            trimmed
        }
    }

    /**
     * Generates a standardized filename for exported CSV files.
     * Example: aynaas_transaksi_20260908_153000.csv
     */
    fun generateCsvFileName(timestampMillis: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val formattedDate = sdf.format(Date(timestampMillis))
        return "aynaas_transaksi_$formattedDate.csv"
    }

    /**
     * Writes CSV string to a SAF-provided Document Uri.
     */
    fun writeCsvToUri(context: Context, uri: Uri, csvString: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri, "wt")?.use { outputStream ->
                OutputStreamWriter(outputStream, StandardCharsets.UTF_8).use { writer ->
                    writer.write(csvString)
                    writer.flush()
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Saves CSV string to the app's local document storage directory.
     */
    fun saveToAppLocalCsvDir(context: Context, fileName: String, csvString: String): File {
        val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: context.filesDir

        val exportDir = File(baseDir, "exports").apply {
            if (!exists()) mkdirs()
        }

        val targetFile = File(exportDir, fileName)
        FileOutputStream(targetFile).use { fos ->
            OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                writer.write(csvString)
                writer.flush()
            }
        }
        return targetFile
    }

    /**
     * Shares the CSV file via the Android Sharesheet.
     */
    fun shareCsv(context: Context, csvString: String, fileName: String) {
        try {
            val cacheDir = File(context.cacheDir, "shared_exports").apply {
                if (!exists()) mkdirs()
            }
            val tempFile = File(cacheDir, fileName)
            FileOutputStream(tempFile).use { fos ->
                OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                    writer.write(csvString)
                    writer.flush()
                }
            }

            val authority = "${context.packageName}.provider"
            val contentUri: Uri = FileProvider.getUriForFile(context, authority, tempFile)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "Ekspor Transaksi AYNAAS Finance (CSV)")
                putExtra(Intent.EXTRA_TEXT, "Berikut berkas ekspor transaksi AYNAAS Finance format CSV.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Bagikan Ekspor Transaksi (CSV)")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            // Fallback to plain text sharing if FileProvider fails
            val textIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Ekspor Transaksi AYNAAS Finance")
                putExtra(Intent.EXTRA_TEXT, csvString)
            }
            val chooser = Intent.createChooser(textIntent, "Bagikan Teks CSV")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        }
    }

    /**
     * Copies CSV content to the clipboard.
     */
    fun copyToClipboard(context: Context, csvString: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText("AYNAAS Transaksi CSV", csvString)
        clipboard?.setPrimaryClip(clip)
        Toast.makeText(context, "Data CSV transaksi disalin ke papan klip", Toast.LENGTH_SHORT).show()
    }
}
