package com.autel.ux.widget.remotecontrolpower

import com.autel.common.feature.phone.PhoneBatteryManager
import com.autel.ux.core.base.AutelSDKModel
import com.autel.ux.core.base.WidgetModel
import com.autel.ux.core.utils.DataProcessor

class RemoteControlPowerModel(private val sdkModel: AutelSDKModel) : WidgetModel(sdkModel) {

    private val remoteControlPowerProcessor = DataProcessor.create(100)
    private val remoteControlTotalPowerProcessor = DataProcessor.create(100)

    val remoteControlPower = remoteControlPowerProcessor.toFlow()
    val remoteControlTotalPower = remoteControlTotalPowerProcessor.toFlow()

    /**
     * 遥控器电池电量
     * */
    private val mRcBatteryListener = PhoneBatteryManager.BatteryChangeListener { current, total ->
        remoteControlPowerProcessor.emit(current)
        remoteControlTotalPowerProcessor.emit(total)
    }

    override fun inSetupWithRemoteControl() {
        PhoneBatteryManager.sPhoneBatteryManager.addBatteryChangeListener(mRcBatteryListener)
    }

    override fun inSetupWithDrone() {
    }

    override fun inCleanup() {
        PhoneBatteryManager.sPhoneBatteryManager.removeBatteryChangeListener(mRcBatteryListener)
    }
}