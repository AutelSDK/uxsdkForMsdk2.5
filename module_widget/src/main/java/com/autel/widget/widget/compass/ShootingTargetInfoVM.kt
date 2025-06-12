package com.autel.widget.widget.compass

import com.autel.common.base.widget.BaseWidgetModel
import com.autel.common.utils.DeviceUtils
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.bean.DroneAttitudeBean
import com.autel.drone.sdk.vmodelx.module.camera.bean.GimbalTypeEnum
import com.autel.drone.sdk.vmodelx.module.camera.bean.LensTypeEnum
import com.autel.common.model.lens.ILens
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Created by  2024/7/10
 */
class ShootingTargetInfoVM : BaseWidgetModel(), ILens {

    private var drone: IAutelDroneDevice? = null
    private var gimbalTypeEnum: GimbalTypeEnum? = null
    private var lensTypeEnum: LensTypeEnum? = null

    val shootingTargetFlow =
        MutableSharedFlow<ShootingTargetModel>(replay = 1, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override fun fixedFrequencyRefresh() {
        val device = drone
        if (device != null) {
            val flightControlData = device.getDeviceStateData().flightControlData
            val fov = getFov(device)

            val target = ShootingTargetModel(
                gimbalAttitudeBean = DroneAttitudeBean(
                    flightControlData.gimbalAttitudePitch,
                    flightControlData.gimbalAttitudeRoll,
                    flightControlData.gimbalAttitudeYaw
                ),
                flightControlData.homeLatitude,
                flightControlData.homeLongitude,
                fov[0],
                fov[1],
                flightControlData.droneLatitude,
                flightControlData.droneLongitude,
                flightControlData.altitude,
                getLaser(device)
            )
            if (target != shootingTargetFlow.replayCache.firstOrNull()) {
                shootingTargetFlow.tryEmit(target)
            }
        }
    }

    private fun getLaser(device: IAutelDroneDevice): Double {
        return if (device.getDeviceStateData().flightControlData.laserDistanceIsValid) {
            device.getDeviceStateData().flightControlData.laserDistance.toDouble() / 100
        } else {
            0.0
        }
    }

    private fun getFov(device: IAutelDroneDevice): DoubleArray {
        val fov = DoubleArray(2)
        DeviceUtils.getDefaultVisibleLensCameraData(device)?.let {
            fov[0] = it.fovH.toDouble()
            fov[1] = it.fovV.toDouble()
        }
        val cameraData = device?.getDeviceStateData()?.gimbalDataMap?.get(device.getGimbalDeviceType())?.cameraData?.cameraInfoList
        if (lensTypeEnum == null) {
            return fov
        }
        val lensId = DeviceUtils.getCameraLensId(device, lensTypeEnum!!)
        val lensData = cameraData?.find { it.cameraId == lensId }
        lensData?.let {
            fov[0] = it.fovH.toDouble()
            fov[1] = it.fovV.toDouble()
        }
        return fov

    }

    override fun getDrone(): IAutelDroneDevice? {
        return this.drone
    }

    override fun getGimbal(): GimbalTypeEnum? {
        return this.gimbalTypeEnum
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