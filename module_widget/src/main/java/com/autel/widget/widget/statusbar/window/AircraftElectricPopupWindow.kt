package com.autel.widget.widget.statusbar.window

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.autel.common.sdk.ModelXDroneConst
import com.autel.common.utils.NumberParseUtil
import com.autel.common.utils.TransformUtils
import com.autel.common.widget.BasePopWindow
import com.autel.widget.R
import com.autel.widget.databinding.LayoutAircraftElectricBinding
import com.autel.widget.widget.statusbar.bean.BatteryInfo

/**
 * Created by  2022/10/26
 * 飞行器电量弹窗
 */
class AircraftElectricPopupWindow(context: Context) : BasePopWindow(context, autoDismiss = true) {

    private val uiBinding: LayoutAircraftElectricBinding = LayoutAircraftElectricBinding.inflate(LayoutInflater.from(context))

    init {
        contentView = uiBinding.root
        width = ViewGroup.LayoutParams.WRAP_CONTENT
        height = ViewGroup.LayoutParams.WRAP_CONTENT
    }

    fun setData(systemStateBean: BatteryInfo) {
        if (systemStateBean.droneBatteryPercentage != null) {
            val batteryPercentAge = systemStateBean.droneBatteryPercentage
            uiBinding.tvElectricValue.text = if (batteryPercentAge < 0) {
                context.getString(R.string.common_text_no_value)
            } else {
                "${batteryPercentAge}%"
            }
            if (batteryPercentAge >= 0) {
                if (systemStateBean.criticalLowBattery == null) return
                if (systemStateBean.lowBattery == null) return
                when {
                    batteryPercentAge <= systemStateBean.criticalLowBattery -> {
                        uiBinding.tvElectricValue.setTextColor(ContextCompat.getColor(context, R.color.common_color_red))
                    }

                    batteryPercentAge <= systemStateBean.lowBattery -> {
                        //黄色
                        uiBinding.tvElectricValue.setTextColor(ContextCompat.getColor(context, R.color.common_color_FF771E))
                    }

                    else -> {
                        //绿色
                        uiBinding.tvElectricValue.setTextColor(ContextCompat.getColor(context, R.color.common_color_secondary_3ce171))
                    }
                }
            }
        }

        if (systemStateBean.batteryTemperature != null) {
            uiBinding.tvTemperatureValue.text = "${TransformUtils.centigrade2Defalut(systemStateBean.batteryTemperature)}"
            if (systemStateBean.batteryTemperature < ModelXDroneConst.BATTERY_TEMPERATURE_EXTREMELY_MIN) {
                //红色
                uiBinding.tvTemperatureValue.setTextColor(ContextCompat.getColor(context, R.color.common_color_red))
            } else if (systemStateBean.batteryTemperature < ModelXDroneConst.BATTERY_TEMPERATURE_MIN) {
                //橙色
                uiBinding.tvTemperatureValue.setTextColor(ContextCompat.getColor(context, R.color.common_color_FF771E))
            } else if (systemStateBean.batteryTemperature < ModelXDroneConst.BATTERY_TEMPERATURE_MAX) {
                //绿色
                uiBinding.tvTemperatureValue.setTextColor(ContextCompat.getColor(context, R.color.common_color_secondary_3ce171))
            } else {
                //红色
                uiBinding.tvTemperatureValue.setTextColor(ContextCompat.getColor(context, R.color.common_color_red))
            }
        }

        if (systemStateBean.batteryVoltage != null) {
            uiBinding.tvVoltageValue.text = "${NumberParseUtil.keepOneDigits(systemStateBean.batteryVoltage)}V"
            if (systemStateBean.batteryVoltage > ModelXDroneConst.getVoltageLow(systemStateBean.droneType)) {
                //绿色
                uiBinding.tvVoltageValue.setTextColor(ContextCompat.getColor(context, R.color.common_color_secondary_3ce171))
            } else {
                //红色
                uiBinding.tvVoltageValue.setTextColor(ContextCompat.getColor(context, R.color.common_color_red))
            }
        }
    }
}