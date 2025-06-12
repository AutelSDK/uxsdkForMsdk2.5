package com.external.uxdemo.function

import com.autel.common.delegate.function.FunctionModel
import com.autel.widget.function.inter.IFunctionOperate
import com.autel.widget.function.model.IFunctionModel
import com.autel.widget.function.model.SwitchFunctionModel

interface FunctionStyleImpl {
    /** 保存bar*/
    fun saveBarToStorage()

    /** 保存panel*/
    fun savePanelToStorage()

    /** 绑定panel Operate*/
    fun bindPanelOperate(operate: IFunctionOperate)

    /** 绑定Bar Operate*/
    fun bindBarOperate(operate: IFunctionOperate)

    /** 获取panel列表*/
    fun getFunctionPanelList(): List<SwitchFunctionModel>

    /** 获取bar列表*/
    fun getFunctionBarList(): List<IFunctionModel>

    fun switchBarToPanelList(functionItem: SwitchFunctionModel)

    fun switchPanelToBarList(functionItem: SwitchFunctionModel)

    /**重置Function**/
    fun resetFunction()

    fun updateFunction(functionModel: FunctionModel)

    fun refreshAllFunction()
}