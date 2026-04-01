package com.example.paintrackerfree.ui.reports

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.example.paintrackerfree.data.model.PainEntry
import com.example.paintrackerfree.util.DateUtils

class PainChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    var entries: List<PainEntry> = emptyList()
        set(value) { field = value.sortedBy { it.timestamp }; invalidate() }

    private val padL = 80f
    private val padR = 24f
    private val padT = 24f
    private val padB = 56f

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E0E0E0"); strokeWidth = 1f
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#5C8BE0"); strokeWidth = 4f; style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND; strokeCap = Paint.Cap.ROUND
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#757575")
        textSize = resources.displayMetrics.scaledDensity * 13f
    }
    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#BDBDBD")
        textSize = resources.displayMetrics.scaledDensity * 16f
        textAlign = Paint.Align.CENTER
    }

    private fun dotColor(level: Int): Int = when {
        level <= 3 -> Color.parseColor("#4CAF50")
        level <= 6 -> Color.parseColor("#FF9800")
        else -> Color.parseColor("#F44336")
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

        val minT = entries.first().timestamp.toFloat()
        val maxT = entries.last().timestamp.toFloat()
        val rangeT = if (maxT > minT) maxT - minT else 1f

        fun xOf(ts: Long) = padL + (ts - minT) / rangeT * chartW
        fun yOf(level: Int) = padT + chartH * (1f - level / 10f)

        // Line path
        val path = Path()
        entries.forEachIndexed { i, e ->
            val x = xOf(e.timestamp); val y = yOf(e.painLevel)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        canvas.drawPath(path, linePaint)

        // Dots
        entries.forEach { e ->
            dotPaint.color = dotColor(e.painLevel)
            canvas.drawCircle(xOf(e.timestamp), yOf(e.painLevel), 8f, dotPaint)
        }

        // X axis date labels (up to 5)
        val step = maxOf(1, entries.size / 5)
        val labeled = entries.filterIndexed { i, _ -> i % step == 0 || i == entries.size - 1 }
            .distinctBy { DateUtils.formatChartDate(it.timestamp) }
        labeled.forEach { e ->
            val label = DateUtils.formatChartDate(e.timestamp)
            val x = xOf(e.timestamp)
            canvas.drawText(label, x - labelPaint.measureText(label) / 2f, h - 10f, labelPaint)
        }
    }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        val desiredH = (200 * resources.displayMetrics.density).toInt()
        super.onMeasure(widthSpec, MeasureSpec.makeMeasureSpec(desiredH, MeasureSpec.EXACTLY))
    }
}
