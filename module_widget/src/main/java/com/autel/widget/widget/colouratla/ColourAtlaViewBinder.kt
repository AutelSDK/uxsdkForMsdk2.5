package com.autel.widget.widget.colouratla

import android.view.LayoutInflater
import android.view.ViewGroup
import com.autel.common.extension.getColourAtlaResource
import com.autel.common.feature.recyclerview.DefaultViewHolder
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.camera.enums.ThermalColorEnum
import com.autel.widget.databinding.MissionLayoutItemColourAtlaBinding
import com.drakeet.multitype.ItemViewBinder

class ColourAtlaViewBinder(private val onItemClickListener: (ThermalColorEnum) -> Unit) :
    ItemViewBinder<ColourAtlaModel, DefaultViewHolder<MissionLayoutItemColourAtlaBinding>>() {

    override fun onCreateViewHolder(inflater: LayoutInflater, parent: ViewGroup): DefaultViewHolder<MissionLayoutItemColourAtlaBinding> {
        return DefaultViewHolder(MissionLayoutItemColourAtlaBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: DefaultViewHolder<MissionLayoutItemColourAtlaBinding>, item: ColourAtlaModel) {
        with(holder.dataBinding) {
            ivPseudo.setImageResource(item.thermalColorEnum.getColourAtlaResource())
            ivPseudo.isSelected = item.isSelected
        }
        holder.itemView.setOnClickListener {
            onItemClickListener.invoke(item.thermalColorEnum)
        }
    }
}