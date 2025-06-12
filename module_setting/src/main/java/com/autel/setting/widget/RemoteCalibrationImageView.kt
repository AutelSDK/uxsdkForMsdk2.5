package com.autel.setting.widget

import android.content.Context
import android.util.AttributeSet
import com.autel.setting.R

/**
 * @Author create by LJ
 * @Date 2022/10/26 16
 */
class RemoteCalibrationImageView(context: Context, attributeSet: AttributeSet?, def: Int) :
    androidx.appcompat.widget.AppCompatImageView(context, attributeSet, def) {

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, 0)

    //是否已经校准
    private var calibrationState: Boolean = false


    fun setIsCalibration(calibration: Boolean) {
        if(calibration){
           setImageResource(R.drawable.setting_calibration_arrow_already)
        }else{
            setImageResource(R.drawable.setting_calibration_arrow_normal)
        }
        calibrationState = calibration
    }

    fun isCalibrationState(): Boolean {
        return calibrationState
    }
}