package com.autel.setting.provider.function

import android.view.View
import com.autel.common.constant.AppTagConst.GnssTag
import com.autel.common.constant.SharedParams
import com.autel.common.delegate.IMainProvider
import com.autel.common.delegate.function.AbsDelegateFunction
import com.autel.common.delegate.function.FunctionType
import com.autel.common.delegate.function.FunctionViewType
import com.autel.common.utils.GoogleTextToSpeechManager
import com.autel.common.utils.UIUtils.getString
import com.autel.log.AutelLog
import com.autel.setting.R
import com.autel.setting.business.GNSSViewModel

class GNSSFunctionEntry(mainProvider: IMainProvider) : AbsDelegateFunction(mainProvider) {

    private var viewModel: GNSSViewModel = GNSSViewModel()

    override fun getFunctionType(): FunctionType {
        return FunctionType.NoGpsFly
    }

    override fun getFunctionName(): String {
        return mainProvider.getMainContext().getString(R.string.common_text_no_gps_fly_open)
    }

    override fun getFunctionIconRes(): Int {
        return R.drawable.common_selector_shortcuts_no_gps_fly
    }

    override fun getFunctionDisplayTitle(): String {
        return mainProvider.getMainContext().getString(R.string.common_text_no_gps_fly)
    }

    override fun functionEnableDependsOnAircraft(): Boolean {
        return true
    }

    override fun onFunctionCreate() {
        super.onFunctionCreate()
        viewModel.addObserver()
        SharedParams.gnssSwitch.observe(mainProvider.getMainLifecycleOwner()) {
            AutelLog.i(GnssTag,"openLd $it")
            updateFunctionOnStatus(!it)
        }
        getGNSSStatus()
    }

    override fun onFunctionDestroy() {
        super.onFunctionDestroy()
        viewModel.removeObserver()
    }
    /**
     * 获取GNSS开关状态
     * */
    private fun getGNSSStatus() {
        viewModel.getOpenStatus()
    }
    /**
     * 点击关闭GNSS
     * */
    override fun onFunctionStart(viewType: FunctionViewType, view: View) {
        super.onFunctionStart(viewType, view)
        viewModel.setAllDronesOpen(false)
        GoogleTextToSpeechManager.instance().speak(getString(R.string.common_text_close_gnss), true)
    }
    /**
     * 点击开启GNSS
     * */
    override fun onFunctionStop(viewType: FunctionViewType, view: View) {
        super.onFunctionStop(viewType, view)
        viewModel.setAllDronesOpen(true)
        GoogleTextToSpeechManager.instance().speak(getString(R.string.common_text_open_gnss), true)
    }

    /**
     * 开启GNSS后，显示白色GNSS图标和已开启
     * 关闭GNSS后，显示黄色GNSS关闭的图标和已关闭
     * */
    private fun updateFunctionOnStatus(on: Boolean) {
        functionModel.isOn = on
        if (on) {
            functionModel.functionName = getString(R.string.common_text_no_gps_fly_close)
        } else {
            functionModel.functionName = getString(R.string.common_text_no_gps_fly_open)
        }
        mainProvider.getMainHandler().updateFunctionState(functionModel)
    }

}