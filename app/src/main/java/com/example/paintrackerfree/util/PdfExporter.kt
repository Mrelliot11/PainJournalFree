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
        } catch (e: IOException) {
            null
        }
    }

    private fun writePdfToCache(context: Context, entries: List<PainEntry>): File {
        val file = File(
            context.getExternalFilesDir(null),
            "pain_journal_${System.currentTimeMillis()}.pdf"
        )
        file.outputStream().use { buildPdf(entries).writeTo(it) }
        return file
    }

    private fun buildPdf(entries: List<PainEntry>): PdfDocument {
        val doc = PdfDocument()

        // --- Paints ---
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 22f
            color = Color.parseColor("#1A1A2E")
            isFakeBoldText = true
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 11f
            color = Color.parseColor("#757575")
        }
        val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 9f
            color = Color.parseColor("#5C8BE0")
            isFakeBoldText = true
            letterSpacing = 0.08f
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 10f
            color = Color.parseColor("#1A1A2E")
        }
        val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 9f
            color = Color.parseColor("#757575")
        }
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 13f
            color = Color.WHITE
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val dividerPaint = Paint().apply {
            color = Color.parseColor("#E0E0E0")
            strokeWidth = 0.5f
        }

        var pageNum = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
        var page = doc.startPage(pageInfo)
        var canvas: Canvas = page.canvas
        var y = MARGIN

        fun finishPage() {
            // page footer
            val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 9f
                color = Color.parseColor("#9E9E9E")
                textAlign = Paint.Align.CENTER
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

        // --- Header ---
        canvas.drawText("Pain Journal", MARGIN, y + titlePaint.textSize, titlePaint)
        y += titlePaint.textSize + 6f

        val generatedText = "Generated ${DateUtils.formatDateTime(System.currentTimeMillis())}  •  ${entries.size} entries"
        canvas.drawText(generatedText, MARGIN, y + subtitlePaint.textSize, subtitlePaint)
        y += subtitlePaint.textSize + 14f

        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, dividerPaint)
        y += 18f

        // --- Entries ---
        for (entry in entries) {
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
                entry.painLevel <= 3 -> Color.parseColor("#4CAF50")
                entry.painLevel <= 6 -> Color.parseColor("#FF9800")
                else -> Color.parseColor("#F44336")
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
