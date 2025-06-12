package com.autel.setting.business

import androidx.lifecycle.viewModelScope
import com.autel.common.base.BaseViewModel
import com.autel.common.sdk.KeyManagerCoroutineWrapper
import com.autel.common.sdk.exception.SdkDeviceNonExistException
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.GimbalKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.base.AutelKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.base.KeyTools
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.gimbal.bean.DroneGimbalStateBean
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

/**
 * Created by  2022/10/13
 */
class SettingGimbalViewModel : BaseViewModel() {

    //开启云台校准 GimbalUploadMsgManager 毁弃，要用就重写
    /*fun setGimbalStartCalibration(owner: LifecycleOwner, onSuccess: (status: Int) -> Unit, onError: (Throwable) -> Unit) {
        val keyManager = getKeyManager()
        if (keyManager == null) {
            onError.invoke(SdkDeviceNonExistException())
        } else {
            viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
                onError.invoke(throwable)
            }) {
                val key = KeyTools.createKey(GimbalKey.KeyStartCalibration)
                keyManager.performAction(key, callback = object : CommonCallbacks.CompletionCallbackWithParam<Void> {
                    override fun onSuccess(t: Void?) {
                        GimbalUploadMsgManager.gimbalHeatBeatData.observe(owner) { droneGimbalStateBean ->
                            onSuccess.invoke(droneGimbalStateBean.gimbalCalibratePercent)
                        }
                    }

                    override fun onFailure(error: IAutelCode, msg: String?) {
                        AutelLog.i("SettingGimbalViewModel", "start calibration Gimbal error")
                        onError.invoke(Exception(msg))
                    }
                })
            }
        }
    }*/

    fun getMaxPitchSpeed(onSuccess: (value: Int) -> Unit, onError: (Throwable) -> Unit) {
        val keyManager = getKeyManager()
        if (keyManager == null) {
            onError.invoke(SdkDeviceNonExistException())
        } else {
            viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
                onError.invoke(throwable)
            }) {
                val key = KeyTools.createKey(GimbalKey.KeyPitchSpeed)
                onSuccess.invoke(KeyManagerCoroutineWrapper.getValue<Int>(keyManager, key))
            }
        }
    }

    //设置云台俯仰轴最大速度
    fun setMaxPitchSpeed(speed: Int, onSuccess: () -> Unit, onError: (Throwable) -> Unit) {
        val keyManager = getKeyManager()
        if (keyManager == null) {
            onError.invoke(SdkDeviceNonExistException())
        } else {
            viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
                onError.invoke(throwable)
            }) {
                val key = KeyTools.createKey(GimbalKey.KeyPitchSpeed)
                KeyManagerCoroutineWrapper.setValue(keyManager, key, speed)
                onSuccess.invoke()
            }
        }
    }

    //重置云台参数
    fun resetGimbalParam(droneGimbalStateBean: DroneGimbalStateBean, onSuccess: () -> Unit, onError: (Throwable) -> Unit) {
        val keyManager = getKeyManager()
        if (keyManager == null) {
            onError.invoke(SdkDeviceNonExistException())
        } else {
            viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
                onError.invoke(throwable)
            }) {
                val key = KeyTools.createKey(GimbalKey.KeyHeatBeat)
                KeyManagerCoroutineWrapper.setValue(keyManager, key, droneGimbalStateBean)
                onSuccess.invoke()
            }
        }
    }

    //云台俯仰角30度
    fun setPitchRote30(switch: Boolean, onSuccess: () -> Unit, onError: (Throwable) -> Unit) {
        val keyManager = getKeyManager()
        if (keyManager == null) {
            onError.invoke(SdkDeviceNonExistException())
        } else {
            viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
                onError.invoke(throwable)
            }) {
                val key = KeyTools.createKey(GimbalKey.KeyPitchAngelRange)
                KeyManagerCoroutineWrapper.setValue(keyManager, key, switch)
                onSuccess.invoke()
            }
        }
    }

    fun getPitchRote30(onSuccess: (Boolean) -> Unit, onError: (Throwable) -> Unit) {
        val keyManager = getKeyManager()
        if (keyManager == null) {
            onError.invoke(SdkDeviceNonExistException())
        } else {
            viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
                onError.invoke(throwable)
            }) {
                val key = KeyTools.createKey(GimbalKey.KeyPitchAngelRange)
                onSuccess.invoke(KeyManagerCoroutineWrapper.getValue(keyManager, key))
            }
        }
    }

    //云台参数重置
    fun resetGimbalPatch(onSuccess: () -> Unit, onError: (Throwable) -> Unit) {
        val keyManager = getKeyManager()
        if (keyManager == null) {
            onError.invoke(SdkDeviceNonExistException())
        } else {
            viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
                onError.invoke(throwable)
            }) {
                val keyList: List<AutelKey<Int>> = listOf(
                    KeyTools.createKey(GimbalKey.KeyPitchAngleRange),
                    KeyTools.createKey(GimbalKey.KeyRollAdjustAngle),
                    KeyTools.createKey(GimbalKey.KeyYawAdjustAngle)
                )
                val a = async { KeyManagerCoroutineWrapper.setValue(keyManager, keyList[0], 0) }
                val b = async { KeyManagerCoroutineWrapper.setValue(keyManager, keyList[1], 0) }
                val c = async { KeyManagerCoroutineWrapper.setValue(keyManager, keyList[2], 0) }
                a.await()
                b.await()
                c.await()
                onSuccess.invoke()
            }
        }
    }

}