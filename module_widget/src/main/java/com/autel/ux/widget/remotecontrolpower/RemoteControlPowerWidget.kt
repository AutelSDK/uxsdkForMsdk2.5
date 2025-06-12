package com.autel.ux.widget.remotecontrolpower

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.core.content.ContextCompat
import com.autel.ux.core.base.AutelSDKModel
import com.autel.ux.core.base.widget.ConstraintLayoutWidget
import com.autel.widget.R
import com.autel.widget.databinding.UxWidgetRemoteControlBinding
import kotlinx.coroutines.flow.combine

/**
 * 遥控器电量
 */
class RemoteControlPowerWidget @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
) : ConstraintLayoutWidget(context, attrs) {

    private lateinit var binding: UxWidgetRemoteControlBinding

    private val widgetModel: RemoteControlPowerModel by lazy {
        RemoteControlPowerModel(AutelSDKModel.getInstance())
    }

    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        binding = UxWidgetRemoteControlBinding.inflate(LayoutInflater.from(context), this)
    }

    override fun reactToModelChanges() {
        combine(widgetModel.remoteControlPower, widgetModel.remoteControlTotalPower) { cur, total ->
            cur to total
        }.collectInWidget {
            updatePower(it.first, it.second)
        }
    }

    private fun updatePower(cur: Int, total: Int) {
        val percentValue = cur * 100 / total
        binding.ivRemoteElectric.setImageLevel(percentValue)
        binding.tvRemoteControlElectric.text = if (cur < 0 || total <= 0) {
            context.getString(R.string.common_text_no_value)
        } else {
            "$percentValue%"
        }
        binding.tvRemoteControlElectric.setTextColor(
            ContextCompat.getColor(
                context, when (percentValue) {
                    in 0..10 -> R.color.common_color_red
                    in 16..30 -> R.color.common_color_FF771E
                    else -> R.color.common_color_3CE171
                }
            )
        )
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