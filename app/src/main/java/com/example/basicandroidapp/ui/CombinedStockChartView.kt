package com.example.basicandroidapp.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import com.example.basicandroidapp.model.Stock

/**
 * A chart view that renders the normalised price history of multiple stocks
 * simultaneously, each in its own distinct colour, for quick portfolio comparison.
 */
class CombinedStockChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        /** Distinct colours assigned to each series in order. */
        val SERIES_COLORS = intArrayOf(
            0xFF4FC3F7.toInt(), // light blue
            0xFF81C784.toInt(), // green
            0xFFFFB74D.toInt(), // orange
            0xFFF06292.toInt(), // pink
            0xFFBA68C8.toInt(), // purple
            0xFF4DD0E1.toInt(), // cyan
            0xFFDCE775.toInt(), // lime
            0xFFFF8A65.toInt(), // deep orange
            0xFF90A4AE.toInt(), // blue-grey
            0xFFFFF176.toInt()  // yellow
        )
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 2.5f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x22FFFFFF
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFB0BEC5.toInt()
        textSize = android.util.TypedValue.applyDimension(
            android.util.TypedValue.COMPLEX_UNIT_SP, 9f,
            context.resources.displayMetrics
        )
    }

    private val legendPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = android.util.TypedValue.applyDimension(
            android.util.TypedValue.COMPLEX_UNIT_SP, 10f,
            context.resources.displayMetrics
        )
        isFakeBoldText = true
    }

    /** List of (stock, colorInt) pairs to render. */
    var series: List<Pair<Stock, Int>> = emptyList()
        set(value) {
            field = value
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (series.isEmpty()) return

        val w = width.toFloat()
        val h = height.toFloat()

        // Reserve space at the bottom for the legend
        val legendHeight = android.util.TypedValue.applyDimension(
            android.util.TypedValue.COMPLEX_UNIT_SP, 14f,
            context.resources.displayMetrics
        ) + 8f

        val paddingTop = 16f
        val paddingBottom = 24f + legendHeight
        val paddingLeft = 8f
        val paddingRight = 8f

        val chartWidth = w - paddingLeft - paddingRight
        val chartHeight = h - paddingTop - paddingBottom

        if (chartWidth <= 0 || chartHeight <= 0) return

        // Draw horizontal grid lines
        for (i in 0..2) {
            val y = paddingTop + chartHeight * (i / 2f)
            canvas.drawLine(paddingLeft, y, paddingLeft + chartWidth, y, gridPaint)
        }

        // Draw each series normalised to percentage-from-first-price
        for ((stock, color) in series) {
            val history = stock.priceHistory.toList()
            if (history.size < 2) continue

            val firstPrice = history.first()
            if (firstPrice == 0.0) continue

            // Normalised to [0, 1] relative to first price; collect all for y range
            val normalised = history.map { (it - firstPrice) / firstPrice }

            val minN = normalised.min()
            val maxN = normalised.max()
            val range = maxOf(maxN - minN, 0.001)

            fun xFor(i: Int): Float =
                paddingLeft + (i.toFloat() / (history.size - 1)) * chartWidth

            fun yFor(n: Double): Float =
                paddingTop + chartHeight - ((n - minN) / range * chartHeight).toFloat()

            val path = Path()
            path.moveTo(xFor(0), yFor(normalised[0]))
            for (i in 1 until normalised.size) {
                path.lineTo(xFor(i), yFor(normalised[i]))
            }

            linePaint.color = color
            canvas.drawPath(path, linePaint)

            // Draw dot at current price position
            val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                style = Paint.Style.FILL
            }
            canvas.drawCircle(xFor(normalised.size - 1), yFor(normalised.last()), 4f, dotPaint)
        }

        // Draw legend at the bottom
        val legendY = h - 6f
        val swatchSize = legendPaint.textSize
        val swatchGap = 6f
        val itemSpacing = swatchSize + swatchGap

        // Measure total legend width to centre it
        val symbolWidths = series.map { (stock, _) ->
            swatchSize + 4f + legendPaint.measureText(stock.symbol)
        }
        val totalLegendWidth = symbolWidths.sum() + (series.size - 1) * itemSpacing

        var legendX = (w - totalLegendWidth) / 2f

        for ((idx, pair) in series.withIndex()) {
            val (stock, color) = pair
            val swatchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                style = Paint.Style.FILL
            }
            // Colour swatch
            canvas.drawRect(
                legendX, legendY - swatchSize,
                legendX + swatchSize, legendY,
                swatchPaint
            )
            // Symbol label
            legendPaint.color = color
            canvas.drawText(stock.symbol, legendX + swatchSize + 4f, legendY, legendPaint)
            legendX += symbolWidths[idx] + itemSpacing
        }
    }
}
