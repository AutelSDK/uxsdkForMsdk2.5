package com.autel.widget.widget.temp

import com.autel.common.base.widget.BaseWidgetModel
import com.autel.common.manager.AutelStorageManager
import com.autel.common.manager.StorageKey
import com.autel.common.model.lens.ITransLocation
import com.autel.common.sdk.ModelXDroneConst
import com.autel.common.sdk.service.cameraSetting.CameraSettingService
import com.autel.drone.sdk.vmodelx.dronestate.ThermalCameraData
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.camera.bean.ThermalTempAttrBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.camera.enums.TemperatureModeEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.enums.DroneWorkModeEnum
import com.autel.drone.sdk.vmodelx.module.camera.bean.GimbalTypeEnum
import com.autel.drone.sdk.vmodelx.module.camera.bean.LensTypeEnum
import com.autel.log.AutelLog
import com.autel.common.model.lens.ILens
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Created by  2023/10/1
 * 测温widgetModel
 */
class TempMeasureWidgetModel(private val iTrans: ITransLocation) : BaseWidgetModel(), ILens {
    private var drone: IAutelDroneDevice? = null
    private var gimbal: GimbalTypeEnum? = null
    private var lensType: LensTypeEnum? = null

    val thermalCameraDataFlow =
        MutableSharedFlow<ThermalCameraData>(replay = 1, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val thermalTempAttrDataFlow =
        MutableSharedFlow<ThermalTempAttrBean>(replay = 1, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    val canTempMeasureFlow = MutableSharedFlow<Boolean>(replay = 1, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override fun fixedFrequencyRefresh() {
        if (lensType != null) {
            val gimbalData = drone?.getDeviceStateData()?.gimbalDataMap?.get(gimbal)
            val cameraData = gimbalData?.cameraData ?: return
            val thermalBean = gimbalData.cameraOperateData.thermalTempAttrBean
            thermalCameraDataFlow.tryEmit(cameraData.thermalCameraData.thermalCameraData)
            if (thermalBean != thermalTempAttrDataFlow.replayCache.firstOrNull()) {
                val newThermal = thermalBean.copy()
                thermalTempAttrDataFlow.tryEmit(newThermal)
            }
            val workModel = drone?.getDeviceStateData()?.flightControlData?.droneWorkMode
            val measureSwitch = AutelStorageManager.getPlainStorage().getBooleanValue(StorageKey.PlainKey.KEY_TEMPERATURE_MEASUREMENT_SWITCH)
            val canTempMeasure = drone?.isConnected() == true && measureSwitch
                    && workModel != DroneWorkModeEnum.INFRARED_TRACK
                    && workModel != DroneWorkModeEnum.TRACK
            if (canTempMeasure != canTempMeasureFlow.replayCache.firstOrNull()) {
                AutelLog.i("TempMeasureWidget", "workModel ==$workModel, drone Connected = ${drone?.isConnected()}, measureSwitch = $measureSwitch")
                canTempMeasureFlow.tryEmit(canTempMeasure)
            }
        }
    }

    fun setRegionTemperature(tx: Float, ty: Float, w: Float, h: Float) {
        val topX = iTrans.transScreenXLocationToCamera(tx)
        val topY = iTrans.transScreenYLocationToCamera(ty)
        val width = iTrans.transScreenXSizeToCamera(w)
        val height = iTrans.transScreenYSizeToCamera(h)
        val gimbalData = drone?.getDeviceStateData()?.gimbalDataMap?.get(gimbal)
        val thermalTempAttrBean = gimbalData?.cameraOperateData?.thermalTempAttrBean
        val bean = thermalTempAttrBean?.copy(
            TemperatureModeEnum.REGION,
            regionX = (topX * ModelXDroneConst.DRONE_CAMERA_COORDINATE_MAX).toInt(),
            regionY = (topY * ModelXDroneConst.DRONE_CAMERA_COORDINATE_MAX).toInt(),
            regionW = (width * ModelXDroneConst.DRONE_CAMERA_COORDINATE_MAX).toInt(),
            regionH = (height * ModelXDroneConst.DRONE_CAMERA_COORDINATE_MAX).toInt(),
        ) ?: ThermalTempAttrBean(
            TemperatureModeEnum.REGION,
            regionX = (topX * ModelXDroneConst.DRONE_CAMERA_COORDINATE_MAX).toInt(),
            regionY = (topY * ModelXDroneConst.DRONE_CAMERA_COORDINATE_MAX).toInt(),
            regionW = (width * ModelXDroneConst.DRONE_CAMERA_COORDINATE_MAX).toInt(),
            regionH = (height * ModelXDroneConst.DRONE_CAMERA_COORDINATE_MAX).toInt()
        )
        drone?.let { droneDevice ->
            lensType?.let { typeEnum -> CameraSettingService.getInstance().lensSetting.setInfraredTemperatureAttribute(droneDevice, typeEnum, bean) }
        }
    }

    fun setPointTemperature(tx: Float, ty: Float) {
        val touchX = iTrans.transScreenXLocationToCamera(tx)
        val touchY = iTrans.transScreenYLocationToCamera(ty)

        val gimbalData = drone?.getDeviceStateData()?.gimbalDataMap?.get(gimbal)
        val thermalTempAttrBean = gimbalData?.cameraOperateData?.thermalTempAttrBean
        val bean = thermalTempAttrBean?.copy(
            TemperatureModeEnum.TOUCH,
            (touchX * ModelXDroneConst.DRONE_CAMERA_COORDINATE_MAX).toInt(),
            (touchY * ModelXDroneConst.DRONE_CAMERA_COORDINATE_MAX).toInt()
        ) ?: ThermalTempAttrBean(
            TemperatureModeEnum.TOUCH,
            (touchX * ModelXDroneConst.DRONE_CAMERA_COORDINATE_MAX).toInt(),
            (touchY * ModelXDroneConst.DRONE_CAMERA_COORDINATE_MAX).toInt()
        )
        drone?.let { droneDevice ->
            lensType?.let { typeEnum -> CameraSettingService.getInstance().lensSetting.setInfraredTemperatureAttribute(droneDevice, typeEnum, bean) }
        }
    }


    fun reset() {
        thermalCameraDataFlow.resetReplayCache()
        canTempMeasureFlow.resetReplayCache()
        thermalTempAttrDataFlow.resetReplayCache()
        fixedFrequencyRefresh()
    }

    /**
     * 设置热成像测温属性
     */
    fun forbiddenTemperature() {
        val bean = ThermalTempAttrBean(TemperatureModeEnum.NONE)
        drone?.let { droneDevice ->
            lensType?.let { typeEnum -> CameraSettingService.getInstance().lensSetting.setInfraredTemperatureAttribute(droneDevice, typeEnum, bean) }
        }
    }

    override fun getDrone(): IAutelDroneDevice? {
        return drone
    }

    override fun getGimbal(): GimbalTypeEnum? {
        return gimbal
    }

    override fun getLensTypeEnum(): LensTypeEnum? {
        return lensType
    }

    override fun updateLensInfo(drone: IAutelDroneDevice?, gimbal: GimbalTypeEnum?, lensType: LensTypeEnum?) {
        this.drone = drone
        this.gimbal = gimbal
        this.lensType = lensType
    }

}