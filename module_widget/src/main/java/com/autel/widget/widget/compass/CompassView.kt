package com.autel.widget.widget.compass

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import androidx.core.graphics.toColorInt
import com.autel.widget.R
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

/**
 * @Author create by LJ
 * @Date 2022/09/14 17:52
 */
class CompassView(context: Context) : View(context) {

    private val mLinePaint = Paint()
    private val mLocationPaint = Paint()

    private val mWidth = context.resources.getDimension(R.dimen.common_60dp)
    private val startCircle = context.resources.getDimension(R.dimen.common_25dp)
    private var altitude = "0m"
    private var positionstr = "C"
    private var angle = "0°"

    private var northDegrees = 0f
    private var TN = ""
    private var TE = ""
    private var TD = ""
    private var theight = 0.0
    private var TAM = ""


    private val N = context.getString(R.string.common_north_N) ?: "N"
    private val E = context.getString(R.string.common_north_E) ?: "E"
    private val S = context.getString(R.string.common_north_S) ?: "S"
    private val W = context.getString(R.string.common_north_W) ?: "W"
    private var IsOpenNorth = true

    /**
     * 是否显示经纬度信息
     */
    private var isShowEN = false

    private var yaw: Float = 0f
    private val northN = BitmapFactory.decodeResource(resources, R.drawable.icon_compass_n)


    init {
        mLinePaint.isAntiAlias = true
        mLinePaint.color = "#0059FF".toColorInt()
        mLinePaint.strokeWidth = context.resources.getDimension(R.dimen.common_3dp)
        mLocationPaint.textSize = context.resources.getDimension(R.dimen.common_15dp)
        mLocationPaint.color = context.resources.getColor(R.color.common_color_33CC33)
        mLocationPaint.style = Paint.Style.FILL_AND_STROKE
        mLinePaint.isAntiAlias = true
        mLocationPaint.isAntiAlias = true
    }


    /**
     * 打开指北针
     */
    fun openNorth() {
        IsOpenNorth = true
    }

    /**
     * 关闭指北针
     */
    fun closeOpenNorth() {
        IsOpenNorth = false
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.translate(width / 2f, height / 2f)
        canvas.save()
        canvas.rotate(-northDegrees)

        canvas.drawBitmap(
            northN,
            -northN.width / 2f,
            -northN.height.toFloat() - startCircle,
            mLinePaint
        )
        mLinePaint.color = "#0091FF".toColorInt()
        canvas.drawLine(0f, startCircle, 0f, mWidth, mLinePaint)
        mLinePaint.color = "#DDDDDD".toColorInt()
        canvas.drawLine(startCircle, 0f, mWidth, 0f, mLinePaint)
//        canvas.drawLine(0f, -startCircle, 0f, -mWidth, mLinePaint)
        canvas.drawLine(-startCircle, 0f, -mWidth, 0f, mLinePaint)
        mLocationPaint.color = "#03FEF4".toColorInt()
        canvas.drawText(
            N,
            0f - northN.width / 2f + 10,
            -northN.height.toFloat() - startCircle - 20,
            mLocationPaint
        )
        canvas.drawText(E, mWidth + 50f - 10, 7f, mLocationPaint)
        canvas.drawText(S, -10f, mWidth + 50, mLocationPaint)
        canvas.drawText(W, -mWidth - 50, 10f, mLocationPaint)

        canvas.restore()

        if (isShowEN) {
            canvas.save()
            mLocationPaint.color = "#03FEF4".toColorInt()
            canvas.drawText(TN, -mWidth / 2f - 50, 240f, mLocationPaint)
            canvas.drawText(TE, -mWidth / 2f - 50, 280f, mLocationPaint)
            canvas.restore()
        }

    }


    /**
     * speed:飞行速度
     * northAngle:旋转角
     */
    fun update(
        northAngle: Float, tn: Double, te: Double,
        td: String, altitude: Double, isRefresh: Boolean, isShowEN: Boolean,
    ) {
        if (tn <= 0.0 || te <= 0.0) {
            TN = "N ${context.getString(R.string.common_text_no_value)}"
            TE = "E ${context.getString(R.string.common_text_no_value)}"
        } else {
            TN = "N ${getFloatNoMoreThanTwoDigits(tn)}"
            TE = "E ${getFloatNoMoreThanTwoDigits(te)}"
        }
        this.isShowEN = isShowEN
        TD = td
        yaw = if (northAngle == 0f) 0f else (northAngle * 180 / Math.PI).toFloat()
        var degree = yaw
        if (abs(degree) > 360) {
            degree %= 360
        }
        this.altitude = "H: $altitude m"
        angle = "$degree°"
        TAM = "AM:$angle"
        this.northDegrees = degree
        if ((degree in 0.0..22.5) || (degree in 337.5..360.0)) {

            positionstr = N;

        } else if (22.5 < degree && degree <= 67.5) {

            positionstr = "$E$N";

        } else if (67.5 < degree && degree <= 112.5) {

            positionstr = "$E";

        } else if (112.5 < degree && degree <= 157.5) {

            positionstr = "$E$S";

        } else if (157.5 < degree && degree <= 202.5) {

            positionstr = "$S";

        } else if (202.5 < degree && degree <= 247.5) {

            positionstr = "$W$S";

        } else if (247.5 < degree && degree <= 292.5) {

            positionstr = "$W";

        } else if (degree > 292.5)
            positionstr = "$W$N";

        if (!isRefresh) {
            mLocationPaint.color = Color.TRANSPARENT
            mLinePaint.color = Color.TRANSPARENT
        } else {
            mLocationPaint.color = Color.GREEN

        }
        invalidate()
    }

    private fun getFloatNoMoreThanTwoDigits(number: Double): String {
        val format = DecimalFormat("#.#########", DecimalFormatSymbols(Locale.ENGLISH))
        //舍弃规则，RoundingMode.FLOOR表示直接舍弃。
        format.roundingMode = RoundingMode.FLOOR
        return format.format(number)
    }

}