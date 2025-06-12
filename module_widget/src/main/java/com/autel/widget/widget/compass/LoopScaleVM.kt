package com.autel.widget.widget.compass

import com.autel.common.base.widget.BaseWidgetModel
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.module.camera.bean.GimbalTypeEnum
import com.autel.drone.sdk.vmodelx.module.camera.bean.LensTypeEnum
import com.autel.common.model.lens.ILens
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Created by  2024/7/10
 *循环刻度尺 VM
 */
class LoopScaleVM : BaseWidgetModel(), ILens {

    private var drone: IAutelDroneDevice? = null
    private var gimbalTypeEnum: GimbalTypeEnum? = null
    private var lensTypeEnum: LensTypeEnum? = null

    val loopScaleFlow = MutableSharedFlow<LoopScaleModel>(replay = 1, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override fun fixedFrequencyRefresh() {
        val device = drone ?: return
        val loopScaleModel = LoopScaleModel(device.getDeviceStateData().flightControlData.gimbalAttitudeYaw.toDouble().toFloat())
        if (loopScaleModel != loopScaleFlow.replayCache.firstOrNull()) {
            loopScaleFlow.tryEmit(loopScaleModel)
        }

    }

    override fun getDrone(): IAutelDroneDevice? {
        return drone
    }

    override fun getGimbal(): GimbalTypeEnum? {
        return gimbalTypeEnum
    }

    override fun getLensTypeEnum(): LensTypeEnum? {
        return lensTypeEnum
    }

    override fun updateLensInfo(drone: IAutelDroneDevice?, gimbal: GimbalTypeEnum?, lensType: LensTypeEnum?) {
        this.drone = drone
        this.gimbalTypeEnum = gimbal
        this.lensTypeEnum = lensType
    }
}