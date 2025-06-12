package com.autel.widget.widget.fullScreen

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.widget.FrameLayout
import com.autel.common.model.lens.ILens
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.module.camera.bean.GimbalTypeEnum
import com.autel.drone.sdk.vmodelx.module.camera.bean.LensTypeEnum
import com.autel.widget.R

/**
 * Created by  2023/11/16
 * 手势全屏组件
 */
class FullScreenWidget(context: Context) : FrameLayout(context), ILens {

    private var hasDispatchFull = false //单次手势中已经分发了全屏手势

    private var dispatchFullResult = false //分发全屏手势的结果

    private var fullScreenListener: FullScreenListener? = null

    private val mDistanceMinLength = context.resources.getDimensionPixelSize(R.dimen.common_72dp)
    var startY = 0.0f

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        isClickable = true
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startY = event.y
                hasDispatchFull = false
                dispatchFullResult = false
            }

            MotionEvent.ACTION_MOVE -> {
                processDistance(event.y - startY)
            }
        }
        return super.onTouchEvent(event)
    }

    fun setFullScreenListener(fullScreenListener: FullScreenListener) {
        this.fullScreenListener = fullScreenListener
    }

    private fun processDistance(detalY: Float) {
        if (fullScreenListener != null && Math.abs(detalY) > mDistanceMinLength && !hasDispatchFull) {
            hasDispatchFull = true
            dispatchFullResult = fullScreenListener?.fullScreenSwitch(detalY < 0) ?: false
        }
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

    interface FullScreenListener {
        /**
         * 全屏手势
         */
        fun fullScreenSwitch(full: Boolean): Boolean
    }

}