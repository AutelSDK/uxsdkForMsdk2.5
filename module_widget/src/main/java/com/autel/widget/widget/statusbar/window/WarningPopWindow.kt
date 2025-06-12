package com.autel.widget.widget.statusbar.window

import android.content.Context
import android.view.LayoutInflater
import com.autel.common.constant.AppTagConst.WarningTag
import com.autel.widget.R
import com.autel.widget.databinding.FragmentWarningBinding
import com.autel.widget.widget.statusbar.warn.WarningBean
import com.autel.common.manager.MiddlewareManager
import com.autel.common.widget.BasePopWindow
import com.autel.drone.sdk.vmodelx.interfaces.IBaseDevice
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.log.AutelLog
import com.autel.ui.decoration.LineDividerItemDecoration
import com.autel.widget.widget.statusbar.warn.BatteryInstallDialog
import com.autel.widget.widget.statusbar.warn.HArmUnfoldDialog
import com.drakeet.multitype.MultiTypeAdapter

/**
 * @date 2022/9/7.
 * @author maowei
 * @description 告警信息弹框
 */
class WarningPopWindow(context: Context) : BasePopWindow(context) {

    private var currentWarnBeanList = ArrayList<WarningBean>()
    private val binding = FragmentWarningBinding.inflate(LayoutInflater.from(context))
    private lateinit var adapter: MultiTypeAdapter

    init {
        contentView = binding.root
        initView()
    }

    private fun initView() {
        adapter = MultiTypeAdapter().apply {
            register(WarnItemViewBinder(::onItemClick))
            register(WarnDeviceNameViewBinder())
        }
        binding.warnListV.addItemDecoration(
            LineDividerItemDecoration(
                context.resources.getDimensionPixelSize(R.dimen.common_3dp),
                context.getColor(R.color.common_color_white_10)
            )
        )
        binding.warnListV.adapter = adapter
    }

    fun updateWarnData(warnBeanList: ArrayList<WarningBean>) {
        currentWarnBeanList = warnBeanList
        val tmpArray = mutableListOf<Any>()
        val droneSize = DeviceManager.getDeviceManager().getDroneDevices().filter { it.isConnected() }.size
        var showRemoteName = false
        warnBeanList.find {
            it.deviceId == DeviceManager.getDeviceManager().getLocalRemoteDevice().getDeviceNumber()
        }?.let {
            showRemoteName = true
        }
        var isFirst = true
        warnBeanList.forEach { warningBean ->
            val deviceName = warningBean.deviceName
            val deviceId = warningBean.deviceId
            val droneDevice = DeviceManager.getDeviceManager().getDroneDeviceById(deviceId)
            if (droneDevice != null) {
                if (droneDevice.isConnected()) {
                    val added = addDeviceName(droneDevice, tmpArray, droneSize, showRemoteName, isFirst)
                    if (added) { isFirst = false }
                }
                tmpArray.add(warningBean)
            } else {
                if (DeviceManager.getDeviceManager().getLocalRemoteDevice().getDeviceNumber() == deviceId) {
                    if (deviceName != null) {
                        val added = addDeviceName(DeviceManager.getDeviceManager().getLocalRemoteDevice(), tmpArray, droneSize, showRemoteName, isFirst)
                        if (added) { isFirst = false }
                    }
                }
                tmpArray.add(warningBean)
            }
        }
        val noDialogWarnBeanList = ArrayList<Any>()
        noDialogWarnBeanList.addAll(tmpArray)
        val iterator = noDialogWarnBeanList.iterator()
        while (iterator.hasNext()) {
            val warningBean = iterator.next()
            if (warningBean is WarningBean) {
                if (warningBean.tip is WarningBean.TipType.TipToast) {
                    iterator.remove()
                }
            }
        }
        if (adapter.items != noDialogWarnBeanList) {
            AutelLog.i(WarningTag, "WarningPopWindow updateWarnData: $noDialogWarnBeanList")
        }
        adapter.items = noDialogWarnBeanList
        adapter.notifyDataSetChanged()
    }

    private fun addDeviceName(device: IBaseDevice?, tmpArray: MutableList<Any>, droneSize: Int, showRemoteName: Boolean, isFirst: Boolean): Boolean {
        if (device != null) {//大于1个飞机或则一个遥控器告警加一个飞机告警时
            var containDeviceName = false
            tmpArray.forEach {
                if (it is HashMap<*, *>) {
                    if (it.containsKey(device)) {
                        containDeviceName = true
                        return@forEach
                    }
                }
            }
            if (!containDeviceName && ((droneSize > 1) || showRemoteName)) {
                val map = hashMapOf<IBaseDevice, Boolean>()
                map[device] = isFirst
                tmpArray.add(map)
                return true
            }
        }
        return false
    }

    fun canShow(): Boolean {
        return adapter.items.isNotEmpty()
    }

    private fun onItemClick(warningBean: WarningBean, action: WarningBean.Action) {
        val deviceId = warningBean.deviceId
        val droneDevice = DeviceManager.getDeviceManager().getDroneDeviceById(deviceId)
        AutelLog.i(WarningTag, "onItemClick: action: $action, deviceId:$deviceId, droneDevice: $droneDevice")
        when (action) {
            WarningBean.Action.COMPASS_CALI -> {
                droneDevice?.let { MiddlewareManager.settingModule.jumpCompassCalibration(context, it) }
            }
            WarningBean.Action.IMU_CALI -> {
                droneDevice?.let { MiddlewareManager.settingModule.jumpIMUCalibration(context, it) }
            }
            WarningBean.Action.RC_CALI -> {
                MiddlewareManager.settingModule.jumpRemoteCalibration(context, droneDevice)
            }
            WarningBean.Action.CONNECTING_AIRCRAFT -> {
                dismiss()
                MiddlewareManager.settingModule.startRemoteControlMatch(context)
            }
            WarningBean.Action.RID_MSG -> {
                // 跳转到RID设置界面
                MiddlewareManager.settingModule.jumpRemoteIdSetting(context)
                dismiss()
            }
            WarningBean.Action.RC_COMPASS_CALL -> {
                MiddlewareManager.settingModule.jumpRemoteCompassCalibration(context)
                dismiss()
            }
            WarningBean.Action.UOM -> {
                // 跳转到UOM设置界面
//                MiddlewareManager.guideModule.jumpUOMActivity(context,false, deviceId, "CN")
                dismiss()
            }
            WarningBean.Action.ACTIVATE_DRONE -> {
//                MiddlewareManager.guideModule.activateDrone(deviceId)
                dismiss()
//                AutelToast.normalToast(context, R.string.common_text_activating)
            }
            WarningBean.Action.SHOW_ARM_UNFOLD_DIALOG -> {
                HArmUnfoldDialog(context).show()
            }

            WarningBean.Action.SHOW_BATTERY_INSTALL_DIALOG -> {
                BatteryInstallDialog(context).show()
            }
            else -> {}
        }
    }

    fun updateWarnData() {
        updateWarnData(currentWarnBeanList)
    }
}
