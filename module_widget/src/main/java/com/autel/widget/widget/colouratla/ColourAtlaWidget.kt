package com.autel.widget.widget.colouratla

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import com.autel.common.base.widget.ConstraintLayoutWidget
import com.autel.common.sdk.ModelXDroneConst
import com.autel.common.utils.NumberParseUtil
import com.autel.common.utils.TransformUtils
import com.autel.common.widget.BasePopWindow
import com.autel.common.widget.toast.AutelToast
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.camera.enums.ThermalColorEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.camera.enums.ThermalGainEnum
import com.autel.drone.sdk.vmodelx.module.camera.bean.GimbalTypeEnum
import com.autel.drone.sdk.vmodelx.module.camera.bean.IrGain
import com.autel.drone.sdk.vmodelx.module.camera.bean.LensTypeEnum
import com.autel.widget.R
import com.autel.widget.databinding.WidgetLayoutColourAtlaBinding
import com.autel.common.model.lens.ILens
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * Created by  2023/9/15
 * 伪彩功能开发
 */
class ColourAtlaWidget @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0) :
    ConstraintLayoutWidget(context, attrs, defStyleAttr, defStyleRes), ILens {

    companion object {
        private const val SWITCH_THERMAL_GAIN_TIME = 6_000L
        private const val SWITCH_THERMAL_GAIN_LOADING_DURATION = 1_000L
    }

    private val uiBinding: WidgetLayoutColourAtlaBinding
    private val cancelLoadingRunnable: Runnable
    private var colourAtlaPopupWindow: ColourAtlaPopupWindow? = null
    private var simpleModel = false
    private var isInLoading = false

    private val switchGainLoadingAnimation = RotateAnimation(
        0f,
        360f,
        Animation.RELATIVE_TO_SELF,
        0.5f,
        Animation.RELATIVE_TO_SELF,
        0.5f
    )


    private val widgetModel: ColourAtlaVM by lazy {
        ColourAtlaVM()
    }

    init {
        uiBinding = WidgetLayoutColourAtlaBinding.inflate(LayoutInflater.from(context), this)
        switchGainLoadingAnimation.duration = SWITCH_THERMAL_GAIN_LOADING_DURATION
        // 动画的重复次数
        switchGainLoadingAnimation.repeatCount = Animation.INFINITE
        switchGainLoadingAnimation.interpolator = LinearInterpolator()
        cancelLoadingRunnable = Runnable {
            isInLoading = false
            uiBinding.ivModelChangeLoading.clearAnimation()
            switchGainLoadingAnimation.cancel()
            refreshUIStyle()
        }
        uiBinding.ivColourAtla.setOnClickListener {
            uiBinding.ivColourAtla.isSelected = true
            if (colourAtlaPopupWindow == null) {
                colourAtlaPopupWindow = ColourAtlaPopupWindow(context)
                updateColourAtlaPopupWindow()
                widgetModel.thermalColorFlow.replayCache.firstOrNull()?.let {
                    colourAtlaPopupWindow?.setThermalColorEnum(it)
                }
                colourAtlaPopupWindow?.setColourAtlaSelectListener(object : IColourAtlaSelectListner {
                    override fun onColourAtlaSelect(colorEnum: ThermalColorEnum) {
                        scope?.launch(CoroutineExceptionHandler { _, throwable ->

                        }) {
                            widgetModel.setCameraThermalColor(colorEnum)
                        }
                    }
                })
                colourAtlaPopupWindow!!.setOnDismissListener {
                    uiBinding.ivColourAtla.isSelected = false
                }
            }
            colourAtlaPopupWindow?.showOnAnchor(
                it,
                BasePopWindow.HorizontalPosition.ALIGN_RIGHT,
                BasePopWindow.VerticalPosition.ALIGN_TOP,
                context.resources.getDimensionPixelOffset(R.dimen.common_7dp),
                0,
                false
            )
        }

        uiBinding.modelChange.setOnClickListener {
            uiBinding.modelChange.visibility = View.INVISIBLE
            uiBinding.ivModelChangeLoading.visibility = View.VISIBLE
            isInLoading = true
            uiBinding.ivModelChangeLoading.startAnimation(switchGainLoadingAnimation)
            uiBinding.modelChange.postDelayed(cancelLoadingRunnable, SWITCH_THERMAL_GAIN_TIME)
            scope?.launch(CoroutineExceptionHandler { _, throwable ->

            }) {
                val enum = if (widgetModel.thermalGainFlow.firstOrNull() == ThermalGainEnum.LOW) {
                    ThermalGainEnum.HIGH
                } else {
                    ThermalGainEnum.LOW
                }
                widgetModel.setCameraThermalGain(enum)
                uiBinding.modelChange.postDelayed({
                    if (enum == ThermalGainEnum.HIGH) {
                        AutelToast.normalToast(context, R.string.common_text_temperature_measurement_mode_accurate)
                    } else if (enum == ThermalGainEnum.LOW) {
                        AutelToast.normalToast(context, R.string.common_text_temperature_measurement_mode_wide_range)
                    }
                }, SWITCH_THERMAL_GAIN_TIME)
            }
        }
        uiBinding.tvFfc.setOnClickListener {
            scope?.launch(CoroutineExceptionHandler { _, throwable ->
                AutelToast.normalToast(context, R.string.common_text_ffc_calibration_failed)
            }) {
                widgetModel.actionCameraFfc()
                AutelToast.normalToast(context, R.string.common_text_ffc_calibration_success)
            }
        }
    }

    fun setSimpleMode(simpleMode: Boolean) {
        this.simpleModel = simpleMode
        refreshUIStyle()
    }

    private fun refreshUIStyle() {
        if (simpleModel || widgetModel.canTempMeasureFlow.replayCache.firstOrNull() != true) {
            uiBinding.modelChange.visibility = View.GONE
            uiBinding.ivModelChangeLoading.visibility = View.GONE
            uiBinding.tvFfc.visibility = View.GONE
        } else {
            if (isInLoading) {
                uiBinding.modelChange.visibility = View.INVISIBLE
                uiBinding.ivModelChangeLoading.visibility = View.VISIBLE
                uiBinding.tvFfc.visibility = View.VISIBLE
            } else {
                uiBinding.modelChange.visibility = View.VISIBLE
                uiBinding.ivModelChangeLoading.visibility = View.GONE
                uiBinding.tvFfc.visibility = View.VISIBLE
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isInEditMode) {
            widgetModel.setup()
        }
    }


    override fun onDetachedFromWindow() {
        if (!isInEditMode) {
            widgetModel.cleanup()
        }
        super.onDetachedFromWindow()
    }


    override fun reactToModelChanges() {
        super.reactToModelChanges()
        widgetModel.thermalColorFlow.subscribe {
            colourAtlaPopupWindow?.setThermalColorEnum(it)
        }
        widgetModel.thermalGainFlow.subscribe {
            setThermalGainMode(it)
        }
        widgetModel.canTempMeasureFlow.subscribe {
            refreshUIStyle()
        }
    }

    private fun setThermalGainMode(enum: ThermalGainEnum) {
        val drone = getDrone()
        val lensTypeEnum = getLensTypeEnum()
        var irGain: IrGain? = null
        if (drone != null && lensTypeEnum != null) {
            irGain = drone.getCameraAbilitySetManger().getCameraSupport2()?.getIrGain(lensTypeEnum)
        }
        when (enum) {
            ThermalGainEnum.HIGH -> {
                val min =
                    NumberParseUtil.formatFloat(
                        TransformUtils.centigrade2DefalutWithoutUnit(
                            irGain?.highGain?.min?.toFloat() ?: ModelXDroneConst.DRONE_THERMAL_GAIN_HIGH_TEMP_MIN
                        ), 0
                    )
                        .toInt()
                val max =
                    NumberParseUtil.formatFloat(
                        TransformUtils.centigrade2DefalutWithoutUnit(
                            irGain?.highGain?.max?.toFloat() ?: ModelXDroneConst.DRONE_THERMAL_GAIN_HIGH_TEMP_MAX
                        ), 0
                    )
                        .toInt()
                uiBinding.tempInterval.text = "${min}~${max}"
            }

            ThermalGainEnum.LOW -> {
                val min =
                    NumberParseUtil.formatFloat(
                        TransformUtils.centigrade2DefalutWithoutUnit(
                            irGain?.lowGain?.min?.toFloat() ?: ModelXDroneConst.DRONE_THERMAL_GAIN_LOW_TEMP_MIN
                        ), 0
                    )
                        .toInt()
                val max =
                    NumberParseUtil.formatFloat(
                        TransformUtils.centigrade2DefalutWithoutUnit(
                            irGain?.lowGain?.max?.toFloat() ?: ModelXDroneConst.DRONE_THERMAL_GAIN_LOW_TEMP_MAX
                        ), 0
                    )
                        .toInt()
                uiBinding.tempInterval.text = "${min}~${max}"
            }

            else -> {

            }
        }
    }

    private fun updateColourAtlaPopupWindow() {
        val cacheFlow = widgetModel.thermalColorFlow.replayCache.firstOrNull()
        val list = getLensTypeEnum()?.let { lensTypeEnum ->
            getDrone()?.getCameraAbilitySetManger()
                ?.getCameraSupport2()
                ?.supportedIrColor(lensTypeEnum)
                ?.map { colorEnum ->
                    ColourAtlaModel(colorEnum, colorEnum == cacheFlow)
                }
        } ?: emptyList<ColourAtlaModel>()
        colourAtlaPopupWindow?.updateColourAtlaModelList(list)
    }

    override fun getDrone(): IAutelDroneDevice? {
        return widgetModel.getDrone()
    }

    override fun getGimbal(): GimbalTypeEnum? {
        return widgetModel.getGimbal()
    }

    override fun getLensTypeEnum(): LensTypeEnum? {
        return widgetModel.getLensTypeEnum()
    }

    override fun updateLensInfo(drone: IAutelDroneDevice?, gimbal: GimbalTypeEnum?, lensType: LensTypeEnum?) {
        widgetModel.updateLensInfo(drone, gimbal, lensType)
        updateColourAtlaPopupWindow()
        val cacheGain = widgetModel.thermalGainFlow.replayCache.firstOrNull()
        val cacheColor = widgetModel.thermalColorFlow.replayCache.firstOrNull()
        if (cacheGain == null || cacheGain == ThermalGainEnum.UNKNOWN
            || cacheColor == null || cacheColor == ThermalColorEnum.UNKNOWN
        ) {
            scope?.launch(CoroutineExceptionHandler { _, throwable ->

            }) {
                widgetModel.getCameraThermalColor()
                widgetModel.getCameraThermalGain()
            }
        }
    }

    fun hiddenAtlaPopupWindow() {
        this.colourAtlaPopupWindow?.dismiss()
    }

    fun refreshUnit() {
        widgetModel.clear()
    }


}