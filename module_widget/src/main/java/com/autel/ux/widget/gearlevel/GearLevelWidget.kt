package com.autel.ux.widget.gearlevel

import android.content.Context
import android.util.AttributeSet
import androidx.core.view.isVisible
import com.autel.common.utils.DeviceUtils
import com.autel.common.utils.UIUtils.getString
import com.autel.common.widget.GearLevelSwitchDialog
import com.autel.drone.sdk.vmodelx.manager.data.ControlMode
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.enums.GearLevelEnum
import com.autel.ux.core.base.AutelSDKModel
import com.autel.ux.core.base.widget.ConstraintLayoutWidget
import com.autel.widget.R
import com.autel.widget.databinding.UxWidgetGearLevelBinding

/**
 * 飞机档位
 */
class GearLevelWidget @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
) : ConstraintLayoutWidget(context, attrs) {

    private lateinit var binding: UxWidgetGearLevelBinding

    private val widgetModel: GearLevelModel by lazy {
        GearLevelModel(AutelSDKModel.getInstance())
    }

    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        binding = UxWidgetGearLevelBinding.inflate(layoutInflater, this)

        binding.tvGear.setOnClickListener {
            if (widgetModel.productConnected.value.not()) return@setOnClickListener
            if (DeviceUtils.isMainRC().not()) return@setOnClickListener
            when (widgetModel.deviceGearLevel.value) {
                GearLevelEnum.NORMAL -> {
                    GearLevelSwitchDialog(context).apply {
                        setOnConfirmBtnClick {
                            widgetModel.setFlightGearLevel(GearLevelEnum.SPORT)
                        }
                        show()
                    }
                }

                GearLevelEnum.SPORT -> {
                    widgetModel.setFlightGearLevel(GearLevelEnum.LOW_SPEED)
                }

                GearLevelEnum.LOW_SPEED -> {
                    widgetModel.setFlightGearLevel(GearLevelEnum.SMOOTH)
                }

                else -> {
                    widgetModel.setFlightGearLevel(GearLevelEnum.NORMAL)
                }
            }
        }
    }

    override fun reactToModelChanges() {
        widgetModel.deviceGearLevel.collectInWidget(::updateGearLevel)

        widgetModel.controlMode.collectInWidget {
            isVisible = it.controlMode == ControlMode.SINGLE
        }


    }

    private fun updateGearLevel(level: GearLevelEnum) {
        val gear = when (level) {
            GearLevelEnum.SMOOTH -> {
                getString(R.string.common_text_comfort_gear)
            }

            GearLevelEnum.SPORT -> {
                getString(R.string.common_text_sport_gear)
            }

            GearLevelEnum.LOW_SPEED -> {
                getString(R.string.common_text_gear_low_speed)
            }

            else -> {
                context.getString(R.string.common_text_standard_gear)
            }
        }
        binding.tvGear.text = gear
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