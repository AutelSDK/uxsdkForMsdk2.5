package com.autel.setting.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import com.autel.common.base.BaseAircraftFragment
import com.autel.common.manager.AppInfoManager
import com.autel.common.model.serve.ResourceObserver
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.common.bean.DroneVersionItemBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.common.enums.DroneComponentIdEnum
import com.autel.setting.R
import com.autel.setting.business.SettingAboutVM
import com.autel.setting.databinding.SettingAircraftVersionFragmentBinding

/**
 * @author 
 * @date 2023/5/17
 * 飞机版本
 */
class AircraftVersionFragment : BaseAircraftFragment() {

    private lateinit var binding: SettingAircraftVersionFragmentBinding
    private val settingAboutVm: SettingAboutVM by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = SettingAircraftVersionFragmentBinding.inflate(LayoutInflater.from(context))
        binding.citRadarVersion.isVisible = AppInfoManager.isSupportRadarVersion()
        binding.citVision.isVisible = AppInfoManager.isSupportVisionVersion()
        return binding.root
    }

    override fun getData() {
        settingAboutVm.querySystemDevicesInfo()
    }

    /**
     * 飞机子版本
     * 相机-PLATFORM_VISION
     * 图传-PLATFORM_SKYLINK
     * 雷达-RADAR_DOWN
     * 飞控-FCS
     * 电调-ESC1
     */
    override fun addListen() {
        settingAboutVm.systemProfileLD.observe(viewLifecycleOwner, ResourceObserver<List<DroneVersionItemBean>?>().apply {
            success {
                data.let {
                    it?.forEach { itemBean ->
                        when (itemBean.componentID) {
                            DroneComponentIdEnum.PLATFORM_VISION -> {
                                if (AppInfoManager.isSupportVisionVersion()) {
                                    binding.citVision.updateRightText(itemBean.softwareVersion ?: getString(R.string.common_text_no_value))
                                } else {
                                    //不支持视觉，X用来显示相机版本
                                    if (!AppInfoManager.isModelS()) {
                                        binding.citCameraVersion.updateRightText(itemBean.softwareVersion ?: getString(R.string.common_text_no_value))
                                    }
                                }
                            }

                            DroneComponentIdEnum.PLATFORM_SKYLINK -> {
                                binding.citDspVision.updateRightText(itemBean.softwareVersion ?: getString(R.string.common_text_no_value))
                            }

                            DroneComponentIdEnum.RADAR_DOWN -> {
                                binding.citRadarVersion.updateRightText(itemBean.softwareVersion ?: getString(R.string.common_text_no_value))
                            }

                            DroneComponentIdEnum.FCS -> {
                                binding.citFcsVersion.updateRightText(itemBean.softwareVersion ?: getString(R.string.common_text_no_value))
                            }

                            DroneComponentIdEnum.ESC1 -> {
                                binding.citEscVersion.updateRightText(itemBean.softwareVersion ?: getString(R.string.common_text_no_value))
                            }

                            DroneComponentIdEnum.COMPONENT_ID_PLATFORM_CAMERA -> {
                                //H M S，相机版本使用该字段，X 使用PLATFORM_VISION
                                if (AppInfoManager.isModelM() || AppInfoManager.isModelH() || AppInfoManager.isModelS()) {
                                    binding.citCameraVersion.updateRightText(itemBean.softwareVersion ?: getString(R.string.common_text_no_value))
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