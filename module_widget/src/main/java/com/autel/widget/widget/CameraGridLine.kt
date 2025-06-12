package com.autel.widget.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.autel.common.model.lens.ILens
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.module.camera.bean.GimbalTypeEnum
import com.autel.drone.sdk.vmodelx.module.camera.bean.LensTypeEnum
import com.autel.widget.R

/**
 * Created by  2023/6/6
 * 镜头网格线
 */
class CameraGridLine(context: Context, attrs: AttributeSet?) : View(context, attrs), ILens {
    var width: Float = 0f
    var height: Float = 0f
    var icon: Bitmap? = null
    var nine_grid: Boolean = false
    var diagonal_line: Boolean = false
    var iconLine: Boolean = false
    private val mPaint by lazy {
        Paint().also {
            it.isAntiAlias = true
            it.color = getContext().getColor(R.color.common_color_99FFFFFF)
            it.style = Paint.Style.FILL
            it.strokeWidth = 2f
        }
    }

    init {
        icon = BitmapFactory.decodeResource(context.resources, R.drawable.common_grid_icon)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        var widthMode = MeasureSpec.getMode(widthMeasureSpec)
        var widthSize = MeasureSpec.getSize(widthMeasureSpec)
        var heightMode = MeasureSpec.getMode(heightMeasureSpec)
        var heightSize = MeasureSpec.getSize(heightMeasureSpec)
        width = widthSize.toFloat()
        height = heightSize.toFloat()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas?.let {
            //绘画井字
            if (nine_grid) {
                canvas.drawLine(0f, height / 3.0f, width, height / 3.0f, mPaint)
                canvas.drawLine(0f, height * 2 / 3.0f, width, height * 2 / 3.0f, mPaint)
                canvas.drawLine(width / 3.0f, 0f, width / 3.0f, height, mPaint)
                canvas.drawLine(width * 2 / 3.0f, 0f, width * 2 / 3.0f, height, mPaint)
            }
            //绘画对角线
            if (diagonal_line) {
                canvas.drawLine(0f, 0f, width, height, mPaint)
                canvas.drawLine(0f, height, width, 0f, mPaint)
            }
            //绘画聚焦
            if (iconLine && icon != null) {
                canvas.drawBitmap(
                    icon!!,
                    width / 2.0f - icon!!.width / 2.0f + 0.7f,
                    height / 2.0f - icon!!.height / 2.0f + 0.5f,
                    mPaint
                )
            }

        }
    }


    fun setLineType(nine_grid_pattern: Boolean, diagonal: Boolean, icon: Boolean) {
        this.nine_grid = nine_grid_pattern
        this.diagonal_line = diagonal
        iconLine = icon
        invalidate()
    }


    override fun getDrone(): IAutelDroneDevice? {
        return null
    }

    override fun getGimbal(): GimbalTypeEnum? {
        return null
    }

    override fun getLensTypeEnum(): LensTypeEnum? {
        return null
    }

    override fun updateLensInfo(drone: IAutelDroneDevice?, gimbal: GimbalTypeEnum?, lensType: LensTypeEnum?) {

    }
}