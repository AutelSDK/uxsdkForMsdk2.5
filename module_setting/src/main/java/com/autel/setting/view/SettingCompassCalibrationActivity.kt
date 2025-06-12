package com.autel.setting.view

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.autel.common.base.BaseAircraftActivity
import com.autel.common.constant.AppTagConst.CompassCalibrateTag
import com.autel.common.constant.StringConstants.Companion.ARGS_DRONE_DEVICE_ID
import com.autel.common.extension.launchAndCollectIn
import com.autel.common.manager.SameResourceHelper
import com.autel.common.sdk.ModelXDroneConst
import com.autel.common.utils.TransformUtils
import com.autel.common.widget.dialog.CommonSingleButtonDialog
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.common.enums.CalibrationEventEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.common.enums.CalibrationTypeEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.common.enums.CompassCalibrationStepEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.bean.DroneSystemStateLFNtfyBean
import com.autel.log.AutelLog
import com.autel.setting.R
import com.autel.setting.business.SettingCalibrationVM
import com.autel.setting.databinding.SettingCompassCalibrationFragmentBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * @Author create by LJ
 * @Date 2022/09/15 19:52
 * 指南针校准
 */
class SettingCompassCalibrationActivity : BaseAircraftActivity() {

    private val settingCompassVM: SettingCalibrationVM by viewModels()
    private lateinit var binding: SettingCompassCalibrationFragmentBinding
    private var droneDevice: IAutelDroneDevice? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SettingCompassCalibrationFragmentBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val intent = intent
        val droneDeviceId = intent.getIntExtra(ARGS_DRONE_DEVICE_ID, -1)
        droneDevice = DeviceManager.getDeviceManager().getDroneDeviceById(droneDeviceId)
        AutelLog.i(CompassCalibrateTag, "droneDeviceId -> $droneDeviceId droneDevice -> $droneDevice")
        droneDevice?.let { settingCompassVM.updateDroneDevice(it) }
        initView()
    }

    override fun initObserver() {
        super.initObserver()
        settingCompassVM.calibrationStatus.launchAndCollectIn(this, Lifecycle.State.CREATED) {
            AutelLog.i(CompassCalibrateTag, "calibrationStatus -> $it")
            when (it) {
                CalibrationEventEnum.START -> {
                    calInStep(CompassCalibrationStepEnum.STEP1)
                }

                CalibrationEventEnum.SUCCESS -> {
                    calSuccess()
                }

                CalibrationEventEnum.SAVE_DATA_FAILED,
                CalibrationEventEnum.TIMEOUT,
                CalibrationEventEnum.NO_GPS,
                CalibrationEventEnum.FAIL,
                CalibrationEventEnum.RESERVE_COMPASS_FAIL,
                CalibrationEventEnum.MAIN_COMPASS_FAIL -> {
                    calFail()
                }

                else -> {}
            }
        }
        settingCompassVM.calibrationStep.launchAndCollectIn(this, Lifecycle.State.CREATED) {
            AutelLog.i(CompassCalibrateTag, "calibrationStep -> $it")
            if (it.calibrationType == CalibrationTypeEnum.COMPASS) {
                calInStep(it.compassStep)
            }
        }
    }

    /**
     * 校准失败
     */
    private fun calFail() {
        binding.clStep.isGone = true
        binding.layoutCal.root.isGone = false
        binding.layoutCal.btnRestart.isVisible = true
        binding.layoutCal.ivStatus.setImageResource(R.drawable.setting_icon_cal_fail)
        binding.layoutCal.tvStatus.setText(R.string.common_text_calibration_faild)
        binding.gpInitCalibration.visibility = View.VISIBLE
        binding.gpStartCalibration.visibility = View.GONE
    }

    private var isCanBack = true

    /**
     * 校准成功
     */
    private fun calSuccess() {
        binding.clStep.isGone = true
        binding.layoutCal.root.isGone = false
        binding.layoutCal.btnRestart.isVisible = false

        binding.layoutCal.ivStatus.setImageResource(R.drawable.setting_icon_cal_success)
        binding.layoutCal.tvStatus.setText(R.string.common_text_calibration_sucess)

        lifecycleScope.launch {
            delay(500)
            isCanBack = false
            AutelLog.i(CompassCalibrateTag, "校准成功，提示用户必须马上重启飞机，否则会有风险！！！")
            CommonSingleButtonDialog(this@SettingCompassCalibrationActivity).apply {
                setDialogTitle(getString(R.string.common_text_return_compass_cal_success))
                setMessage(getString(R.string.common_text_return_compass_cal_success_tips))
                setButtonText(getString(R.string.common_text_mission_got_known))
                setCancelable(false)
                setOnConfirmListener {
                    AutelLog.i(CompassCalibrateTag, "用户已经点击确定")
                    isCanBack = true
                }
                show()
            }
        }
    }

    override fun onBackPressed() {
        if (!isCanBack) return
        super.onBackPressed()
    }

    private fun calInStep(step: CompassCalibrationStepEnum) {
        when (step) {
            CompassCalibrationStepEnum.STEP1, CompassCalibrationStepEnum.ROTATE1 -> {
                binding.gpInitCalibration.visibility = View.GONE
                binding.gpStartCalibration.visibility = View.VISIBLE
                binding.settingTvCalNumber.text = "1/3"
                binding.tvCalibrationStepTitle.text = getString(R.string.common_text_step_one)
                binding.tvCalibrationStepContent.text = getString(R.string.common_text_step_first_content)
                binding.settingIvCalibrationInit.setImageResource(SameResourceHelper.getCompassCalibration1Res(droneDevice))
                binding.ivIndicateOne.setBackgroundResource(R.drawable.common_indicate_cricle_selector)
            }

            CompassCalibrationStepEnum.STEP2, CompassCalibrationStepEnum.ROTATE2 -> {
                binding.gpInitCalibration.visibility = View.GONE
                binding.gpStartCalibration.visibility = View.VISIBLE
                binding.settingTvCalNumber.text = "2/3"
                binding.tvCalibrationStepTitle.text = getString(R.string.common_text_step_tow)
                binding.tvCalibrationStepContent.text = getString(R.string.common_text_step_seconde_content)
                binding.settingIvCalibrationInit.setImageResource(SameResourceHelper.getCompassCalibration2Res(droneDevice))
                binding.ivIndicateOne.setBackgroundResource(R.drawable.common_indicate_cricle_selector)
                binding.ivIndicateTwo.setBackgroundResource(R.drawable.common_indicate_cricle_selector)
            }

            CompassCalibrationStepEnum.STEP3, CompassCalibrationStepEnum.ROTATE3 -> {
                binding.gpInitCalibration.visibility = View.GONE
                binding.gpStartCalibration.visibility = View.VISIBLE
                binding.settingIvCalibrationInit.setImageResource(SameResourceHelper.getCompassCalibration3Res(droneDevice))
                binding.settingTvCalNumber.text = "3/3"
                binding.tvCalibrationStepTitle.text = getString(R.string.common_text_step_three)
                binding.tvCalibrationStepContent.text = getString(R.string.common_text_step_third_content)
                binding.ivIndicateOne.setBackgroundResource(R.drawable.common_indicate_cricle_selector)
                binding.ivIndicateTwo.setBackgroundResource(R.drawable.common_indicate_cricle_selector)
                binding.ivIndicateThree.setBackgroundResource(R.drawable.common_indicate_cricle_selector)
            }

            else -> {}
        }
    }

    private fun initView() {
        binding.tvStartInfo.text =
            getString(
                R.string.common_text_calibration_content,
                TransformUtils.getDistanceValueWithm(ModelXDroneConst.COMPASS_CALIBRATION_DISTANCE, 1)
            )
        binding.viewTitle.setOnClickListener {
            super.onBackPressed()
        }
        binding.settingIvCalibrationInit.setImageResource(SameResourceHelper.getCompassCalibrationRes(droneDevice))
        binding.buttonCalibrationStart.setOnClickListener {
            checkAndStartCal()
        }
        binding.layoutCal.btnRestart.setOnClickListener {
            binding.clStep.isGone = false
            binding.layoutCal.root.isGone = true
            checkAndStartCal()
        }
    }

    private fun checkAndStartCal() {
        if (!isCanCalibrate(droneDevice)) return
        val strengthPercentage = droneDevice?.getDeviceStateData()?.flightControlData?.gpsStrengthPercentage ?: 0
        if (strengthPercentage <= DroneSystemStateLFNtfyBean.GPS_WEAK) {
            showToast(R.string.common_text_step_no_gps)
            return
        }
        settingCompassVM.startCalibration(CalibrationTypeEnum.COMPASS, {

        }) {
            showToast(R.string.common_text_step_fail_2)
        }
    }

}