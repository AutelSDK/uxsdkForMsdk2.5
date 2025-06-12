package com.autel.widget.widget.compass

import com.autel.common.base.widget.BaseWidgetModel
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.bean.DroneAttitudeBean
import com.autel.drone.sdk.vmodelx.module.camera.bean.GimbalTypeEnum
import com.autel.drone.sdk.vmodelx.module.camera.bean.LensTypeEnum
import com.autel.common.model.lens.ILens
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Created by  2024/7/9
 */
class Compass3DVM : BaseWidgetModel(), ILens {

    private var drone: IAutelDroneDevice? = null
    private var gimbalTypeEnum: GimbalTypeEnum? = null
    private var lensTypeEnum: LensTypeEnum? = null

    val compass3dModelFlow = MutableSharedFlow<Compass3DModel>(replay = 1, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val resetPreciseCalibrationFlow = MutableSharedFlow<Boolean>(replay = 1, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override fun fixedFrequencyRefresh() {
        val device = drone
        if (device != null) {
            val flightControlData = device.getDeviceStateData().flightControlData
            val fov = getFov(device)
            val compass3dModel = Compass3DModel(
                gimbalAttitudeBean = DroneAttitudeBean(
                    flightControlData.gimbalAttitudePitch,
                    flightControlData.gimbalAttitudeRoll,
                    flightControlData.gimbalAttitudeYaw
                ),
                droneAttitudeBean = DroneAttitudeBean(
                    flightControlData.droneAttitudePitch,
                    flightControlData.droneAttitudeRoll,
                    flightControlData.droneAttitudeYaw
                ),
                flightControlData.homeLatitude,
                flightControlData.homeLongitude,
                fov[0],
                fov[1],
                flightControlData.droneLatitude,
                flightControlData.droneLongitude,
                flightControlData.altitude,
                flightControlData.distance,
                getLaser(device)
            )

            val data = compass3dModelFlow.replayCache.firstOrNull()
            if (compass3dModel.needResetPreciseCalibration(data)) {
                resetPreciseCalibrationFlow.tryEmit(true)
            }
            if (compass3dModel.needRefreshCompass(data)) {
                compass3dModelFlow.tryEmit(compass3dModel)
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

    private fun getFov(droneDevice: IAutelDroneDevice): DoubleArray {
        val fov = DoubleArray(2)
        val cameraData = droneDevice.getDeviceStateData().gimbalDataMap[droneDevice.getGimbalDeviceType()]?.cameraData
        val cameraBaseData = if (lensTypeEnum == LensTypeEnum.WideAngle) {
            cameraData?.wideAngleCameraData
        } else if (lensTypeEnum == LensTypeEnum.Zoom) {
            cameraData?.zoomCameraData
        } else if (lensTypeEnum == LensTypeEnum.TeleZoom) {
            cameraData?.teleZoomCameraData
        } else if (lensTypeEnum == LensTypeEnum.NightVision) {
            cameraData?.nightCameraData
        } else if (lensTypeEnum == LensTypeEnum.Thermal) {
            cameraData?.thermalCameraData
        } else {
            null
        }
        cameraBaseData?.let {
            fov[0] = it.fovH.toDouble()
            fov[1] = it.fovV.toDouble()
        }
        return fov

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