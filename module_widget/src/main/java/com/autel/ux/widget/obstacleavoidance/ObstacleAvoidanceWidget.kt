package com.autel.ux.widget.obstacleavoidance

import android.content.Context
import android.service.controls.Control
import android.util.AttributeSet
import androidx.core.view.isVisible
import com.autel.drone.sdk.vmodelx.manager.data.ControlMode
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.enums.GearLevelEnum
import com.autel.ux.core.base.AutelSDKModel
import com.autel.ux.core.base.widget.ConstraintLayoutWidget
import com.autel.widget.R
import com.autel.widget.databinding.UxWidgetObstacleAvoidanceBinding
import kotlinx.coroutines.flow.combine

class ObstacleAvoidanceWidget @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
) : ConstraintLayoutWidget(context, attrs) {

    private lateinit var binding: UxWidgetObstacleAvoidanceBinding

    private val widgetModel: ObstacleAvoidanceModel by lazy {
        ObstacleAvoidanceModel(AutelSDKModel.getInstance())
    }

    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        binding = UxWidgetObstacleAvoidanceBinding.inflate(layoutInflater, this)
    }

    override fun reactToModelChanges() {
        combine(
            widgetModel.obstacleAvoidanceEnabled,
            widgetModel.deviceGearLevel,
            widgetModel.productConnected
        ) { isEnable, gearLevel, isConnect ->
            if (!isConnect) {
                R.drawable.mission_icon_vision_off
            } else {
                if (isEnable) {
                    if (gearLevel == GearLevelEnum.SPORT) {
                        R.drawable.mission_icon_vision_none
                    } else {
                        R.drawable.mission_icon_vision_all
                    }
                } else {
                    R.drawable.mission_icon_vision_none
                }
            }
        }.collectInWidget {
            binding.ivObstacleAvoidance.setImageResource(it)
        }

        widgetModel.controlMode.collectInWidget {
            isVisible = it.controlMode == ControlMode.SINGLE
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        widgetModel.setup()
    }

    override fun onDetachedFromWindow() {
        widgetModel.cleanup()
        super.onDetachedFromWindow()
    }
}