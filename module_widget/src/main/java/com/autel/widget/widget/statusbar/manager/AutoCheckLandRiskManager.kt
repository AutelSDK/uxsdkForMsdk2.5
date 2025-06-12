package com.autel.widget.widget.statusbar.manager

import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.autel.common.R
import com.autel.common.base.AppActivityManager
import com.autel.common.constant.AppTagConst
import com.autel.common.sdk.service.SettingService
import com.autel.common.utils.DeviceUtils
import com.autel.common.utils.UIUtils
import com.autel.common.widget.dialog.CommonTwoButtonDialog
import com.autel.common.widget.toast.AutelToast
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.CommonKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.base.KeyTools
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.bean.DroneSystemStateLFNtfyBean
import com.autel.log.AutelLog

object AutoCheckLandRiskManager {
    // 是否正在展示降落保护提示
    private var isShowLandRiskMap = mutableMapOf<Int, Boolean>()

    //用于记录上一次收到的上报状态，比较前后两次收到的状态是否发生改变，改变才通知业务进行处理
    private var landRiskStatusMap = mutableMapOf<Int, Boolean>()

    /**
     * 是否正在展示弹窗
     * */
    private fun isShowLandRisk(deviceId: Int): Boolean {
        val show = isShowLandRiskMap[deviceId]
        if (show != null) {
            return show
        }
        AutelLog.i(AppTagConst.LandProtectionTag, "获取是否展示降落保护提示:$deviceId 展示过：默认值false")
        return false
    }

    fun updateIsShowLandRisk(deviceId: Int, show: Boolean) {
        if (isShowLandRiskMap[deviceId] != show) {
            AutelLog.i(AppTagConst.LandProtectionTag, "是否展示降落保护提示:$deviceId 更新为展示过:$show")
            isShowLandRiskMap[deviceId] = show
        }
    }

    private val landRiskStatusListener = object : DeviceManager.KeyManagerListenerCallBack {
        override fun onListenerValueChanged(value: DeviceManager.DeviceListenerResult<*>) {
            if (DeviceUtils.isMainRC()) {//主遥控器才展示降落保护提示
                if (DeviceUtils.isNetMeshing()) {//组网中不展示降落保护提示
                    return
                }
                val device = value.drone
                val deviceId = device.getDeviceNumber()
                (value.result as? DroneSystemStateLFNtfyBean)?.let {
                    val currentShowStatus = it.landIsRisky
                    if (currentShowStatus) {
                        AutelLog.i(AppTagConst.LandProtectionTag, "飞机降落风险上报 landIsRisky:$currentShowStatus device:$deviceId")
                    }
                    val lastShowStatus = landRiskStatusMap[deviceId]
                    val changed = lastShowStatus != currentShowStatus
                    landRiskStatusMap[deviceId] = currentShowStatus
                    //前后两次上报是否有变化，有变化才进入弹窗逻辑
                    if (changed) {
                        AutelLog.i(AppTagConst.LandProtectionTag, "飞机降落风险上报 changed:$changed currentShowStatus:$currentShowStatus")
                        if (currentShowStatus) {
                            val isShow = isShowLandRisk(deviceId)
                            AutelLog.i(AppTagConst.LandProtectionTag, "是否正在展示降落保护提示，展示中：$isShow")
                            if (!isShow) {
                                showDialog(device)
                            }
                        } else {
                            updateIsShowLandRisk(deviceId, false)
                        }
                    }
                }
            }
        }
    }

    /**
     * 添加飞机监听
     * */
    fun addListener() {
        AutelLog.i(AppTagConst.LandProtectionTag, "addListener")
        val key = KeyTools.createKey(CommonKey.KeyDroneSystemStatusLFNtfy)
        DeviceManager.getDeviceManager().addDroneDevicesListener(key, landRiskStatusListener)
    }

    /**
     * 移除飞机监听
     * */
    fun removeListener() {
        val key = KeyTools.createKey(CommonKey.KeyDroneSystemStatusLFNtfy)
        DeviceManager.getDeviceManager().removeDroneDevicesListener(key, landRiskStatusListener)
    }

    /**
     * 显示降落保护提示
     * */
    private fun showDialog(droneDevice: IAutelDroneDevice) {
        AutelLog.i(AppTagConst.LandProtectionTag, "showDialog")
        if (isShowDialog(droneDevice.getDeviceNumber())) return
        ((AppActivityManager.INSTANCE.getCurrentActivity()) as? FragmentActivity)?.let {
            updateIsShowLandRisk(droneDevice.getDeviceNumber(), true)
            CommonTwoButtonDialog(it).apply {
                if (DeviceUtils.allDrones().size > 1) {
                    val deviceName = droneDevice.getDeviceInfoBean()?.deviceName
                    if (deviceName?.isNotEmpty() == true) {
                        setTitle(it.getString(R.string.common_text_land_risk_title, deviceName))
                    }
                }
                setMessage(it.getString(R.string.common_text_land_tips_content))
                setLeftBtnStr(UIUtils.getString(R.string.common_text_cancel))
                setRightBtnStr(UIUtils.getString(R.string.common_text_confirm))
                setLeftBtnListener {
                    AutelLog.i(AppTagConst.LandProtectionTag, "点击取消:${droneDevice.getDeviceNumber()}")
                    dealConfirm(it, droneDevice, false) {
                        updateIsShowLandRisk(droneDevice.getDeviceNumber(), false)
                    }
                }
                setRightBtnListener {
                    AutelLog.i(AppTagConst.LandProtectionTag, "点击确认:${droneDevice.getDeviceNumber()}")
                    dealConfirm(it, droneDevice, true) {
                        updateIsShowLandRisk(droneDevice.getDeviceNumber(), false)
                    }
                }
            }.show()

            AutelLog.i(AppTagConst.LandProtectionTag, "showDialog 弹框成功")
        }
    }

    /**
     * 从缓存中读取是否已展示弹窗的数据
     * */
    private fun isShowDialog(deviceId: Int): Boolean {
        if (isShowLandRiskMap.containsKey(deviceId)) {
            return isShowLandRiskMap[deviceId] ?: false
        }
        return false
    }

    /**
     * 处理确认降落
     * @param ignoreRisks true : 忽略风险,强制降落
     *                    false : 取消降落行为
     */
    private fun dealConfirm(context: Context, droneDevice: IAutelDroneDevice, ignoreRisks: Boolean, successBlock: (() -> Unit)? = null) {
        AutelLog.i(AppTagConst.LandProtectionTag, "确认降落:${DeviceUtils.droneDeviceName(droneDevice)} , ignoreRisks:$ignoreRisks")
        SettingService.getInstance().flightParamService.setIgnoreRiskLand(droneDevice, ignoreRisks, {
            AutelLog.i(AppTagConst.LandProtectionTag, "设置是否强制降落 {$ignoreRisks} 成功")
            successBlock?.invoke()
        }, {
            AutelLog.e(AppTagConst.LandProtectionTag, "$it")
            AutelToast.normalToast(context, context.getString(R.string.common_text_set_failed))
        })
    }
}