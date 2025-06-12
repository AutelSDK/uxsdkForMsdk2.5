package com.autel.ux.widget.positionmode

import android.content.Context
import android.util.AttributeSet
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.autel.common.utils.UIUtils.getColor
import com.autel.common.utils.UIUtils.getString
import com.autel.drone.sdk.vmodelx.manager.data.ControlMode
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.enums.FlightControlMainModeEnum
import com.autel.ux.core.base.AutelSDKModel
import com.autel.ux.core.base.widget.ConstraintLayoutWidget
import com.autel.widget.R
import com.autel.widget.databinding.UxWidgetPositionModeBinding
import kotlinx.coroutines.flow.combine

/**
 * 定位模式
 */
class PositionModeWidget @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
) : ConstraintLayoutWidget(context, attrs) {

    private lateinit var binding: UxWidgetPositionModeBinding

    private val widgetModel: PositionModeModel by lazy {
        PositionModeModel(AutelSDKModel.getInstance())
    }

    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        binding = UxWidgetPositionModeBinding.inflate(layoutInflater, this)
    }

    override fun reactToModelChanges() {
        combine(widgetModel.flightControlMainMode, widgetModel.productConnected, widgetModel.controlMode) { mainMode, connect, mode ->
            ((mainMode != FlightControlMainModeEnum.UNKNOWN && mainMode != FlightControlMainModeEnum.IOC) &&
                    connect && mode.controlMode == ControlMode.SINGLE) to mainMode
        }.collectInWidget {
            isInvisible = !it.first
            updateUI(it.second)
        }
    }

    private fun updateUI(mode: FlightControlMainModeEnum) {
        when (mode) {
            FlightControlMainModeEnum.UNKNOWN -> false
            FlightControlMainModeEnum.ATTITUDE -> {
                val entryStr = getString(R.string.common_text_attitude_mode)
                binding.tvPositioningMode.setTextColor(getColor(R.color.common_battery_setting_critical))
                binding.tvPositioningMode.text = entryStr
            }

            FlightControlMainModeEnum.GPS -> {
                val entryStr = getString(R.string.common_text_gnss_mode)
                binding.tvPositioningMode.text = entryStr
                binding.tvPositioningMode.setTextColor(getColor(R.color.common_battery_setting_safe))
            }

            FlightControlMainModeEnum.IOC -> false
            FlightControlMainModeEnum.STARPOINT -> {
                val entryStr = getString(R.string.common_text_visual_positioning_mode)
                binding.tvPositioningMode.setTextColor(getColor(R.color.common_battery_setting_safe))
                binding.tvPositioningMode.text = entryStr
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        widgetModel.setup()
    }

    override fun onDetachedFromWindow() {
        widgetModel.cleanup()
        super.onDetachedFromWindow()
    }
}