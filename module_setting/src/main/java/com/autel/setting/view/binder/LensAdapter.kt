package com.autel.setting.view.binder

import android.widget.ImageView
import android.widget.TextView
import com.autel.common.extension.getLensTypeName
import com.autel.common.manager.AppInfoManager
import com.autel.drone.sdk.vmodelx.module.camera.bean.LensTypeEnum
import com.autel.setting.R
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

class LensAdapter(
    lensInfoList: ArrayList<LensTypeEnum>,
    private val selectLens: MutableList<LensTypeEnum>,
) :
    BaseQuickAdapter<LensTypeEnum, BaseViewHolder>(R.layout.setting_select_lens_item, lensInfoList) {

    override fun convert(holder: BaseViewHolder, item: LensTypeEnum) {
        holder.getView<ImageView>(R.id.im_selected).isSelected = selectLens.contains(item)
        holder.getView<TextView>(R.id.tv_primary_title).text = getLensTypeEnumName(item)
    }

    private fun getLensTypeEnumName(it: LensTypeEnum): String {
        return it.getLensTypeName(AppInfoManager.getApplicationContext())
    }
}