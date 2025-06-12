package com.autel.widget.widget.linkagezoom

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.core.view.isVisible
import com.autel.common.base.widget.ConstraintLayoutWidget
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.widget.R
import com.autel.widget.databinding.WidgetLayoutLinkageZoomBinding

class LinkageZoomWidget @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0) :
    ConstraintLayoutWidget(context, attrs, defStyleAttr, defStyleRes) {
    private val uiBinding: WidgetLayoutLinkageZoomBinding
    private val widgetModel: LinkageZoomVM by lazy { LinkageZoomVM() }

    init {
        uiBinding = WidgetLayoutLinkageZoomBinding.inflate(LayoutInflater.from(context), this)
        setBackgroundResource(R.drawable.mission_selector_icon_bg_all)
        setOnClickListener {
            setLinkageZoomState(!uiBinding.ivLinkageZoom.isSelected)
        }
    }

    /**
     * 设置联动变焦
     * */
    private fun setLinkageZoomState(selected: Boolean) {
        widgetModel.setLinkageZoom(selected)
    }


    override fun reactToModelChanges() {
        super.reactToModelChanges()
        widgetModel.linkState.subscribe {
            if (it.isConnected) {
                this.isSelected = it.inLinkage
                this.isVisible = it.isShow
            } else {
                this.isVisible = false
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
        super.onDetachedFromWindow()
        if (!isInEditMode) {
            widgetModel.cleanup()
        }
    }

    fun updateLensInfo(drone: IAutelDroneDevice?, isShow: Boolean) {
        val gimbalTypeEnum = drone?.getGimbalDeviceType()
        widgetModel.updateLensInfo(drone, gimbalTypeEnum, isShow)
    }

}