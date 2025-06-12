package com.autel.widget.function.view

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.GridLayoutManager
import com.autel.common.delegate.function.FunctionViewType
import com.autel.ui.decoration.GridItemDecoration
import com.autel.widget.R
import com.autel.widget.databinding.WidgetLayoutFunctionFloatWindowBinding
import com.autel.widget.function.binder.EmptyItemViewBinder
import com.autel.widget.function.binder.FunctionItemViewBinder
import com.autel.widget.function.inter.IFunctionManager
import com.autel.widget.function.model.EmptyFunctionModel
import com.autel.widget.function.model.SwitchFunctionModel


class FunctionFloatWindowView @JvmOverloads constructor(context: Context, attr: AttributeSet? = null) : BaseFunctionBarView(context, attr) {

    private val binding: WidgetLayoutFunctionFloatWindowBinding =
        WidgetLayoutFunctionFloatWindowBinding.inflate(LayoutInflater.from(context), this)

    private var clickDismiss = false

    private var dismissListener: (() -> Unit)? = null

    private var fvAreaSize = context.resources.getDimensionPixelSize(R.dimen.common_320dp)

    init {
        multiTypeAdapter.register(SwitchFunctionModel::class.java, FunctionItemViewBinder(FunctionViewType.FloatBall))
        multiTypeAdapter.register(EmptyFunctionModel::class.java, EmptyItemViewBinder(FunctionViewType.FloatBall) {
            manager?.openFunctionPanelAndEnterEditMode(FunctionViewType.FloatBall, it)
        })

        binding.fvArea.updateLayoutParams<FrameLayout.LayoutParams> {
            width = fvAreaSize
            height = fvAreaSize
        }

        val itemDecoration = GridItemDecoration.Builder(context)
            .verColor(R.color.common_color_transparent)
            .horColor(R.color.common_color_transparent)
            .verSize(resources.getDimensionPixelSize(R.dimen.common_5dp))
            .horSize(resources.getDimensionPixelSize(R.dimen.common_5dp))
            .margin(0, 0)
            .isExistHead(false)
            .showHeadDivider(false)
            .showLastDivider(false)
            .build()
        binding.fvArea.addItemDecoration(itemDecoration)

        binding.fvArea.adapter = multiTypeAdapter
        binding.fvArea.itemAnimator = null
    }


    override fun attachFunctionManager(manager: IFunctionManager) {
        super.attachFunctionManager(manager)
        val touchHelper = FunctionItemTouchHelper(multiTypeAdapter, true)
        touchHelper.bindView(binding.fvArea, manager)
        val targetList = manager.getFunctionBarList()
        binding.fvArea.layoutManager = GridLayoutManager(context, 3)
        multiTypeAdapter.items = targetList
        manager.bindBarInFloatStyleOperate(this)
    }

    override fun resetFunction() {
        val targetList = manager?.getFunctionBarList() ?: return
        multiTypeAdapter.items = targetList
        multiTypeAdapter.notifyDataSetChanged()
    }


    fun show(clickButtonLocation: IntArray) {
        val clickButtonLeft = clickButtonLocation[0]
        val clickButtonTop = clickButtonLocation[1]

        // 检查另一个控件是否超出屏幕范围
        val parentWidth = (parent as View).width + 0f
        val parentHeight = (parent as View).height + 0f
        val overlayViewWidth = fvAreaSize
        val overlayViewHeight = fvAreaSize

        val pivotX: Float

        val pivotY: Float


        if (clickButtonLeft + overlayViewWidth > parentWidth) {
            binding.fvArea.x = parentWidth - overlayViewWidth
            pivotX = clickButtonLeft + overlayViewWidth - parentWidth
        } else {
            binding.fvArea.x = clickButtonLeft + 0f
            pivotX = 0f
        }
        if (clickButtonTop + overlayViewHeight > parentHeight) {
            binding.fvArea.y = parentHeight - overlayViewHeight
            pivotY = clickButtonTop + overlayViewHeight - parentHeight
        } else {
            binding.fvArea.y = clickButtonTop + 0f
            pivotY = 0f
        }

        binding.fvArea.post {
            this.visibility = View.VISIBLE
            animateView(binding.fvArea, pivotX, pivotY)
        }
    }

    fun showCenter() {
        this.visibility = View.VISIBLE
        val parentWidth = (parent as View).width + 0f
        val parentHeight = (parent as View).height + 0f

        if (parentWidth - binding.fvArea.x - binding.fvArea.width - resources.getDimensionPixelSize(R.dimen.common_variety_function_panel_width) < 10) {
            val centerX =
                (parentWidth - resources.getDimensionPixelSize(R.dimen.common_variety_function_panel_width) - fvAreaSize) - resources.getDimensionPixelSize(
                    R.dimen.common_20dp
                )
            val centerY = (parentHeight - fvAreaSize) / 2

            binding.fvArea.animate().x(centerX).y(centerY).setDuration(200).start()
        }
    }

    private fun animateView(view: View, pivotX: Float, pivotY: Float) {
        // 创建一个放大动画
        val scaleXAnimator = ObjectAnimator.ofFloat(view, "scaleX", 0f, 1f)
        val scaleYAnimator = ObjectAnimator.ofFloat(view, "scaleY", 0f, 1f)

        // 设置动画的持续时间
        scaleXAnimator.setDuration(200)
        scaleYAnimator.setDuration(200)

        view.pivotX = pivotX
        view.pivotY = pivotY

        // 创建一个动画集合，将两个动画添加进去
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleXAnimator, scaleYAnimator)

        // 启动动画
        animatorSet.start()
    }

    fun setClickDismiss(clickDismiss: Boolean) {
        this.clickDismiss = clickDismiss
    }

    fun setDismissListener(listener: (() -> Unit)) {
        this.dismissListener = listener
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (clickDismiss && event.action == MotionEvent.ACTION_DOWN) {
            return true
        }
        if (clickDismiss && event.action == MotionEvent.ACTION_UP) {
            visibility = View.GONE
            dismissListener?.invoke()
            return true
        }
        return super.onTouchEvent(event)
    }
}