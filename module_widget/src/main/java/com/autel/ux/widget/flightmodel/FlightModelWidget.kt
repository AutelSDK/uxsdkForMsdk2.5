package com.autel.ux.widget.flightmodel

import android.content.Context
import android.util.AttributeSet
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.autel.drone.sdk.vmodelx.manager.data.ControlMode
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.enums.AiServiceStatueEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.enums.AiServiceStatueEnum.AI_RECOGNITION
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.enums.AiServiceStatueEnum.INTELLIGENT_TRACKING
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.enums.DroneWorkModeEnum
import com.autel.ux.core.base.AutelSDKModel
import com.autel.ux.core.base.widget.ConstraintLayoutWidget
import com.autel.ux.core.model.FlyModeEnum
import com.autel.widget.databinding.UxWidgetFlightModeBinding
import kotlinx.coroutines.flow.combine

/**
 * 飞行模式
 */
class FlightModelWidget @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
) : ConstraintLayoutWidget(context, attrs) {

    private lateinit var binding: UxWidgetFlightModeBinding

    private val widgetModel: FlightModelModel by lazy {
        FlightModelModel(AutelSDKModel.getInstance())
    }

    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        binding = UxWidgetFlightModeBinding.inflate(layoutInflater, this)
    }

    override fun reactToModelChanges() {
        combine(widgetModel.flightMode, widgetModel.aiServiceStatus) { work, ai ->
            changeToFlightMode(work, ai)
        }.collectInWidget {
            binding.flightMode.setText(it.txtRes)
        }

        combine(widgetModel.productConnected, widgetModel.controlMode) { connect, mode ->
            connect && mode.controlMode == ControlMode.SINGLE
        }.collectInWidget {
            isInvisible = !it
        }
    }

    private fun changeToFlightMode(workModel: DroneWorkModeEnum, aiStatus: AiServiceStatueEnum): FlyModeEnum {
        var isNeedShowAIStatus = false
        var flyMode: FlyModeEnum = when (workModel) {
            DroneWorkModeEnum.SMART_MODE_ORBIT_MODELING -> {
                FlyModeEnum.SINGLE_SURROUND
            }

            DroneWorkModeEnum.RETURN -> {
                FlyModeEnum.RETURN_HOME

            }

            DroneWorkModeEnum.LAND, DroneWorkModeEnum.LAND_MANUAL -> {
                FlyModeEnum.LANDING

            }

            DroneWorkModeEnum.WAYPOINT -> {
                FlyModeEnum.POINT_TASK

            }

            DroneWorkModeEnum.RECTANGLE -> {
                FlyModeEnum.RECT_TASK

            }

            DroneWorkModeEnum.POLYGON -> {
                FlyModeEnum.POLYGON_TASK

            }

            DroneWorkModeEnum.PTHOTOGRAPHY -> {
                FlyModeEnum.OBLIQUE_TASK

            }

            DroneWorkModeEnum.AIR_STRIP -> {
                FlyModeEnum.BELT_TASK

            }

            DroneWorkModeEnum.POLYGONAL_IMITATION -> {
                FlyModeEnum.EARTH_IMITATING

            }

            DroneWorkModeEnum.MONOMER_SURROUND -> {
                FlyModeEnum.SINGLE_SURROUND

            }

            DroneWorkModeEnum.DISARM -> {
                isNeedShowAIStatus = true
                FlyModeEnum.DEFAULT
            }

            DroneWorkModeEnum.INTEREST_POINT -> {
                FlyModeEnum.QUICK_TASK

            }

            DroneWorkModeEnum.TRACK -> {
                FlyModeEnum.LOCK_TARGET

            }

            DroneWorkModeEnum.INFRARED_TRACK -> {
                FlyModeEnum.LOCK_TARGET

            }

            DroneWorkModeEnum.SMART_MODE_KML -> {
                FlyModeEnum.WAYLINE_MISSION

            }

            DroneWorkModeEnum.MANUAL_NORMAL -> {
                isNeedShowAIStatus = true
                FlyModeEnum.DEFAULT
            }

            else -> {
                FlyModeEnum.DEFAULT
            }
        }
        if (isNeedShowAIStatus) {
            flyMode = when (aiStatus) {
                AI_RECOGNITION -> {
                    FlyModeEnum.AI
                }

                INTELLIGENT_TRACKING -> {
                    FlyModeEnum.LOCK_TARGET
                }

                else -> {
                    flyMode
                }
            }
        }
        return flyMode
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