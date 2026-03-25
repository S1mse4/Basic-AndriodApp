package com.example.basicandroidapp.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View

class PriceChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 3f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x22FFFFFF
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFB0BEC5.toInt()
        // will be set in init after context is available
    }

    init {
        labelPaint.textSize = android.util.TypedValue.applyDimension(
            android.util.TypedValue.COMPLEX_UNIT_SP, 10f, context.resources.displayMetrics
        )
    }

    var priceHistory: List<Double> = emptyList()
        set(value) {
            field = value
            invalidate()
        }

    var isPositive: Boolean = true
        set(value) {
            field = value
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (priceHistory.size < 2) return

        val w = width.toFloat()
        val h = height.toFloat()
        val paddingTop = 20f
        val paddingBottom = 30f
        val paddingLeft = 10f
        val paddingRight = 60f

        val chartWidth = w - paddingLeft - paddingRight
        val chartHeight = h - paddingTop - paddingBottom

        val minPrice = priceHistory.min()
        val maxPrice = priceHistory.max()
        val priceRange = maxOf(maxPrice - minPrice, 0.01)

        val lineColor = if (isPositive) 0xFF00C853.toInt() else 0xFFFF1744.toInt()
        val fillColorTop = if (isPositive) 0x4400C853 else 0x44FF1744
        val fillColorBottom = 0x00000000

        linePaint.color = lineColor

        val n = priceHistory.size

        fun xFor(i: Int): Float = paddingLeft + (i.toFloat() / (n - 1)) * chartWidth
        fun yFor(price: Double): Float = paddingTop + chartHeight - ((price - minPrice) / priceRange * chartHeight).toFloat()

        // Draw horizontal grid lines (3 levels)
        for (i in 0..2) {
            val y = paddingTop + chartHeight * (i / 2f)
            canvas.drawLine(paddingLeft, y, paddingLeft + chartWidth, y, gridPaint)
        }

        // Draw price labels on right side
        val prices = listOf(maxPrice, (maxPrice + minPrice) / 2, minPrice)
        for ((idx, price) in prices.withIndex()) {
            val y = paddingTop + chartHeight * (idx / 2f) + if (idx == 0) 24f else if (idx == 2) -6f else 10f
            canvas.drawText("$%.2f".format(price), paddingLeft + chartWidth + 4f, y, labelPaint)
        }

        // Build fill path
        val fillPath = Path()
        fillPath.moveTo(xFor(0), paddingTop + chartHeight)
        fillPath.lineTo(xFor(0), yFor(priceHistory[0]))
        for (i in 1 until n) {
            fillPath.lineTo(xFor(i), yFor(priceHistory[i]))
        }
        fillPath.lineTo(xFor(n - 1), paddingTop + chartHeight)
        fillPath.close()

        fillPaint.shader = LinearGradient(
            0f, paddingTop, 0f, paddingTop + chartHeight,
            fillColorTop, fillColorBottom, Shader.TileMode.CLAMP
        )
        canvas.drawPath(fillPath, fillPaint)

        // Draw line
        val linePath = Path()
        linePath.moveTo(xFor(0), yFor(priceHistory[0]))
        for (i in 1 until n) {
            linePath.lineTo(xFor(i), yFor(priceHistory[i]))
        }
        canvas.drawPath(linePath, linePaint)

        // Draw current price dot
        val lastX = xFor(n - 1)
        val lastY = yFor(priceHistory.last())
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = lineColor
            style = Paint.Style.FILL
        }
        canvas.drawCircle(lastX, lastY, 6f, dotPaint)
    }
}
