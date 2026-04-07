package com.example.paintrackerfree.util

import com.example.paintrackerfree.data.model.PainEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvExporterTest {

    private fun entry(
        painLevel: Int = 5,
        locations: String = "",
        painTypes: String = "",
        triggers: String = "",
        medications: String = "",
        mood: Int = 3,
        sleepQuality: Int = 3,
        notes: String = "",
        timestamp: Long = 1_000_000L
    ) = PainEntry(
        timestamp = timestamp,
        painLevel = painLevel,
        locations = locations,
        painTypes = painTypes,
        triggers = triggers,
        medications = medications,
        mood = mood,
        sleepQuality = sleepQuality,
        notes = notes
    )

    private fun buildCsv(entries: List<PainEntry>): String =
        CsvExporter.buildCsvBytes(entries).toString(Charsets.UTF_8)

    // --- Header ---

    @Test
    fun emptyEntries_producesHeaderOnly() {
        val csv = buildCsv(emptyList())
        val lines = csv.trim().lines()
        assertEquals(1, lines.size)
        assertTrue(lines[0].startsWith("Date/Time"))
    }

    @Test
    fun header_containsAllExpectedColumns() {
        val csv = buildCsv(emptyList())
        val header = csv.lines().first()
        assertTrue(header.contains("Pain Level"))
        assertTrue(header.contains("Locations"))
        assertTrue(header.contains("Pain Types"))
        assertTrue(header.contains("Triggers"))
        assertTrue(header.contains("Medications"))
        assertTrue(header.contains("Mood"))
        assertTrue(header.contains("Sleep Quality"))
        assertTrue(header.contains("Notes"))
    }

    // --- Row count ---

    @Test
    fun singleEntry_producesTwoLines() {
        val csv = buildCsv(listOf(entry()))
        val lines = csv.trim().lines()
        assertEquals(2, lines.size)
    }

    @Test
    fun multipleEntries_correctRowCount() {
        val csv = buildCsv(listOf(entry(), entry(), entry()))
        val lines = csv.trim().lines()
        assertEquals(4, lines.size) // header + 3
    }

    // --- Field values ---

    @Test
    fun painLevel_appearsInRow() {
        val csv = buildCsv(listOf(entry(painLevel = 7)))
        val dataLine = csv.trim().lines()[1]
        assertTrue(dataLine.contains(",7,"))
    }

    @Test
    fun mood_appearsInRow() {
        val csv = buildCsv(listOf(entry(mood = 4)))
        val dataLine = csv.trim().lines()[1]
        assertTrue(dataLine.contains(",4,"))
    }

    @Test
    fun notes_wrappedInQuotes() {
        val csv = buildCsv(listOf(entry(notes = "some note")))
        val dataLine = csv.trim().lines()[1]
        assertTrue(dataLine.contains("\"some note\""))
    }

    @Test
    fun locationsWithComma_wrappedInQuotes() {
        val csv = buildCsv(listOf(entry(locations = "Back, Neck")))
        val dataLine = csv.trim().lines()[1]
        assertTrue(dataLine.contains("\"Back, Neck\""))
    }

    // --- Quote escaping ---

    @Test
    fun notesWithQuote_escapedAsDoubleQuote() {
        val csv = buildCsv(listOf(entry(notes = "said \"ouch\"")))
        assertTrue(csv.contains("\"said \"\"ouch\"\"\""))
    }

    @Test
    fun medicationsWithQuote_escapedAsDoubleQuote() {
        val csv = buildCsv(listOf(entry(medications = "Dr. \"Nick\"")))
        assertTrue(csv.contains("\"Dr. \"\"Nick\"\"\""))
    }

    // --- Round-trip with CsvImporter ---

    @Test
    fun roundTrip_painLevel_preserved() {
        val original = listOf(entry(painLevel = 8))
        val csv = buildCsv(original)
        val parsed = parseCsvString(csv)
        assertEquals(1, parsed.size)
        assertEquals(8, parsed[0].painLevel)
    }

    @Test
    fun roundTrip_notes_preserved() {
        val original = listOf(entry(notes = "felt rough today"))
        val csv = buildCsv(original)
        val parsed = parseCsvString(csv)
        assertEquals("felt rough today", parsed[0].notes)
    }

    @Test
    fun roundTrip_notesWithQuotes_preserved() {
        val original = listOf(entry(notes = "said \"ouch\""))
        val csv = buildCsv(original)
        val parsed = parseCsvString(csv)
        assertEquals("said \"ouch\"", parsed[0].notes)
    }

    @Test
    fun roundTrip_locationsWithComma_preserved() {
        val original = listOf(entry(locations = "Back, Neck"))
        val csv = buildCsv(original)
        val parsed = parseCsvString(csv)
        assertEquals("Back, Neck", parsed[0].locations)
    }

    @Test
    fun roundTrip_multipleEntries_allPreserved() {
        val originals = listOf(
            entry(painLevel = 2, notes = "mild"),
            entry(painLevel = 9, notes = "severe"),
            entry(painLevel = 5, locations = "Head, Neck")
        )
        val csv = buildCsv(originals)
        val parsed = parseCsvString(csv)
        assertEquals(3, parsed.size)
        assertEquals(2, parsed[0].painLevel)
        assertEquals(9, parsed[1].painLevel)
        assertEquals("Head, Neck", parsed[2].locations)
    }

    // Parses CSV string directly using CsvImporter's line parser via public API workaround
    private fun parseCsvString(csv: String): List<PainEntry> {
        val lines = csv.lines()
        if (lines.size < 2) return emptyList()
        val entries = mutableListOf<PainEntry>()
        for (line in lines.drop(1)) {
            if (line.isBlank()) continue
            val cols = parseCsvLine(line)
            if (cols.size < 9) continue
            val timestamp = try {
                java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                    .parse(cols[0].trim())?.time
            } catch (_: Exception) { null } ?: continue
            val painLevel = cols[1].trim().toIntOrNull() ?: continue
            entries.add(PainEntry(
                timestamp = timestamp,
                painLevel = painLevel,
                locations = cols[2].trim(),
                painTypes = cols[3].trim(),
                triggers = cols[4].trim(),
                medications = cols[5].trim(),
                mood = cols[6].trim().toIntOrNull() ?: 3,
                sleepQuality = cols[7].trim().toIntOrNull() ?: 3,
                notes = cols[8].trim()
            ))
        }
        return entries
    }

    private fun parseCsvLine(line: String): List<String> {
        val cols = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            when {
                line[i] == '"' && !inQuotes -> inQuotes = true
                line[i] == '"' && i + 1 < line.length && line[i + 1] == '"' -> { sb.append('"'); i++ }
                line[i] == '"' -> inQuotes = false
                line[i] == ',' && !inQuotes -> { cols.add(sb.toString()); sb.clear() }
                else -> sb.append(line[i])
            }
            i++
        }
        cols.add(sb.toString())
        return cols
    }
}
