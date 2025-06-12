package com.autel.setting.view

import android.os.Bundle
import android.os.SystemClock
import androidx.lifecycle.lifecycleScope
import com.autel.common.base.BaseAircraftActivity
import com.autel.common.sdk.KeyManagerCoroutineWrapper
import com.autel.common.utils.DeviceUtils
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.RadarPropertyKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.base.KeyTools
import com.autel.log.AutelLog
import com.autel.setting.R
import com.autel.setting.databinding.SettingActivityRadarCalibrationBinding
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author 
 * @date 2024/11/7
 * 雷达校准，给供应商的
 */
class SettingRadarCalibrationActivity : BaseAircraftActivity() {
    private lateinit var binding: SettingActivityRadarCalibrationBinding

    companion object {
        const val TAG = "SettingRadarCalibrationActivity"
    }

    private val isCal = AtomicBoolean(false)
    private var startTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SettingActivityRadarCalibrationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnStart.setOnClickListener {
            //开始校准，然后定频查询
            if (!isCal.get()) startRadarCal()
        }
    }

    /**
     * 0 失败，1 成功，2 开始标定，3标定中
     */
    private fun startRadarCal() {
        AutelLog.i(TAG, "startRadarCal -> param=2")
        binding.btnStart.isEnabled = false
        binding.tvRadarStatus.text = getString(R.string.common_text_radar_cal_status, "...")
        DeviceUtils.singleControlDrone()?.let {
            lifecycleScope.launch(CoroutineExceptionHandler { _, throwable ->
                AutelLog.i(TAG, "startRadarCal -> error = $throwable")
                isCal.set(false)
                binding.btnStart.isEnabled = true
            }) {
                val key = KeyTools.createKey(RadarPropertyKey.RADAR_CALIB_STATE)
                KeyManagerCoroutineWrapper.setValue(
                    it.getKeyManager(),
                    key,
                    2
                )
                AutelLog.i(TAG, "startRadarCal -> success")
                isCal.set(true)
                startTime = SystemClock.elapsedRealtime()
                delay(2000)
                getRadarCalStatus()
            }
        }
    }

    private fun getRadarCalStatus() {
        AutelLog.i(TAG, "getRadarCalStatus -> ")
        if (SystemClock.elapsedRealtime() - startTime > 60_000){
            AutelLog.i(TAG, "getRadarCalStatus -> timeout 60s")
            binding.tvRadarStatus.text = getString(R.string.common_text_radar_cal_status, "timeout")
            isCal.set(false)
            binding.btnStart.isEnabled = true
            return
        }
        DeviceUtils.singleControlDrone()?.let {
            lifecycleScope.launch(CoroutineExceptionHandler { _, throwable ->
                AutelLog.i(TAG, "getRadarCalStatus -> error = $throwable")
                lifecycleScope.launch(Dispatchers.Main) {
                    delay(2000)
                    getRadarCalStatus()
                }
            }) {
                val key = KeyTools.createKey(RadarPropertyKey.RADAR_CALIB_STATE)
                val result = KeyManagerCoroutineWrapper.getValue(
                    it.getKeyManager(),
                    key,
                )
                AutelLog.i(TAG, "getRadarCalStatus -> success result=$result")
                lifecycleScope.launch(Dispatchers.Main) {
                    when (result) {
                        0 -> { //失败
                            binding.tvRadarStatus.text = getString(R.string.common_text_radar_cal_status, "failure")
                            isCal.set(false)
                            binding.btnStart.isEnabled = true
                        }
                        1 -> { //成功
                            binding.tvRadarStatus.text = getString(R.string.common_text_radar_cal_status, "success")
                            isCal.set(false)
                            binding.btnStart.isEnabled = true
                        }
                        else -> { //校准中
                            binding.tvRadarStatus.text = getString(R.string.common_text_radar_cal_status, "...")
                            delay(2000)
                            getRadarCalStatus()
                        }
                    }
                }

            }
        }

    }

}