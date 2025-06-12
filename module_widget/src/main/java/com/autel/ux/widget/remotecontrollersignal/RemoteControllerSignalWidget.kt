package com.autel.ux.widget.remotecontrollersignal

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.core.view.isVisible
import com.autel.drone.sdk.vmodelx.manager.data.ControlMode
import com.autel.log.AutelLog
import com.autel.ux.core.base.AutelSDKModel
import com.autel.ux.core.base.widget.ConstraintLayoutWidget
import com.autel.widget.databinding.WidgetRemoteControllerSignalBinding

class RemoteControllerSignalWidget @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
) : ConstraintLayoutWidget(context, attrs) {

    private val widgetModel: RemoteControllerSignalWidgetModel by lazy {
        RemoteControllerSignalWidgetModel(AutelSDKModel.getInstance())
    }

    private lateinit var binding: WidgetRemoteControllerSignalBinding

    /* private val popover: RemoteSignalPopupWindow by lazy {
         RemoteSignalPopupWindow(context)
     }*/

    init {
        /*setOnClickListener {
            if (widgetModel.productConnected.value != true) return@setOnClickListener
            popover.setData(RemoteSignalLevelEnum.parseValue(widgetModel.currentSignalQuality.value))
            popover.showCenterDown(it)
        }*/
    }

    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        binding = WidgetRemoteControllerSignalBinding.inflate(LayoutInflater.from(context), this)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        widgetModel.setup()
    }

    override fun onDetachedFromWindow() {
        widgetModel.cleanup()
        super.onDetachedFromWindow()
    }

    override fun reactToModelChanges() {
        widgetModel.currentSignalQuality.collectInWidget {
            updateView(it)
        }
        widgetModel.controlMode.collectInWidget {
            isVisible = it.controlMode == ControlMode.SINGLE
        }
    }

    private fun updateView(level: Int) {
        AutelLog.d(TAG, "remote controller signal level = $level")
        binding.ivRemoteControlSignal.setImageLevel(level)
    }

}