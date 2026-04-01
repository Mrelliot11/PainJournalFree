package com.example.paintrackerfree.ui.reports

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.example.paintrackerfree.data.model.PainEntry
import com.example.paintrackerfree.util.DateUtils
import androidx.core.graphics.toColorInt
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PainChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    var entries: List<PainEntry> = emptyList()
        set(value) { field = value.sortedBy { it.timestamp }; invalidate() }

    private val padL = 80f
    private val padR = 24f
    private val padT = 24f
    private val padB = 56f

    private val dayFormat = SimpleDateFormat("yyyyMMdd", Locale.US)

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#E0E0E0".toColorInt(); strokeWidth = 1f
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#5C8BE0".toColorInt(); strokeWidth = 4f; style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND; strokeCap = Paint.Cap.ROUND
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#757575".toColorInt()
        textSize = resources.displayMetrics.scaledDensity * 13f
    }
    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#BDBDBD".toColorInt()
        textSize = resources.displayMetrics.scaledDensity * 16f
        textAlign = Paint.Align.CENTER
    }

    private fun dotColor(level: Int): Int = when {
        level <= 3 -> "#4CAF50".toColorInt()
        level <= 6 -> "#FF9800".toColorInt()
        else -> "#F44336".toColorInt()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        val chartW = w - padL - padR; val chartH = h - padT - padB

        // Y axis grid + labels
        for (i in 0..10 step 2) {
            val y = padT + chartH * (1f - i / 10f)
            canvas.drawLine(padL, y, w - padR, y, gridPaint)
            canvas.drawText(i.toString(), padL - 28f, y + labelPaint.textSize / 3f, labelPaint)
        }

        if (entries.isEmpty()) {
            canvas.drawText("No data for this period", w / 2f, h / 2f, emptyPaint)
            return
        }

        // Average pain per calendar day so multiple same-day entries don't cause vertical spikes
        val dailyPoints: List<Pair<Long, Float>> = entries
            .groupBy { dayFormat.format(Date(it.timestamp)) }
            .entries
            .sortedBy { it.key }
            .map { (_, dayEntries) ->
                val midTs = dayEntries.map { it.timestamp }.average().toLong()
                val avgLevel = dayEntries.map { it.painLevel }.average().toFloat()
                midTs to avgLevel
            }

        val minT = dailyPoints.first().first.toFloat()
        val maxT = dailyPoints.last().first.toFloat()
        val rangeT = if (maxT > minT) maxT - minT else 1f

        fun xOf(ts: Long) = padL + (ts - minT) / rangeT * chartW
        fun yOf(level: Float) = padT + chartH * (1f - level / 10f)

        // Line path — reset each draw to avoid accumulation across invalidations
        val path = Path()
        dailyPoints.forEachIndexed { i, (ts, lvl) ->
            if (i == 0) path.moveTo(xOf(ts), yOf(lvl)) else path.lineTo(xOf(ts), yOf(lvl))
        }
        canvas.drawPath(path, linePaint)

        // Dots
        dailyPoints.forEach { (ts, lvl) ->
            dotPaint.color = dotColor(lvl.toInt())
            canvas.drawCircle(xOf(ts), yOf(lvl), 8f, dotPaint)
        }

        // X-axis date labels (up to 5)
        val step = maxOf(1, dailyPoints.size / 5)
        dailyPoints.filterIndexed { i, _ -> i % step == 0 || i == dailyPoints.size - 1 }
            .forEach { (ts, _) ->
                val label = DateUtils.formatChartDate(ts)
                canvas.drawText(label, xOf(ts) - labelPaint.measureText(label) / 2f, h - 10f, labelPaint)
            }
    }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        val desiredH = (200 * resources.displayMetrics.density).toInt()
        super.onMeasure(widthSpec, MeasureSpec.makeMeasureSpec(desiredH, MeasureSpec.EXACTLY))
    }
}
