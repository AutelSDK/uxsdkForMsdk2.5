package com.autel.widget.function.view

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.activity.ComponentActivity
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.autel.common.delegate.function.FunctionType
import com.autel.common.delegate.function.FunctionViewType
import com.autel.common.utils.AnimateUtil
import com.autel.widget.R
import com.autel.widget.databinding.WidgetLayoutFunctionBarBinding
import com.autel.widget.function.FunctionBarVM
import com.autel.widget.function.binder.EmptyItemViewBinder
import com.autel.widget.function.binder.FunctionItemViewBinder
import com.autel.widget.function.inter.IFunctionManager
import com.autel.widget.function.model.EmptyFunctionModel
import com.autel.common.delegate.function.FunctionBarState
import com.autel.widget.function.model.SwitchFunctionModel


class FunctionBarView @JvmOverloads constructor(context: Context, attr: AttributeSet? = null) : BaseFunctionBarView(context, attr) {
    private val binding: WidgetLayoutFunctionBarBinding = WidgetLayoutFunctionBarBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        multiTypeAdapter.register(SwitchFunctionModel::class.java, FunctionItemViewBinder(FunctionViewType.Bar))
        multiTypeAdapter.register(EmptyFunctionModel::class.java, EmptyItemViewBinder(FunctionViewType.Bar))
    }

    private val functionBarVm: FunctionBarVM = if (isInEditMode) {
        FunctionBarVM()
    } else {
        ViewModelProvider(context as ComponentActivity)[FunctionBarVM::class.java]
    }

    private var maxCount = 12

    init {
        binding.fvArea.adapter = multiTypeAdapter
        binding.fvArea.itemAnimator = null

        binding.functionMore.setIconSizeAndFontSize(
            context.resources.getDimensionPixelSize(R.dimen.common_30dp),
            context.resources.getDimension(R.dimen.common_text_size_sp_13)
        )

        if (!isInEditMode) {
            functionBarVm.functionBarLD.observe(context as LifecycleOwner) {
                when (it) {
                    FunctionBarState.Unfolded -> {
                        //未折叠
                        binding.root.updateLayoutParams<MarginLayoutParams> {
                            topMargin = 0
                        }
                    }

                    FunctionBarState.Folded -> {
                        //折叠
                        binding.root.updateLayoutParams<MarginLayoutParams> {
                            topMargin = -context.resources.getDimensionPixelOffset(R.dimen.common_variety_function_bar_height)
                        }
                    }

                    FunctionBarState.Folding,
                    FunctionBarState.Unfolding,
                    -> {
                        val isFolding = it == FunctionBarState.Folding
                        val valueAnimator = if (isFolding) ValueAnimator.ofFloat(0f, 1f) else ValueAnimator.ofFloat(1f, 0f)
                        valueAnimator.duration = AnimateUtil.ANIMATE_SWITCH_TIME
                        valueAnimator.addUpdateListener {
                            val value = it.animatedValue as Float
                            binding.root.updateLayoutParams<MarginLayoutParams> {
                                topMargin = (value * (-context.resources.getDimensionPixelOffset(R.dimen.common_variety_function_bar_height))).toInt()
                            }
                        }
                        valueAnimator.addListener(object : Animator.AnimatorListener {
                            override fun onAnimationStart(animation: Animator) {

                            }

                            override fun onAnimationEnd(animation: Animator) {
                                if (isFolding) {
                                    functionBarVm.updateFunctionBarLD(FunctionBarState.Folded)
                                } else {
                                    functionBarVm.updateFunctionBarLD(FunctionBarState.Unfolded)
                                }

                            }

                            override fun onAnimationCancel(animation: Animator) {
                                if (isFolding) {
                                    functionBarVm.updateFunctionBarLD(FunctionBarState.Unfolded)
                                } else {
                                    functionBarVm.updateFunctionBarLD(FunctionBarState.Folded)
                                }
                            }

                            override fun onAnimationRepeat(animation: Animator) {

                            }
                        })
                        valueAnimator.start()
                    }
                }
            }
        }
    }

    fun setMaxCount(maxCount: Int) {
        this.maxCount = maxCount
    }

    override fun attachFunctionManager(manager: IFunctionManager) {
        super.attachFunctionManager(manager)
        val touchHelper = FunctionItemTouchHelper(multiTypeAdapter, false)
        touchHelper.bindView(binding.fvArea, manager)
        binding.functionMore.setFunctionItem(FunctionViewType.Bar, manager.getMoreFunctionModel())
        val targetList = manager.getFunctionBarList()
        binding.fvArea.layoutManager = GridLayoutManager(context, maxCount)
        multiTypeAdapter.items = targetList
        manager.bindBarInBarStyleOperate(this)
    }


    override fun refreshFunction(functionItem: SwitchFunctionModel) {
        super.refreshFunction(functionItem)
        if (functionItem.functionModel.functionType == FunctionType.FunctionMore) {
            binding.functionMore.setFunctionItem(FunctionViewType.Bar, functionItem)
        }
    }

    override fun resetFunction() {
        val targetList = manager?.getFunctionBarList() ?: return
        multiTypeAdapter.items = targetList
        multiTypeAdapter.notifyDataSetChanged()
    }

    override fun isShowMoreFunction(isShow: Boolean) {
        binding.functionMore.isVisible =isShow
    }
}