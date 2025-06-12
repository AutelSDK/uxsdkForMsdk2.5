package com.autel.setting.utils.payload

import com.autel.drone.sdk.libbase.error.AutelStatusCode
import com.autel.drone.sdk.libbase.error.IAutelCode
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.interfaces.IPayloadManager
import com.autel.drone.sdk.vmodelx.manager.keyvalue.callback.CommonCallbacks
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.payload.bean.PayloadInfoBean
import com.autel.drone.sdk.vmodelx.module.payload.PayloadCenter
import com.autel.drone.sdk.vmodelx.module.payload.PayloadIndexType
import com.autel.drone.sdk.vmodelx.module.payload.WidgetValue
import com.autel.drone.sdk.vmodelx.module.payload.data.PayloadWidgetInfo
import com.autel.drone.sdk.vmodelx.module.payload.listener.PayloadWidgetInfoListener
import com.autel.log.AutelLog

/**
 * Copyright: Autel Robotics
 * @author R24033 on 2025/4/21
 * 三方负载
 */
class PayloadPluginItemDelegate(private val type: PayloadIndexType) {

    companion object {
        private const val TAG = "PayloadPluginItemDelegate"
    }

    private val payloadManagerMap = PayloadCenter.get().getPayloadManager()

    private var onWidgetInfoCallback: ((Pair<IAutelDroneDevice, PayloadWidgetInfo>) -> Unit)? = null

    private val payloadWidgetInfoListener: PayloadWidgetInfoListener =
        object : PayloadWidgetInfoListener {
            override fun onPayloadWidgetInfoUpdate(
                device: IAutelDroneDevice,
                widgetInfo: PayloadWidgetInfo
            ) {
                val pair = Pair(device, widgetInfo)
                onWidgetInfoCallback?.invoke(pair)
            }

        }


    /**
     * Widget信息回调
     */
    fun setWidgetInfoCallback(callback: (Pair<IAutelDroneDevice, PayloadWidgetInfo>) -> Unit) {
        onWidgetInfoCallback = callback
    }

    /**
     * 设置widget值
     * @param droneDevice
     * @param value
     * @param onSuccess
     * @param onFailure
     */
    fun setWidgetValue(
        droneDevice: IAutelDroneDevice?,
        value: WidgetValue,
        onSuccess: (() -> Unit)? = null,
        onFailure: ((code: IAutelCode, msg: String?) -> Unit)? = null
    ) {
        val manager = getPayloadManager()
        AutelLog.d(TAG, "setWidgetValue->type:$type")
        manager?.setWidgetValue(droneDevice, value, object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                AutelLog.i(TAG, "[$type] PayloadManager set widget value:${value} success")
                onSuccess?.invoke()
            }

            override fun onFailure(code: IAutelCode, msg: String?) {
                AutelLog.e(TAG, "[$type] PayloadManager set widget value failure")
                onFailure?.invoke(code, msg)
            }

        }) ?: run {
            AutelLog.e(TAG, "[$type] PayloadManager is null")
            onFailure?.invoke(AutelStatusCode.UNKNOWN, "manager is null")
        }
    }


    /**
     * 拉取Widget数据
     * @param droneDevice
     * @return
     */
    fun pullWidgetInfo(
        droneDevice: IAutelDroneDevice?,
    ): Boolean {
        val manager = getPayloadManager()
        AutelLog.d(TAG, "pullWidgetInfo->type:$type")
        manager?.pullWidgetInfoFromPayload(droneDevice) ?: run {
            AutelLog.e(TAG, "[$type] PayloadManager is null")
            return false
        }
        return true
    }

    /**
     * 获取负载管理器
     */
    fun getPayloadManager(): IPayloadManager? {
        return payloadManagerMap[type]
    }

    /**
     * 获取负载挂载位置
     */
    fun getPayloadIndexType(): PayloadIndexType {
        return type
    }


    /**
     * 添加监听器
     */
    fun addPayloadWidgetInfoListener() {
        val manager = getPayloadManager()
        manager?.addPayloadWidgetInfoListener(payloadWidgetInfoListener)
    }

    /**
     * 移除监听器
     */
    fun removePayloadWidgetInfoListener() {
        val manager = getPayloadManager()
        manager?.removePayloadWidgetInfoListener(payloadWidgetInfoListener)
    }

    /**
     * 释放回调
     */
    fun releaseCallbacks() {
        onWidgetInfoCallback = null
    }
}