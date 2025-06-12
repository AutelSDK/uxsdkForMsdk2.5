package com.external.uxdemo.function

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.autel.common.delegate.IMainProvider
import com.autel.common.delegate.function.AbsDelegateFunction
import com.autel.common.delegate.function.FunctionType
import com.autel.common.delegate.function.FunctionViewType
import com.autel.common.delegate.layout.DelegateLayoutType
import com.autel.common.delegate.layout.IMainPanelListener
import com.autel.widget.function.inter.IFunctionPanelHandleListener
import com.autel.widget.function.view.FunctionPanelView
import com.external.uxddemo.R

/**
 * Created by  2023/9/7
 * 更多工具
 */
class MoreFunctionEntry(mainProvider: IMainProvider) : AbsDelegateFunction(mainProvider) {

    private val panelView = FunctionPanelView(mainProvider.getMainContext())
    private val panelRemoveListener = object : IMainPanelListener {
        override fun panelShow() {
            functionModel.isOn = true
            mainProvider.getMainHandler().updateFunctionState(functionModel)
        }

        override fun panelRemove() {
            functionModel.isOn = false
            mainProvider.getMainHandler().updateFunctionState(functionModel)
        }
    }

    override fun getFunctionType(): FunctionType {
        return FunctionType.FunctionMore
    }

    override fun getFunctionName(): String {
        return mainProvider.getMainContext().getString(R.string.common_text_function_toolbox)
    }

    override fun getFunctionIconRes(): Int {
        return R.drawable.common_selector_shortcuts_function_more
    }

    override fun onFunctionCreate() {
        super.onFunctionCreate()
        mainProvider.getMainLayoutManager().addPanelListener(DelegateLayoutType.FunctionPanelType, panelRemoveListener)
        panelView.setPanelHandlerListener(object : IFunctionPanelHandleListener {
            override fun enterEditModel() {
                mainProvider.getMainHandler().enterFunctionEditModel()

            }

            override fun exitEditModel() {
                removePanel()
                mainProvider.getMainHandler().exitFunctionEditModel()
            }

            override fun hiddenPanel() {
                hiddenFunctionPanel()
            }
        })
    }

    override fun functionEnableDependsOnAircraft(): Boolean {
        return false
    }

    fun getPanelView(): FunctionPanelView {
        return panelView
    }

    override fun onFunctionDestroy() {
        super.onFunctionDestroy()
        mainProvider.getMainLayoutManager().removePanelRemoveListener(DelegateLayoutType.FunctionPanelType, panelRemoveListener)
    }

    override fun onFunctionStart(viewType: FunctionViewType, view: View) {
        super.onFunctionStart(viewType, view)
        mainProvider.getMainLayoutManager().showViewToPanel(
            DelegateLayoutType.FunctionPanelType,
            panelView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
    }

    override fun onFunctionStop(viewType: FunctionViewType, view: View) {
        super.onFunctionStop(viewType, view)
        hiddenFunctionPanel()
    }

    fun hiddenFunctionPanel() {
        panelView.exitEditMode()
        mainProvider.getMainHandler().exitFunctionEditModel()
        removePanel()
    }

    fun enterEditMode() {
        panelView.clickEnterEditMode()
    }

    private fun removePanel() {
        functionModel.isOn = false
        mainProvider.getMainHandler().updateFunctionState(functionModel)
        mainProvider.getMainLayoutManager().removeViewFromPanel(DelegateLayoutType.FunctionPanelType)
    }
}