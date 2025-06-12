package com.autel.widget.widget.colouratla

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.autel.common.extension.getThermalName
import com.autel.common.widget.BasePopWindow
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.camera.enums.ThermalColorEnum
import com.autel.widget.R
import com.autel.widget.databinding.MissionColourAtlaPopupwindowBinding
import com.drakeet.multitype.MultiTypeAdapter


class ColourAtlaPopupWindow(context: Context) : BasePopWindow(context) {
    private var uiBinding: MissionColourAtlaPopupwindowBinding = MissionColourAtlaPopupwindowBinding.inflate(LayoutInflater.from(context))
    private val adapter: MultiTypeAdapter = MultiTypeAdapter()
    private var colourAtlaSelectListener: IColourAtlaSelectListner? = null


    private val colourAtlaList: MutableList<ColourAtlaModel> = mutableListOf()


    init {
        contentView = uiBinding.root
        width = ViewGroup.LayoutParams.MATCH_PARENT
        height = context.resources.getDimensionPixelSize(R.dimen.common_343dp)

        uiBinding.rvContent.layoutManager = LinearLayoutManager(context)
        uiBinding.rvContent.adapter = adapter
        adapter.register(ColourAtlaViewBinder(onItemClickListener = {
            colourAtlaSelectListener?.onColourAtlaSelect(it)
        }))
        adapter.items = colourAtlaList
        uiBinding.root.setOnClickListener {
            dismiss()
        }
    }


    fun setColourAtlaSelectListener(colourAtlaSelectListner: IColourAtlaSelectListner) {
        this.colourAtlaSelectListener = colourAtlaSelectListner
    }


    fun updateColourAtlaModelList(list: List<ColourAtlaModel> ) {
        this.colourAtlaList.clear()
        this.colourAtlaList.addAll(list)
        adapter.notifyDataSetChanged()
    }


    fun setThermalColorEnum(thermalColorEnum: ThermalColorEnum) {
        uiBinding.tvColourAtla.setText(thermalColorEnum.getThermalName())
        colourAtlaList.forEach {
            it.isSelected = thermalColorEnum == it.thermalColorEnum
        }
        adapter.notifyDataSetChanged()
    }




}