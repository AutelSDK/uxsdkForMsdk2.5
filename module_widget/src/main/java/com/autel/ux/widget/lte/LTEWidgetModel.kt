package com.autel.ux.widget.lte

import com.autel.common.utils.DeviceUtils
import com.autel.drone.sdk.libbase.error.IAutelCode
import com.autel.drone.sdk.v2.bean.LTELinkInfo
import com.autel.drone.sdk.v2.bean.NetworkType
import com.autel.drone.sdk.v2.callback.LTELinkInfoListener
import com.autel.drone.sdk.v2.enums.LTELinkType
import com.autel.drone.sdk.v2.enums.NetworkStatus
import com.autel.drone.sdk.v2.enums.WlmLinkQualityLevel
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.interfaces.IBaseDevice
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.drone.sdk.vmodelx.manager.keyvalue.callback.CommonCallbacks
import com.autel.log.AutelLog
import com.autel.ux.core.base.AutelSDKModel
import com.autel.ux.core.base.WidgetModel
import com.autel.ux.core.utils.DataProcessor

class LTEWidgetModel @JvmOverloads constructor(autelSdkModel: AutelSDKModel, widgetDevice: IBaseDevice? = null) :
    WidgetModel(autelSdkModel, widgetDevice) {

    private val isEnableLTEMode = DataProcessor.create(false)

    private val droneLetSignal = DataProcessor.create(WlmLinkQualityLevel.NO_SIGNAL)

    private val droneLTEType = DataProcessor.create(NetworkType.NETWORK_NONE)

    private val remoteLetSignal = DataProcessor.create(WlmLinkQualityLevel.NO_SIGNAL)

    private val isSupportLTEProcessor = DataProcessor.create(false)

    private val droneNetworkStatusProcessor = DataProcessor.create(NetworkStatus.NETWORK_STATUS_NONE)
    private val remoteControlNetworkStatusProcessor = DataProcessor.create(NetworkStatus.NETWORK_STATUS_NONE)

    val droneLTETypeData = droneLTEType.toFlow()
    val droneLetSignalFlow = droneLetSignal.toFlow()

    val remoteLetSignalFlow = remoteLetSignal.toFlow()
    val isEnableLTEModeFlow = isEnableLTEMode.toFlow()
    val isSupportLTE = isSupportLTEProcessor.toFlow()

    val droneNetworkStatus = droneNetworkStatusProcessor.toFlow()
    val remoteControlNetworkStatus = remoteControlNetworkStatusProcessor.toFlow()

    private var curDrone: IAutelDroneDevice? = null

    private val lteInfoListener = object : LTELinkInfoListener {
        override fun onLTELinkInfoUpdate(info: LTELinkInfo) {
            AutelLog.d(TAG, "lte info remote info  :${info.getRemoteControllerNetworkInfo()} , drone :${info.getAircraftNetworkInfo()}")
            droneLTEType.emit(info.getAircraftNetworkInfo().networkType)
            droneLetSignal.emit(info.getAircraftNetworkInfo().networkSignalStrength)
            remoteLetSignal.emit(info.getRemoteControllerNetworkInfo().networkSignalStrength)
            isSupportLTEProcessor.emit(info.isSupportLTE())
            droneNetworkStatusProcessor.emit(info.getAircraftNetworkInfo().networkStatus)
            remoteControlNetworkStatusProcessor.emit(info.getRemoteControllerNetworkInfo().networkStatus)
        }
    }

    override fun inSetupWithDrone() {
        curDrone = DeviceManager.getDeviceManager().getFirstDroneDevice() ?: return
        AutelLog.d(TAG, "inSetupWithDrone  -----")
        curDrone?.getLTEManager()?.addLTELinkInfoListener(lteInfoListener)
        curDrone?.getLTEManager()?.getLTEEnhancedTransmissionType(object : CommonCallbacks.CompletionCallbackWithParam<LTELinkType> {
            override fun onFailure(error: IAutelCode, msg: String?) {

            }

            override fun onSuccess(t: LTELinkType?) {
                AutelLog.i(TAG, "getLTEEnhancedTransmissionType: $t")
                isEnableLTEMode.emit(t == LTELinkType.SKY_LINK_LTE)
            }
        })
    }

    override fun inCleanupWithDrone() {
        super.inCleanupWithDrone()
        curDrone?.getLTEManager()?.removeLTELinkInfoListener(lteInfoListener)
    }

    override fun inCleanup() {
        curDrone?.getLTEManager()?.removeLTELinkInfoListener(lteInfoListener)
    }

    fun setEnableLTEMode(isEnable: Boolean) {
        AutelLog.i(TAG, "set lte enable : $isEnable")
        curDrone?.getLTEManager()?.setLTEEnhancedTransmissionType(
            if (isEnable) LTELinkType.SKY_LINK_LTE else LTELinkType.SKY_LINK,
            object : CommonCallbacks.CompletionCallback {
                override fun onFailure(error: IAutelCode, msg: String?) {
                    isEnableLTEMode.emit(!isEnable)
                }

                override fun onSuccess() {
                    AutelLog.i(TAG, "set lte enable result :$isEnable")
                    isEnableLTEMode.emit(isEnable)
                }
            })
    }

    /**
     * 图传信号是否OK
     */
    fun isRcSignalAvailable(): Boolean{
        val rcSignalQuality = DeviceUtils.getLocalRemoteDevice().getDeviceStateData().rcStateNtfyBean.rcSignalQuality
        return rcSignalQuality > 0
    }

}