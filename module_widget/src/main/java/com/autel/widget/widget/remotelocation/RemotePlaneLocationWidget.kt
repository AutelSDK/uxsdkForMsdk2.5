package com.autel.widget.widget.remotelocation

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import com.autel.common.base.widget.ConstraintLayoutWidget
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.widget.R
import com.autel.widget.databinding.WidgetRemotePlaneLocationBinding

class RemotePlaneLocationWidget @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    ConstraintLayoutWidget(context, attrs, defStyleAttr) {

    private val binding: WidgetRemotePlaneLocationBinding = WidgetRemotePlaneLocationBinding.inflate(LayoutInflater.from(context), this)

    private val widgetModel: RemotePlaneLocationVM = RemotePlaneLocationVM()


    override fun reactToModelChanges() {
        super.reactToModelChanges()
        widgetModel.remotePlaneLocationFlow.subscribe { model ->
            if (model.droneIsValid) {
                binding.otvPlaneLocation.text =
                    context.getString(R.string.common_text_aircraft) + " ${model.droneLocationLat} ${model.droneLocationLng}"
            } else {
                binding.otvPlaneLocation.text =
                    context.getString(R.string.common_text_aircraft) + " " + context.getString(R.string.common_text_no_value)
            }
            if (model.remoteIsValid) {
                binding.otvRemoteLocation.text = context.getString(R.string.common_text_go_home_controller) + " ${model.remoteLat} ${model.remoteLng}"
            } else {
                binding.otvRemoteLocation.text =
                    context.getString(R.string.common_text_go_home_controller) + " " + context.getString(R.string.common_text_no_value)
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

    fun updateDrone(drone: IAutelDroneDevice?) {
        widgetModel.updateDevice(drone)
    }


}