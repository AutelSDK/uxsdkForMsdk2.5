package com.autel.setting.view

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.autel.common.base.BaseAircraftActivity
import com.autel.common.constant.AppTagConst
import com.autel.common.constant.StringConstants
import com.autel.common.extension.launchAndCollectIn
import com.autel.common.manager.SameResourceHelper
import com.autel.common.widget.dialog.CommonSingleButtonDialog
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.common.enums.CalibrationEventEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.common.enums.CalibrationTypeEnum
import com.autel.log.AutelLog
import com.autel.setting.R
import com.autel.setting.business.SettingCalibrationVM
import com.autel.setting.databinding.SettingActivityImuCalibrationBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * @author 
 * @date 2022/10/12
 * IMU校准，校准飞控IMU校准和云台IMU校准
 */
class SettingIMUCalibrationActivity : BaseAircraftActivity() {
    private lateinit var binding: SettingActivityImuCalibrationBinding
    private val settingIMUVM: SettingCalibrationVM by viewModels()
    private var droneDevice: IAutelDroneDevice? = null
    private var isCanBack = true
    private val stepViews: Array<ImageView> by lazy {
        arrayOf(binding.ivStep1, binding.ivStep2, binding.ivStep3, binding.ivStep4, binding.ivStep6)
    }

    companion object {
        const val TAG = "SettingIMUCalibrationActivity"
    }

    private var currentStep = 0

    private val stepMap = HashMap<Int,Int>()

    /**
     * 是否正在校准
     */
    private var isInCal: Boolean = false

    private var space = 200

    private val handler = Handler(Looper.getMainLooper()) {

        var progress = binding.progress.progress
        if (progress < (currentStep - 1) * space) {
            progress = (currentStep - 1) * space;
        } else if (progress > currentStep * space) {
            progress = (currentStep - 1) * space;
        }
        if (progress < currentStep * space) {
            progress++
            binding.progress.progress = progress
        }
        startProgress()
        true
    }

    private fun startProgress() {
        handler.sendEmptyMessageDelayed(1, 500)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SettingActivityImuCalibrationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val intent = intent
        val droneDeviceId = intent.getIntExtra(StringConstants.ARGS_DRONE_DEVICE_ID, -1)
        droneDevice = DeviceManager.getDeviceManager().getDroneDeviceById(droneDeviceId)
        droneDevice?.let { settingIMUVM.updateDroneDevice(it) }
        AutelLog.i(AppTagConst.IMUCalibrateTag, "droneDeviceId -> $droneDeviceId droneDevice -> $droneDevice")
        init()
        listenStatus()
        initStepMap()
    }

    private fun initStepMap() {
        stepMap.put(1,SameResourceHelper.getIMUDroneIcon1Res(droneDevice))
        stepMap.put(2,SameResourceHelper.getIMUDroneIcon2Res(droneDevice))
        stepMap.put(3,SameResourceHelper.getIMUDroneIcon3Res(droneDevice))
        stepMap.put(4,SameResourceHelper.getIMUDroneIcon4Res(droneDevice))
        stepMap.put(5,SameResourceHelper.getIMUDroneIcon6Res(droneDevice))
    }

    private fun listenStatus() {
        settingIMUVM.calibrationStatus.launchAndCollectIn(this, Lifecycle.State.CREATED) {
            AutelLog.i(AppTagConst.IMUCalibrateTag, "calibrationStatus -> $it")
            when (it) {
                CalibrationEventEnum.START -> {
                    setStepTips()
                    startProgress()
                    nextStep(1)
                }
                CalibrationEventEnum.SUCCESS -> {
                    calibrationSuccess()
                }
                CalibrationEventEnum.SAVE_DATA_FAILED,
                CalibrationEventEnum.TIMEOUT,
                CalibrationEventEnum.NO_GPS,
                CalibrationEventEnum.FAIL,
                CalibrationEventEnum.RESERVE_COMPASS_FAIL,
                CalibrationEventEnum.MAIN_COMPASS_FAIL -> {
                    calibrationError()
                }
                CalibrationEventEnum.WRONG_DIRECTION -> {}
                else -> {

                }
            }
        }
        settingIMUVM.calibrationStep.launchAndCollectIn(this, Lifecycle.State.CREATED) {
            if (it.calibrationType == CalibrationTypeEnum.IMU) {
                AutelLog.i(AppTagConst.IMUCalibrateTag, "calibrationStep -> $it")
                nextStep(it.imcStep.value)
            }
        }
    }


    /**
     * 校准成功
     */
    private fun calibrationSuccess() {
        isInCal = false
        handler.removeCallbacksAndMessages(null)
        binding.clStep.isGone = true
        binding.layoutCal.root.isGone = false
        binding.layoutCal.btnRestart.isVisible = false

        binding.layoutCal.ivStatus.setImageResource(R.drawable.setting_icon_cal_success)
        binding.layoutCal.tvStatus.setText(R.string.common_text_calibration_sucess)

        lifecycleScope.launch {
            delay(500)
            isCanBack = false
            AutelLog.i(TAG, "校准成功，提示用户必须马上重启飞机，否则会有风险！！！")
            CommonSingleButtonDialog(this@SettingIMUCalibrationActivity).apply {
                setDialogTitle(getString(R.string.common_text_imu_cal_success))
                setMessage(getString(R.string.common_text_return_compass_cal_success_tips))
                setButtonText(getString(R.string.common_text_mission_got_known))
                setCancelable(false)
                setOnConfirmListener {
                    AutelLog.i(TAG, "用户已经点击确定")
                    isCanBack = true
                }
                show()
            }
        }
    }

    private fun checkAndStartCal() {
        if (!isCanCalibrate(droneDevice)) return
        handler.removeCallbacksAndMessages(null)
        startCal()
    }

    private fun resetStep() {
        stepViews.forEach {
            it.setImageResource(R.drawable.common_bg_808080_r5)
        }
    }

    private fun startCal() {
        resetStep()
        isInCal = true
        currentStep = 0
        binding.clStep.isVisible = true
        binding.tvStep.isVisible = false
        binding.tvStep.text = "1/5"
        binding.btnStart.isGone = true
        binding.llCalProgress.isVisible = true
        binding.progress.progress = 0
        binding.llCalibration.isVisible = true
        binding.layoutCal.root.isGone = true

        binding.tipsLayout.isGone = true
        binding.stepTips.isVisible = true
        settingIMUVM.startCalibration(CalibrationTypeEnum.IMU, {

        }) {
            calibrationError()
        }
    }

    /**
     * 校准失败
     */
    private fun calibrationError() {
        handler.removeCallbacksAndMessages(null)
        isInCal = false
        binding.tvStep.isGone = true
        binding.clStep.isGone = true
        binding.layoutCal.root.isGone = false
        binding.layoutCal.btnRestart.isVisible = true
        binding.layoutCal.ivStatus.setImageResource(R.drawable.setting_icon_cal_fail)
        binding.layoutCal.tvStatus.setText(R.string.common_text_calibration_faild)
    }

    private fun init() {
        binding.viewTitle.setOnClickListener { finish() }
        binding.ivLogo.setImageResource(SameResourceHelper.getIMUDroneIcon0Res(droneDevice))
        binding.btnStart.setOnClickListener {
            checkAndStartCal()
        }
        binding.layoutCal.btnRestart.setOnClickListener {
            checkAndStartCal()
        }
    }

    /**
     * @param step 当前校准步骤（1-6分别代表：水平朝上、水平朝下、右侧朝上侧放、左侧朝上侧放、竖直朝上、竖直朝下）
     */
    private fun nextStep(step: Int) {
        if (step != currentStep) {
            currentStep = step
            val res = stepMap[currentStep] ?: return
            binding.ivLogo.setImageResource(res)

            binding.tvStep.text = "${currentStep}/6"
            setStepTips()
            stepViews.forEachIndexed { index, imageView ->
                imageView.setImageResource(
                    if (index == step - 1) {
                        R.drawable.common_bg_007aff_r5
                    } else {
                        R.drawable.common_bg_808080_r5
                    }
                )
            }
        }
    }

    private fun setStepTips() {
        binding.stepLayout.isVisible = true
        binding.progress.progress = currentStep * 20

        when (currentStep) {
            1 -> {
                binding.stepTips.text = getString(R.string.common_text_step_one_tips)
                binding.step.text = getString(R.string.common_text_step_one)
            }
            2 -> {
                binding.stepTips.text = getStepTwoStr()
                binding.step.text = getString(R.string.common_text_step_tow)
            }
            3 -> {
                binding.stepTips.text = getString(R.string.common_text_step_three_tips)
                binding.step.text = getString(R.string.common_text_step_three)
            }
            4 -> {
                binding.stepTips.text = getString(R.string.common_text_step_four_tips)
                binding.step.text = getString(R.string.common_text_step_four)
            }
            5 -> {
                binding.stepTips.text = getString(R.string.common_text_step_six_tips)
                binding.step.text = getString(R.string.common_text_step_five)
            }
        }
    }

    /**
     * 变色字体，IMU校准第二步
     */
    private fun getStepTwoStr(): SpannableStringBuilder {
        val strA = getString(R.string.common_text_step_tow_tips)
        val strB = getString(R.string.common_text_protection_lens)
        val style = SpannableStringBuilder("$strA$strB")
        style.setSpan(ForegroundColorSpan(getColor(R.color.common_color_black)), 0, strA.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        style.setSpan(
            ForegroundColorSpan(getColor(R.color.common_color_FF771E)),
            strA.length,
            strA.length + strB.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return style
    }
}