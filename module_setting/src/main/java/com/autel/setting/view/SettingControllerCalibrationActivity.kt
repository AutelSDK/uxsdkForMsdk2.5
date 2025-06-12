package com.autel.setting.view

import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.autel.common.base.BaseAircraftActivity
import com.autel.common.constant.AppTagConst
import com.autel.common.constant.StringConstants
import com.autel.common.manager.AppInfoManager
import com.autel.common.manager.SameResourceHelper
import com.autel.common.manager.appinfo.AppRunningDeviceEnum
import com.autel.common.utils.DeviceUtils
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.drone.sdk.vmodelx.manager.keyvalue.callback.CommonCallbacks
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.RemoteControllerKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.base.KeyTools
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.common.bean.RockerCalibrationStateNtfyBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.common.enums.GimbalCalState
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.common.enums.RcDirectionEnum
import com.autel.log.AutelLog
import com.autel.setting.R
import com.autel.setting.business.SettingControlVM
import com.autel.setting.databinding.SettingControllerCalibrationFragmentBinding
import com.autel.setting.widget.RemoteCalibrationImageView
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.atomic.AtomicInteger

/**
 * @Author create by LJ
 * @Date 2022/09/12 11:24
 *
 * 遥控器校准页面
 */
class SettingControllerCalibrationActivity : BaseAircraftActivity() {
    /**
     * 遥控器校准步骤
     */
    private var calibrationNumber: AtomicInteger = if (AppInfoManager.getAppRunningDevice() == AppRunningDeviceEnum.AutelRemotePad10_9) {
        AtomicInteger(22)
    } else {
        AtomicInteger(20)
    }

    private val settingControlVM: SettingControlVM by viewModels()

    private var droneDevice: IAutelDroneDevice? = null

    /**
     * 是否取消校准
     */
    private var isCancelCalibration: Boolean = false

    private lateinit var rootView: SettingControllerCalibrationFragmentBinding
    private var isStartCal = false//是否开始校准
    private var rcHardWareInfoKey = KeyTools.createKey(RemoteControllerKey.KeyRCRockerCalibrationState)
    private var rcHardWareCallback = object : CommonCallbacks.KeyListener<RockerCalibrationStateNtfyBean> {
        override fun onValueChange(oldValue: RockerCalibrationStateNtfyBean?, newValue: RockerCalibrationStateNtfyBean) {
            calibrationLeftRemote(newValue)
            calibrationRightRemote(newValue)
            calibrationLeftRoll(newValue)
            calibrationRightRoll(newValue)
            calibrationRightTopRoll(newValue)
            isCalibrationOver(newValue)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rootView = SettingControllerCalibrationFragmentBinding.inflate(LayoutInflater.from(this))
        setContentView(rootView.root)
        val intent = intent
        val droneDeviceId = intent.getIntExtra(StringConstants.ARGS_DRONE_DEVICE_ID, -1)
        droneDevice = DeviceManager.getDeviceManager().getDroneDeviceById(droneDeviceId)
        AutelLog.i(AppTagConst.RemoteCalibrateTag, "droneDeviceId -> $droneDeviceId droneDevice -> $droneDevice")
        initView()
    }


    private fun initView() {
        rootView.includeStartControllerCalibration.buttonCalibrationStart.setOnClickListener {
            if (droneDevice != null && DeviceUtils.isDroneFlying(droneDevice!!)) {
                showToast(R.string.common_text_motor_working_no_calibrate)
                return@setOnClickListener
            }
            settingControlVM.enterRCCalibration(
                {
                    isStartCal = true
                    isCancelCalibration = false
                    showCalibration()
                    AutelLog.e(AppTagConst.RemoteCalibrateTag, "enterRCCalibration success")
                },
                { e ->
                    showToast(R.string.common_text_step_fail_2)
                    AutelLog.e(AppTagConst.RemoteCalibrateTag, "enterRCCalibration error:$e")
                },
            )
        }
        rootView.rightRollUp.isVisible = AppInfoManager.getAppRunningDevice() == AppRunningDeviceEnum.AutelRemotePad10_9

        rootView.viewTitle.setLeftIconClickListener {
            if (rootView.gpCalibrationContent.isVisible) {
                settingControlVM.exitRCCalibration({
                    //停止校准，返回开始页面
                    AutelLog.e(AppTagConst.RemoteCalibrateTag, "exitRcCalibration ")
                    resetCalibration()
                    isCancelCalibration = true
                    showStartCalibration()
                }, { e ->
                    AutelLog.e(AppTagConst.RemoteCalibrateTag, "exitRcCalibration error:$e")
                    showToast(R.string.common_text_step_fail_2)
                })
            } else {
                AutelLog.i(AppTagConst.RemoteCalibrateTag, "finish")
                //关闭校准页面
                finish()
            }
        }
        rootView.includeStartControllerCalibration.ivControllIcon.setImageResource(SameResourceHelper.getRemoteControlRes())
    }

    override fun initObserver() {
        super.initObserver()
        settingControlVM.getRemoteManager()?.listen(rcHardWareInfoKey, callback = rcHardWareCallback)
    }

    override fun onDestroy() {
        super.onDestroy()
        settingControlVM.getRemoteManager()?.cancelListen(rcHardWareInfoKey, callback = rcHardWareCallback)
    }


    /**
     * 是否校准完成（失败或者成功）
     */
    private fun isCalibrationOver(rocker: RockerCalibrationStateNtfyBean) {
        if (rocker.rightStickStatus == -1 && rocker.leftStickStatus == -1 && rocker.rightThumbWheelStatus == -1 && rocker.leftThumbWheelStatus == -1) {
            if (AppInfoManager.getAppRunningDevice() == AppRunningDeviceEnum.AutelRemotePad10_9) {
                if (rocker.gimYawThumbWheelStatus == -1) {
                    if (calibrationNumber.get() <= 0) {
                        calibrationSuccessOrError(true)
                    } else {
                        if (!isStartCal) return
                        calibrationSuccessOrError(false)
                    }
                }
            } else {
                if (calibrationNumber.get() <= 0) {
                    calibrationSuccessOrError(true)
                } else {
                    if (!isStartCal) return
                    calibrationSuccessOrError(false)
                }
            }
        }

    }


    /**
     * 左边遥感
     */
    private fun calibrationLeftRemote(rocker: RockerCalibrationStateNtfyBean) {
        matchCalibration(rootView.ivLeftCalibration1, (rocker.getLeftStickState(RcDirectionEnum.TOP)))
        matchCalibration(rootView.ivLeftCalibration2, (rocker.getLeftStickState(RcDirectionEnum.LEFT_TOP)))
        matchCalibration(rootView.ivLeftCalibration3, (rocker.getLeftStickState(RcDirectionEnum.LEFT)))
        matchCalibration(rootView.ivLeftCalibration4, (rocker.getLeftStickState(RcDirectionEnum.LEFT_BOTTOM)))
        matchCalibration(rootView.ivLeftCalibration5, (rocker.getLeftStickState(RcDirectionEnum.BOTTOM)))
        matchCalibration(rootView.ivLeftCalibration6, (rocker.getLeftStickState(RcDirectionEnum.RIGHT_BOTTOM)))
        matchCalibration(rootView.ivLeftCalibration7, (rocker.getLeftStickState(RcDirectionEnum.RIGHT)))
        matchCalibration(rootView.ivLeftCalibration8, (rocker.getLeftStickState(RcDirectionEnum.RIGHT_TOP)))
    }

    /***
     * 右边遥感
     */
    private fun calibrationRightRemote(rocker: RockerCalibrationStateNtfyBean) {
        matchCalibration(rootView.ivRightCalibration1, (rocker.getRightStickState(RcDirectionEnum.TOP)))
        matchCalibration(rootView.ivRightCalibration2, (rocker.getRightStickState(RcDirectionEnum.LEFT_TOP)))
        matchCalibration(rootView.ivRightCalibration3, (rocker.getRightStickState(RcDirectionEnum.LEFT)))
        matchCalibration(rootView.ivRightCalibration4, (rocker.getRightStickState(RcDirectionEnum.LEFT_BOTTOM)))
        matchCalibration(rootView.ivRightCalibration5, (rocker.getRightStickState(RcDirectionEnum.BOTTOM)))
        matchCalibration(rootView.ivRightCalibration6, (rocker.getRightStickState(RcDirectionEnum.RIGHT_BOTTOM)))
        matchCalibration(rootView.ivRightCalibration7, (rocker.getRightStickState(RcDirectionEnum.RIGHT)))
        matchCalibration(rootView.ivRightCalibration8, (rocker.getRightStickState(RcDirectionEnum.RIGHT_TOP)))
    }

    /**
     * 左拨轮
     */
    private fun calibrationLeftRoll(rocker: RockerCalibrationStateNtfyBean) {
        matchCalibration(rootView.ivLeftRollRight, rocker.getLeftThumbWheelState(RcDirectionEnum.ROLL_LEFT))
        matchCalibration(rootView.ivLeftRollLeft, rocker.getLeftThumbWheelState(RcDirectionEnum.ROLL_RIGHT))
    }

    /**
     * 右拨轮
     */
    private fun calibrationRightRoll(rocker: RockerCalibrationStateNtfyBean) {
        matchCalibration(rootView.ivRightRollRight, rocker.getRightThumbWheelState(RcDirectionEnum.ROLL_LEFT))
        matchCalibration(rootView.ivRightRollLeft, rocker.getRightThumbWheelState(RcDirectionEnum.ROLL_RIGHT))
    }

    /**
     * 右上拨轮
     */
    private fun calibrationRightTopRoll(rocker: RockerCalibrationStateNtfyBean) {
        matchCalibration(rootView.ivRightRollUpRight, rocker.getGimbalYawThumbWheelState(RcDirectionEnum.ROLL_LEFT))
        matchCalibration(rootView.ivRightRollUpLeft, rocker.getGimbalYawThumbWheelState(RcDirectionEnum.ROLL_RIGHT))
    }

    /**
     * 是否校准判断
     */
    private fun calibrationSuccessOrError(calibrationFlag: Boolean) {
        rootView.includeCalibrationResult.root.isVisible = !isCancelCalibration
        rootView.gpCalibrationContent.isVisible = false
        resetCalibration()
        if (calibrationFlag) {
            rootView.includeCalibrationResult.ivStatus.setImageResource(R.drawable.setting_icon_cal_success)
            rootView.includeCalibrationResult.tvStatus.setText(R.string.common_text_calibration_sucess)
            Timer().schedule(object : TimerTask() {
                override fun run() {
                    runOnUiThread {
                        finish()
                    }
                }
            }, 2000)
        } else {
            rootView.includeCalibrationResult.ivStatus.setImageResource(R.drawable.setting_icon_cal_fail)
            rootView.includeCalibrationResult.tvStatus.setText(R.string.common_text_calibration_faild)
            Timer().schedule(object : TimerTask() {
                override fun run() {
                    runOnUiThread {
                        rootView.includeCalibrationResult.root.isVisible = false
                        showStartCalibration()
                    }
                }
            }, 2000)
        }

    }

    override fun onBackPressed() {
        super.onBackPressed()
        settingControlVM.exitRCCalibration({}, {})
    }

    private fun matchCalibration(view: RemoteCalibrationImageView, gimbalCalState: GimbalCalState) {
        synchronized(this) {
            when (gimbalCalState) {
                GimbalCalState.NORMAL -> { //已校准
                    if (!view.isCalibrationState()) {  //上次返回的状态中没有校准,则发出“滴”的长鸣声
                        view.setIsCalibration(true)
                        calibrationNumber.decrementAndGet()
                    }
                }

                GimbalCalState.INVALID -> {  //方向有波动，但未达到有效位置
                    view.setImageResource(R.drawable.setting_calibration_arrow_ing)
                }

                GimbalCalState.UNKNOWN -> { //未知校准状态

                }

                else -> {

                }
            }
        }

    }


    /**
     * 校准页面
     */
    private fun showCalibration() {
        rootView.includeStartControllerCalibration.root.isVisible = false
        rootView.gpCalibrationContent.isVisible = true
    }

    /**
     * 开始校准页面
     */
    private fun showStartCalibration() {

        rootView.includeStartControllerCalibration.root.isVisible = true
        rootView.gpCalibrationContent.isVisible = false
    }

    /**
     * 重置所有选中状态
     */
    private fun resetCalibration() {
        isStartCal = false
        rootView.ivLeftCalibration1.setIsCalibration(false)
        rootView.ivLeftCalibration2.setIsCalibration(false)
        rootView.ivLeftCalibration3.setIsCalibration(false)
        rootView.ivLeftCalibration4.setIsCalibration(false)
        rootView.ivLeftCalibration5.setIsCalibration(false)
        rootView.ivLeftCalibration6.setIsCalibration(false)
        rootView.ivLeftCalibration7.setIsCalibration(false)
        rootView.ivLeftCalibration8.setIsCalibration(false)

        rootView.ivRightCalibration1.setIsCalibration(false)
        rootView.ivRightCalibration2.setIsCalibration(false)
        rootView.ivRightCalibration3.setIsCalibration(false)
        rootView.ivRightCalibration4.setIsCalibration(false)
        rootView.ivRightCalibration5.setIsCalibration(false)
        rootView.ivRightCalibration6.setIsCalibration(false)
        rootView.ivRightCalibration7.setIsCalibration(false)
        rootView.ivRightCalibration8.setIsCalibration(false)

        rootView.ivRightRollRight.setIsCalibration(false)
        rootView.ivRightRollLeft.setIsCalibration(false)

        rootView.ivLeftRollLeft.setIsCalibration(false)
        rootView.ivLeftRollRight.setIsCalibration(false)

        rootView.ivRightRollUpLeft.setIsCalibration(false)
        rootView.ivRightRollUpRight.setIsCalibration(false)

    }

}