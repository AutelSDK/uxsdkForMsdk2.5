package com.autel.setting.provider.function

import android.os.SystemClock
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.autel.common.delegate.IMainProvider
import com.autel.common.delegate.function.AbsDelegateFunction
import com.autel.common.delegate.function.FunctionType
import com.autel.common.delegate.function.FunctionViewType
import com.autel.common.utils.DeviceUtils
import com.autel.setting.R
import com.autel.setting.dialog.SettingManagerDialog

/**
 * Created by  2023/5/15
 *  设置功能入口实现
 */
class SettingFunctionEntry(mainProvider: IMainProvider) : AbsDelegateFunction(mainProvider) {

    private var curTime = 0L
    private var settingManagerDialog: DialogFragment? = null

    override fun getFunctionType(): FunctionType {
        return FunctionType.Setting
    }

    override fun getFunctionName(): String {
        return mainProvider.getMainContext().getString(R.string.common_text_system_setting)
    }

    override fun getFunctionIconRes(): Int {
        return R.drawable.common_selector_shortcuts_setting
    }

    override fun onFunctionStart(viewType: FunctionViewType, view: View) {
        super.onFunctionStart(viewType, view)
        if (SystemClock.elapsedRealtime() - curTime < 1000) return
        curTime = SystemClock.elapsedRealtime()
        if (settingManagerDialog == null) {
            settingManagerDialog = SettingManagerDialog()
        }
        if (settingManagerDialog?.isAdded == true) {
            return
        }
        (mainProvider.getMainContext() as FragmentActivity).supportFragmentManager.let {
            settingManagerDialog?.show(it, "settingMangerDialog")
        }
    }

    override fun functionEnableDependsOnAircraft(): Boolean {
        return true
    }

    override fun functionEnableCondition(): Boolean {
        return DeviceUtils.isSingleControl() || DeviceUtils.allControlDrones().none { it.isConnected() }
    }
}