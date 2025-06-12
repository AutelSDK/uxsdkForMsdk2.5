package com.autel.setting.provider.function

import android.os.SystemClock
import android.view.View
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import com.autel.common.constant.AppTagConst.DownFillLightTag
import com.autel.common.delegate.IMainProvider
import com.autel.common.delegate.function.AbsDelegateFunction
import com.autel.common.delegate.function.FunctionType
import com.autel.common.delegate.function.FunctionViewType
import com.autel.common.sdk.business.DroneLightVM
import com.autel.common.utils.DeviceUtils
import com.autel.common.utils.UIUtils
import com.autel.common.widget.toast.AutelToast
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.common.enums.DroneLedStatusEnum
import com.autel.log.AutelLog
import com.autel.setting.R

/**
 * Created by  2023/5/27
 *  下视灯入口实现
 */
class DownFillLightFunctionEntry(mainProvider: IMainProvider) : AbsDelegateFunction(mainProvider) {

    private var curTime = 0L
    private val droneLightVM: DroneLightVM = ViewModelProvider(mainProvider.getMainContext() as ComponentActivity)[DroneLightVM::class.java]

    override fun getFunctionType(): FunctionType {
        return FunctionType.DownFillLight
    }

    override fun getFunctionName(): String {
        return mainProvider.getMainContext().getString(R.string.common_text_down_fill_light)
    }

    override fun getFunctionIconRes(): Int {
        return R.drawable.common_selector_shortcuts_down_fill_light
    }

    override fun functionEnableDependsOnAircraft(): Boolean {
        return true
    }

    override fun onFunctionCreate() {
        super.onFunctionCreate()
        droneLightVM.addObserver()
        droneLightVM.bottomLightLD.observe(mainProvider.getMainLifecycleOwner()) {
            AutelLog.i(DownFillLightTag, "开关 observe:$it")
            refreshBottomLightOn()
        }
        droneLightVM.silenceModeStatusLD.observe(mainProvider.getMainLifecycleOwner()) {
            refreshBottomLightOn()
        }
        droneLightVM.queryAllLedLight()
    }

    override fun functionEnableCondition(): Boolean {
        return super.functionEnableCondition() && droneLightVM.silenceModeStatusLD.value != true
    }

    private fun refreshBottomLightOn() {
        val ledStatusEnum = droneLightVM.bottomLightLD.value
        val silenceModeStatus = droneLightVM.silenceModeStatusLD.value
        when (ledStatusEnum) {
            DroneLedStatusEnum.OPEN -> {
                functionModel.isOn = true
            }

            DroneLedStatusEnum.AUTO, DroneLedStatusEnum.FLASH -> {
                functionModel.isOn = false
            }

            DroneLedStatusEnum.UNKNOWN -> {
                functionModel.isOn = false
            }

            else -> {
                functionModel.isOn = false
            }
        }
        if (silenceModeStatus == true) {
            functionModel.isEnabled = functionEnableCondition()
            functionModel.isOn = false
        } else {
            functionModel.isEnabled = functionEnableCondition()
        }
        mainProvider.getMainHandler().updateFunctionState(functionModel)


    }

    override fun onFunctionDestroy() {
        super.onFunctionDestroy()
        droneLightVM.removeObserver()
    }

    override fun onFunctionStart(viewType: FunctionViewType, view: View) {
        super.onFunctionStart(viewType, view)
        if (SystemClock.elapsedRealtime() - curTime < 500) {
            AutelLog.i(DownFillLightTag, "onFunctionStart return")
            AutelToast.normalToast(mainProvider.getMainContext(), UIUtils.getString(R.string.common_text_operate_too_frequent))
            return
        }

        curTime = SystemClock.elapsedRealtime()
        if (DeviceUtils.allControlDrones().isEmpty()) {
            AutelToast.normalToast(mainProvider.getMainContext(), UIUtils.getString(R.string.common_text_no_drone_controler))
        } else {
            droneLightVM.setLedLightStatus(DroneLedStatusEnum.OPEN, {}, {})
        }
    }

    override fun onFunctionStop(viewType: FunctionViewType, view: View) {
        super.onFunctionStop(viewType, view)
        if (SystemClock.elapsedRealtime() - curTime < 500) {
            AutelLog.i(DownFillLightTag, "onFunctionStop return")
            AutelToast.normalToast(mainProvider.getMainContext(), UIUtils.getString(R.string.common_text_operate_too_frequent))
            return
        }
        functionStop()
    }

    fun functionStop() {
        AutelLog.i(DownFillLightTag, "functionStop")
        curTime = SystemClock.elapsedRealtime()
        if (DeviceUtils.allControlDrones().isEmpty()) {
            AutelToast.normalToast(mainProvider.getMainContext(), UIUtils.getString(R.string.common_text_no_drone_controler))
        } else {
            droneLightVM.setLedLightStatus(DroneLedStatusEnum.AUTO, {}, {})
        }
    }

}