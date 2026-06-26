package ru.github.debitcredit.customview

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class StatsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var radius = 0f
    private var center = PointF(0f, 0f)
    private var oval = RectF(0f, 0f, 0f, 0f)
    private var lineWidth = dp(24f).toFloat()
    private var fontSize = dp(32f).toFloat()
    private var progress = 0f
    private var valueAnimator: ValueAnimator? = null
    private var currentRotation = 0f

    var showPercentage = false

    var isSmallMode = false
        set(value) {
            field = value
            if (value) {
                lineWidth = dp(12f).toFloat()
                fontSize = dp(16f).toFloat()
            } else {
                lineWidth = dp(24f).toFloat()
                fontSize = dp(32f).toFloat()
            }
            updatePaintAndText()
            requestLayout()
        }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
    }

    var data: List<CategoryData> = emptyList()
        set(value) {
            field = value
            startAnimation()
        }

    data class CategoryData(
        val name: String,
        val amount: Float,
        val color: Int
    )

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        updatePaintAndText()
    }

    private fun updatePaintAndText() {
        paint.strokeWidth = lineWidth
        textPaint.textSize = fontSize
        textPaint.color = getTextColorFromTheme()
    }

    private fun getTextColorFromTheme(): Int {
        val isNightMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        return if (isNightMode) Color.WHITE else Color.BLACK
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        textPaint.color = getTextColorFromTheme()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        radius = min(w, h) / 2f - lineWidth / 2
        center = PointF(w / 2f, h / 2f)
        oval = RectF(
            center.x - radius,
            center.y - radius,
            center.x + radius,
            center.y + radius
        )
    }

    override fun onDraw(canvas: Canvas) {
        if (data.isEmpty()) return

        val total = data.sumOf { it.amount.toDouble() }.toFloat()
        if (total == 0f) return

        var startAngle = -90f + currentRotation

        // Рисуем сегменты
        for ((_, category) in data.withIndex()) {
            val sweepAngle = 360f * (category.amount / total) * progress

            val shader = SweepGradient(
                center.x, center.y,
                intArrayOf(category.color, lightenColor(category.color)),
                null
            )
            paint.shader = shader
            paint.color = category.color

            canvas.drawArc(oval, startAngle, sweepAngle, false, paint)

            startAngle += sweepAngle
        }

        // Рисуем текст в центре
        if (showPercentage) {
            drawPercentageText(canvas)
        } else {
            drawTotalSum(canvas, total)
        }

        drawIndicatorDot(canvas)
    }

    private fun drawTotalSum(canvas: Canvas, total: Float) {
        textPaint.textSize = fontSize * 0.8f
        textPaint.color = getTextColorFromTheme()
        textPaint.textAlign = Paint.Align.CENTER

        val totalText = String.format(Locale.US,"%.0f", total)
        canvas.drawText("$totalText ₽", center.x, center.y + fontSize * 0.3f, textPaint)
    }

    private fun drawPercentageText(canvas: Canvas) {
        // Находим данные о потраченном (spent)
        val spentData = data.firstOrNull { it.name == "spent" }
        val percentage = if (spentData != null) {
            // Процент - это значение spent, так как мы передаем его в amount
            spentData.amount.toInt()
        } else {
            // Если данных нет, вычисляем из первого элемента
            val total = data.sumOf { it.amount.toDouble() }.toFloat()
            if (total > 0) {
                (data.first().amount / total * 100).toInt()
            } else {
                0
            }
        }

        // Показываем процент
        textPaint.textSize = fontSize * 0.9f
        textPaint.color = getTextColorFromTheme()
        textPaint.textAlign = Paint.Align.CENTER

        val percentText = "$percentage%"
        canvas.drawText(percentText, center.x, center.y + fontSize * 0.3f, textPaint)
    }

    private fun drawIndicatorDot(canvas: Canvas) {
        if (data.isNotEmpty()) {
            val dotRadius = lineWidth * 0.5f
            val dotAngle = Math.toRadians((-90f + currentRotation).toDouble())
            val dotX = center.x + radius * cos(dotAngle).toFloat()
            val dotY = center.y + radius * sin(dotAngle).toFloat()

            val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = data.firstOrNull()?.color ?: Color.WHITE
                style = Paint.Style.FILL
            }
            canvas.drawCircle(dotX, dotY, dotRadius, dotPaint)
        }
    }

    fun startAnimation() {
        valueAnimator?.cancel()
        valueAnimator = null

        if (visibility != VISIBLE) return

        progress = 0f
        currentRotation = 0f
        invalidate()

        valueAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1500
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                progress = animation.animatedValue as Float
                currentRotation = progress * 90f
                invalidate()
            }
            start()
        }
    }

    private fun lightenColor(color: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] = min(1f, hsv[2] * 1.2f)
        return Color.HSVToColor(hsv)
    }

    private fun dp(value: Float): Int = (context.resources.displayMetrics.density * value).toInt()
}