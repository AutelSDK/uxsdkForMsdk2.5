package com.autel.setting.provider.function

import android.content.Context
import android.view.View
import com.autel.common.delegate.IMainProvider
import com.autel.common.delegate.function.AbsDelegateFunction
import com.autel.common.delegate.function.FunctionType
import com.autel.common.delegate.function.FunctionViewType
import com.autel.common.utils.DeviceUtils
import com.autel.common.utils.TransformUtils
import com.autel.common.utils.toLength
import com.autel.common.widget.dialog.CommonNoTitleTwoButtonDialog
import com.autel.common.widget.dialog.CommonSingleButtonDialog
import com.autel.drone.sdk.libbase.error.IAutelCode
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.drone.sdk.vmodelx.manager.keyvalue.callback.CommonCallbacks
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.GimbalKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.ext.create
import com.autel.drone.sdk.vmodelx.module.camera.bean.LaserLightModeEnum
import com.autel.log.AutelLog
import com.autel.setting.R

/**
 * 云台补光灯
 */
class GimbalFillLightFunctionEntry(mainProvider: IMainProvider) : AbsDelegateFunction(mainProvider) {

    private val TAG = "GimbalFillLightFunctionEntry"

    override fun getFunctionType(): FunctionType {
        return FunctionType.GIMBAL_FILL_LIGHT
    }

    override fun getFunctionName(): String {
        return mainProvider.getMainContext().getString(R.string.common_text_fill_light)
    }

    override fun getFunctionIconRes(): Int {
        return R.drawable.common_selector_fill_light_4n_selector
    }

    override fun onFunctionStart(viewType: FunctionViewType, view: View) {
        super.onFunctionStart(viewType, view)
        val drone = DeviceUtils.singleControlDrone() ?: return
        val height = drone.getDeviceStateData().flightControlData.altitude
        if (height < 20) {// 20米下不允许开灯
            showLimitTurnOnDialog()
        } else {
            showTurnOnLightDialog()
        }
    }

    private fun getContext(): Context {
        return mainProvider.getMainContext();
    }

    private fun showLimitTurnOnDialog() {
        val dialog = CommonSingleButtonDialog(getContext())
        dialog.hideTitle()
        dialog.setMessage(
            getContext().getString(
                R.string.common_text_fill_light_limit_tips,
                "${20.toLength().toInt()}",
                TransformUtils.getLengthUnit()
            )
        )
        dialog.setButtonText(getContext().getString(R.string.common_text_mission_got_known))
        dialog.show()
    }

    private fun showTurnOnLightDialog() {
        val dialog = CommonNoTitleTwoButtonDialog(mainProvider.getMainContext())
        dialog.setMessage(getContext().getString(R.string.common_text_turn_on_fill_light_tips))
        dialog.setLeftBtnStr(getContext().getString(R.string.common_text_cancel))
        dialog.setRightBtnStr(getContext().getString(R.string.common_text_continue_to_open))
        dialog.setLeftBtnListener {
            val drone = DeviceUtils.singleControlDrone() ?: return@setLeftBtnListener
            val mode = drone.getCameraAbilitySetManger().getCameraSupport2()?.getLaserLightMode() ?: return@setLeftBtnListener
            if (mode == LaserLightModeEnum.NOT_SUPPORT) {
                return@setLeftBtnListener
            }
            AutelLog.i(TAG, "fill light mode: $mode")
            drone.getKeyManager().setValue(GimbalKey.KeyLaserSwitch.create(), true, object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    AutelLog.i(TAG, "fill light open success")
                }

                override fun onFailure(code: IAutelCode, msg: String?) {
                    AutelLog.i(TAG, "fill light open failed: $code, $msg")
                }

            })
        }
        dialog.show()
    }

    override fun functionEnableCondition(): Boolean {
        if (!DeviceManager.getDeviceManager().isConnected()) {
            return false
        } else {
            val drone = DeviceUtils.singleControlDrone() ?: return false
            return checkAbility(drone)
        }
    }

    override fun functionEnableDependsOnAircraft(): Boolean {
        return true
    }

    override fun onCameraAbilityFetchListener(localFetched: Boolean, remoteFetched: Boolean, drone: IAutelDroneDevice) {
        super.onCameraAbilityFetchListener(localFetched, remoteFetched, drone)

    }

    private fun checkAbility(drone: IAutelDroneDevice): Boolean {
        val mode = drone.getCameraAbilitySetManger().getCameraSupport2()?.getLaserLightMode() ?: LaserLightModeEnum.NOT_SUPPORT
        AutelLog.i(TAG,"drone fill light mode : $mode")
        return mode != LaserLightModeEnum.NOT_SUPPORT
    }
}