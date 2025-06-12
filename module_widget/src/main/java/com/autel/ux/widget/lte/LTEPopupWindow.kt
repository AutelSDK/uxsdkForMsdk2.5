package com.autel.ux.widget.lte

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.autel.common.widget.dialog.CommonSingleButtonDialog
import com.autel.common.widget.dialog.CommonTwoButtonDialog
import com.autel.drone.sdk.v2.enums.NetworkStatus
import com.autel.drone.sdk.v2.enums.WlmLinkQualityLevel
import com.autel.log.AutelLog
import com.autel.common.utils.getString
import com.autel.ux.core.base.AutelSDKModel
import com.autel.ux.core.base.widget.ConstraintLayoutWidget
import com.autel.widget.R
import com.autel.widget.databinding.WidgetLtePopupWindowBinding
import kotlinx.coroutines.flow.combine

class LTEPopupWindow @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
) : ConstraintLayoutWidget(context, attrs) {
    private lateinit var binding: WidgetLtePopupWindowBinding

    private val widgetModel: LTEWidgetModel by lazy {
        LTEWidgetModel(AutelSDKModel.getInstance())
    }

    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        binding = WidgetLtePopupWindowBinding.inflate(LayoutInflater.from(context), this)
        initView()
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
        binding.lteTipsGroup.isInvisible = true
        combine(widgetModel.isEnableLTEModeFlow, widgetModel.droneLetSignalFlow) { isEnable, signal ->
            isEnable to signal
        }.collectInWidget {
            updateDroneStatus(it.first, it.second)
        }

        combine(widgetModel.isEnableLTEModeFlow, widgetModel.remoteLetSignalFlow) { isEnable, signal ->
            isEnable to signal
        }.collectInWidget {
            updateRemoteControlStatus(it.first, it.second)
        }

        widgetModel.isEnableLTEModeFlow.collectInWidget {
            AutelLog.i(TAG, "isEnableLTEModeFlow: $it")
            binding.switchButton.setCheckedWithNoListen(it)
            updateRemoteControlWarn(widgetModel.remoteControlNetworkStatus.value)
            updateDroneWarn(widgetModel.droneNetworkStatus.value)
        }

        widgetModel.droneNetworkStatus.collectInWidget(::updateDroneWarn)
        widgetModel.remoteControlNetworkStatus.collectInWidget(::updateRemoteControlWarn)
    }

    private fun updateDroneWarn(status: NetworkStatus) {
        AutelLog.i(TAG, "drone warn change :$status")
        binding.layoutDroneWarn.isVisible = status != NetworkStatus.NETWORK_STATUS_CONNECTED && widgetModel.isEnableLTEModeFlow.value
        binding.tvDroneWarn.text = getString(
            when (status) {
                NetworkStatus.NETWORK_STATUS_NONE -> R.string.common_text_enhance_drone_warn_1
                NetworkStatus.NETWORK_STATUS_SIM_NOT_INSERTED -> R.string.common_text_enhance_drone_warn_2
                NetworkStatus.NETWORK_STATUS_UNREACHABLE -> R.string.common_text_enhance_drone_warn_3
                NetworkStatus.NETWORK_STATUS_CONNECTED -> R.string.common_text_no_value
            }
        )
    }

    private fun updateRemoteControlWarn(status: NetworkStatus) {
        AutelLog.i(TAG, "remote control warn change :$status")
        binding.layoutRemoteWarn.isVisible = status != NetworkStatus.NETWORK_STATUS_CONNECTED &&  widgetModel.isEnableLTEModeFlow.value
        binding.tvRemoteControlWarn.text = getString(
            when (status) {
                NetworkStatus.NETWORK_STATUS_NONE,
                NetworkStatus.NETWORK_STATUS_SIM_NOT_INSERTED,
                    -> R.string.common_text_enhance_rc_warn_1

                NetworkStatus.NETWORK_STATUS_UNREACHABLE -> R.string.common_text_enhance_rc_warn_2
                NetworkStatus.NETWORK_STATUS_CONNECTED -> R.string.common_text_no_value
            }
        )
    }

    private fun initView() {
        binding.lteTipsGroup.isInvisible = true
        binding.viewBg.setOnClickListener {
            binding.lteTipsGroup.isInvisible = true
        }
        binding.layoutLteTip.setOnClickListener {
            // do nothing
        }
        binding.switchButton.setOnCheckedChangeListener { btn, switch ->
            binding.lteTipsGroup.isInvisible = true
            if (widgetModel.productConnected.value.not()) {
                binding.switchButton.setCheckedWithNoListen(!switch)
                return@setOnCheckedChangeListener
            }

            //当关闭增强图传时，要根据图传信号强度判断一下
            if (!switch) {
                if (widgetModel.isRcSignalAvailable()) {
                    var isCommit = false
                    context?.let {
                        CommonTwoButtonDialog(it)
                            .apply {
                                setMessage(getString(R.string.common_text_enhance_close_tips))
                                setRightBtnStr(getString(R.string.common_text_close))
                                setRightBtnListener {
                                    isCommit = true
                                    widgetModel.setEnableLTEMode(switch)
                                }
                                setOnDismissListener {
                                    if (!isCommit) binding.switchButton.setCheckedWithNoListen(!switch)
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
                                    binding.switchButton.setCheckedWithNoListen(!switch)
                                }
                                show()
                            }
                    }
                }
            } else {
                widgetModel.setEnableLTEMode(switch)
            }
        }
        binding.titleTipIcon.setOnClickListener {
            binding.lteTipsGroup.isInvisible = !binding.lteTipsGroup.isInvisible
        }
    }

    private fun updateDroneStatus(isEnable: Boolean, signal: WlmLinkQualityLevel) {
        binding.tvDroneLteLevel.text = getLTELevelStr(isEnable, signal)
        binding.tvDroneLteLevel.setTextColor(getLTELevelColor(signal))
    }

    private fun updateRemoteControlStatus(isEnable: Boolean, signal: WlmLinkQualityLevel) {
        binding.tvRemoteLteLevel.text = getLTELevelStr(isEnable, signal)
        binding.tvRemoteLteLevel.setTextColor(getLTELevelColor(signal))
    }

    private fun getLTELevelStr(isEnable: Boolean, level: WlmLinkQualityLevel): String {
        return context.getString(
            when (level) {
                WlmLinkQualityLevel.NO_SIGNAL -> if (isEnable) {
                    R.string.common_text_gps_tag_none
                } else {
                    R.string.common_text_not_enabled
                }

                WlmLinkQualityLevel.LEVEL_1 -> R.string.common_text_gps_tag_weak
                WlmLinkQualityLevel.LEVEL_2,
                WlmLinkQualityLevel.LEVEL_3,
                    -> R.string.common_text_gps_tag_normal

                WlmLinkQualityLevel.LEVEL_4,
                WlmLinkQualityLevel.LEVEL_5,
                    -> R.string.common_text_gps_tag_strong
            }
        )
    }

    private fun getLTELevelColor(level: WlmLinkQualityLevel): Int {
        return context.getColor(
            when (level) {
                WlmLinkQualityLevel.NO_SIGNAL -> R.color.common_color_red

                WlmLinkQualityLevel.LEVEL_1 -> R.color.common_color_secondary_e60012
                WlmLinkQualityLevel.LEVEL_2,
                WlmLinkQualityLevel.LEVEL_3,
                    -> R.color.common_color_secondary_fa6400

                WlmLinkQualityLevel.LEVEL_4,
                WlmLinkQualityLevel.LEVEL_5,
                    -> R.color.common_color_3CE171
            }
        )
    }

}