package com.autel.widget.function.view

import android.content.Context
import android.os.SystemClock
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.autel.common.delegate.function.FunctionType
import com.autel.common.delegate.function.FunctionViewType
import com.autel.widget.R
import com.autel.widget.databinding.MissionLayoutFunctionItemViewBinding
import com.autel.widget.function.model.SwitchFunctionModel



class SwitchFunctionItemView @JvmOverloads constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int = 0) :
    ConstraintLayout(context, attrs, defStyleAttr) {

    companion object {
        private const val FUNCTION_CLICK_TIME_INTERVAL = 200
    }

    private val binding = MissionLayoutFunctionItemViewBinding.inflate(LayoutInflater.from(context), this)

    private var item: SwitchFunctionModel? = null

    private var isAdd = false

    private var lastClickTime = 0L

    fun setEditMode(isAdd: Boolean) {
        this.isAdd = isAdd
        if (isAdd) {
            binding.ivEdit.setImageResource(R.drawable.mission_ic_function_edit_add)
        } else {
            binding.ivEdit.setImageResource(R.drawable.mission_ic_function_edit_remove)
        }
    }

    fun setFunctionItem(viewType: FunctionViewType, item: SwitchFunctionModel) {
        this.item = item
        binding.ivFunction.setImageResource(item.functionModel.functionIconRes)
        binding.tvFunction.text = item.functionModel.functionName
        binding.tvFunction.isVisible = item.functionModel.isShowName

        binding.tvFunction.isSelected = item.functionModel.isOn
        binding.tvFunction.isEnabled = item.functionModel.isEnabled
        binding.tvFunction.setTextColor(context.getColorStateList(item.functionModel.functionTextColorRes))
        binding.ivFunction.isEnabled = item.functionModel.isEnabled
        binding.ivFunction.isSelected = item.functionModel.isOn
        binding.ivEdit.isVisible = item.isEdit
        binding.root.isEnabled = item.functionModel.isEnabled
        binding.virtualView.isVisible = item.isEdit
        binding.virtualView.setOnClickListener {
            if (binding.root.scaleX == 1.0f && binding.root.scaleY == 1.0f) {
                item.editListener?.invoke(isAdd, item)
            }
        }
        binding.root.setOnClickListener {
            val currentTime = SystemClock.elapsedRealtime()
            if (currentTime - lastClickTime > FUNCTION_CLICK_TIME_INTERVAL) {
                lastClickTime = currentTime
                item.clickListener.invoke(viewType, it)
            }
        }
    }

    fun setIconSizeAndFontSize(iconSize: Int, fontSize: Float) {
        binding.ivFunction.updateLayoutParams<ConstraintLayout.LayoutParams> {
            width = iconSize
            height = iconSize
        }
        binding.tvFunction.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize)
    }
}