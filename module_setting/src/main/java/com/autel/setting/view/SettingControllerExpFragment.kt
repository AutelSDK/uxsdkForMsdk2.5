package com.autel.setting.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.autel.common.base.BaseAircraftFragment
import com.autel.common.utils.DeviceUtils
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.enums.DroneFlightModeEnum
import com.autel.log.AutelLog
import com.autel.setting.R
import com.autel.setting.business.SettingControlVM
import com.autel.setting.databinding.SettingControllerExpFragmentBinding
import com.autel.setting.enums.ExpFeelEnum

/**
 * @Author create by LJ
 * @Date 2022/10/20 14
 * 遥控手感
 */
class SettingControllerExpFragment : BaseAircraftFragment() {

    private var rootView: SettingControllerExpFragmentBinding? = null
    private val settingControlVM: SettingControlVM by viewModels()

    /**
     * 更改灵敏度之前的值
     */
    private var upDown: Float = 0f
    private var leftRight: Float = 0f
    private var forwardBack: Float = 0f;

    companion object {
        const val TAG = "SettingControllerExpFragment"
        const val EXP_UP_DOWN_DEFAULT = 0.4f
        const val EXP_LEFT_RIGHT_DEFAULT = 0.4f
        const val EXP_FORWARD_BACK_DEFAULT = 0.3f
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = SettingControllerExpFragmentBinding.inflate(inflater, container, false)
        return rootView?.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rootView?.expUpDown?.apply {
            setTip(getString(R.string.common_text_controller_model_up_title), getString(R.string.common_text_controller_model_down_title))
            getExp()?.updateUpDown(0)
            getExp()?.exp?.let {
                upDown = it
            }
            setOnExpAdjustViewListener {
                settingThrust(it, upDown)
            }
        }

        rootView?.expLeftRight?.apply {
            setTip(getString(R.string.common_text_controller_model_turn_right_title), getString(R.string.common_text_controller_model_turn_left_title))
            getExp()?.updateUpDown(0)
            getExp()?.exp?.let {
                leftRight = it
            }
            setOnExpAdjustViewListener {
                settingYaw(it, leftRight)
            }
        }
        rootView?.expForwardBack?.apply {
            setTip(getString(R.string.common_text_exp_forward_right), getString(R.string.common_text_exp_back_left))
            getExp()?.updateLeftRight(0)
            getExp()?.exp?.let {
                forwardBack = it
            }
            setOnExpAdjustViewListener {
                settingRoll(it, forwardBack)
            }

        }
        /**
         * 重置
         */
        rootView?.tvResetExp?.setOnClickListener {
            //飞机未连接不让校准
            if (!isCanSetExp()) return@setOnClickListener

            rootView?.expUpDown?.setExp(EXP_UP_DOWN_DEFAULT)
            rootView?.expLeftRight?.setExp(EXP_LEFT_RIGHT_DEFAULT)
            rootView?.expForwardBack?.setExp(EXP_FORWARD_BACK_DEFAULT)
            settingThrust(EXP_UP_DOWN_DEFAULT, upDown)
            settingYaw(EXP_LEFT_RIGHT_DEFAULT, leftRight)
            settingRoll(EXP_FORWARD_BACK_DEFAULT, forwardBack)
        }

    }

    override fun addListen() {

    }

    /**
     * 获取设置
     */
    override fun getData() {
        settingControlVM.getExpValue(ExpFeelEnum.RISE_AND_FALL, {
            rootView?.expUpDown?.setExp(it)
        }, {
            AutelLog.i(TAG, "Thrust get onError:$it")
        })
        settingControlVM.getExpValue(ExpFeelEnum.TURN_LEFT_AND_RIGHT, {
            rootView?.expLeftRight?.setExp(it)
        }, {
            AutelLog.i(TAG, "Roll get onError:$it")
        })
        settingControlVM.getExpValue(ExpFeelEnum.FORWARD_AND_BACKWARD, {
            rootView?.expForwardBack?.setExp(it)
        }, {
            AutelLog.i(TAG, "Yaw get onError:$it")
        })

    }

    private var isRetryThrust = false

    private fun settingThrust(it: Float, preValue: Float) {
        if (!isCanSetExp()) {
            rootView?.expUpDown?.setExp(preValue)
            return
        }
        settingControlVM.setExpValue(it, ExpFeelEnum.RISE_AND_FALL, {
            AutelLog.i(TAG, "Thrust setting Success")
            rootView?.expUpDown?.getExp()?.exp?.let {
                upDown = it
            }
            isRetryThrust = false
        }, { e ->
            if (!isRetryThrust) {
                settingThrust(it, preValue)
                isRetryThrust = true
                AutelLog.e(TAG, "isRetryThrust start ->")
            } else {
                showToast(R.string.common_text_set_failed)
                rootView?.expUpDown?.setExp(preValue)
            }
            e.message?.let { AutelLog.i(TAG, "Thrust setting Error:$e") }
        })
    }

    private var isRetryYaw = false

    private fun settingYaw(it: Float, preValue: Float) {
        if (!isCanSetExp()) {
            rootView?.expLeftRight?.setExp(preValue)
            return
        }
        settingControlVM.setExpValue(it, ExpFeelEnum.TURN_LEFT_AND_RIGHT, {
            AutelLog.i(TAG, "Yaw setting Success")
            rootView?.expLeftRight?.getExp()?.exp?.let {
                forwardBack = it
            }
            isRetryYaw = false
        }, { e ->
            if (!isRetryYaw) {
                settingYaw(it, preValue)
                isRetryYaw = true
                AutelLog.e(TAG, "isRetryYaw start ->")
            } else {
                showToast(R.string.common_text_set_failed)
                rootView?.expLeftRight?.setExp(preValue)
            }
            e.message?.let { AutelLog.i(TAG, "Yaw setting Error:$e") }
        })
    }

    private var isRetryRoll = false
    private fun settingRoll(it: Float, preValue: Float) {
        if (!isCanSetExp()) {
            rootView?.expForwardBack?.setExp(preValue)
            return
        }
        settingControlVM.setExpValue(it, ExpFeelEnum.FORWARD_AND_BACKWARD, {
            AutelLog.i(TAG, "Roll setting Success")
            isRetryRoll = true
            rootView?.expForwardBack?.getExp()?.exp?.let {
                leftRight = it
            }
        }, { e ->
            if (!isRetryRoll) {
                settingRoll(it, preValue)
                isRetryRoll = true
                AutelLog.e(TAG, "isRetryRoll start ->")
            } else {
                showToast(R.string.common_text_set_failed)
                rootView?.expForwardBack?.setExp(preValue)
            }
            e.message?.let { AutelLog.i(TAG, "Roll setting Error:$e") }
        })
    }

    /**
     * 是否可以校准 exp
     */
    private fun isCanSetExp(): Boolean {
        //飞机未连接不让校准
        if (!DeviceUtils.isSingleControlDroneConnected()) {
            showToast(com.autel.common.R.string.common_text_aircraft_disconnect)
            return false
        }
        //电机转动不能校准 宁总要求飞行过程中可以校准
        /* if (mode != DroneFlightModeEnum.DISARM) {
             showToast(com.autel.common.R.string.common_text_motor_working_no_calibrate)
             return false
         }*/
        return true
    }

}