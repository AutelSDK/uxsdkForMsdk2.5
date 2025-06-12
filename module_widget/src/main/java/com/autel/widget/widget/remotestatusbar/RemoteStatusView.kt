package com.autel.widget.widget.remotestatusbar

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.autel.common.base.widget.ConstraintLayoutWidget
import com.autel.common.manager.AppInfoManager
import com.autel.common.sdk.GpsSignalLevelEnum
import com.autel.common.sdk.RemoteSignalLevelEnum
import com.autel.common.utils.DeviceUtils
import com.autel.widget.R
import com.autel.widget.databinding.WidgetLayoutRemoteStatusBinding
import com.autel.widget.widget.statusbar.bean.RemoteBattery
import com.autel.widget.widget.statusbar.bean.SignalStrength
import com.autel.widget.widget.statusbar.window.RemoteSignalPopupWindow

/**
 * Created by  2023/9/7
 */
class RemoteStatusView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0) :
    ConstraintLayoutWidget(context, attrs, defStyleAttr, defStyleRes) {

    private val binding: WidgetLayoutRemoteStatusBinding = WidgetLayoutRemoteStatusBinding.inflate(LayoutInflater.from(context), this, true)

    private var remoteSignalPopupWindow: RemoteSignalPopupWindow? = null

    init {
        binding.ivRemoteControlSignal.setOnClickListener {
            showRemoteSignalWindow(it)
        }
    }

    private val mWidgetModel: RemoteStatusWidgetModel by lazy {
        RemoteStatusWidgetModel()
    }

    override fun reactToModelChanges() {
        super.reactToModelChanges()
        mWidgetModel.remoteBattery.subscribe {
            refreshRemoteBattery(it)
        }
        mWidgetModel.signalStrength.subscribe {
            refreshSignalStrength(it)
        }
        mWidgetModel.droneConnectStatus.subscribe {
            if (it == true) {
                refreshSignalStrength(mWidgetModel.signalStrength.value)
            } else {
                refreshSignalStrength(SignalStrength(GpsSignalLevelEnum.GPS_SIGNAL_LEVEL_NONE, 0, RemoteSignalLevelEnum.REMOTE_SIGNAL_LEVEL_NONE))
            }
        }
    }

    private fun refreshRemoteBattery(batteryInfo: RemoteBattery) {
        if (batteryInfo.total == 0) {
            return
        }
        val percentValue = batteryInfo.current * 100 / batteryInfo.total
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

        binding.tvRemoteControlElectric.text = if (batteryInfo.current < 0 || batteryInfo.total <= 0) {
            context.getString(R.string.common_text_no_value)
        } else {
            "${percentValue}%"
        }
    }

    private fun refreshSignalStrength(it: SignalStrength) {
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
        remoteSignalPopupWindow?.setData(it.rcSignalLevel)
        //是否显示遥控器信号值
        binding.tvRcSignalQuality.isVisible = AppInfoManager.isShowRcSignalQuality()
        if (AppInfoManager.isShowRcSignalQuality()){
            val rcSignalQuality = DeviceUtils.getLocalRemoteDevice().getDeviceStateData().rcStateNtfyBean.rcSignalQuality
            binding.tvRcSignalQuality.text = rcSignalQuality.toString()
            val color = when(it.rcSignalLevel){
                RemoteSignalLevelEnum.REMOTE_SIGNAL_LEVEL_4,RemoteSignalLevelEnum.REMOTE_SIGNAL_LEVEL_5 -> {
                    R.color.common_color_secondary_3ce171
                }
                RemoteSignalLevelEnum.REMOTE_SIGNAL_LEVEL_2,RemoteSignalLevelEnum.REMOTE_SIGNAL_LEVEL_3 -> {
                    R.color.common_color_secondary_fa6400
                }
                else -> R.color.common_color_red
            }
            binding.tvRcSignalQuality.setTextColor(ContextCompat.getColor(context,color))
        }
    }

    private fun showRemoteSignalWindow(v: View) {
        if (mWidgetModel.droneConnectStatus.value == true) {
            if (remoteSignalPopupWindow == null) {
                remoteSignalPopupWindow = RemoteSignalPopupWindow(context)
                remoteSignalPopupWindow?.setData(mWidgetModel.signalStrength.value.rcSignalLevel)
            }
            remoteSignalPopupWindow?.showCenterDown(v)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        mWidgetModel.setup()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mWidgetModel.cleanup()
    }
}