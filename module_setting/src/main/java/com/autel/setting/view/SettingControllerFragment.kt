package com.autel.setting.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.autel.common.base.BaseAircraftFragment
import com.autel.common.feature.route.RouteManager
import com.autel.common.feature.route.RouterConst
import com.autel.common.manager.AppInfoManager
import com.autel.common.manager.MiddlewareManager
import com.autel.common.utils.BusinessType
import com.autel.common.utils.DeviceUtils
import com.autel.common.widget.dialog.CommonTwoButtonDialog
import com.autel.drone.sdk.vmodelx.device.IAutelDroneListener
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.remotecontrol.enums.RcOperateModeEnum
import com.autel.setting.R
import com.autel.setting.business.SettingControlVM
import com.autel.setting.databinding.SettingRemoterFragmentBinding
import com.autel.setting.state.SwitchStateVM
import com.autel.setting.utils.matchUtils.MatchUtils

/**
 * @Author create by LJ
 * @Date 2022/9/1 10:28
 * 遥控设置
 */
class SettingControllerFragment : BaseAircraftFragment(), IAutelDroneListener {
    private lateinit var binding: SettingRemoterFragmentBinding
    private val switchVM: SwitchStateVM by activityViewModels()
    private val settingControlVM: SettingControlVM by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = SettingRemoterFragmentBinding.inflate(LayoutInflater.from(context))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    override fun getData() {

    }

    override fun addListen() {
        super.addListen()
        DeviceManager.getDeviceManager().addDroneListener(this)
    }

    override fun removeListen() {
        super.removeListen()
        DeviceManager.getDeviceManager().removeDroneListener(this)
    }

    private fun initView() {

        binding.settingCitRemoteCompassCalibration.isVisible = AppInfoManager.isSupportRemoteCompassWarn()

        if (!DeviceUtils.isMainRC()) {
            binding.settingCitRemoteControlHandle.visibility = View.GONE
            binding.settingCitRemoteModel.visibility = View.GONE
            binding.settingCitRemoteCustomKey.visibility = View.GONE
            binding.settingCitRemoteCalibration.background = resources.getDrawable(R.drawable.common_item_bg_all)
        }

        binding.settingCitRemoteModel.setOnItemClickListener {
            switchVM.addFragment(
                SettingControllerModelFragment(),
                context?.resources?.getString(R.string.common_text_remote_model), true
            )
        }
        binding.settingCitRemoteCalibration.setOnItemClickListener {
            //遥控校准
            DeviceUtils.singleControlDrone().let { MiddlewareManager.settingModule.jumpRemoteCalibration(requireContext(), it) }
        }

        binding.settingCitRemoteCustomKey.setOnItemClickListener {
            switchVM.addFragment(
                SettingControllerCustomKeyFragment(),
                context?.resources?.getString(R.string.common_text_remote_custom_key),
                true
            )
        }

        binding.settingCitRemoteCompassCalibration.setOnItemClickListener {
            MiddlewareManager.settingModule.jumpRemoteCompassCalibration(requireContext())
        }

        binding.settingCitRemoteControlHandle.setOnClickListener {
            switchVM.addFragment(
                SettingControllerExpFragment(),
                getString(R.string.common_text_remote_control_handle),
                true
            )
        }

        binding.settingCitRemoteMatch.setOnItemClickListener {
            if (DeviceUtils.isSingleControlDroneConnected()) {
                CommonTwoButtonDialog(requireContext()).apply {
                    setTitle(getString(R.string.common_text_connect_aircraft))
                    setMessage(getString(R.string.common_text_connect_new_aircraft_repair_tips))
                    setLeftBtnStr(getString(R.string.common_text_connect_aircraft_pair))
                    setRightBtnStr(getString(R.string.common_text_connect_aircraft_cancel_pair))
                    setLeftBtnListener {
                        startRcMatch()
                        binding.settingCitRemoteMatch.setEndText(getString(R.string.common_text_not_connected))
                    }
                    show()
                }
            } else {
                startRcMatch()
                binding.settingCitRemoteMatch.setEndText(getString(R.string.common_text_not_connected))
            }
        }
        binding.settingCitRemoteMatch.visibility = if (DeviceUtils.isBusinessTypeValid(BusinessType.SINGLEMATCH)) View.VISIBLE else View.GONE



        binding.cisRcVoice.setOnSwitchChangeListener {value ->
            settingControlVM.setRCVoiceSwitch(value, onSuccess = {

            }, onError = {
                showToast(R.string.common_text_set_failed)
                binding.cisRcVoice.setCheckedWithoutListener(!value)
            })
        }
    }

    private fun startRcMatch() {
        MatchUtils.startRcMatch(requireContext()) {

        }
    }

    override fun onResume() {
        super.onResume()
        refreshMatchView(DeviceUtils.isSingleControlDroneConnected())
        settingControlVM.getRCRockerControlMode(
            onSuccess = {
                when (it) {
                    RcOperateModeEnum.JAPANESE_HAND -> binding.settingCitRemoteModel.setRightText(getString(R.string.common_text_controller_model_japan))
                    RcOperateModeEnum.CHINESE_HAND -> binding.settingCitRemoteModel.setRightText(getString(R.string.common_text_controller_model_china))
                    else -> binding.settingCitRemoteModel.setRightText(getString(R.string.common_text_controller_model_usa))
                }
            },
            onError = {
                binding.settingCitRemoteModel.setRightText(getString(R.string.common_text_no_value))
            })
        settingControlVM.getRCVoiceSwitch(onSuccess = {
            binding.cisRcVoice.setCheckedWithoutListener(it)
        },{})
    }

    override fun onDroneChangedListener(connected: Boolean, drone: IAutelDroneDevice) {
        refreshMatchView(connected)

    }

    private fun refreshMatchView(connect: Boolean) {
        if (connect) {
            binding.settingCitRemoteMatch.setEndText(getString(R.string.common_text_server_connected))
        } else {
            binding.settingCitRemoteMatch.setEndText(getString(R.string.common_text_not_connected))
        }
    }

}