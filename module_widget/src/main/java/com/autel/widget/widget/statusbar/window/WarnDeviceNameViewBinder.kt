package com.autel.widget.widget.statusbar.window

import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import com.autel.widget.R
import com.autel.widget.widget.statusbar.warn.WarningBean
import com.autel.common.feature.recyclerview.DefaultViewHolder
import com.autel.common.utils.UIUtils.getColor
import com.autel.common.utils.UIUtils.getString
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.common.enums.WaringIdEnum
import com.autel.widget.databinding.WidgetLayoutWarningItemBinding
import com.drakeet.multitype.ItemViewBinder

/**
 * @date 2022/9/7.
 * @author maowei
 * @description 告警样式ViewHolder
 */
class WarnItemViewBinder(val onItemClicked: (WarningBean, WarningBean.Action) -> Unit) : ItemViewBinder<WarningBean, DefaultViewHolder<WidgetLayoutWarningItemBinding>>() {

    override fun onCreateViewHolder(inflater: LayoutInflater, parent: ViewGroup): DefaultViewHolder<WidgetLayoutWarningItemBinding> {
        return DefaultViewHolder(WidgetLayoutWarningItemBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: DefaultViewHolder<WidgetLayoutWarningItemBinding>, item: WarningBean) {
        with(holder.dataBinding) {
            when (item.warnLevel) {
                WarningBean.WarnLevel.HIGH_TIP -> ivWarnLevel.setImageResource(R.drawable.mission_ic_serious_warn)
                WarningBean.WarnLevel.MIDDLE_TIP -> ivWarnLevel.setImageResource(R.drawable.mission_ic_weak_warn)
                WarningBean.WarnLevel.NO_FLY -> ivWarnLevel.setImageDrawable(null)
            }
            if (item.tip.contentRes != 0) {
                if (item.warnId == WaringIdEnum.AIRCRAFT_DISCONNECT) {
                    tvContent.text = getString((R.string.common_text_connect_aircraft))
                } else {
                    tvContent.setText(item.tip.contentRes)
                }
            } else {
                tvContent.setText(item.tip.contentStr)
            }
            if (item.detailMsg?.isNotEmpty() == true) {
                llWarnDetail.visibility = VISIBLE
            } else {
                llWarnDetail.visibility = GONE
            }
            tvDetail.text = item.detailMsg
            tvDetail.setTextColor(getColor(R.color.common_color_BDBDBD))
            val tip = item.tip
            val action = if (tip is WarningBean.TipType.TipWindow) {
                tip.action
            } else if (tip is WarningBean.TipType.TipDialog) {
                tip.rightBtnAction
            } else {
                null
            }
            if (action != null) {
                when (action) {
                    //暂时只有指南针校准和IMU校准，第一次对频需要跳转
                    WarningBean.Action.COMPASS_CALI,
                    WarningBean.Action.CONNECTING_AIRCRAFT,
                    WarningBean.Action.IMU_CALI,
                    WarningBean.Action.RC_CALI,
                    WarningBean.Action.RID_MSG,
                    WarningBean.Action.ACTIVATE_DRONE,
                    WarningBean.Action.SHOW_ARM_UNFOLD_DIALOG,
                    WarningBean.Action.SHOW_BATTERY_INSTALL_DIALOG,
                    WarningBean.Action.UOM-> {
                        tvContent.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.common_ic_arrow_right_white, 0)
                        llLayout.setOnClickListener {
                            onItemClicked(item, action)
                        }
                    }

                    else -> {
                        tvContent.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
                        llLayout.setOnClickListener(null)
                    }
                }

            } else {
                tvContent.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
                llLayout.setOnClickListener(null)
            }
        }
    }
}