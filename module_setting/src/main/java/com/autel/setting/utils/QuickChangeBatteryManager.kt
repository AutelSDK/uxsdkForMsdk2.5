package com.autel.setting.utils

import android.os.SystemClock
import androidx.fragment.app.FragmentActivity
import com.autel.common.base.AppActivityManager
import com.autel.common.constant.AppTagConst.QuickChangeBatteryTag
import com.autel.common.manager.AppInfoManager
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.CommonKey.KeyDroneWorkStatusInfoReport
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.base.KeyTools
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.bean.FlightControlStatusInfo
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.enums.BatteryInPlaceEnum
import com.autel.log.AutelLog
import com.autel.setting.dialog.QuickChangeBatteryDialog

/**
 * @author 
 * @date 2023/2/9
 * 快速换电池管理类
 */
object QuickChangeBatteryManager {

    private val TIME_LIMIT = 30_000L
    private var isAdd = false
    private const val QuickChangeBatteryDialogTag = "QuickChangeBatteryDialog"
    private var showTimes = mutableMapOf<Int, Long>()
    private var isShowDialogs = mutableMapOf<Int, Boolean>()

    private val droneWorkStatusInfoReportKey = KeyTools.createKey(KeyDroneWorkStatusInfoReport)

    //换电池监听
    private val droneWorkStatusInfoReport = object : DeviceManager.KeyManagerListenerCallBack {
        override fun onListenerValueChanged(value: DeviceManager.DeviceListenerResult<*>) {
            (value.result as? FlightControlStatusInfo)?.let {
                if (AppInfoManager.isOnlyFactoryTest()) return@let

                if (it.batteryNotInPlaceFlag != BatteryInPlaceEnum.READY) {
                    val isFlying = value.drone.getDeviceStateData().flightControlData.flightMode.isFlying()
                    showDialog(value.drone.getDeviceNumber(),isFlying)
                } else {
                    dismissDialog(value.drone.getDeviceNumber())
                }
            }
        }
    }

    /**
     * 添加换电监听
     */
    fun addListener() {
        if (isAdd) return
        AutelLog.i(QuickChangeBatteryTag, "addListener")
        isAdd = true
        DeviceManager.getDeviceManager().addDroneDevicesListener(droneWorkStatusInfoReportKey, droneWorkStatusInfoReport)
    }

    /**
     * 移除换电监听
     * */
    fun removeListener() {
        if (!isAdd) return
        AutelLog.i(QuickChangeBatteryTag, "removeListener")
        isAdd = false
        DeviceManager.getDeviceManager().removeDroneDevicesListener(droneWorkStatusInfoReportKey, droneWorkStatusInfoReport)
    }

    private fun dismissDialog(deviceId: Int) {
        val isShowDialog = isShowDialogs[deviceId] ?: false
        if (!isShowDialog) return
        ((AppActivityManager.INSTANCE.getCurrentActivity()) as? FragmentActivity)?.let {
            (it.supportFragmentManager.findFragmentByTag(QuickChangeBatteryDialogTag) as? QuickChangeBatteryDialog)?.let {
                AutelLog.i(QuickChangeBatteryTag, "dismissDialog -> $deviceId 消失弹框")
                it.dismiss()
                isShowDialogs[deviceId] = false
            }
        }
    }

     fun showDialog(deviceId: Int,isFlying: Boolean) {
        if (AppInfoManager.isOnOTA()) return
        val isShowDialog = isShowDialogs[deviceId] ?: false
        if (isShowDialog) {
            AutelLog.i(QuickChangeBatteryTag, "showDialog -> $deviceId 已弹框，不需要再弹")
            return
        }

        var showTime = showTimes[deviceId] ?: 0L
        if (SystemClock.elapsedRealtime() - showTime < TIME_LIMIT) {
            AutelLog.i(QuickChangeBatteryTag, "showDialog -> $TIME_LIMIT s 内 $deviceId 不重复弹框")
            return
        }
        showTime = SystemClock.elapsedRealtime()
        showTimes[deviceId] = showTime
        isShowDialogs[deviceId] = true
        ((AppActivityManager.INSTANCE.getCurrentActivity()) as? FragmentActivity)?.let {
            val quickChangeBatteryDialog = QuickChangeBatteryDialog(dialogShow = {
                isShowDialogs[deviceId] = it
            },isFlying)
            quickChangeBatteryDialog.let { dialog ->
                AutelLog.i(QuickChangeBatteryTag, "showDialog -> $deviceId 显示快速换电弹框 isFlying=$isFlying")
                it.supportFragmentManager.beginTransaction().add(dialog, QuickChangeBatteryDialogTag).commitAllowingStateLoss()
            }
        }
    }

}