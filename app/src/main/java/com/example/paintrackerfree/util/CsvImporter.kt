package com.example.paintrackerfree.util

import android.content.Context
import android.net.Uri
import com.example.paintrackerfree.data.model.PainEntry
import java.text.SimpleDateFormat
import java.util.Locale

object CsvImporter {

    sealed class Result {
        data class Success(val entries: List<PainEntry>, val skipped: Int) : Result()
        data class Failure(val message: String) : Result()
    }

    fun parseCsv(context: Context, uri: Uri): Result {
        val entries = mutableListOf<PainEntry>()
        var skipped = 0

        val lines = try {
            context.contentResolver.openInputStream(uri)
                ?.bufferedReader()
                ?.readLines()
                ?: return Result.Failure("Could not open file")
        } catch (e: Exception) {
            return Result.Failure(e.message ?: "Unknown error")
        }

        if (lines.isEmpty()) return Result.Failure("File is empty")

        // Skip header row
        for (line in lines.drop(1)) {
            if (line.isBlank()) continue
            val cols = parseCsvLine(line)
            if (cols.size < 9) { skipped++; continue }

            val timestamp = try {
                val csvDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                csvDateFormat.parse(cols[0].trim())?.time
            } catch (_: Exception) { null }
            if (timestamp == null) { skipped++; continue }

            val painLevel = cols[1].trim().toIntOrNull()?.coerceIn(0, 10)
            if (painLevel == null) { skipped++; continue }

            entries.add(
                PainEntry(
                    timestamp = timestamp,
                    painLevel = painLevel,
                    locations = cols[2].trim(),
                    painTypes = cols[3].trim(),
                    triggers = cols[4].trim(),
                    medications = cols[5].trim(),
                    mood = cols[6].trim().toIntOrNull()?.coerceIn(1, 5) ?: 3,
                    sleepQuality = cols[7].trim().toIntOrNull()?.coerceIn(1, 5) ?: 3,
                    notes = cols[8].trim()
                )
            )
        }

        return Result.Success(entries, skipped)
    }

    // Parses a single CSV line respecting quoted fields (handles embedded commas and escaped quotes)
    private fun parseCsvLine(line: String): List<String> {
        val cols = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            when (val c = line[i]) {
                '"' if !inQuotes -> inQuotes = true
                '"' if i + 1 < line.length && line[i + 1] == '"' -> {
                    sb.append('"'); i++ // escaped quote inside quoted field
                }
                '"' if true -> inQuotes = false
                ',' if !inQuotes -> { cols.add(sb.toString()); sb.clear() }
                else -> sb.append(c)
            }
            i++
        }
        cols.add(sb.toString())
        return cols
    }
}
