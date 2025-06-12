package com.autel.setting.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.autel.common.base.BaseAircraftFragment
import com.autel.common.feature.route.RouteManager
import com.autel.common.feature.route.RouterConst
import com.autel.common.lifecycle.LiveDataBus
import com.autel.common.lifecycle.event.GimbalAdjustEvent
import com.autel.common.lifecycle.event.ShowGimbalAdjustModel
import com.autel.common.utils.DeviceUtils
import com.autel.common.widget.dialog.CommonTwoButtonDialog
import com.autel.log.AutelLog
import com.autel.setting.R
import com.autel.setting.business.SettingGimbalViewModel
import com.autel.setting.databinding.SettingGimbalFragmentBinding
import com.autel.setting.state.SwitchStateVM


/**
 * @Author create by LJ
 * @Date 2022/9/1 10:34
 * 云台设置
 */
class SettingGimbalFragment : BaseAircraftFragment() {
    private lateinit var binding: SettingGimbalFragmentBinding
    private val settingGimbalVM: SettingGimbalViewModel by viewModels()
    private val switchVM: SwitchStateVM by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = SettingGimbalFragmentBinding.inflate(LayoutInflater.from(context))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()

    }

    override fun onVisible() {
        super.onVisible()
        if (!DeviceUtils.isSingleControlDroneConnected()) {
            showToast(R.string.common_text_aircraft_disconnect)
            return
        }
    }

    override fun addListen() {

    }

    private fun init() {

        //云台俯仰轴最大速度
        binding.viewPtzMaximumPitch.setOnSeekBarStoppedListener {
            settingGimbalVM.setMaxPitchSpeed(
                it.toInt(),
                {
                    AutelLog.i("SettingGimbalFragment", "setMaxPitchSpeed is Success")

                }, { e ->
                    e.message?.let { msg ->
                    }
                    AutelLog.i("SettingGimbalFragment", "setMaxPitchSpeed is Error $e")
                })
        }
        //俯仰角开关
        binding.cisCoordinateTurn.setOnSwitchChangeListener {
            settingGimbalVM.setPitchRote30(it,
                {
                    AutelLog.i("SettingGimbalFragment", "setMaxPitchSpeed is Success")

                }, { e ->
                    e.message?.let { msg ->
                    }
                    AutelLog.i("SettingGimbalFragment", "setMaxPitchSpeed is Error $e")
                    //指令失败处理
                    showToast(R.string.common_text_set_failed)
                    binding.cisCoordinateTurn.setCheckedWithoutListener(!it)
                })
        }
        //云台重置,就是把三个轴都变成0
        binding.viewResetPtzParameters.setOnRightViewListener {
            CommonTwoButtonDialog(requireContext()).apply {
                setMessage(getString(R.string.common_text_is_reset_ptz_parameters))
                setRightBtnListener {
                    settingGimbalVM.resetGimbalPatch(
                        onSuccess = {
                            showToast(R.string.common_text_reset_succeeded)
                        },
                        onError = {
                            showToast(R.string.common_text_reset_failed)
                        })
                }
                show()
            }
        }
        //云台自动校准
        binding.viewPtzAutomaticCalibration.setOnItemClickListener {
            RouteManager.routeTo(requireContext(), RouterConst.PathConst.ACTIVITY_URL_SETTING_GIMBALCAL)
        }
        //云台微调
        binding.viewPtzFineAdjustment.setOnItemClickListener {
            LiveDataBus.of(GimbalAdjustEvent::class.java).showGimbalAdjust().post(ShowGimbalAdjustModel(true))
            switchVM.dismiss()
        }
    }

    override fun getData() {
        settingGimbalVM.getPitchRote30(
            onSuccess = {
                binding.cisCoordinateTurn.setCheckedWithoutListener(it)
            },
            onError = {})
        settingGimbalVM.getMaxPitchSpeed(onSuccess = {
            binding.viewPtzMaximumPitch.setProgress(it)
        },{})
    }
}