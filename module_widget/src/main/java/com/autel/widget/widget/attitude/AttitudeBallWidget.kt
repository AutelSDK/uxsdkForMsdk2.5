package com.autel.widget.widget.attitude

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import com.autel.common.base.widget.ConstraintLayoutWidget
import com.autel.common.widget.toast.AutelToast
import com.autel.map.bean.AutelLatLng
import com.autel.widget.R
import com.autel.widget.databinding.WidgetAttitubeBallBinding
import com.autel.widget.widget.attitude.vm.AttitudeWM
import com.autel.widget.widget.attitude.vm.RealTimeAttitudeWM

/**
 * 姿态球组件
 */
class AttitudeBallWidget @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0) :
    ConstraintLayoutWidget(context, attrs, defStyleAttr, defStyleRes) {

    private val uiBinding: WidgetAttitubeBallBinding

    val widgetModel: AttitudeWM

    private var ignoreRotation = false

    init {
        uiBinding = WidgetAttitubeBallBinding.inflate(LayoutInflater.from(context), this, true)
        val attribute = context.obtainStyledAttributes(attrs, R.styleable.AttitudeBallWidget)
        var clz = attribute.getString(R.styleable.AttitudeBallWidget_widget_model)
        ignoreRotation = attribute.getBoolean(R.styleable.AttitudeBallWidget_ignore_rotation, false)
        if (clz.isNullOrEmpty()) {
            clz = RealTimeAttitudeWM::class.java.name
        }
        val clazz = Class.forName(clz)
        widgetModel = clazz.newInstance() as AttitudeWM
        attribute.recycle()

        uiBinding.ivBallVisual.setOnClickListener {
            val inRemote = uiBinding.ivBallVisual.isSelected
            if (inRemote) {
                AutelToast.normalToast(context, R.string.common_text_switch_drone_perspective)
            } else {
                AutelToast.normalToast(context, R.string.common_text_switch_remote_perspective)
            }
            uiBinding.ivBallVisual.isSelected = !inRemote
            uiBinding.ballView.updateInDrone(inRemote)
        }
    }

    override fun reactToModelChanges() {
        widgetModel.droneAttitudeData.subscribe {
            uiBinding.ballView.updateData(
                it.connected,
                it.droneYaw,
                it.gimbalYaw,
                it.remoteLatLng,
                it.homeLatLng,
                it.droneLatLng,
                it.seriousLowBatteryWarningValue,
                it.lowBatteryWarningValue,
                it.droneBatteryPercent,
                it.horizontalSpeed,
                it.compassDegree
            )
            uiBinding.aslView.updateData(it.connected, it.altitude, it.altitudeMSL, it.distance, it.horizontalSpeedStr, it.verticalSpeed)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        widgetModel.setup()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        widgetModel.cleanup()
    }

}