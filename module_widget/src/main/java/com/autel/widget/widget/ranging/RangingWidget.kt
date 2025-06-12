package com.autel.widget.widget.ranging

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.autel.common.base.widget.ConstraintLayoutWidget
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.module.camera.bean.GimbalTypeEnum
import com.autel.drone.sdk.vmodelx.module.camera.bean.LensTypeEnum
import com.autel.widget.R
import com.autel.widget.databinding.MissionLayoutLidarViewBinding
import com.autel.common.model.lens.ILens

/**
 * Created by  2024/11/12
 * 激光雷达测距控件
 */
class RangingWidget @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    ConstraintLayoutWidget(context, attrs, defStyleAttr), ILens {
    private val binding: MissionLayoutLidarViewBinding = MissionLayoutLidarViewBinding.inflate(LayoutInflater.from(context), this)

    private var drone: IAutelDroneDevice? = null
    private var gimbal: GimbalTypeEnum? = null
    private var lensType: LensTypeEnum? = null

    private val rangingVm: RangingVM by lazy {
        RangingVM()
    }

    init {
        binding.tvTargetN.text = context.getString(R.string.common_north_N)
        binding.tvTargetE.text = context.getString(R.string.common_north_E)
    }

    override fun reactToModelChanges() {
        super.reactToModelChanges()
        rangingVm.rangingModelFlow.subscribe {
            it?.let {
                setRangingModel(it)
            }
        }
    }

    private fun setRangingModel(model: RangingModel) {
        if (model.laserDistanceIsValid) {
            binding.tvRng.text = model.targetRng
            binding.tvAsl.text = model.targetAsl
            if (model.targetIsInvalid) {
                binding.tvTargetNValue.text = context.getString(R.string.common_text_no_value)
                binding.tvTargetEValue.text = context.getString(R.string.common_text_no_value)
            } else {
                binding.tvTargetNValue.text = model.targetLat
                binding.tvTargetEValue.text = model.targetLng
            }
        } else {
            binding.tvRng.text = context.getString(R.string.common_text_no_value)
            binding.tvAsl.text = context.getString(R.string.common_text_no_value)
            binding.tvTargetNValue.text = context.getString(R.string.common_text_no_value)
            binding.tvTargetEValue.text = context.getString(R.string.common_text_no_value)
        }
        binding.tvAslTitle.text = context.getString(com.autel.common.R.string.common_text_MSL)
    }



    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        rangingVm.setup()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        rangingVm.cleanup()
    }

    override fun getDrone(): IAutelDroneDevice? {
        return drone
    }

    override fun getGimbal(): GimbalTypeEnum? {
        return gimbal
    }

    override fun getLensTypeEnum(): LensTypeEnum? {
        return lensType
    }

    override fun updateLensInfo(drone: IAutelDroneDevice?, gimbal: GimbalTypeEnum?, lensType: LensTypeEnum?) {
        this.drone = drone
        this.gimbal = gimbal
        this.lensType = lensType
        rangingVm.updateDevice(drone)
    }
}