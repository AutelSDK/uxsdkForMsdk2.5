package com.autel.widget.widget.remotestatusbar

import com.autel.common.base.widget.BaseWidgetModel
import com.autel.common.feature.phone.PhoneBatteryManager
import com.autel.common.sdk.GpsSignalLevelEnum
import com.autel.common.sdk.RemoteSignalLevelEnum
import com.autel.common.utils.DeviceUtils
import com.autel.widget.widget.statusbar.bean.RemoteBattery
import com.autel.widget.widget.statusbar.bean.SignalStrength
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Created by  2023/9/7
 */
class RemoteStatusWidgetModel : BaseWidgetModel() {

    private var remoteCurrentBattery: Int = 0
    private var remoteTotalBattery: Int = 0

    val remoteBattery = MutableStateFlow(RemoteBattery(100, 100))

    /**
     * 信号强度信息
     */
    val signalStrength = MutableStateFlow(SignalStrength(GpsSignalLevelEnum.GPS_SIGNAL_LEVEL_NONE, 0, RemoteSignalLevelEnum.REMOTE_SIGNAL_LEVEL_NONE))

    /**
     * 飞机状态改变上报
     */
    val droneConnectStatus: MutableStateFlow<Boolean?> = MutableStateFlow(null)

    /**
     * 遥控器电池电量
     * */
    private val mRcBatteryListener = PhoneBatteryManager.BatteryChangeListener { current, total ->
        remoteCurrentBattery = current
        remoteTotalBattery = total
    }


    override fun fixedFrequencyRefresh() {
        val remoteCacheData = DeviceUtils.getLocalRemoteDevice().getDeviceStateData()
        val remoteModel = RemoteBattery(remoteCurrentBattery, remoteTotalBattery)
        if (remoteBattery.value != remoteModel) {
            remoteBattery.value = remoteModel
        }
        val signalModel = SignalStrength(
            GpsSignalLevelEnum.GPS_SIGNAL_LEVEL_NONE,
            0,
            RemoteSignalLevelEnum.parseValue(remoteCacheData.rcStateNtfyBean.rcSignalQuality)
        )
        if (signalStrength.value != signalModel) {
            signalStrength.value = signalModel
        }
        val hasDroneConnected = DeviceUtils.hasDroneConnected()
        if (droneConnectStatus.value != hasDroneConnected) {
            droneConnectStatus.value = hasDroneConnected
        }
    }


    override fun setup() {
        super.setup()
        PhoneBatteryManager.sPhoneBatteryManager.addBatteryChangeListener(mRcBatteryListener)
    }

    override fun cleanup() {
        super.cleanup()
        PhoneBatteryManager.sPhoneBatteryManager.removeBatteryChangeListener(mRcBatteryListener)
    }
}