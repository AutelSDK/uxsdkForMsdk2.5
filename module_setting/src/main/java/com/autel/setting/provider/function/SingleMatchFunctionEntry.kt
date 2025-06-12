package com.autel.setting.provider.function

import android.content.Intent
import android.os.Handler
import android.view.View
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.autel.common.delegate.IMainProvider
import com.autel.common.delegate.function.AbsDelegateFunction
import com.autel.common.delegate.function.FunctionType
import com.autel.common.delegate.function.FunctionViewType
import com.autel.common.feature.route.RouterDataKey
import com.autel.common.manager.MiddlewareManager
import com.autel.common.utils.DeviceUtils
import com.autel.common.widget.dialog.CommonTwoButtonDialog
import com.autel.common.widget.toast.AutelToast
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.log.AutelLog
import com.autel.setting.R
import com.autel.setting.utils.matchUtils.MatchUtils

class SingleMatchFunctionEntry(mainProvider: IMainProvider) : AbsDelegateFunction(mainProvider) {
    override fun getFunctionType(): FunctionType {
        return FunctionType.SingleMatch
    }

    override fun getFunctionName(): String {
        return mainProvider.getMainContext().getString(R.string.common_text_single_match)
    }

    override fun getFunctionIconRes(): Int = R.drawable.common_selector_shortcuts_match

    override fun onFunctionStart(viewType: FunctionViewType, view: View) {
        super.onFunctionStart(viewType, view)
        AutelLog.i("SingleMatchFunctionEntry", "onFunctionStart")
        startMatch()
    }

    override fun functionEnableCondition(): Boolean {
        val flightMode = DeviceUtils.singleControlDrone()?.getDeviceStateData()?.flightControlData?.flightMode
        return DeviceUtils.singleControlDrone()?.isConnected() == true && flightMode?.isFlying() == false
    }

    override fun onFunctionStop(viewType: FunctionViewType, view: View) {
        super.onFunctionStop(viewType, view)
        AutelLog.i("SingleMatchFunctionEntry", "onFunctionStop")
    }

    /**
     * 如果是组网CP，且当前已经连接了飞机，弹出提示框
     * */
    private fun startMatch() {
        //飞机飞行中，不允许进行点对点对频
        val isArmOrFlying = DeviceManager.getDeviceManager().getDroneDevices().any {
            it.isConnected() && it.getDeviceStateData().flightControlData.flightMode.isArmOrFlying()
        }
        if (isArmOrFlying) {
            AutelToast.normalToast(
                mainProvider.getMainContext(),
                mainProvider.getMainContext().getString(R.string.common_text_firmware_match_invalid_flying)
            )
            return
        }
        //如果是组网CP，且当前已经连接了飞机，弹出提示框
        val isNetMeshMatchCp = DeviceUtils.isNetMeshMatchCp()
        val isMainRc = DeviceUtils.isMainRC()
        AutelLog.i("SingleMatchFunctionEntry", "enterNetMeshEntrance isNetMeshMatchCp = $isNetMeshMatchCp isMainRc = $isMainRc")
        if (isNetMeshMatchCp && DeviceManager.getDeviceManager().getDroneDevices().isNotEmpty()) {
            CommonTwoButtonDialog(mainProvider.getMainContext()).apply {
                setTitle(mainProvider.getMainContext().getString(R.string.common_text_tips_title))
                setMessage(mainProvider.getMainContext().getString(R.string.common_text_single_match_quit_netmesh))
                setLeftBtnStr(mainProvider.getMainContext().getString(R.string.common_text_cancel))
                setRightBtnStr(mainProvider.getMainContext().getString(R.string.common_text_confirm))
                setRightBtnListener {
//                    MiddlewareManager.netmeshModule.cancelTeam {
//                        if (it) {
//                            MatchUtils.startRcMatch(mainProvider.getMainContext()) { success ->
//                                AutelLog.i("SingleMatchFunctionEntry", "startRcMatch success1 = $success isMainRc = $isMainRc")
//                                if (success) {
//                                    if (!isMainRc) {//由从遥控器切换到点对点对频成功后，需要重启遥控器
//                                        restartApp()
//                                    }
//                                }
//                            }
//                        } else {
//                            AutelToast.normalToast(
//                                mainProvider.getMainContext(),
//                                mainProvider.getMainContext().getString(R.string.common_text_request_fail)
//                            )
//                        }
//                    }
                }
                show()
            }
        } else {
            startSingleMatch()
        }
    }

    /**
     * 开启点对点对频
     * */
    private fun startSingleMatch() {
        //飞机飞行中，不允许进行点对点对频
        val flightMode = DeviceUtils.singleControlDrone()?.getDeviceStateData()?.flightControlData?.flightMode
        val isNotFlying = flightMode?.isFlying() == false
        if (!isNotFlying && flightMode != null && DeviceUtils.singleControlDrone()?.isConnected() == true) {
            AutelLog.e("SingleMatchFunctionEntry", "startSingleMatch flightMode = $flightMode")
            AutelToast.normalToast(
                mainProvider.getMainContext(),
                mainProvider.getMainContext().getString(R.string.common_text_firmware_match_invalid_flying)
            )
            return
        }
        //如果当前已经连接了一台点对点对频飞机，弹出提示框
        if (DeviceUtils.isSingleControlDroneConnected()) {
            CommonTwoButtonDialog(mainProvider.getMainContext()).apply {
                setTitle(mainProvider.getMainContext().getString(R.string.common_text_connect_aircraft))
                setMessage(mainProvider.getMainContext().getString(R.string.common_text_connect_new_aircraft_repair_tips))
                setLeftBtnStr(mainProvider.getMainContext().getString(R.string.common_text_connect_aircraft_pair))
                setRightBtnStr(mainProvider.getMainContext().getString(R.string.common_text_connect_aircraft_cancel_pair))
                setLeftBtnListener {
                    val isMainRc = DeviceUtils.isMainRC()
                    MatchUtils.startRcMatch(mainProvider.getMainContext()) {
                        AutelLog.i("SingleMatchFunctionEntry", "startRcMatch success2 = $it isMainRc = $isMainRc")
                        if (it) {
                            if (!isMainRc) {//由从遥控器切换到点对点对频成功后，需要重启遥控器
                                restartApp()
                            }
                        }
                    }
                }
                show()
            }
        } else {
            val isMainRc = DeviceUtils.isMainRC()
            MatchUtils.startRcMatch(mainProvider.getMainContext()) {
                AutelLog.i("SingleMatchFunctionEntry", "startRcMatch success3 = $it isMainRc = $isMainRc")
                if (it) {
                    if (!isMainRc) {//由从遥控器切换到点对点对频成功后，需要重启遥控器
                        restartApp()
                    }
                }
            }
        }
    }

    private fun restartApp() {
        Handler().postDelayed({
            val startIntent = Intent(Intent(RouterDataKey.RESTART_ACTION))
            LocalBroadcastManager.getInstance(mainProvider.getMainContext()).sendBroadcast(startIntent)
            // 在这里进行基于 context 的操作
        }, 2000)
    }
}