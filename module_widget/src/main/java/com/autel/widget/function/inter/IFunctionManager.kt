package com.autel.widget.function.inter

import android.view.View
import com.autel.common.delegate.function.FunctionViewType
import com.autel.widget.function.model.IFunctionModel
import com.autel.widget.function.model.SwitchFunctionModel

/**
 * Created by  2023/5/17
 *  功能管理接口
 */
interface IFunctionManager {
    /** 保存bar*/
    fun saveBarToStorage()

    /** 保存panel*/
    fun savePanelToStorage()

    /** 绑定panel Operate*/
    fun bindPanelInBarStyleOperate(operate: IFunctionOperate)

    /** 绑定Bar Operate*/
    fun bindBarInBarStyleOperate(operate: IFunctionOperate)

    /** 绑定panel Operate*/
    fun bindPanelInFloatStyleOperate(operate: IFunctionOperate)

    /** 绑定Bar Operate*/
    fun bindBarInFloatStyleOperate(operate: IFunctionOperate)

    /** 获取panel列表*/
    fun getFunctionPanelList(): List<SwitchFunctionModel>

    /** 获取bar列表*/
    fun getFunctionBarList(): List<IFunctionModel>

    /**获取更多功能*/
    fun getMoreFunctionModel(): SwitchFunctionModel

    /**重置Function**/
    fun resetFunction()

    /**
     * 打开面板并进入编辑模式
     */
    fun openFunctionPanelAndEnterEditMode(viewType: FunctionViewType, view: View)
}