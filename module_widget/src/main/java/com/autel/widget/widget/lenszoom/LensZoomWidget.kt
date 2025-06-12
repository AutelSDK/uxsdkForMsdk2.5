package com.autel.widget.widget.lenszoom

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.PopupWindow
import com.autel.common.base.widget.ConstraintLayoutWidget
import com.autel.common.extension.getLensTypeName
import com.autel.common.utils.NumberParseUtil
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.module.camera.bean.GimbalTypeEnum
import com.autel.drone.sdk.vmodelx.module.camera.bean.LensTypeEnum
import com.autel.widget.R
import com.autel.widget.databinding.WidgetLayoutLensZoomBinding
import com.autel.common.model.lens.ILens
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch

/**
 * Created by  2023/6/6
 *  镜头变焦组件
 */
class LensZoomWidget @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0) :
    ConstraintLayoutWidget(context, attrs, defStyleAttr, defStyleRes), ILens {
    private val uiBinding: WidgetLayoutLensZoomBinding

    private val widgetModel: LensZoomVM by lazy {
        LensZoomVM()
    }

    private var lensZoomPwShow: ILensZoomPwShow? = null

    private var virtualPopupWindow: VirtualZoomSettingPopupWindow? = null

    init {
        uiBinding = WidgetLayoutLensZoomBinding.inflate(LayoutInflater.from(context), this)
        setBackgroundResource(R.drawable.mission_selector_icon_bg_all)
        setOnClickListener {
            widgetModel.fixedFrequencyRefresh()
            virtualPopupWindow?.let {
                lensZoomPwShow?.showPopupWindow(it)
            }
        }
    }

    fun setLensZoomPwShow(lensZoomPwShow: ILensZoomPwShow) {
        this.lensZoomPwShow = lensZoomPwShow
    }

    fun getPpWindow(): PopupWindow? {
        return virtualPopupWindow
    }

    override fun reactToModelChanges() {
        super.reactToModelChanges()
        widgetModel.lensState.subscribe {
            if (it.isConnected) {
                virtualPopupWindow?.updateZoomScale(it.zoomValue, it.range)
                if (it.zoomValue == 0) {
                    uiBinding.tvZoomValue.text = context.getString(R.string.common_text_no_value)
                } else {
                    val strBuilder = StringBuilder()
                        .append(NumberParseUtil.formatFloat(it.zoomValue / 100f, 1).toString())
                        .append("X").toString()
                    uiBinding.tvZoomValue.text = strBuilder
                }
            } else {
                uiBinding.tvZoomValue.text = context.getString(R.string.common_text_no_value)
            }
        }
        widgetModel.pbDismissState.subscribe {
            getPpWindow()?.dismiss()
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

    override fun getDrone(): IAutelDroneDevice? {
        return widgetModel.getDrone()
    }

    override fun getGimbal(): GimbalTypeEnum? {
        return widgetModel.getGimbal()
    }

    override fun getLensTypeEnum(): LensTypeEnum? {
        return widgetModel.getLensTypeEnum()
    }

    override fun updateLensInfo(drone: IAutelDroneDevice?, gimbalTypeEnum: GimbalTypeEnum?, lensTypeEnum: LensTypeEnum?) {
        if (lensTypeEnum != null) {
            uiBinding.tvZoomTag.text = lensTypeEnum.getLensTypeName(context)
        }

        if (drone == null || gimbalTypeEnum == null || lensTypeEnum == null) {
            return
        }

        if (virtualPopupWindow == null) {
            virtualPopupWindow = VirtualZoomSettingPopupWindow(context)
        }
        virtualPopupWindow?.setZoomListener(object : IZoomChangeListener {
            override fun zoomChange(zoomMulti100: Int) {
                scope?.launch(CoroutineExceptionHandler { _, throwable ->

                }) {
                    widgetModel.setZoomFactor(zoomMulti100)
                }
            }

            override fun quickZoom(zoomMulti100: Int) {
                scope?.launch(CoroutineExceptionHandler { _, throwable ->

                }) {
                    widgetModel.setQuickZoom(zoomMulti100)
                }
            }

            override fun zoomDismiss() {
                widgetModel.cleanLensStateCache()
            }
        })
        widgetModel.updateLensInfo(drone, gimbalTypeEnum, lensTypeEnum)
    }

}