package com.autel.widget.widget.lenszoom

import com.autel.drone.sdk.vmodelx.module.camera.bean.RangeStepValue

/**
 * Created by  2023/6/6
 * 镜头状态模型
 */
data class LensStateModel(
    val isConnected: Boolean,
    val zoomValue: Int,
    val range: RangeStepValue?
)