package com.autel.ux.widget.sdcard

import android.content.Context
import android.util.AttributeSet
import androidx.core.view.isVisible
import com.autel.drone.sdk.vmodelx.manager.data.ControlMode
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.camera.enums.CardStatusEnum
import com.autel.ux.core.base.AutelSDKModel
import com.autel.ux.core.base.widget.ConstraintLayoutWidget
import com.autel.widget.databinding.UxWidgetSdcardBinding
import kotlinx.coroutines.flow.combine

/**
 * 飞机SD卡状态
 */
class SDCardWidget @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
) : ConstraintLayoutWidget(context, attrs) {

    private lateinit var binding: UxWidgetSdcardBinding

    private val widgetModel: SDCardModel by lazy {
        SDCardModel(AutelSDKModel.getInstance())
    }

    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        binding = UxWidgetSdcardBinding.inflate(layoutInflater, this)
    }

    override fun reactToModelChanges() {
        combine(widgetModel.cardStatus, widgetModel.productConnected, widgetModel.controlMode) { status, connect, controlMode ->
            logi("status = $status, connect = $connect, controlMode = $controlMode")
            !CardStatusEnum.isEnable(status) && connect && controlMode.controlMode == ControlMode.SINGLE
        }.collectInWidget {
            logd("sd card visible = $it")
            isVisible = it
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