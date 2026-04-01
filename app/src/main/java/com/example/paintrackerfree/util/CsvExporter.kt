package com.example.paintrackerfree.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.paintrackerfree.data.model.PainEntry
import java.io.File
import java.io.IOException

object CsvExporter {

    fun buildShareIntent(context: Context, entries: List<PainEntry>): Intent {
        val file = writeCsvToCache(context, entries)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "Pain Journal Export")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * Saves the CSV directly to the public Downloads folder.
     * Returns the file name on success, or null on failure.
     */
    fun saveToDownloads(context: Context, entries: List<PainEntry>): String? {
        val fileName = "pain_journal_${System.currentTimeMillis()}.csv"
        val csvContent = buildCsvContent(entries)
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return null
                resolver.openOutputStream(uri)?.use { it.write(csvContent.toByteArray()) }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            } else {
                @Suppress("DEPRECATION")
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                dir.mkdirs()
                File(dir, fileName).writeText(csvContent)
            }
            fileName
        } catch (e: IOException) {
            null
        }
    }

    private fun writeCsvToCache(context: Context, entries: List<PainEntry>): File {
        val file = File(
            context.getExternalFilesDir(null),
            "pain_journal_${System.currentTimeMillis()}.csv"
        )
        file.writeText(buildCsvContent(entries))
        return file
    }

    private fun buildCsvContent(entries: List<PainEntry>): String {
        val sb = StringBuilder()
        sb.appendLine("Date/Time,Pain Level,Locations,Pain Types,Triggers,Medications,Mood (1-5),Sleep Quality (1-5),Notes")
        entries.forEach { e ->
            sb.appendLine(
                "${DateUtils.formatCsv(e.timestamp)}," +
                        "${e.painLevel}," +
                        "\"${e.locations}\"," +
                        "\"${e.painTypes}\"," +
                        "\"${e.triggers}\"," +
                        "\"${e.medications.replace("\"", "\"\"")}\"," +
                        "${e.mood}," +
                        "${e.sleepQuality}," +
                        "\"${e.notes.replace("\"", "\"\"")}\""
            )
        }
        return sb.toString()
    }
}
