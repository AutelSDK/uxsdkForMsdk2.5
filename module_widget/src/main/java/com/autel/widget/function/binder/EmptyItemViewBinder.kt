package com.autel.widget.function.binder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.autel.common.delegate.function.FunctionViewType
import com.autel.widget.R
import com.autel.widget.function.model.EmptyFunctionModel
import com.drakeet.multitype.ItemViewBinder

class EmptyItemViewBinder(
    private val viewType: FunctionViewType,
    private val enterPanelEdit: ((View) -> Unit)? = null
) :
    ItemViewBinder<EmptyFunctionModel, EmptyItemViewHolder>() {
    override fun onBindViewHolder(holder: EmptyItemViewHolder, item: EmptyFunctionModel) {
        when (viewType) {
            FunctionViewType.Bar -> {
                holder.view.updateLayoutParams<ViewGroup.LayoutParams> {
                    height = holder.view.context.resources.getDimensionPixelOffset(R.dimen.common_variety_function_bar_height)
                }
                holder.view.setBackgroundResource(0)
                holder.view.setImageResource(0)
                holder.view.setOnClickListener(null)
            }

            FunctionViewType.FloatBall -> {
                holder.view.updateLayoutParams<ViewGroup.LayoutParams> {
                    height = holder.view.context.resources.getDimensionPixelOffset(R.dimen.common_90dp)
                }
                if (item.edit) {
                    holder.view.setBackgroundResource(0)
                    holder.view.setImageResource(0)
                    holder.view.setOnClickListener(null)
                } else {
                    holder.view.setOnClickListener {
                        enterPanelEdit?.invoke(it)
                    }
                    holder.view.setBackgroundResource(R.drawable.common_shape_black_30_r_8)
                    holder.view.setImageResource(R.drawable.common_icon_float_window_add_item)
                }
            }

            FunctionViewType.Panel -> {
                holder.view.updateLayoutParams<ViewGroup.LayoutParams> {
                    height = holder.view.context.resources.getDimensionPixelOffset(R.dimen.common_90dp)
                }
                holder.view.setBackgroundResource(0)
                holder.view.setImageResource(0)
                holder.view.setOnClickListener(null)
            }
        }

    }

    override fun onCreateViewHolder(inflater: LayoutInflater, parent: ViewGroup): EmptyItemViewHolder {
        return EmptyItemViewHolder(inflater.inflate(R.layout.widget_layout_empty_view, parent, false) as ImageView)
    }
}

class EmptyItemViewHolder(val view: ImageView) : RecyclerView.ViewHolder(view) {

}