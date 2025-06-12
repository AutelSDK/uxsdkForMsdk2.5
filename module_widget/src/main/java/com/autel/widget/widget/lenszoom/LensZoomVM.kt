package com.autel.widget.widget.lenszoom

import com.autel.common.base.widget.BaseWidgetModel
import com.autel.common.bean.CustomRemoteKeyEnum
import com.autel.common.sdk.service.cameraSetting.CameraSettingService
import com.autel.common.utils.CustomKeyUtils
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.drone.sdk.vmodelx.manager.keyvalue.callback.CommonCallbacks
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.RemoteControllerKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.base.KeyTools
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.remotecontrol.bean.HardwareButtonInfoBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.remotecontrol.enums.RCButtonTypeEnum
import com.autel.drone.sdk.vmodelx.module.camera.bean.GimbalTypeEnum
import com.autel.drone.sdk.vmodelx.module.camera.bean.LensTypeEnum
import com.autel.common.model.lens.ILens
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Created by  2023/6/6
 * 镜头切换VM
 */
class LensZoomVM : BaseWidgetModel(), ILens {
    private var drone: IAutelDroneDevice? = null
    private var gimbalTypeEnum: GimbalTypeEnum? = null
    private var lensTypeEnum: LensTypeEnum? = null

    val lensState = MutableSharedFlow<LensStateModel>(replay = 1, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val pbDismissState = MutableSharedFlow<Boolean>(replay = 1, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private var rcHardWareInfoKey = KeyTools.createKey(RemoteControllerKey.KeyRCHardwareInfo)
    private var rcHardWareCallback = object : CommonCallbacks.KeyListener<HardwareButtonInfoBean> {
        override fun onValueChange(oldValue: HardwareButtonInfoBean?, newValue: HardwareButtonInfoBean) {
            if (newValue.buttonType == RCButtonTypeEnum.ZOOM_IN || newValue.buttonType == RCButtonTypeEnum.ZOOM_OUT) {
                pbDismissState.tryEmit(true)
            } else {
                if ((newValue.buttonType == RCButtonTypeEnum.LEFT_CUSTOM && CustomKeyUtils.getDefineCustomC1() == CustomRemoteKeyEnum.MAP_FPV_SWITCH) ||
                    (newValue.buttonType == RCButtonTypeEnum.RIGHT_CUSTOM && CustomKeyUtils.getDefineCustomC2() == CustomRemoteKeyEnum.MAP_FPV_SWITCH)
                ) {
                    pbDismissState.tryEmit(true)
                }
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

    suspend fun setQuickZoom(zoomValueMulti100: Int) {
        val device = drone
        val localLensType = lensTypeEnum
        if (device == null || localLensType == null) {
            return
        }
        CameraSettingService.getInstance().lensSetting.setTypeXoomFixedFactor(device, localLensType, zoomValueMulti100)
    }

    suspend fun setZoomFactor(zoomValue: Int) {
        val device = drone
        val localLensType = lensTypeEnum
        if (device == null || localLensType == null) {
            return
        }
        CameraSettingService.getInstance().lensSetting.setZoomFactor(device, localLensType, zoomValue)
    }

    fun cleanLensStateCache() {
        lensState.resetReplayCache()
    }

    override fun setup() {
        super.setup()
        DeviceManager.getDeviceManager().getLocalRemoteDevice().getKeyManager().listen(rcHardWareInfoKey, callback = rcHardWareCallback)
    }

    override fun cleanup() {
        super.cleanup()
        DeviceManager.getDeviceManager().getLocalRemoteDevice().getKeyManager().cancelListen(rcHardWareInfoKey, callback = rcHardWareCallback)
    }

    override fun fixedFrequencyRefresh() {
        if (lensTypeEnum != null) {
            val gimbalData = drone?.getDeviceStateData()?.gimbalDataMap?.get(gimbalTypeEnum)
            val cameraData = gimbalData?.cameraData ?: return
            val connected = drone?.isConnected() == true
            val range = lensTypeEnum?.let {
                drone?.getCameraAbilitySetManger()?.getZoomRange(it)
            }
            val lensStateModel = when (lensTypeEnum) {
                LensTypeEnum.Zoom -> {
                    LensStateModel(connected, cameraData.zoomCameraData.professionalCameraData.zoomValue, range)

                }

                LensTypeEnum.Thermal -> {
                    LensStateModel(connected, cameraData.thermalCameraData.thermalCameraData.zoomValue, range)
                }

                LensTypeEnum.WideAngle -> {
                    LensStateModel(connected, cameraData.wideAngleCameraData.professionalCameraData.zoomValue, range)
                }

                LensTypeEnum.TeleZoom -> {
                    LensStateModel(connected, cameraData.teleZoomCameraData.professionalCameraData.zoomValue, range)
                }

                LensTypeEnum.TeleThermal -> {
                    null
                }

                LensTypeEnum.NightVision -> {
                    LensStateModel(connected, cameraData.nightCameraData.professionalCameraData.zoomValue, range)
                }

                LensTypeEnum.Visible -> {
                    null
                }

                LensTypeEnum.Telephoto -> {
                    null
                }

                null -> {
                    null
                }
            }
            if (lensStateModel != null && lensState.replayCache.firstOrNull() != lensStateModel) {
                lensState.tryEmit(lensStateModel)
            }
        }


    }
}