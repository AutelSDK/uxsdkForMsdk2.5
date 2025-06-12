package com.autel.setting.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.autel.common.base.BaseAircraftFragment
import com.autel.common.bean.CastScreenAspectRatio
import com.autel.common.bean.CastScreenType
import com.autel.common.extension.getCastScreenSelectSideLens
import com.autel.common.extension.getLensTypeName
import com.autel.common.extension.getSortValue
import com.autel.common.lifecycle.LiveDataBus
import com.autel.common.lifecycle.event.CastScreenEvent
import com.autel.common.lifecycle.event.SplitScreenEffectEvent
import com.autel.common.listener.DisclaimerListener
import com.autel.common.manager.AppInfoManager
import com.autel.common.manager.AutelStorageManager
import com.autel.common.manager.CommonDialogManager
import com.autel.common.manager.StorageKey
import com.autel.common.utils.DeviceUtils
import com.autel.common.widget.dialog.CommonSingleButtonDialog
import com.autel.common.widget.dialog.CommonTwoButtonDialog
import com.autel.drone.sdk.v2.bean.LTELinkInfo
import com.autel.drone.sdk.v2.callback.LTELinkInfoListener
import com.autel.drone.sdk.v2.enums.NetworkStatus
import com.autel.drone.sdk.vmodelx.enums.FrequencyBand
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.drone.sdk.vmodelx.manager.FrequencyBandManager
import com.autel.drone.sdk.vmodelx.manager.RtmpServiceManager
import com.autel.drone.sdk.vmodelx.manager.frequency.OnFrequencyBandListener
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.alink.enums.VideoTransMissionModeEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.nest.enums.ModemModeEnum
import com.autel.drone.sdk.vmodelx.module.camera.bean.LensTypeEnum
import com.autel.log.AutelLog
import com.autel.setting.R
import com.autel.setting.business.SettingHDVM
import com.autel.setting.databinding.SettingHdFragmentBinding
import com.autel.setting.state.SwitchStateVM
import kotlinx.coroutines.launch

/**
 * @Author create by LJ
 * @Date 2022/9/1 10:30
 *
 * 图传设置
 */
class SettingHDFragment : BaseAircraftFragment() {
    private val TAG = "SettingHDFragment"
    private lateinit var binding: SettingHdFragmentBinding
    private val settingHDVM: SettingHDVM by viewModels()

    //private val bandList = ArrayList<BandModeBean>()
    private val bandList = ArrayList<FrequencyBand>()
    private var curBandIndex = 0

    private val switchVM: SwitchStateVM by activityViewModels()
    private var isOpenHdEnhance = false
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = SettingHdFragmentBinding.inflate(LayoutInflater.from(context))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    override fun onVisible() {
        super.onVisible()
        if (!DeviceUtils.isSingleControlDroneConnected()) {
            showToast(R.string.common_text_aircraft_disconnect)
            return
        }
    }

    private fun initView() {
        if (!DeviceUtils.isMainRC()) {
            binding.csisvImageTransmissionFrequencyBand.visibility = View.GONE
            binding.csisvImageTransmissionMode.visibility = View.GONE
        }
        //图传模式设置
        loadImageTransmissionModeSetting()

        //主遥控才能设置频段，其他只能查看
        FrequencyBandManager.get().addListener(listener)

        //图传分屏设置
        loadImageTransmissionSplitScreenSetting()
        loadImageTransmissionFullScreenSetting()

        //投屏方式设置
        loadVideoOutTypeSetting()

        //增强图传
        initHDEnhance()
    }

    private fun initHDEnhance() {
        binding.citHdEnhanceConfig.setEndText(getString(R.string.common_text_battery_text_norml))
        binding.citHdEnhanceConfig.setRightTextColor(R.color.common_color_33CC33)
        binding.cisHdEnhanceSwitch.setOnSwitchChangeListener { enable ->
            //当关闭增强图传时，要根据图传信号强度判断一下
            if (!enable) {
                if (settingHDVM.isRcSignalAvailable()) {
                    var isCommit = false
                    context?.let {
                        CommonTwoButtonDialog(it)
                            .apply {
                                setMessage(getString(R.string.common_text_enhance_close_tips))
                                setRightBtnStr(getString(R.string.common_text_close))
                                setRightBtnListener {
                                    isCommit = true
                                    dealHdEnhanceSwitch(enable)
                                }
                                setOnDismissListener {
                                    if (!isCommit) refreshHdEnhanceView()
                                }
                                show()
                            }
                    }
                } else {
                    context?.let {
                        CommonSingleButtonDialog(it)
                            .apply {
                                setMessage(getString(R.string.common_text_enhance_close_tips_1))
                                setButtonText(getString(R.string.common_text_mission_got_known))
                                setOnDismissListener {
                                    refreshHdEnhanceView()
                                }
                                show()
                            }
                    }
                }
            } else {
                dealHdEnhanceSwitch(enable)
            }
        }
        binding.citHdEnhanceConfig.setOnItemClickListener {
            switchVM.addFragment(
                SettingHdEnhanceFragment(),
                resources.getString(R.string.common_text_hd_enhance_config),
                true
            )
        }
        settingHDVM.addLTELinkInfoListener(lteListener)
    }

    private fun dealHdEnhanceSwitch(enable: Boolean) {
        settingHDVM.setHdEnhanceState(enable, onSuccess = {
            isOpenHdEnhance = enable
            refreshHdEnhanceView()
        }, onError = {
            showToast(R.string.common_text_set_failed)
            refreshHdEnhanceView()
        })
    }

    private val lteListener = object : LTELinkInfoListener {
        override fun onLTELinkInfoUpdate(info: LTELinkInfo) {
            AutelLog.i(TAG, "onLTELinkInfoUpdate -> info=$info")
            binding.llHdEnhance.isVisible = info.isSupportLTE() && AppInfoManager.isSupportHdEnhance()

            //总状态显示
            val isNormal = info.getAircraftNetworkInfo().networkStatus == NetworkStatus.NETWORK_STATUS_CONNECTED
                    && info.getRemoteControllerNetworkInfo().networkStatus == NetworkStatus.NETWORK_STATUS_CONNECTED
            if (isNormal) {
                binding.citHdEnhanceConfig.setEndText(getString(R.string.common_text_battery_text_norml))
                binding.citHdEnhanceConfig.setRightTextColor(R.color.common_color_33CC33)
            } else {
                binding.citHdEnhanceConfig.setEndText(getString(R.string.common_text_battery_text_exception))
                binding.citHdEnhanceConfig.setRightTextColor(R.color.common_color_secondary_e60012)
            }

        }

    }

    private val listener = object : OnFrequencyBandListener {
        override fun onChange(country: String?, list: List<FrequencyBand>) {
            AutelLog.i(TAG, "FrequencyBand onChange -> country=$country list=$list")
            initBandModeConfig(list)
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        FrequencyBandManager.get().removeListener(listener)
        settingHDVM.removeLTELinkInfoListener(lteListener)
    }

    /**
     * 加载图传分屏设置
     * */
    private fun loadImageTransmissionSplitScreenSetting() {
        val screenEffectList = listOf(
            getString(R.string.common_text_uniform_scale),
            getString(R.string.common_text_fit_screen)
        )
        binding.csisvImageTransmissionSplitScreen.updateSpinnerData(screenEffectList)
        binding.csisvImageTransmissionSplitScreen.updateSpinnerTitleIndex(
            AutelStorageManager.getPlainStorage()
                .getIntValue(StorageKey.PlainKey.KEY_SCREEN_EFFECT, 0)
        )
        binding.csisvImageTransmissionSplitScreen.setSpinnerSelectedListener { position ->
            AutelStorageManager.getPlainStorage()
                .setIntValue(StorageKey.PlainKey.KEY_SCREEN_EFFECT, position)
            LiveDataBus.of(SplitScreenEffectEvent::class.java).isUniformScale().post(position == 0)
        }
    }

    /**
     * 加载图传分屏设置
     * */
    private fun loadImageTransmissionFullScreenSetting() {
        val screenEffectList = listOf(
            getString(R.string.common_text_fit_screen),
            getString(R.string.common_text_original_proportion)
        )
        binding.csisvImageTransmissionFullScreen.updateSpinnerData(screenEffectList)
        binding.csisvImageTransmissionFullScreen.updateSpinnerTitleIndex(
            AutelStorageManager.getPlainStorage()
                .getIntValue(StorageKey.PlainKey.KEY_SCREEN_FULL_EFFECT, 0)
        )
        binding.csisvImageTransmissionFullScreen.setSpinnerSelectedListener { position ->
            AutelStorageManager.getPlainStorage()
                .setIntValue(StorageKey.PlainKey.KEY_SCREEN_FULL_EFFECT, position)
            LiveDataBus.of(SplitScreenEffectEvent::class.java).updateFullScreenStyle().post(position)
        }
    }

    /**
     * 加载图传模式设置
     * */
    private fun loadImageTransmissionModeSetting() {
        val droneDevice = DeviceUtils.singleControlDrone()
        val lengthType =
            droneDevice?.let { droneDevice -> DeviceUtils.getDefaultVisibleLensType(droneDevice) }
        // 动态获取能力集
        val transferModes = lengthType?.let { typeEnum ->
            droneDevice.getCameraAbilitySetManger().getCameraSupport2()?.getTransferMode(typeEnum)
        }
        val imageTransmissionList = mutableListOf<String>()
        if (transferModes?.isNotEmpty() == true) {
            for (item in transferModes) {
                if (item != VideoTransMissionModeEnum.SUPER) {
                    imageTransmissionList.add(getTransferMode(item))
                }
            }
        }
        binding.csisvImageTransmissionMode.updateSpinnerTitle(getString(R.string.common_text_fluent))
        binding.csisvImageTransmissionMode.updateSpinnerData(imageTransmissionList)
        binding.csisvImageTransmissionMode.setSpinnerSelectedListener { position ->
            if (RtmpServiceManager.getInstance().bCanPushVideoData) {
                showToast(getString(R.string.common_text_cannot_set_resolution_while_live))
                return@setSpinnerSelectedListener
            }
            settingHDVM.setALinkTransmissionMode(
                transferModes?.get(position) ?: VideoTransMissionModeEnum.LOW_LATENCY
            )
        }
    }

    /**
     * 更新图传模式显示
     * */
    private fun updateImageTransmissionMode(mode: VideoTransMissionModeEnum) {
        when (mode) {
            VideoTransMissionModeEnum.LOW_LATENCY -> {
                binding.csisvImageTransmissionMode.updateSpinnerTitle(getString(R.string.common_text_fluent))
            }

            VideoTransMissionModeEnum.HIGH_QUALITY -> {
                binding.csisvImageTransmissionMode.updateSpinnerTitle(getString(R.string.common_text_hd))
            }

            VideoTransMissionModeEnum.SUPER -> {
                binding.csisvImageTransmissionMode.updateSpinnerTitle(getString(R.string.common_text_uhd))
            }

            else -> {
                binding.csisvImageTransmissionMode.updateSpinnerTitle(getString(R.string.common_text_no_value))
            }
        }
    }

    /**
     * 能力集清晰度转字符串
     */
    private fun getTransferMode(item: VideoTransMissionModeEnum): String {
        return when (item) {
            VideoTransMissionModeEnum.LOW_LATENCY -> {
                getString(R.string.common_text_fluent)
            }

            VideoTransMissionModeEnum.HIGH_QUALITY -> {
                getString(R.string.common_text_hd)
            }

            VideoTransMissionModeEnum.SUPER -> {
                getString(R.string.common_text_uhd)
            }

            else -> {
                getString(R.string.common_text_fluent)
            }
        }
    }

    /**
     * 初始化合规频段
     */
    private fun initBandModeConfig(list: List<FrequencyBand>) {
        binding.csisvImageTransmissionFrequencyBand.isVisible = isMainRc() && isSupportCheckBandMode()
        binding.citBandMode.isVisible = !(isMainRc() && isSupportCheckBandMode())
        bandList.clear()
        bandList.addAll(list)
        if (isMainRc() && isSupportCheckBandMode()) {
            initMainRcBandMode()
        } else {
            initOtherRcBandMode()
        }

        //频段
        settingHDVM.getCurBandMode(onSuccess = {
            updateImageTransmissionFrequencyBand(it)
        }, {})
    }

    /**
     * 初始化其他遥控器
     */
    private fun initOtherRcBandMode() {

    }

    /**
     * 是否是主遥控
     */
    private fun isMainRc(): Boolean {
        return DeviceManager.getMultiDeviceOperator().isMainRC()
    }

    /**
     * 只有点对点模式才支持选择
     */
    private fun isSupportCheckBandMode(): Boolean {
        return DeviceManager.getDeviceManager()
            .getModemMode() == ModemModeEnum.P2P_MASTER_SLAVE_MODE
    }

    /**
     * 主遥控初始化
     */
    private fun initMainRcBandMode() {
        //图传频段设置
        if (bandList.size == 0) return
        binding.csisvImageTransmissionFrequencyBand.updateSpinnerData(getBandStrList())
        binding.csisvImageTransmissionFrequencyBand.updateSpinnerTitleIndex(0)
        binding.csisvImageTransmissionFrequencyBand.setSpinnerSelectedListener { position ->
            AutelLog.i(TAG, "position=$position bandList.size=${bandList.size}")
            if (position < bandList.size && curBandIndex != position) {
                if (CommonDialogManager.isNeedBandModeTips()) {
                    CommonDialogManager.showDisclaimerDialog(object : DisclaimerListener {
                        override fun onCallBack(isCommit: Boolean) {
                            AutelLog.i(
                                TAG,
                                "DisclaimerDialog -> onCallBack isCommit=$isCommit"
                            )
                            if (isCommit) {
                                CommonDialogManager.setNeedBandModeTips(false)
                                settingHDVM.setCurBandMode(
                                    bandList[position],
                                    onSuccess = {
                                        curBandIndex = position
                                        refreshHideBandModeView(position == 0)
                                    },
                                    onError = {
                                        showToast(R.string.common_text_set_failed)
                                        binding.csisvImageTransmissionFrequencyBand.updateSpinnerTitleIndex(
                                            curBandIndex
                                        )
                                    })
                            } else {
                                binding.csisvImageTransmissionFrequencyBand.updateSpinnerTitleIndex(
                                    curBandIndex
                                )
                            }
                        }
                    })
                } else {
                    CommonDialogManager.setNeedBandModeTips(false)
                    settingHDVM.setCurBandMode(bandList[position], onSuccess = {
                        curBandIndex = position
                        refreshHideBandModeView(position == 0)
                    }, onError = {
                        showToast(R.string.common_text_set_failed)
                        binding.csisvImageTransmissionFrequencyBand.updateSpinnerTitleIndex(
                            curBandIndex
                        )
                    })
                }

            }
        }
    }

    /**
     * 是否显示屏蔽频段
     */
    private fun refreshHideBandModeView(isShow: Boolean) {
//        binding.line1.isVisible = isShow && AppInfoManager.isSupportHideBandMode()
//        binding.citHideBandMode.isVisible = isShow && AppInfoManager.isSupportHideBandMode()
//        binding.rlHideBandMode.isVisible = isShow && AppInfoManager.isSupportHideBandMode()
    }

    /**
     * 更新图传频段显示
     * 如果不在支持的频段范围内，则需要做校正，校正为第一个
     * */
    private fun updateImageTransmissionFrequencyBand(tag: String) {
        if (isMainRc() && isSupportCheckBandMode()) {
            val index = findBandModeIndex(tag)
            binding.csisvImageTransmissionFrequencyBand.updateSpinnerTitleIndex(index)
            curBandIndex = index
        } else {
            binding.citBandMode.setEndText(tag)
        }
    }

    /**
     * 获取其中显示的频段文案
     */
    private fun getBandStrList(): ArrayList<String> {
        val strList = ArrayList<String>()
        bandList.forEach { strList.add(it.tag) }
        return strList
    }

    /**
     * 查询频段的位置
     */
    private fun findBandModeIndex(tag: String): Int {
        for (x in bandList.indices) {
            if (bandList[x].tag == tag) return x
        }
        return 0
    }

    override fun getData() {
        settingHDVM.getALinkTransmissionMode(
            onSuccess = {
                updateImageTransmissionMode(it)
            },
            onError = {
                updateImageTransmissionMode(VideoTransMissionModeEnum.UNKNOWN)
            }
        )

        settingHDVM.getHdEnhanceState(onSuccess = {
            isOpenHdEnhance = it
            refreshHdEnhanceView()
        }, onError = {
            isOpenHdEnhance = false
            refreshHdEnhanceView()
        })
    }

    private fun refreshHdEnhanceView() {
        binding.cisHdEnhanceSwitch.setCheckedWithoutListener(isOpenHdEnhance)
        binding.citHdEnhanceConfig.isVisible = isOpenHdEnhance
        binding.cisHdEnhanceSwitch.setBottomLineVisible(if (isOpenHdEnhance) View.VISIBLE else View.GONE)
    }

    private fun loadVideoOutTypeSetting() {
        binding.csisvVideoOutType.updateSpinnerData(
            listOf(
                getString(R.string.common_text_copy_screen),
                getString(R.string.common_text_cast_screen)
            )
        )

        val type = CastScreenType.fromValue(
            AutelStorageManager.getPlainStorage()
                .getIntValue(StorageKey.PlainKey.KEY_CAST_SCREEN_TYPE, 0)
        )
        updateCastScreenView(type)
        binding.csisvVideoOutType.updateSpinnerTitleIndex(type.value)
        binding.csisvVideoOutType.setSpinnerSelectedListener { position ->
            AutelStorageManager.getPlainStorage()
                .setIntValue(StorageKey.PlainKey.KEY_CAST_SCREEN_TYPE, position)
            LiveDataBus.of(CastScreenEvent::class.java).updateCastScreenType().post(CastScreenType.fromValue(position))

            updateCastScreenView(CastScreenType.fromValue(position))
        }

        binding.aspectRatio.updateSpinnerData(
            listOf(
                getString(R.string.common_text_adapt),
                getString(R.string.common_text_filling),
                getString(R.string.common_text_cover_the_screen)
            )
        )
        val aspectRatio = AutelStorageManager.getPlainStorage()
            .getIntValue(StorageKey.PlainKey.KEY_CAST_SCREEN_ASPECT_RATIO, CastScreenAspectRatio.ADAPT.value)

        binding.aspectRatio.updateSpinnerTitleIndex(aspectRatio)
        binding.aspectRatio.setSpinnerSelectedListener {
            AutelStorageManager.getPlainStorage()
                .setIntValue(StorageKey.PlainKey.KEY_CAST_SCREEN_ASPECT_RATIO, it)
            LiveDataBus.of(CastScreenEvent::class.java).updateCastScreenAspectRatio().post(CastScreenAspectRatio.fromValue(it))
        }

        val isShowFlyParams = AutelStorageManager.getPlainStorage().getBooleanValue(StorageKey.PlainKey.KEY_CAST_SCREEN_SHOW_FLY_PARAMS, true)
        binding.cisShowFlyParams.setCheckedWithoutListener(isShowFlyParams)
        binding.cisShowFlyParams.setOnSwitchChangeListener {
            AutelStorageManager.getPlainStorage().setBooleanValue(StorageKey.PlainKey.KEY_CAST_SCREEN_SHOW_FLY_PARAMS, it)
            LiveDataBus.of(CastScreenEvent::class.java).updateShowFlyParams().post(it)
        }

        val drone = DeviceUtils.singleControlDrone()
        val lens = drone?.getCameraAbilitySetManger()?.getLensList()?.sortedBy { it.getSortValue() } ?: listOf()
        binding.mainVideo.updateSpinnerData(lens.map { it.getLensTypeName(requireContext()) })
        val mainLens = LensTypeEnum.find(
            AutelStorageManager.getPlainStorage().getStringValue(StorageKey.PlainKey.KEY_CAST_MAIN_SCREEN, LensTypeEnum.Zoom.value)
                ?: LensTypeEnum.Zoom.value
        )
        val lensIndex = if (lens.contains(mainLens)) {
            lens.indexOf(mainLens)
        } else {
            val totalLens = LensTypeEnum.values().sortedBy { it.getSortValue() }
            totalLens.find { lens.contains(it) }?.let {
                lens.indexOf(it)
            } ?: 0
        }
        binding.mainVideo.updateSpinnerTitleIndex(lensIndex)
        binding.mainVideo.setSpinnerSelectedListener {
            AutelStorageManager.getPlainStorage().setStringValue(StorageKey.PlainKey.KEY_CAST_MAIN_SCREEN, lens[it].value)
            LiveDataBus.of(CastScreenEvent::class.java).updateMainCastScreen().post(lens[it])
            updateSideLens(getCastScreenSelectSideLens())
        }

        binding.cameraLens.setTitle(getString(R.string.common_text_small_screen_lens))
        binding.cameraLens.setOnClickListener {
            switchVM.addFragment(
                SettingSelectLensFragment(),
                resources.getString(R.string.common_text_small_screen_lens),
                true
            )
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                updateSideLens(getCastScreenSelectSideLens())
            }
        }
    }

    private fun updateSideLens(lens: List<LensTypeEnum>) {
        if (context == null) return
        binding.cameraLens.setSideText(lens.joinToString("、") { it.getLensTypeName(requireContext()) })
        binding.cameraLens.setTips(if (lens.isNotEmpty()) "" else getString(R.string.common_text_mission_please_select))
    }

    private fun updateCastScreenView(type: CastScreenType) {
        val isShow = type == CastScreenType.DRONE_CAMERA
        binding.layoutCastScreen.isVisible = isShow
        binding.csisvVideoOutType.setBackgroundResource(if (isShow) R.drawable.common_item_bg_only_top else R.drawable.common_item_bg_all)
        binding.csisvVideoOutType.updateBottomLineVisible(isShow)
    }
}