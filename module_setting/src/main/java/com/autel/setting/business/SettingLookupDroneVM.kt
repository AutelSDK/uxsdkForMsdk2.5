package com.autel.setting.business

import androidx.lifecycle.viewModelScope
import com.autel.common.base.BaseViewModel
import com.autel.common.sdk.KeyManagerCoroutineWrapper
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.FlightPropertyKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.base.KeyTools
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch

/**
 * Created by  2022/10/24
 */
class SettingLookupDroneVM : BaseViewModel() {

    /**
     * 蜂鸣状态
     */
    fun getFcsBuzzingStatus(drone: IAutelDroneDevice, onSuccess: (Boolean) -> Unit, onError: (Throwable) -> Unit) {
        val keyManager = drone.getKeyManager()
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            onError.invoke(throwable)
        }) {
            val key = KeyTools.createKey(FlightPropertyKey.KeyBuzzingStatus)
            onSuccess.invoke(KeyManagerCoroutineWrapper.getValue(keyManager, key))
        }
    }

    fun setFcsBuzzingStatus(drone: IAutelDroneDevice, buzzingStatus: Boolean, onSuccess: () -> Unit, onError: (Throwable) -> Unit) {
        val keyManager = drone.getKeyManager()
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            onError.invoke(throwable)
        }) {
            val key = KeyTools.createKey(FlightPropertyKey.KeyBuzzingStatus)
            KeyManagerCoroutineWrapper.setValue(keyManager, key, buzzingStatus)
            onSuccess.invoke()
        }
    }
}