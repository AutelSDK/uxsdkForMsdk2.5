package com.autel.widget.function.view

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.activity.ComponentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.autel.common.utils.AnimateUtil
import com.autel.common.utils.DeviceUtils
import com.autel.widget.R
import com.autel.widget.function.FunctionBarVM
import com.autel.common.delegate.function.FunctionBarState

/**
 * Created by  2023/5/23
 *  虚拟的状态栏、工具栏控件
 */
class VirtualFunctionBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val functionBarVm: FunctionBarVM = if (isInEditMode) {
        FunctionBarVM()
    } else {
        ViewModelProvider(context as ComponentActivity)[FunctionBarVM::class.java]
    }
    private val statusBarHeight =
        if (DeviceUtils.isMainRC()) context.resources.getDimensionPixelOffset(R.dimen.common_variety_status_bar_height) else 0
    private val functionBarHeight = context.resources.getDimensionPixelOffset(R.dimen.common_variety_function_bar_height)

    private var mHeightSize = functionBarHeight + statusBarHeight
    private var valueAnimator: ValueAnimator? = null

    private var observer = Observer<FunctionBarState> { it ->
        when (it) {
            FunctionBarState.Unfolded -> {
                mHeightSize = functionBarHeight + statusBarHeight
                requestLayout()
            }

            FunctionBarState.Folded -> {
                mHeightSize = statusBarHeight
                requestLayout()
            }

            FunctionBarState.Folding,
            FunctionBarState.Unfolding -> {
                val isFolding = it == FunctionBarState.Folding
                if (valueAnimator != null && valueAnimator?.isRunning == true) {
                    valueAnimator?.end()
                }
                valueAnimator = if (isFolding) ValueAnimator.ofFloat(0f, 1f) else ValueAnimator.ofFloat(1f, 0f)
                valueAnimator?.duration = AnimateUtil.ANIMATE_SWITCH_TIME
                valueAnimator?.addUpdateListener {
                    val value = it.animatedValue as Float
                    mHeightSize = ((functionBarHeight + statusBarHeight) - value * functionBarHeight).toInt()
                    requestLayout()
                }
                valueAnimator?.start()
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        functionBarVm.functionBarLD.observeForever(observer)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        valueAnimator?.end()
        functionBarVm.functionBarLD.removeObserver(observer)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(widthSize, mHeightSize)
    }

}