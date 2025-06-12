package com.autel.widget.widget.cancellanding

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.core.view.isVisible
import com.autel.common.base.widget.ConstraintLayoutWidget
import com.autel.common.lifecycle.LiveDataBus
import com.autel.common.lifecycle.event.CancelLandingEvent
import com.autel.common.utils.GoogleTextToSpeechManager
import com.autel.common.utils.UIUtils.getString
import com.autel.common.widget.toast.AutelToast
import com.autel.widget.R
import com.autel.widget.databinding.LayoutWidgetCancelLandingBinding


class CancelLandingWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) :
    ConstraintLayoutWidget(context, attrs, defStyleAttr, defStyleRes) {
    private val uiBinding: LayoutWidgetCancelLandingBinding

    private val widgetModel: CancelLandingWidgetVM by lazy {
        CancelLandingWidgetVM()
    }

    init {
        uiBinding = LayoutWidgetCancelLandingBinding.inflate(LayoutInflater.from(context), this)
        uiBinding.flLoading.setOnClickListener {
            val state = widgetModel.cancelLandingData.replayCache.firstOrNull()
            state?.returnDrones?.let { droneDevices ->
                widgetModel.cancelReturn(droneDevices) {
                    GoogleTextToSpeechManager.instance()
                        .speak(getString(R.string.common_text_return_flight_canceled), true)
                    AutelToast.normalToast(
                        context,
                        getString(R.string.common_text_return_flight_canceled)
                    )
                }
            }
        }

        uiBinding.flLanding.setOnClickListener {
            val state = widgetModel.cancelLandingData.replayCache.firstOrNull()
            state?.landDrones?.let { droneDevices ->
                widgetModel.cancelLanding(droneDevices) {
                    AutelToast.normalToast(context, getString(R.string.common_text_land_canceled))
                }
            }
        }
    }

    override fun reactToModelChanges() {
        super.reactToModelChanges()
        widgetModel.cancelLandingData.subscribe {
            uiBinding.flLoading.isVisible = it.cancelReturn
            uiBinding.flLanding.isVisible = it.cancelDecline
            LiveDataBus.of(CancelLandingEvent::class.java).onCancelLandingVisiablity()
                .post(Pair(first = it.cancelReturn, second = it.cancelDecline))
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