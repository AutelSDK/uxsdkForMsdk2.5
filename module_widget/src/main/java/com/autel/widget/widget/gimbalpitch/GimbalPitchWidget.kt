package com.autel.widget.widget.gimbalpitch

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import com.autel.common.base.widget.ConstraintLayoutWidget
import com.autel.common.utils.NumberParseUtil
import com.autel.common.widget.toast.AutelToast
import com.autel.widget.R
import com.autel.widget.databinding.LayoutWidgetGimbalPitchBinding
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * Created by  2023/5/28
 *  云台角度调整控件
 */
class GimbalPitchWidget @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0) :
    ConstraintLayoutWidget(context, attrs, defStyleAttr, defStyleRes) {

    private val uiBinding: LayoutWidgetGimbalPitchBinding

    private val widgetModel: GimbalPitchVM by lazy {
        GimbalPitchVM()
    }

    private var gimbalPbWindow: GimbalControlPopupWindow? =null

    init {
        uiBinding = LayoutWidgetGimbalPitchBinding.inflate(LayoutInflater.from(context), this)
        setBackgroundResource(R.drawable.mission_selector_icon_bg_all)
        setOnClickListener {
            val gimbal = widgetModel.gimbalPitchData.replayCache.firstOrNull()
            gimbalPbWindow = GimbalControlPopupWindow(context, (gimbal?.max ?: 0) / 100, (gimbal?.min ?: -90) / 100).apply {
                setGimbalListener(object : GimbalControlPopupWindow.IGimbalInterface {
                    override fun gimbalValue(value: Int) {
                        scope?.launch(CoroutineExceptionHandler { _, throwable ->
                            AutelToast.normalToast(context, R.string.common_text_gimbal_adjustment_failed)
                        }) {
                            widgetModel.setGimbalAngel(value.toFloat())
                            dismiss()
                        }
                    }
                })
                show(this@GimbalPitchWidget)
            }

            gimbalPbWindow?.show(this@GimbalPitchWidget)
        }
    }

    override fun reactToModelChanges() {
        super.reactToModelChanges()
        widgetModel.gimbalPitchData.subscribe {
            val isConnected = widgetModel.connectedData.firstOrNull() ?: false
            if (isConnected) {
                setGimbalPitchAngel(it.current)
            }
        }
        widgetModel.connectedData.subscribe {
            if (it) {
                showConnected()
            } else {
                showUnconnected()
            }
        }

        widgetModel.pbDismissState.subscribe {
            gimbalPbWindow?.dismiss()
        }
    }

    //云台俯仰角
    private fun setGimbalPitchAngel(value: Float) {
        uiBinding.ivPitchAngle.rotation = -value
        var textValue = NumberParseUtil.keepZeroDigits(value)
        if (textValue == "0" || textValue == "-0" || textValue == "+0") {
            textValue = "0"
        }
        uiBinding.tvPitchAngle.text = "${textValue}°"
    }

    private fun showUnconnected() {
        this.isEnabled = false
        uiBinding.ivPitchAngle.rotation = 0f
        uiBinding.ivPitchAngle.setImageResource(R.drawable.mission_ic_gimbal_pitch_angel_dark)
        uiBinding.ivGimbalSacle.setImageResource(R.drawable.common_ic_gimbal_scale_dark)
        uiBinding.tvPitchAngle.text = context.getString(R.string.common_text_no_value)
    }

    private fun showConnected() {
        this.isEnabled = true
        uiBinding.ivPitchAngle.setImageResource(R.drawable.mission_ic_gimbal_pitch_angel)
        uiBinding.ivGimbalSacle.setImageResource(R.drawable.common_ic_gimbal_scale)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isInEditMode) {
            widgetModel.setup()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (!isInEditMode) {
            widgetModel.cleanup()
        }
    }
}