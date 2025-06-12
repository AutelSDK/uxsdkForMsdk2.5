package com.autel.ux.core.base

import android.util.Log
import androidx.annotation.CallSuper
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.interfaces.IBaseDevice
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.base.AutelKey
import com.autel.log.AutelLog
import com.autel.ux.core.utils.DataProcessor
import com.autel.ux.core.utils.MultiDataProcessor
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

abstract class BaseWidgetModel(val autelSdkModel: AutelSDKModel) {
    protected val TAG = this::class.java.simpleName

    protected var scope: CoroutineScope? = null

    protected val jobs = mutableListOf<Job>()

    open fun updateStates(){}

    @CallSuper
    open fun setup() {
        scope = CoroutineScope(Dispatchers.Main + CoroutineName(TAG) + CoroutineExceptionHandler { _, t ->
            AutelLog.e(TAG, "CoroutineExceptionHandler caught exception when setup: $t")
        })
    }

    @CallSuper
    open fun cleanup() {
        jobs.forEach { it.cancel() }
        jobs.clear()
        scope?.cancel()
        scope = null
    }

    protected fun <T, R> bindDataProcessor(
        keyInfo: AutelKey<T>,
        dataProcessor: DataProcessor<R>,
        transform: (value: T) -> R,
        bindDrone: IBaseDevice? = null,
        changeWithDrone: Boolean = true,
    ) {
        val device = bindDrone ?: return
        registerKey(
            keyInfo, dataProcessor::emit, // 传递 emit 方法
            transform, device, changeWithDrone
        )
    }

    protected fun <T> bindDataProcessor(
        keyInfo: AutelKey<T>,
        dataProcessor: DataProcessor<T>,
        bindDrone: IBaseDevice? = null,
        changeWithDrone: Boolean = true,
    ) {
        val device = bindDrone ?: return
        registerKey(keyInfo, dataProcessor::emit, device, changeWithDrone)
    }

    private fun <T, R> registerKey(
        keyInfo: AutelKey<T>,
        action: suspend (R?) -> Unit,
        transform: (value: T) -> R,
        bindDrone: IBaseDevice,
        changeWithDrone: Boolean = true,
    ) {
        if (changeWithDrone) {
            jobs += autelSdkModel.addListener(bindDrone, keyInfo, transform).catch {
                AutelLog.i(TAG, "registerKey failed to collect key: $keyInfo, error: $it")
            }.conflate() // 只保留最新值
                .onEach(action).collectInModel {
                    updateStates()
                } ?: return
        } else {
            autelSdkModel.addListener(bindDrone, keyInfo, transform).catch {
                AutelLog.i(TAG, "registerKey failed to collect key: $keyInfo, error: $it")
            }.conflate() // 只保留最新值
                .onEach(action).collectInModel {
                    updateStates()
                }
        }
    }

    private fun <T> registerKey(
        keyInfo: AutelKey<T>,
        action: suspend (T?) -> Unit,
        bindDrone: IBaseDevice,
        changeWithDrone: Boolean = true,
    ) {
        if (changeWithDrone) {
            jobs += autelSdkModel.addListener(bindDrone, keyInfo, this)
                .catch {
                    AutelLog.i(TAG, "registerKey failed to collect key: $keyInfo, error: $it")
                }.conflate() // 只保留最新值
                .onEach(action).collectInModel {
                    updateStates()
                } ?: return
        } else {
            autelSdkModel.addListener(bindDrone, keyInfo, this)
                .catch {
                    AutelLog.i(TAG, "registerKey failed to collect key: $keyInfo, error: $it")
                }.conflate() // 只保留最新值
                .onEach(action).collectInModel {
                    updateStates()
                }
        }
    }

    protected fun <T, R> bindMultiDataProcessor(
        keyInfo: AutelKey<T>,
        dataProcessor: DataProcessor<Pair<IAutelDroneDevice, R>>,
        transform: (value: T) -> R,
    ) {
        registerMultiKey(
            keyInfo, dataProcessor::emit, // 传递 emit 方法
            transform
        )
    }

    protected fun <T> bindMultiDataProcessor(
        keyInfo: AutelKey<T>,
        dataProcessor: MultiDataProcessor<T>,
        changeWithDrone: Boolean = true,
    ) {
        registerMultiKey(keyInfo, dataProcessor::emit, changeWithDrone)
    }

    private fun <T, R> registerMultiKey(
        keyInfo: AutelKey<T>,
        action: suspend (Pair<IAutelDroneDevice, R>) -> Unit,
        transform: (value: T) -> R,
        changeWithDrone: Boolean = true,
    ) {
        if (changeWithDrone) {
            jobs += autelSdkModel.addMultiListener(keyInfo, transform).catch {
                AutelLog.i(TAG, "registerKey failed to collect key: $keyInfo, error: $it")
            }.conflate() // 只保留最新值
                .onEach(action).collectInModel {
                    updateStates()
                } ?: return
        } else {
            autelSdkModel.addMultiListener(keyInfo, transform).catch {
                AutelLog.i(TAG, "registerKey failed to collect key: $keyInfo, error: $it")
            }.conflate() // 只保留最新值
                .onEach(action).collectInModel {
                    updateStates()
                }
        }
    }

    private fun <T> registerMultiKey(
        keyInfo: AutelKey<T>,
        action: suspend (Pair<IAutelDroneDevice, T>) -> Unit,
        changeWithDrone: Boolean = true,
    ) {
        if (changeWithDrone) {
            jobs += autelSdkModel.addMultiListener(keyInfo)
                .catch {
                    AutelLog.i(TAG, "registerKey failed to collect key: $keyInfo, error: $it")
                }.conflate() // 只保留最新值
                .onEach(action).collectInModel {
                    updateStates()
                } ?: return
        } else {
            autelSdkModel.addMultiListener(keyInfo)
                .catch {
                    AutelLog.i(TAG, "registerKey failed to collect key: $keyInfo, error: $it")
                }.conflate() // 只保留最新值
                .onEach(action).collectInModel {
                    updateStates()
                }
        }
    }

    protected fun <T> Flow<T>.collectInModel(collect: FlowCollector<T>): Job? {
        return scope?.launch(Dispatchers.Main.immediate + CoroutineName(TAG) + CoroutineExceptionHandler { _, t ->
            AutelLog.e(TAG, "CoroutineExceptionHandler caught exception when collectInModel: $t")
        }) {
            this@collectInModel.collect(collect)
        }
    }

    protected fun logi(msg: String) {
        AutelLog.i(TAG, msg)
    }

    protected fun loge(error: String, t: Throwable? = null) {
        AutelLog.e(TAG, "$error :${if (t != null) Log.getStackTraceString(t) else ""}")
    }
}