package com.autel.widget.function.view

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.autel.widget.function.DefaultItemCallback
import com.autel.widget.function.ItemTouchHelperAdapter
import com.autel.widget.function.inter.IFunctionManager
import com.drakeet.multitype.MultiTypeAdapter
import java.util.Collections


class FunctionItemTouchHelper(adapter: MultiTypeAdapter, lastItemFixed: Boolean) {

    private var manager: IFunctionManager? = null

    private val defaultCallBack = DefaultItemCallback(object : ItemTouchHelperAdapter {
        override fun onItemMove(holder: RecyclerView.ViewHolder, fromPosition: Int, targetPosition: Int): Boolean {
            if (lastItemFixed && (fromPosition == adapter.items.size - 1 || targetPosition == adapter.items.size - 1)) {
                return false // 不执行交换位置的操作
            }

            if (fromPosition < adapter.items.size && targetPosition < adapter.items.size) {
                if (fromPosition < targetPosition) {
                    for (i in fromPosition until targetPosition) {
                        Collections.swap(adapter.items, i, i + 1)
                    }
                } else {
                    for (i in fromPosition downTo targetPosition + 1) {
                        Collections.swap(adapter.items, i, i - 1)
                    }
                }
                adapter.notifyItemMoved(fromPosition, targetPosition)
            }
            return true
        }

        override fun onItemSelect(holder: RecyclerView.ViewHolder?) {
            if (holder != null) {
                if (lastItemFixed && holder.bindingAdapterPosition == adapter.items.size - 1) {
                    //do nothing
                } else {
                    holder.itemView.scaleX = 1.2f
                    holder.itemView.scaleY = 1.2f
                }
            }
        }

        override fun onItemClear(holder: RecyclerView.ViewHolder) {
            holder.itemView.scaleX = 1.0f
            holder.itemView.scaleY = 1.0f
            manager?.saveBarToStorage()
            manager?.savePanelToStorage()
        }
    })

    private val touchHelper = ItemTouchHelper(defaultCallBack)

    fun bindView(view: RecyclerView, manager: IFunctionManager?) {
        this.manager = manager
        touchHelper.attachToRecyclerView(view)
    }


}