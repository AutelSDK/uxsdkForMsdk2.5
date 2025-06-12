package com.autel.widget.widget.compass

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import com.autel.common.base.widget.StandardViewWidget
import com.autel.common.utils.AnimateUtil.animateProperty
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.module.camera.bean.GimbalTypeEnum
import com.autel.drone.sdk.vmodelx.module.camera.bean.LensTypeEnum
import com.autel.widget.R
import com.autel.common.model.lens.ILens

/**
 * 循环刻度尺
 */
class LoopScaleView : StandardViewWidget, ILens {
    //尺子控件总宽度
    private var viewWidth = 0f

    //尺子控件总宽度
    private var viewHeight = 0f

    //中间的标识图片
    private var cursorMap: Bitmap? = null

    //标签的位置
    private var cursorLocation = 0f

    //未设置标识图片时默认绘制一条线作为标尺的线的颜色
    private var cursorColor = Color.RED

    //设置屏幕宽度内最多显示的大刻度数，默认为3个
    private var showItemSize = 3

    //标尺开始位置
    private var currLocation = 0f

    private var yaw = 0f

    //刻度表的最大值，默认为200
    private var maxValue = 72

    //一个刻度表示的值的大小
    private var oneItemValue = 1

    //设置刻度线间宽度,大小由 showItemSize确定
    private var scaleDistance = 0

    //刻度文字的颜色，默认为灰色
    private var scaleTextColor = Color.GRAY

    private val scaleName: ArrayList<*> = ArrayList(mutableListOf("0", "45", "E", "135", "180", "225", "W", "315"))

    private val greenPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val blackPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val widgetModel: LoopScaleVM by lazy {
        LoopScaleVM()
    }

    @JvmOverloads
    constructor(context: Context, attrs: AttributeSet? = null) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.north_LoopScaleView)
        showItemSize = ta.getInteger(R.styleable.north_LoopScaleView_north_maxShowItem, showItemSize)
        maxValue = ta.getInteger(R.styleable.north_LoopScaleView_north_maxValue, maxValue)
        oneItemValue = ta.getInteger(R.styleable.north_LoopScaleView_north_oneItemValue, oneItemValue)
        scaleTextColor = ta.getColor(R.styleable.north_LoopScaleView_north_scaleTextColor, scaleTextColor)
        cursorColor = ta.getColor(R.styleable.north_LoopScaleView_north_cursorColor, cursorColor)
        val cursorMapId = ta.getResourceId(R.styleable.north_LoopScaleView_north_cursorMap, -1)
        if (cursorMapId != -1) {
            cursorMap = BitmapFactory.decodeResource(resources, cursorMapId)
        }
        ta.recycle()

        greenPaint.color = Color.parseColor("#03FEF4")
        greenPaint.strokeWidth = 4.0f
        greenPaint.style = Paint.Style.STROKE
        greenPaint.isAntiAlias = true

        textPaint.color = Color.parseColor("#03FEF4")
        textPaint.textSize = resources.getDimension(R.dimen.common_text_size_sp_12)
        textPaint.style = Paint.Style.FILL
        textPaint.isAntiAlias = true

        blackPaint.color = Color.BLACK
        blackPaint.strokeWidth = 5.0f
        blackPaint.style = Paint.Style.STROKE
        blackPaint.isAntiAlias = true
        blackPaint.textSize = resources.getDimension(R.dimen.common_text_size_sp_13)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        viewHeight = MeasureSpec.getSize(heightMeasureSpec).toFloat()
        //一个小刻度的宽度（十进制，每9个小刻度为一个大刻度）
        scaleDistance = measuredWidth / (showItemSize * 9)
        //尺子长度总的个数*一个的宽度
        viewWidth = (maxValue / oneItemValue * scaleDistance).toFloat()
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        canvas.clipRect(paddingStart.toFloat(), paddingTop.toFloat(), (width - paddingRight).toFloat(), viewHeight - paddingBottom)
        drawCursor(canvas)
        for (i in 0 until maxValue / oneItemValue) {
            drawScale(canvas, i, -1)
        }
        for (i in 0 until maxValue / oneItemValue) {
            drawScale(canvas, i, 1)
        }
    }


    /**
     * 绘制指示标签
     *
     * @param canvas 绘制控件的画布
     */
    //指示图标距离顶部的位置
    var top: Float = 150f
    var left: Float = 0f
    var right: Float = 0f
    var bottom: Float = 0f

    var rectF: RectF = RectF()


    var value: String = "0"

    private fun drawCursor(canvas: Canvas) {
        cursorLocation = width / 2f //画布的中心点
        if (cursorMap == null) { //绘制一条红色的竖线线
            canvas.drawLine(cursorLocation, (paddingTop - paddingBottom).toFloat(), cursorLocation, viewHeight - paddingBottom, greenPaint)
        } else { //绘制标识图片
            left = cursorLocation - cursorMap!!.width / 2
            right = cursorLocation + cursorMap!!.width / 2
            bottom = top + cursorMap!!.height
            rectF[left, top, right] = bottom
            canvas.drawBitmap(cursorMap!!, null, rectF, greenPaint)
            value = (yaw / 2f).toInt().toString()
            canvas.drawText(value, width / 2f - textPaint.measureText(value) / 2f, bottom + 25, textPaint)
        }
    }


    /**
     * 绘制刻度线
     *
     * @param canvas 画布
     * @param value  刻度值
     * @param type   正向绘制还是逆向绘制
     */
    var position: Int = 0
    var maxLine: Int = 140
    var minLine: Int = 110

    var lingMarginTop: Int = 70

    var scaleDistanceNumber: Int = 8

    private fun drawScale(canvas: Canvas, value: Int, type: Int) {
        var value = value
        if (currLocation + showItemSize / 2 * scaleDistanceNumber * scaleDistance >= viewWidth) {
            currLocation = (-showItemSize / 2 * scaleDistanceNumber * scaleDistance).toFloat()
        } else if (currLocation - showItemSize / 2 * scaleDistanceNumber * scaleDistance <= -viewWidth) {
            currLocation = (showItemSize / 2 * scaleDistanceNumber * scaleDistance).toFloat()
        }
        val location = cursorLocation - currLocation + value * scaleDistance * type
        if (value % 9 == 0) {
            canvas.drawLine(location, lingMarginTop.toFloat(), location, maxLine.toFloat(), blackPaint)
            canvas.drawLine(location, lingMarginTop.toFloat(), location, maxLine.toFloat(), greenPaint)
            if (type < 0) {
                value = (maxValue / oneItemValue - value) * oneItemValue //按每一个刻度代表的值进行缩放
                if (value == maxValue) { //左闭右开区间，不取最大值
                    value = 0
                }
            } else {
                value = value * oneItemValue
            }
            position = value / 8
            if (position < scaleName.size) {
                val drawStr = scaleName[position].toString()
                canvas.drawText(drawStr, location - greenPaint.measureText(drawStr), 50f, blackPaint)
                canvas.drawText(drawStr, location - greenPaint.measureText(drawStr), 50f, textPaint)
            }
        } else {
            canvas.drawLine(location, lingMarginTop.toFloat(), location, minLine.toFloat(), blackPaint)
            canvas.drawLine(location, lingMarginTop.toFloat(), location, minLine.toFloat(), greenPaint)
        }
    }


    fun updateYaw(yaw: Float) {
        if (yaw < 0) {
            this.yaw = 360 + yaw
        } else {
            this.yaw = yaw
        }
        this.yaw *= 2f
    }

    private fun updateCurrLocation(distance: Float) {
        if (distance == 0f) {
            return
        }
        currLocation += (260 / 90.0 * distance).toFloat()
        //设置新的位置
        setCurrLocation(currLocation)
    }

    /**
     * 设置屏幕宽度内大Item的数量
     *
     * @param showItemSize 屏幕宽度内显示的大 item数量
     */
    fun setShowItemSize(showItemSize: Int) {
        this.showItemSize = showItemSize
        invalidate()
    }

    fun setCoursorBitmap(resId: Int) {
        cursorMap = BitmapFactory.decodeResource(resources, resId)
    }

    /**
     * 设置当前游标所在的值
     *
     * @param currLocation 当前游标所在的值
     */
    private fun setCurrLocation(currLocation: Float) {
        this.currLocation = currLocation
        invalidate()
    }

    fun updatePosition(x: Int, y: Int) {
        animateProperty(this, "translationX", x.toFloat(), 250)
        animateProperty(this, "translationY", y.toFloat(), 250)
    }


    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isInEditMode) {
            widgetModel.setup()
        }
    }


    override fun onDetachedFromWindow() {
        if (!isInEditMode) {
            widgetModel.cleanup()
        }
        super.onDetachedFromWindow()
    }


    private var lastYaw = 0f
    override fun reactToModelChanges() {
        super.reactToModelChanges()
        widgetModel.loopScaleFlow.subscribe {
            val gap = it.gimbalAttitudeYaw - lastYaw
            lastYaw =  it.gimbalAttitudeYaw
            updateYaw(it.gimbalAttitudeYaw)
            updateCurrLocation(gap)
        }
    }

    override fun getDrone(): IAutelDroneDevice? {
        return widgetModel.getDrone()
    }

    override fun getGimbal(): GimbalTypeEnum? {
        return widgetModel.getGimbal()
    }

    override fun getLensTypeEnum(): LensTypeEnum? {
        return widgetModel.getLensTypeEnum()
    }

    override fun updateLensInfo(drone: IAutelDroneDevice?, gimbal: GimbalTypeEnum?, lensType: LensTypeEnum?) {
        widgetModel.updateLensInfo(drone, gimbal, lensType)
    }
}
