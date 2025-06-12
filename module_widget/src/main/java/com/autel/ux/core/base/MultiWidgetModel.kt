package com.autel.ux.core.base

import android.util.Log
import com.autel.drone.sdk.vmodelx.device.statenew.data.ControlData
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.drone.sdk.vmodelx.manager.data.ControlMode
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.FlightControlKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.RemoteControllerKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.ext.create
import com.autel.ux.core.utils.DataProcessor
import com.autel.ux.core.utils.MultiDataProcessor
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

abstract class MultiWidgetModel(
    autelSdkModel: AutelSDKModel,
) : BaseWidgetModel(autelSdkModel) {

    /**
     * 当前控制模式
     */
    private val controlModeProcessor: DataProcessor<ControlData> = DataProcessor.create(ControlData(ControlMode.UNKNOWN, emptyList()))

    /**
     * 是否已经连接飞机
     */
    private val productConnectionProcessor: MultiDataProcessor<Boolean> = MultiDataProcessor.create(false)

    /**
     * watch 飞机列表
     */
    private val watchDevicesProcessor = DataProcessor.create<List<IAutelDroneDevice>>(listOf())

    val controlMode: StateFlow<ControlData>
        get() = controlModeProcessor.toFlow()

    val productConnected: SharedFlow<Pair<IAutelDroneDevice, Boolean>>
        get() = productConnectionProcessor.toFlow()

    val watchDevices: StateFlow<List<IAutelDroneDevice>>
        get() = watchDevicesProcessor.toFlow()

    override fun setup() {
        super.setup()
        Log.d(TAG, "widget setup")
        setupWithDrone()
        setupWithRemoteControl()
    }

    private fun setupWithDrone() {
        if (scope != null) {
            Log.d(TAG, "widget scope is isActive...")
            return
        }
        Log.d(TAG, "setup with drone .")
        bindMultiDataProcessor(FlightControlKey.KeyConnection.create(), productConnectionProcessor)
    }


    private fun setupWithRemoteControl() {
        if (scope != null) {
            Log.d(TAG, " widget remote scope is isActive...")
            return
        }
        Log.d(TAG, "setup with remote control.")
        bindDataProcessor(
            RemoteControllerKey.KeyControlChange.create(),
            controlModeProcessor,
            DeviceManager.getDeviceManager().getLocalRemoteDevice(),
            false,
        )
        bindDataProcessor(
            RemoteControllerKey.KeyWatchChange.create(),
            watchDevicesProcessor,
            DeviceManager.getDeviceManager().getLocalRemoteDevice(),
            false
        )
        watchDevices.collectInModel {
            Log.d(TAG, "watch change : $it ")

        }
        controlMode.collectInModel {
            Log.d(TAG, "control mode change $it}")

        }
    }

    override fun cleanup() {
        super.cleanup()
    }

    /*  protected fun <T> bindDataProcessor(key: UXKey, dataProcessor: DataProcessor<T>) {
          registerKey(key, dataProcessor)
      }

      private fun <T> registerKey(key: UXKey, dataProcessor: DataProcessor<T>) {

      }*/
}