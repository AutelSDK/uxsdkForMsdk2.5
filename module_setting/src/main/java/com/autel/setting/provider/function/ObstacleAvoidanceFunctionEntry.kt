package com.autel.setting.provider.function

import android.os.SystemClock
import android.view.View
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import com.autel.common.constant.AppTagConst
import com.autel.common.delegate.IMainProvider
import com.autel.common.delegate.function.AbsDelegateFunction
import com.autel.common.delegate.function.FunctionType
import com.autel.common.delegate.function.FunctionViewType
import com.autel.common.sdk.business.SettingObstacleAvoidanceVM
import com.autel.common.utils.DeviceUtils
import com.autel.common.widget.toast.AutelToast
import com.autel.drone.sdk.vmodelx.device.IAutelDroneListener
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.log.AutelLog
import com.autel.setting.R

/**
 * 工具栏：绕障
 * */
class ObstacleAvoidanceFunctionEntry(mainProvider: IMainProvider) : AbsDelegateFunction(mainProvider) {
    private val settingObstacleAvoidanceVM: SettingObstacleAvoidanceVM =
        ViewModelProvider(mainProvider.getMainContext() as ComponentActivity)[SettingObstacleAvoidanceVM::class.java]
    private var curTime = 0L
//    private var obstacleAvoidanceVM = ViewModelProvider(mainProvider.getMainContext() as ComponentActivity)[ObstacleAvoidanceVM::class.java]


    override fun getFunctionType(): FunctionType {
        return FunctionType.Detour
    }

    override fun getFunctionName(): String {
        return mainProvider.getMainContext().getString(R.string.common_text_avoid_obstacles)
    }

    override fun getFunctionIconRes(): Int {
        return R.drawable.common_selector_shortcuts_navigation_light
    }

    override fun functionEnableDependsOnAircraft(): Boolean {
        return true
    }

    private val droneListener = object : IAutelDroneListener {
        override fun onDroneChangedListener(connected: Boolean, drone: IAutelDroneDevice) {
            super.onDroneChangedListener(connected, drone)
            if (connected) {
                AutelLog.i(AppTagConst.ObstacleAvoidanceTag, "onDroneChangedListener getObstacleAvoidance drone:${drone.getDeviceNumber()}")
                settingObstacleAvoidanceVM.getObstacleAvoidance(drone, {
                    AutelLog.i(
                        AppTagConst.ObstacleAvoidanceTag,
                        "onDroneChangedListener getObstacleAvoidance success drone:${drone.getDeviceNumber()}"
                    )
                }, {
                    AutelLog.e(
                        AppTagConst.ObstacleAvoidanceTag,
                        "onDroneChangedListener getObstacleAvoidance fail drone:${drone.getDeviceNumber()}"
                    )
                })
            }
        }
    }

    override fun onFunctionCreate() {
        super.onFunctionCreate()

        DeviceManager.getDeviceManager().addDroneListener(droneListener)
        val drones = DeviceUtils.allOnlineDrones()
        AutelLog.i(AppTagConst.ObstacleAvoidanceTag, "onFunctionCreate getObstacleAvoidance drones:${drones}")
        drones.forEach { droneDevice ->
            AutelLog.i(AppTagConst.ObstacleAvoidanceTag, "onFunctionCreate getObstacleAvoidance droneDevice:${droneDevice.getDeviceNumber()}")
            settingObstacleAvoidanceVM.getObstacleAvoidance(droneDevice, {}, {})
        }
    }

    override fun onFunctionDestroy() {
        super.onFunctionDestroy()
        DeviceManager.getDeviceManager().removeDroneListener(droneListener)
    }

    override fun timerRefresh(): Boolean {
        return true
    }

    override fun fixedFrequencyRefresh() {
        super.fixedFrequencyRefresh()

        var changed = false
        val isEnabled = DeviceUtils.allControlDrones().any { it.isConnected() }
        val isOn = settingObstacleAvoidanceVM.obstacleAvoidance()
        if (functionModel.isEnabled != isEnabled) {
            functionModel.isEnabled = isEnabled
            changed = true
        }
        if (functionModel.isOn != isOn) {
            functionModel.isOn = isOn
            changed = true
        }
        if (changed) {
            AutelLog.i(AppTagConst.ObstacleAvoidanceTag, "isOn:$isOn isEnabled:$isEnabled")
            mainProvider.getMainHandler().updateFunctionState(functionModel)
        }
    }

    override fun onFunctionStart(viewType: FunctionViewType, view: View) {
        super.onFunctionStart(viewType, view)
        if (SystemClock.elapsedRealtime() - curTime < 500) {
            AutelLog.i(AppTagConst.ObstacleAvoidanceTag, "onFunctionStart return")
            AutelToast.normalToast(mainProvider.getMainContext(), R.string.common_text_operate_too_frequent)
            return
        }

        curTime = SystemClock.elapsedRealtime()
        val hasControlledDroneConnected = DeviceUtils.allControlDrones().any { it.isConnected() }
        if (!hasControlledDroneConnected) {
            AutelToast.normalToast(mainProvider.getMainContext(), R.string.common_text_no_drone_controler)
        } else {
            DeviceUtils.allControlDrones().forEach { droneDevice ->
                settingObstacleAvoidanceVM.setObstacleAvoidance(droneDevice, true, {}, {})
            }
        }
    }

    override fun onFunctionStop(viewType: FunctionViewType, view: View) {
        super.onFunctionStop(viewType, view)
        if (SystemClock.elapsedRealtime() - curTime < 500) {
            AutelLog.i(AppTagConst.ObstacleAvoidanceTag, "onFunctionStop return")
            AutelToast.normalToast(mainProvider.getMainContext(), R.string.common_text_operate_too_frequent)
            return
        }
        curTime = SystemClock.elapsedRealtime()
        val drones = DeviceUtils.allControlDrones()
        if (drones.isEmpty()) {
            AutelToast.normalToast(mainProvider.getMainContext(), R.string.common_text_no_drone_controler)
        } else {
            DeviceUtils.allControlDrones().forEach { droneDevice ->
                settingObstacleAvoidanceVM.setObstacleAvoidance(droneDevice, false, {}, {})
            }
        }
    }
}