package com.autel.widget.function.inter


interface IFunctionPanelHandleListener {
    /**
     * 进入编辑模式
     */
    fun enterEditModel()

    /**
     * 退出编辑模式
     */
    fun exitEditModel()

    /**隐藏面板**/
    fun hiddenPanel()
}