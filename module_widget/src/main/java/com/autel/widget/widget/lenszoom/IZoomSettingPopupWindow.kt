package com.autel.widget.widget.lenszoom

import com.autel.drone.sdk.vmodelx.module.camera.bean.RangeStepValue

/**
 * Created by  2023/6/7
 * 变焦设置window
 */
interface IZoomSettingPopupWindow {
    /** 设置zoom变化监听*/
    fun setZoomListener(listener: IZoomChangeListener)

    /** 更新zoomScale*/
    fun updateZoomScale(zoomMulti100: Int, range: RangeStepValue?)
}