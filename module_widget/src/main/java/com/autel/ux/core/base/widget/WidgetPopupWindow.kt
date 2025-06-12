package com.autel.ux.core.base.widget

import android.content.Context
import android.view.View
import android.view.ViewGroup
import com.autel.common.widget.BasePopWindow

class WidgetPopupWindow {

    companion object {
        fun build(context: Context, layout: View): BasePopWindow {
            val window = BasePopWindow(context)
            window.contentView = layout
            window.width = ViewGroup.LayoutParams.WRAP_CONTENT
            window.height = ViewGroup.LayoutParams.WRAP_CONTENT
            return window
        }
    }

}