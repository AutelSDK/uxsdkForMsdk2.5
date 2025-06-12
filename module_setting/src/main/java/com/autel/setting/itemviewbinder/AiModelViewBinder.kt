package com.autel.setting.itemviewbinder

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.autel.common.bean.AiModelBean
import com.autel.common.bean.AiModelState
import com.autel.common.feature.recyclerview.DefaultViewHolder
import com.autel.setting.R
import com.autel.setting.bean.FileTypeEnum
import com.autel.setting.databinding.SettingAiModelItemBinding
import com.drakeet.multitype.ItemViewBinder

/**
 * @author 
 * @date 2023/6/16
 * AI模型adapter
 */
class AiModelViewBinder(
    private val context: Context,
    private val dealItemClick: (AiModelBean) -> Unit,
    private val dealItemDelete: (AiModelBean) -> Unit,
    private val canDelete: Boolean = false
) : ItemViewBinder<AiModelBean, DefaultViewHolder<SettingAiModelItemBinding>>() {
    companion object {
        const val TAG = "AccountViewBinder"
    }

    override fun onCreateViewHolder(inflater: LayoutInflater, parent: ViewGroup): DefaultViewHolder<SettingAiModelItemBinding> {
        return DefaultViewHolder(SettingAiModelItemBinding.inflate(inflater, parent, false))
    }

    @SuppressLint("SetTextI18n", "UseCompatLoadingForDrawables")
    override fun onBindViewHolder(holder: DefaultViewHolder<SettingAiModelItemBinding>, item: AiModelBean) {
        with(holder.dataBinding) {
            if (canDelete) {
                tvTitleName.text = item.projectName.replace(FileTypeEnum.AI_MODE.tag, "")
            } else {
                tvTitleName.text = "${item.projectName}-V${item.verCode}"
            }

            tvIsBest.isVisible = item.isBest

            //标签不为空则显示
            tvIsBest.isVisible = item.tag.isNotEmpty()
            //下划线，最后一个不显示
            vBottomLine.isVisible = item.showBottomLine
            //状态文本 只在已完成显示
            ivStateUploaded.isVisible = item.state == AiModelState.UPLOAD_SUCCESS
            //状态布局，在非已完成显示
            llState.isVisible = item.state != AiModelState.UPLOAD_SUCCESS
            //进度，在进度状态显示
            progress.isVisible = AiModelState.isExecute(item.state)
            tvProgress.isVisible = AiModelState.isExecute(item.state)
            ivDelete.isVisible = canDelete

            when (item.state) {
                AiModelState.DOWNLOAD_WAIT -> {
                    llState.setBackgroundResource(R.drawable.common_bg_bdbdbd_r100)
                    ivState.setImageResource(R.drawable.icon_download)
                }
                AiModelState.UPLOAD_WAIT -> {
                    llState.setBackgroundResource(R.drawable.common_bg_bdbdbd_r100)
                    ivState.setImageResource(R.drawable.icon_upload)
                }
                AiModelState.DOWNLOAD_EXECUTE -> {
                    llState.setBackgroundResource(0)
                    ivState.setImageResource(R.drawable.icon_download_execute)
                    tvProgress.text = "${item.progress}%"
                    progress.progress = item.progress
                }
                AiModelState.UPLOAD_EXECUTE -> {
                    llState.setBackgroundResource(0)
                    ivState.setImageResource(R.drawable.icon_upload_execute)
                    tvProgress.text = "${item.progress}%"
                    progress.progress = item.progress
                }
                else -> {}
            }

            llState.setOnClickListener {
                dealItemClick.invoke(item)
            }
            ivStateUploaded.setOnClickListener {
                dealItemClick.invoke(item)
            }

            ivDelete.setOnClickListener {
                dealItemDelete.invoke(item)
            }
        }
    }

}