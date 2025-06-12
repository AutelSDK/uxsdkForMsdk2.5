package com.autel.setting.view

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.activityViewModels
import com.autel.common.base.BaseAircraftFragment
import com.autel.common.listener.CheckPwdListener
import com.autel.common.manager.AppInfoManager
import com.autel.common.manager.MiddlewareManager
import com.autel.common.model.serve.ResourceObserver
import com.autel.common.utils.DeviceUtils
import com.autel.drone.sdk.libbase.error.IAutelCode
import com.autel.common.utils.LanguageUtils
import com.autel.drone.sdk.vmodelx.device.IAutelDroneListener
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.drone.sdk.vmodelx.manager.OTAUpgradeManger
import com.autel.drone.sdk.vmodelx.manager.PayloadManager
import com.autel.drone.sdk.vmodelx.manager.keyvalue.callback.CommonCallbacks
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.common.bean.DroneVersionItemBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.common.enums.DroneComponentEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.common.enums.DroneComponentIdEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.payload.enums.PayloadType
import com.autel.drone.sdk.vmodelx.module.record.TAG_Flight
import com.autel.drone.sdk.vmodelx.module.upgrade.bean.ota.CheckResponseBean
import com.autel.log.AutelLog
import com.autel.sdk.debugtools.activity.AllTestingToolsActivity
import com.autel.setting.R
import com.autel.setting.business.SettingAboutVM
import com.autel.setting.databinding.SettingAboutFragmentBinding
import com.autel.setting.state.SwitchStateVM
import com.autel.common.utils.NetWorksUtils
import com.autel.setting.dialog.CheckPwdDialog


/**
 * @Author create by LJ
 * @Date 200/09/13 14:12
 * 关于
 */
class SettingAboutFragment : BaseAircraftFragment() {

    private lateinit var binding: SettingAboutFragmentBinding
    private val switchVM: SwitchStateVM by activityViewModels()
    private val settingAboutVm: SettingAboutVM by activityViewModels()
    private var hasNewOTAVersion = false
    private var clickCheckOTA = false

    private var hasNewAppVersion = false
    private var clickCheckApp = false

    private var clickTimes: Int = 0
    private val maxClickTimes: Int = 5
    private var lastClickTime: Long = 0L
    private var pwdDialog: CheckPwdDialog? = null

    private val upgradeListener = object : OTAUpgradeManger.UpgradeVersionListener {
        override fun onAppUpgrade(manual: Boolean, needUpgrade: Boolean, bean: CheckResponseBean?) {
            hasNewAppVersion = needUpgrade
            refreshAppUpgradeStatus(clickCheckApp)
        }

        override fun onDeviceUpgrade(beanMap: HashMap<String, CheckResponseBean.Data>) {
            var hasUpgrade = false
            beanMap.forEach {
                if (it.value.isNeed_upgrade) {
                    hasUpgrade = true
                }
            }
            hasNewOTAVersion = hasUpgrade
            refreshUpgradeStatus(clickCheckOTA)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = SettingAboutFragmentBinding.inflate(LayoutInflater.from(context))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initVisible()
    }

    private fun initVisible() {
        if (!DeviceUtils.isMainRC()) {
            binding.citAircraftVersion.visibility = View.GONE
            binding.citBatteryVersion.visibility = View.GONE
            binding.citGimbalVersion.visibility = View.GONE
            binding.aboutBatteryVersion.visibility = View.GONE
            binding.aboutGimbalSerialNumber.visibility = View.GONE
            binding.aboutFlySerialNumber.visibility = View.GONE
//            binding.updateAppVersion.visibility = View.GONE
//            binding.updateFirmwareVersion.visibility = View.GONE
            binding.updateFirmwareVersion.setBottomLineVisible(false)
            binding.aboutControllerSerialNumber.setBottomLineVisible(false)

            val topMargin = requireContext().resources.getDimensionPixelSize(R.dimen.common_10dp)
            binding.aboutControllerSerialNumber.updateLayoutParams<MarginLayoutParams> {
                this.topMargin = topMargin
            }
            binding.aboutControllerSerialNumber.setBackgroundResource(com.autel.common.R.drawable.common_item_bg_all)
        }
        //不连飞机也要请求显示遥控器版本和SN
        getData()
    }

    private fun initView() {
        binding.citAircraftVersion.setOnTitleClickListener {
            //快速点5次才能进去，默认点击不让进去
            val time = SystemClock.elapsedRealtime()
            if (time - lastClickTime < 1_000) {
                lastClickTime = time
                clickTimes++
                if (clickTimes >= maxClickTimes) {
                    switchVM.addFragment(
                        SettingFirmwareVersionFragment(),
                        resources.getString(R.string.common_text_firmware_version),
                        true
                    )
                    clickTimes = 0
                }
            } else {
                lastClickTime = time
                clickTimes = 1
            }
        }
        binding.citRemoteVersion.setOnItemClickListener {
            switchVM.addFragment(
                RemoteVersionFragment(),
                resources.getString(R.string.common_text_remote_version__title),
                true
            )
        }
        binding.aboutAppVersion.updateRightText(AppInfoManager.getAppVersionName())
        binding.aboutAppVersion.setOnItemClickListener {
            val time = SystemClock.elapsedRealtime()
            if (time - lastClickTime < 5_00) {
                lastClickTime = time
                clickTimes++
                if (clickTimes >= maxClickTimes) {
                    val intent = Intent(context, AllTestingToolsActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    clickTimes = 0
                }
            } else {
                lastClickTime = time
                clickTimes = 1
            }
        }

        binding.updateFirmwareVersion.setOnItemClickListener {
            if (!AppInfoManager.isNeedOta()) {
                showToast(R.string.common_text_already_new)
                return@setOnItemClickListener
            }
            val drone = DeviceUtils.singleControlDrone()
            if (hasNewOTAVersion) {
                if (drone?.let { DeviceUtils.isDroneFlying(it) } == true) {
                    showToast(R.string.common_text_firmware_upgrade_in_flying)
                } else {
//                    MiddlewareManager.otaModule.goFirmwareUpdatePage(requireContext())
                }
            } else {
                if (!NetWorksUtils.isInternetAvailable(requireContext())) {
                    showToast(R.string.common_text_network_disconnect)
                    return@setOnItemClickListener
                }

                //不检测飞机连接
                /*if(!isDroneConnect()){
                    showToast(R.string.common_text_aircraft_disconnect)
                    return@setOnItemClickListener
                }*/
                binding.updateFirmwareVersion.updateRightText(getString(R.string.common_text_update_checking))
                clickCheckOTA = true
                if (AppInfoManager.isNeedOta()) {
                    OTAUpgradeManger.getInstance().detectDeviceUpdateInfo(LanguageUtils.getAppLanguageType().name)
                }
            }
        }

        binding.updateAppVersion.setOnItemClickListener {
            if (!AppInfoManager.isNeedOta()) {
                showToast(R.string.common_text_already_new)
                return@setOnItemClickListener
            }
            if (hasNewAppVersion) {
                val drone = DeviceUtils.singleControlDrone()
                if (drone == null) {
//                    MiddlewareManager.otaModule.goAppUpdatePage(requireContext())
                } else {
                    if (DeviceUtils.isDroneFlying(drone)) {
                        showToast(R.string.common_text_firmware_upgrade_in_flying)
                    } else {
//                        MiddlewareManager.otaModule.goAppUpdatePage(requireContext())
                    }
                }
            } else {
                if (!NetWorksUtils.isInternetAvailable(requireContext())) {
                    showToast(R.string.common_text_network_disconnect)
                    return@setOnItemClickListener
                }

                //不检测飞机连接
                /*if(!isDroneConnect()){
                    showToast(R.string.common_text_aircraft_disconnect)
                    return@setOnItemClickListener
                }*/
                binding.updateAppVersion.updateRightText(getString(R.string.common_text_update_checking))
                clickCheckApp = true
                OTAUpgradeManger.getInstance().detectAppUpdateInfo(LanguageUtils.getAppLanguageType().name)
            }
        }
        binding.aboutBatteryVersion.setOnItemClickListener {
            val time = SystemClock.elapsedRealtime()
            if (time - lastClickTime < 5_00) {
                lastClickTime = time
                clickTimes++
                if (clickTimes >= maxClickTimes) {
                    context?.let {
                        if (AppInfoManager.isSupportDebugToolsPwd()) {
                            if (pwdDialog != null) return@setOnItemClickListener
                            pwdDialog = CheckPwdDialog(it)
                            pwdDialog?.show()
                            pwdDialog?.setOnConfirmListener(object : CheckPwdListener {
                                override fun onSuccess() {
                                    val intent = Intent(context, AllTestingToolsActivity::class.java)
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    startActivity(intent)
                                    clickTimes = 0
                                }
                            })
                            pwdDialog?.setOnDismissListener { pwdDialog = null }
                        } else {
                            val intent = Intent(context, AllTestingToolsActivity::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(intent)
                            clickTimes = 0
                        }
                    }

                }
            } else {
                lastClickTime = time
                clickTimes = 1
            }
        }
    }

    override fun getData() {
        settingAboutVm.querySystemDevicesInfo()
        refreshLoadSpeaker()
    }

    /**
     * 喊话器版本号显示
     */
    private fun refreshLoadSpeaker() {
        val payloadVersion = PayloadManager.getPayloadManager().getPayLoadVersion(PayloadType.PAYLOAD_LIGHT_SPEAKER)
        binding.citLoudSpeaker.isVisible = !TextUtils.isEmpty(payloadVersion)
        binding.citLoudSpeaker.setEndText("V$payloadVersion")

        PayloadManager.getPayloadManager().getSpeakerVersion(object : CommonCallbacks.CompletionCallbackWithParam<String>{
            override fun onSuccess(param: String?) {
                if (!isAdded) {
                    return
                }
                binding.citLoudSpeaker.setEndText("")
                binding.citLoudSpeaker.setEndArrowVisible(true)

                binding.citLoudSpeaker.setOnItemClickListener {
                    switchVM.addFragment(
                        LoudSpeakerVersionFragment.newInstance(payloadVersion, param),
                        resources.getString(R.string.common_text_loud_speaker),
                        true
                    )
                }
            }

            override fun onFailure(error: IAutelCode, msg: String?) {
                if (!isAdded) {
                    return
                }
                binding.citLoudSpeaker.setOnItemClickListener {
                    switchVM.addFragment(
                        LoudSpeakerVersionFragment.newInstance(payloadVersion, null),
                        resources.getString(R.string.common_text_loud_speaker),
                        true
                    )
                }
            }
        })
    }


    private fun refreshUpgradeStatus(showToast: Boolean) {
        if (!AppInfoManager.isNeedOta() || !isAdded) {
            return
        }
        requireActivity().runOnUiThread {
            if (hasNewOTAVersion) {
                binding.updateFirmwareVersion.setRightTextColor(R.color.common_color_secondary_ffda00)
                binding.updateFirmwareVersion.updateRightText(getString(R.string.common_text_has_new_version))
            } else {
                binding.updateFirmwareVersion.setRightTextColor(R.color.common_color_white)
                binding.updateFirmwareVersion.updateRightText("")
                if (showToast) {
                    clickCheckOTA = false
                    showToast(R.string.common_text_already_new)
                }
            }
        }
    }

    private fun refreshAppUpgradeStatus(showToast: Boolean) {
        if (!AppInfoManager.isNeedOta()) {
            return
        }
        requireActivity().runOnUiThread {
            if (hasNewAppVersion) {
                binding.updateAppVersion.setRightTextColor(R.color.common_color_secondary_ffda00)
                binding.updateAppVersion.updateRightText(getString(R.string.common_text_has_new_version))
            } else {
                binding.updateAppVersion.setRightTextColor(R.color.common_color_white)
                binding.updateAppVersion.updateRightText("")
                if (showToast) {
                    clickCheckApp = false
                    showToast(R.string.common_text_already_new)
                }
            }
        }
    }

    override fun onDroneChangedListener(connected: Boolean, drone: IAutelDroneDevice) {
        super.onDroneChangedListener(connected, drone)
        hasNewOTAVersion = false
    }

    override fun addListen() {
        super.addListen()
        if (AppInfoManager.isNeedOta()) {
            hasNewOTAVersion = OTAUpgradeManger.getInstance().isDroneNeedUpdate() || OTAUpgradeManger.getInstance().isRCNeedUpdate()
            refreshUpgradeStatus(false)
            hasNewAppVersion = OTAUpgradeManger.getInstance().isAppNeedUpdate()
            refreshAppUpgradeStatus(false)
            OTAUpgradeManger.getInstance().addUpgradeVersionListener(upgradeListener)
            DeviceManager.getDeviceManager().addDroneListener(droneListener)
        }

        settingAboutVm.systemProfileLD.observe(viewLifecycleOwner, ResourceObserver<List<DroneVersionItemBean>?>().apply {
            success {
                data.let {
                    it?.forEach { itemBean ->
                        when (itemBean.componentType) {
                            DroneComponentEnum.SYSTEM_PACK -> {
                                // OTAUpgradeManger.getInstance().mDroneVersion.set(itemBean.softwareVersion)
                                //来源为1的时，表示是飞机的数据
                                if (itemBean.dataSource == 1) {
                                    binding.citAircraftVersion.updateRightText(itemBean.softwareVersion ?: getString(R.string.common_text_no_value))
                                } else {
                                    binding.citRemoteVersion.updateRightText(itemBean.softwareVersion ?: getString(R.string.common_text_no_value))
                                }
                            }

                            DroneComponentEnum.TARGET_GIMBAL -> {
                                AutelLog.i(TAG_Flight,"TARGET_GIMBAL  -> componentSN=${itemBean.componentSN}")
                                binding.aboutGimbalSerialNumber.updateRightText(itemBean.componentSN ?: getString(R.string.common_text_no_value))
                            }

                            DroneComponentEnum.TARGET_FCS -> {
                                binding.aboutFlySerialNumber.updateRightText(itemBean.componentSN ?: getString(R.string.common_text_no_value))
                            }

                            DroneComponentEnum.TARGET_BATTERY -> {
                                binding.aboutBatteryVersion.updateRightText(itemBean.componentSN ?: getString(R.string.common_text_no_value))
                            }

                            DroneComponentEnum.COMPONENT_SYS_MANAGER_RC -> {
                            }

                            else -> {}
                        }
                        when (itemBean.componentID) {
                            DroneComponentIdEnum.GIMBAL -> {
                                binding.citGimbalVersion.updateRightText(itemBean.softwareVersion ?: getString(R.string.common_text_no_value))
                            }

                            DroneComponentIdEnum.BATTERY -> {
                                binding.citBatteryVersion.updateRightText(itemBean.softwareVersion ?: getString(R.string.common_text_no_value))
                            }

                            else -> {}
                        }
                    }
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        DeviceManager.getDeviceManager().getFirstRemoteDevice()?.getRemoteSn {
            binding.aboutControllerSerialNumber.updateRightText(it)
        }
    }

    override fun removeListen() {
        super.removeListen()
        if (AppInfoManager.isNeedOta()) {
            DeviceManager.getDeviceManager().removeDroneListener(droneListener)
            OTAUpgradeManger.getInstance().removeUpgradeVersionListener(upgradeListener)
        }
    }

    private val droneListener: IAutelDroneListener = object : IAutelDroneListener {
        override fun onDroneChangedListener(connected: Boolean, drone: IAutelDroneDevice) {
            //当飞机离线，再进行一次ota检查，看遥控器是否需要升级，然后更新关于右侧的小红点和关于里面版本升级的文案提示
            OTAUpgradeManger.getInstance().detectDeviceUpdateInfo(LanguageUtils.getAppLanguageType().name)
            //TODO zrp  多机情况
        }
    }
}