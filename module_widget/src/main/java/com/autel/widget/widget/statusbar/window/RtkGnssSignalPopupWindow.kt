package com.autel.widget.widget.statusbar.window

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.autel.common.sdk.GpsSignalLevelEnum
import com.autel.common.widget.BasePopWindow
import com.autel.widget.R
import com.autel.widget.databinding.LayoutGnssSignalBinding
import com.autel.widget.widget.statusbar.bean.SignalStrength

/**
 * Created by  2022/10/26
 * RTK GNSS信号弹窗
 */
class RtkGnssSignalPopupWindow(context: Context) : BasePopWindow(context, autoDismiss = true) {
    private val uiBinding: LayoutGnssSignalBinding = LayoutGnssSignalBinding.inflate(LayoutInflater.from(context))

    init {
        contentView = uiBinding.root
        width = ViewGroup.LayoutParams.WRAP_CONTENT
        height = ViewGroup.LayoutParams.WRAP_CONTENT
    }

    fun setData(systemState: SignalStrength) {
        if (systemState.gpsSignalLevel == GpsSignalLevelEnum.GPS_SIGNAL_LEVEL_NONE) {
            uiBinding.tvSignalStatus.text = context.getString(R.string.common_text_gps_tag_none)
            uiBinding.tvSignalStatus.setTextColor(context.getColor(R.color.common_color_white_50))
            uiBinding.tvStarsNum.setTextColor(context.getColor(R.color.common_color_white_50))
        } else if (systemState.gpsSignalLevel == GpsSignalLevelEnum.GPS_SIGNAL_LEVEL_WEAK) {
            uiBinding.tvSignalStatus.text = context.getString(R.string.common_text_gps_tag_weak)
            uiBinding.tvSignalStatus.setTextColor(context.getColor(R.color.common_color_red))
            uiBinding.tvStarsNum.setTextColor(context.getColor(R.color.common_color_red))
        } else if (systemState.gpsSignalLevel == GpsSignalLevelEnum.GPS_SIGNAL_LEVEL_MIDDLE) {
            uiBinding.tvSignalStatus.text = context.getString(R.string.common_text_gps_tag_normal)
            uiBinding.tvSignalStatus.setTextColor(context.getColor(R.color.common_color_FF771E))
            uiBinding.tvStarsNum.setTextColor(context.getColor(R.color.common_color_FF771E))
        } else if (systemState.gpsSignalLevel == GpsSignalLevelEnum.GPS_SIGNAL_LEVEL_STRONG) {
            uiBinding.tvSignalStatus.text = context.getString(R.string.common_text_gps_tag_strong)
            uiBinding.tvSignalStatus.setTextColor(context.getColor(R.color.common_color_secondary_3ce171))
            uiBinding.tvStarsNum.setTextColor(context.getColor(R.color.common_color_secondary_3ce171))
        }
        uiBinding.tvStarsNum.text = "${systemState.gpsCount}"
    }

    fun showCenterDown(v: View) {
        contentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val conHeight = contentView.measuredHeight
        val conWidth = contentView.measuredWidth
        val vWidth = v.width
        showAsDropDown(v, -conWidth / 2 + vWidth / 2, context.resources.getDimensionPixelSize(R.dimen.common_10dp))
    }
}