package com.autel.setting.widget

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.animation.doOnEnd
import androidx.core.graphics.toColorInt
import com.autel.setting.R
import kotlin.math.min

/**
 * 简单的圆形进度
 */
class SimpleCircleProgress(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) :
    View(context, attrs, defStyleAttr, defStyleRes) {

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : this(context, attrs, defStyleAttr, 0)

    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0, 0)

    constructor(context: Context?) : this(context, null, 0, 0)

    private val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeCap = Paint.Cap.ROUND
        style = Paint.Style.STROKE
    }

    private val defaultBgColor = "#DADADA".toColorInt()

    private val defaultProgressColor = "#007AFF".toColorInt()

    var bgColor: Int = defaultBgColor

    var progressColor: Int = defaultProgressColor

    var max = 100

    var current = 0
        set(value) {
            field = value
            invalidate()
        }

    private val rect = RectF()

    init {
        if (context!=null) {
            if (attrs!=null) {
                val typedArray =
                    context.theme.obtainStyledAttributes(attrs, R.styleable.SimpleCircleProgress, defStyleAttr, defStyleRes)
                paint.strokeWidth = typedArray.getDimension(R.styleable.SimpleCircleProgress_scp_strokeWidth, context.resources.getDimension(R.dimen.common_10dp))
                bgColor = typedArray.getColor(R.styleable.SimpleCircleProgress_scp_bgColor, defaultBgColor)
                progressColor = typedArray.getColor(R.styleable.SimpleCircleProgress_scp_progressColor, defaultProgressColor)
                max = typedArray.getInt(R.styleable.SimpleCircleProgress_scp_max, 100)
                typedArray.recycle()
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        canvas?.run {
            paint.color = bgColor
            drawCircle(width / 2f, height / 2f, (min(width, height) - paint.strokeWidth) / 2f, paint)

            paint.color = progressColor
            rect.set(paint.strokeWidth / 2, paint.strokeWidth / 2f, width - paint.strokeWidth / 2f,
                height - paint.strokeWidth / 2f)
            drawArc(rect, 90f, current * 360 / max.toFloat(), false, paint)
        }
    }

    private var isPlaying = false

    @Synchronized
    fun postToComplete(onProgress: (Int) -> Unit, onComplete: (Int) -> Unit) {
        if (isPlaying) return
        if (current >= max) {
            onComplete.invoke(max)
            return
        }
        val an = ObjectAnimator.ofInt(this, "current", current, max)
        an.duration = 1000
        an.repeatCount = 0
        an.addUpdateListener {
            val value = it.animatedValue as Int
            onProgress.invoke(value)
        }
        an.doOnEnd {
            current = max
            onComplete.invoke(max)
            isPlaying = false
        }
        isPlaying = true
        an.start()
    }
}