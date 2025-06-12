package com.autel.widget.widget.lenszoom

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.Build
import android.os.SystemClock
import android.text.TextPaint
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.autel.widget.R

/**
 * 刻度尺
 */
class VirtualZoomScaleView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    View(context, attrs, defStyleAttr) {

    data class Scale(val startY: Float, val endY: Float, val number: String, val longLine: Boolean, val drawStr: Boolean, val value: Int)

    /**
     * 刻度数 - 每个刻度对应的值
     */
    private var scales: Array<Scale> = Array(0) { Scale(0f, 0f, "", false, false, 0) }


    /**
     * 线的高度
     */
    private var mLineHeight = context.resources.getDimension(R.dimen.common_1dp)

    /**
     * 线的间距
     */
    private var mLineGap = context.resources.getDimension(R.dimen.common_9_5dp)

    /**
     * 长线的长度
     */
    private var mLineLongWidth = context.resources.getDimension(R.dimen.common_20dp)

    /**
     * 短线的宽度
     */
    private var mLineShortWidth = context.resources.getDimension(R.dimen.common_12dp)

    private var mTextGap = context.resources.getDimension(R.dimen.common_4dp)

    private var endX = context.resources.getDimension(R.dimen.common_12dp)

    private var mLastX: Float = 0f
    private var mLastY: Float = 0f

    private var isStartSlop: Boolean = false

    private var mOldScroll: Int = 0

    private var mOnScaleListener: OnScaleListener? = null

    private var lastSelectScale: Scale? = null

    //选中线条的位置
    private var selectLineHeight = (mLineHeight + mLineGap) * 15f

    private var selectTextSize = context.resources.getDimension(R.dimen.common_text_size_sp_16)

    private var normalTextSize = context.resources.getDimension(R.dimen.common_text_size_sp_13)

    private val mTextPaint = TextPaint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.RIGHT
        textSize = normalTextSize
        strokeWidth = mLineHeight
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            setShadowLayer(context.resources.getDimension(R.dimen.common_2dp), 0f, 0f, 0x80000000)
        }
    }

    init {
        val attr = context.obtainStyledAttributes(attrs, R.styleable.VirtualZoomScaleView)
        mLineHeight = attr.getDimension(R.styleable.VirtualZoomScaleView_mission_scale_view_line_height, mLineHeight)
        mLineGap = attr.getDimension(R.styleable.VirtualZoomScaleView_mission_scale_view_line_gap, mLineGap)
        mLineLongWidth =
            attr.getDimension(R.styleable.VirtualZoomScaleView_mission_scale_view_line_long_width, mLineLongWidth)
        mLineShortWidth =
            attr.getDimension(R.styleable.VirtualZoomScaleView_mission_scale_view_line_short_width, mLineShortWidth)
        attr.recycle()
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeWidth = mLineHeight
    }


    fun setScale(scale: List<ScaleItem>) {
        var topOffset: Float = (paddingTop + mLineHeight)
        scales = scale.asReversed().mapIndexed { index, it ->
            val start = topOffset
            topOffset += mLineHeight + mLineGap
            val end = topOffset
            Scale(start, end, it.str, it.longLine, it.drawStr, it.value)
        }.toTypedArray()

        requestLayout()
    }

    fun setOnScaleListener(listener: OnScaleListener) {
        mOnScaleListener = listener
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        measureVertical(widthMeasureSpec, heightMeasureSpec)
    }

    private fun measureVertical(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = scales.size
        val heightSize = (mLineGap * (size - 1) + mLineHeight * size + paddingTop + paddingBottom).toInt()
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(widthSize, heightSize)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawLine(canvas)
    }

    private fun drawLine(canvas: Canvas) {
        var topOffset: Float = (paddingTop + mLineHeight / 2.0).toFloat()
        val endOffset = measuredWidth - paddingEnd - 0f
        paint.color = Color.WHITE
        val currentScale = findScale()
        val currentColor = ContextCompat.getColor(context, R.color.common_color_FEE15D)
        for (i in scales.indices) {
            val scale = scales[i]
            var lineStart: Float
            if (scale.longLine) {
                lineStart = endOffset - mLineLongWidth
                canvas.drawLine(lineStart, topOffset, endOffset, topOffset, paint)
            } else {
                lineStart = endOffset - mLineShortWidth
                canvas.drawLine(lineStart, topOffset, endOffset, topOffset, paint)
            }

            if (scale.drawStr) {
                if (currentScale == scale) {
                    mTextPaint.textSize = selectTextSize
                    mTextPaint.color = currentColor
                    canvas.drawText(scale.number, lineStart - mTextGap, topOffset + selectTextSize / 2 - 6, mTextPaint)
                } else {
                    mTextPaint.textSize = normalTextSize
                    mTextPaint.color = Color.WHITE
                    canvas.drawText(scale.number, lineStart - mTextGap, topOffset + normalTextSize / 2 - 4, mTextPaint)
                }
            }
            topOffset += mLineHeight + mLineGap
        }
        drawSelectLine(canvas)
    }

    private fun drawSelectLine(canvas: Canvas) {
        paint.alpha = 255
        paint.color = ContextCompat.getColor(context, R.color.common_color_FEE15D)

        val path = Path()
        val topStart = paddingTop + selectLineHeight - context.resources.getDimension(R.dimen.common_1dp)
        val startValue = width - paddingEnd - mLineLongWidth
        val bottomStart = topStart + context.resources.getDimension(R.dimen.common_2dp)
        path.lineTo(startValue, scrollY + bottomStart)
        path.lineTo(startValue, scrollY + topStart)
        path.lineTo(startValue + context.resources.getDimension(R.dimen.common_12dp), scrollY + topStart)
        val topEnd = paddingTop + selectLineHeight - context.resources.getDimension(R.dimen.common_4dp)
        path.lineTo(width - paddingEnd - 0f, scrollY + topEnd)
        path.lineTo(width - paddingEnd - 0f, scrollY + topEnd + context.resources.getDimension(R.dimen.common_8dp))
        path.lineTo(startValue + context.resources.getDimension(R.dimen.common_12dp), scrollY + bottomStart)
        path.lineTo(startValue, scrollY + bottomStart)
        canvas.drawPath(path, paint)
    }

    private var eventY = 0f
    private var lastTime = SystemClock.elapsedRealtime()
    private val time_interval = 20L
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                mLastY = event.y
                mLastX = event.x
                eventY = mLastY
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                var dy = event.y - eventY
                eventY = event.y
                if (dy >= 0) {
                    dy = Math.min(dy, mLineGap)
                } else {
                    dy = Math.max(dy, -mLineGap)
                }
                scrollBy(0, -dy.toInt())
                val scale = findScale()
                scale?.let {
                    if (it.value == lastSelectScale?.value) {
                        return@let
                    }
                    val current = SystemClock.elapsedRealtime()
                    if (current - lastTime < 100) {
                        return@let
                    }
                    lastTime = current
                    lastSelectScale = it
                    performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    mOnScaleListener?.onValueChangeListener(it.value)
                }
            }

            else -> {
                mOldScroll = scrollY
                isStartSlop = false
                onScrollStop()
            }
        }
        return super.onTouchEvent(event)
    }

    private fun onScrollStop() {
        // 超过最上面回弹
        if (mOldScroll < -selectLineHeight) {
            mOldScroll = -selectLineHeight.toInt()
            scrollY = mOldScroll
            if (scales.isNotEmpty()) {
                val tempScale = scales.first()
                if (tempScale.value == lastSelectScale?.value) {
                    return
                }
                handler.postDelayed({
                    mOnScaleListener?.onValueChangeListener(tempScale.value)
                }, time_interval)
            }
        } else if (scales.isNotEmpty() && mOldScroll > scales.last().startY - selectLineHeight) { // 超过最下面回弹
            mOldScroll = (scales.last().startY.toInt() - selectLineHeight).toInt()
            scrollY = mOldScroll
            if (scales.isNotEmpty()) {
                val tempScale = scales.last()
                if (tempScale.value == lastSelectScale?.value) {
                    return
                }
                handler.postDelayed({
                    mOnScaleListener?.onValueChangeListener(tempScale.value)
                }, time_interval)
            }
        } else {
            val scale = findScale()
            scale?.let {
                scrollY = (it.startY - selectLineHeight).toInt()
                if (it.value == lastSelectScale?.value) {
                    return@let
                }
                handler.postDelayed({
                    mOnScaleListener?.onValueChangeListener(it.value)
                }, time_interval)
            }
        }
    }

    fun setCurrentValue(value: Int) {
        if (scales.isNotEmpty()) {
            if (value < scales.last().value) {
                val i = scales.size - 1
                if (i != 0) {
                    val targetY = scales[i - 1].startY + mLineGap * (((value - scales[i - 1].value) * 1.0f) / (scales[i].value - scales[i - 1].value))
                    scrollY = (targetY - selectLineHeight).toInt()
                    mOldScroll = scrollY
                } else {
                    scrollY = (scales[i].startY - selectLineHeight).toInt()
                    mOldScroll = scrollY
                }
                return
            }
            for (i in 0 until scales.size) {
                if (scales[i].value <= value) {
                    if (i != 0) {
                        val targetY =
                            scales[i - 1].startY + mLineGap * (((value - scales[i - 1].value) * 1.0f) / (scales[i].value - scales[i - 1].value))
                        scrollY = (targetY - selectLineHeight).toInt()
                        mOldScroll = scrollY
                        break
                    } else {
                        scrollY = (scales[i].startY - selectLineHeight).toInt()
                        mOldScroll = scrollY
                    }
                }
            }
        }
    }

    fun resetLastSelectScale() {
        lastSelectScale = null
    }


    /**
     * 中间选中的数值
     */
    private fun findScale(): Scale? {
        val halfGap = mLineGap / 2.0f
        if (scales.isEmpty()) return null
        val selectY = scrollY + selectLineHeight
        return scales.firstOrNull { selectY < it.endY - halfGap && selectY >= it.startY - halfGap }
    }

    interface OnScaleListener {
        fun onValueChangeListener(value: Int)
    }

    class ScaleItem(val value: Int, val str: String, val longLine: Boolean, val drawStr: Boolean)
}