package ru.github.debitcredit.customview

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
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

    // Флаг, определяющий режим отображения (большой или маленький)
    var isSmallMode = false
        set(value) {
            field = value
            if (value) {
                // Для маленького режима делаем линии тоньше и шрифт меньше
                lineWidth = dp(12f).toFloat()
                fontSize = dp(16f).toFloat()
            } else {
                // Для большого режима возвращаем нормальные размеры
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
        color = Color.WHITE
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
        for ((index, category) in data.withIndex()) {
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

        if (isSmallMode) {
            // Маленький режим: показываем сумму под графиком
            val totalText = String.format("%.0f", total)

            // Текст суммы (крупнее)
            textPaint.textSize = fontSize
            textPaint.color = Color.WHITE
            canvas.drawText("$totalText ₽", center.x, center.y + fontSize / 3, textPaint)

            // Рисуем точку-индикатор сверху (уменьшенную)
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
        } else {
            // Большой режим: показываем сумму в центре и подпись "всего"
            val totalText = String.format("%.0f", total)
            textPaint.textSize = fontSize
            textPaint.color = Color.WHITE
            canvas.drawText("$totalText ₽", center.x, center.y + fontSize / 4, textPaint)

            // Рисуем точку-индикатор сверху (нормальную)
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
    }

    fun startAnimation() {
        // Полностью останавливаем предыдущую анимацию
        valueAnimator?.cancel()
        valueAnimator = null

        if (visibility != View.VISIBLE) return

        // Сбрасываем прогресс перед началом
        progress = 0f
        currentRotation = 0f
        invalidate()  // Принудительно перерисовываем сброшенное состояние

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