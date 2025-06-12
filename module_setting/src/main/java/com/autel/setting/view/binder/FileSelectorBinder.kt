package com.autel.setting.view.binder

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.autel.common.R
import com.autel.common.feature.recyclerview.DefaultViewHolder
import com.autel.setting.bean.SelectorFile
import com.autel.setting.databinding.ItemImportAiModeBinding
import com.drakeet.multitype.ItemViewBinder

/**
 * @author zeng
 * @date 2023/8/4
 * Ai 文件选择
 */
class FileSelectorBinder(private val dealItemClick: (SelectorFile) -> Unit) :
    ItemViewBinder<SelectorFile, DefaultViewHolder<ItemImportAiModeBinding>>() {

    override fun onCreateViewHolder(inflater: LayoutInflater, parent: ViewGroup): DefaultViewHolder<ItemImportAiModeBinding> {
        return DefaultViewHolder(ItemImportAiModeBinding.inflate(inflater, parent, false))
    }

    @SuppressLint("SetTextI18n", "UseCompatLoadingForDrawables")
    override fun onBindViewHolder(holder: DefaultViewHolder<ItemImportAiModeBinding>, item: SelectorFile) {
        with(holder.dataBinding) {
            tvFileName.text = item.file.name
            ivChoose.isVisible = item.isMultipleSelectMode || item.isSelect
            ivChoose.setBackgroundResource(getChooseIcon(item.isSelect))
            ivFileType.setBackgroundResource(getFileTypeIcon(item))

            rlContainer.setOnClickListener {
                if (!item.file.isDirectory) {
                    item.isSelect = !item.isSelect
                    ivChoose.setBackgroundResource(getChooseIcon(item.isSelect))
                }
                dealItemClick.invoke(item)
            }
        }
    }

    private fun getChooseIcon(choose: Boolean): Int {
        return if (choose) R.drawable.common_icon_checked else R.drawable.common_icon_unchecked
    }

    private fun getFileTypeIcon(type: SelectorFile): Int {
        if (type.file.isDirectory) {
            return R.drawable.icon_type_folder
        }
        return R.drawable.icon_type_file
    }

}