package com.autel.setting.business

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.autel.common.extension.asLiveData
import com.autel.common.sdk.KeyManagerCallbackWrapper
import com.autel.drone.sdk.libbase.error.IAutelCode
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.drone.sdk.vmodelx.manager.keyvalue.callback.CommonCallbacks
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.CommonKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.GimbalKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.base.KeyTools
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.bean.DroneSystemStateLFNtfyBean

/**
 * Created by  2024/11/8
 *
 *  * 激光测距
 *  * 启动时，飞机连接时，查询所有的激光测距状态，
 *  * 如果当前看的飞机激光测距是开，则打开功能状态，
 *  * 如果当前飞机激光测距功能光比，则关闭功能
 *  *
 *  * 打开、关闭功能，打开时、仅开启当前屏幕显示的飞机的，关闭时，仅关闭当前显示的飞机的
 *  * 切换控制时不处理，watchDroneNumber变化时，重新查询飞机开关，并刷新UI
 *  * 飞机上线时，如果是当前watchDroneNumber飞机，则重新查询飞机开关，并刷新UI
 */
class RangingViewModel : ViewModel() {

    private var _rangingFunctionState: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)
    var rangingFunctionState = _rangingFunctionState.asLiveData()

    private var watchDroneNumber = -1

    private var lfNtfyBeanCallback = object : DeviceManager.KeyManagerListenerCallBack {
        override fun onListenerValueChanged(value: DeviceManager.DeviceListenerResult<*>) {
            val result = value.result
            if(watchDroneNumber != value.drone.deviceNumber()) {
                return
            }
            if (result is DroneSystemStateLFNtfyBean) {
                val isOpen = result.laserRangingSwitch == 1
                if (_rangingFunctionState.value != isOpen) {
                    _rangingFunctionState.value = isOpen
                }
            }
        }
    }

    init {
        DeviceManager.getMultiDeviceOperator().addDroneDevicesListener(KeyTools.createKey(CommonKey.KeyDroneSystemStatusLFNtfy), lfNtfyBeanCallback)
    }


    fun setWatchDroneNumber(watchDroneNumber:Int){
        this.watchDroneNumber = watchDroneNumber
    }



    /**
     * 查询激光测距状态
     */
    fun queryRangingSwitch(drone: IAutelDroneDevice) {
        KeyManagerCallbackWrapper.getValue(
            drone.getKeyManager(),
            KeyTools.createKey(GimbalKey.KeyLaserRangingSwitch),
            object : CommonCallbacks.CompletionCallbackWithParam<Boolean> {
                override fun onSuccess(t: Boolean?) {
//                    if (t == true) {
//                        _rangingFunctionState.value = true
//                    } else {
//                        _rangingFunctionState.value = false
//                    }
                }

                override fun onFailure(error: IAutelCode, msg: String?) {

                }
            })
    }

    /**
     * 开关激光测距
     */
    fun setRangingSwitch(drone: IAutelDroneDevice, param: Boolean) {
        KeyManagerCallbackWrapper.setValue(
            drone.getKeyManager(),
            KeyTools.createKey(GimbalKey.KeyLaserRangingSwitch),
            param,
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
//                    _rangingFunctionState.value = param
                }

                override fun onFailure(code: IAutelCode, msg: String?) {

                }
            })
    }

    override fun onCleared() {
        super.onCleared()
        DeviceManager.getMultiDeviceOperator().removeDroneDevicesListener(KeyTools.createKey(CommonKey.KeyDroneSystemStatusHFNtfy), lfNtfyBeanCallback)
    }
}