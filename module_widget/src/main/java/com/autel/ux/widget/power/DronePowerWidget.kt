package com.autel.ux.widget.power

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import com.autel.ux.core.base.AutelSDKModel
import com.autel.ux.core.base.widget.ConstraintLayoutWidget
import com.autel.widget.R
import com.autel.widget.databinding.UxWidgetAircraftPowerBinding

class DronePowerWidget @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
) : ConstraintLayoutWidget(context, attrs) {

    private lateinit var binding: UxWidgetAircraftPowerBinding
    private val widgetModel: DronePowerWidgetModel by lazy {
        DronePowerWidgetModel(AutelSDKModel.getInstance())
    }

    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        binding = UxWidgetAircraftPowerBinding.inflate(LayoutInflater.from(context), this)
    }

    override fun reactToModelChanges() {
        widgetModel.dronePower.collectInWidget(::updatePower)
    }

    private fun updatePower(power: Int) {
        binding.tvAircraftElectric.text = if (power < 0) {
            context.getString(R.string.common_text_no_value)
        } else {
            "$power%"
        }
//        binding.pbPower.progress = power
    }

    /*
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
     */

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        widgetModel.setup()
    }

    override fun onDetachedFromWindow() {
        widgetModel.cleanup()
        super.onDetachedFromWindow()
    }
}