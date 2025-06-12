package com.autel.ux.widget.lte

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.core.view.isVisible
import com.autel.common.widget.BasePopWindow
import com.autel.drone.sdk.v2.bean.NetworkType
import com.autel.drone.sdk.v2.enums.WlmLinkQualityLevel
import com.autel.drone.sdk.vmodelx.manager.data.ControlMode
import com.autel.log.AutelLog
import com.autel.ux.core.base.AutelSDKModel
import com.autel.ux.core.base.widget.ConstraintLayoutWidget
import com.autel.ux.core.base.widget.WidgetPopupWindow
import com.autel.widget.R
import com.autel.widget.databinding.UxWidgetLteBinding
import kotlinx.coroutines.flow.combine

class LTEWidget @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
) : ConstraintLayoutWidget(context, attrs) {

    private lateinit var binding: UxWidgetLteBinding

    private val widgetModel: LTEWidgetModel by lazy {
        LTEWidgetModel(AutelSDKModel.getInstance())
    }

    private val ltePopupWindow: BasePopWindow by lazy {
        WidgetPopupWindow.build(context, LTEPopupWindow(context))
    }

    init {
        setOnClickListener {
            ltePopupWindow.showOnAnchor(
                it, BasePopWindow.HorizontalPosition.CENTER,
                BasePopWindow.VerticalPosition.BELOW,
                0, 0, false
            )
        }
    }

    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        binding = UxWidgetLteBinding.inflate(LayoutInflater.from(context), this)
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
        combine(widgetModel.droneLetSignalFlow, widgetModel.droneLTETypeData) { level, type ->
            level to type
        }.collectInWidget {
            updateDroneLteStatus(it.first, it.second)
        }

        widgetModel.remoteLetSignalFlow.collectInWidget(::updateRemoteControlLteStatus)

        widgetModel.productConnected.collectInWidget {
//            isVisible = it
        }

        combine(widgetModel.controlMode, widgetModel.isSupportLTE) { mode, isSupport ->
            AutelLog.i(TAG, "currentConnectMode: $mode , isSupport: $isSupport")
            mode.controlMode == ControlMode.SINGLE && isSupport
        }.collectInWidget {
            isVisible = it
        }

        widgetModel.remoteControlNetworkStatus.collectInWidget {
            AutelLog.i(TAG, "remote net change :$it")
        }
    }

    private fun updateDroneLteStatus(level: WlmLinkQualityLevel, type: NetworkType) {
        binding.droneLte.setImageResource(getDroneQualityLevelIcon(level, type))
    }

    private fun getDroneQualityLevelIcon(level: WlmLinkQualityLevel, type: NetworkType): Int {
        return if (type == NetworkType.NETWORK_LTE_5G) {
            when (level) {
                WlmLinkQualityLevel.NO_SIGNAL -> R.drawable.ux_icon_drone_5g_signal_0
                WlmLinkQualityLevel.LEVEL_1 -> R.drawable.ux_icon_drone_5g_signal_1
                WlmLinkQualityLevel.LEVEL_2 -> R.drawable.ux_icon_drone_5g_signal_2
                WlmLinkQualityLevel.LEVEL_3 -> R.drawable.ux_icon_drone_5g_signal_3
                WlmLinkQualityLevel.LEVEL_4 -> R.drawable.ux_icon_drone_5g_signal_4
                WlmLinkQualityLevel.LEVEL_5 -> R.drawable.ux_icon_drone_5g_signal_5
            }
        } else {
            when (level) {
                WlmLinkQualityLevel.NO_SIGNAL -> R.drawable.ux_icon_drone_4g_signal_0
                WlmLinkQualityLevel.LEVEL_1 -> R.drawable.ux_icon_drone_4g_signal_1
                WlmLinkQualityLevel.LEVEL_2 -> R.drawable.ux_icon_drone_4g_signal_2
                WlmLinkQualityLevel.LEVEL_3 -> R.drawable.ux_icon_drone_4g_signal_3
                WlmLinkQualityLevel.LEVEL_4 -> R.drawable.ux_icon_drone_4g_signal_4
                WlmLinkQualityLevel.LEVEL_5 -> R.drawable.ux_icon_drone_4g_signal_5
            }
        }
    }

    private fun updateRemoteControlLteStatus(level: WlmLinkQualityLevel) {
        binding.remoteLte.setImageResource(getRemoteControlQualityLevelIcon(level))
    }

    private fun getRemoteControlQualityLevelIcon(level: WlmLinkQualityLevel): Int {
        return when (level) {
            WlmLinkQualityLevel.NO_SIGNAL -> R.drawable.ux_icon_remote_net_signal_0
            WlmLinkQualityLevel.LEVEL_1 -> R.drawable.ux_icon_remote_net_signal_1
            WlmLinkQualityLevel.LEVEL_2 -> R.drawable.ux_icon_remote_net_signal_2
            WlmLinkQualityLevel.LEVEL_3 -> R.drawable.ux_icon_remote_net_signal_3
            WlmLinkQualityLevel.LEVEL_4 -> R.drawable.ux_icon_remote_net_signal_4
            WlmLinkQualityLevel.LEVEL_5 -> R.drawable.ux_icon_remote_net_signal_5
        }
    }
}