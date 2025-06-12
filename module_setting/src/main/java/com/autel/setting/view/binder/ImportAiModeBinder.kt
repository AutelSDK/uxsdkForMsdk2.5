package com.autel.setting.view.binder

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.autel.common.R
import com.autel.common.feature.recyclerview.DefaultViewHolder
import com.autel.setting.bean.FileInfoBean
import com.autel.setting.bean.FileTypeEnum
import com.autel.setting.databinding.ItemImportAiModeBinding
import com.drakeet.multitype.ItemViewBinder

/**
 * @author 
 * @date 2023/8/4
 * Ai 文件选择
 */
class ImportAiModeBinder(private val dealItemClick: (Int) -> Unit) : ItemViewBinder<FileInfoBean, DefaultViewHolder<ItemImportAiModeBinding>>() {

    override fun onCreateViewHolder(inflater: LayoutInflater, parent: ViewGroup): DefaultViewHolder<ItemImportAiModeBinding> {
        return DefaultViewHolder(ItemImportAiModeBinding.inflate(inflater, parent, false))
    }

    @SuppressLint("SetTextI18n", "UseCompatLoadingForDrawables")
    override fun onBindViewHolder(holder: DefaultViewHolder<ItemImportAiModeBinding>, item: FileInfoBean) {
        with(holder.dataBinding) {
            tvFileName.text = item.fileName
            ivChoose.isVisible = item.isEdit && item.fileType == FileTypeEnum.AI_MODE
            ivChoose.setBackgroundResource(getChooseIcon(item.isChoose))
            ivFileType.setBackgroundResource(getFileTypeIcon(item.fileType))

            rlContainer.setOnClickListener {
                item.isChoose = !item.isChoose
                ivChoose.setBackgroundResource(getChooseIcon(item.isChoose))
                dealItemClick.invoke(item.index)
            }
        }
    }

    private fun getChooseIcon(choose: Boolean): Int {
        return if (choose) R.drawable.common_icon_checked else R.drawable.common_icon_unchecked
    }

    private fun getFileTypeIcon(type: FileTypeEnum): Int {
        return when (type) {
            FileTypeEnum.FOLDER -> R.drawable.icon_type_folder
            FileTypeEnum.AI_MODE -> R.drawable.icon_type_ai_mode
            FileTypeEnum.FILE -> R.drawable.icon_type_file
        }
    }

}