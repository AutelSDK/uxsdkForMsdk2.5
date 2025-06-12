package com.autel.setting.utils.payload

import androidx.lifecycle.MutableLiveData
import com.autel.common.utils.DeviceUtils
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.interfaces.IControlDroneListener
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.drone.sdk.vmodelx.manager.data.ControlMode
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.payload.bean.PayloadInfoBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.payload.enums.PayloadType
import com.autel.drone.sdk.vmodelx.module.payload.IPayloadListener
import com.autel.drone.sdk.vmodelx.module.payload.PayloadCenter
import com.autel.drone.sdk.vmodelx.module.payload.PayloadIndexType
import com.autel.drone.sdk.vmodelx.module.payload.data.PayloadWidgetInfo
import com.autel.log.AutelLog
import com.autel.setting.BuildConfig
import com.autel.setting.bean.PluginBean
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Copyright: Autel Robotics
 * @author R24033 on 2025/4/22
 * 三方负载
 */
class PluginsDataManager private constructor() : IControlDroneListener, IPayloadListener {


    companion object {
        private const val TAG = "PluginsDataManager"
        private val instance = PluginsDataManager()

        @Synchronized
        fun getInstance(): PluginsDataManager {
            return instance
        }
    }

    //delegate
    private val upItemDelegate by lazy { PayloadPluginItemDelegate(PayloadIndexType.UP) }
    private val mainItemDelegate by lazy { PayloadPluginItemDelegate(PayloadIndexType.LEFT_OR_MAIN) }
    private val rightItemDelegate by lazy { PayloadPluginItemDelegate(PayloadIndexType.RIGHT) }
    private val externalItemDelegate by lazy { PayloadPluginItemDelegate(PayloadIndexType.EXTERNAL) }
    private val external2ItemDelegate by lazy { PayloadPluginItemDelegate(PayloadIndexType.EXTERNAL_2) }
    private val external3ItemDelegate by lazy { PayloadPluginItemDelegate(PayloadIndexType.EXTERNAL_3) }

    //widget info:当前受控飞机数据
    private val widgetInfoMap: HashMap<PayloadIndexType, PayloadWidgetInfo> by lazy { HashMap() }

    private val widgetChannel: Channel<Long> = Channel()

    //负载数据
    var basicInfoLiveData: MutableLiveData<MutableList<PluginBean>> =
        MutableLiveData()

    //负载信息
    private val basicInfoList: MutableList<PluginBean> = ArrayList()

    //widget info
    var widgetInfoLiveData: MutableLiveData<HashMap<PayloadIndexType, PayloadWidgetInfo>> =
        MutableLiveData()

    private val mainScope = MainScope()

    fun init() {
        publishWidgetInfo()
        registerWidgetInfoCallback()
        addAllPayloadListener()

        updateWidgetInfo()
        updatePayloadBasicInfo()

        DeviceManager.getMultiDeviceOperator().addControlChangeListener(this)
        PayloadCenter.get().addPayloadListener(this)
    }


    /**
     * 获取飞机挂载负载信息
     */
    fun updatePayloadBasicInfo(): List<PayloadInfoBean> {
        val droneDevice = getControlDrone()
        val mode = DeviceUtils.getControlMode()

        val list = if (mode == ControlMode.SINGLE && droneDevice != null) {
            PayloadCenter.get().getPayloadInfosByDevice(droneDevice)
        } else {
            emptyList()
        }
        //转化为一级面板对应数据
        convertBasicInfo(list)
        return emptyList()
    }

    /**
     * 更新飞机Widget信息
     */
    fun updateWidgetInfo() {
        val droneDevice = getControlDrone()
        val mode = DeviceUtils.getControlMode()

        if (widgetInfoMap.isNotEmpty()) {
            widgetInfoMap.clear()
        }
        if (mode == ControlMode.SINGLE && droneDevice != null) {
            pullAllWidgetInfo(droneDevice)
        }
    }

    /**
     * 是否只有喊话探照一体机
     */
    fun enablePSDKSetting(): Boolean {
//        if (BuildConfig.DEBUG) {
//            //调试模式允许出现选项
//            return false
//        }
        if (basicInfoList.isNotEmpty()) {
            val size = basicInfoList.size
            if (size == 1 && basicInfoList[0].payloadType.value == 6) {
                return false
            }
        }else{
            return false
        }
        return true

    }

    fun onDestroy() {
        removeAllPayloadListener()
        releaseCallbacks()
        DeviceManager.getMultiDeviceOperator().removeControlChangeListener(this)
        PayloadCenter.get().removePayloadListener(this)
    }


    override fun onControlChange(mode: ControlMode, droneList: List<IAutelDroneDevice>) {
        AutelLog.d(TAG, "onControlChange->mode:$mode")
        updatePayloadBasicInfo()
        updateWidgetInfo()
    }

    override fun onPayloadSizeChanged(size: Int) {
        updatePayloadBasicInfo()
        updateWidgetInfo()
    }

    //======================================内部方法==========================================//

    /**
     * 是否当前单控飞机
     */
    private fun isCurrentControlDrone(drone: IAutelDroneDevice): Boolean {
        return getControlDrone()?.deviceNumber() == drone.deviceNumber()
    }

    /**
     * 获取单控飞机
     */
    private fun getControlDrone(): IAutelDroneDevice? {
        return DeviceUtils.singleControlDrone()
    }

    /**
     * 转化数据
     */
    private fun convertBasicInfo(list: List<PayloadInfoBean>) {
        if (basicInfoList.isNotEmpty()) {
            basicInfoList.clear()
        }
        if (list.isNotEmpty()) {
            for (index in list.indices) {
                val item = list[index]
                val pluginBean = PluginBean().apply {
                    this.info = item
                    this.payloadIndexType =
                        PayloadIndexType.Companion.findType(item.payloadPosition)
                    this.payloadType = PayloadType.findType(item.payloadType)
                }
                basicInfoList.add(pluginBean)
            }
        }
        basicInfoLiveData.postValue(basicInfoList)
        AutelLog.d(TAG, "convertBasicInfo->basicInfoList:${basicInfoList}")
    }

    private fun publishWidgetInfo() {
        mainScope.launch {
            var lastProcessTime = 0L
            AutelLog.d(TAG, "publishWidgetInfo->process start~")
            for (channel in widgetChannel) {
                val currentTime = System.currentTimeMillis()
                //5秒同步一次
                if (currentTime - lastProcessTime > 1000) {
                    AutelLog.d(TAG, "publishWidgetInfo->can publish message")
                    widgetInfoLiveData.postValue(widgetInfoMap)
                    lastProcessTime = currentTime
                }
            }
        }
    }

    /**
     * 添加所有监听器
     */
    private fun addAllPayloadListener() {
        upItemDelegate.addPayloadWidgetInfoListener()
        mainItemDelegate.addPayloadWidgetInfoListener()
        rightItemDelegate.addPayloadWidgetInfoListener()
        externalItemDelegate.addPayloadWidgetInfoListener()
        external2ItemDelegate.addPayloadWidgetInfoListener()
        external3ItemDelegate.addPayloadWidgetInfoListener()
    }

    /**
     * 移除所有监听器
     */
    private fun removeAllPayloadListener() {
        upItemDelegate.removePayloadWidgetInfoListener()
        mainItemDelegate.removePayloadWidgetInfoListener()
        rightItemDelegate.removePayloadWidgetInfoListener()
        externalItemDelegate.removePayloadWidgetInfoListener()
        external2ItemDelegate.removePayloadWidgetInfoListener()
        external3ItemDelegate.removePayloadWidgetInfoListener()
    }

    /**
     * 拉取所有管理器widget信息
     */
    private fun pullAllWidgetInfo(droneDevice: IAutelDroneDevice?) {
        upItemDelegate.pullWidgetInfo(droneDevice)
        mainItemDelegate.pullWidgetInfo(droneDevice)
        rightItemDelegate.pullWidgetInfo(droneDevice)
        externalItemDelegate.pullWidgetInfo(droneDevice)
        external2ItemDelegate.pullWidgetInfo(droneDevice)
        external3ItemDelegate.pullWidgetInfo(droneDevice)
    }

    /**
     * 监听widget信息
     */
    /**
     * 监听widget信息
     */
    private fun registerWidgetInfoCallback() {
        upItemDelegate.setWidgetInfoCallback {
            if (isCurrentControlDrone(it.first)) {
                widgetInfoMap[PayloadIndexType.UP] = it.second
                AutelLog.d(
                    TAG,
                    ">>up PayloadManager receive [drone:${it.first}] widget info:${it.hashCode()}"
                )
                sendElement(widgetChannel, ">>up PayloadManager send widget channel")
            }

        }
        mainItemDelegate.setWidgetInfoCallback {
            if (isCurrentControlDrone(it.first)) {
                widgetInfoMap[PayloadIndexType.LEFT_OR_MAIN] = it.second
                AutelLog.d(
                    TAG,
                    "==main PayloadManager receive [drone:${it.first}] widget info:${it.hashCode()}"
                )
                sendElement(widgetChannel, ">>main PayloadManager send widget channel")
            }

        }
        rightItemDelegate.setWidgetInfoCallback {
            if (isCurrentControlDrone(it.first)) {
                widgetInfoMap[PayloadIndexType.RIGHT] = it.second
                AutelLog.d(
                    TAG,
                    "~~right PayloadManager [drone:${it.first}] receive widget info:${it.hashCode()}"
                )
                sendElement(widgetChannel, ">>right PayloadManager send widget channel")
            }

        }
        externalItemDelegate.setWidgetInfoCallback {
            if (isCurrentControlDrone(it.first)) {
                widgetInfoMap[PayloadIndexType.EXTERNAL] = it.second
                AutelLog.d(
                    TAG,
                    "<<external PayloadManager [drone:${it.first}] receive widget info:${it.hashCode()}"
                )
                sendElement(widgetChannel, ">>external PayloadManager send widget channel")
            }

        }

        external2ItemDelegate.setWidgetInfoCallback {
            if (isCurrentControlDrone(it.first)) {
                widgetInfoMap[PayloadIndexType.EXTERNAL_2] = it.second
                AutelLog.d(
                    TAG,
                    "<<external2 PayloadManager [drone:${it.first}] receive widget info:${it.hashCode()}"
                )
                sendElement(widgetChannel, ">>external2 PayloadManager send widget channel")
            }

        }

        external3ItemDelegate.setWidgetInfoCallback {
            if (isCurrentControlDrone(it.first)) {
                widgetInfoMap[PayloadIndexType.EXTERNAL_3] = it.second
                AutelLog.d(
                    TAG,
                    "<<external3 PayloadManager [drone:${it.first}] receive widget info:${it.hashCode()}"
                )
                sendElement(widgetChannel, ">>external3 PayloadManager send widget channel")
            }

        }

    }

    private fun sendElement(channel: Channel<Long>, msg: String) {
        mainScope.launch {
            try {
                channel.send(System.currentTimeMillis())
//                AutelLog.d(TAG, msg)
            } catch (exception: Exception) {
                exception.printStackTrace()
                AutelLog.e(TAG, "sendElement exception:${exception.message}")
            }
        }
    }

    /**
     * 释放回调
     */
    private fun releaseCallbacks() {
        upItemDelegate.releaseCallbacks()
        mainItemDelegate.releaseCallbacks()
        rightItemDelegate.releaseCallbacks()
        externalItemDelegate.releaseCallbacks()
        external2ItemDelegate.releaseCallbacks()
        external3ItemDelegate.releaseCallbacks()
    }


}