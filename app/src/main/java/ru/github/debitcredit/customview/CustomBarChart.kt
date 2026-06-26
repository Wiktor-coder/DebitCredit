package ru.github.debitcredit.customview

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.toColorInt
import java.util.Locale
import kotlin.math.max

class CustomBarChart @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 32f
        color = Color.BLACK
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#E0E0E0".toColorInt()
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    private var maxValue = 1f
    private var visibleStartIndex = 0
    private var maxVisibleItems = 15
    private var touchStartX = 0f
    private var scrollOffset = 0f

    data class BarData(
        val label: String,
        val expense: Float,
        val income: Float
    )

    var data: List<BarData> = emptyList()
        set(value) {
            field = value
            maxValue = max(
                value.maxOfOrNull { it.expense } ?: 1f,
                value.maxOfOrNull { it.income } ?: 1f
            ).coerceAtLeast(1f)

            // Обновляем видимую область
            val totalItems = value.size
            maxVisibleItems = when {
                totalItems <= 15 -> totalItems
                else -> 15
            }
            visibleStartIndex = 0
            scrollOffset = 0f
            invalidate()
        }

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        isClickable = true
        isFocusable = true
    }

    override fun performClick(): Boolean {
        // Вызываем супер-метод, чтобы сработали стандартные OnClickListener, если они установлены
        return super.performClick()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchStartX = event.x
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaX = touchStartX - event.x
                val itemWidth = (width - 80f) / maxVisibleItems
                val maxScroll = (data.size - maxVisibleItems) * itemWidth

                scrollOffset += deltaX
                scrollOffset = scrollOffset.coerceIn(0f, maxScroll)
                touchStartX = event.x

                // Вычисляем начальный индекс
                visibleStartIndex = (scrollOffset / itemWidth).toInt()
                visibleStartIndex = visibleStartIndex.coerceIn(0,
                    max(0, data.size - maxVisibleItems)
                )

                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                // Вызываем performClick при отпускании пальца
                performClick()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (data.isEmpty()) {
            drawEmptyState(canvas)
            return
        }

        val totalWidth = width.toFloat()
        val totalHeight = height.toFloat()
        val paddingTop = 40f
        val paddingBottom = 60f
        val paddingStart = 40f
        val chartHeight = totalHeight - paddingTop - paddingBottom
        val itemWidth = (totalWidth - paddingStart - 20f) / maxVisibleItems
        val barWidth = itemWidth * 0.35f
        val gap = itemWidth * 0.15f

        // Рисуем сетку
        drawGrid(canvas, paddingTop, chartHeight)

        // Определяем видимые данные
        val visibleData = if (data.size > maxVisibleItems) {
            data.subList(visibleStartIndex, minOf(visibleStartIndex + maxVisibleItems, data.size))
        } else {
            data
        }

        // Рисуем бары
        for ((index, item) in visibleData.withIndex()) {
            val x = paddingStart + index * itemWidth + itemWidth / 2 - barWidth

            // Расход (красный)
            val expenseHeight = if (maxValue > 0) (item.expense / maxValue) * chartHeight else 0f
            paint.color = "#FF6B6B".toColorInt()
            paint.style = Paint.Style.FILL
            canvas.drawRect(
                x,
                paddingTop + chartHeight - expenseHeight,
                x + barWidth,
                paddingTop + chartHeight,
                paint
            )

            // Добавляем значение над столбцом расхода
            if (item.expense > 0) {
                textPaint.textSize = 20f
                textPaint.color = "#FF6B6B".toColorInt()
                textPaint.textAlign = Paint.Align.CENTER
                canvas.drawText(
                    String.format(Locale.US,"%.0f", item.expense),
                    x + barWidth / 2,
                    paddingTop + chartHeight - expenseHeight - 8f,
                    textPaint
                )
            }

            // Доход (зеленый)
            val incomeHeight = if (maxValue > 0) (item.income / maxValue) * chartHeight else 0f
            paint.color = "#4ECDC4".toColorInt()
            canvas.drawRect(
                x + barWidth + gap,
                paddingTop + chartHeight - incomeHeight,
                x + barWidth * 2 + gap,
                paddingTop + chartHeight,
                paint
            )

            // Добавляем значение над столбцом дохода
            if (item.income > 0) {
                textPaint.textSize = 20f
                textPaint.color = "#4ECDC4".toColorInt()
                textPaint.textAlign = Paint.Align.CENTER
                canvas.drawText(
                    String.format(Locale.US,"%.0f", item.income),
                    x + barWidth + gap + barWidth / 2,
                    paddingTop + chartHeight - incomeHeight - 8f,
                    textPaint
                )
            }

            // Подпись
            textPaint.textSize = 28f
            textPaint.color = getTextColor()
            textPaint.textAlign = Paint.Align.CENTER
            val labelX = x + barWidth + gap / 2
            canvas.drawText(item.label, labelX, totalHeight - 10f, textPaint)
        }

        // Отображаем информацию о скролле
        if (data.size > maxVisibleItems) {
            drawScrollInfo(canvas)
        }
    }
    @Suppress("SameParameterValue")
    private fun drawGrid(canvas: Canvas, paddingTop: Float, chartHeight: Float) {
        gridPaint.color = "#E0E0E0".toColorInt()
        gridPaint.style = Paint.Style.STROKE
        gridPaint.strokeWidth = 1f

        // Горизонтальные линии
        for (i in 0..4) {
            val y = paddingTop + chartHeight - (chartHeight / 4) * i
            canvas.drawLine(20f, y, width - 20f, y, gridPaint)
        }

        // Значения на оси Y
        textPaint.textSize = 24f
        textPaint.color = getTextColor()
        textPaint.textAlign = Paint.Align.RIGHT
        for (i in 0..4) {
            val value = (maxValue / 4) * i
            val y = paddingTop + chartHeight - (chartHeight / 4) * i
            canvas.drawText("%.0f".format(Locale.US, value) , 35f, y + 8f, textPaint)
        }
        textPaint.textAlign = Paint.Align.CENTER
    }

    private fun drawScrollInfo(canvas: Canvas) {
        textPaint.textSize = 20f
        textPaint.color = getTextColor()
        val progress = if (data.size > maxVisibleItems) {
            visibleStartIndex.toFloat() / (data.size - maxVisibleItems)
        } else {
            0f
        }
        val barWidth = width * 0.3f
        val barX = (width - barWidth) / 2
        val barY = height - 20f

        // Полоса прокрутки
        paint.color = "#CCCCCC".toColorInt()
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(barX, barY, barX + barWidth, barY + 6f, 3f, 3f, paint)

        // Индикатор положения
        paint.color = "#DAA520".toColorInt()
        val indicatorWidth = barWidth / (data.size / maxVisibleItems.toFloat() + 1)
        val indicatorX = barX + progress * (barWidth - indicatorWidth)
        canvas.drawRoundRect(indicatorX, barY, indicatorX + indicatorWidth, barY + 6f, 3f, 3f, paint)
    }

    private fun drawEmptyState(canvas: Canvas) {
        textPaint.textSize = 40f
        textPaint.color = getTextColor()
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("Нет данных", width / 2f, height / 2f, textPaint)
        textPaint.textAlign = Paint.Align.CENTER
    }

    private fun getTextColor(): Int {
        val isNightMode = (resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        return if (isNightMode) Color.WHITE else Color.BLACK
    }
}