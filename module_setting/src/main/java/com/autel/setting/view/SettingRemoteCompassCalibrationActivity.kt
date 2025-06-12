package com.autel.setting.view

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.autel.common.base.BaseAircraftActivity
import com.autel.common.feature.compass.Compass
import com.autel.common.feature.compass.CompassCallbackListener
import com.autel.common.utils.GoogleTextToSpeechManager
import com.autel.setting.R
import com.autel.setting.business.SettingControlCompassVM
import com.autel.setting.databinding.SettingActivityRemoteCompassCalibrationBinding


/**
 * Created by  2023/12/29
 * 遥控器指南针校准
 */

class SettingRemoteCompassCalibrationActivity : BaseAircraftActivity() {

    companion object {
        private const val TAG = "SettingRemoteCompassCalibrationActivity"

        private const val MAX_CALIBRATE_TIME = 30_000L //最大校验时间
        private const val MIN_CALIBRATE_TIME = 3_000L //最小校验时间
    }

    private lateinit var binding: SettingActivityRemoteCompassCalibrationBinding
    private var calibrationStatus: CalibrationStatus = CalibrationStatus.CALIBRATION_UNSTART //开始校准

    private var startCalibrationTime: Long = SystemClock.elapsedRealtime()
    private val settingControlVM: SettingControlCompassVM by viewModels()
    private val handler = Handler(Looper.getMainLooper())
    private val compassCallbackListener = object : CompassCallbackListener {
        override fun onNewAzimuth(azimuth: Float) {

        }



        override fun onCalibrationState(state: Boolean?) {
            checkCallback(false)
        }
    }
    private val minCalibrateRunnable = Runnable {
        checkCallback(true)
    }

    private val maxCalibrateRunnable = Runnable {
        checkCallback(false)
    }

    private fun checkCallback(onlySuccess: Boolean) {
        if (calibrationStatus == CalibrationStatus.CALIBRATIONING) {
            if (onlySuccess) {
                if (Compass.getInstance().getCalibrationState() == true) {
                    handler.removeCallbacks(minCalibrateRunnable)
                    handler.removeCallbacks(maxCalibrateRunnable)
                    calibrationSuccess()
                }
            } else {
                handler.removeCallbacks(minCalibrateRunnable)
                handler.removeCallbacks(maxCalibrateRunnable)
                if (Compass.getInstance().getCalibrationState() == true) {
                    //校准成功
                    calibrationSuccess()
                } else {
                    //校准失败
                    calibrationFailed()
                }
            }
        }
    }

    //校验状态
    enum class CalibrationStatus {
        CALIBRATION_UNSTART,
        CALIBRATIONING,
        CALIBRATION_SUCCESS,
        CALIBRATION_FAILED
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SettingActivityRemoteCompassCalibrationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
        Compass.getInstance().addCompassCallbackListener(compassCallbackListener)
    }

    private fun initView() {
        binding.viewTitle.setLeftIconClickListener {
            onBackPressed()
        }

        binding.buttonCalibrationStart.setOnClickListener {
            //开始校准
            settingControlVM.enterRCCalibration(onSuccess = {
                calibrationing()
            }, {
                showToast(R.string.common_text_step_fail_2)
            })
        }

        binding.layoutCal.btnRestart.setOnClickListener {
            //开始校准
            settingControlVM.enterRCCalibration(onSuccess = {
                calibrationing()
            }, {
                showToast(R.string.common_text_step_fail_2)
            })
        }

    }

    override fun finish() {
        super.finish()
        settingControlVM.exitRCCalibration()
    }

    /**
     * 进行校准
     */
    private fun calibrationing() {
        calibrationStatus = CalibrationStatus.CALIBRATIONING
        startCalibrationTime = SystemClock.elapsedRealtime()
        binding.buttonCalibrationStart.visibility = View.GONE
        binding.clCalibration.visibility = View.VISIBLE
        binding.clCalibration.isEnabled = false
        binding.clStartCalibration.visibility = View.VISIBLE
        binding.layoutCal.root.visibility = View.GONE
        handler.postDelayed(minCalibrateRunnable, MIN_CALIBRATE_TIME)
        handler.postDelayed(maxCalibrateRunnable, MAX_CALIBRATE_TIME)
    }

    /**
     * 校准成功
     */
    private fun calibrationSuccess() {
        calibrationStatus = CalibrationStatus.CALIBRATION_SUCCESS
        binding.clStartCalibration.visibility = View.GONE
        binding.layoutCal.root.visibility = View.VISIBLE
        binding.layoutCal.btnRestart.isVisible = false

        binding.layoutCal.ivStatus.setImageResource(R.drawable.setting_icon_cal_success)
        binding.layoutCal.tvStatus.setText(R.string.common_text_calibration_sucess)
        GoogleTextToSpeechManager.instance().speak(getString(R.string.common_text_calibration_sucess), false)
    }

    /**
     * 校准失败
     */
    private fun calibrationFailed() {
        calibrationStatus = CalibrationStatus.CALIBRATION_FAILED
        binding.clStartCalibration.visibility = View.GONE
        binding.layoutCal.root.visibility = View.VISIBLE
        binding.layoutCal.btnRestart.isVisible = true
        binding.layoutCal.ivStatus.setImageResource(R.drawable.setting_icon_cal_fail)
        binding.layoutCal.tvStatus.setText(R.string.common_text_calibration_faild)
        GoogleTextToSpeechManager.instance().speak(getString(R.string.common_text_calibration_faild), false)
    }

    override fun onDestroy() {
        super.onDestroy()
        Compass.getInstance().removeCompassCallbackListener(compassCallbackListener)
        handler.removeCallbacks(minCalibrateRunnable)
        handler.removeCallbacks(maxCalibrateRunnable)
    }
}