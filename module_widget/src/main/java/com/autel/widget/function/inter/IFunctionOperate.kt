package com.autel.widget.function.inter

import com.autel.widget.function.model.SwitchFunctionModel


interface IFunctionOperate {
    /** 刷新功能状态 */
    fun refreshFunction(model: SwitchFunctionModel)

    /** 刷新全部功能 */
    fun refreshAllFunction()

    /** 插入数据*/
    fun notifyItemInserted(position: Int)

    /** 移除数据*/
    fun notifyItemRemoved(position: Int)

    /** 数据变更*/
    fun notifyItemChanged(position: Int)

    fun resetFunction()

    /**
     * 是否显示更多按钮
     */
    fun isShowMoreFunction(isShow :Boolean)
}