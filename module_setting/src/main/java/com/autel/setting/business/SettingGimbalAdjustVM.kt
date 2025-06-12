package com.autel.setting.business

import androidx.lifecycle.viewModelScope
import com.autel.common.base.BaseViewModel
import com.autel.common.sdk.KeyManagerCoroutineWrapper
import com.autel.common.sdk.exception.SdkDeviceNonExistException
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.GimbalKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.base.KeyTools
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch

class SettingGimbalAdjustVM : BaseViewModel() {

    /**
     * 设置俯仰角
     */
    fun adjustGimbalPitch(degree: Int, onSuccess: () -> Unit, onError: (Throwable) -> Unit) {
        val keyManager = getKeyManager()
        if (keyManager == null) {
            onError.invoke(SdkDeviceNonExistException())
        } else {
            viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
                onError.invoke(throwable)
            }) {
                val key = KeyTools.createKey(GimbalKey.KeyPitchAngleRange)
                KeyManagerCoroutineWrapper.setValue(keyManager, key, degree)
                onSuccess.invoke()
            }
        }
    }

    /**
     * 获取俯仰角度
     *
     */
    fun getGimbalPitch(onSuccess: (Int) -> Unit, onError: (Throwable) -> Unit) {
        val keyManager = getKeyManager()
        if (keyManager == null) {
            onError.invoke(SdkDeviceNonExistException())
        } else {
            viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
                onError.invoke(throwable)
            }) {
                val key = KeyTools.createKey(GimbalKey.KeyPitchAngleRange)
                onSuccess.invoke(KeyManagerCoroutineWrapper.getValue(keyManager, key))
            }
        }
    }

    /**
     * 微调水平角
     */
    fun adjustGimbalRoll(degree: Int, onSuccess: () -> Unit, onError: (Throwable) -> Unit) {
        val keyManager = getKeyManager()
        if (keyManager == null) {
            onError.invoke(SdkDeviceNonExistException())
        } else {
            viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
                onError.invoke(throwable)
            }) {
                val key = KeyTools.createKey(GimbalKey.KeyRollAdjustAngle)
                KeyManagerCoroutineWrapper.setValue(keyManager, key, degree)
                onSuccess.invoke()
            }
        }
    }

    /**
     * 获取水平角
     */
    fun getGimbalRoll(onSuccess: (Int) -> Unit, onError: (Throwable) -> Unit) {
        val keyManager = getKeyManager()
        if (keyManager == null) {
            onError.invoke(SdkDeviceNonExistException())
        } else {
            viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
                onError.invoke(throwable)
            }) {
                val key = KeyTools.createKey(GimbalKey.KeyRollAdjustAngle)
                KeyManagerCoroutineWrapper.getValue(keyManager, key)
                onSuccess.invoke(KeyManagerCoroutineWrapper.getValue(keyManager, key))
            }
        }
    }

    /**
     * 微调偏航角
     */
    fun adjustGimbalYaw(degree: Int, onSuccess: () -> Unit, onError: (Throwable) -> Unit) {
        val keyManager = getKeyManager()
        if (keyManager == null) {
            onError.invoke(SdkDeviceNonExistException())
        } else {
            viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
                onError.invoke(throwable)
            }) {
                val key = KeyTools.createKey(GimbalKey.KeyYawAdjustAngle)
                KeyManagerCoroutineWrapper.setValue(keyManager, key, degree)
                onSuccess.invoke()
            }
        }
    }

    /**
     * 获取偏航角
     */
    fun getGimbalYaw(onSuccess: (Int) -> Unit, onError: (Throwable) -> Unit) {
        val keyManager = getKeyManager()
        if (keyManager == null) {
            onError.invoke(SdkDeviceNonExistException())
        } else {
            viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
                onError.invoke(throwable)
            }) {
                val key = KeyTools.createKey(GimbalKey.KeyYawAdjustAngle)
                onSuccess.invoke(KeyManagerCoroutineWrapper.getValue(keyManager, key))
            }
        }
    }
}