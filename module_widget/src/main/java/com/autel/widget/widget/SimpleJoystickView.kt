package com.autel.widget.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import com.autel.widget.R
import com.autel.widget.databinding.ViewSimpleJoystickBinding


/**
 * 建议摇杆界面
 */
class SimpleJoystickView(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int,
    defStyleRes: Int
) : ConstraintLayout(context, attrs, defStyleAttr, defStyleRes) {

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
    ) : this(context, attrs, defStyleAttr, 0)

    constructor(
        context: Context,
        attrs: AttributeSet?,
    ) : this(context, attrs, 0, 0)

    constructor(
        context: Context,
    ) : this(context, null, 0, 0)

    private val binding: ViewSimpleJoystickBinding =
        ViewSimpleJoystickBinding.inflate(LayoutInflater.from(context), this, true)

    fun direction(
        left: Boolean = false,
        top: Boolean = false,
        right: Boolean = false,
        bottom: Boolean = false
    ) {
        select(binding.left, left)
        select(binding.top, top)
        select(binding.right, right)
        select(binding.bottom, bottom)
        binding.outerRing.setImageResource(
            if (left || top || right || bottom) {
                R.drawable.mission_bg_joystick_outer_ring_select
            } else {
                R.drawable.mission_bg_joystick_outer_ring
            }
        )
    }

    private fun select(iv: ImageView, isSelect: Boolean) {
        iv.setImageResource(if (isSelect) R.drawable.mission_bg_joystick_top_select else R.drawable.mission_bg_joystick_top)
    }
}