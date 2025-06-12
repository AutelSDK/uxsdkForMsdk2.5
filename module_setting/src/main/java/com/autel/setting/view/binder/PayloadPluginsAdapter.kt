package com.autel.setting.view.binder

import com.autel.common.widget.CommonItemText
import com.autel.setting.R
import com.autel.setting.bean.PluginBean
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

/**
 * Copyright: Autel Robotics
 * @author R24033 on 2025/4/24
 * 负载设置适配器
 */
class PayloadPluginsAdapter(private val dataList: MutableList<PluginBean>) :
    BaseQuickAdapter<PluginBean, BaseViewHolder>(
        layoutResId = R.layout.setting_layout_payload_plugins_ist_item,
        data = dataList
    ) {

    override fun convert(holder: BaseViewHolder, item: PluginBean) {
        val citItem = holder.getView<CommonItemText>(R.id.cit_item)
        val size = dataList.size
        val showLine = holder.layoutPosition != size - 1
        citItem.apply {
            setTitle(item.pluginName)
            setBottomLineVisible(showLine)
        }
    }

}