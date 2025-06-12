package com.autel.widget.widget.compass

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.PointF
import android.os.Handler
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.view.MotionEvent
import com.autel.aiso.AIJni
import com.autel.bean.Compass3DDataBean
import com.autel.common.base.widget.StandardViewWidget
import com.autel.common.manager.AutelStorageManager
import com.autel.common.manager.StorageKey.PlainKey.COMPASS_3D_HINT_THREE
import com.autel.common.utils.DeviceUtils
import com.autel.common.utils.TransformUtils
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.module.camera.bean.GimbalTypeEnum
import com.autel.drone.sdk.vmodelx.module.camera.bean.LensTypeEnum
import com.autel.utils.ScreenUtils
import com.autel.widget.R
import com.autel.common.model.lens.ILens
import kotlin.math.abs
import kotlin.math.sqrt

class Compass3DScaleView(context: Context, attribute: AttributeSet? = null) :
    StandardViewWidget(context, attribute), ILens {
    private val showFovScale = 0.34
    private var mWidth = width
    private var mHeight = height

    //码流裁剪偏移值
    private var offsetX = 0


    /**
     * 是否可以进行交互
     */
    private var isTouchEnable = false


    var centerBitmap: Bitmap? = null


    private val greenTextPaint = Paint().apply {
        color = Color.parseColor("#03FEF4")
        textSize = context.resources.getDimension(R.dimen.common_text_size_sp_15)
        isAntiAlias = true
    }
    private val blackTextPaint = Paint().apply {
        color = Color.BLACK
        textSize = context.resources.getDimension(R.dimen.common_text_size_sp_16)
        isFakeBoldText = true
        isAntiAlias = true
    }

    private val redPaint = Paint().apply {
        color = Color.parseColor("#E02020")
        style = Paint.Style.STROKE
        strokeWidth = context.resources.getDimension(R.dimen.common_3dp)
        isAntiAlias = true
    }


    private val bgPaint = Paint().apply {
        color = Color.parseColor("#FEE15D")
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = context.resources.getDimension(R.dimen.common_40dp)
        isAntiAlias = true
    }

    private val wihtePaint = Paint().apply {
        color = Color.WHITE
        strokeWidth = context.resources.getDimension(R.dimen.common_3dp)
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    private val trianglePaint = Paint().apply {
        color = Color.parseColor("#FEE15D")
        strokeWidth = context.resources.getDimension(R.dimen.common_3dp)
        style = Paint.Style.FILL
        isAntiAlias = true
        setShadowLayer(context.resources.getDimension(R.dimen.common_2dp), 2f, 2f, Color.BLACK)
    }

    private val bigCriclePaint = Paint().apply {
        color = Color.parseColor("#FEE15D")
        strokeWidth = context.resources.getDimension(R.dimen.common_3dp)
        style = Paint.Style.STROKE
        isAntiAlias = true
        setShadowLayer(context.resources.getDimension(R.dimen.common_2dp), 2f, 2f, Color.BLACK)
    }

    private val pressBigCriclePaint = Paint().apply {
        color = Color.parseColor("#C7C7C7")
        strokeWidth = context.resources.getDimension(R.dimen.common_3dp)
        style = Paint.Style.STROKE
        isAntiAlias = true
        setShadowLayer(context.resources.getDimension(R.dimen.common_2dp), 2f, 2f, Color.BLACK)
    }
    private val blackPaint = Paint().apply {
        color = Color.BLACK
        strokeWidth = context.resources.getDimension(R.dimen.common_4dp)
        style = Paint.Style.STROKE
        isAntiAlias = true

    }
    private val northPath = Path()
    private val eastPath = Path()
    private val northSegmentPath = Path()
    private val eastSegmentPath = Path()

    private val middleIndex = 50
    private var northData: Compass3DDataBean? = null
    private var eastData: Compass3DDataBean? = null

    private var mIHandler = Handler(context.mainLooper)

    private var canvas: Canvas? = null
    private var isShowHint = true
    private var isShowCenterImage = true
    private var showHintNumber = 0

    private val bubblePaint = Paint().apply {
        color = context.getColor(R.color.common_color_fee15d)
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val textPaint = TextPaint().apply {
        color = Color.BLACK
        textSize = context.resources.getDimension(R.dimen.common_text_size_sp_15)
        isAntiAlias = true
    }

    private val bubbleText = context.resources.getString(R.string.common_text_press_hint)
    private val maxWidth = context.resources.getDimension(R.dimen.common_350dp)
    private val horizontalPadding = context.resources.getDimension(R.dimen.common_15dp)
    private val verticalPadding = context.resources.getDimension(R.dimen.common_11dp)

    private val cornerRadius = context.resources.getDimension(R.dimen.common_4dp)
    private val triangleWidth = context.resources.getDimension(R.dimen.common_10dp)
    private val triangleHeight = context.resources.getDimension(R.dimen.common_15dp)

    // 计算文本换行
    private val textLayout =
        StaticLayout.Builder.obtain(bubbleText, 0, bubbleText.length, textPaint, maxWidth.toInt() - 2 * horizontalPadding.toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1f)
            .setIncludePad(false)
            .build()

    private val textWidth = textPaint.measureText(bubbleText)

    private val bubbleWidth = minOf(textWidth + 2 * horizontalPadding, maxWidth)
    private val bubbleHeight = textLayout.height + 2 * verticalPadding

    private val bubblePath = Path()

    private val widgetModel: Compass3DVM by lazy {
        Compass3DVM()
    }

    init {
        showHintNumber = AutelStorageManager.getPlainStorage()
            .getIntValue(COMPASS_3D_HINT_THREE)
        centerBitmap =
            BitmapFactory.decodeResource(context.resources, R.drawable.icon_compass_center_point)
    }

    private fun update(
        northData: Compass3DDataBean,
        westData: Compass3DDataBean,
    ) {
        mWidth = width
        mHeight = height
        this.northData = northData
        this.eastData = westData
        postInvalidate()
    }

    fun updateTouchEnable(isTouchEnable: Boolean) {
        this.isTouchEnable = isTouchEnable
        invalidate()
    }

    private fun updateDistance(x: String, y: String) {
        targetDistanceS = x
        targetDistanceW = y
        postInvalidate()
    }


    private var longPressGap = context.resources.getDimension(R.dimen.common_20dp)
    private var pressCircleRaduis = context.resources.getDimension(R.dimen.common_30dp)
    private var pressMidCircleRaduis = context.resources.getDimension(R.dimen.common_10dp)
    private var circleRaduis = context.resources.getDimension(R.dimen.common_2dp)
    private var startX = -1f
    private var startY = -1f
    private var endX = -1f;
    private var endY = -1f;
    private var isLongPress = false
    private var isShowTargetDisatance = false

    private val longPressedCallback = Runnable {
        isShowTargetDisatance = false
        isLongPress = true
        isShowCenterImage = false
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {

        //大于0，说明在分屏下，分屏下按照产品要求不能拖动测距
        if (!isTouchEnable && DeviceUtils.isMainRC()) {
            return false
        }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (((mWidth / 2 + longPressGap) >= event.x && (mWidth / 2 - longPressGap) <= event.x
                            && (mHeight / 2 + longPressGap) >= event.y && (mHeight / 2 - longPressGap) <= event.y
                            ) || (event.x <= endX + longPressGap && event.x >= endX - longPressGap
                            && event.y <= endY + longPressGap && event.y >= endY - longPressGap)
                ) {
                    pressCircleRaduis = context.resources.getDimension(R.dimen.common_30dp)
                    pressMidCircleRaduis = context.resources.getDimension(R.dimen.common_10dp)
                    startX = width / 2f
                    startY = height / 2f
                    mIHandler.postDelayed(longPressedCallback, 300)
                    return true
                }
                if (isShowHint) {
                    AutelStorageManager.getPlainStorage()
                        .setIntValue(COMPASS_3D_HINT_THREE, ++showHintNumber)
                    isShowHint = false
                }
                reset()
                return false

            }

            MotionEvent.ACTION_MOVE -> {
                if (isLongPress) {
                    endX = event.x
                    endY = event.y
                    postInvalidate()
                } else {
                    isLongPress = false
                    mIHandler.removeCallbacksAndMessages(longPressedCallback)
                }

            }

            MotionEvent.ACTION_CANCEL,
            MotionEvent.ACTION_UP -> {
                if (isLongPress) {
                    isLongPress = false
                    pressMidCircleRaduis = circleRaduis
                    pressCircleRaduis = circleRaduis
                    isShowTargetDisatance = true
                    onCalculateDistance(event.x, event.y)
                    postInvalidate()
                }
                mIHandler.removeCallbacks(longPressedCallback)
            }
        }

        return true
    }

    /**
     * 重置测距
     */
    fun reset() {
        endX = 0f
        endY = 0f
        startX = 0f
        startY = 0f
        isShowCenterImage = true
        tPath.reset()
        postInvalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mWidth = w
        mHeight = h
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        this.canvas = canvas
        drawNorth(canvas)
        drawEast(canvas)
        drawDirection(canvas, "N", 0)
        drawDirection(canvas, "E", 1)
        drawDirection(canvas, "S", 2)
        drawDirection(canvas, "W", 3)
        if (isTouchEnable) {
            drawNorthSegment(canvas)
            drawEastSegment(canvas)
            if (DeviceUtils.isMainRC()) {
                drawLongPress(canvas)
                if (isShowCenterImage) {
                    drawCenterPoint(canvas)
                }
            }
        }
        drawDistance(canvas)
        drawBubble(canvas)
    }

    private fun drawBubble(canvas: Canvas) {
        if (showHintNumber >= 3 || !isTouchEnable) {
            return
        }
        if (isShowHint && isTouchEnable) {
            bubblePath.reset()

            // Starting from top-left corner, moving clockwise
            bubblePath.moveTo(0f, cornerRadius)
            bubblePath.quadTo(0f, 0f, cornerRadius, 0f)
            bubblePath.lineTo(bubbleWidth - cornerRadius, 0f)
            bubblePath.quadTo(bubbleWidth, 0f, bubbleWidth, cornerRadius)
            bubblePath.lineTo(bubbleWidth, bubbleHeight / 2f - triangleHeight / 2f)
            bubblePath.lineTo(bubbleWidth + triangleWidth, bubbleHeight / 2f)
            bubblePath.lineTo(bubbleWidth, bubbleHeight / 2f + triangleHeight / 2f)
            bubblePath.lineTo(bubbleWidth, bubbleHeight - cornerRadius)
            bubblePath.quadTo(bubbleWidth, bubbleHeight, bubbleWidth - cornerRadius, bubbleHeight)
            bubblePath.lineTo(cornerRadius, bubbleHeight)
            bubblePath.quadTo(0f, bubbleHeight, 0f, bubbleHeight - cornerRadius)
            bubblePath.close()

            canvas.save()
            canvas.translate(
                mWidth / 2f - bubbleWidth - triangleWidth - context.resources.getDimension(R.dimen.common_30dp),
                mHeight / 2f - bubbleHeight / 2f
            )
            canvas.drawPath(bubblePath, bubblePaint)

            // 绘制换行文本
            canvas.translate(horizontalPadding, verticalPadding)
            textLayout.draw(canvas)

            canvas.restore()
        }
    }

    private fun drawCenterPoint(canvas: Canvas) {
        centerBitmap?.let {
            canvas.drawBitmap(
                it,
                mWidth / 2f - it.width / 2f,
                mHeight / 2f - it.height / 2f,
                greenTextPaint
            )
        }

    }


    /**
     * 拖动到目标点
     */
    private var middlePointX = 0f
    private var middlePointY = 0f
    private var middlePath = Path()
    private var lineLength = 0f
    private var middlePathStartX = 0f
    private var middlePathStartY = 0f
    private var middlePathEndX = 0f
    private var middlePathEndY = 0f

    private var textlenght = 0f
    private var tPath = Path()
    private var ratio = 0f
    private var targetDistanceS = "S 100.1m"
    private var targetDistanceW = "W 150.2m"
    private var pressLinePath = Path()
    private fun drawLongPress(canvas: Canvas) {
        if (startX < 0f || startX == 0f || endX < 0f || endX == 0f || offsetX > 0) {
            return
        }
        //开始按下，则提示消失
        isShowHint = false
        pressLinePath.reset()
        pressLinePath.moveTo(startX, startY)
        pressLinePath.lineTo(endX, endY)
        canvas.drawPath(pressLinePath, blackPaint)
        bigCriclePaint.style = Paint.Style.STROKE
        canvas.drawPath(pressLinePath, bigCriclePaint)
        drawTriangle(canvas, pressLinePath)
        if (isShowTargetDisatance) {
            drawTargetDistance(canvas)
        }
        if (isLongPress) {
            bigCriclePaint.style = Paint.Style.FILL
            canvas.drawCircle(endX, endY, pressMidCircleRaduis, bigCriclePaint)
            canvas.drawCircle(endX, endY, pressCircleRaduis, pressBigCriclePaint)
        } else {
            canvas.drawCircle(endX, endY, circleRaduis, bigCriclePaint)
        }
    }

    private var trianglePath = Path()
    private fun drawTriangle(canvas: Canvas, path: Path) {
        // 计算路径的切线
        val pathMeasure = PathMeasure(path, false)
        val pos = FloatArray(2)  // 切线点的坐标
        val tan = FloatArray(2)  // 切线的方向向量

        // 获取切线点的坐标
        val centerPoint = PointF()
        pathMeasure.getPosTan(context.resources.getDimension(R.dimen.common_20dp), pos, tan)
        centerPoint.x = pos[0]
        centerPoint.y = pos[1]

        // 计算垂直于切线的向量
        val perpendicularVector = floatArrayOf(tan[1], -tan[0]) // 注意这里的顺序

        val dimen = context.resources.getDimension(R.dimen.common_10dp)
        // 计算垂直线的起始点和终止点
        val x1 = centerPoint.x - perpendicularVector[0] * dimen
        val y1 = centerPoint.y - perpendicularVector[1] * dimen
        val x2 = centerPoint.x + perpendicularVector[0] * dimen
        val y2 = centerPoint.y + perpendicularVector[1] * dimen

        // 绘制三角形
        trianglePath.reset()
        trianglePath.moveTo(startX, startY)
        trianglePath.lineTo(x1, y1)
        trianglePath.lineTo(x2, y2)
        trianglePath.close()
        canvas.drawPath(trianglePath, trianglePaint)
    }

    /**
     * 绘制距离显示
     */
    private var xYDistance = 0f
    private fun drawTargetDistance(canvas: Canvas) {


        lineLength = sqrt((endX - startX) * (endX - startX) + (endY - startY) * (endY - startY))
        textlenght = greenTextPaint.measureText(
            targetDistanceS
        )
        ratio = textlenght / lineLength


        middlePointX = (endX - startX) / 2f
        middlePointY = (endY - startY) / 2f


        middlePathStartX = startX + middlePointX - ratio * (endX - startX) / 2f
        middlePathStartY = startY + middlePointY - ratio * (endY - startY) / 2f

        middlePathEndX = startX + middlePointX + ratio * (endX - startX) / 2f
        middlePathEndY = startY + middlePointY + ratio * (endY - startY) / 2f

        middlePath.reset()
        middlePath.moveTo(
            middlePathStartX, middlePathStartY
        )
        middlePath.lineTo(middlePathEndX, middlePathEndY)
        canvas.drawPath(middlePath, bgPaint)
        xYDistance = sqrt((endX - startX) * (endX - startX) + (endY - startY) * (endY - startY))
        tPath.reset()
        if (middlePathEndX - width / 2 < 0) {
            tPath.moveTo(middlePathEndX, middlePathEndY)
            tPath.lineTo(middlePathStartX, middlePathStartY)
        } else {
            tPath.moveTo(middlePathStartX, middlePathStartY)
            tPath.lineTo(middlePathEndX, middlePathEndY)
        }

        canvas.drawTextOnPath(
            targetDistanceS,
            tPath,
            sqrt(
                (middlePathEndX - middlePathStartX) * (middlePathEndX - middlePathStartX)
                        + (middlePathEndY - middlePathStartY) * (middlePathEndY - middlePathStartY)
            ) / 2 - greenTextPaint.measureText(
                targetDistanceS
            ) / 2,
            -10f,
            blackTextPaint
        )
        canvas.drawTextOnPath(
            targetDistanceW,
            tPath,
            sqrt(
                (middlePathEndX - middlePathStartX) * (middlePathEndX - middlePathStartX)
                        + (middlePathEndY - middlePathStartY) * (middlePathEndY - middlePathStartY)
            ) / 2 - greenTextPaint.measureText(
                targetDistanceW
            ) / 2,
            blackPaint.measureText(targetDistanceW) / 2 + 5,
            blackTextPaint
        )
    }

    private var distanceX = 0f
    private var distanceY = 0f
    private var distance = "0m"
    private fun drawDistance(canvas: Canvas) {
        northData?.let {
            distanceX = it.arrow_project_x[9].toFloat() * (mWidth - offsetX * 2) + offsetX
            distanceY = it.arrow_project_y[9].toFloat() * mHeight
        }
        northData?.let {
            distance = "${it.arrow_length.toInt()} m"
        }
        canvas.drawText(distance, distanceX, distanceY, blackTextPaint)
        canvas.drawText(distance, distanceX, distanceY, greenTextPaint)
    }

    private var pointX = 0f
    private var pointY = 0f
    private fun drawDirection(canvas: Canvas, s: String, north: Int) {
        when (north) {
            0 -> {
                northData?.let {
                    pointX = it.arrow_project_x[7].toFloat() * (mWidth - offsetX * 2) + offsetX
                    pointY = it.arrow_project_y[7].toFloat() * mHeight
                }
            }

            1 -> {
                eastData?.let {
                    pointX = it.arrow_project_x[7].toFloat() * (mWidth - offsetX * 2) + offsetX
                    pointY = it.arrow_project_y[7].toFloat() * mHeight

                }
            }

            2 -> {
                northData?.let {
                    pointX = it.arrow_project_x[8].toFloat() * (mWidth - offsetX * 2) + offsetX
                    pointY = it.arrow_project_y[8].toFloat() * mHeight
                }
            }

            3 -> {
                eastData?.let {
                    pointX = it.arrow_project_x[8].toFloat() * (mWidth - offsetX * 2) + offsetX
                    pointY = it.arrow_project_y[8].toFloat() * mHeight
                }
            }
        }
        canvas.drawText(s, pointX, pointY, blackTextPaint)
        canvas.drawText(s, pointX, pointY, greenTextPaint)
    }


    private fun drawNorth(canvas: Canvas) {
        northPath.reset()
        northData?.let {
            northPath.moveTo(
                it.arrow_project_x[0].toFloat() * (mWidth - offsetX * 2) + offsetX,
                it.arrow_project_y[0].toFloat() * mHeight
            )
            northPath.lineTo(
                it.arrow_project_x[1].toFloat() * (mWidth - offsetX * 2) + offsetX,
                it.arrow_project_y[1].toFloat() * mHeight
            )
            canvas.drawPath(northPath, blackPaint)
            canvas.drawPath(northPath, wihtePaint)
            northPath.reset()
            northPath.moveTo(
                it.arrow_project_x[3].toFloat() * (mWidth - offsetX * 2) + offsetX,
                it.arrow_project_y[3].toFloat() * mHeight
            )
            northPath.lineTo(
                it.arrow_project_x[4].toFloat() * (mWidth - offsetX * 2) + offsetX,
                it.arrow_project_y[4].toFloat() * mHeight
            )
            northPath.lineTo(
                it.arrow_project_x[5].toFloat() * (mWidth - offsetX * 2) + offsetX,
                it.arrow_project_y[5].toFloat() * mHeight
            )
            northPath.moveTo(
                it.arrow_project_x[4].toFloat() * (mWidth - offsetX * 2) + offsetX,
                it.arrow_project_y[4].toFloat() * mHeight
            )
            northPath.lineTo(
                it.arrow_project_x[6].toFloat() * (mWidth - offsetX * 2) + offsetX,
                it.arrow_project_y[6].toFloat() * mHeight
            )
            canvas.drawPath(northPath, blackPaint)
            canvas.drawPath(northPath, redPaint)
            northPath.reset()
        }
    }

    private fun drawNorthSegment(canvas: Canvas) {
        northSegmentPath.reset()
        northData?.let {
            for (i in 1..it.count) {
                northSegmentPath.moveTo(
                    (it.scale_project_x[middleIndex - i] * (mWidth - offsetX * 2) + offsetX).toFloat(),
                    (it.scale_project_y[middleIndex - i] * mHeight).toFloat()
                )
                northSegmentPath.lineTo(
                    (it.scale_project_end_x[middleIndex - i] * (mWidth - offsetX * 2) + offsetX).toFloat(),
                    (it.scale_project_end_y[middleIndex - i] * mHeight).toFloat()
                )

                canvas.drawPath(northSegmentPath, blackPaint)
                canvas.drawPath(northSegmentPath, wihtePaint)
                northSegmentPath.reset()
                if (i != it.count || it.count == 1) {
                    northSegmentPath.moveTo(
                        (it.scale_project_x[middleIndex + i] * (mWidth - offsetX * 2) + offsetX).toFloat(),
                        (it.scale_project_y[middleIndex + i] * mHeight).toFloat()
                    )
                    northSegmentPath.lineTo(
                        (it.scale_project_end_x[middleIndex + i] * (mWidth - offsetX * 2) + offsetX).toFloat(),
                        (it.scale_project_end_y[middleIndex + i] * mHeight).toFloat()
                    )
                    canvas.drawPath(northSegmentPath, blackPaint)
                    canvas.drawPath(northSegmentPath, redPaint)
                }
                northSegmentPath.reset()
            }
        }

        northSegmentPath.reset()
    }

    private fun drawEast(canvas: Canvas) {
        eastData?.let {
            eastPath.moveTo(
                it.arrow_project_x[0].toFloat() * (mWidth - offsetX * 2) + offsetX,
                it.arrow_project_y[0].toFloat() * mHeight
            )
            eastPath.lineTo(
                it.arrow_project_x[1].toFloat() * (mWidth - offsetX * 2) + offsetX,
                it.arrow_project_y[1].toFloat() * mHeight
            )
            eastPath.moveTo(
                it.arrow_project_x[3].toFloat() * (mWidth - offsetX * 2) + offsetX,
                it.arrow_project_y[3].toFloat() * mHeight
            )
            eastPath.lineTo(
                it.arrow_project_x[4].toFloat() * (mWidth - offsetX * 2) + offsetX,
                it.arrow_project_y[4].toFloat() * mHeight
            )
            canvas.drawPath(eastPath, blackPaint)
            canvas.drawPath(eastPath, wihtePaint)
            eastPath.reset()
        }

    }

    private fun drawEastSegment(canvas: Canvas) {
        eastSegmentPath.reset()
        eastData?.let {
            for (i in 1..it.count) {
                eastSegmentPath.moveTo(
                    (it.scale_project_x[middleIndex - i] * (mWidth - offsetX * 2) + offsetX).toFloat(),
                    (it.scale_project_y[middleIndex - i] * mHeight).toFloat()
                )
                eastSegmentPath.lineTo(
                    (it.scale_project_end_x[middleIndex - i] * (mWidth - offsetX * 2) + offsetX).toFloat(),
                    (it.scale_project_end_y[middleIndex - i] * mHeight).toFloat()
                )
                eastSegmentPath.moveTo(
                    (it.scale_project_x[middleIndex + i] * (mWidth - offsetX * 2) + offsetX).toFloat(),
                    (it.scale_project_y[middleIndex + i] * mHeight).toFloat()
                )
                eastSegmentPath.lineTo(
                    (it.scale_project_end_x[middleIndex + i] * (mWidth - offsetX * 2) + offsetX).toFloat(),
                    (it.scale_project_end_y[middleIndex + i] * mHeight).toFloat()
                )

            }
            canvas.drawPath(eastSegmentPath, blackPaint)
            canvas.drawPath(eastSegmentPath, wihtePaint)
            eastSegmentPath.reset()

        }
    }

    private fun onCalculateDistance(x: Float, y: Float) {
        val value = widgetModel.compass3dModelFlow.replayCache.firstOrNull() ?: return
        val eul = getEul(value)
        val camera = getCamera(value)
        val robotGeo = getRobotGeo(value)
        val homeLatLng = getHomeLatLng(value)
        val laser = value.laserDistanceM

        val distanceBean = AIJni.calcTwoPointDistance(
            x.toDouble() / ScreenUtils.getScreenWidth(context),
            y.toDouble() / ScreenUtils.getScreenHeight(context),
            eul,
            camera,
            robotGeo,
            homeLatLng,
            laser
        )
        var distanceX = "0m"
        var distanceY = "0m"


        if (distanceBean.x > 0 && distanceBean.y > 0) {
            distanceX =
                "E ${TransformUtils.getDistanceValueWithm(abs(distanceBean.x), 1)}"
            distanceY =
                "N ${TransformUtils.getDistanceValueWithm(abs(distanceBean.y), 1)}"
        } else if (distanceBean.x < 0 && distanceBean.y > 0) {
            distanceX =
                "W ${TransformUtils.getDistanceValueWithm(abs(distanceBean.x), 1)}"
            distanceY =
                "N ${TransformUtils.getDistanceValueWithm(abs(distanceBean.y), 1)}"
        } else if (distanceBean.x < 0 && distanceBean.y < 0) {
            distanceX =
                "W ${TransformUtils.getDistanceValueWithm(abs(distanceBean.x), 1)}"
            distanceY =
                "S ${TransformUtils.getDistanceValueWithm(abs(distanceBean.y), 1)}"
        } else if (distanceBean.x > 0 && distanceBean.y < 0) {
            distanceX =
                "E ${TransformUtils.getDistanceValueWithm(abs(distanceBean.x), 1)}"
            distanceY =
                "S ${TransformUtils.getDistanceValueWithm(abs(distanceBean.y), 1)}"
        } else if (distanceBean.x == 0.0 && distanceBean.y > 0) {
            distanceX =
                "N ${TransformUtils.getDistanceValueWithm(abs(distanceBean.y), 1)}"
        } else if (distanceBean.x == 0.0 && distanceBean.y < 0) {
            distanceX =
                "S ${TransformUtils.getDistanceValueWithm(abs(distanceBean.y), 1)}"
        } else if (distanceBean.x > 0 && distanceBean.y == 0.0) {
            distanceX =
                "E ${TransformUtils.getDistanceValueWithm(abs(distanceBean.x), 1)}"
        } else if (distanceBean.x < 0 && distanceBean.y == 0.0) {
            distanceX =
                "W ${TransformUtils.getDistanceValueWithm(abs(distanceBean.x), 1)}"
        }
        updateDistance(distanceX, distanceY)
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


    override fun reactToModelChanges() {
        super.reactToModelChanges()
        widgetModel.compass3dModelFlow.subscribe {
            val result = AIJni.calcCompass3DData(
                getEul(it),
                getCamera(it),
                getRobotGeo(it),
                getHomeLatLng(it),
                it.laserDistanceM,
                showFovScale
            )
            update(result[0], result[1])

        }
        widgetModel.resetPreciseCalibrationFlow.subscribe {
            reset()
        }
    }

    private fun getEul(it: Compass3DModel): DoubleArray {
        val eul = DoubleArray(3)
        eul[0] = Math.toRadians(it.gimbalAttitudeBean.getYawDegree().toDouble())
        eul[1] = Math.toRadians(it.gimbalAttitudeBean.getPitchDegree().toDouble())
        eul[2] = Math.toRadians(it.gimbalAttitudeBean.getRollDegree().toDouble())
        return eul
    }

    private fun getRobotGeo(it: Compass3DModel): DoubleArray {
        val robotGeo = DoubleArray(3)
        robotGeo[0] = it.droneLatitude
        robotGeo[1] = it.droneLongitude
        robotGeo[2] = it.droneAltitude.toDouble()
        return robotGeo
    }

    private fun getCamera(it: Compass3DModel): DoubleArray {
        val cameras = DoubleArray(4)
        cameras[0] = it.fovH
        cameras[1] = it.fovV
        cameras[2] = ScreenUtils.getScreenWidth(context).toDouble()
        cameras[3] = ScreenUtils.getScreenHeight(context).toDouble()
        return cameras
    }

    private fun getHomeLatLng(it: Compass3DModel): DoubleArray {
        val homeLatLng = DoubleArray(3)
        homeLatLng[0] = it.homeLatitude
        homeLatLng[1] = it.homeLongitude
        homeLatLng[2] = 0.0
        return homeLatLng
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