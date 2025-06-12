package com.autel.widget.widget.temp

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.autel.common.base.widget.FrameLayoutWidget
import com.autel.common.manager.AutelStorageManager.getPlainStorage
import com.autel.common.manager.StorageKey
import com.autel.common.model.lens.ITransLocation
import com.autel.common.sdk.ModelXDroneConst
import com.autel.common.utils.TransformUtils
import com.autel.drone.sdk.vmodelx.dronestate.ThermalCameraData
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.camera.enums.TemperatureModeEnum
import com.autel.drone.sdk.vmodelx.module.camera.bean.GimbalTypeEnum
import com.autel.drone.sdk.vmodelx.module.camera.bean.LensTypeEnum
import com.autel.log.AutelLog
import com.autel.widget.R
import com.autel.common.model.lens.ILens
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

/**
 * created by  2023/1/8
 */
class TempMeasureWidget @JvmOverloads constructor(
    context: Context,
    private val iTrans: ITransLocation,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) :
    FrameLayoutWidget(context, attrs, defStyleAttr, defStyleRes), ILens {

    companion object {
        //拖拽方向常量
        private const val DRAG_LEFT_TOP = 1
        private const val DRAG_RIGHT_TOP = 2
        private const val DRAG_LEFT_BOTTOM = 3
        private const val DRAG_RIGHT_BOTTOM = 4
        private const val DRAG_CENTER = 5
        private const val DRAG_NONE = 6

        //区域测温最大化的比例和限制性的框的大小(根据UI稿直接换算过来)
        private const val mRegionLimitRatioX = 0.09277f
        private const val mRegionLimitRatioY = 0.253906f
        private const val mRegionLimitRatioW = 0.8134f
        private const val mRegionLimitRatioH = 0.62453f

        //区域测温文本上下间距
        private const val REGION_TEXT_SPACING = 10
    }

    private val widgetModel = TempMeasureWidgetModel(iTrans)
    private var dragDirection = DRAG_NONE //拖拽方向记录
    private var lastTouchX = 0f //上次事件的x坐标
    private var lastTouchY = 0f //上次事件的y坐标
    private val DEFAULT_SIZE = context.resources.getDimension(R.dimen.common_100dp) //弹窗的默认大小
    private val DEFAULT_OFFSET_X = DEFAULT_SIZE / 2
    private val DEFAULT_OFFSET_Y = DEFAULT_SIZE / 2
    private var centerX = 0f //区域测温时的中心坐标x
    private var centerY = 0f //区域测温时的中心坐标y
    private var lastCenterX = 0f
    private var lastCenterY = 0f
    private var offsetX = 0f //区域测温时的中心的水平偏移长度
    private var offsetY = 0f //区域测温时的中心的垂直偏移长度
    private var lastOffsetX = 0f //区域测温时的中心的水平偏移长度
    private var lastOffsetY = 0f //区域测温时的中心的垂直偏移长度
    private var minWarningBitmap: Bitmap//低温告警图片
    private var maxWarningBitmap: Bitmap //高温告警图片
    private var mTouchTempBp: Bitmap//触摸点测温位图
    private var mColdPointBp: Bitmap //冷点测温位图
    private var mHotPointBp: Bitmap//热点测温位图

    private val roundRectRegionRadius = context.resources.getDimension(R.dimen.common_4_5dp) //区域测温矩形圆角
    private val roundRectBoldSize = context.resources.getDimension(R.dimen.common_24dp) //加粗范围的圆角
    private val roundRectBoldWidth = context.resources.getDimension(R.dimen.common_3dp) //粗线
    private val roundRectThinWidth = context.resources.getDimension(R.dimen.common_1_5dp) //细线
    private var mode = TemperatureModeEnum.NONE //当前的测温模式
    private var info: ThermalCameraData? = null //当前的测温信息

    private var bmPaint: Paint //绘制图片和边框
    private var fontPaint: Paint//绘制文本信息
    private val regionRectF = RectF() //计算区域测温方框的位置
    private val fontRect = Rect()//用于文本数据的测量

    private var mCheckForLongPress: CheckForLongPress? = null //长按检测
    private var isForbiddenTempMeasure = true

    //限制性框，主要用于区域操作提示
    private val mLimitAreaRectF = RectF() //限制区域大小
    private val mLimitAreaPaint = Paint() //限制区域绘制
    private val mLimitPath = Path() //限制区域path
    private val mHandler = Handler(Looper.getMainLooper())
    private var isShowLimitArea = false //是否显示限制区域


    private var mTouchTempX = 0f //点测温的坐标比例X
    private var mTouchTempY = 0f //点测温的坐标比例Y
    private var hasRevise = false

    //区域测温辅助按键
    private val operationView = LayoutInflater.from(context).inflate(R.layout.mission_layout_temp_measure_operation_view, this, false)
    private val ivMaximize = operationView.findViewById<ImageView>(R.id.iv_maximize)
    private val tvMaximize = operationView.findViewById<TextView>(R.id.tv_maximize)
    private val llOperationClose = operationView.findViewById<LinearLayout>(R.id.ll_operation_close)
    private val llMaximize = operationView.findViewById<LinearLayout>(R.id.ll_maximize)
    private var isMaxRegion = false
    private val debugPaint = Paint(Paint.ANTI_ALIAS_FLAG) //调试画笔
    private val debug = false //调试开关，用于显示当前的框的实际大小
    private var touchRatioX = 0f
    private var touchRatioY = 0f

    private var regionalTempIsSetting = false //区域测温属性是否正在设置
    private var fullScreenListener: FullScreenListener? = null


    private val defaultOffsetX: Float = DEFAULT_OFFSET_X

    private val defaultOffsetY: Float = DEFAULT_OFFSET_Y

    private var isTriggerLongPress = false

    private val mDistanceMinLength = context.resources.getDimension(R.dimen.common_72dp)
    private var hasDispatchFull = false //单次手势中已经分发了全屏手势
    private var dispatchFullResult = false //分发全屏手势的结果

    private var onTouchValid: Boolean = false
    private var onLongPress: Boolean = false
    private var drawRegionIng: Boolean = false

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        isClickable = true
        bmPaint = Paint()
        bmPaint.color = ContextCompat.getColor(context, R.color.common_color_03fef4)
        bmPaint.strokeWidth = context.resources.getDimension(R.dimen.common_1_5dp)
        bmPaint.style = Paint.Style.STROKE
        fontPaint = Paint()
        fontPaint.color = Color.WHITE
        fontPaint.strokeWidth = 2.0f
        fontPaint.isAntiAlias = true
        fontPaint.style = Paint.Style.FILL
        fontPaint.textSize = context.resources.getDimension(R.dimen.common_text_size_sp_13)
        fontPaint.setShadowLayer(5f, 0f, 0f, Color.BLACK)
        maxWarningBitmap = BitmapFactory.decodeResource(resources, R.drawable.mission_ic_infra_max)
        minWarningBitmap = BitmapFactory.decodeResource(resources, R.drawable.mission_ic_infra_min)
        mTouchTempBp = BitmapFactory.decodeResource(resources, R.drawable.mission_ic_touch_temp)
        mColdPointBp = BitmapFactory.decodeResource(resources, R.drawable.mission_ic_cold_point)
        mHotPointBp = BitmapFactory.decodeResource(resources, R.drawable.mission_ic_hot_point)

        mLimitAreaPaint.isAntiAlias = true
        mLimitAreaPaint.strokeWidth = context.resources.getDimension(R.dimen.common_1dp)
        mLimitAreaPaint.color = ContextCompat.getColor(context, R.color.common_color_FEE15D)
        mLimitAreaPaint.style = Paint.Style.STROKE
        val dash = context.resources.getDimension(R.dimen.common_12dp)
        val dashPathEffect = DashPathEffect(floatArrayOf(dash, dash), 0f)
        mLimitAreaPaint.pathEffect = dashPathEffect
        setWillNotDraw(false)
        llOperationClose.setOnClickListener {
            val drone = getDrone()
            if (drone == null) {
                setNone()
                return@setOnClickListener
            }
            scope?.launch(CoroutineExceptionHandler { _, throwable ->

            }) {
                widgetModel.forbiddenTemperature()
            }
        }
        llMaximize.setOnClickListener {
            if (isMaxRegion) {
                val leftX = width * mRegionLimitRatioX
                val topY = height * mRegionLimitRatioY
                val widthSize = width * mRegionLimitRatioW
                val heightSize = height * mRegionLimitRatioH
                setRegion(leftX, topY, widthSize, heightSize)
            } else {
                val size = DEFAULT_SIZE
                val leftX = width / 2 - size / 2
                val topY = height / 2 - size / 2
                setRegion(leftX, topY, size, size)
            }
            scope?.launch(CoroutineExceptionHandler { _, throwable ->

            }) {
                widgetModel.setRegionTemperature(
                    (centerX - offsetX) / width.toFloat(),
                    (centerY - offsetY) / height.toFloat(),
                    offsetX * 2 / width.toFloat(),
                    offsetY * 2 / height.toFloat()
                )
                setRegionalTempIsSetting(false)
            }
        }
    }

    private fun setRegionalTempIsSetting(regionalTempIsSetting: Boolean) {
        this.regionalTempIsSetting = regionalTempIsSetting
    }

    /**
     * 更新限制区域
     */
    private fun updateLimitArea() {
        val boundRegionLimitX = width * mRegionLimitRatioX
        val boundRegionLimitH = height * mRegionLimitRatioY
        mLimitAreaRectF[boundRegionLimitX, boundRegionLimitH, boundRegionLimitX + width * mRegionLimitRatioW] =
            boundRegionLimitH + height * mRegionLimitRatioH
        mLimitPath.reset()
        mLimitPath.addRect(mLimitAreaRectF, Path.Direction.CW)
    }

    /**
     * 校验位置是否在限制区域内
     */
    private fun verifyPointInLimitArea(x: Float, y: Float): Boolean {
        return mLimitAreaRectF.isEmpty || x >= mLimitAreaRectF.left && (x <= mLimitAreaRectF.right) && (y >= mLimitAreaRectF.top) && (y <= mLimitAreaRectF.bottom)
    }


    /**
     * 切换模式时，重置相关数据
     */
    fun reset() {
        lastTouchX = 0f
        lastTouchY = 0f
        lastCenterX = 0f
        lastCenterY = 0f
        centerX = 0f
        centerY = 0f
        offsetX = 0f
        offsetY = 0f
        info = null
        mTouchTempX = 0f
        mTouchTempY = 0f
    }

    private fun setNone() {
        mode = TemperatureModeEnum.NONE
        removeView(operationView)
        reset()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateLimitArea()
        widgetModel.reset()
    }


    private fun isMaxRegion(regionWidth: Float, regionHeight: Float): Boolean {
        return regionWidth / mRegionLimitRatioW > 0.8 && regionHeight / mRegionLimitRatioH > 0.8
    }

    /**
     * 设置区域测温模式
     *
     * @param leftX 区域测温左上角
     * @param topY  区域测温左上角
     * @param widthSize     区域测温框的宽度
     * @param heightSize     区域测温框的高度
     */
    private fun setRegion(leftX: Float, topY: Float, widthSize: Float, heightSize: Float) {
        if (isForbiddenTempMeasure) return
        reset()
        mode = TemperatureModeEnum.REGION
        centerX = leftX + widthSize / 2f
        centerY = topY + heightSize / 2f
        offsetX = widthSize / 2f
        offsetY = heightSize / 2f
        invalidate()
        updateOperation()
    }

    /**
     * 设置点测温
     */
    private fun setTouch(touchRatioX: Float, touchRatioY: Float) {
        if (isForbiddenTempMeasure) return
        this.touchRatioX = touchRatioX
        this.touchRatioY = touchRatioY
        AutelLog.i("TempMeasureLayout", "设置点测温的位置touchRatioX：" + touchRatioX + "touchRatioY:" + touchRatioY)
        removeView(operationView)
        reset()
        mode = TemperatureModeEnum.TOUCH
        if (width == 0 || height == 0) {
            return
        }
        lastTouchX = touchRatioX * width
        lastTouchY = touchRatioY * height
        mTouchTempX = lastTouchX
        mTouchTempY = lastTouchY

        invalidate()
    }

    /**
     * 设置中心点测温
     */
    private fun setCenter(touchRatioX: Float, touchRatioY: Float) {
        if (isForbiddenTempMeasure) return
        AutelLog.i("TempMeasureLayout", "设置中心点测温的位置touchRatioX：" + touchRatioX + "touchRatioY:" + touchRatioY)

        removeView(operationView)
        reset()
        mode = TemperatureModeEnum.CENTER
        if (width == 0 || height == 0) {
            return
        }
        lastTouchX = touchRatioX * width
        lastTouchY = touchRatioY * height
        mTouchTempX = lastTouchX
        mTouchTempY = lastTouchY

        invalidate()
    }

    private val isShowOperation: Boolean
        private get() = !isForbiddenTempMeasure && mode === TemperatureModeEnum.REGION && centerX != 0f && centerY != 0f && offsetX != 0f && offsetY != 0f && !drawRegionIng

    private fun updateOperation() {
        if (operationView.parent == null) {
            this.addView(operationView)
        }
        if (isShowOperation) {
            operationView.visibility = VISIBLE
        } else {
            operationView.visibility = GONE
        }
        val regionWidth = offsetX * 2 / width.toFloat()
        val regionHeight = offsetY * 2 / height.toFloat()
        if (isMaxRegion(regionWidth, regionHeight)) {
            isMaxRegion = false
            ivMaximize.setImageResource(R.drawable.mission_ic_minimize)
            tvMaximize.setText(R.string.common_text_minimize)
        } else {
            isMaxRegion = true
            ivMaximize.setImageResource(R.drawable.mission_ic_maximize)
            tvMaximize.setText(R.string.common_text_maximize)
        }
        mHandler.post {
            val operationTopMargin = centerY - offsetY - context.resources.getDimension(R.dimen.common_62dp)

            val layoutParams = LayoutParams(operationView.getMeasuredWidth(), context.resources.getDimensionPixelSize(R.dimen.common_55dp))
            layoutParams.leftMargin = (centerX - offsetX).toInt()
            layoutParams.topMargin = operationTopMargin.toInt()
            operationView.layoutParams = layoutParams
        }
    }

    /**
     * 设置限制区域的显示情况
     *
     * @param isShowLimitArea 是否展示限制区域
     */
    fun setLimitAreaVisible(isShowLimitArea: Boolean) {
        this.isShowLimitArea = isShowLimitArea
        invalidate()
    }

    /**
     * 更新区域测温信息
     */
    private fun updateTemperature(info: ThermalCameraData?) {
        if (mode == TemperatureModeEnum.CENTER || mode == TemperatureModeEnum.REGION && !regionalTempIsSetting || mode == TemperatureModeEnum.TOUCH) {
            this.info = info?.copy()
            this.info?.let {
                it.hotX = (iTrans.transCameraXLocationToScreen(it.hotX * 1.0f / ModelXDroneConst.DRONE_CAMERA_COORDINATE_MAX)
                        * ModelXDroneConst.DRONE_CAMERA_COORDINATE_MAX).toInt()
                it.coldX = (iTrans.transCameraXLocationToScreen(it.coldX * 1.0f / ModelXDroneConst.DRONE_CAMERA_COORDINATE_MAX)
                        * ModelXDroneConst.DRONE_CAMERA_COORDINATE_MAX).toInt()
                it.hotY = (iTrans.transCameraYLocationToScreen(it.hotY * 1.0f / ModelXDroneConst.DRONE_CAMERA_COORDINATE_MAX)
                        * ModelXDroneConst.DRONE_CAMERA_COORDINATE_MAX).toInt()
                it.coldY = (iTrans.transCameraYLocationToScreen(it.coldY * 1.0f / ModelXDroneConst.DRONE_CAMERA_COORDINATE_MAX)
                        * ModelXDroneConst.DRONE_CAMERA_COORDINATE_MAX).toInt()

            }
            postInvalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (isForbiddenTempMeasure) {
            return
        }
        if (isShowLimitArea) {
            canvas.drawPath(mLimitPath, mLimitAreaPaint)
        }
        if (debug) {
            drawDebug(canvas)
        }
        if (mode == TemperatureModeEnum.REGION && offsetX > 0 && offsetY > 0) {
            drawRegion(canvas)
        } else if (((mode == TemperatureModeEnum.TOUCH || mode == TemperatureModeEnum.CENTER) && mTouchTempX != 0f) and (mTouchTempY != 0f)) {
            drawTouch(canvas)
        }
    }

    /**
     * 绘制调试信息，用于表示区域测温的位置大小
     */
    private fun drawDebug(canvas: Canvas) {
        debugPaint.color = Color.WHITE
        debugPaint.alpha = 127
        val l = width * mRegionLimitRatioX
        val t = height * mRegionLimitRatioY
        val r = l + width * mRegionLimitRatioW
        val b = t + height * mRegionLimitRatioH
        canvas.drawRect(l, t, r, b, debugPaint)
    }

    /**
     * 绘制区域测温
     */
    private fun drawRegion(canvas: Canvas) {
        canvas.save()
        if (centerX == 0f) {
            centerX = width / 2f
        }
        if (centerY == 0f) {
            centerY = height / 2f
        }
        canvas.translate(centerX, centerY)
        bmPaint.style = Paint.Style.STROKE
        regionRectF[-offsetX, -offsetY, offsetX] = offsetY
        drawCustomRoundRect(canvas, regionRectF)
        canvas.restore()
        if (!drawRegionIng) {
            //绘制底部区域测温相关，最大温度、最小温度
            drawRegionInfo(canvas)
            //绘制最小温度、最大温度、平均温度
            drawTempTextInfo(canvas)
        }
    }

    private val erasePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.TRANSPARENT
        style = Paint.Style.STROKE
        strokeWidth = roundRectBoldWidth + 2  // 确保完全覆盖
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    private val edgePath = Path()

    fun drawCustomRoundRect(
        canvas: Canvas,
        rect: RectF
    ) {
        val count = canvas.saveLayer(null, null)
        bmPaint.strokeWidth = roundRectBoldWidth
        canvas.drawRoundRect(rect, roundRectRegionRadius, roundRectRegionRadius, bmPaint)
        edgePath.reset()

        edgePath.apply {
            if (rect.width() > roundRectBoldSize * 2) {
                // 上边
                moveTo(rect.left + roundRectBoldSize, rect.top)
                lineTo(rect.right - roundRectBoldSize, rect.top)

                // 下边
                moveTo(rect.left + roundRectBoldSize, rect.bottom)
                lineTo(rect.right - roundRectBoldSize, rect.bottom)

            }

            if (rect.height() > roundRectBoldSize * 2) {
                // 左边
                moveTo(rect.left, rect.top + roundRectBoldSize)
                lineTo(rect.left, rect.bottom - roundRectBoldSize)

                // 右边
                moveTo(rect.right, rect.top + roundRectBoldSize)
                lineTo(rect.right, rect.bottom - roundRectBoldSize)
            }
        }
        canvas.drawPath(edgePath, erasePaint)
        canvas.restoreToCount(count)
        bmPaint.strokeWidth = roundRectThinWidth
        canvas.drawRoundRect(rect, roundRectRegionRadius, roundRectRegionRadius, bmPaint)
    }

    private fun drawTempTextInfo(canvas: Canvas) {
        val info = info
        if (info != null) {
            regionRectF[centerX - offsetX, centerY - offsetY, centerX + offsetX] = centerY + offsetY
            var fontTop = 0

            val maxTemp = context.getString(R.string.common_text_templayout_max) + TransformUtils.centigrade2Defalut(info.hotTemp / 10.0f)
            fontPaint.getTextBounds(maxTemp, 0, maxTemp.length, fontRect)
            fontTop += fontRect.height() + REGION_TEXT_SPACING
            canvas.drawText(maxTemp, regionRectF.left + 10f, regionRectF.top + fontTop, fontPaint)

            val minTemp = context.getString(R.string.common_text_templayout_min) + TransformUtils.centigrade2Defalut(info.coldTemp / 10.0f)
            fontPaint.getTextBounds(minTemp, 0, minTemp.length, fontRect)
            fontTop += fontRect.height() + REGION_TEXT_SPACING
            canvas.drawText(minTemp, regionRectF.left + 10f, regionRectF.top + fontTop, fontPaint)

            val avgTamp = context.getString(R.string.common_text_templayout_avg) + TransformUtils.centigrade2Defalut(info.averageTemp / 10.0f)
            fontPaint.getTextBounds(avgTamp, 0, avgTamp.length, fontRect)
            fontTop += fontRect.height() + REGION_TEXT_SPACING
            canvas.drawText(avgTamp, regionRectF.left + 10f, regionRectF.top + fontTop, fontPaint)
        }
    }

    /**
     * 绘制区域测温温度信息
     */
    private fun drawRegionInfo(canvas: Canvas) {
        val info = info
        if (mode === TemperatureModeEnum.REGION && info != null) {
            val coldX = info.coldX * 1.0f / ModelXDroneConst.DRONE_CAMERA_COORDINATE_MAX * width
            val coldY = info.coldY * 1.0f / ModelXDroneConst.DRONE_CAMERA_COORDINATE_MAX * height
            val hotX = info.hotX * 1.0f / ModelXDroneConst.DRONE_CAMERA_COORDINATE_MAX * width
            val hotY = info.hotY * 1.0f / ModelXDroneConst.DRONE_CAMERA_COORDINATE_MAX * height
            val hotPoint = TemperaturePoint(hotX, hotY)
            var hotBp = mHotPointBp
            if (getPlainStorage().getBooleanValue(StorageKey.PlainKey.KEY_TEMPERATURE_ALARM)) {
                if (info.hotTemp / 10.0 > getPlainStorage().getFloatValue(StorageKey.PlainKey.KEY_TEMPERATURE_ALARM_MAX)) {
                    hotBp = maxWarningBitmap
                }
            }
            drawTemperaturePoint(hotPoint, canvas, hotBp)

            val coldPoint = TemperaturePoint(coldX, coldY)
            var codeBp = mColdPointBp
            if (getPlainStorage().getBooleanValue(StorageKey.PlainKey.KEY_TEMPERATURE_ALARM)) {
                if (info.coldTemp / 10.0 < getPlainStorage().getFloatValue(StorageKey.PlainKey.KEY_TEMPERATURE_ALARM_MIN)) {
                    codeBp = minWarningBitmap
                }
            }
            drawTemperaturePoint(coldPoint, canvas, codeBp)
        }
    }

    private fun drawTemperaturePoint(point: TemperaturePoint, canvas: Canvas, bp: Bitmap) {
        canvas.drawBitmap(bp, point.pointX - bp.width / 2f, point.pointY - bp.height / 2f, bmPaint)
    }

    /**
     * 绘制点测温、中心测温
     */
    private fun drawTouch(canvas: Canvas) {
        val info = info
        if (info == null) {
            return
        }
        canvas.save()
        bmPaint.style = Paint.Style.STROKE
        bmPaint.isAntiAlias = true
        //绘制点测温图标
        canvas.drawBitmap(mTouchTempBp, mTouchTempX - mTouchTempBp.width / 2f, mTouchTempY - mTouchTempBp.height / 2f, bmPaint)
        //绘制点测温温度值
        if (mode === TemperatureModeEnum.TOUCH) {
            drawTemperatureText(info.touchTemp / 10f, mTouchTempX, mTouchTempY, canvas)
        } else {
            drawTemperatureText(info.centerTemp / 10f, mTouchTempX, mTouchTempY, canvas)
        }
        if (getPlainStorage().getBooleanValue(StorageKey.PlainKey.KEY_TEMPERATURE_ALARM)) {
            if (info.hotTemp / 10.0 > getPlainStorage().getFloatValue(StorageKey.PlainKey.KEY_TEMPERATURE_ALARM_MAX)) {
                //高温点
                canvas.drawBitmap(
                    maxWarningBitmap,
                    width * info.hotX * 1.0f / ModelXDroneConst.DRONE_CAMERA_COORDINATE_MAX - maxWarningBitmap.width / 2f,
                    height * info.hotY * 1.0f / ModelXDroneConst.DRONE_CAMERA_COORDINATE_MAX - maxWarningBitmap.height / 2f,
                    bmPaint
                )
            }
        }
        if (getPlainStorage().getBooleanValue(StorageKey.PlainKey.KEY_TEMPERATURE_ALARM)) {
            if (info.coldTemp / 10.0 < getPlainStorage().getFloatValue(StorageKey.PlainKey.KEY_TEMPERATURE_ALARM_MIN)) {
                //低温点
                canvas.drawBitmap(
                    minWarningBitmap,
                    width * info.coldX * 1.0f / ModelXDroneConst.DRONE_CAMERA_COORDINATE_MAX - minWarningBitmap.width / 2f,
                    height * info.coldY * 1.0f / ModelXDroneConst.DRONE_CAMERA_COORDINATE_MAX - minWarningBitmap.height / 2f,
                    bmPaint
                )
            }
        }
        canvas.restore()
    }

    /**
     * 绘制测温文本信息
     */
    private fun drawTemperatureText(temperature: Float, x: Float, y: Float, canvas: Canvas) {
        val text = TransformUtils.centigrade2Defalut(temperature)
        fontPaint.textSize = getResources().getDimension(R.dimen.common_text_size_sp_20)
        fontPaint.setShadowLayer(5f, 0f, 0f, Color.BLACK)
        fontPaint.getTextBounds(text, 0, text.length, fontRect)
        val bmw = mTouchTempBp.width.toFloat()
        var txtStartX = x + bmw / 2f + 10f
        if (txtStartX + fontRect.width() > width * (mRegionLimitRatioX + mRegionLimitRatioW)) {
            txtStartX = x - bmw / 2f - fontRect.width() - 10f
        }
        fontPaint.style = Paint.Style.FILL
        fontPaint.color = Color.WHITE
        fontPaint.strokeWidth = 1f
        canvas.drawText(text, txtStartX, y + fontRect.height() / 2f, fontPaint)
    }


    /**
     * 长按进入区间测温
     */
    fun onLongPress(x: Float, y: Float) {
        mode = TemperatureModeEnum.REGION
        onLongPress = true
        reset()
        lastTouchX = 0f
        lastTouchY = 0f
        lastCenterX = 0f
        lastCenterY = 0f
        centerX = x
        centerY = y
        isTriggerLongPress = true
        val defaultOffsetX = defaultOffsetX
        val defaultOffsetY = defaultOffsetY
        if (centerX - defaultOffsetX < mLimitAreaRectF.left) {
            //在左边
            centerX = mLimitAreaRectF.left + defaultOffsetX
        }
        if (centerX + defaultOffsetX > mLimitAreaRectF.right) {
            //在右边
            centerX = mLimitAreaRectF.right - defaultOffsetX
        }
        if (centerY + defaultOffsetY > mLimitAreaRectF.bottom) {
            //在下面
            centerY = mLimitAreaRectF.bottom - defaultOffsetY
        }
        if (centerY - defaultOffsetY < mLimitAreaRectF.top) {
            //在上面
            centerY = mLimitAreaRectF.top + defaultOffsetY
        }
        offsetX = defaultOffsetX
        offsetY = defaultOffsetY
        info = null
        mTouchTempX = 0f
        mTouchTempY = 0f
        invalidate()
        updateOperation()
        regionalTempIsSetting = true
        scope?.launch(CoroutineExceptionHandler { _, throwable ->

        }) {
            widgetModel.setRegionTemperature(
                (centerX - offsetX) / width.toFloat(),
                (centerY - offsetY) / height.toFloat(),
                offsetX * 2 / width.toFloat(),
                offsetY * 2 / height.toFloat()
            )
            setRegionalTempIsSetting(false)
        }
    }


    /**
     * 长按进入区间测温，点击进入 点测温
     * 处于区间测温的UI模式下，不响应点测温
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.action
        AutelLog.i("TempMeasureLayout", "on touch event , the event = $event")
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                //不在限制区域内，显示边界提示框3s
                if (!isForbiddenTempMeasure && !verifyPointInLimitArea(event.x, event.y)) {
                    onTouchValid = false
                    isShowLimitArea = true
                    invalidate()
                    mHandler.removeCallbacksAndMessages(null)
                    mHandler.postDelayed({
                        isShowLimitArea = false
                        invalidate()
                    }, 3000)
//                    return false
                }
                isTriggerLongPress = false
                onLongPress = false
                onTouchValid = true
                hasRevise = false
                lastTouchY = event.y
                lastTouchX = event.x
                dragDirection = if (mode === TemperatureModeEnum.REGION) {
                    getDirection(lastTouchX, lastTouchY)
                } else {
                    DRAG_NONE
                }
                hasDispatchFull = false
                dispatchFullResult = false
                lastCenterX = centerX
                lastCenterY = centerY
                lastOffsetX = offsetX
                lastOffsetY = offsetY
                //非区域测温且未禁止测温
                if (mode !== TemperatureModeEnum.REGION && !isForbiddenTempMeasure) {
                    checkForLongPress(event.x, event.y)
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (!onTouchValid) {
                    return false
                }
                if (dragDirection == DRAG_NONE) {
                    processDistance(lastTouchX, lastTouchY, event.x, event.y)
                    return super.onTouchEvent(event)
                }
                if (mode !== TemperatureModeEnum.REGION || isForbiddenTempMeasure) {
                    return super.onTouchEvent(event)
                }
                val defaultOffsetX = defaultOffsetX
                val defaultOffsetY = defaultOffsetY
                var eventX = event.x
                var eventY = event.y
                val l = width * mRegionLimitRatioX
                val t = height * mRegionLimitRatioY
                val r = l + width * mRegionLimitRatioW
                val b = t + height * mRegionLimitRatioH
                if (eventX < l) {
                    eventX = l
                    hasRevise = true
                }
                if (eventY > b) {
                    eventY = b
                    hasRevise = true
                }
                if (eventX > r) {
                    eventX = r
                    hasRevise = true
                }
                if (eventY < t) {
                    eventY = t
                    hasRevise = true
                }

                //x方向总共移动了多少 lastX是down事件时的位置
                val dx = eventX - lastTouchX
                //y方向总共移动了多少 lastY是down事件时的位置
                val dy = eventY - lastTouchY
                if (Math.abs(dx) > 20 || Math.abs(dy) > 20 || hasRevise) {
                    hasRevise = false
                    if (dragDirection == DRAG_RIGHT_BOTTOM) {
                        if (dx <= 0 && offsetX == defaultOffsetX && dy <= 0 && offsetY == defaultOffsetY) {
                            return super.onTouchEvent(event)
                        }
                        centerX = lastCenterX + dx / 2
                        centerY = lastCenterY + dy / 2
                        offsetX = (lastOffsetX + dx / 2).toInt().toFloat()
                        offsetY = (lastOffsetY + dy / 2).toInt().toFloat()
                        //防止左上角抖动
                        centerX = Math.max(centerX, lastCenterX - lastOffsetX + defaultOffsetX)
                        centerY = Math.max(centerY, lastCenterY - lastOffsetY + defaultOffsetY)
                    } else if (dragDirection == DRAG_LEFT_TOP) {
                        if (dx >= 0 && offsetX == defaultOffsetX && dy >= 0 && offsetY == defaultOffsetY) {
                            return super.onTouchEvent(event)
                        }
                        centerX = lastCenterX + dx / 2
                        centerY = lastCenterY + dy / 2
                        offsetX = (lastOffsetX - dx / 2).toInt().toFloat()
                        offsetY = (lastOffsetY - dy / 2).toInt().toFloat()
                        //防止右下角抖动
                        centerX = Math.min(centerX, lastCenterX + lastOffsetX - defaultOffsetX)
                        centerY = Math.min(centerY, lastCenterY + lastOffsetY - defaultOffsetY)
                    } else if (dragDirection == DRAG_LEFT_BOTTOM) {
                        if (dx >= 0 && offsetX == defaultOffsetX && dy <= 0 && offsetY == defaultOffsetY) {
                            return super.onTouchEvent(event)
                        }
                        centerX = lastCenterX + dx / 2
                        centerY = lastCenterY + dy / 2
                        offsetX = (lastOffsetX - dx / 2).toInt().toFloat()
                        offsetY = (lastOffsetY + dy / 2).toInt().toFloat()
                        //防止右上角抖动
                        centerX = Math.min(centerX, lastCenterX + lastOffsetX - defaultOffsetX)
                        centerY = Math.max(centerY, lastCenterY - lastOffsetY + defaultOffsetY)
                    } else if (dragDirection == DRAG_RIGHT_TOP) {
                        if (dx <= 0 && offsetX == defaultOffsetX && dy >= 0 && offsetY == defaultOffsetY) {
                            return super.onTouchEvent(event)
                        }
                        centerX = lastCenterX + dx / 2
                        centerY = lastCenterY + dy / 2
                        offsetX = (lastOffsetX + dx / 2).toInt().toFloat()
                        offsetY = (lastOffsetY - dy / 2).toInt().toFloat()
                        //防止左下角抖动
                        centerX = Math.max(centerX, lastCenterX - lastOffsetX + defaultOffsetX)
                        centerY = Math.min(centerY, lastCenterY + lastOffsetY - defaultOffsetY)
                    } else {
                        //lastCenterX：本次滑动事件DOWN时，centerX的位置
                        //lastCenterY：本次滑动事件DOWN时，centerY的位置
                        centerX = lastCenterX + dx
                        centerY = lastCenterY + dy
                    }
                    offsetX = Math.max(offsetX, defaultOffsetX)
                    offsetY = Math.max(offsetY, defaultOffsetY)
                    offsetX = Math.min(offsetX, width * mRegionLimitRatioW / 2)
                    offsetY = Math.min(offsetY, height * mRegionLimitRatioH / 2)
                    if (centerX - offsetX <= mRegionLimitRatioX * width) {
                        centerX = offsetX + mRegionLimitRatioX * width
                    } else if (centerX + offsetX >= width * (mRegionLimitRatioX + mRegionLimitRatioW)) {
                        centerX = width * (mRegionLimitRatioX + mRegionLimitRatioW) - offsetX
                    }
                    if (centerY - offsetY <= mRegionLimitRatioY * height) {
                        centerY = offsetY + mRegionLimitRatioY * height
                    } else if (centerY + offsetY >= height * (mRegionLimitRatioY + mRegionLimitRatioH)) {
                        centerY = height * (mRegionLimitRatioY + mRegionLimitRatioH) - offsetY
                    }
                    invalidate()
                    updateOperation()
                }
            }

//            MotionEvent.ACTION_CANCEL,
            MotionEvent.ACTION_UP -> {
                if (!onTouchValid) {
                    drawRegionIng = false
                    return false
                }
                removeLongPressCheck()
                if (dispatchFullResult || isForbiddenTempMeasure) {
                    drawRegionIng = false
                    return super.onTouchEvent(event)
                }
                if (mode === TemperatureModeEnum.REGION) {
                    if (dragDirection == DRAG_NONE) {
                        //没有触摸边缘，则根据滑动距离判断是否需要生成新的框
                        if (onLongPress || (!drawRegionIng && !verifyPointInLimitArea(event.x, event.y) && !verifyPointInLimitArea(
                                lastTouchX,
                                lastTouchY
                            ))
                        ) {
                            return false
                        }
                        adjustRect(lastTouchX, lastTouchY, event.x, event.y)
                        scope?.launch(CoroutineExceptionHandler { _, throwable ->

                        }) {
                            widgetModel.setRegionTemperature(
                                (centerX - offsetX) / width.toFloat(),
                                (centerY - offsetY) / height.toFloat(),
                                offsetX * 2 / width.toFloat(),
                                offsetY * 2 / height.toFloat()
                            )
                            setRegionalTempIsSetting(false)
                        }
                    } else {
                        if (!isTriggerLongPress) {
                            regionalTempIsSetting = true
                            scope?.launch(CoroutineExceptionHandler { _, throwable ->

                            }) {
                                widgetModel.setRegionTemperature(
                                    (centerX - offsetX) / width.toFloat(),
                                    (centerY - offsetY) / height.toFloat(),
                                    offsetX * 2 / width.toFloat(),
                                    offsetY * 2 / height.toFloat()
                                )
                                setRegionalTempIsSetting(false)
                            }
                        }
                    }
                } else {
                    if (lastTouchX < width * mRegionLimitRatioX) {
                        return super.onTouchEvent(event)
                    }
                    if (lastTouchX > width * (mRegionLimitRatioX + mRegionLimitRatioW)) {
                        return super.onTouchEvent(event)
                    }
                    if (lastTouchY < height * mRegionLimitRatioY) {
                        return super.onTouchEvent(event)
                    }
                    if (lastTouchY > height * (mRegionLimitRatioY + mRegionLimitRatioH)) {
                        return super.onTouchEvent(event)
                    }
                    val tempTouchX = lastTouchX
                    val tempTouchY = lastTouchY
                    reset()
                    lastTouchX = tempTouchX
                    lastTouchY = tempTouchY
                    mTouchTempX = tempTouchX
                    mTouchTempY = tempTouchY
                    mode = TemperatureModeEnum.TOUCH
                    invalidate()
                    scope?.launch(CoroutineExceptionHandler { _, throwable ->

                    }) {
                        widgetModel.setPointTemperature(
                            lastTouchX / width.toFloat(), lastTouchY / height.toFloat()
                        )
                    }
                }
                drawRegionIng = false
            }


            else -> {}
        }
        return super.onTouchEvent(event)
    }


    private fun adjustRect(startX: Float, startY: Float, endX: Float, endY: Float) {
        // 规范化startX, startY, endX, endY为左上、右下
        var left: Float = Math.min(startX, endX)
        var right: Float = Math.max(startX, endX)
        var top: Float = Math.min(startY, endY)
        var bottom: Float = Math.max(startY, endY)

        val width = right - left
        val height = bottom - top

        // 如果宽或高小于 DEFAULT_SIZE，进行扩展
        if (width < DEFAULT_SIZE) {
            val expand = (DEFAULT_SIZE - width) / 2
            left -= expand
            right += expand
        }
        if (height < DEFAULT_SIZE) {
            val expand = (DEFAULT_SIZE - height) / 2
            top -= expand
            bottom += expand
        }

        // 限制到mLimitAreaRectF内
        if (left < mLimitAreaRectF.left) {
            right += (mLimitAreaRectF.left - left)
            left = mLimitAreaRectF.left
        }
        if (top < mLimitAreaRectF.top) {
            bottom += (mLimitAreaRectF.top - top)
            top = mLimitAreaRectF.top
        }
        if (right > mLimitAreaRectF.right) {
            left -= (right - mLimitAreaRectF.right)
            right = mLimitAreaRectF.right
        }
        if (bottom > mLimitAreaRectF.bottom) {
            top -= (bottom - mLimitAreaRectF.bottom)
            bottom = mLimitAreaRectF.bottom
        }

        // 防止调整后left>right 或 top>bottom
        left = max(mLimitAreaRectF.left.toDouble(), left.toDouble()).toFloat()
        top = max(mLimitAreaRectF.top.toDouble(), top.toDouble()).toFloat()
        right = min(mLimitAreaRectF.right.toDouble(), right.toDouble()).toFloat()
        bottom = min(mLimitAreaRectF.bottom.toDouble(), bottom.toDouble()).toFloat()

        centerX = (left + right) / 2
        centerY = (top + bottom) / 2
        offsetX = Math.abs(left - right) / 2
        offsetY = Math.abs(top - bottom) / 2
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        widgetModel.setup()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        widgetModel.cleanup()
    }

    override fun reactToModelChanges() {
        super.reactToModelChanges()
        widgetModel.thermalCameraDataFlow.subscribe {
            updateTemperature(it)
        }
        widgetModel.thermalTempAttrDataFlow.subscribe {
            if (it.tempMode === TemperatureModeEnum.REGION) {
                val regionXRatio = iTrans.transCameraXLocationToScreen(it.regionX * 1.0f / ModelXDroneConst.DRONE_CAMERA_COORDINATE_MAX)
                val regionYRatio = iTrans.transCameraYLocationToScreen(it.regionY * 1.0f / ModelXDroneConst.DRONE_CAMERA_COORDINATE_MAX)
                val regionWidth = iTrans.transCameraXSizeToScreen(it.regionW * 1.0f / ModelXDroneConst.DRONE_CAMERA_COORDINATE_MAX)
                val regionHeight = iTrans.transCameraYSizeToScreen(it.regionH * 1.0f / ModelXDroneConst.DRONE_CAMERA_COORDINATE_MAX)
                val leftX = regionXRatio * width
                val topY = regionYRatio * height
                val width = regionWidth * width
                val height = regionHeight * height
                setRegion(
                    leftX,
                    topY,
                    width,
                    height
                )
            } else if (it.tempMode === TemperatureModeEnum.TOUCH) {
                setTouch(
                    iTrans.transCameraXLocationToScreen(it.touchX * 1.0f / ModelXDroneConst.DRONE_CAMERA_COORDINATE_MAX),
                    iTrans.transCameraYLocationToScreen(it.touchY * 1.0f / ModelXDroneConst.DRONE_CAMERA_COORDINATE_MAX)
                )
            } else if (it.tempMode == TemperatureModeEnum.CENTER) {
                setCenter(
                    iTrans.transCameraXLocationToScreen(it.touchX * 1.0f / ModelXDroneConst.DRONE_CAMERA_COORDINATE_MAX),
                    iTrans.transCameraYLocationToScreen(it.touchY * 1.0f / ModelXDroneConst.DRONE_CAMERA_COORDINATE_MAX)
                )
            } else {
                setNone()
            }
        }
        widgetModel.canTempMeasureFlow.subscribe {
            setForbiddenTempMeasure(!it)
        }
    }

    private fun processDistance(lTouchX: Float, lTouchY: Float, eventX: Float, eventY: Float) {
        val fullState = fullScreenListener?.getFullScreenSwitch() == true
        val dx = (eventX - lTouchX).toDouble()
        val dy = (eventY - lTouchY).toDouble()
        if (!drawRegionIng) {
            if (fullState && !hasDispatchFull && dy > 0) {
                //处于全屏状态下，且向下滑动超过阈值，则关闭全屏模式
                if (dy > mDistanceMinLength) {
                    hasDispatchFull = true
                    dispatchFullResult = fullScreenListener?.fullScreenSwitch(dy < 0) ?: false
                }
                return
            }

            if (!fullState && !hasDispatchFull && dy < 0) {
                //处于全屏状态下，且向下滑动超过阈值，则关闭全屏模式
                if (dy < -mDistanceMinLength) {
                    hasDispatchFull = true
                    dispatchFullResult = fullScreenListener?.fullScreenSwitch(dy < 0) ?: false
                }
                return
            }
        }
        if (hasDispatchFull) {
            return
        }

        if ((!verifyPointInLimitArea(lTouchX, lTouchY) && !verifyPointInLimitArea(eventX, eventY)) ||
            (!drawRegionIng && Math.hypot(dx, dy) < ViewConfiguration.getTouchSlop())
        ) {
            return
        }
        if (onLongPress) {
            return
        }
        drawRegionIng = true
        operationView.visibility = GONE
        mode = TemperatureModeEnum.REGION
        removeLongPressCheck()
        var left: Float = Math.min(lTouchX, eventX)
        var right: Float = Math.max(lTouchX, eventX)
        var top: Float = Math.min(lTouchY, eventY)
        var bottom: Float = Math.max(lTouchY, eventY)

        centerX = (left + right) / 2
        centerY = (top + bottom) / 2
        offsetX = Math.abs(left - right) / 2
        offsetY = Math.abs(top - bottom) / 2
        invalidate()
    }

    protected fun getDirection(x: Float, y: Float): Int {
        if (Math.abs(x - centerX - offsetX) < 50 && Math.abs(y - centerY - offsetY) < 50) {
            return DRAG_RIGHT_BOTTOM
        }
        if (Math.abs(centerX - offsetX - x) < 50 && Math.abs(centerY - offsetY - y) < 50) {
            return DRAG_LEFT_TOP
        }
        if (Math.abs(centerX - offsetX - x) < 50 && Math.abs(y - centerY - offsetY) < 50) {
            return DRAG_LEFT_BOTTOM
        }
        if (Math.abs(x - centerX - offsetX) < 50 && Math.abs(centerY - offsetY - y) < 50) {
            return DRAG_RIGHT_TOP
        }
        return if (Math.abs(centerX - x) < offsetX && Math.abs(centerY - y) < offsetY) DRAG_CENTER else DRAG_NONE
    }

    private fun checkForLongPress(x: Float, y: Float) {
        if (mCheckForLongPress == null) {
            mCheckForLongPress = CheckForLongPress(x, y)
            mCheckForLongPress!!.rememberWindowAttachCount()
            postDelayed(mCheckForLongPress, ViewConfiguration.getLongPressTimeout().toLong())
        }
    }

    private fun removeLongPressCheck() {
        if (mCheckForLongPress != null) {
            mCheckForLongPress!!.resetWindowAttachCount()
            removeCallbacks(mCheckForLongPress)
            mCheckForLongPress = null
        }
    }

    /**
     * 禁止测温
     */
    fun setForbiddenTempMeasure(isForbiddenTempMeasure: Boolean) {
        this.isForbiddenTempMeasure = isForbiddenTempMeasure
        if (isForbiddenTempMeasure) {
            setNone()
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

    fun setFullScreenListener(fullScreenListener: FullScreenListener) {
        this.fullScreenListener = fullScreenListener
    }

    private inner class CheckForLongPress(private val x: Float, private val y: Float) : Runnable {
        private var mWindowAttachCount = 0
        fun resetWindowAttachCount() {
            mWindowAttachCount = 0
        }

        fun rememberWindowAttachCount() {
            mWindowAttachCount = windowAttachCount
        }

        override fun run() {
            if (mWindowAttachCount == windowAttachCount) {
                mCheckForLongPress = null
                isPressed = false
                if (dispatchFullResult) {
                    return
                }
                onLongPress(x, y)
            }
        }
    }

    interface FullScreenListener {
        /**
         * 全屏手势
         */
        fun fullScreenSwitch(full: Boolean): Boolean

        fun getFullScreenSwitch(): Boolean

    }
}