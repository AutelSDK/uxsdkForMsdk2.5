package com.autel.ux.panel.topbar

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.autel.widget.databinding.UxWidgetTopBarPanelBinding

/**
 * 顶部状态栏
 */
class TopBarPanelWidget @JvmOverloads constructor(context: Context, attributeSet: AttributeSet? = null) : ConstraintLayout(context, attributeSet) {

    private val binding = UxWidgetTopBarPanelBinding.inflate(LayoutInflater.from(context), this, true)

}