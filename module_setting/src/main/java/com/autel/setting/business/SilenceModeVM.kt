package com.autel.setting.business

import androidx.lifecycle.MutableLiveData
import com.autel.common.base.BaseViewModel
import com.autel.common.constant.AppTagConst
import com.autel.common.extension.asLiveData
import com.autel.common.utils.BusinessType
import com.autel.common.utils.DeviceUtils
import com.autel.drone.sdk.libbase.error.IAutelCode
import com.autel.drone.sdk.vmodelx.device.IAutelDroneListener
import com.autel.drone.sdk.vmodelx.interfaces.*
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.drone.sdk.vmodelx.manager.data.ControlMode
import com.autel.drone.sdk.vmodelx.manager.keyvalue.callback.CommonCallbacks
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.FlightPropertyKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.base.KeyTools
import com.autel.log.AutelLog
import kotlinx.coroutines.*

class SilenceModeVM: BaseViewModel(), IAutelDroneListener {
    /**
     * 下视LED灯光
     */
    private val _silenceModeStatusLD = MutableLiveData<Boolean>()
    val silenceModeStatusLD = _silenceModeStatusLD.asLiveData()

    private val silenceModeStatuslist = mutableMapOf<Int, Boolean>()

    private val  controlDroneListener = object : IControlDroneListener {
        override fun onControlChange(mode: ControlMode, droneList: List<IAutelDroneDevice>) {
            MainScope().launch {
                AutelLog.i(AppTagConst.SilenceModeTag,"显示静默模式状态5")
                _silenceModeStatusLD.value = false
            }
            updateSilenceStatus()
        }
    }
    fun addObserver() {
        if (DeviceUtils.isBusinessTypeValid(BusinessType.NETMESH)) {
            DeviceManager.getMultiDeviceOperator().addControlChangeListener(controlDroneListener)
        }
        DeviceManager.getDeviceManager().addDroneListener(this)
    }

    fun removeObserver() {
        if (DeviceUtils.isBusinessTypeValid(BusinessType.NETMESH)) {
            DeviceManager.getMultiDeviceOperator().removeControlChangeListener(controlDroneListener)
        }
        DeviceManager.getDeviceManager().removeDroneListener(this)
    }

    /**
     * 切换受控飞机的静默模式
     */
    fun switchSilenceModeStatus() {
        val drones = DeviceUtils.allControlDrones()
        if (drones?.isNotEmpty() == true) {
            val enum = when (currentLightStatus(drones)) {
                true -> false
                else -> {
                    true
                }
            }
            var successCount = 0
            var failCount = 0
            drones.forEach {
                AutelLog.i(AppTagConst.SilenceModeTag,"switchSilenceModeStatus ${it.getDeviceNumber()} ${DeviceUtils.droneDeviceName(it)} $enum")
                setLedLightStatus(it,
                    enum,
                    {
                        successCount ++
                        if (successCount + failCount == drones.size) {
                            MainScope().launch {
                                if (successCount == drones.size) {//全部成功
                                    AutelLog.i(AppTagConst.SilenceModeTag,"显示静默模式状态6 $enum")
                                    _silenceModeStatusLD.value = enum
                                } else if (successCount > 0) {//部分成功
                                    AutelLog.i(AppTagConst.SilenceModeTag,"显示静默模式状态7 $enum")
                                    _silenceModeStatusLD.value = enum
                                } else {//全部失败
                                    AutelLog.i(AppTagConst.SilenceModeTag,"显示静默模式状态8 $enum")
                                    _silenceModeStatusLD.value = !enum
                                }
                            }
                        }
                    },
                    {
                        failCount ++
                        if (successCount + failCount == drones.size) {
                            MainScope().launch {
                                if (successCount == drones.size) {//全部成功
                                    AutelLog.i(AppTagConst.SilenceModeTag,"显示静默模式状态9 $enum")
                                    _silenceModeStatusLD.value = enum
                                } else if (successCount > 0) {//部分成功
                                    AutelLog.i(AppTagConst.SilenceModeTag,"显示静默模式状态10 $enum")
                                    _silenceModeStatusLD.value = enum
                                } else {//全部失败
                                    AutelLog.i(AppTagConst.SilenceModeTag,"显示静默模式状态11 $enum")
                                    _silenceModeStatusLD.value = !enum
                                }
                            }
                        }
                    })
            }
        } else {
            AutelLog.i(AppTagConst.SilenceModeTag,"switchSilenceModeStatus 受控飞机为空")
        }
    }

    /**
     * 获取当前受控飞机的静默模式
     */
    private fun currentLightStatus(list: List<IAutelDroneDevice>): Boolean {
        var silenceModeStatus = false
        list.forEach {
            if (silenceModeStatuslist[it.getDeviceNumber()] == true) {
                silenceModeStatus = true
            }
        }
        return silenceModeStatus
    }

    /**
     * 设置静默模式状态
     */
    private fun setLedLightStatus(drone: IAutelDroneDevice, enum: Boolean, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val key = KeyTools.createKey(FlightPropertyKey.KeySilentModeStatus)
        val keyManager = drone?.getKeyManager()
        if (keyManager == null) {
            AutelLog.i(AppTagConst.SilenceModeTag,"设置静默模式 失败 "+ drone.getDeviceNumber() + DeviceUtils.droneDeviceName(drone) +" keyManager is null")
            onError("keyManager is null")
            return
        }
        keyManager?.let {
                it.setValue(
                    key,
                    enum,
                    object : CommonCallbacks.CompletionCallback {
                        override fun onSuccess() {
                        AutelLog.i(AppTagConst.SilenceModeTag,"设置静默模式 成功 "+ drone.getDeviceNumber() + DeviceUtils.droneDeviceName(drone) + enum)
                        synchronized(silenceModeStatuslist) {
                            silenceModeStatuslist[drone.getDeviceNumber()] = enum
                        }
                        onSuccess()
                    }

                        override fun onFailure(code: IAutelCode, msg: String?) {
                        AutelLog.i(AppTagConst.SilenceModeTag,"设置静默模式 失败 "+ drone.getDeviceNumber() + DeviceUtils.droneDeviceName(drone) +" reason " + code + msg)
                        onError("OnFailure "+ drone.getDeviceNumber() +" reason "+ msg)
                    } })
            }
    }
    /**根据静默状态缓存通知界面更新UI
     * 如果有受控飞机：显示受控飞机的静默状态
     * 如果没有受控飞机：显示所有飞机的静默状态
     * */
    private fun updateSilenceStatus() {
        val controledDrones = DeviceUtils.allControlDrones()
        if (controledDrones?.isNotEmpty() == true) {
            var ledStatus = false
            controledDrones?.forEach {
                if (silenceModeStatuslist[it.getDeviceNumber()] == true) {
                    AutelLog.i(AppTagConst.SilenceModeTag,"${DeviceUtils.droneDeviceName(it)} 静默模式已开启")
                    ledStatus = true
                }
            }
            MainScope().launch {
                AutelLog.i(AppTagConst.SilenceModeTag,"显示静默模式状态1 $ledStatus")
                _silenceModeStatusLD.value = ledStatus
            }

        } else {
            if (DeviceManager.getDeviceManager().getDroneDevices().isNotEmpty()) {
                MainScope().launch {
                    val status = silenceModeStatuslist.values.contains(true)
                    AutelLog.i(AppTagConst.SilenceModeTag,"显示静默模式状态2 $status")
                    _silenceModeStatusLD.value = status
                }
            } else {
                MainScope().launch {
                    synchronized(silenceModeStatuslist) {
                        silenceModeStatuslist.clear()
                    }
                    AutelLog.i(AppTagConst.SilenceModeTag,"显示静默模式状态3")
                    _silenceModeStatusLD.value = false
                }
            }
        }
    }

    private fun querySilenceMode(drone: IAutelDroneDevice, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val key = KeyTools.createKey(FlightPropertyKey.KeySilentModeStatus)
        val keyManager = drone?.getKeyManager()
        if (keyManager == null) {
            AutelLog.i(AppTagConst.SilenceModeTag,"查询静默模式 失败 "+ drone.getDeviceNumber() + DeviceUtils.droneDeviceName(drone) +" keyManager is null")
            onError("keyManager is null")
            return
        }
        keyManager?.let {
            AutelLog.i(AppTagConst.SilenceModeTag,"查询静默模式 "+ drone.getDeviceNumber() + DeviceUtils.droneDeviceName(drone))
            it.getValue(key,object : CommonCallbacks.CompletionCallbackWithParam<Boolean> {
                override fun onSuccess(t: Boolean?) {
                    if (t != null) {
                        AutelLog.i(AppTagConst.SilenceModeTag,"查询静默模式 成功 "+ drone.getDeviceNumber() + DeviceUtils.droneDeviceName(drone) + t)
                        synchronized(silenceModeStatuslist) {
                            silenceModeStatuslist[drone.getDeviceNumber()] = t
                        }
                        updateSilenceStatus()
                    }
                    onSuccess()
                }

                override fun onFailure(error: IAutelCode, msg: String?) {
                    AutelLog.i(AppTagConst.SilenceModeTag,"查询静默模式 失败 "+ drone.getDeviceNumber() + DeviceUtils.droneDeviceName(drone) +" reason " + error + msg)
                    onError("OnFailure "+ drone.getDeviceNumber() + DeviceUtils.droneDeviceName(drone) + " reason "+ msg)
                }
            })
        }
    }

    fun queryAllSilenceMode() {
        DeviceManager.getDeviceManager().getDroneDevices().forEach {
            querySilenceMode(it,{

            },{

            })
        }
    }

    override fun onDroneChangedListener(connected: Boolean, drone: IAutelDroneDevice) {
        if (connected) {

        } else {
            val netmeshDroneDeviceListIterator = silenceModeStatuslist.iterator()
            while (netmeshDroneDeviceListIterator.hasNext()) {
                val device = netmeshDroneDeviceListIterator.next()
                if (device.key == drone.getDeviceNumber()) {
                    synchronized(this) {
                        netmeshDroneDeviceListIterator.remove() // 使用迭代器的 remove() 方法
                    }
                }
            }
            MainScope().launch {
                AutelLog.i(AppTagConst.SilenceModeTag,"显示静默模式状态4")
                _silenceModeStatusLD.value = false
            }
        }
    }

    override fun onMainServiceValid(valid: Boolean, drone: IAutelDroneDevice) {
        if (valid) querySilenceMode(drone,{}, {})
    }

}