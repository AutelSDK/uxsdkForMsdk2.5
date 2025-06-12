package com.autel.setting.view.binder

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.autel.common.utils.DeviceUtils
import com.autel.common.widget.CommonItemSwitch
import com.autel.common.widget.CommonItemText
import com.autel.common.widget.CommonSettingItemSpinnerView
import com.autel.common.widget.CommonSuperSeekView
import com.autel.common.widget.HeightModifyView
import com.autel.common.widget.spinnerview.CommonSpinnerView.SpinnerViewListener
import com.autel.drone.sdk.libbase.error.IAutelCode
import com.autel.drone.sdk.log.SDKLog
import com.autel.drone.sdk.vmodelx.manager.keyvalue.callback.CommonCallbacks
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.payload.bean.PayloadAckBean
import com.autel.drone.sdk.vmodelx.module.payload.PayloadCenter
import com.autel.drone.sdk.vmodelx.module.payload.PayloadIndexType
import com.autel.drone.sdk.vmodelx.module.payload.WidgetType
import com.autel.drone.sdk.vmodelx.module.payload.WidgetValue
import com.autel.drone.sdk.vmodelx.module.payload.widget.PayloadWidget
import com.autel.log.AutelLog
import com.autel.setting.R

/**
 * Copyright: Autel Robotics
 * @author R24033 on 2025/4/17
 * 配置界面
 */
class SettingPluginsConfigAdapter(
    private val payloadIndexType: PayloadIndexType,
    private val onItemClick: ((position: Int, widget: PayloadWidget) -> Unit)? = null
) : ListAdapter<PayloadWidget, RecyclerView.ViewHolder>(DiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        val callBack:(WidgetValue)-> Unit = { widgetValue -> setWidgetValue(widgetValue) }

        val viewHolder = when (viewType) {
            1 -> {//button
                val view =
                    inflater.inflate(
                        R.layout.setting_layout_plugins_config_list_item_button,
                        parent,
                        false
                    )
                ButtonViewHolder(view, parent.context, callBack)
            }

            2 -> {//switch
                val view =
                    inflater.inflate(
                        R.layout.setting_layout_plugins_config_list_item_switch,
                        parent,
                        false
                    )
                SwitchViewHolder(view, parent.context, callBack)
            }

            3 -> {//scale
                val view =
                    inflater.inflate(
                        R.layout.setting_layout_plugins_config_list_item_scale,
                        parent,
                        false
                    )
                ScaleViewHolder(view, parent.context, callBack)
            }

            4 -> {//list
                val view =
                    inflater.inflate(
                        R.layout.setting_layout_plugins_config_list_item_list,
                        parent,
                        false
                    )
                ListViewHolder(view, parent.context, callBack)
            }

            5 -> {//input
                val view =
                    inflater.inflate(
                        R.layout.setting_layout_plugins_config_list_item_input,
                        parent,
                        false
                    )
                IntegerInputViewHolder(view, parent.context, callBack)
            }

            else -> {
                val view =
                    inflater.inflate(
                        R.layout.setting_layout_plugins_config_list_item_button,
                        parent,
                        false
                    )
                ButtonViewHolder(view, parent.context, callBack)
            }

        }

        return viewHolder
    }

    private fun setWidgetValue(widgetValue: WidgetValue){
        val droneDevice = DeviceUtils.singleControlDrone()
        if(droneDevice == null){
            AutelLog.e("SettingPluginsConfigAdapter","setWidgetValue droneDevice is null")
        } else {
            PayloadCenter.get().getPayloadManager()[payloadIndexType]?.setWidgetValue(
                droneDevice,
                widgetValue,
                object : CommonCallbacks.CompletionCallback {
                    override fun onSuccess() {
                        AutelLog.i("SettingPluginsConfigAdapter", "setWidgetValue success $widgetValue ${droneDevice?.toSampleString()}")
                    }

                    override fun onFailure(code: IAutelCode, msg: String?) {
                        AutelLog.e("SettingPluginsConfigAdapter", "setWidgetValue onFailure $widgetValue ${droneDevice?.toSampleString()}")
                    }
                })
        }
    }

    private fun getWidgetValue(payloadWidget: PayloadWidget, holder: BaseHolder){
        val widgetValue = WidgetValue().apply {
            index = payloadWidget.widgetIndex ?: 0
            type = WidgetType.findWidgetType(payloadWidget.widgetType)
            value = 0
        }
        val droneDevice = DeviceUtils.singleControlDrone()
        if(droneDevice == null){
            AutelLog.e("SettingPluginsConfigAdapter","getWidgetValue droneDevice is null")
        } else {
            PayloadCenter.get().getPayloadManager()[payloadIndexType]?.getWidgetValue(
                droneDevice,
                widgetValue,
                object : CommonCallbacks.CompletionCallbackWithParam<PayloadAckBean> {
                    override fun onSuccess(t: PayloadAckBean?) {
                        AutelLog.i("SettingPluginsConfigAdapter", "getWidgetValue success $widgetValue, $t ${droneDevice?.toSampleString()}")
                        t?.value?.let { holder.updateData(it) }
                    }

                    override fun onFailure(code: IAutelCode, msg: String?) {
                        AutelLog.e("SettingPluginsConfigAdapter", "getWidgetValue onFailure $widgetValue ${droneDevice?.toSampleString()}")
                    }
                })
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        val viewType = getItemViewType(position)
        /*holder.itemView.setOnClickListener {
            onItemClick?.invoke(position, item)
        }*/
        when (viewType) {
            1 -> {//button
                val btHolder = holder as ButtonViewHolder
                holder.bind(item)
                getWidgetValue(item, btHolder)
            }

            2 -> {//switch
                val switchHolder = holder as SwitchViewHolder
                switchHolder.bind(item)
                getWidgetValue(item, switchHolder)
            }

            3 -> {//scale
                val scaleHolder = holder as ScaleViewHolder
                scaleHolder.bind(item)
                getWidgetValue(item, scaleHolder)
            }

            4 -> {//list
                val listHolder = holder as ListViewHolder
                listHolder.bind(item)
                getWidgetValue(item, listHolder)
            }

            5 -> {//input
                val inputHolder = holder as IntegerInputViewHolder
                inputHolder.bind(item)
                getWidgetValue(item, inputHolder)
            }

            else -> {
                val defHolder = holder as ButtonViewHolder
                defHolder.bind(item)
                getWidgetValue(item, defHolder)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        val type = WidgetType.findWidgetType(getItem(position).widgetType).value
        return type
    }

    //button
    class ButtonViewHolder(itemView: View, val context: Context,val callback:(WidgetValue)->Unit) :BaseHolder(itemView) {
        private val buttonName: CommonItemText = itemView.findViewById(R.id.cit_button)

        fun bind(item: PayloadWidget) {
            buttonName.setTitle(item.widgetName ?: "")
            buttonName.setOnClickListener {
                val widgetValue = WidgetValue().apply {
                    index = item.widgetIndex ?: 0
                    type = WidgetType.findWidgetType(item.widgetType)
                    value = 1
                }
                callback.invoke(widgetValue)
            }
        }

        override fun updateData(value: Int) {
            SDKLog.i("SettingPluginsConfigAdapter", "ButtonViewHolder ScaleViewHolder $value")
        }
    }

    //scale
    class ScaleViewHolder(itemView: View, val context: Context,val callback:(WidgetValue)->Unit) : BaseHolder(itemView) {
        private val scale: CommonSuperSeekView = itemView.findViewById(R.id.cssv_scale)

        fun bind(item: PayloadWidget) {
            scale.setTitle(item.widgetName)
            scale.initSeekBar(0, 100)
            scale.setOnSeekBarStoppedListener {
                val widgetValue = WidgetValue().apply {
                    index = item.widgetIndex ?: 0
                    type = WidgetType.findWidgetType(item.widgetType)
                    value = it.toInt()
                }
                callback.invoke(widgetValue)
            }
        }

        override fun updateData(value: Int) {
            SDKLog.i("SettingPluginsConfigAdapter", "ListViewHolder ScaleViewHolder $value")
        }
    }

    //input
    class IntegerInputViewHolder(itemView: View, val context: Context,val callback:(WidgetValue)->Unit) : BaseHolder(itemView) {
        private val input: HeightModifyView = itemView.findViewById(R.id.hmv_input)

        fun bind(item: PayloadWidget) {
            input.setTitleName(item.widgetName ?: "")
            input.setDescStr(item.hintMessage ?: "")
            input.setMaxValue(Int.MAX_VALUE)
            input.setMinValue(Int.MIN_VALUE)
//            input.setInputTypeWithNegative()
            input.setHeightData(0)
            input.setOnHeightChangeListener {
                val widgetValue = WidgetValue().apply {
                    index = item.widgetIndex ?: 0
                    type = WidgetType.findWidgetType(item.widgetType)
                    value = it
                }
                callback.invoke(widgetValue)
            }
        }

        override fun updateData(value: Int) {
            SDKLog.i("SettingPluginsConfigAdapter", "ListViewHolder IntegerInputViewHolder $value")
        }
    }

    //switch
    class SwitchViewHolder(itemView: View, val context: Context,val callback:(WidgetValue)->Unit) :BaseHolder(itemView) {
        private val switch: CommonItemSwitch = itemView.findViewById(R.id.cis_switch)

        fun bind(item: PayloadWidget) {
            switch.setTitleName(item.widgetName ?: "")
            switch.setOnSwitchChangeListener { isChecked ->
                val widgetValue = WidgetValue().apply {
                    index = item.widgetIndex ?: 0
                    type = WidgetType.findWidgetType(item.widgetType)
                    value = if(isChecked) 1 else 0
                }
                callback.invoke(widgetValue)
            }
        }

        override fun updateData(value: Int) {
            SDKLog.i("SettingPluginsConfigAdapter", "SwitchViewHolder updateData $value")
        }
    }

    //List
    class ListViewHolder(itemView: View, val context: Context,val callback:(WidgetValue)->Unit) : BaseHolder(itemView) {
        private val listView: CommonSettingItemSpinnerView = itemView.findViewById(R.id.csisv_list)

        fun bind(item: PayloadWidget) {
            listView.updateSettingTitle(item.widgetName ?: "")
            listView.updateSpinnerData(getSpannerData(item))
            listView.setSpinnerSelectedListener(object : SpinnerViewListener {
                override fun onSelectPosition(position: Int) {
                    val widgetValue = WidgetValue().apply {
                        index = item.widgetIndex ?: 0
                        type = WidgetType.findWidgetType(item.widgetType)
                        value = position + 1
                    }
                    callback.invoke(widgetValue)
                }
            })
        }

        override fun updateData(value: Int) {
            SDKLog.i("SettingPluginsConfigAdapter", "ListViewHolder updateData $value")
        }

        private fun getSpannerData(item: PayloadWidget): List<String> {
            val itemList = item.subItemsList
            val result: MutableList<String> = ArrayList()
            if (!itemList.isNullOrEmpty()) {
                itemList.forEach {
                    it.subItemsName?.let { name -> result.add(name) }
                }
            }
            return result
        }


    }

    abstract class BaseHolder(itemView: View):   RecyclerView.ViewHolder(itemView) {
        abstract fun updateData(value:Int)
    }


    object DiffCallback : DiffUtil.ItemCallback<PayloadWidget>() {

        override fun areItemsTheSame(oldItem: PayloadWidget, newItem: PayloadWidget): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: PayloadWidget, newItem: PayloadWidget): Boolean {
            return oldItem.widgetType.equals(newItem.widgetType) && oldItem.widgetIndex == newItem.widgetIndex
        }

    }
}