package com.autel.ux.core.utils

import com.autel.common.sdk.exception.SdkFailureResultException
import com.autel.drone.sdk.libbase.error.AutelError
import com.autel.drone.sdk.libbase.error.AutelStatusCode
import com.autel.drone.sdk.libbase.error.IAutelCode
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.interfaces.IBaseDevice
import com.autel.drone.sdk.vmodelx.interfaces.IKeyManager
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.drone.sdk.vmodelx.manager.keyvalue.callback.CommonCallbacks
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.base.AutelKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.base.AutelKey.ActionKey
import com.autel.log.AutelLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object FlowUtil {
    private const val TAG = "FlowUtil"

    @JvmStatic
    fun <T> setValue(device: IBaseDevice, key: AutelKey<T>, value: T, retryCount: Int = 0): Flow<T> {
        return flow {
            setValue(device.getKeyManager(), key, value, retryCount)
            emit(value)
        }
    }

    @JvmStatic
    fun <T> setValue(devices: List<IBaseDevice>, key: AutelKey<T>, value: T, retryCount: Int = 0): Flow<T> {
        return flow {
            for (device in devices) {
                setValue(device.getKeyManager(), key, value, retryCount)
            }
            emit(value)
        }
    }

    @JvmStatic
    fun <T> getValue(device: IBaseDevice, key: AutelKey<T>, retryCount: Int = 0): Flow<T> {
        return flow {
            emit(getValueWithSuspend(device.getKeyManager(), key, retryCount))
        }
    }

    fun <T, Result> performAction(
        device: IBaseDevice,
        key: ActionKey<T, Result>,
        param: T,
        retryCount: Int = 0,
        bizType: String? = null,
    ): Flow<Result> {
        return flow {
            emit(performAction(device.getKeyManager(), key, param, retryCount, bizType))
        }
    }

    fun <T, Result> performAction(
        devices: List<IBaseDevice>,
        key: ActionKey<T, Result>,
        param: T,
        retryCount: Int = 0,
        bizType: String? = null,
    ): Flow<List<Result>> {
        return flow {
            val results = mutableListOf<Result>()
            for (device in devices) {
                results += performAction(device.getKeyManager(), key, param, retryCount, bizType)
            }
            emit(results)
        }
    }

    @JvmStatic
    fun <T> addListener(device: IBaseDevice, key: AutelKey<T>, listener: Any? = null): Flow<T> {
        val flow = MutableStateFlow<T?>(null)
        val keyManager = device.getKeyManager()
        val keyListener = object : CommonCallbacks.KeyListener<T> {
            override fun onValueChange(oldValue: T?, newValue: T) {
                flow.value = newValue // 更新 MutableStateFlow 的值
            }
        }
        if (listener != null) {
            keyManager.listen(key, listener, keyListener)
        } else {
            keyManager.listen(key, keyListener)
        }
        return flow.filter { it != null }.map { it!! }.onCompletion {
            if (it is CancellationException) { // 取消订阅号,自动移除监听器
                AutelLog.i(TAG, "toFlow: cancel listen for key ${key.keyInfo.keyName}")
                if (listener != null) {
                    keyManager.cancelListen(key, listener)
                } else {
                    keyManager.cancelListen(key, keyListener)
                }
            }
        }
    }

    @JvmStatic
    fun <Param, Result> addListener(
        device: IBaseDevice,
        key: AutelKey<Param>,
        transform: (value: Param) -> Result,
        listener: Any? = null,
    ): Flow<Result> {
        val flow = MutableStateFlow<Param?>(null)
        val keyManager = device.getKeyManager()
        val keyListener = object : CommonCallbacks.KeyListener<Param> {
            override fun onValueChange(oldValue: Param?, newValue: Param) {
                flow.value = newValue // 更新 MutableStateFlow 的值
            }
        }
        if (listener != null) {
            keyManager.listen(key, listener, keyListener)
        } else {
            keyManager.listen(key, keyListener)
        }
        return flow
            .filter { it != null }
            .map {
                transform.invoke(it!!)
            }
            .onCompletion {
                if (it is CancellationException) { // 取消订阅号,自动移除监听器
                    AutelLog.i(TAG, "toFlow: cancel listen for key ${key.keyInfo.keyName}")
                    if (listener != null) {
                        keyManager.cancelListen(key, listener)
                    } else {
                        keyManager.cancelListen(key, keyListener)
                    }
                }
            }
    }

    /**
     * 添加多机监听
     */
    fun <Result> addMultiListener(
        key: AutelKey<Result>
    ): Flow<Pair<IAutelDroneDevice, Result>> {
        val flow =
            MutableSharedFlow<Pair<IAutelDroneDevice, Result>>(replay = 0, extraBufferCapacity = 0, onBufferOverflow = BufferOverflow.DROP_OLDEST)
        val callback = object : DeviceManager.KeyManagerListenerCallBack {
            override fun onListenerValueChanged(value: DeviceManager.DeviceListenerResult<*>) {
                val data = value.result as Result
                flow.tryEmit(value.drone to data)
            }

        }
        DeviceManager.getDeviceManager().addDroneDevicesListener(key, callback)
        return flow.onCompletion {
            if (it is CancellationException) { // 取消订阅号,自动移除监听器
                AutelLog.i(TAG, "toFlow: cancel listen for key ${key.keyInfo.keyName}")
                DeviceManager.getDeviceManager().removeDroneDevicesListener(key, callback)
            }
        }
    }

    /**
     * 添加多机监听
     */
    fun <Result, TransForm> addMultiListener(
        key: AutelKey<Result>,
        transform: (value: Result) -> TransForm,
    ): Flow<Pair<IAutelDroneDevice, TransForm>> {
        val flow =
            MutableSharedFlow<Pair<IAutelDroneDevice, TransForm>>(replay = 0, extraBufferCapacity = 0, onBufferOverflow = BufferOverflow.DROP_OLDEST)
        val callback = object : DeviceManager.KeyManagerListenerCallBack {
            override fun onListenerValueChanged(value: DeviceManager.DeviceListenerResult<*>) {
                val data = value.result as Result
                flow.tryEmit(value.drone to transform.invoke(data))
            }

        }
        DeviceManager.getDeviceManager().addDroneDevicesListener(key, callback)
        return flow.onCompletion {
            if (it is CancellationException) { // 取消订阅号,自动移除监听器
                AutelLog.i(TAG, "toFlow: cancel listen for key ${key.keyInfo.keyName}")
                DeviceManager.getDeviceManager().removeDroneDevicesListener(key, callback)
            }
        }
    }


    private suspend fun <Param> setValue(
        keyManager: IKeyManager,
        key: AutelKey<Param>,
        param: Param,
        retryCount: Int,
    ) {
        return suspendCancellableCoroutine { cor ->
            keyManager.setValue(key, param, object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    if (cor.isActive.not()) {
                        return
                    }
                    cor.resume(Unit)
                }

                override fun onFailure(code: IAutelCode, msg: String?) {
                    if (cor.isActive.not()) {
                        return
                    }
                    cor.resumeWithException(SdkFailureResultException(code, msg))
                }
            }, retryCount)
        }
    }

    suspend fun <Param> getValueWithSuspend(
        keyManager: IKeyManager,
        key: AutelKey<Param>,
        retryCount: Int = 0,
    ) = suspendCancellableCoroutine<Param> { cor ->
        keyManager.getValue(key, object : CommonCallbacks.CompletionCallbackWithParam<Param> {
            override fun onSuccess(t: Param?) {
                if (cor.isActive.not()) {
                    return
                }
                if (t == null) {
                    cor.resumeWithException(SdkFailureResultException(AutelStatusCode.UNKNOWN, AutelError.COMMAND_FAILED.desc))
                } else {
                    cor.resume(t)
                }
            }

            override fun onFailure(code: IAutelCode, msg: String?) {
                if (cor.isActive.not()) {
                    return
                }
                cor.resumeWithException(SdkFailureResultException(code, msg))
            }
        }, retryCount)
    }

    private suspend fun <Param, Result> performAction(
        keyManager: IKeyManager,
        key: ActionKey<Param, Result>,
        param: Param,
        retryCount: Int,
        bizType: String?,
    ) = suspendCancellableCoroutine<Result> { cor ->
        keyManager.performAction(key, param, object : CommonCallbacks.CompletionCallbackWithParam<Result> {
            override fun onFailure(code: IAutelCode, msg: String?) {
                if (cor.isActive.not()) {
                    return
                }
                cor.resumeWithException(SdkFailureResultException(code, msg))
            }

            override fun onSuccess(t: Result?) {
                if (cor.isActive.not()) {
                    return
                }
                if (t == null) {
                    cor.resumeWithException(SdkFailureResultException(AutelStatusCode.UNKNOWN, AutelError.COMMAND_FAILED.desc))
                } else {
                    cor.resume(t)
                }
            }
        }, retryCount, bizType)
    }


}