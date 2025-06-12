package com.autel.widget.widget.statusbar

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.autel.common.base.widget.ConstraintLayoutWidget
import com.autel.common.manager.AppInfoManager
import com.autel.common.sdk.GpsSignalLevelEnum
import com.autel.common.sdk.RemoteSignalLevelEnum
import com.autel.common.utils.UIConstants
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.common.bean.WarningAtom
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.enums.FlightControlMainModeEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.enums.GearLevelEnum
import com.autel.widget.R
import com.autel.widget.databinding.ViewSingleAircraftStatusBarBinding
import com.autel.widget.widget.statusbar.manager.HistoryWarnModelManager
import com.autel.widget.widget.statusbar.warn.WarningBean
import com.autel.widget.widget.statusbar.wm.DeviceWarnAtomList
import com.autel.widget.widget.statusbar.wm.SingleStatusBarWidgetModel

/**
 * 飞行记录状态栏
 */
class FlightRecordStatusBarView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0) :
    ConstraintLayoutWidget(context, attrs, defStyleAttr, defStyleRes) {

    private val binding: ViewSingleAircraftStatusBarBinding

    val widgetModel: SingleStatusBarWidgetModel by lazy { SingleStatusBarWidgetModel() }

    private val warnManager: HistoryWarnModelManager by lazy { HistoryWarnModelManager() }

    private var observer = Observer<List<DeviceWarnAtomList>?> {
        val warningAtomList: List<WarningAtom> = it?.firstOrNull()?.warningAtomList ?: return@Observer
        warnManager.checkAtomLists(warningAtomList)
    }
    init {
        binding = ViewSingleAircraftStatusBarBinding.inflate(LayoutInflater.from(context), this)
        binding.ivBack.setOnClickListener {
            if (context is Activity) {
                context.finish()
            }
        }
        binding.ivObstacleAvoidance.isVisible = AppInfoManager.isSupportBarOA()
    }

    override fun reactToModelChanges() {
        // 避障相关
        widgetModel.visionStateInfo.subscribe {
            val iconId = if (it) {
                if (widgetModel.droneGear.value == GearLevelEnum.SPORT) {
                    R.drawable.mission_icon_vision_none
                } else {
                    R.drawable.mission_icon_vision_all
                }
            } else {
                R.drawable.mission_icon_vision_none
            }
            binding.ivObstacleAvoidance.setImageResource(iconId)
        }
        // 飞机档位
        widgetModel.droneGear.subscribe {
            val gear = when (it) {
                GearLevelEnum.SMOOTH -> {
                    context.getString(R.string.common_text_comfort_gear)
                }

                GearLevelEnum.SPORT -> {
                    context.getString(R.string.common_text_sport_gear)
                }

                else -> {
                    context.getString(R.string.common_text_standard_gear)
                }
            }
            binding.tvGear.text = gear
        }
        // 信号强度相关
        widgetModel.signalStrength.subscribe {
            binding.ivRemoteControlSignal.setImageResource(
                when (it.rcSignalLevel) {
                    RemoteSignalLevelEnum.REMOTE_SIGNAL_LEVEL_5 -> R.drawable.mission_ic_remote_control_signal5
                    RemoteSignalLevelEnum.REMOTE_SIGNAL_LEVEL_4 -> R.drawable.mission_ic_remote_control_signal4
                    RemoteSignalLevelEnum.REMOTE_SIGNAL_LEVEL_3 -> R.drawable.mission_ic_remote_control_signal3
                    RemoteSignalLevelEnum.REMOTE_SIGNAL_LEVEL_2 -> R.drawable.mission_ic_remote_control_signal2
                    RemoteSignalLevelEnum.REMOTE_SIGNAL_LEVEL_1 -> R.drawable.mission_ic_remote_control_signal1
                    RemoteSignalLevelEnum.REMOTE_SIGNAL_LEVEL_NONE -> R.drawable.mission_ic_remote_control_signal_disable
                }
            )

            if (it.gpsSignalLevel == GpsSignalLevelEnum.GPS_SIGNAL_LEVEL_NONE) {
                binding.tvGpsSignal.text = context.getString(R.string.common_text_gps_tag_none)
                binding.tvGpsSignal.setTextColor(context.getColor(R.color.common_color_white_50))
                binding.tvGpsCount.setTextColor(context.getColor(R.color.common_color_white_50))
                binding.ivMissionGps.setImageResource(R.drawable.mission_ic_gps_signal_disable)
            } else if (it.gpsSignalLevel == GpsSignalLevelEnum.GPS_SIGNAL_LEVEL_WEAK) {
                binding.tvGpsSignal.text = context.getString(R.string.common_text_gps_tag_weak)
                binding.tvGpsSignal.setTextColor(context.getColor(R.color.common_color_red))
                binding.tvGpsCount.setTextColor(context.getColor(R.color.common_color_red))
                binding.ivMissionGps.setImageResource(R.drawable.mission_ic_gps_signal_red)
            } else if (it.gpsSignalLevel == GpsSignalLevelEnum.GPS_SIGNAL_LEVEL_MIDDLE) {
                binding.tvGpsSignal.text = context.getString(R.string.common_text_gps_tag_normal)
                binding.tvGpsSignal.setTextColor(context.getColor(R.color.common_color_FF771E))
                binding.tvGpsCount.setTextColor(context.getColor(R.color.common_color_FF771E))
                binding.ivMissionGps.setImageResource(R.drawable.mission_ic_gps_signal_orange)
            } else {
                binding.tvGpsSignal.text = context.getString(R.string.common_text_gps_tag_strong)
                binding.tvGpsSignal.setTextColor(context.getColor(R.color.common_color_3CE171))
                binding.tvGpsCount.setTextColor(context.getColor(R.color.common_color_3CE171))
                binding.ivMissionGps.setImageResource(R.drawable.mission_ic_gps_signal_green)
            }
            binding.tvGpsCount.text = "${it.gpsCount}"
        }
        // 电量相关
        widgetModel.batteryInfo.subscribe {
            binding.tvAircraftElectric.text = if (it.droneBatteryPercentage == null || it.droneBatteryPercentage < 0) {
                context.getString(R.string.common_text_no_value)
            } else {
                "${it.droneBatteryPercentage}%"
            }
            if (it.droneBatteryPercentage != null && it.droneBatteryPercentage >= 0) {
                when {
                    it.droneBatteryPercentage <= (it.criticalLowBattery ?: UIConstants.DEFAULT_CRITICAL_LOW_BATTERY) -> {
                        //红色
                        binding.ivMissionAircraftElectric.setImageResource(R.drawable.mission_ic_aircraft_electric_red)
                        binding.tvAircraftElectric.setTextColor(ContextCompat.getColor(context, R.color.common_color_red))
                    }

                    it.droneBatteryPercentage <= (it.lowBattery ?: UIConstants.DEFAULT_LOW_BATTERY) -> {
                        //黄色
                        binding.ivMissionAircraftElectric.setImageResource(R.drawable.mission_ic_aircraft_electric_orange)
                        binding.tvAircraftElectric.setTextColor(ContextCompat.getColor(context, R.color.common_color_FF771E))
                    }

                    else -> {
                        //绿色
                        binding.ivMissionAircraftElectric.setImageResource(R.drawable.mission_ic_aircraft_electric_normal)
                        binding.tvAircraftElectric.setTextColor(ContextCompat.getColor(context, R.color.common_color_secondary_3ce171))
                    }
                }
            }
        }
        // 遥控器电量
        widgetModel.remoteBattery.subscribe {
            val percentValue = ((it.current.toFloat() / it.total.toFloat()) * 100).toInt()
            if (percentValue <= 10) {
                binding.ivMissionRemoteElectric.setImageResource(R.drawable.mission_ic_remote_control_electric_red)
                binding.tvRemoteControlElectric.setTextColor(ContextCompat.getColor(context, R.color.common_color_red))
            } else if (percentValue <= 30) {
                binding.ivMissionRemoteElectric.setImageResource(R.drawable.mission_ic_remote_control_orange)
                binding.tvRemoteControlElectric.setTextColor(ContextCompat.getColor(context, R.color.common_color_FF771E))
            } else {
                binding.ivMissionRemoteElectric.setImageResource(R.drawable.mission_ic_remote_control_electric_green)
                binding.tvRemoteControlElectric.setTextColor(ContextCompat.getColor(context, R.color.common_color_3CE171))
            }

            binding.tvRemoteControlElectric.text = if (it.current < 0 || it.total <= 0) {
                context.getString(R.string.common_text_no_value)
            } else {
                "${percentValue}%"
            }
        }

        warnManager.warnMsg.subscribe {
            updateStatusBar(it)
        }

        widgetModel.mainMode.subscribe { mainMode ->
            if (mainMode == FlightControlMainModeEnum.GPS) {
                binding.tvPositioningMode.isInvisible = false
                val entryStr = context.getString(R.string.common_text_gnss_mode)
                if (binding.tvPositioningMode.text.toString() != entryStr) {
                    binding.tvPositioningMode.text = entryStr
                    context?.let { binding.tvPositioningMode.setTextColor(it.getColor(R.color.common_battery_setting_safe)) }
//                    binding.tvFlyStatus.isVisible =
//                        !(seriousWarningNum == 0 && generalWarningNum == 0 && binding.tvFlyStatus.text.toString() == context.getString(R.string.common_text_gps_positioning_mode))
                }
            } else if (mainMode == FlightControlMainModeEnum.STARPOINT) {
                //飞控主模式为STARPOINT（室内定位：无定位，有视觉）时， 显示视觉定位模式模式
                binding.tvPositioningMode.isInvisible = false
                binding.tvFlyStatus.isVisible = true
                val entryStr = context.getString(R.string.common_text_visual_positioning_mode)
                if (binding.tvPositioningMode.text.toString() != entryStr) {
                    context?.let { binding.tvPositioningMode.setTextColor(it.getColor(R.color.common_battery_setting_safe)) }
                    binding.tvPositioningMode.text = entryStr
                }
            } else if (mainMode == FlightControlMainModeEnum.ATTITUDE) {
                //飞机主模式为ATTITUDE时，显示姿态模式
                binding.tvPositioningMode.isInvisible = false
                binding.tvFlyStatus.isVisible = true
                val entryStr = context.getString(R.string.common_text_attitude_mode)
                if (binding.tvPositioningMode.text.toString() != entryStr) {
                    context?.let { binding.tvPositioningMode.setTextColor(it.getColor(R.color.common_battery_setting_critical)) }
                    binding.tvPositioningMode.text = entryStr
                }
            } else {
                binding.tvFlyStatus.isVisible = true
                binding.tvPositioningMode.isInvisible = true
            }
        }
    }

    private fun updateStatusBar(warns: List<WarningBean>) {
        var seriousWarningNum = 0
        var generalWarningNum = 0
        warns.forEach {
            if (it.warnLevel == WarningBean.WarnLevel.HIGH_TIP) {
                seriousWarningNum++
            } else if (it.warnLevel == WarningBean.WarnLevel.MIDDLE_TIP) {
                generalWarningNum++
            }
        }
        var msg = warns.firstOrNull { it.warnLevel == WarningBean.WarnLevel.HIGH_TIP }?.content(context)
        if (msg.isNullOrEmpty()) {
            msg = warns.firstOrNull { it.warnLevel == WarningBean.WarnLevel.MIDDLE_TIP }?.content(context) ?: ""
        }
        binding.tvFlyStatus.text = msg
        if (seriousWarningNum > 0) {
            binding.clWarn.visibility = View.VISIBLE
            if (generalWarningNum > 0) {// 两种警告都有
                binding.tvSeriousWarn.visibility = View.VISIBLE
                binding.tvWarn.visibility = View.VISIBLE
                binding.tvSeriousWarn.text = seriousWarningNum.toString()
                binding.tvWarn.text = generalWarningNum.toString()
                binding.tvFlyStatus.setBackgroundResource(R.drawable.common_shape_solid_color_red_radius_4_5)
            } else {// 只有严重警告
                binding.tvSeriousWarn.visibility = View.VISIBLE
                binding.tvSeriousWarn.text = seriousWarningNum.toString()
                binding.tvWarn.visibility = View.GONE
                binding.tvFlyStatus.setBackgroundResource(R.drawable.common_shape_solid_color_red_radius_4_5)
            }
        } else if (generalWarningNum > 0) { // 只有一般警告
            binding.clWarn.visibility = View.VISIBLE
            binding.tvWarn.text = generalWarningNum.toString()
            binding.tvSeriousWarn.visibility = View.GONE
            binding.tvFlyStatus.setBackgroundResource(R.drawable.common_shape_solid_color_orange_radius_4_5)
        } else {
            binding.tvFlyStatus.text = context.getString(R.string.common_text_fly_safety)
            binding.clWarn.visibility = View.GONE
            binding.tvFlyStatus.setBackgroundResource(R.drawable.common_shape_solid_color_green_radius_4_5)
        }
    }

    fun initSafeWarn(){
        updateStatusBar(listOf())
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        widgetModel.warningAtomList.observeForever(observer)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        widgetModel.warningAtomList.removeObserver(observer)
    }

}