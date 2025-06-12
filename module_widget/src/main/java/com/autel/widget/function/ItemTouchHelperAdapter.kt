package com.autel.widget.function

import androidx.recyclerview.widget.RecyclerView

/**
 * Created by  2023/2/13
 */
interface ItemTouchHelperAdapter {
    fun onItemMove(holder: RecyclerView.ViewHolder, fromPosition: Int, targetPosition: Int): Boolean

    fun onItemSelect(holder: RecyclerView.ViewHolder?)

    fun onItemClear(holder: RecyclerView.ViewHolder)
}