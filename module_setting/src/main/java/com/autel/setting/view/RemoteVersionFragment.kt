package com.autel.setting.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.autel.common.base.BaseAircraftFragment
import com.autel.common.manager.AppInfoManager
import com.autel.common.manager.appinfo.AppRunningDeviceEnum
import com.autel.common.model.serve.ResourceObserver
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.common.bean.DroneVersionItemBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.common.enums.DroneComponentIdEnum
import com.autel.setting.R
import com.autel.setting.business.SettingAboutVM
import com.autel.setting.databinding.SettingRemoteVersionFragmentBinding

/**
 * @author 
 * @date 2023/5/17
 * 遥控版本
 */
class RemoteVersionFragment : BaseAircraftFragment() {

    private lateinit var binding: SettingRemoteVersionFragmentBinding
    private val settingAboutVm: SettingAboutVM by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = SettingRemoteVersionFragmentBinding.inflate(LayoutInflater.from(context))
        return binding.root
    }

    override fun getData() {
        //版本号这里不要用getData，因为遥控器可以获取
    }

    override fun onResume() {
        super.onResume()
        settingAboutVm.querySystemDevicesInfo()
    }

    /**
     * 遥控子版本
     * 图传-COMPONENT_ID_SYS_MANAGER_SKYLINK_RC
     * 摇杆-COMPONENT_MCU
     * 平板系统-COMPONENT_ID_SYS_MANAGER_660_RC
     */
    override fun addListen() {
        settingAboutVm.systemProfileLD.observe(viewLifecycleOwner, ResourceObserver<List<DroneVersionItemBean>?>().apply {
            success {
                data.let {
                    it?.forEach { itemBean ->
                        when (itemBean.componentID) {
                            DroneComponentIdEnum.COMPONENT_ID_SYS_MANAGER_SKYLINK_RC -> {
                                binding.citDpsVersion.updateRightText(itemBean.softwareVersion ?: getString(R.string.common_text_no_value))
                                if (AppInfoManager.getAppRunningDevice() == AppRunningDeviceEnum.AutelRemotePad6_0) {
                                    binding.citPadVersion.updateRightText(itemBean.softwareVersion ?: getString(R.string.common_text_no_value))
                                }
                            }
                            DroneComponentIdEnum.COMPONENT_MCU -> {
                                binding.citRockerVersion.updateRightText(itemBean.softwareVersion ?: getString(R.string.common_text_no_value))
                            }
                            DroneComponentIdEnum.COMPONENT_ID_SYS_MANAGER_660_RC -> {
                                if (AppInfoManager.getAppRunningDevice()!= AppRunningDeviceEnum.AutelRemotePad6_0) {
                                    binding.citPadVersion.updateRightText(itemBean.softwareVersion ?: getString(R.string.common_text_no_value))
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        })
    }

}