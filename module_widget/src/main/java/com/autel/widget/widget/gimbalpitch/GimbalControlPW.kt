package com.autel.widget.widget.gimbalpitch

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import com.autel.common.widget.BasePopWindow
import com.autel.widget.R
import com.autel.widget.databinding.MissionLayoutGimbalControlBinding

/**
 * @author 
 * @date 2023/1/29
 * 云台操控 快捷弹框，回中，朝下
 */
class GimbalControlPopupWindow(context: Context, pitchMax: Int, pitchMin: Int) : BasePopWindow(context) {

    private val binding = MissionLayoutGimbalControlBinding.inflate(LayoutInflater.from(context))
    private var gimbalListener: IGimbalInterface? = null

    init {
        contentView = binding.root
        binding.llGimbalMiddle.setOnClickListener {
            gimbalListener?.gimbalValue(0)
        }
        binding.llGimbalBottom.setOnClickListener {
            gimbalListener?.gimbalValue(90)
        }
        binding.llGimbalAngle45.setOnClickListener {
            gimbalListener?.gimbalValue(45)
        }
        //<30 0 , [30,45) 1 ,[45,90) 2 , >=90 3
        val rangeMax = if (pitchMax < 30) {
            0
        } else if (pitchMax >= 30 && pitchMax < 45) {
            1
        } else if (pitchMax >= 45 && pitchMax < 90) {
            2
        } else {
            3
        }
        if (rangeMax == 0) {
            binding.llGimbalTop.visibility = View.GONE
            binding.llGimbalAngleTop45.visibility = View.GONE
            binding.vLineTop.visibility = View.GONE
            binding.vLineTop45.visibility = View.GONE
        } else if (rangeMax == 1) {
            binding.llGimbalTop.visibility = View.GONE
            binding.vLineTop.visibility = View.GONE
            binding.llGimbalAngleTop45.visibility = View.VISIBLE
            binding.ivGimbalAngleTop45.setImageResource(R.drawable.mission_ic_gimbal_angle_top_30)
        } else if (rangeMax == 2) {
            binding.llGimbalTop.visibility = View.GONE
            binding.vLineTop.visibility = View.GONE
            binding.llGimbalAngleTop45.visibility = View.VISIBLE
            binding.ivGimbalAngleTop45.setImageResource(R.drawable.mission_ic_gimbal_angle_top_45)
        } else {
            binding.llGimbalTop.visibility = View.VISIBLE
            binding.llGimbalAngleTop45.visibility = View.VISIBLE
            binding.ivGimbalAngleTop45.setImageResource(R.drawable.mission_ic_gimbal_angle_top_45)
        }
        binding.llGimbalTop.setOnClickListener {
            gimbalListener?.gimbalValue(-90)
        }
        binding.llGimbalAngleTop45.setOnClickListener {
            if (rangeMax == 1) {
                gimbalListener?.gimbalValue(-30)
            } else {
                gimbalListener?.gimbalValue(-45)
            }
        }
    }

    fun setGimbalListener(gimbalListener: IGimbalInterface) {
        this.gimbalListener = gimbalListener
    }

    fun show(view: View) {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        binding.root.measure(widthSpec, heightSpec)
        val rootHeight = binding.root.measuredHeight
        showAtLocation(
            view,
            Gravity.START or Gravity.TOP,
            location[0] + view.width + context.resources.getDimensionPixelSize(R.dimen.common_10dp),
            (location[1] - (rootHeight / 2 - view.height / 2))
        )
    }

    interface IGimbalInterface {
        fun gimbalValue(value: Int)
    }
}