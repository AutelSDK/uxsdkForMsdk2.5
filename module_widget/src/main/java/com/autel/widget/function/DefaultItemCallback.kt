package com.autel.widget.function

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.autel.widget.function.binder.EmptyItemViewHolder

class DefaultItemCallback(private val touchHelperAdapter: ItemTouchHelperAdapter) : ItemTouchHelper.Callback() {

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        val dragFlags: Int = ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT //允许上下左右的拖动
        return if (viewHolder is EmptyItemViewHolder) {
            makeMovementFlags(0, 0)
        } else {
            makeMovementFlags(dragFlags, 0)
        }
    }

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        return touchHelperAdapter.onItemMove(viewHolder, viewHolder.getAdapterPosition(), target.getAdapterPosition())
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        touchHelperAdapter.onItemSelect(viewHolder)
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        touchHelperAdapter.onItemClear(viewHolder)
    }

    override fun isLongPressDragEnabled(): Boolean {
        return true
    }

    override fun isItemViewSwipeEnabled(): Boolean {
        return false
    }
}