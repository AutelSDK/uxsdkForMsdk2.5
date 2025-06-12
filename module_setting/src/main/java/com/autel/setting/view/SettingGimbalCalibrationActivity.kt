package com.autel.setting.view

import android.os.Bundle
import android.view.View.GONE
import androidx.activity.viewModels
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.facade.annotation.Route
import com.autel.common.base.BaseAircraftActivity
import com.autel.common.extension.launchAndCollectIn
import com.autel.common.feature.route.RouterConst
import com.autel.common.manager.SameResourceHelper
import com.autel.common.utils.DeviceUtils
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.common.enums.CalibrationEventEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.common.enums.CalibrationTypeEnum
import com.autel.log.AutelLog
import com.autel.setting.R
import com.autel.setting.business.SettingCalibrationVM
import com.autel.setting.databinding.SettingActivityGimbalCalibrationBinding
import com.autel.setting.widget.SettingGimbalCalibrationProgressMultiState
import com.autel.setting.widget.SettingGimbalCalibrationResultMultiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Created by gaojie 2022/9/9
 * 云台校准
 */
@Route(path = RouterConst.PathConst.ACTIVITY_URL_SETTING_GIMBALCAL)
class SettingGimbalCalibrationActivity : BaseAircraftActivity() {
    private lateinit var binding: SettingActivityGimbalCalibrationBinding
    private lateinit var gimbalCalibrationProgressState: SettingGimbalCalibrationProgressMultiState
    private lateinit var gimbalCalibrationResultState: SettingGimbalCalibrationResultMultiState
    private val settingGimbalViewModel: SettingCalibrationVM by viewModels()

    private var isStartCal = false
    private var curProcess = 0//当前进度
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SettingActivityGimbalCalibrationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        init()
        listenCalibration()
    }

    private fun listenCalibration() {
        settingGimbalViewModel.calibrationStatus.launchAndCollectIn(this, Lifecycle.State.CREATED) {
            if (it != CalibrationEventEnum.UNKNOWN) {
                when (it) {
                    CalibrationEventEnum.START -> {
                        startCal()
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

                    else -> {

                    }
                }
            }
        }
        settingGimbalViewModel.calibrationStep.launchAndCollectIn(this, Lifecycle.State.CREATED) {
            if (it.calibrationType == CalibrationTypeEnum.GIMBAL_ANGLE) {
                //如果进度直接达到10以上了，则认为他是脏数据
                if (curProcess == 0 && it.calibrationPercent > 10) {
                    AutelLog.i("SettingGimbalCalibrationActivity", "calibrationStep -> 脏数据")
                } else {
                    curProcess = it.calibrationPercent
                    binding.scpProgress.current = it.calibrationPercent
                    binding.tvProgress.text = "${it.calibrationPercent}%"
                }
            }
        }
    }

    private fun init() {
        binding.ivLogo.setImageResource(SameResourceHelper.getGimbalCalibrationRes(DeviceUtils.singleControlDrone()))
        gimbalCalibrationProgressState = SettingGimbalCalibrationProgressMultiState()
        gimbalCalibrationResultState = SettingGimbalCalibrationResultMultiState()

        binding.multiContainer.registerMultiState(gimbalCalibrationProgressState)
        binding.multiContainer.registerMultiState(gimbalCalibrationResultState)

        binding.title.setLeftIconClickListener { onBackPressed() }

        binding.tvStart.setOnClickListener {
            if (!isCanCalibrate(DeviceUtils.singleControlDrone())) return@setOnClickListener
            //开始自动校准
            settingGimbalViewModel.startCalibration(CalibrationTypeEnum.GIMBAL_ANGLE, {

            }) {
                calibrationError()
            }
        }
    }

    /**
     * 开始校准
     */
    private fun startCal() {
        binding.layoutCal.root.isGone = true
        binding.multiContainer.visibility = GONE
        binding.clCalProgress.isVisible = true
        binding.scpProgress.current = 0
        binding.tvProgress.text = "0%"
        isStartCal = true
    }

    /**
     * 校准成功
     */
    private fun calibrationSuccess() {
        binding.multiContainer.visibility = GONE
        binding.layoutCal.root.isGone = false
        binding.clCalProgress.isVisible = false
        binding.layoutCal.ivStatus.setImageResource(R.drawable.setting_icon_cal_success)
        binding.layoutCal.tvStatus.setText(R.string.common_text_calibration_sucess)

        lifecycleScope.launch {
            delay(2000)
            finish()
        }
        isStartCal = false
    }

    /**
     * 校准失败
     */
    private fun calibrationError() {
        binding.multiContainer.visibility = GONE
        binding.layoutCal.root.isGone = false
        binding.clCalProgress.isVisible = false
        binding.layoutCal.ivStatus.setImageResource(R.drawable.setting_icon_cal_fail)
        binding.layoutCal.tvStatus.setText(R.string.common_text_calibration_faild)
        isStartCal = false
    }

    override fun onDroneChangedListener(connected: Boolean, drone: IAutelDroneDevice) {
        if (!connected && isStartCal) {
            showToast(R.string.common_text_aircraft_disconnect)
            calibrationError()
        }
    }

}