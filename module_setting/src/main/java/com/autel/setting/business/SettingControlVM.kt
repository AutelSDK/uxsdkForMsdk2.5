package com.autel.setting.business

import androidx.lifecycle.viewModelScope
import com.autel.common.base.BaseViewModel
import com.autel.common.manager.MiddlewareManager
import com.autel.common.sdk.KeyManagerCoroutineWrapper
import com.autel.common.sdk.exception.SdkDeviceNonExistException
import com.autel.drone.sdk.vmodelx.interfaces.IKeyManager
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.FlightPropertyKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.RemoteControllerKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.base.KeyTools
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.remotecontrol.enums.RcOperateModeEnum
import com.autel.log.AutelLog
import com.autel.setting.enums.ExpFeelEnum
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch

/**
 * Created by gaojie 2022/10/17
 */
class SettingControlVM : BaseViewModel() {
    companion object {
        const val TAG = "SettingControlVM"
    }

    /**
     * 兼容中继模式
     */
    fun getRemoteManager(): IKeyManager? {
        return if (isHiddenWork()) getRCHiddenKeyManager() else getRemoteKeyManager()
    }

    /**
     * 是否是中继模式
     */
    private fun isHiddenWork(): Boolean {
        return false
    }

    /**
     * 获取摇杆操控模式
     */
    fun getRCRockerControlMode(onSuccess: (RcOperateModeEnum) -> Unit, onError: (Throwable) -> Unit) {
        AutelLog.i(TAG, "getRCRockerControlMode ->  isHiddenWork=${isHiddenWork()}")
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            onError.invoke(throwable)
            AutelLog.i(TAG, "getRCRockerControlMode ->  throwable=$throwable")
        }) {
            val key = KeyTools.createKey(RemoteControllerKey.KeyRCRockerControlMode)
            getRemoteManager()?.let { KeyManagerCoroutineWrapper.getValue(it, key) }?.let {
                onSuccess.invoke(it)
                AutelLog.i(TAG, "getRCRockerControlMode ->  onSuccess=$it")
            }
        }
    }

    /**
     * 设置摇杆操控模式
     */
    fun setRCRockerControlMode(mode: RcOperateModeEnum, onSuccess: () -> Unit, onError: (Throwable) -> Unit) {
        AutelLog.i(TAG, "setRCRockerControlMode ->  mode=$mode isHiddenWork=${isHiddenWork()}")
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            onError.invoke(throwable)
            AutelLog.i(TAG, "setRCRockerControlMode -> e: $throwable")
        }) {
            val key = KeyTools.createKey(RemoteControllerKey.KeyRCRockerControlMode)
            getRemoteManager()?.let { KeyManagerCoroutineWrapper.setValue(it, key, mode) }
            AutelLog.i(TAG, "setRCRockerControlMode -> success")
            onSuccess.invoke()
        }
    }

    /**
     * 进入遥控器校准模式
     */
    fun enterRCCalibration(
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        AutelLog.i(TAG, "enterRCCalibration ->  ")
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            onError.invoke(throwable)
            AutelLog.i(TAG, "enterRCCalibration -> e: $throwable")
        }) {
            val key = KeyTools.createKey(RemoteControllerKey.KeyRCEnterCalibration)
            getRemoteManager()?.let { KeyManagerCoroutineWrapper.performAction(it, key) }
            onSuccess.invoke()
            AutelLog.i(TAG, "enterRCCalibration -> success")
        }
    }

    /**
     * 退出遥控器校准模式
     */
    fun exitRCCalibration(onSuccess: () -> Unit, onError: (Throwable) -> Unit) {
        AutelLog.i(TAG, "exitRCCalibration ->  ")
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            onError.invoke(throwable)
            AutelLog.i(TAG, "exitRCCalibration -> e: $throwable")
        }) {
            val key = KeyTools.createKey(RemoteControllerKey.KeyRCExitCalibration)
            getRemoteManager()?.let { KeyManagerCoroutineWrapper.performAction(it, key) }
            onSuccess.invoke()
            AutelLog.i(TAG, "exitRCCalibration -> success")
        }
    }

    /**
     * 设置exp手感
     * @param exp exp值
     * @param type 手感方向
     */
    fun setExpValue(exp: Float, type: ExpFeelEnum, onSuccess: () -> Unit, onError: (Throwable) -> Unit) {
        AutelLog.i(TAG, "setExpValue ->  ")
        val keyManager = getKeyManager()
        if (keyManager == null) {
            onError.invoke(SdkDeviceNonExistException())
            return
        }
        viewModelScope.launch(CoroutineExceptionHandler { _, e ->
            onError.invoke(e)
            AutelLog.i(TAG, "setExpValue -> e: $e")
        }) {
            when (type) {
                //默认上升/下降
                ExpFeelEnum.RISE_AND_FALL -> {
                    val key = KeyTools.createKey(FlightPropertyKey.KeyThrustSensitivity)
                    KeyManagerCoroutineWrapper.setValue(keyManager, key, exp)
                    AutelLog.i(TAG, "setExpValue -> RISE_AND_FALL success")
                }
                //右转/左转
                ExpFeelEnum.TURN_LEFT_AND_RIGHT -> {
                    val key = KeyTools.createKey(FlightPropertyKey.KeyYawAngleSensitivity)
                    KeyManagerCoroutineWrapper.setValue(keyManager, key, exp)
                    AutelLog.i(TAG, "setExpValue -> TURN_LEFT_AND_RIGHT success")
                }
                //前进/后退  与 向右/向左需要调用两个接口
                ExpFeelEnum.FORWARD_AND_BACKWARD -> {
                    val key = KeyTools.createKey(FlightPropertyKey.KeyPitchSensitivity)
                    KeyManagerCoroutineWrapper.setValue(keyManager, key, exp)
                    AutelLog.i(TAG, "setExpValue -> FORWARD_AND_BACKWARD success")
                }
            }
            onSuccess.invoke()
        }
    }

    /**
     * 获取exp手感
     * @param type 手感方向
     */
    fun getExpValue(type: ExpFeelEnum, onSuccess: (value: Float) -> Unit, onError: (Throwable) -> Unit) {
        val keyManager = getKeyManager()
        if (keyManager == null) {
            onError.invoke(SdkDeviceNonExistException())
            return
        }
        viewModelScope.launch(CoroutineExceptionHandler { _, e ->
            onError.invoke(e)
        }) {
            when (type) {
                //默认上升/下降
                ExpFeelEnum.RISE_AND_FALL -> {
                    val key = KeyTools.createKey(FlightPropertyKey.KeyThrustSensitivity)
                    onSuccess.invoke(KeyManagerCoroutineWrapper.getValue(keyManager, key))
                }
                //右转/左转
                ExpFeelEnum.TURN_LEFT_AND_RIGHT -> {
                    val key = KeyTools.createKey(FlightPropertyKey.KeyYawAngleSensitivity)
                    onSuccess.invoke(KeyManagerCoroutineWrapper.getValue(keyManager, key))
                }
                //前进/后退  与 向右/向左同一个，所以只需要调用一个既可
                ExpFeelEnum.FORWARD_AND_BACKWARD -> {
                    val key = KeyTools.createKey(FlightPropertyKey.KeyPitchSensitivity)
                    onSuccess.invoke(KeyManagerCoroutineWrapper.getValue(keyManager, key))
                }
            }
        }
    }

    /**
     * 获取遥控器开关机音
     */
    fun getRCVoiceSwitch(onSuccess: (Boolean) -> Unit, onError: (Throwable) -> Unit) {
        AutelLog.i(TAG, "getRCVoiceSwitch -> isHiddenWork=${isHiddenWork()}")
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            onError.invoke(throwable)
            AutelLog.i(TAG, "getRCVoiceSwitch -> e=$throwable")
        }) {
            val key = KeyTools.createKey(RemoteControllerKey.KeyRcStartupVoiceSwitch)
            getRemoteManager()?.let { KeyManagerCoroutineWrapper.getValue(it, key) }?.let {
                onSuccess.invoke(it)
                AutelLog.i(TAG, "getRCVoiceSwitch -> result=$it")
            }
        }
    }

    /**
     * 设置遥控器开关机音
     */
    fun setRCVoiceSwitch(enable: Boolean, onSuccess: () -> Unit, onError: (Throwable) -> Unit) {
        AutelLog.i(TAG, "setRCVoiceSwitch -> enable=$enable isHiddenWork=${isHiddenWork()}")
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            onError.invoke(throwable)
            AutelLog.i(TAG, "setRCVoiceSwitch -> e=$throwable")
        }) {
            val key = KeyTools.createKey(RemoteControllerKey.KeyRcStartupVoiceSwitch)
            getRemoteManager()?.let { KeyManagerCoroutineWrapper.setValue(it, key, enable) }
            AutelLog.i(TAG, "setRCVoiceSwitch -> success")
            onSuccess.invoke()
        }
    }
}