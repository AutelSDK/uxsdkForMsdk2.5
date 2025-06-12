package com.autel.widget.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.autel.widget.databinding.WidgetTitleAndSideBinding

class TitleAndSideView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0) :
    ConstraintLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val binding: WidgetTitleAndSideBinding = WidgetTitleAndSideBinding.inflate(LayoutInflater.from(context), this, true)

    fun setTitle(title: String) {
        binding.title.text = title
    }

    fun setSideText(sideText: String) {
        binding.sideTitle.text = sideText
        binding.sideTitle.isVisible = sideText.isNotEmpty()
    }

    fun setTips(tips: String) {
        binding.tips.text = tips
        binding.tips.isVisible = tips.isNotEmpty()
    }
}