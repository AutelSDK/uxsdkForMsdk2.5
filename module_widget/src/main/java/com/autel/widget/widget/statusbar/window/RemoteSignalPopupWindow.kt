package com.autel.widget.widget.statusbar.window

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.autel.common.sdk.RemoteSignalLevelEnum
import com.autel.common.widget.BasePopWindow
import com.autel.drone.sdk.vmodelx.manager.FrequencyBandManager
import com.autel.widget.R
import com.autel.widget.databinding.LayoutRemoteSignalBinding

/**
 * Created by  2022/10/26
 * 遥控器信号弹窗
 */
class RemoteSignalPopupWindow(context: Context) : BasePopWindow(context, autoDismiss = true) {
    private val binding: LayoutRemoteSignalBinding = LayoutRemoteSignalBinding.inflate(LayoutInflater.from(context))

    init {
        contentView = binding.root
        width = ViewGroup.LayoutParams.WRAP_CONTENT
        height = ViewGroup.LayoutParams.WRAP_CONTENT
    }

    fun setData(rcSignalQuality: RemoteSignalLevelEnum) {
        //产品定义，等级1为红弱，等级23为中橙，等级45为强绿
        when(rcSignalQuality){
            RemoteSignalLevelEnum.REMOTE_SIGNAL_LEVEL_4,RemoteSignalLevelEnum.REMOTE_SIGNAL_LEVEL_5 ->{
                binding.tvVoltageValue.setText(R.string.common_text_gps_tag_strong)
                binding.tvVoltageValue.setTextColor(ContextCompat.getColor(context, R.color.common_color_secondary_3ce171))
                binding.tvBandMode.setTextColor(ContextCompat.getColor(context, R.color.common_color_secondary_3ce171))
            }
            RemoteSignalLevelEnum.REMOTE_SIGNAL_LEVEL_2,RemoteSignalLevelEnum.REMOTE_SIGNAL_LEVEL_3 ->{
                binding.tvVoltageValue.setText(R.string.common_text_gps_tag_normal)
                binding.tvVoltageValue.setTextColor(ContextCompat.getColor(context, R.color.common_color_secondary_fa6400))
                binding.tvBandMode.setTextColor(ContextCompat.getColor(context, R.color.common_color_secondary_fa6400))
            }
            else -> {
                binding.tvVoltageValue.setText(R.string.common_text_gps_tag_weak)
                binding.tvVoltageValue.setTextColor(ContextCompat.getColor(context, R.color.common_color_red))
                binding.tvBandMode.setTextColor(ContextCompat.getColor(context, R.color.common_color_red))
            }
        }

        refreshBandShow()
    }

    private fun refreshBandShow() {
        binding.tvBandMode.text = FrequencyBandManager.get().getCurrFrequencyBand().tag
    }

    fun showCenterDown(v: View) {
        contentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val conHeight = contentView.measuredHeight
        val conWidth = contentView.measuredWidth
        val vWidth = v.width
        showAsDropDown(v, -conWidth / 2 + vWidth / 2, context.resources.getDimensionPixelSize(R.dimen.common_10dp))
        refreshBandShow()
    }

}