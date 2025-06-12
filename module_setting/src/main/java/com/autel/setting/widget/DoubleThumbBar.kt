package com.autel.setting.widget

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.drawable.toBitmap
import com.autel.setting.R
import kotlin.math.max
import kotlin.math.min

class DoubleThumbBar(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) :
    View(context, attrs, defStyleAttr, defStyleRes) {

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(
        context,
        attrs,
        defStyleAttr,
        0
    )

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0, 0)

    constructor(context: Context) : this(context, null, 0, 0)

    private var firstThumb: Bitmap? = null

    private var secondaryThumb: Bitmap? = null

    private var _backgroundColor: Int = 0

    private var firstProgressColor: Int = 0

    private var secondaryProgressColor: Int = 0

    private var firstTextColor: Int = 0

    private var secondaryTextColor: Int = 0

    private var normalTextColor: Int = 0

    private var progressHeight = context.resources.getDimension(R.dimen.common_15dp)

    private var textSize = context.resources.getDimension(R.dimen.common_text_size_sp_15)

    private var startMargin = 0f
    private var endMargin = 0f

    private var intervalProgress = 0

    private var progressOffset: Int = 0

    var maxProgress = 100

    var firstMinProgress = 8

    var firstMaxProgress = maxProgress

    var secondlyMaxProgress = maxProgress

    var secondlyMinProgress = 0

    private val space: Float
        get() {
            return (width - startMargin - endMargin) / maxProgress
        }

    private var secondaryEndX = 0f
        set(value) {
            field = when {
                value > startMargin + secondlyMaxProgress * space -> {
                    startMargin + secondlyMaxProgress * space
                }
                value < startMargin + secondlyMinProgress * space -> {
                    val mix = startMargin + secondlyMinProgress * space
                    max(mix, firstEndX + intervalProgress * space)
                }
                value < firstEndX + intervalProgress * space -> {
                    firstEndX + intervalProgress * space
                }
                else -> value
            }
            invalidate()
        }

    private var firstEndX = 0f
        set(value) {
            field = when {
                value > startMargin + firstMaxProgress * space -> {
                    min(
                        startMargin + firstMaxProgress * space,
                        secondaryEndX - intervalProgress * space
                    )
                }
                value > secondaryEndX - intervalProgress * space -> {
                    secondaryEndX - intervalProgress * space
                }
                value < startMargin + firstMinProgress * space -> {
                    startMargin + firstMinProgress * space
                }
                else -> value
            }
            invalidate()
        }

    var secondaryProgress: Int = 0
        set(value) {
            field = value - progressOffset
            secondaryEndX = startMargin + field * space
            invalidate()
        }
        get() {
            val i: Float = maxProgress.toFloat() / (width - startMargin - endMargin)
            val value = ((secondaryEndX - startMargin) * i + 0.5f).toInt()
            if (value < secondlyMinProgress) return secondlyMinProgress
            if (value > secondlyMaxProgress) return secondlyMaxProgress
            return value
        }

    var firstProgress: Int = 0
        set(value) {
            field = value - progressOffset
            firstEndX = startMargin + field * space
            invalidate()
        }
        get() {
            val i: Float = maxProgress.toFloat() / (width - startMargin - endMargin)
            val value = ((firstEndX - startMargin) * i + 0.5f).toInt()
            if (value < firstMinProgress) return firstMinProgress
            if (value > firstMaxProgress) return firstMaxProgress
            return value
        }

    private val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeCap = Paint.Cap.ROUND
        strokeWidth = progressHeight
        textSize = this.textSize
    }

    private val secondaryRect = RectF()

    private val firstRect = RectF()

    private var onProgressListener: OnProgressListener? = null

    private val msg1: String
    private val msg2: String

    init {
        if (attrs != null) {
            val array = context?.obtainStyledAttributes(attrs, R.styleable.DoubleThumbBar)
            array?.let {
                _backgroundColor =
                    it.getColor(
                        R.styleable.DoubleThumbBar_bar_progressBackgroundColor,
                        Color.WHITE
                    )
                firstProgressColor =
                    it.getColor(
                        R.styleable.DoubleThumbBar_bar_firstProgressColor,
                        Color.WHITE
                    )
                secondaryProgressColor =
                    it.getColor(
                        R.styleable.DoubleThumbBar_bar_secondaryProgressColor,
                        Color.WHITE
                    )
                firstTextColor =
                    it.getColor(
                        R.styleable.DoubleThumbBar_bar_firstTextColor,
                        Color.BLACK
                    )
                secondaryTextColor =
                    it.getColor(
                        R.styleable.DoubleThumbBar_bar_secondaryTextColor,
                        Color.BLACK
                    )
                normalTextColor =
                    it.getColor(
                        R.styleable.DoubleThumbBar_bar_normalTextColor,
                        Color.BLACK
                    )
                val thumbSize =
                    it.getDimensionPixelSize(
                        R.styleable.DoubleThumbBar_bar_thumbSize,
                        0
                    )
                if (thumbSize != 0) {
                    firstThumb =
                        it.getDrawable(R.styleable.DoubleThumbBar_bar_firstThumb)
                            ?.toBitmap(thumbSize, thumbSize)
                    secondaryThumb =
                        it.getDrawable(R.styleable.DoubleThumbBar_bar_secondaryThumb)
                            ?.toBitmap(thumbSize, thumbSize)
                } else {
                    firstThumb =
                        it.getDrawable(R.styleable.DoubleThumbBar_bar_firstThumb)
                            ?.toBitmap()
                    secondaryThumb =
                        it.getDrawable(R.styleable.DoubleThumbBar_bar_secondaryThumb)
                            ?.toBitmap()
                }
                progressHeight = it.getDimensionPixelSize(
                    R.styleable.DoubleThumbBar_bar_progressHeight,
                    context.resources.getDimensionPixelSize(R.dimen.common_15dp)
                ).toFloat()
                paint.strokeWidth = progressHeight

                textSize = it.getDimension(
                    R.styleable.DoubleThumbBar_bar_tipTextSize,
                    context.resources.getDimension(R.dimen.common_text_size_sp_15)
                )
                paint.textSize = textSize

                maxProgress =
                    it.getInt(R.styleable.DoubleThumbBar_bar_maxProgress, 100)

                startMargin =
                    it.getDimension(R.styleable.DoubleThumbBar_bar_startMargin, 0f)
                endMargin =
                    it.getDimension(R.styleable.DoubleThumbBar_bar_endMargin, 0f)

                intervalProgress =
                    it.getInt(R.styleable.DoubleThumbBar_bar_intervalProgress, 0)
                firstMinProgress =
                    it.getInt(R.styleable.DoubleThumbBar_bar_firstMinProgress, 8)
                firstMaxProgress =
                    it.getInt(
                        R.styleable.DoubleThumbBar_bar_firstMaxProgress,
                        maxProgress
                    )

                secondlyMinProgress =
                    it.getInt(R.styleable.DoubleThumbBar_bar_secondaryMinProgress, 0)
                secondlyMaxProgress =
                    it.getInt(
                        R.styleable.DoubleThumbBar_bar_secondaryMaxProgress,
                        maxProgress
                    )
                progressOffset =
                    it.getInt(R.styleable.DoubleThumbBar_bar_progressOffset, 0)
                it.recycle()

                post {
                    secondaryEndX = 0f
                    firstEndX = 0f
                }
            }
        }

        msg1 = resources.getString(R.string.common_text_low_battery_alarm)
        msg2 = resources.getString(R.string.common_text_serious_low_battery_alarm)
    }

    fun setOnProgressListener(
        listener: OnProgressListener
    ) {
        this.onProgressListener = listener
    }

    override fun onDraw(canvas: Canvas) {
        canvas?.run {
            drawProgress()
            drawThumb()
            drawText()
        }
    }

    private fun Canvas.drawProgress() {
        //背景
        paint.color = _backgroundColor
        drawLine(
            paint.strokeWidth + startMargin,
            height / 2f,
            width - paint.strokeWidth - endMargin,
            height / 2f,
            paint
        )

        //第二进度
        paint.color = secondaryProgressColor
        val startX = paint.strokeWidth + startMargin
        val startY = height / 2f
        drawLine(
            startX,
            startY,
            secondaryEndX,
            startY,
            paint
        )

        //第一进度
        paint.color = firstProgressColor
        drawLine(
            startX,
            startY,
            firstEndX,
            startY,
            paint
        )
    }

    private fun Canvas.drawThumb() {
        if (secondaryThumb != null) {
            // 增加下触摸范围
            secondaryRect.left = secondaryEndX - secondaryThumb!!.width / 2f
            secondaryRect.top =
                height / 2 - secondaryThumb!!.height / 2f - secondaryThumb!!.width / 2f
            secondaryRect.bottom =
                secondaryRect.top + secondaryThumb!!.height + secondaryThumb!!.width / 2f
            secondaryRect.right =
                secondaryRect.left + secondaryThumb!!.width + secondaryThumb!!.width / 2f

            drawBitmap(
                secondaryThumb!!,
                secondaryEndX - secondaryThumb!!.width / 2f,
                height / 2 - secondaryThumb!!.height / 2f,
                paint
            )
        }
        if (firstThumb != null) {
            // 增加下触摸范围
            firstRect.left = firstEndX - firstThumb!!.width / 2f
            firstRect.top = height / 2 - firstThumb!!.height / 2f - firstThumb!!.width / 2f
            firstRect.bottom = firstRect.top + firstThumb!!.height + firstThumb!!.width / 2f
            firstRect.right = firstRect.left + firstThumb!!.width + firstThumb!!.width / 2f
            drawBitmap(
                firstThumb!!,
                firstEndX - firstThumb!!.width / 2f,
                height / 2 - firstThumb!!.height / 2f,
                paint
            )
        }
    }

    private fun Canvas.drawText() {
        paint.color = secondaryTextColor
        val text1 = "${secondaryProgress + progressOffset}%  "
        val text = "$text1 $msg1"
        val w = paint.measureText(text)
        var text1StartX = secondaryEndX
        if (text1StartX + w > width) {
            text1StartX = width - w - endMargin
        }
        drawText(text1, text1StartX, height / 2f + (secondaryThumb?.height ?: 0) + context.resources.getDimension(R.dimen.common_10dp), paint)

        paint.color = normalTextColor
        val w1 = paint.measureText(text1)
        drawText(msg1, text1StartX + w1, height / 2f + (secondaryThumb?.height ?: 0) + context.resources.getDimension(R.dimen.common_10dp), paint)

        paint.color = firstTextColor
        val text3 = "${firstProgress + progressOffset}%  "
        val text2 = "$text3 $msg2"
        val w2 = paint.measureText(text2)
        var text2StartX = firstEndX
        if (text2StartX + w2 > width) {
            text2StartX = width - w2
        }
        drawText(text3, text2StartX, height / 2f - (secondaryThumb?.height ?: 0) / 2f - context.resources.getDimension(R.dimen.common_10dp), paint)

        paint.color = normalTextColor
        val w3 = paint.measureText(text3)
        drawText(msg2, text2StartX + w3, height / 2f - (secondaryThumb?.height ?: 0) / 2f - context.resources.getDimension(R.dimen.common_10dp), paint)
    }

    /**
     * 1: secondaryRect
     * 2: firstRect
     */
    private var touchType = 0

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                //触摸范围扩大5倍
                if (secondaryRect.contains(x, y)) {
                    touchType = 1
                    parent.requestDisallowInterceptTouchEvent(true);
                    return true
                }
                if (firstRect.contains(x, y)) {
                    touchType = 2
                    parent.requestDisallowInterceptTouchEvent(true);
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                var moveX = when {
                    x > width - endMargin -> {
                        width - endMargin
                    }
                    x < startMargin -> {
                        startMargin
                    }
                    else -> {
                        x
                    }
                }
                if (touchType == 1) {
                    secondaryEndX = moveX
                } else if (touchType == 2) {
                    firstEndX = moveX
                }
            }
            MotionEvent.ACTION_UP -> {
                onProgressListener?.let {
                    if (touchType == 1) {
                        it.onProgressChanged(firstProgress + progressOffset, secondaryProgress + progressOffset)
                    } else if (touchType == 2) {
                        it.onProgressChanged(firstProgress + progressOffset, secondaryProgress + progressOffset)
                    }
                }
                touchType = 0
            }
        }
        return super.dispatchTouchEvent(event)
    }

    interface OnProgressListener {
        fun onProgressChanged(firstProgress: Int, secondProgress: Int)
    }
}