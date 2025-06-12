package com.autel.widget.function.model

import android.view.View
import com.autel.common.delegate.function.FunctionModel
import com.autel.common.delegate.function.FunctionViewType

/**
 * 功能开关类的Item
 */
class SwitchFunctionModel(
    var isEdit: Boolean = false, //是否出于编辑态
    val editListener: ((isAdd: Boolean, functionItem: SwitchFunctionModel) -> Unit)?,
    val clickListener: (viewType: FunctionViewType, view: View) -> Unit,
    val functionModel: FunctionModel
) : IFunctionModel