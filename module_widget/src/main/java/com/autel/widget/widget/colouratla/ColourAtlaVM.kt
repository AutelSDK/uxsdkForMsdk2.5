package com.autel.widget.widget.colouratla

import com.autel.common.base.widget.BaseWidgetModel
import com.autel.common.manager.AutelStorageManager
import com.autel.common.manager.StorageKey
import com.autel.common.sdk.KeyManagerCoroutineWrapper
import com.autel.common.sdk.service.cameraSetting.CameraSettingService
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.CameraKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.base.KeyTools
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.camera.enums.ThermalColorEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.camera.enums.ThermalGainEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.enums.DroneWorkModeEnum
import com.autel.drone.sdk.vmodelx.module.camera.bean.GimbalTypeEnum
import com.autel.drone.sdk.vmodelx.module.camera.bean.LensTypeEnum
import com.autel.common.model.lens.ILens
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Created by  2023/9/15
 * 伪彩VM
 */
class ColourAtlaVM : BaseWidgetModel(), ILens {

    private var drone: IAutelDroneDevice? = null
    private var gimbalTypeEnum: GimbalTypeEnum? = null
    private var lensTypeEnum: LensTypeEnum? = null

    val thermalColorFlow = MutableSharedFlow<ThermalColorEnum>(replay = 1, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val thermalGainFlow = MutableSharedFlow<ThermalGainEnum>(replay = 1, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val canTempMeasureFlow = MutableSharedFlow<Boolean>(replay = 1, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private fun getLensId(): Int {
        return drone?.getCameraAbilitySetManger()?.getLenId(lensTypeEnum ?: LensTypeEnum.Thermal, gimbalTypeEnum) ?: 0
    }

    /**
     * 设置红外的伪彩模式
     */
    suspend fun setCameraThermalColor(mode: ThermalColorEnum) {
        drone?.getKeyManager()?.let {
            val key = KeyTools.createKey(CameraKey.KeyThermalColor, getLensId())
            KeyManagerCoroutineWrapper.setValue(it, key, mode)
        }
    }

    /**
     * 校准红外的FFC
     */
    suspend fun actionCameraFfc() {
        drone?.getKeyManager()?.let {
            val key = KeyTools.createKey(CameraKey.KeyCameraFfc, getLensId())
            KeyManagerCoroutineWrapper.performAction(it, key)
        }
    }

    /**
     * 设置热成像图像增益模式
     */
    fun setCameraThermalGain(enum: ThermalGainEnum) {
        drone?.let { droneDevice ->
            lensTypeEnum?.let { typeEnum -> CameraSettingService.getInstance().lensSetting.setInfraredThermalGain(droneDevice, typeEnum, enum) }
        }
    }

    fun getCameraThermalColor() {
        drone?.let { droneDevice ->
            lensTypeEnum?.let { typeEnum -> CameraSettingService.getInstance().lensSetting.getInfraredThermalColor(droneDevice, typeEnum) }
        }
    }

    fun getCameraThermalGain() {
        drone?.let { droneDevice ->
            lensTypeEnum?.let { typeEnum -> CameraSettingService.getInstance().lensSetting.getInfraredThermalGain(droneDevice, typeEnum) }
        }
    }

    fun clear() {
        thermalColorFlow.resetReplayCache()
        thermalGainFlow.resetReplayCache()
        canTempMeasureFlow.resetReplayCache()
    }

    override fun fixedFrequencyRefresh() {
        if (drone != null) {
            val thermalColorEnum = drone?.getDeviceStateData()?.gimbalDataMap?.get(gimbalTypeEnum)?.cameraOperateData?.iThermalColorEnum
            if (thermalColorEnum != null) {
                if (thermalColorEnum != thermalColorFlow.replayCache.firstOrNull()) {
                    thermalColorFlow.tryEmit(thermalColorEnum)
                }
            }
            val thermalGainEnum = drone?.getDeviceStateData()?.gimbalDataMap?.get(gimbalTypeEnum)?.cameraOperateData?.iThermalGainEnum
            if (thermalGainEnum != null && thermalGainEnum != thermalGainFlow.replayCache.firstOrNull()) {
                thermalGainFlow.tryEmit(thermalGainEnum)
            }
            val workModel = drone?.getDeviceStateData()?.flightControlData?.droneWorkMode
            val canTempMeasure = drone?.isConnected() == true &&
                    AutelStorageManager.getPlainStorage().getBooleanValue(StorageKey.PlainKey.KEY_TEMPERATURE_MEASUREMENT_SWITCH)
                    && workModel != DroneWorkModeEnum.INFRARED_TRACK
                    && workModel != DroneWorkModeEnum.TRACK
            if (canTempMeasure != canTempMeasureFlow.replayCache.firstOrNull()) {
                canTempMeasureFlow.tryEmit(canTempMeasure)
            }
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