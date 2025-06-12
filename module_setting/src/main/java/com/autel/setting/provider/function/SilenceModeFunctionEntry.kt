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
import com.autel.common.listener.DisclaimerListener
import com.autel.common.manager.CommonDialogManager
import com.autel.common.sdk.business.DroneLightVM
import com.autel.common.utils.DeviceUtils
import com.autel.common.widget.toast.AutelToast
import com.autel.log.AutelLog
import com.autel.setting.R

class SilenceModeFunctionEntry(mainProvider: IMainProvider) : AbsDelegateFunction(mainProvider) {
    private var curTime = 0L
    private var silenceModeVM = ViewModelProvider(mainProvider.getMainContext() as ComponentActivity)[DroneLightVM::class.java]
    override fun getFunctionType(): FunctionType {
        return FunctionType.StealthMode
    }

    override fun getFunctionName(): String {
        return mainProvider.getMainContext().getString(R.string.common_text_light_concealment_model_title)
    }

    override fun getFunctionIconRes(): Int {
        return R.drawable.common_selector_shortcuts_silence_mode
    }

    override fun functionEnableDependsOnAircraft(): Boolean {
        return true
    }

    override fun onFunctionCreate() {
        super.onFunctionCreate()
        silenceModeVM.addObserver()
        silenceModeVM.silenceModeStatusLD.observe(mainProvider.getMainLifecycleOwner()) {
            AutelLog.i(AppTagConst.SilenceModeTag, "静默模式 observe:$it")
            if (it) {
                functionModel.isOn = true
                mainProvider.getMainHandler().updateFunctionState(functionModel)
            } else {
                functionModel.isOn = false
                mainProvider.getMainHandler().updateFunctionState(functionModel)
            }
        }
        silenceModeVM.queryAllSilenceMode()
    }

    override fun onFunctionDestroy() {
        super.onFunctionDestroy()
        silenceModeVM.removeObserver()
    }

    override fun onFunctionStart(viewType: FunctionViewType, view: View) {
        super.onFunctionStart(viewType, view)
        AutelLog.i(AppTagConst.SilenceModeTag, "onFunctionStart")
        if (SystemClock.elapsedRealtime() - curTime < 500) {
            AutelLog.i(AppTagConst.SilenceModeTag, "onFunctionStart return")
            AutelToast.normalToast(mainProvider.getMainContext(), R.string.common_text_operate_too_frequent)
            return
        }

        curTime = SystemClock.elapsedRealtime()
        if (DeviceUtils.allControlDrones().isEmpty()) {
            AutelToast.normalToast(mainProvider.getMainContext(), R.string.common_text_no_drone_controler)
        } else {
            CommonDialogManager.showDisclaimerDialog(object : DisclaimerListener {
                override fun onCallBack(isCommit: Boolean) {
                    AutelLog.i("SilenceModeFunctionEntry", "DisclaimerDialog -> onCallBack isCommit=$isCommit")
                    if (isCommit) {
                        silenceModeVM.switchSilenceModeStatus()
                    }
                }
            }, true)
        }
    }

    override fun onFunctionStop(viewType: FunctionViewType, view: View) {
        super.onFunctionStop(viewType, view)
        AutelLog.i(AppTagConst.SilenceModeTag, "onFunctionStop")
        if (SystemClock.elapsedRealtime() - curTime < 500) {
            AutelLog.i(AppTagConst.SilenceModeTag, "onFunctionStop return")
            AutelToast.normalToast(mainProvider.getMainContext(), R.string.common_text_operate_too_frequent)
            return
        }
        curTime = SystemClock.elapsedRealtime()
        if (DeviceUtils.allControlDrones().isEmpty()) {
            AutelToast.normalToast(mainProvider.getMainContext(), R.string.common_text_no_drone_controler)
        } else {
            silenceModeVM.switchSilenceModeStatus()
        }
    }

}