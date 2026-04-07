package com.example.paintrackerfree.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.core.graphics.toColorInt
import com.example.paintrackerfree.data.model.PainEntry
import java.io.File
import java.io.IOException

object PdfExporter {

    // A4 at 72 dpi
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 48f
    private const val CONTENT_WIDTH = PAGE_WIDTH - MARGIN * 2

    fun buildShareIntent(context: Context, entries: List<PainEntry>): Intent {
        val file = writePdfToCache(context, entries)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_SUBJECT, "Pain Journal Export")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun saveToDownloads(context: Context, entries: List<PainEntry>): String? {
        val fileName = "pain_journal_${System.currentTimeMillis()}.pdf"
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return null
                resolver.openOutputStream(uri)?.use { buildPdf(entries).writeTo(it) }
                buildPdf(entries).close()
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            } else {
                @Suppress("DEPRECATION")
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                dir.mkdirs()
                File(dir, fileName).outputStream().use { buildPdf(entries).writeTo(it) }
                buildPdf(entries).close()
            }
            fileName
        } catch (_: IOException) {
            null
        }
    }

    private fun writePdfToCache(context: Context, entries: List<PainEntry>): File {
        val dir = context.getExternalFilesDir("exports")?.also { it.mkdirs() }
        val file = File(dir, "pain_journal_${System.currentTimeMillis()}.pdf")
        file.outputStream().use { buildPdf(entries).writeTo(it) }
        return file
    }

    private fun buildPdf(entries: List<PainEntry>): PdfDocument {
        val allSorted = entries.sortedBy { it.timestamp }
        val now = System.currentTimeMillis()
        val doc = PdfDocument()

        // --- Shared paints ---
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 22f; color = "#1A1A2E".toColorInt(); isFakeBoldText = true
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 11f; color = "#757575".toColorInt()
        }
        val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 9f; color = "#5C8BE0".toColorInt()
            isFakeBoldText = true; letterSpacing = 0.08f
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 10f; color = "#1A1A2E".toColorInt()
        }
        val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 9f; color = "#757575".toColorInt()
        }
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 13f; color = Color.WHITE
            isFakeBoldText = true; textAlign = Paint.Align.CENTER
        }
        val dividerPaint = Paint().apply {
            color = "#E0E0E0".toColorInt(); strokeWidth = 0.5f
        }
        val statMutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 9f; color = "#757575".toColorInt()
        }
        val statBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 9f; color = "#1A1A2E".toColorInt()
        }

        var pageNum = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
        var page = doc.startPage(pageInfo)
        var canvas: Canvas = page.canvas
        var y = MARGIN

        fun finishPage() {
            val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 9f; color = "#9E9E9E".toColorInt(); textAlign = Paint.Align.CENTER
            }
            canvas.drawText("Pain Tracker  •  Page $pageNum", PAGE_WIDTH / 2f, PAGE_HEIGHT - 24f, footerPaint)
            doc.finishPage(page)
        }

        fun newPage() {
            finishPage()
            pageNum++
            pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
            page = doc.startPage(pageInfo)
            canvas = page.canvas
            y = MARGIN
        }

        fun ensureSpace(needed: Float) {
            if (y + needed > PAGE_HEIGHT - MARGIN - 20f) newPage()
        }

        // Draws a trend chart + stats block for the given slice of entries.
        fun drawTrendSection(label: String, slice: List<PainEntry>) {
            ensureSpace(220f)

            // Section heading
            canvas.drawText(label, MARGIN, y + sectionPaint.textSize, sectionPaint)
            y += sectionPaint.textSize + 8f

            if (slice.size < 2) {
                canvas.drawText(
                    if (slice.isEmpty()) "No entries in this period." else "Only 1 entry — not enough data to chart.",
                    MARGIN, y + mutedPaint.textSize, mutedPaint
                )
                y += mutedPaint.textSize + 12f
                canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, dividerPaint)
                y += 16f
                return
            }

            // Chart geometry
            val chartH = 120f
            val padL = 36f; val padR = 8f; val padT = 6f; val padB = 22f
            val chartLeft = MARGIN + padL
            val chartRight = PAGE_WIDTH.toFloat() - MARGIN - padR
            val chartTop = y + padT
            val chartBottom = y + padT + chartH - padB
            val chartW = chartRight - chartLeft

            val gridPaint = Paint().apply { color = "#E8E8E8".toColorInt(); strokeWidth = 0.5f }
            val axisLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 7f; color = "#9E9E9E".toColorInt(); textAlign = Paint.Align.RIGHT
            }
            for (i in 0..10 step 2) {
                val gy = chartBottom - (i / 10f) * (chartBottom - chartTop)
                canvas.drawLine(chartLeft, gy, chartRight, gy, gridPaint)
                canvas.drawText(i.toString(), chartLeft - 4f, gy + axisLabelPaint.textSize / 2.5f, axisLabelPaint)
            }

            // Aggregate: average pain per calendar day to eliminate intra-day vertical spikes
            val dayFormat = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US)
            val dailyPoints: List<Pair<Long, Float>> = slice
                .groupBy { dayFormat.format(java.util.Date(it.timestamp)) }
                .entries
                .sortedBy { it.key }
                .map { (_, dayEntries) ->
                    val midTimestamp = dayEntries.map { it.timestamp }.average().toLong()
                    val avgLevel = dayEntries.map { it.painLevel }.average().toFloat()
                    midTimestamp to avgLevel
                }

            val minT = dailyPoints.first().first.toFloat()
            val maxT = dailyPoints.last().first.toFloat()
            val rangeT = if (maxT > minT) maxT - minT else 1f
            fun xOf(ts: Long) = chartLeft + (ts - minT) / rangeT * chartW
            fun yOf(lvl: Float) = chartBottom - (lvl / 10f) * (chartBottom - chartTop)

            // Filled area — explicit baseline edges so the fill never leaks
            val fillPath = android.graphics.Path()
            fillPath.moveTo(xOf(dailyPoints.first().first), chartBottom)
            dailyPoints.forEach { (ts, lvl) -> fillPath.lineTo(xOf(ts), yOf(lvl)) }
            fillPath.lineTo(xOf(dailyPoints.last().first), chartBottom)
            fillPath.lineTo(xOf(dailyPoints.first().first), chartBottom)
            fillPath.close()
            canvas.drawPath(fillPath, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = "#285C8BE0".toColorInt()
            })

            // Line
            val linePath = android.graphics.Path()
            dailyPoints.forEachIndexed { i, (ts, lvl) ->
                if (i == 0) linePath.moveTo(xOf(ts), yOf(lvl))
                else linePath.lineTo(xOf(ts), yOf(lvl))
            }
            canvas.drawPath(linePath, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE; color = "#5C8BE0".toColorInt()
                strokeWidth = 2f; strokeJoin = Paint.Join.ROUND; strokeCap = Paint.Cap.ROUND
            })

            // Dots — coloured by the averaged pain level for the day
            val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
            dailyPoints.forEach { (ts, lvl) ->
                dotPaint.color = when {
                    lvl <= 3f -> "#4CAF50".toColorInt()
                    lvl <= 6f -> "#FF9800".toColorInt()
                    else -> "#F44336".toColorInt()
                }
                canvas.drawCircle(xOf(ts), yOf(lvl), 3f, dotPaint)
            }

            // X-axis labels (one per day point, thinned to ~5)
            val xLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 7f; color = "#9E9E9E".toColorInt(); textAlign = Paint.Align.CENTER
            }
            val step = maxOf(1, dailyPoints.size / 5)
            dailyPoints.filterIndexed { i, _ -> i % step == 0 || i == dailyPoints.size - 1 }
                .forEach { (ts, _) ->
                    canvas.drawText(DateUtils.formatChartDate(ts), xOf(ts), chartBottom + 14f, xLabelPaint)
                }

            y += padT + chartH + 6f

            // Color legend
            val legendPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 8f; color = "#757575".toColorInt() }
            val dotLegend = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
            var lx = MARGIN
            listOf(Pair("Low (0–3)", "#4CAF50"), Pair("Moderate (4–6)", "#FF9800"), Pair("High (7–10)", "#F44336"))
                .forEach { (lbl, hex) ->
                    dotLegend.color = hex.toColorInt()
                    canvas.drawCircle(lx + 5f, y + 4f, 4f, dotLegend)
                    canvas.drawText(lbl, lx + 13f, y + 8f, legendPaint)
                    lx += legendPaint.measureText(lbl) + 28f
                }
            y += 18f

            // Stats row
            val avg = slice.map { it.painLevel }.average()
            val maxP = slice.maxOf { it.painLevel }
            val minP = slice.minOf { it.painLevel }
            val topLoc = slice.flatMap { it.locations.split(",").map(String::trim).filter(String::isNotBlank) }
                .groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: "—"
            val topTrig = slice.flatMap { it.triggers.split(",").map(String::trim).filter(String::isNotBlank) }
                .groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: "—"
            val avgMood = slice.map { it.mood }.average()
            val avgSleep = slice.map { it.sleepQuality }.average()

            val col2X = MARGIN + CONTENT_WIDTH / 2f
            val statRows = listOf(
                Pair("Entries", slice.size.toString()) to Pair("Avg Pain", "%.1f".format(avg)),
                Pair("Range", "$minP – $maxP") to Pair("Top Location", topLoc),
                Pair("Top Trigger", topTrig) to Pair("Avg Mood", "%.1f / 5".format(avgMood)),
                Pair("Avg Sleep", "%.1f / 5".format(avgSleep)) to null
            )
            statRows.forEach { (left, right) ->
                canvas.drawText(left.first, MARGIN, y + statBodyPaint.textSize, statMutedPaint)
                canvas.drawText(left.second, MARGIN + 72f, y + statBodyPaint.textSize, statBodyPaint)
                if (right != null) {
                    canvas.drawText(right.first, col2X, y + statBodyPaint.textSize, statMutedPaint)
                    canvas.drawText(right.second, col2X + 72f, y + statBodyPaint.textSize, statBodyPaint)
                }
                y += 13f
            }

            y += 4f
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, dividerPaint)
            y += 16f
        }

        // --- Header ---
        canvas.drawText("Pain Journal", MARGIN, y + titlePaint.textSize, titlePaint)
        y += titlePaint.textSize + 6f
        canvas.drawText(
            "Generated ${DateUtils.formatDateTime(now)}  •  ${allSorted.size} total entries",
            MARGIN, y + subtitlePaint.textSize, subtitlePaint
        )
        y += subtitlePaint.textSize + 14f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, dividerPaint)
        y += 18f

        // --- Trend section for the exported entries ---
        val trendLabel = if (allSorted.isNotEmpty()) {
            val from = DateUtils.formatChartDateYear(allSorted.first().timestamp)
            val to = DateUtils.formatChartDateYear(now)
            "PAIN TREND  $from – $to"
        } else {
            "PAIN TREND"
        }
        drawTrendSection(trendLabel, allSorted)

        // --- Entries list ---
        newPage()
        canvas.drawText("ENTRIES", MARGIN, y + sectionPaint.textSize, sectionPaint)
        y += sectionPaint.textSize + 10f

        // --- Entries ---
        for (entry in allSorted) {
            val badgeSize = 32f
            val rowHeight = badgeSize + 16f
            val notesText = entry.notes.trim()
            val extraLines = if (notesText.isNotEmpty()) {
                val maxChars = (CONTENT_WIDTH / 5.5f).toInt()
                val noteLines = notesText.chunked(maxChars).size.coerceAtMost(3)
                noteLines
            } else 0
            val detailLines = listOf(entry.locations, entry.painTypes, entry.triggers, entry.medications)
                .filter { it.isNotBlank() }.size
            val entryHeight = rowHeight + detailLines * 13f + (if (extraLines > 0) extraLines * 12f + 8f else 0f) + 12f

            ensureSpace(entryHeight)

            val rowTop = y

            // Badge
            badgePaint.color = when {
                entry.painLevel <= 3 -> "#4CAF50".toColorInt()
                entry.painLevel <= 6 -> "#FF9800".toColorInt()
                else -> "#F44336".toColorInt()
            }
            canvas.drawRoundRect(
                RectF(MARGIN, rowTop, MARGIN + badgeSize, rowTop + badgeSize),
                8f, 8f, badgePaint
            )
            canvas.drawText(
                entry.painLevel.toString(),
                MARGIN + badgeSize / 2f,
                rowTop + badgeSize / 2f + badgeTextPaint.textSize / 2.5f,
                badgeTextPaint
            )

            // Timestamp
            val textX = MARGIN + badgeSize + 12f
            canvas.drawText(
                DateUtils.formatDateTime(entry.timestamp),
                textX,
                rowTop + 12f,
                mutedPaint
            )

            // Detail rows
            var detailY = rowTop + 12f + mutedPaint.textSize + 4f
            if (entry.locations.isNotBlank()) {
                canvas.drawText("Location: ${entry.locations}", textX, detailY, bodyPaint)
                detailY += 13f
            }
            if (entry.painTypes.isNotBlank()) {
                canvas.drawText("Type: ${entry.painTypes}", textX, detailY, bodyPaint)
                detailY += 13f
            }
            if (entry.triggers.isNotBlank()) {
                canvas.drawText("Triggers: ${entry.triggers}", textX, detailY, bodyPaint)
                detailY += 13f
            }
            if (entry.medications.isNotBlank()) {
                canvas.drawText("Medications: ${entry.medications}", textX, detailY, bodyPaint)
                detailY += 13f
            }

            // Mood & sleep on right side
            val rightX = PAGE_WIDTH - MARGIN
            val moodText = "Mood: ${"★".repeat(entry.mood)}${"☆".repeat(5 - entry.mood)}"
            val sleepText = "Sleep: ${"★".repeat(entry.sleepQuality)}${"☆".repeat(5 - entry.sleepQuality)}"
            mutedPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(moodText, rightX, rowTop + 12f, mutedPaint)
            canvas.drawText(sleepText, rightX, rowTop + 12f + mutedPaint.textSize + 3f, mutedPaint)
            mutedPaint.textAlign = Paint.Align.LEFT

            // Notes
            if (notesText.isNotEmpty()) {
                detailY += 4f
                canvas.drawText("Notes:", textX, detailY, sectionPaint)
                detailY += 11f
                val maxChars = ((CONTENT_WIDTH - badgeSize - 12f) / 5.5f).toInt()
                notesText.chunked(maxChars).take(3).forEach { line ->
                    canvas.drawText(line, textX, detailY, mutedPaint)
                    detailY += 12f
                }
            }

            y = maxOf(rowTop + rowHeight, detailY) + 10f

            // Row divider
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, dividerPaint)
            y += 10f
        }

        finishPage()
        return doc
    }
}
