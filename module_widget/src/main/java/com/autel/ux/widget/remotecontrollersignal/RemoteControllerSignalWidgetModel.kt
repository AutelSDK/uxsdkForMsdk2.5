package com.autel.ux.widget.remotecontrollersignal

import com.autel.drone.sdk.vmodelx.interfaces.IBaseDevice
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.RemoteControllerKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.ext.create
import com.autel.log.AutelLog
import com.autel.ux.core.base.AutelSDKModel
import com.autel.ux.core.base.WidgetModel
import com.autel.ux.core.utils.DataProcessor
import kotlinx.coroutines.flow.Flow

class RemoteControllerSignalWidgetModel(autelSdkModel: AutelSDKModel, device: IBaseDevice? = null) :
    WidgetModel(autelSdkModel, device) {

    private val remoteControllerSignal = DataProcessor.create(0)

    val currentSignalQuality: Flow<Int>
        get() = remoteControllerSignal.toFlow()

    override fun inSetupWithDrone() {
        AutelLog.d(TAG,"set up bind data processor")
        bindDataProcessor(
            RemoteControllerKey.KeyRCState.create(),
            remoteControllerSignal,
            {
                it.rcSignalQuality
            },
            DeviceManager.getDeviceManager().getLocalRemoteDevice()
        )

        productConnected.collectInModel {
            if (!it) {
                // 如果飞机断开连接, 信号等级设置为 NONE
                remoteControllerSignal.emit(0)
            }
        }
    }

    override fun inCleanup() {

    }

    override fun updateStates() {

    }
}