package com.autel.ux.core.base

import com.autel.common.utils.DeviceUtils
import com.autel.drone.sdk.vmodelx.device.statenew.data.ControlData
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.interfaces.IBaseDevice
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.drone.sdk.vmodelx.manager.data.ControlMode
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.FlightControlKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.RemoteControllerKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.ext.create
import com.autel.log.AutelLog
import com.autel.ux.core.utils.DataProcessor
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow

abstract class WidgetModel(
    autelSdkModel: AutelSDKModel,
    protected var widgetDevice: IBaseDevice? = null,
) : BaseWidgetModel(autelSdkModel) {

    /**
     * 当前控制模式
     */
    private val controlModeProcessor: DataProcessor<ControlData> = DataProcessor.create(ControlData(ControlMode.UNKNOWN, emptyList()))

    /**
     * 是否已经连接飞机
     */
    private val productConnectionProcessor: DataProcessor<Boolean> = DataProcessor.create(false)

    /**
     * watch 飞机列表
     */
    private val watchDevicesProcessor = DataProcessor.create<List<IAutelDroneDevice>>(listOf())

    val controlMode: StateFlow<ControlData>
        get() = controlModeProcessor.toFlow()

    val productConnected: StateFlow<Boolean>
        get() = productConnectionProcessor.toFlow()

    val watchDevices: StateFlow<List<IAutelDroneDevice>>
        get() = watchDevicesProcessor.toFlow()

    /**
     * 当飞机发生变化时,会触发
     */
    abstract fun inSetupWithDrone()

    open fun inSetupWithRemoteControl() {}

    /**
     * 当受控飞机发生变化时,会触发
     */
    open fun inCleanupWithDrone() {}

    /**
     * clean up all
     */
    abstract fun inCleanup()

    /**
     * 当单机/组网模式发生变化时，是否需要重新加载数据
     */
    open fun resetWhenModeChange(): Boolean {
        return widgetDevice == null // 传了飞机,就不再根据飞机变化刷新数据
    }

    /**
     * 是否需要在无人机变化时重新加载数据
     */
    open fun resetWhenDroneChanged(): Boolean {
        return widgetDevice == null // 传了飞机,就不再根据飞机变化刷新数据
    }

    /**
     * 是否使用默认设备, 为 true 时, 未bindDevice会默认获取first device
     */
    protected open fun useDefaultDevice(): Boolean {
        return true
    }

    override fun setup() {
        super.setup()
        AutelLog.d(TAG, "widget setup")
        scope = CoroutineScope(Dispatchers.Main + CoroutineExceptionHandler { context, thr ->
            AutelLog.e(TAG, "CoroutineExceptionHandler caught exception: $thr")
        })
        setupWithDrone()
        setupWithRemoteControl()
    }

    private fun setupWithDrone() {
        AutelLog.d(TAG, "setup with drone .")
        val device = widgetDevice ?: if (useDefaultDevice()) {
            DeviceUtils.singleControlDrone() ?: DeviceManager.getFirstDroneDevice() ?: return
        } else {
            return
        }
        bindDataProcessor(FlightControlKey.KeyConnection.create(), productConnectionProcessor, device)
        inSetupWithDrone()
    }


    private fun setupWithRemoteControl() {
        AutelLog.d(TAG, "setup with remote control.")
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
            AutelLog.d(TAG, "watch change : $it , restart :${resetWhenDroneChanged()}")
            if (resetWhenDroneChanged().not()) return@collectInModel
            restart()
        }
        controlMode.collectInModel {
            AutelLog.d(TAG, "control mode change $it . , is reset ? ${resetWhenModeChange()}")
            if (resetWhenModeChange().not()) return@collectInModel
            restart()
        }
        inSetupWithRemoteControl()
    }

    override fun cleanup() {
        AutelLog.d(TAG, "widget cleanup...")
        try {
            inCleanup()
            inCleanupWithDrone()
        } catch (e: Exception) {
            AutelLog.e(TAG, "inClean() failed: $e")
        }
        super.cleanup()
    }

    /**
     * 更新绑定的设备
     */
    fun updateDevice(device: IBaseDevice?) {
        widgetDevice = device
        restart()
    }

    /**
     * 重置,例如当受控飞机发生变化时,重置与飞机相关bind
     */
    protected fun restart() {
        AutelLog.d(TAG, "restart model , when drone change ? :${resetWhenDroneChanged()} , mode change :${resetWhenModeChange()}")
        cleanupWithDrone()
        setupWithDrone()
    }

    private fun cleanupWithDrone() {
        AutelLog.d(TAG, "cleanup with drone ..")
        try {
            inCleanupWithDrone()
        } catch (e: Exception) {
            AutelLog.e(TAG, "inClean() failed: $e")
        } finally {
            jobs.forEach {
                it.cancel()
            }
            jobs.clear()
        }
    }

    /*  protected fun <T> bindDataProcessor(key: UXKey, dataProcessor: DataProcessor<T>) {
          registerKey(key, dataProcessor)
      }

      private fun <T> registerKey(key: UXKey, dataProcessor: DataProcessor<T>) {

      }*/

}