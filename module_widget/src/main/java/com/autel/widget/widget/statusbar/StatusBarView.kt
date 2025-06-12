package com.autel.widget.widget.statusbar

import android.content.Context
import android.os.SystemClock
import android.text.TextUtils
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import com.autel.common.base.widget.ConstraintLayoutWidget
import com.autel.common.constant.AppTagConst.StatusBarView
import com.autel.common.constant.AppTagConst.WarningTag
import com.autel.common.extension.asLiveData
import com.autel.common.lifecycle.SingleLiveEvent
import com.autel.common.listener.HiddenConnectListener
import com.autel.common.manager.AppInfoManager
import com.autel.common.manager.AutelStorageManager
import com.autel.common.manager.MiddlewareManager
import com.autel.common.manager.StorageKey
import com.autel.common.sdk.GpsSignalLevelEnum
import com.autel.common.sdk.RemoteSignalLevelEnum
import com.autel.common.utils.DeviceUtils
import com.autel.common.utils.GoogleTextToSpeechManager
import com.autel.common.utils.UIUtils.getColor
import com.autel.common.utils.UIUtils.getString
import com.autel.common.widget.GearLevelSwitchDialog
import com.autel.common.widget.toast.AutelToast
import com.autel.common.widget.toast.ToastBean
import com.autel.drone.sdk.vmodelx.enums.FrequencyBand
import com.autel.drone.sdk.vmodelx.manager.FrequencyBandManager
import com.autel.drone.sdk.vmodelx.manager.frequency.OnFrequencyBandListener
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.camera.enums.CardStatusEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.common.enums.WaringIdEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.enums.*
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.rtk.enums.RTKPositionTypeEnum
import com.autel.log.AutelLog
import com.autel.widget.R
import com.autel.widget.databinding.ViewStatusBarBinding
import com.autel.widget.widget.statusbar.bean.BatteryInfo
import com.autel.widget.widget.statusbar.bean.RemoteBattery
import com.autel.widget.widget.statusbar.manager.NewWarnModelManager
import com.autel.widget.widget.statusbar.warn.CheckEntry
import com.autel.widget.widget.statusbar.warn.WarningBean
import com.autel.widget.widget.statusbar.window.*
import com.autel.widget.widget.statusbar.wm.DeviceWarnAtomList
import com.autel.widget.widget.statusbar.wm.RealTimeStatusBarWidget
import com.autel.widget.widget.statusbar.wm.StatusBarViewInterface

/**
 *  状态栏
 */
class StatusBarView
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0,
) : ConstraintLayoutWidget(context, attrs, defStyleAttr, defStyleRes),
    View.OnClickListener,
    StatusBarViewInterface {
    private val binding: ViewStatusBarBinding

    private var warnPopWindow: WarningPopWindow? = null
    private var remoteSignalPopupWindow: RemoteSignalPopupWindow? = null
    private var rtkGnssSignalPopupWindow: RtkGnssSignalPopupWindow? = null
    private var aircraftElectricPopupWindow: AircraftElectricPopupWindow? = null
    private var clickTimes: Int = 0
    private val maxClickTimes: Int = 30
    private var lastClickTime: Long = 0L
    private val mWidgetModel = RealTimeStatusBarWidget()
    private var warnCount = 0
    private var seriousWarnCount = 0
    private var generalWarnCount = 0
    private var checkEntrys: List<CheckEntry>? = null

    // 是否为多次点击
    private val _multipleClicks = SingleLiveEvent<Void>()
    val multipleClicks = _multipleClicks.asLiveData()

    private fun updateMultipleClicks() {
        _multipleClicks.call()
    }

    private val checkEntryObserver =
        Observer<List<CheckEntry>> { checkEntryList ->
            updateWarnPopWindow(checkEntryList)
        }

    private fun updateWarnPopWindow(checkEntryList: List<CheckEntry>) {
        val count = updateWarningNumber(checkEntryList)
        updateStatusBar(checkEntryList)
        if (count == 0) {
            warnPopWindow?.dismiss()
        }

        checkEntryList.forEach { checkEntry ->
            val iterator = checkEntry.warns.iterator()
            while (iterator.hasNext()) {
                val warningBean = iterator.next()
                if (warningBean.tip is WarningBean.TipType.TipToast) {
                    iterator.remove()
                }
                if (warningBean.warnId == WaringIdEnum.UNKNOWN) {
                    iterator.remove()
                }
            }
        }
        val warns = ArrayList<WarningBean>()
        checkEntryList.forEach {
            it.warns.sortBy { warningBean ->
                warningBean.warnLevel
            }
            it.dialog.sortBy { warningBean ->
                warningBean.warnLevel
            }
            warns.addAll(it.warns)
            warns.addAll(it.dialog)
        }
        warnPopWindow?.updateWarnData(warns)
    }

    private var isOpenVision = false // 是否打开避障开关

    private val isShowWarnTips: Boolean

    private var observer = Observer<List<DeviceWarnAtomList>?> {
        if (it != null) {
            NewWarnModelManager.checkDroneWarningAtomList(it)
        } else {
            NewWarnModelManager.checkDroneWarningAtomList(mutableListOf())
        }
    }
    init {
        binding = ViewStatusBarBinding.inflate(LayoutInflater.from(context), this, true)
        val a = context.obtainStyledAttributes(attrs, R.styleable.StatusBarView)
        isShowWarnTips = a.getBoolean(R.styleable.StatusBarView_show_warn_tips, true)
        val defaultFont = a.getBoolean(R.styleable.StatusBarView_default_font, false)
        if (defaultFont){
            binding.tvAircraftElectric.setTypeface(null)
            binding.tvRemoteControlElectric.setTypeface(null)
        }
        a.recycle()
        initListener()
        initData()
        refreshVisionIcon()
        setMultipleClick()
    }

    /**
     * 国家码控件初始化
     */
    private fun initCountryCodeView() {
        // 国家码实时更新
        binding.tvCountryCode.visibility = if (AppInfoManager.isSupportCountryUpdate()) View.VISIBLE else View.GONE
        FrequencyBandManager.get().addListener(frequencyBandListener)
        binding.tvCountryCode.setOnClickListener { view: View? ->
            val infoDialog = BandInfoDialog(context)
            infoDialog.show()
        }
    }

    private val frequencyBandListener: OnFrequencyBandListener =
        object : OnFrequencyBandListener {
            override fun onChange(
                country: String?,
                list: List<FrequencyBand>,
            ) {
                AutelLog.i("StatusBarView", "frequencyBand onChange -> country=$country list=$list")
                binding.tvCountryCode.text = if (TextUtils.isEmpty(country)) "空" else country
            }
        }

//    private val bandModeCfgListener: BandModeCfgListener = object : BandModeCfgListener {
//        override fun onChange(code: String, list: CopyOnWriteArrayList<FrequencyInfoBean>) {
//            binding.tvCountryCode.text = if (TextUtils.isEmpty(code)) "空" else code
//        }
//    }

    override fun reactToModelChanges() {
        initObserver()
    }

    // 点击5次显示图传的调试界面
    private fun setMultipleClick() {
        binding.flightMode.setOnClickListener {
            val time = SystemClock.elapsedRealtime()
            if (time - lastClickTime < 1_000) {
                lastClickTime = time
                clickTimes++
                if (clickTimes >= maxClickTimes) {
                    updateMultipleClicks()
                    clickTimes = 0
                }
            } else {
                lastClickTime = time
                clickTimes = 1
            }
        }
    }

    /**
     * 初始化避障开关
     */
    private fun initVisionIcon() {
        refreshVisionIcon()
    }

    private fun initData() {
        warnPopWindow = WarningPopWindow(context)
        if (mWidgetModel.droneConnectStatus.value == false) {
            setViewOnDisconnected()
        }

        binding.tvGpsSignal.text = context.getString(R.string.common_text_gps_tag_none)
        binding.tvGpsCount.text = "0"
        binding.ivMissionGps.setImageResource(R.drawable.mission_ic_gps_signal_disable)
    }

    private fun initListener() {
        binding.tvFlyStatus.setOnClickListener(this)
        binding.ivRemoteControlSignal.setOnClickListener(this)
        binding.llGps.setOnClickListener(this)
        binding.clElectric.setOnClickListener(this)
        binding.tvGear.setOnClickListener(this)
        binding.ivObstacleAvoidance.setOnClickListener(this)
        binding.clWarn.setOnClickListener(this)
    }

    private val hiddenConnectListener =
        object : HiddenConnectListener {
            override fun onConnectStateChanged(
                isConnect: Boolean,
                role: Int,
                otherPower: Int,
            ) {
                binding.ivRelayConnect.isVisible = isConnect
            }
        }

    private fun initObserver() {
        mWidgetModel.rtkEnable.subscribe {
            AutelLog.i(StatusBarView, "rtkEnable: ${mWidgetModel.deviceName.value} $it")
            updateRTKStatus(
                mWidgetModel.rtkEnable.value,
                mWidgetModel.rtkSVCount.value,
                mWidgetModel.rtkFixStatus.value,
                mWidgetModel.rtkPosType.value,
                mWidgetModel.rtkSupport.value,
            )
        }
        mWidgetModel.rtkFixStatus.subscribe {
            AutelLog.i(StatusBarView, "rtkFixStatus: ${mWidgetModel.deviceName.value} $it")
            updateRTKStatus(
                mWidgetModel.rtkEnable.value,
                mWidgetModel.rtkSVCount.value,
                mWidgetModel.rtkFixStatus.value,
                mWidgetModel.rtkPosType.value,
                mWidgetModel.rtkSupport.value,
            )
        }
        mWidgetModel.rtkSVCount.subscribe {
            AutelLog.i(StatusBarView, "rtkSVCount: ${mWidgetModel.deviceName.value} $it")
            updateRTKStatus(
                mWidgetModel.rtkEnable.value,
                mWidgetModel.rtkSVCount.value,
                mWidgetModel.rtkFixStatus.value,
                mWidgetModel.rtkPosType.value,
                mWidgetModel.rtkSupport.value,
            )
        }
        mWidgetModel.rtkPosType.subscribe {
            AutelLog.i(StatusBarView, "rtkPosType: ${mWidgetModel.deviceName.value} $it")
            updateRTKStatus(
                mWidgetModel.rtkEnable.value,
                mWidgetModel.rtkSVCount.value,
                mWidgetModel.rtkFixStatus.value,
                mWidgetModel.rtkPosType.value,
                mWidgetModel.rtkSupport.value,
            )
        }
        mWidgetModel.rtkSupport.subscribe {
            AutelLog.i(StatusBarView, "rtkSupport: ${mWidgetModel.rtkSupport.value} $it")
            updateRTKStatus(
                mWidgetModel.rtkEnable.value,
                mWidgetModel.rtkSVCount.value,
                mWidgetModel.rtkFixStatus.value,
                mWidgetModel.rtkPosType.value,
                mWidgetModel.rtkSupport.value,
            )
        }

        mWidgetModel.visionStateInfo.subscribe {
            AutelLog.d(StatusBarView, "visionStateInfo $it")
            isOpenVision = it
            initVisionIcon()
        }

        NewWarnModelManager.observeForeverWarnModel(checkEntryObserver)
        // 切换档位
        mWidgetModel.droneGear.subscribe {
            AutelLog.i(StatusBarView, "droneGear:$it")
            val gear =
                when (it) {
                    GearLevelEnum.SMOOTH -> {
                        getString(R.string.common_text_comfort_gear)
                    }

                    GearLevelEnum.SPORT -> {
                        getString(R.string.common_text_sport_gear)
                    }

                    GearLevelEnum.LOW_SPEED -> {
                        getString(R.string.common_text_gear_low_speed)
                    }

                    else -> {
                        context.getString(R.string.common_text_standard_gear)
                    }
                }
            binding.tvGear.text = gear
            refreshVisionIcon()
        }
        mWidgetModel.cardStatusInfo.subscribe {
            AutelLog.i(StatusBarView, "cardStatusInfo:$it")
            binding.ivAircraftNoSdcard.isVisible = CardStatusEnum.isEnable(it) == false
        }

        mWidgetModel.signalStrength.subscribe {
            when (it.gpsSignalLevel) {
                GpsSignalLevelEnum.GPS_SIGNAL_LEVEL_NONE -> {
                    binding.tvGpsSignal.text = context.getString(R.string.common_text_gps_tag_none)
                    binding.tvGpsSignal.setTextColor(context.getColor(R.color.common_color_white_50))
                    binding.tvGpsCount.setTextColor(context.getColor(R.color.common_color_white_50))
                    binding.ivMissionGps.setImageResource(R.drawable.mission_ic_gps_signal_disable)
                }

                GpsSignalLevelEnum.GPS_SIGNAL_LEVEL_WEAK -> {
                    binding.tvGpsSignal.text = context.getString(R.string.common_text_gps_tag_weak)
                    binding.tvGpsSignal.setTextColor(context.getColor(R.color.common_color_red))
                    binding.tvGpsCount.setTextColor(context.getColor(R.color.common_color_red))
                    binding.ivMissionGps.setImageResource(R.drawable.mission_ic_gps_signal_red)
                }

                GpsSignalLevelEnum.GPS_SIGNAL_LEVEL_MIDDLE -> {
                    binding.tvGpsSignal.text =
                        context.getString(R.string.common_text_gps_tag_normal)
                    binding.tvGpsSignal.setTextColor(context.getColor(R.color.common_color_FF771E))
                    binding.tvGpsCount.setTextColor(context.getColor(R.color.common_color_FF771E))
                    binding.ivMissionGps.setImageResource(R.drawable.mission_ic_gps_signal_orange)
                }

                GpsSignalLevelEnum.GPS_SIGNAL_LEVEL_STRONG -> {
                    binding.tvGpsSignal.text =
                        context.getString(R.string.common_text_gps_tag_strong)
                    binding.tvGpsSignal.setTextColor(context.getColor(R.color.common_color_3CE171))
                    binding.tvGpsCount.setTextColor(context.getColor(R.color.common_color_3CE171))
                    binding.ivMissionGps.setImageResource(R.drawable.mission_ic_gps_signal_green)
                }
            }
            binding.tvGpsCount.text = "${it.gpsCount}"
            rtkGnssSignalPopupWindow?.setData(it)

            binding.ivRemoteControlSignal.setImageResource(
                when (it.rcSignalLevel) {
                    RemoteSignalLevelEnum.REMOTE_SIGNAL_LEVEL_5 -> R.drawable.mission_ic_remote_control_signal5
                    RemoteSignalLevelEnum.REMOTE_SIGNAL_LEVEL_4 -> R.drawable.mission_ic_remote_control_signal4
                    RemoteSignalLevelEnum.REMOTE_SIGNAL_LEVEL_3 -> R.drawable.mission_ic_remote_control_signal3
                    RemoteSignalLevelEnum.REMOTE_SIGNAL_LEVEL_2 -> R.drawable.mission_ic_remote_control_signal2
                    RemoteSignalLevelEnum.REMOTE_SIGNAL_LEVEL_1 -> R.drawable.mission_ic_remote_control_signal1
                    RemoteSignalLevelEnum.REMOTE_SIGNAL_LEVEL_NONE -> R.drawable.mission_ic_remote_control_signal_disable
                },
            )
            remoteSignalPopupWindow?.setData(it.rcSignalLevel)

            //是否显示遥控器信号值
            binding.tvRcSignalQuality.isVisible = AppInfoManager.isShowRcSignalQuality()
            if (AppInfoManager.isShowRcSignalQuality()) {
                val rcSignalQuality = DeviceUtils.getLocalRemoteDevice().getDeviceStateData().rcStateNtfyBean.rcSignalQuality
                binding.tvRcSignalQuality.text = rcSignalQuality.toString()
                val color = when (it.rcSignalLevel) {
                    RemoteSignalLevelEnum.REMOTE_SIGNAL_LEVEL_4, RemoteSignalLevelEnum.REMOTE_SIGNAL_LEVEL_5 -> {
                        R.color.common_color_secondary_3ce171
                    }

                    RemoteSignalLevelEnum.REMOTE_SIGNAL_LEVEL_2, RemoteSignalLevelEnum.REMOTE_SIGNAL_LEVEL_3 -> {
                        R.color.common_color_secondary_fa6400
                    }

                    else -> R.color.common_color_red
                }
                binding.tvRcSignalQuality.setTextColor(ContextCompat.getColor(context, color))
            }
        }

        mWidgetModel.batteryInfo.subscribe {
            refreshAircraftBattery(it)
            aircraftElectricPopupWindow?.setData(it)
        }
        mWidgetModel.remoteBattery.subscribe {
            refreshRemoteBattery(it)
        }

        mWidgetModel.environmentInfo.subscribe {
            if (it == EnvironmentEnum.LOW_BRIGHTNESS) {
                AutelToast.normalToast(
                    context,
                    R.string.common_text_brightness_loww_perception_close,
                )
            }
        }


        mWidgetModel.droneConnectStatus.subscribe {
            AutelLog.i(StatusBarView, "droneConnectStatus:$it")
            if (it == null) {
                return@subscribe
            }
            val connect = it == true
            if (connect) {
                updateIconVisible()
                updatePositionMode(listOf())
            } else {
                setViewOnDisconnected()
                AutelStorageManager
                    .getPlainStorage()
                    .setBooleanValue(StorageKey.PlainKey.KEY_IS_SHOW_INTELLIGENCE_LOW_BATTERY, true)
                NewWarnModelManager.clearCache()
            }
            refreshVisionIcon()
        }

        mWidgetModel.mainMode.subscribe { mainModeEnum ->
            AutelLog.i(StatusBarView, "mainMode:$mainModeEnum")
            checkEntrys?.let { entries ->
                updatePositionMode(entries)
            }
        }

        mWidgetModel.droneWorkMode.subscribe {
            AutelLog.i(StatusBarView, "droneWorkMode:$it")
            if (it == DroneWorkModeEnum.RETURN) {
                GoogleTextToSpeechManager
                    .instance()
                    .speak(context.getString(R.string.common_text_aircraft_is_returning), false)
            } else {
                // 如果从返航模式切换到其他模式，需要清除返航的提示，更新为告警
                checkEntrys?.let { entries ->
                    updatePositionMode(entries)
                }
            }
        }
        mWidgetModel.singleControlMode.subscribe {
            AutelLog.i(StatusBarView, "singleControlMode:$it")
            updateIconVisible()
        }
//        MiddlewareManager.workerHiddenProvider.addListener(hiddenConnectListener)

        initCountryCodeView()
    }

    private fun updateIconVisible() {
        val singleControl = mWidgetModel.singleControlMode.value == true
        binding.ivMissionGps.isVisible = singleControl
        binding.tvGpsCount.isVisible = singleControl
        binding.tvGpsSignal.isVisible = singleControl
        binding.clElectric.isVisible = singleControl
        binding.llGps.isVisible = singleControl
        binding.ivObstacleAvoidance.isVisible = singleControl
        binding.tvPositioningMode.isVisible =
            singleControl &&
                    mWidgetModel.droneConnectStatus.value == true
        binding.label.isVisible = singleControl && mWidgetModel.droneConnectStatus.value == true
        binding.flightMode.isVisible =
            singleControl &&
                    mWidgetModel.droneConnectStatus.value == true
        binding.tvGear.isVisible = singleControl
        binding.ivRemoteControlSignal.isVisible = singleControl
        if (singleControl) { // 单控下，没有SD卡就显示缺少SD卡图标
            binding.ivAircraftNoSdcard.isVisible =
                CardStatusEnum.isEnable(mWidgetModel.cardStatusInfo.value) == false
        } else { // 非单控不显示缺少SD卡图标
            binding.ivAircraftNoSdcard.isVisible = false
        }
        if (singleControl) {
            binding.clRtk.isVisible = mWidgetModel.rtkSupport.value
        } else {
            binding.clRtk.isVisible = false
        }
    }

    private fun updateRTKStatus(
        isEnable: Boolean,
        svCount: Int?,
        rtkFixStatus: Int?,
        posType: RTKPositionTypeEnum,
        showRtk: Boolean,
    ) {
        if (!showRtk) {
            binding.clRtk.visibility = View.GONE
            return
        }
        binding.clRtk.visibility = View.VISIBLE
        if (!isEnable) {
            binding.tvRtkState.text = context?.getString(R.string.common_text_rtk_status_none)
            binding.tvRtkState.background =
                context?.getDrawable(R.drawable.common_shape_solid_adadad_radius_2_4)
            binding.tvRtkFix.text =
                context?.getString(R.string.common_text_rtk_track_satellite, "0")
            binding.tvRtkFix.setTextColor(getColor(R.color.common_color_ad))
            return
        }
        when (rtkFixStatus) {
            0 -> {
                when (posType) {
                    RTKPositionTypeEnum.UNRECOGNIZED,
                    RTKPositionTypeEnum.UNKNOWN_POSITION,
                    -> {
                        binding.tvRtkState.text =
                            context?.getString(R.string.common_text_rtk_status_none)
                        binding.tvRtkState.background =
                            context?.getDrawable(R.drawable.common_shape_solid_ff0000_radius_2_4)
                        binding.tvRtkFix.setTextColor(getColor(R.color.common_color_ff0000))
                    }

                    RTKPositionTypeEnum.SINGLE_POINT -> {
                        binding.tvRtkState.text =
                            context?.getString(R.string.common_text_rtk_status_single)
                        binding.tvRtkState.background =
                            context?.getDrawable(R.drawable.common_shape_solid_ff771e_radius_2_4)
                        binding.tvRtkFix.setTextColor(getColor(R.color.common_color_FF771E))
                    }

                    RTKPositionTypeEnum.PSEUDORANGE,
                    RTKPositionTypeEnum.SBAS,
                    RTKPositionTypeEnum.L1_FLOAT,
                    RTKPositionTypeEnum.IONOSPHERIC_FLOAT,
                    RTKPositionTypeEnum.NARROW_FLOAT,
                    RTKPositionTypeEnum.INERTIAL_NAVIGATION,
                    RTKPositionTypeEnum.INERTIAL_SINGLE,
                    RTKPositionTypeEnum.INERTIAL_CARRIER_FLOAT,
                    RTKPositionTypeEnum.INERTIAL_CARRIER,
                    RTKPositionTypeEnum.PPP_FLOAT,
                    RTKPositionTypeEnum.PPP_FIX,
                    -> {
                        binding.tvRtkState.text =
                            context?.getString(R.string.common_text_rtk_status_float)
                        binding.tvRtkState.background =
                            context?.getDrawable(R.drawable.common_shape_solid_ff771e_radius_2_4)
                        binding.tvRtkFix.setTextColor(getColor(R.color.common_color_FF771E))
                    }

                    else -> {
                        binding.tvRtkState.text =
                            context?.getString(R.string.common_text_rtk_status_fix)
                        binding.tvRtkState.background =
                            context?.getDrawable(R.drawable.common_shape_solid_3ce171_radius_2_4)
                        binding.tvRtkFix.setTextColor(getColor(R.color.common_color_3CE171))
                    }
                }
            }

            1 -> {
                binding.tvRtkState.text = context?.getString(R.string.common_text_rtk_status_fix)
                binding.tvRtkState.background =
                    context?.getDrawable(R.drawable.common_shape_solid_3ce171_radius_2_4)
                binding.tvRtkFix.setTextColor(getColor(R.color.common_color_3CE171))
            }

            else -> {
                binding.tvRtkState.text = context?.getString(R.string.common_text_rtk_status_none)
                binding.tvRtkFix.setTextColor(getColor(R.color.common_color_ff0000))
            }
        }
        binding.tvRtkFix.text =
            context?.getString(R.string.common_text_rtk_track_satellite, "$svCount")
    }

    private fun refreshRemoteBattery(batteryInfo: RemoteBattery) {
        if (batteryInfo.total == 0) {
            return
        }
        val percentValue = batteryInfo.current * 100 / batteryInfo.total
        if (percentValue <= 10) {
            binding.ivMissionRemoteElectric.setImageResource(R.drawable.mission_ic_remote_control_electric_red)
            binding.tvRemoteControlElectric.setTextColor(
                ContextCompat.getColor(
                    context,
                    R.color.common_color_red,
                ),
            )
        } else if (percentValue <= 30) {
            binding.ivMissionRemoteElectric.setImageResource(R.drawable.mission_ic_remote_control_orange)
            binding.tvRemoteControlElectric.setTextColor(
                ContextCompat.getColor(
                    context,
                    R.color.common_color_FF771E,
                ),
            )
        } else {
            binding.ivMissionRemoteElectric.setImageResource(R.drawable.mission_ic_remote_control_electric_green)
            binding.tvRemoteControlElectric.setTextColor(
                ContextCompat.getColor(
                    context,
                    R.color.common_color_3CE171,
                ),
            )
        }

        binding.tvRemoteControlElectric.text =
            if (batteryInfo.current < 0 || batteryInfo.total <= 0) {
                context.getString(R.string.common_text_no_value)
            } else {
                "$percentValue%"
            }
    }

    /**
     * 刷新雷达避障icon
     *
     */
    private fun refreshVisionIcon() {
        binding.ivObstacleAvoidance.isVisible =
            AppInfoManager.isSupportBarOA() &&
                    mWidgetModel.singleControlMode.value == true
        var iconId = R.drawable.mission_icon_vision_off
        if (mWidgetModel.droneConnectStatus.value == true) {
            iconId =
                if (isOpenVision) {
                    if (mWidgetModel.droneGear.value == GearLevelEnum.SPORT) {
                        R.drawable.mission_icon_vision_none
                    } else {
                        R.drawable.mission_icon_vision_all
                    }
                } else {
                    R.drawable.mission_icon_vision_none
                }
        }

        binding.ivObstacleAvoidance.setImageResource(iconId)
    }

    /**
     * 刷新飞行器电量的显示
     * @param batteryRemainingPower 当前电量
     */
    private fun refreshAircraftBattery(batteryInfo: BatteryInfo) {
        val batteryRemainingPower = batteryInfo.droneBatteryPercentage
        val criticalLowBattery = batteryInfo.criticalLowBattery
        val lowBattery = batteryInfo.lowBattery
        binding.tvAircraftElectric.text =
            if (batteryRemainingPower == null || batteryRemainingPower < 0) {
                context.getString(R.string.common_text_no_value)
            } else {
                "$batteryRemainingPower%"
            }
        if (batteryRemainingPower != null && batteryRemainingPower >= 0) {
            if (criticalLowBattery != null && lowBattery != null) {
                when {
                    batteryRemainingPower <= criticalLowBattery -> {
                        // 红色
                        binding.ivMissionAircraftElectric.setImageResource(R.drawable.mission_ic_aircraft_electric_red)
                        binding.tvAircraftElectric.setTextColor(
                            ContextCompat.getColor(
                                context,
                                R.color.common_color_red,
                            ),
                        )
                    }

                    batteryRemainingPower <= lowBattery -> {
                        // 黄色
                        binding.ivMissionAircraftElectric.setImageResource(R.drawable.mission_ic_aircraft_electric_orange)
                        binding.tvAircraftElectric.setTextColor(
                            ContextCompat.getColor(
                                context,
                                R.color.common_color_FF771E,
                            ),
                        )
                    }

                    else -> {
                        // 绿色
                        binding.ivMissionAircraftElectric.setImageResource(R.drawable.mission_ic_aircraft_electric_normal)
                        binding.tvAircraftElectric.setTextColor(
                            ContextCompat.getColor(
                                context,
                                R.color.common_color_secondary_3ce171,
                            ),
                        )
                    }
                }
            }
        } else {
            binding.ivMissionAircraftElectric.setImageResource(R.drawable.mission_ic_aircraft_electric_disable)
            binding.tvAircraftElectric.setTextColor(ContextCompat.getColor(context, R.color.common_color_4f))
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.cl_warn, R.id.tv_fly_status -> {
                if (warnPopWindow?.canShow() == true) {
                    warnPopWindow?.updateWarnData()
                    warnPopWindow?.apply {
                        showAsDropDown(
                            binding.clWarn,
                            -context.resources.getDimensionPixelSize(R.dimen.common_10dp),
                            context.resources.getDimensionPixelSize(R.dimen.common_4dp),
                        )
                    }
                }
            }

            R.id.iv_remote_control_signal -> {
                if (mWidgetModel.droneConnectStatus.value == true) {
                    if (remoteSignalPopupWindow == null) {
                        remoteSignalPopupWindow = RemoteSignalPopupWindow(context)
                        remoteSignalPopupWindow?.setData(mWidgetModel.signalStrength.value.rcSignalLevel)
                    }
                    remoteSignalPopupWindow?.showCenterDown(v)
                }
            }

            R.id.ll_gps -> {
                if (mWidgetModel.droneConnectStatus.value == true) {
                    if (rtkGnssSignalPopupWindow == null) {
                        rtkGnssSignalPopupWindow = RtkGnssSignalPopupWindow(context)
                        rtkGnssSignalPopupWindow?.setData(mWidgetModel.signalStrength.value)
                    }
                    rtkGnssSignalPopupWindow?.showCenterDown(v)
                }
            }

            R.id.cl_electric -> {
                if (mWidgetModel.droneConnectStatus.value == true) {
                    if (aircraftElectricPopupWindow == null) {
                        aircraftElectricPopupWindow = AircraftElectricPopupWindow(context)
                        aircraftElectricPopupWindow?.setData(mWidgetModel.batteryInfo.value)
                    }
                    aircraftElectricPopupWindow?.showAtLocation(
                        v,
                        Gravity.TOP or Gravity.END,
                        context.resources.getDimensionPixelSize(R.dimen.common_115dp),
                        context.resources.getDimensionPixelSize(R.dimen.common_50dp),
                    )
                }
            }

            R.id.tv_gear -> {
                AutelLog.i("StatusBarView", "tv_gear clicked")
                // 新手模式不允许切换档位
                if (mWidgetModel.droneConnectStatus.value == false || mWidgetModel.isMainRc.value == false) return
                AutelLog.i("StatusBarView", "tv_gear clicked:${mWidgetModel.droneGear.value}")

                when (mWidgetModel.droneGear.value) {
                    GearLevelEnum.NORMAL -> {
                        GearLevelSwitchDialog(context).apply {
                            setOnConfirmBtnClick {
                                AutelLog.i(
                                    "StatusBarView",
                                    "tv_gear clicked1:${mWidgetModel.droneGear.value}",
                                )
                                mWidgetModel.setFlightGear(GearLevelEnum.SPORT)
                            }
                            show()
                        }
                    }

                    GearLevelEnum.SPORT -> {
                        mWidgetModel.setFlightGear(GearLevelEnum.LOW_SPEED)
                    }

                    GearLevelEnum.LOW_SPEED -> {
                        mWidgetModel.setFlightGear(GearLevelEnum.SMOOTH)
                    }

                    else -> {
                        mWidgetModel.setFlightGear(GearLevelEnum.NORMAL)
                    }
                }
            }

            R.id.iv_obstacle_avoidance -> {
            }
        }
    }

    private fun updateWarningNumber(checkEntrys: List<CheckEntry>): Int {
        var count = 0
        var seriousWarningCount = 0
        var generalWarningCount = 0
        checkEntrys.forEach {
            val seriousWarningNum = it.getSeriousWaringNum()
            val generalWarningNum = it.getGeneralWarningNum()
            seriousWarningCount += seriousWarningNum
            generalWarningCount += generalWarningNum
            count += (seriousWarningNum + generalWarningNum)
        }

        binding.clWarn.visibility = if (count > 0) VISIBLE else INVISIBLE
        binding.tvWarnNum.text = "$count"
        binding.tvWarnNum.setBackgroundResource(
            if (seriousWarningCount > 0) {
                R.drawable.mission_shape_red_17
            } else {
                R.drawable.mission_shape_orange_17
            },
        )
        this.warnCount = count
        this.seriousWarnCount = seriousWarningCount
        this.generalWarnCount = generalWarningCount
        return count
    }

    private fun showAircraftIsReturn() {
        val droneWorkMode = mWidgetModel.droneWorkMode.value
        if (droneWorkMode == DroneWorkModeEnum.RETURN) {
            binding.tvFlyStatus.setBackgroundResource(R.drawable.common_shape_solid_color_green_radius_4_5)
            updateWarnContent(getString(R.string.common_text_aircraft_is_returning))
        }
    }

    private fun updatePositionMode(checkEntrys: List<CheckEntry>) {
        val warnCount = updateWarningNumber(checkEntrys)
        val seriousWarningNum = this.seriousWarnCount
        val generalWarningNum = this.generalWarnCount
        val isDroneConnected = mWidgetModel.droneConnectStatus.value == true
        val mainMode = mWidgetModel.mainMode.value
        AutelLog.i(
            StatusBarView,
            "updatePositionMode warnCount:$warnCount $checkEntrys mainMode:$mainMode",
        )
        when (mainMode) {
            FlightControlMainModeEnum.GPS -> {
                // 飞控主模式为GPS时，显示GNSS模式
                binding.flightMode.gravity = Gravity.START or Gravity.CENTER_VERTICAL
                binding.flightMode.isVisible =
                    mWidgetModel.singleControlMode.value == true &&
                            mWidgetModel.droneConnectStatus.value == true
                binding.tvPositioningMode.isVisible =
                    mWidgetModel.singleControlMode.value == true &&
                            mWidgetModel.droneConnectStatus.value == true
                binding.label.isVisible =
                    mWidgetModel.singleControlMode.value == true &&
                            mWidgetModel.droneConnectStatus.value == true
                val entryStr = getString(R.string.common_text_gnss_mode)
                binding.tvPositioningMode.text = entryStr
                binding.tvPositioningMode.setTextColor(getColor(R.color.common_battery_setting_safe))
            }

            FlightControlMainModeEnum.STARPOINT -> {
                // 飞控主模式为STARPOINT（室内定位：无定位，有视觉）时， 显示视觉定位模式模式
                binding.flightMode.gravity = Gravity.START or Gravity.CENTER_VERTICAL
                binding.flightMode.isVisible =
                    mWidgetModel.singleControlMode.value == true &&
                            mWidgetModel.droneConnectStatus.value == true
                binding.tvPositioningMode.isVisible =
                    mWidgetModel.singleControlMode.value == true &&
                            mWidgetModel.droneConnectStatus.value == true
                binding.label.isVisible =
                    mWidgetModel.singleControlMode.value == true &&
                            mWidgetModel.droneConnectStatus.value == true
                val entryStr = getString(R.string.common_text_visual_positioning_mode)
                binding.tvPositioningMode.setTextColor(getColor(R.color.common_battery_setting_safe))
                binding.tvPositioningMode.text = entryStr
            }

            FlightControlMainModeEnum.ATTITUDE -> {
                // 飞机主模式为ATTITUDE时，显示姿态模式
                binding.flightMode.gravity = Gravity.START or Gravity.CENTER_VERTICAL
                binding.flightMode.isVisible =
                    mWidgetModel.singleControlMode.value == true &&
                            mWidgetModel.droneConnectStatus.value == true
                binding.tvPositioningMode.isVisible =
                    mWidgetModel.singleControlMode.value == true &&
                            mWidgetModel.droneConnectStatus.value == true
                binding.label.isVisible =
                    mWidgetModel.singleControlMode.value == true &&
                            mWidgetModel.droneConnectStatus.value == true
                val entryStr = getString(R.string.common_text_attitude_mode)
                binding.tvPositioningMode.setTextColor(getColor(R.color.common_battery_setting_critical))
                binding.tvPositioningMode.text = entryStr
            }

            else -> {
                binding.tvPositioningMode.visibility = INVISIBLE
                binding.label.visibility = INVISIBLE
                binding.flightMode.visibility = INVISIBLE
                binding.flightMode.gravity = Gravity.CENTER
            }
        }

        if (warnCount > 0) {
            val entryStr = warnContent(checkEntrys)
            updateWarnContent(entryStr)
        }
        if (seriousWarningNum > 0) {
            binding.clWarn.visibility = View.VISIBLE
            if (generalWarningNum > 0) { // 两种警告都有
                binding.tvFlyStatus.setBackgroundResource(R.drawable.common_shape_solid_color_red_radius_4_5)
            } else { // 只有严重警告
                binding.tvFlyStatus.setBackgroundResource(R.drawable.common_shape_solid_color_red_radius_4_5)
            }
        } else if (generalWarningNum > 0) { // 只有一般警告
            binding.clWarn.visibility = View.VISIBLE
            binding.tvFlyStatus.setBackgroundResource(R.drawable.common_shape_solid_color_orange_radius_4_5)
        } else {
            // 只有姿态模式才提示 无GNSS和视觉定位，请注意安全
            if (mainMode == FlightControlMainModeEnum.ATTITUDE) {
                binding.clWarn.visibility = View.VISIBLE
                binding.tvFlyStatus.setBackgroundResource(R.drawable.common_shape_solid_color_red_radius_4_5)
                updateWarnContent(getString(R.string.common_text_no_gnss_starpoint))
            } else {
                binding.clWarn.visibility = View.INVISIBLE
            }
        }

        if (seriousWarningNum > 0 || generalWarningNum > 0) {
            binding.clWarn.visibility = View.VISIBLE
            binding.tvWarnNum.visibility = VISIBLE
            binding.tvWarnNum.text = "${seriousWarningNum + generalWarningNum}"
            if (seriousWarningNum <= 0) {
                binding.tvWarnNum.setBackgroundResource(R.drawable.common_shape_solid_color_orange_radius_9)
            } else {
                binding.tvWarnNum.setBackgroundResource(R.drawable.common_shape_icon_red)
            }
        } else {
            binding.tvWarnNum.visibility = INVISIBLE
            binding.clWarn.visibility = View.INVISIBLE
        }
        if (!isDroneConnected) {
            binding.tvFlyStatus.setBackgroundResource(R.drawable.common_shape_solid_color_b2_radius_4_5)
            binding.tvFlyStatus.isVisible = true
            binding.ivWarn.isVisible = true
            binding.clWarn.isVisible = true
        }
        changePositioningModeViewWidth()
    }

    private fun warnContent(checkEntrys: List<CheckEntry>): String {
        val stringBuilder = StringBuilder()
        checkEntrys.forEachIndexed { index, it ->
            val entryStr = it.getContent(context)
            if (index == 0) {
                stringBuilder.append(entryStr)
            } else {
                stringBuilder.append("   ").append(entryStr)
            }
        }
        return stringBuilder.toString()
    }

    private fun changePositioningModeViewWidth() {
        // 此处经常因为告警的显示和消失导致位置模式的宽度变化，所以暂时注释掉调整，如果有问题再说
        val layoutParams = binding.tvPositioningMode.layoutParams
        AutelLog.i("StatusBarView", "changePositioningModeViewWidth:0")
        layoutParams.width = 0
        binding.tvPositioningMode.layoutParams = layoutParams
    }

    private fun updateStatusBar(checkEntrys: List<CheckEntry>) {
        updateWarningNumber(checkEntrys)
        val dialogFilterWarns = filterDialogWarns(checkEntrys)
        for (x in dialogFilterWarns) {
            if (!x.markedNew) {
                continue
            }
            showWarnMsg(x)
        }

        val toastFilterWarns = filterToastWarns(checkEntrys)
        for (x in toastFilterWarns) {
            if (!x.markedNew) {
                continue
            }
            showWarnMsg(x)
        }

        this.checkEntrys = checkEntrys
        updatePositionMode(checkEntrys)

        showPermanentsToastWarn(checkEntrys)
    }

    /**
     * 显示常驻Toast告警
     */
    private fun showPermanentsToastWarn(list: List<CheckEntry>) {
        if (!isShowWarnTips) return
        val warnToasts = mutableListOf<ToastBean>()
        list.forEach {
            warnToasts.addAll(
                it.permanentToast.map { warn ->
                    ToastBean(
                        msg = warn.content(context),
                        textColor = R.color.common_color_fee15d,
                        showTime = 0L,
                    )
                },
            )
        }
        AutelToast.showWarnToast(context, warnToasts)
    }

    /**
     * 筛选出多机所有弹窗告警
     * */
    private fun filterDialogWarns(checkEntrys: List<CheckEntry>): List<WarningBean> {
        val warningBeans = mutableListOf<WarningBean>()
        checkEntrys.forEach {
            it.dialog.forEach { warn ->
                val exist =
                    warningBeans.find { filterWarn ->
                        filterWarn.warnId == warn.warnId &&
                                filterWarn.deviceId == warn.deviceId &&
                                warn.tip is WarningBean.TipType.TipDialog
                    }
                if (exist == null) {
                    warningBeans.add(warn)
                    AutelLog.i(
                        WarningTag,
                        "deviceName：${warn.deviceName} 弹窗告警ID：${warn.warnId} warnNew:${warn.markedNew}",
                    )
                }
            }
        }
        return warningBeans
    }

    /**
     * 筛选出多机所有Toast告警
     * */
    private fun filterToastWarns(checkEntrys: List<CheckEntry>): List<WarningBean> {
        val warningBeans = mutableListOf<WarningBean>()
        checkEntrys.forEach {
            it.toast.forEach { warn ->
                val exist =
                    warningBeans.find { filterWarn ->
                        filterWarn.warnId == warn.warnId &&
                                filterWarn.deviceId == warn.deviceId &&
                                warn.tip is WarningBean.TipType.TipToast
                    }
                if (exist == null) {
                    warningBeans.add(warn)
                    AutelLog.i(
                        WarningTag,
                        "deviceName：${warn.deviceName} toast告警ID：${warn.warnId}",
                    )
                }
            }
        }
        return warningBeans
    }

    private var dialogMap = mutableMapOf<Int, WarnDialog>()

    private fun showWarnMsg(msg: WarningBean) {
        if (!isShowWarnTips) return
        val dialog = dialogMap[msg.deviceId]
        if (((msg.tip is WarningBean.TipType.TipDialog && dialog?.isShowing == true) || !msg.markedNew)) {
            // 列表展开，不显示弹框、Toast警告
            AutelLog.i(
                WarningTag,
                "showWarnMsg:return ${msg.warnId} markedNew:${msg.markedNew} isShowing:${dialog?.isShowing}",
            )
            return
        }
        AutelLog.i(
            WarningTag,
            "showWarnMsg deviceName:${msg.deviceName} deviceId:${msg.deviceId} warnId:${msg.warnId}",
        )
        when (msg.tip) {
            is WarningBean.TipType.TipDialog -> {
                if (msg.warnId == WaringIdEnum.LOW_BATTERY) { // 当正在返航时， 不提示低电量返航提示 , 停桨了也不提示
                    val droneDevice = mWidgetModel.getDrone(msg.deviceId)
                    if (droneDevice?.getDeviceStateData()?.flightControlData?.droneWorkMode == DroneWorkModeEnum.RETURN ||
                        droneDevice?.getDeviceStateData()?.flightControlData?.droneWorkMode == DroneWorkModeEnum.LAND ||
                        droneDevice?.getDeviceStateData()?.flightControlData?.flightMode == DroneFlightModeEnum.DISARM
                    ) {
                        AutelLog.i(
                            WarningTag,
                            "${msg.deviceName} 当正在返航时， 不提示低电量返航提示 , 停桨了也不提示。droneWorkMode:${droneDevice.getDeviceStateData().flightControlData.droneWorkMode}" +
                                    "flightMode:${droneDevice.getDeviceStateData().flightControlData.flightMode}",
                        )
                        return
                    }
                }
                val mPreDialog =
                    WarnDialog(context).apply {
                        setWarnTip(msg, DeviceUtils.allOnlineDrones().size)
                        setHandleAction(
                            object : WarnDialog.IAction {
                                override fun handleAction(action: WarningBean.Action) {
                                    handleAction(action, msg.deviceId) {
                                        dismiss()
                                        dialogMap.remove(msg.deviceId)
                                        if (!it) {
                                            AutelToast.normalToast(
                                                context,
                                                getString(R.string.common_text_set_failed),
                                            )
                                        }
                                    }
                                }
                            },
                        )
                        show()
                    }
                dialogMap[msg.deviceId] = mPreDialog
            }

            is WarningBean.TipType.TipToast -> {
                AutelToast.normalToast(context, msg.content(context))
            }

            is WarningBean.TipType.TipWindow -> {}
        }
    }

    private fun handleAction(
        action: WarningBean.Action,
        deviceId: Int,
        onSuccess: (Boolean) -> Unit,
    ) {
        when (action) {
            WarningBean.Action.IMU_CALI -> {
            }

            WarningBean.Action.GOLanding -> {
                mWidgetModel.landing(true, deviceId, onSuccess)
            }

            WarningBean.Action.RETURNLAND -> {
                mWidgetModel.autoBack(true, deviceId, onSuccess)
            }

            WarningBean.Action.CANCEL_RETURNLAND -> {
                mWidgetModel.autoBack(false, deviceId, onSuccess)
            }

            WarningBean.Action.COMPASS_CALI -> {
            }

            WarningBean.Action.CONNECTING_AIRCRAFT -> {
            }

            WarningBean.Action.RID_MSG -> {
                // 跳转到欧盟设置飞手ID界面
                MiddlewareManager.settingModule.jumpRemoteIdSetting(context)
                onSuccess.invoke(true)
            }

            WarningBean.Action.RC_COMPASS_CALL -> {
                MiddlewareManager.settingModule.jumpRemoteCompassCalibration(context)
                onSuccess.invoke(true)
            }

            WarningBean.Action.UOM -> {
//                MiddlewareManager.guideModule.jumpUOMActivity(context, false, deviceId, "CN")
                onSuccess.invoke(true)
            }

            else -> {}
        }
    }

    private fun setViewOnDisconnected() {
        binding.ivAircraftNoSdcard.isVisible = false
        binding.ivRemoteControlSignal.setImageResource(R.drawable.mission_ic_remote_control_signal_disable)
        binding.ivMissionGps.setImageResource(R.drawable.mission_ic_gps_signal_disable)
        binding.ivMissionAircraftElectric.setImageResource(R.drawable.mission_ic_aircraft_electric_disable)
        binding.tvPositioningMode.visibility = INVISIBLE
        binding.flightMode.gravity = Gravity.CENTER
        binding.flightMode.visibility = INVISIBLE
        binding.label.visibility = INVISIBLE
        binding.tvFlyStatus.setBackgroundResource(R.drawable.common_shape_solid_color_b2_radius_4_5)
        binding.tvFlyStatus.visibility = VISIBLE
        binding.clWarn.visibility = VISIBLE

        binding.clRtk.visibility = View.GONE
        binding.tvGpsCount.setTextColor(getColor(R.color.common_color_white_50))
        binding.tvGpsSignal.setTextColor(getColor(R.color.common_color_white_50))
        binding.tvAircraftElectric.setTextColor(getColor(R.color.common_color_white_50))
        binding.tvAircraftElectric.text = getString(R.string.common_text_no_value)
        binding.tvGpsSignal.text = getString(R.string.common_text_gps_tag_none)
        binding.tvGpsCount.text = "0"

        binding.ivWarn.isVisible = true
        binding.ivMissionGps.isVisible = true
        binding.tvGpsCount.isVisible = true
        binding.tvGpsSignal.isVisible = true
        binding.clElectric.isVisible = true
        binding.llGps.isVisible = true
        binding.ivObstacleAvoidance.isVisible = true
        binding.tvGear.isVisible = true
        binding.ivRemoteControlSignal.isVisible = true
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        mWidgetModel.setup()
        mWidgetModel.listener = this

        mWidgetModel.warningAtomList.observeForever(observer)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mWidgetModel.cleanup()
        mWidgetModel.listener = null
        dialogMap.values.forEach {
            it.dismiss()
        }
        dialogMap.clear()

        NewWarnModelManager.removeObserveForever(checkEntryObserver)
//        MiddlewareManager.workerHiddenProvider.removeListener(hiddenConnectListener)

        FrequencyBandManager.get().removeListener(frequencyBandListener)
        mWidgetModel.warningAtomList.removeObserver(observer)

    }

    private fun updateWarnContent(content: String) {
        if (binding.tvFlyStatus.text.toString() != content) {
            binding.tvFlyStatus.visibility = VISIBLE
            binding.tvFlyStatus.text = content
        }
    }

    override fun forceRefreshWarnList() {
        val warnAtomList = mWidgetModel.warningAtomList.value
        if (warnAtomList?.isNotEmpty() == true) {
            NewWarnModelManager.checkDroneWarningAtomList(warnAtomList)
        } else {
            NewWarnModelManager.checkDroneWarningAtomList(mutableListOf())
        }
    }
}
