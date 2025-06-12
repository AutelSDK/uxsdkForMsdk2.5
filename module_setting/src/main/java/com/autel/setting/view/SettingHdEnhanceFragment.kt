package com.autel.setting.view

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.autel.common.base.BaseFragment
import com.autel.drone.sdk.v2.bean.LTELinkInfo
import com.autel.drone.sdk.v2.callback.LTELinkInfoListener
import com.autel.drone.sdk.v2.enums.NetworkStatus
import com.autel.setting.R
import com.autel.setting.business.SettingHDVM
import com.autel.setting.databinding.SettingHdEnhanceBinding
import com.autel.setting.state.SwitchStateVM

/**
 * @author 
 * @date 2025/4/9
 * 增强图传配置
 */
class SettingHdEnhanceFragment : BaseFragment() {

    private var binding: SettingHdEnhanceBinding? = null
    private val switchVM: SwitchStateVM by activityViewModels()
    private val settingHDVM: SettingHDVM by viewModels()
    private var droneConfigUrl = "" // 飞机服务器配置地址
    private var rcConfigUrl = "" // 遥控器服务器配置地址

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return SettingHdEnhanceBinding.inflate(inflater).also { binding = it }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding?.citEnhanceConfigTitle?.setOnItemClickListener {
            if (settingHDVM.isFlying()){
                showToast(R.string.common_text_please_land_the_plane_first)
                return@setOnItemClickListener
            }

            val fragment = SettingServerConfigFragment()
            fragment.arguments = bundleOf("droneConfigUrl" to droneConfigUrl,"rcConfigUrl" to rcConfigUrl)
            switchVM.addFragment(
                fragment,
                resources.getString(R.string.common_text_enhance_config_title),
                true
            )
        }
        settingHDVM.addLTELinkInfoListener(listener)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        settingHDVM.removeLTELinkInfoListener(listener)
    }

    private val listener = object : LTELinkInfoListener {
        override fun onLTELinkInfoUpdate(info: LTELinkInfo) {
            val droneStatus = info.getAircraftNetworkInfo().networkStatus
            val rcStatus = info.getRemoteControllerNetworkInfo().networkStatus

            //总状态显示
            val isNormal = info.getAircraftNetworkInfo().networkStatus == NetworkStatus.NETWORK_STATUS_CONNECTED
                    && info.getRemoteControllerNetworkInfo().networkStatus == NetworkStatus.NETWORK_STATUS_CONNECTED
            if (isNormal) {
                binding?.tvMonitorStatus?.text = getString(R.string.common_text_battery_text_norml)
                binding?.tvMonitorStatus?.setTextColor(resources.getColor(R.color.common_color_33CC33, null))
            } else {
                binding?.tvMonitorStatus?.text = getString(R.string.common_text_battery_text_exception)
                binding?.tvMonitorStatus?.setTextColor(resources.getColor(R.color.common_color_secondary_e60012, null))
            }

            //飞机和网络连接状态
            val isConnectNormal1 = droneStatus == NetworkStatus.NETWORK_STATUS_UNREACHABLE || droneStatus == NetworkStatus.NETWORK_STATUS_CONNECTED
            binding?.networkStatus1?.setBackgroundResource(if (isConnectNormal1) R.drawable.icon_network_normal else R.drawable.icon_network_abnormal)

            //飞机和服务器连接状态
            val isConnectNormal2 = droneStatus == NetworkStatus.NETWORK_STATUS_CONNECTED
            binding?.networkStatus2?.setBackgroundResource(if (isConnectNormal2) R.drawable.icon_network_normal else R.drawable.icon_network_abnormal)

            //遥控器和网络连接状态
            val isConnectNormal3 = rcStatus == NetworkStatus.NETWORK_STATUS_UNREACHABLE || rcStatus == NetworkStatus.NETWORK_STATUS_CONNECTED
            binding?.networkStatus3?.setBackgroundResource(if (isConnectNormal3) R.drawable.icon_network_normal else R.drawable.icon_network_abnormal)

            //遥控器和网络连接状态
            val isConnectNormal4 = rcStatus == NetworkStatus.NETWORK_STATUS_CONNECTED
            binding?.networkStatus4?.setBackgroundResource(if (isConnectNormal4) R.drawable.icon_network_normal else R.drawable.icon_network_abnormal)

            //飞机告警
            binding?.tvWarnDrone?.isVisible = droneStatus != NetworkStatus.NETWORK_STATUS_CONNECTED
            val droneWarnId = when (droneStatus) {
                NetworkStatus.NETWORK_STATUS_NONE -> R.string.common_text_enhance_drone_warn_2
                NetworkStatus.NETWORK_STATUS_SIM_NOT_INSERTED -> R.string.common_text_enhance_drone_warn_1
                NetworkStatus.NETWORK_STATUS_UNREACHABLE -> R.string.common_text_enhance_drone_warn_3
                else -> 0
            }
            if (droneWarnId != 0) {
                binding?.tvWarnDrone?.setText(droneWarnId)
            }

            //遥控器告警
            binding?.tvWarnRc?.isVisible = rcStatus != NetworkStatus.NETWORK_STATUS_CONNECTED
            val rcWarnId = when (rcStatus) {
                NetworkStatus.NETWORK_STATUS_NONE, NetworkStatus.NETWORK_STATUS_SIM_NOT_INSERTED -> R.string.common_text_enhance_rc_warn_1
                NetworkStatus.NETWORK_STATUS_UNREACHABLE -> R.string.common_text_enhance_rc_warn_2
                else -> 0
            }

            if (rcWarnId != 0) {
                binding?.tvWarnRc?.setText(rcWarnId)
            }

            //飞机服务器地址
            droneConfigUrl = info.getAircraftPrivatizationServerInfo().toURL()
            binding?.tvDroneConfigUrl?.text =
                if (TextUtils.isEmpty(droneConfigUrl)) getString(R.string.common_text_stytem_default) else droneConfigUrl

            //遥控器服务器地址
            rcConfigUrl = info.getRemoteControllerPrivatizationServerInfo().toURL()
            binding?.tvRcConfigUrl?.text =
                if (TextUtils.isEmpty(rcConfigUrl)) getString(R.string.common_text_stytem_default) else rcConfigUrl
        }

    }
}