package com.autel.ux.widget.gpssignal

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.core.view.isVisible
import com.autel.common.sdk.GpsSignalLevelEnum
import com.autel.drone.sdk.vmodelx.manager.data.ControlMode
import com.autel.ux.core.base.AutelSDKModel
import com.autel.ux.core.base.widget.ConstraintLayoutWidget
import com.autel.widget.R
import com.autel.widget.databinding.UxWidgetGpsSignalBinding

class GPSSignalWidget @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
) : ConstraintLayoutWidget(context, attrs) {

    private val widgetModel: GPSSignalWidgetModel by lazy {
        GPSSignalWidgetModel(AutelSDKModel.getInstance())
    }

    private lateinit var binding: UxWidgetGpsSignalBinding

    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        binding = UxWidgetGpsSignalBinding.inflate(LayoutInflater.from(context), this)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        widgetModel.setup()
    }

    override fun onDetachedFromWindow() {
        widgetModel.cleanup()
        super.onDetachedFromWindow()
    }

    override fun reactToModelChanges() {
        widgetModel.gpsSignalLevel.collectInWidget(::updateSignalLevel)
        widgetModel.gpsSatelliteCount.collectInWidget(::updateSatelliteCount)
        widgetModel.controlMode.collectInWidget {
            isVisible = it.controlMode == ControlMode.SINGLE
        }
    }

    private fun updateSignalLevel(level: GpsSignalLevelEnum) {
        when (level) {
            GpsSignalLevelEnum.GPS_SIGNAL_LEVEL_NONE -> {
                binding.tvGpsSignalStrength.text = context.getString(R.string.common_text_gps_tag_none)
                binding.tvGpsSignalStrength.setTextColor(context.getColor(R.color.common_color_white_50))
                binding.tvGpsCount.setTextColor(context.getColor(R.color.common_color_white_50))
                binding.ivGpsSignal.setImageResource(R.drawable.mission_ic_gps_signal_disable)
            }

            GpsSignalLevelEnum.GPS_SIGNAL_LEVEL_WEAK -> {
                binding.tvGpsSignalStrength.text = context.getString(R.string.common_text_gps_tag_weak)
                binding.tvGpsSignalStrength.setTextColor(context.getColor(R.color.common_color_red))
                binding.tvGpsCount.setTextColor(context.getColor(R.color.common_color_red))
                binding.ivGpsSignal.setImageResource(R.drawable.mission_ic_gps_signal_red)
            }

            GpsSignalLevelEnum.GPS_SIGNAL_LEVEL_MIDDLE -> {
                binding.tvGpsSignalStrength.text = context.getString(R.string.common_text_gps_tag_normal)
                binding.tvGpsSignalStrength.setTextColor(context.getColor(R.color.common_color_FF771E))
                binding.tvGpsCount.setTextColor(context.getColor(R.color.common_color_FF771E))
                binding.ivGpsSignal.setImageResource(R.drawable.mission_ic_gps_signal_orange)
            }

            GpsSignalLevelEnum.GPS_SIGNAL_LEVEL_STRONG -> {
                binding.tvGpsSignalStrength.text = context.getString(R.string.common_text_gps_tag_strong)
                binding.tvGpsSignalStrength.setTextColor(context.getColor(R.color.common_color_3CE171))
                binding.tvGpsCount.setTextColor(context.getColor(R.color.common_color_3CE171))
                binding.ivGpsSignal.setImageResource(R.drawable.mission_ic_gps_signal_green)
            }
        }
    }

    private fun updateSatelliteCount(count: Int) {
        binding.tvGpsCount.text = "$count"
    }

}