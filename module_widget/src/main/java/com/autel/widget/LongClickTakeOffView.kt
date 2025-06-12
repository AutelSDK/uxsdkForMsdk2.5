package com.autel.widget

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.animation.addListener
import androidx.core.graphics.toColorInt
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 长按一键起飞
 */
class LongClickTakeOffView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    /**
     * 进度背景颜色
     */
    private var progressBgColor = "#CCCCCC".toColorInt()

    /**
     * 进度颜色
     */
    private var progressColor = "#28CD41".toColorInt()

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 20f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val rectF = RectF()
    /**
     * 进度起始角度
     */
    private var startAngle = -90f

    /**
     * 进度
     */
    private var progress = 0f
    /**
     * 进度动画时间
     */
    private var progressAnimatorTime = 2000L

    private var longClickAnimator: ValueAnimator? = null

    private var _onTriggerListener: OnTriggerListener? = null

    private var isInTouching = AtomicBoolean(false)

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.LongClickTakeOffView)
        progressBgColor = a.getColor(R.styleable.LongClickTakeOffView_progressBgColor, progressBgColor)
        progressColor = a.getColor(R.styleable.LongClickTakeOffView_progressColor, progressColor)
        paint.strokeWidth = a.getDimensionPixelSize(R.styleable.LongClickTakeOffView_progress_strokeWidth, paint.strokeWidth.toInt()).toFloat()
        progressAnimatorTime = a.getInt(R.styleable.LongClickTakeOffView_progressAnimatorTime, progressAnimatorTime.toInt()).toLong()
        a.recycle()
    }

    fun setOnTriggerListener(listener: OnTriggerListener) {
        this._onTriggerListener = listener
    }

    override fun onDraw(canvas: Canvas) {
        canvas.run {
            drawBg()
            drawProgress()
        }
    }

    /**
     * 背景
     */
    private fun Canvas.drawBg() {
        paint.color = progressBgColor
        // 一个圆形进度条
        val padding = paint.strokeWidth / 2
        rectF.set(0f + padding, 0f + padding, width.toFloat() - padding, height.toFloat() - padding)
        drawArc(rectF, startAngle, 360f, false, paint)
    }

    /**
     * 进度
     */
    private fun Canvas.drawProgress() {
        paint.color = progressColor
        // 一个圆形进度条
        val padding = paint.strokeWidth / 2
        rectF.set(0f + padding, 0f + padding, width.toFloat() - padding, height.toFloat() - padding)
        drawArc(rectF, startAngle, progress, false, paint)
    }

    /**
     * 长按动画
     */
    private fun startAutoProgress() {
        if (longClickAnimator != null) {
            longClickAnimator?.cancel()
            longClickAnimator = null
        }
        longClickAnimator = ObjectAnimator.ofFloat(0f, 360f).apply {
            repeatCount = 0
            repeatMode = ObjectAnimator.RESTART
            duration = progressAnimatorTime
            interpolator = LinearInterpolator()
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }
            addListener(onEnd = {
                if (isInTouching.get()) {
                    // 触发事件
                    _onTriggerListener?.onTrigger()
                }
            })
            start()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // 按下
                isInTouching.set(true)
                startAutoProgress()
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // 抬起
                isInTouching.set(false)
                longClickAnimator?.cancel()
                longClickAnimator = null
                progress = 0f
                invalidate()
            }
        }
        return super.onTouchEvent(event)
    }

    /**
     * 触发长按事件
     */
    fun interface OnTriggerListener {
        fun onTrigger()
    }
}