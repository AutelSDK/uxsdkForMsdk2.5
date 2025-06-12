package com.autel.widget.function.binder

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import com.autel.common.delegate.function.FunctionViewType
import com.autel.common.feature.recyclerview.DefaultViewHolder
import com.autel.widget.R
import com.autel.widget.databinding.MissionLayoutFunctionItemBinding
import com.autel.widget.function.model.SwitchFunctionModel
import com.drakeet.multitype.ItemViewBinder


class FunctionItemViewBinder(private val viewType: FunctionViewType) :
    ItemViewBinder<SwitchFunctionModel, DefaultViewHolder<MissionLayoutFunctionItemBinding>>() {

    override fun onCreateViewHolder(inflater: LayoutInflater, parent: ViewGroup): DefaultViewHolder<MissionLayoutFunctionItemBinding> {
        return DefaultViewHolder(MissionLayoutFunctionItemBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: DefaultViewHolder<MissionLayoutFunctionItemBinding>, item: SwitchFunctionModel) {
        holder.dataBinding.functionItem.setFunctionItem(viewType, item)
        val context = holder.dataBinding.root.context
        when (viewType) {
            FunctionViewType.Bar -> {
                holder.dataBinding.functionItem.updateLayoutParams<ViewGroup.LayoutParams> {
                    height = holder.dataBinding.root.context.resources.getDimensionPixelOffset(R.dimen.common_variety_function_bar_height)
                }
                holder.dataBinding.functionItem.setIconSizeAndFontSize(
                    context.resources.getDimensionPixelSize(R.dimen.common_30dp),
                    context.resources.getDimension(R.dimen.common_text_size_sp_13)
                )
            }

            FunctionViewType.Panel -> {
                holder.dataBinding.functionItem.updateLayoutParams<ViewGroup.LayoutParams> {
                    height = holder.dataBinding.root.context.resources.getDimensionPixelOffset(R.dimen.common_90dp)
                }
                holder.dataBinding.functionItem.setIconSizeAndFontSize(
                    context.resources.getDimensionPixelSize(R.dimen.common_34dp),
                    context.resources.getDimension(R.dimen.common_text_size_sp_13)
                )
            }

            FunctionViewType.FloatBall -> {
                holder.dataBinding.functionItem.updateLayoutParams<ViewGroup.LayoutParams> {
                    height = holder.dataBinding.root.context.resources.getDimensionPixelOffset(R.dimen.common_90dp)
                }
                holder.dataBinding.functionItem.setIconSizeAndFontSize(
                    context.resources.getDimensionPixelSize(R.dimen.common_44dp),
                    context.resources.getDimension(R.dimen.common_text_size_sp_14)
                )
            }
        }
        holder.dataBinding.functionItem.setEditMode(viewType == FunctionViewType.Panel)
        if (item.isEdit && viewType == FunctionViewType.FloatBall) {
            holder.dataBinding.functionItem.setBackgroundResource(R.drawable.common_shape_black_30_r_8)
        } else {
            holder.dataBinding.functionItem.setBackgroundResource(0)
        }
    }
}