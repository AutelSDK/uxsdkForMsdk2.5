package com.autel.widget.widget.focusAndZoom

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.AnticipateOvershootInterpolator
import androidx.core.animation.doOnEnd
import androidx.core.view.isVisible
import com.autel.common.extension.resetSize
import com.autel.widget.R

/**
 * 变焦框
 * @author R10091
 */
class ChangeFocusView(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) :
    View(context, attrs, defStyleAttr, defStyleRes) {

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(
        context,
        attrs,
        defStyleAttr,
        0
    )

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0, 0)

    constructor(context: Context) : this(context, null, 0, 0)

    /**
     * 边距
     */
    private val _margin: Float = 10f

    /**
     * 变化值，用于动画
     */
    private var animationValue: Float = _margin

    /**
     * 动画执行时间
     */
    var duration: Long = 500

    /**
     * 最大值
     */
    var max: Float = 3f

    /**
     * 最小值
     */
    var min: Float = -3f

    /**
     * 当前值
     */
    var value: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    /**
     * 是否锁定
     */
    var isLock: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    /**
     * 四边短线的长度
     */
    private var lineLength = 40f

    // 画笔
    private val paint: Paint = Paint().apply {
        isAntiAlias = true
        color = Color.parseColor("#FFDA00")
        strokeWidth = context.resources.getDimension(R.dimen.common_1dp)
        style = Paint.Style.STROKE
        textSize = context.resources.getDimension(R.dimen.common_text_size_sp_9)
    }

    /**
     * 方框
     */
    private val rect = RectF()

    /**
     * 锁头
     */
    private val lockBp: Bitmap by lazy {
        val bp = BitmapFactory.decodeResource(resources, R.drawable.mission_ic_focus_lock)
        bp.resetSize(height / 5f, height / 5f)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (measuredHeight != 0) {
            lineLength = measuredHeight / 12f
        }
    }

    override fun onDraw(canvas: Canvas) {
        canvas?.run {
            rect.top = height / 4f - animationValue
            rect.left = height / 4f - animationValue
            rect.right = height / 4f * 3 + animationValue
            rect.bottom = height / 4f * 3 + animationValue
            drawRect(rect, paint)
            if (isLock) {
                drawBitmap(
                    lockBp,
                    rect.centerX() - lockBp.width / 2,
                    rect.centerY() - lockBp.height / 2,
                    paint
                )
            }

            // 左边横线
            drawLeft(canvas)
            // 上面竖线
            drawTop(canvas)
            // 右边线
            drawRight(canvas)
            // 底部线
            drawBottom(canvas)
        }
    }

    private fun drawLeft(canvas: Canvas) {
        canvas.drawLine(
            rect.left,
            height / 2f,
            rect.left + lineLength,
            height / 2f,
            paint
        )
    }

    private fun drawTop(canvas: Canvas) {
        canvas.drawLine(
            rect.left + rect.width() / 2f,
            rect.top,
            rect.left + rect.width() / 2f,
            rect.top + lineLength,
            paint
        )
    }

    private fun drawRight(canvas: Canvas) {
        canvas.drawLine(
            rect.right,
            height / 2f,
            rect.right - lineLength,
            height / 2f,
            paint
        )
    }

    private fun drawBottom(canvas: Canvas) {
        canvas.drawLine(
            rect.left + rect.width() / 2f,
            rect.bottom - lineLength,
            rect.left + rect.width() / 2f,
            rect.bottom,
            paint
        )
    }

    fun show() {
        isVisible = true
        val objectAnimation = ObjectAnimator.ofFloat(height / 5f, _margin)
        objectAnimation.addUpdateListener {
            animationValue = it.animatedValue as Float
            invalidate()
        }
        objectAnimation.doOnEnd {

        }
        objectAnimation.interpolator = AnticipateOvershootInterpolator()
        objectAnimation.duration = duration
        objectAnimation.start()
    }

    fun showWithoutAnim() {
        isVisible = true
    }

}