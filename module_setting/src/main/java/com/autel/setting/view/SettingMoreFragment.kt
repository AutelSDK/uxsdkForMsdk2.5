package com.autel.setting.view

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.autel.common.base.BaseAircraftFragment
import com.autel.common.bean.LanguageTypeEnum
import com.autel.common.delegate.function.FunctionViewType
import com.autel.common.feature.location.CountryManager
import com.autel.common.feature.route.RouterDataKey
import com.autel.common.lifecycle.LiveDataBus
import com.autel.common.lifecycle.event.FunctionViewStyleEvent
import com.autel.common.listener.DisclaimerListener
import com.autel.common.manager.AppInfoManager
import com.autel.common.manager.AutelStorageManager
import com.autel.common.manager.CommonDialogManager
import com.autel.common.manager.StorageKey
import com.autel.common.utils.DeviceUtils
import com.autel.common.utils.LanguageUtils
import com.autel.common.widget.dialog.CommonTwoButtonDialog
import com.autel.common.widget.spinnerview.CommonSpinnerView
import com.autel.drone.sdk.vmodelx.device.IAutelDroneListener
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.drone.sdk.vmodelx.manager.OTAUpgradeManger
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.enums.RemoteIdStatusEnum
import com.autel.drone.sdk.vmodelx.manager.uas.UASRemoteIDManager
import com.autel.drone.sdk.vmodelx.module.upgrade.bean.ota.CheckResponseBean
import com.autel.log.AutelLog
import com.autel.setting.R
import com.autel.setting.databinding.SettingMoreFragmentBinding
import com.autel.setting.state.SwitchStateVM
import com.autel.setting.view.rid.RemoteIDActivity
import com.autel.common.utils.NetWorksUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * @Author create by LJ
 * @Date  2022/9/1 10:41
 *
 * 更多设置
 */
class SettingMoreFragment : BaseAircraftFragment() {

    companion object {
        const val TAG = "SettingMoreFragment"
    }

    private lateinit var binding: SettingMoreFragmentBinding
    private val switchVM: SwitchStateVM by activityViewModels()
    private val floatWindowList = mutableListOf<FunctionViewType>(FunctionViewType.Bar, FunctionViewType.FloatBall)//工具快捷操作类型
    private val languageList = ArrayList<LanguageTypeEnum>()//翻译词条
    private var languageIndex = 0//默认语言
    private var isCommit = false
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = SettingMoreFragmentBinding.inflate(LayoutInflater.from(context))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    override fun getData() {

    }

    override fun fixedFrequencyRefresh() {
        updateRidStatus()
    }

    private fun updateRidStatus() {
        if (AppInfoManager.isSupportRemoteId() && AppInfoManager.isSupportRemoteIdCountry()) {
            if (CountryManager.isJapanZone()) {
                val drone = DeviceUtils.singleControlDrone() ?: return
                if (!UASRemoteIDManager.get().getDataBlockStatus(drone)) {
                    binding.moreCitRemoteId.setEndText(getString(R.string.common_text_import))
                    return
                } else {
                    updateRIDStatus(drone)
                }
            } else {
                val device = DeviceManager.getDeviceManager().getFirstDroneDevice()
                val isOpenRid = device != null && UASRemoteIDManager.get().getUASRemoteIDWorkStatus(device) == RemoteIdStatusEnum.OPEN
                binding.moreCitRemoteId.setEndText(
                    getString(
                        if (isOpenRid) {
                            R.string.common_text_broadcasting
                        } else {
                            R.string.common_text_unbroadcasting
                        }
                    )
                )
            }
        }
    }

    private fun updateRIDStatus(device: IAutelDroneDevice?) {
        val isOpenRid = device != null && UASRemoteIDManager.get().getUASRemoteIDWorkStatus(device) == RemoteIdStatusEnum.OPEN
        binding.moreCitRemoteId.setEndText(
            getString(
                if (isOpenRid) {
                    R.string.common_text_broadcasting
                } else {
                    R.string.common_text_unbroadcasting
                }
            )
        )
    }

    private fun initRidCheck() {
        binding.llCheckRid.isVisible = true
        val list = java.util.ArrayList<String>()
        list.add(getString(R.string.common_text_rid_zone_other))
        list.add(getString(R.string.common_text_rid_zone_japan))
        list.add(getString(R.string.common_text_rid_zone_eu))
        list.add(getString(R.string.common_text_rid_zone_america))
        list.add(getString(R.string.common_text_rid_zone_china))
        binding.csvRidCheck.dataList = list
        val ridCheckIndex = AutelStorageManager.getPlainStorage().getIntValue(StorageKey.PlainKey.RID_CHECK_INDEX)
        binding.csvRidCheck.setDefaultText(ridCheckIndex)
        binding.csvRidCheck.setSpinnerViewListener {
            AutelStorageManager.getPlainStorage().setIntValue(StorageKey.PlainKey.RID_CHECK_INDEX, it)
            binding.moreCitRemoteId.isVisible = it != 0
            CountryManager.initRidAreaStrategy()
        }
    }

    private val upgradeListener = object : OTAUpgradeManger.UpgradeVersionListener {
        override fun onDeviceUpgrade(beanMap: HashMap<String, CheckResponseBean.Data>) {
            updateOTAVersion()
        }
    }


    override fun addListen() {
        super.addListen()
        if (AppInfoManager.isNeedOta()) {
            updateOTAVersion()
            OTAUpgradeManger.getInstance().addUpgradeVersionListener(upgradeListener)
            DeviceManager.getDeviceManager().addDroneListener(droneListener)
        }
    }

    override fun removeListen() {
        super.removeListen()
        if (AppInfoManager.isNeedOta()) {
            OTAUpgradeManger.getInstance().removeUpgradeVersionListener(upgradeListener)
            DeviceManager.getDeviceManager().removeDroneListener(droneListener)
        }
    }

    private fun updateOTAVersion() {
        lifecycleScope.launch(Dispatchers.Main) {
            var hasNewVersion = OTAUpgradeManger.getInstance().isRCNeedUpdate() || OTAUpgradeManger.getInstance().isDroneNeedUpdate()
            binding.moreSettingAbout.setRedDotShow(hasNewVersion)
        }
    }

    private val droneListener: IAutelDroneListener = object : IAutelDroneListener {
        override fun onDroneChangedListener(connected: Boolean, drone: IAutelDroneDevice) {
            //当飞机离线，再进行一次ota检查，看遥控器是否需要升级，然后更新关于右侧的小红点和关于里面版本升级的文案提示
            OTAUpgradeManger.getInstance().detectDeviceUpdateInfo(LanguageUtils.getAppLanguageType().name)
        }
    }

    private fun initView() {
        if (!DeviceUtils.isMainRC()) {
            binding.moreCitLightSetting.isVisible = false
            binding.civAiRecognition.isVisible = false
        }
        val isMainRc = DeviceUtils.isMainRC()
        binding.moreCitAdvance.isVisible = isMainRc
        if (isMainRc) {
            binding.moreCitAdvance.setOnItemClickListener {
                switchVM.addFragment(
                    SettingAdvanceFragment(),
                    resources.getString(R.string.common_text_setting_safety),
                    true
                )
            }
        }

        binding.moreCitUnitSetting.setOnItemClickListener {
            switchVM.addFragment(
                SettingUnitFragment(),
                resources.getString(R.string.common_text_more_unit_setting_title),
                true
            )
        }
        binding.moreCitLightSetting.setOnItemClickListener {
            switchVM.addFragment(
                SettingLightFragment(),
                resources.getString(R.string.common_text_more_light_setting_title),
                true
            )
        }

        binding.moreSettingAbout.setOnItemClickListener {
            switchVM.addFragment(
                SettingAboutFragment(),
                resources.getString(R.string.common_text_about),
                true
            )
        }

        //AI 识别类型 都能用
        binding.civAiRecognition.setOnClickListener {
            switchVM.addFragment(
                SettingChooseTargetFragment(),
                resources.getString(R.string.common_text_ai_recognition_settings),
                true
            )
        }

        binding.moreCitRemoteId.isVisible = AppInfoManager.isSupportRemoteId() && AppInfoManager.isSupportRemoteIdCountry() && DeviceUtils.isMainRC()
        binding.moreCitRemoteId.setEndArrowVisible(!CountryManager.isChinaZone())
        binding.moreCitRemoteId.setOnItemClickListener {
            if (CountryManager.isJapanZone()) { //日本地区需要有网络
                context?.let {
                    if (!NetWorksUtils.isInternetAvailable(it)) {
                        showToast(R.string.common_text_mycentre_not_intent)
                        return@setOnItemClickListener
                    }
                }
            }

            if (DeviceManager.getDeviceManager().isConnected()) {
                if (!CountryManager.isChinaZone()) {
                    startActivity(Intent(requireContext(), RemoteIDActivity::class.java))
                }
            } else {
                showToast(getString(R.string.common_sure_be_connected))
            }
        }

        binding.csisvQuickActions.updateSpinnerData(getFunctionQuickActionStrList())
        binding.csisvQuickActions.updateSpinnerTitleIndex(getDefaultFunctionIndex())
        binding.csisvQuickActions.setSpinnerSelectedListener(object : CommonSpinnerView.SpinnerViewListener {
            override fun onSelectPosition(position: Int) {
                if (position < floatWindowList.size) {
                    val viewType = floatWindowList.get(position)
                    AutelStorageManager.getPlainStorage().setIntValue(StorageKey.PlainKey.KEY_FUNCTION_QUICK_ACTION, viewType.index)
                    LiveDataBus.of(FunctionViewStyleEvent::class.java).switchViewStyle().post(viewType)
                }
            }
        })

        languageList.clear()
        languageList.add(LanguageTypeEnum.CHINA)
        languageList.add(LanguageTypeEnum.ENGLISH)
        //是否支持所有语言
        if (AppInfoManager.isSupportAllLanguage()) {
            languageList.add(LanguageTypeEnum.TAIWAN)
            languageList.add(LanguageTypeEnum.JAPAN)
            languageList.add(LanguageTypeEnum.SPANISH)
            languageList.add(LanguageTypeEnum.GERMANY)
            languageList.add(LanguageTypeEnum.RUSSIAN)
            languageList.add(LanguageTypeEnum.UKRAINE)
            languageList.add(LanguageTypeEnum.ARABIC)
            languageList.add(LanguageTypeEnum.HEBREW)
            languageList.add(LanguageTypeEnum.KOREAN)
            languageList.add(LanguageTypeEnum.FRENCH)
            languageList.add(LanguageTypeEnum.ITALIAN)
            languageList.add(LanguageTypeEnum.POLISH)
            languageList.add(LanguageTypeEnum.PORTUGUESE)
            languageList.add(LanguageTypeEnum.LATVIAN)
            languageList.add(LanguageTypeEnum.LITHUANIAN)
            languageList.add(LanguageTypeEnum.ESTONIAN)
            languageList.add(LanguageTypeEnum.FINNISH)
            languageList.add(LanguageTypeEnum.SWEDISH)
            languageList.add(LanguageTypeEnum.THAILAND)
            languageList.add(LanguageTypeEnum.TURKEY)
        } else {

            //是否支持乌语
            if (AppInfoManager.isSupportUkLanguage()) {
                languageList.add(LanguageTypeEnum.UKRAINE)
            }

        }
        languageIndex = getDefaultLanguageIndex()
        binding.csisvLanguageSetting.updateSpinnerData(getLanguageStrList())
        binding.csisvLanguageSetting.updateSpinnerTitleIndex(languageIndex)
        binding.csisvLanguageSetting.setSpinnerSelectedListener {
            if (languageIndex != it) {
                onOpenSelected(it)
            }
        }
//        binding.moreCitAdvance.isVisible = AppInfoManager.isSecurityProduct()

        //初始化手动选择rid区域
        if (AppInfoManager.isSupportRidCheck()) {
            initRidCheck()
        }

        initRidSwitch()
    }

    /**
     * rid 手动开关功能
     */
    private fun initRidSwitch() {
        //RID手动开关逻辑
        binding.cisRidSwitch.isVisible = AppInfoManager.isSupportRidSwitch() && DeviceUtils.isMainRC()
        val isEnable = AppInfoManager.isRidSwitchEnable()
        binding.cisRidSwitch.setCheckedWithoutListener(isEnable)
        binding.cisRidSwitch.setOnSwitchChangeListener {
            AutelLog.i(TAG, "cisRidSwitch -> it=$it")
            if (!it) {
                CommonDialogManager.showDisclaimerDialog(object : DisclaimerListener {
                    override fun onCallBack(isCommit: Boolean) {
                        AutelLog.i(TAG, "DisclaimerDialog -> onCallBack isCommit=$isCommit")
                        if (isCommit) {
                            AutelStorageManager.getPlainStorage().setBooleanValue(StorageKey.PlainKey.KEY_RID_SWITCH, it)
                            UASRemoteIDManager.get().setUASRemoteIDEnable(it)
                        } else {
                            binding.cisRidSwitch.setCheckedWithoutListener(!it)
                        }
                    }
                })
            } else {
                AutelStorageManager.getPlainStorage().setBooleanValue(StorageKey.PlainKey.KEY_RID_SWITCH, it)
                UASRemoteIDManager.get().setUASRemoteIDEnable(it)
            }
            binding.moreCitRemoteId.isVisible = it && AppInfoManager.isSupportRemoteId()
        }
    }

    private fun getDefaultFunctionIndex(): Int {
        val index = AutelStorageManager.getPlainStorage().getIntValue(StorageKey.PlainKey.KEY_FUNCTION_QUICK_ACTION, 0)
        val type = FunctionViewType.find(index)
        val result = floatWindowList.indexOf(type)
        return if (result == -1) {
            0
        } else {
            result
        }
    }


    private fun onOpenSelected(index: Int) {
        isCommit = false
        CommonTwoButtonDialog(requireActivity()).apply {
            setMessage(getString(R.string.common_text_confirm_language_change))
            setLeftBtnStr(getString(R.string.common_text_cancel))
            setRightBtnStr(getString(R.string.common_text_confirm))
            setAutoDismiss(true)
            setOnDismissListener {
                if (isCommit) {
                    languageIndex = index
                } else {
                    binding.csisvLanguageSetting.updateSpinnerTitleIndex(languageIndex)
                }
            }
            setRightBtnListener {
                isCommit = true
                val locale = findLanguageLocaleByIndex(index)
                locale?.let { changeAppLanguage(it) }
            }
        }.show()
    }

    /**
     * 获取语言列表list
     */
    private fun getFunctionQuickActionStrList(): ArrayList<String> {
        val list = ArrayList<String>()
        for (x in floatWindowList) {
            list.add(getString(x.tag))
        }
        return list
    }

    /**
     * 获取语言列表list
     */
    private fun getLanguageStrList(): ArrayList<String> {
        val list = ArrayList<String>()
        for (x in languageList) {
            list.add(getString(x.tag))
        }
        return list
    }

    /**
     * 获取默认index
     */
    private fun getDefaultLanguageIndex(): Int {
        val defaultLanguage = LanguageUtils.getAppLanguageType()
        for (x in languageList.indices) {
            if (languageList[x] == defaultLanguage) return x
        }
        return 1//默认设置英语
    }

    /**
     * 根据index获取语言的locale
     */
    private fun findLanguageLocaleByIndex(index: Int): Locale? {
        if (index < 0 || index > languageList.size) return null
        return languageList[index].locale
    }

    /**
     * Change language of app and redirects to Home view
     */
    private fun changeAppLanguage(locale: Locale) {
        requireActivity().finish()
        LanguageUtils.changeSystemLanguage(locale)
        var startIntent = Intent(Intent(RouterDataKey.RESTART_ACTION))
        LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(startIntent)
    }
}