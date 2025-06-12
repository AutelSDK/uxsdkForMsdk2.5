package com.autel.widget.function.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import com.autel.common.delegate.function.FunctionType
import com.autel.widget.function.inter.IFunctionManager
import com.autel.widget.function.inter.IFunctionOperate
import com.autel.widget.function.model.EmptyFunctionModel
import com.autel.widget.function.model.SwitchFunctionModel
import com.drakeet.multitype.MultiTypeAdapter


abstract class BaseFunctionBarView @JvmOverloads constructor(context: Context, attr: AttributeSet? = null) : FrameLayout(context, attr),
    IFunctionOperate {

    protected var manager: IFunctionManager? = null

    protected val multiTypeAdapter: MultiTypeAdapter = MultiTypeAdapter()

    open fun attachFunctionManager(manager: IFunctionManager) {
        this.manager = manager
    }

    override fun refreshFunction(functionItem: SwitchFunctionModel) {
        val index = multiTypeAdapter.items.indexOf(functionItem)
        if (index == -1) return
        multiTypeAdapter.notifyItemChanged(index)
    }

    override fun refreshAllFunction() {
        multiTypeAdapter.notifyDataSetChanged()
    }

    override fun notifyItemInserted(position: Int) {
        multiTypeAdapter.notifyItemInserted(position)
    }

    override fun notifyItemRemoved(position: Int) {
        multiTypeAdapter.notifyItemRemoved(position)
    }

    override fun notifyItemChanged(position: Int) {
        multiTypeAdapter.notifyItemChanged(position)
    }

    fun enterEditMode() {
        multiTypeAdapter.items.forEach {
            if (it is SwitchFunctionModel && it.functionModel.functionType != FunctionType.FunctionMore) {
                it.isEdit = true
            } else if (it is EmptyFunctionModel) {
                it.edit = true
            }
        }
        multiTypeAdapter.notifyDataSetChanged()
    }

    open fun exitEditMode() {
        multiTypeAdapter.items.forEach {
            if (it is SwitchFunctionModel) {
                it.isEdit = false
            } else if (it is EmptyFunctionModel) {
                it.edit = false
            }
        }
        multiTypeAdapter.notifyDataSetChanged()
    }

    override fun isShowMoreFunction(isShow: Boolean) {
        // NO-OP
    }
}