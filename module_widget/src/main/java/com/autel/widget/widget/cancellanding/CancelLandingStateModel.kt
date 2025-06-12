package com.autel.widget.widget.cancellanding

import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice

/**
 *  左侧状态模型
 */
data class CancelLandingStateModel(
    val cancelReturn: Boolean,
    val cancelDecline: Boolean,
    val returnDrones: List<IAutelDroneDevice>,
    val landDrones: List<IAutelDroneDevice>
)