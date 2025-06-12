package com.autel.ux.core.base

import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.interfaces.IBaseDevice
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.base.AutelKey
import com.autel.ux.core.utils.FlowUtil
import kotlinx.coroutines.flow.Flow

class AutelSDKModel private constructor() {

    companion object {
        private const val TAG = "AutelSDKModel"

        @Volatile
        private var instance: AutelSDKModel? = null

        fun getInstance(): AutelSDKModel {
            return instance ?: synchronized(this) {
                instance ?: AutelSDKModel().also { instance = it }
            }
        }
    }

    fun <T> setValue(device: IBaseDevice, key: AutelKey<T>, value: T, retryCount: Int = 0): Flow<T> {
        return FlowUtil.setValue(device, key, value, retryCount)
    }

    fun <T> addListener(device: IBaseDevice, key: AutelKey<T>, listener: Any? = null): Flow<T> {
        return FlowUtil.addListener(device, key, listener)
    }

    fun <Result> addMultiListener(key: AutelKey<Result>): Flow<Pair<IAutelDroneDevice, Result>> {
        return FlowUtil.addMultiListener(key)
    }

    fun <Result, TransForm> addMultiListener(key: AutelKey<Result>, transform: (value: Result) -> TransForm): Flow<Pair<IAutelDroneDevice, TransForm>> {
        return FlowUtil.addMultiListener(key,transform)
    }

    fun <T, R> addListener(device: IBaseDevice, key: AutelKey<T>, transform: (value: T) -> R, listener: Any? = null): Flow<R> {
        return FlowUtil.addListener(device, key, transform, listener)
    }

    fun removeListener(device: IBaseDevice, listener: Any) {
        device.getKeyManager().cancelListen(listener)
    }
}