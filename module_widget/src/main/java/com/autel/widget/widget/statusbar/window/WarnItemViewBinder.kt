package com.autel.widget.widget.statusbar.window

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import com.autel.common.constant.AppTagConst.WarningTag
import com.autel.common.feature.recyclerview.DefaultViewHolder
import com.autel.drone.sdk.vmodelx.device.RemoteDevice
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.interfaces.IBaseDevice
import com.autel.log.AutelLog
import com.autel.widget.R
import com.autel.widget.databinding.WidgetLayoutWarningDeviceNameBinding
import com.drakeet.multitype.ItemViewBinder

/**
 * @date 2022/9/7.
 * @author maowei
 * @description 告警样式ViewHolder
 */
class WarnDeviceNameViewBinder() : ItemViewBinder<HashMap<IBaseDevice, Boolean>, DefaultViewHolder<WidgetLayoutWarningDeviceNameBinding>>() {

    override fun onCreateViewHolder(inflater: LayoutInflater, parent: ViewGroup): DefaultViewHolder<WidgetLayoutWarningDeviceNameBinding> {
        return DefaultViewHolder(WidgetLayoutWarningDeviceNameBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: DefaultViewHolder<WidgetLayoutWarningDeviceNameBinding>, item: HashMap<IBaseDevice, Boolean>) {
        with(holder.dataBinding) {
            val context = holder.dataBinding.root.context
            AutelLog.i(WarningTag, "onBindViewHolder item = $item")
            val device = item.keys.firstOrNull()
            if (device is RemoteDevice) {
                ivDevice.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.common_icon_warn_remote))
                tvContent.text = device.getDeviceInfoBean().deviceName
            } else if (device is IAutelDroneDevice) {
                if (device.isCenter()) {
                    ivDevice.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.common_icon_warn_drone_center))
                } else {
                    ivDevice.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.common_icon_warn_drone_normal))
                }
                tvContent.text = device.getName()
            } else {
                ivDevice.setImageDrawable(null)
                tvContent.text = ""
            }
            llRoot.background = if (item.values.firstOrNull() == true) {
                AppCompatResources.getDrawable(context, R.drawable.warn_device_name_bg_only_top)
            } else {
                AppCompatResources.getDrawable(context, R.drawable.warn_device_name_bg)
            }
        }
    }
}