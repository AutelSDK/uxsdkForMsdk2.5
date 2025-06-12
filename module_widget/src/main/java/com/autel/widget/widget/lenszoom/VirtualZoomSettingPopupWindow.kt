package com.autel.widget.widget.lenszoom

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.autel.common.widget.BasePopWindow
import com.autel.drone.sdk.vmodelx.module.camera.bean.RangeStepValue
import com.autel.log.AutelLog
import com.autel.widget.R
import com.autel.widget.databinding.MissionPopwindowVirtualZoomSettingBinding
import kotlin.math.roundToInt


/**
 * Created by  2022/12/9
 * 虚拟变焦操作浮窗
 *
 * 快速变焦最合理的方式是：相机给出变焦范围到产品。由产品输出快速变焦值，最好放在能力集中，而不是让App计算！！！
 * 之前的产品、设计、相机都不愿处理这个。
 */
class VirtualZoomSettingPopupWindow(context: Context) : BasePopWindow(context), IZoomSettingPopupWindow {

    private val binding = MissionPopwindowVirtualZoomSettingBinding.inflate(LayoutInflater.from(context))

    companion object {
        private const val TIGHT_SCALE_MAX = 10 * 100 //紧密刻度最大值，低于该范围每个刻度表示0.1,高于该刻度刻度的倍率不一样
        private const val QUICK_5 = 5 * 100
        private const val QUICK_3 = 3 * 100
        private const val QUICK_ZOOM_SHOW = 160 * 100 //快速变焦显示，需要最大变焦值超过该范围
    }

    private var quickMax: Int? = null
    private var quickMaxDiv2: Int? = null
    private var quickMaxDiv4: Int? = null
    private var quickMaxDiv8: Int? = null
    private var quickMaxDiv16: Int? = null

    private var listener: IZoomChangeListener? = null
    private var rangeStep: RangeStepValue? = null
    private var zoomMulti100: Int? = null


    init {
        contentView = binding.root
        width = context.resources.getDimensionPixelSize(R.dimen.common_125dp)
        height = context.resources.getDimensionPixelSize(R.dimen.common_345dp)
        binding.tvQuickMax.setOnClickListener {
            quickMax?.let {
                listener?.quickZoom(it)
                binding.scale.setCurrentValue(it)
            }
        }
        binding.tvQuickMax2.setOnClickListener {
            quickMaxDiv2?.let {
                listener?.quickZoom(it)
                binding.scale.setCurrentValue(it)
            }
        }
        binding.tvQuickMax4.setOnClickListener {
            quickMaxDiv4?.let {
                listener?.quickZoom(it)
                binding.scale.setCurrentValue(it)
            }
        }
        binding.tvQuickMax8.setOnClickListener {
            quickMaxDiv8?.let {
                listener?.quickZoom(it)
                binding.scale.setCurrentValue(it)
            }
        }
        binding.tvQuickMax16.setOnClickListener {
            quickMaxDiv16?.let {
                listener?.quickZoom(it)
                binding.scale.setCurrentValue(it)
            }
        }
        binding.tvQuick5.setOnClickListener {
            listener?.quickZoom(QUICK_5)
            binding.scale.setCurrentValue(QUICK_5)
        }
        binding.tvQuick3.setOnClickListener {
            listener?.quickZoom(QUICK_3)
            binding.scale.setCurrentValue(QUICK_3)
        }
        binding.scale.setOnScaleListener(object : VirtualZoomScaleView.OnScaleListener {
            override fun onValueChangeListener(value: Int) {
                listener?.zoomChange(value)
            }
        })
    }

    /**
     * 更新刻度
     */
    private fun updateScale(range: RangeStepValue?) {
        AutelLog.i("VirtualZoomSettingPopupWindow", "range = $range")
        if (range == null) {
            return
        }
        //10刻度以下是0.1、  10-20是1、20-40是2、40-80是4、80-160是8
        var stepTimes = 10f // 10刻度以下是0.1的规则 乘以100倍
        val indexMin = range.min.times(100)
        val indexMax = range.max.times(100)
        val scaleList: ArrayList<VirtualZoomScaleView.ScaleItem> = ArrayList()
        if (indexMax >= QUICK_ZOOM_SHOW) {
            binding.llQuick.visibility = View.VISIBLE
            var index = indexMin + 0f
            val lengthInterval = 10 //每10个画一个长线
            var itemCount = 10
            val maxDiv16 = (indexMax / 16f).div(100).roundToInt() * 100
            val maxDiv8 = (indexMax / 8f).div(100).roundToInt() * 100
            val maxDiv4 = (indexMax / 4f).div(100).roundToInt() * 100
            val maxDiv2 = (indexMax / 2f).div(100).roundToInt() * 100

            quickMaxDiv16 = maxDiv16
            quickMaxDiv8 = maxDiv8
            quickMaxDiv4 = maxDiv4
            quickMaxDiv2 = maxDiv2
            quickMax = indexMax

            binding.tvQuickMax16.text = "${(maxDiv16.div(100))}"
            binding.tvQuickMax8.text = "${(maxDiv8.div(100))}"
            binding.tvQuickMax4.text = "${(maxDiv4.div(100))}"
            binding.tvQuickMax2.text = "${(maxDiv2.div(100))}"
            binding.tvQuickMax.text = "${(indexMax.div(100))}"

            while (index <= indexMax) {
                if (index < TIGHT_SCALE_MAX) {
                    stepTimes = 10f
                } else if (index < maxDiv16) {
                    stepTimes = (maxDiv16 - TIGHT_SCALE_MAX) / 10f
                } else if (index < maxDiv8) {
                    stepTimes = (maxDiv8 - maxDiv16) / 10f
                } else if (index < maxDiv4) {
                    stepTimes = (maxDiv4 - maxDiv8) / 10f
                } else if (index < maxDiv2) {
                    stepTimes = (maxDiv2 - maxDiv4) / 10f
                } else if (index <= indexMax) {
                    stepTimes = (indexMax - maxDiv2) / 10f
                }
                val indexScale = index.roundToInt()
                scaleList.add(
                    VirtualZoomScaleView.ScaleItem(
                        indexScale, "${(indexScale / 100)}x", itemCount == lengthInterval, itemCount == lengthInterval
                    )
                )
                if (itemCount == lengthInterval) {
                    itemCount = 1
                } else {
                    itemCount++
                }
                index += stepTimes
            }
        } else {
            binding.llQuick.visibility = View.GONE
            var index = indexMin + 0f
            val lengthInterval = 10 //每10个画一个长线
            var itemCount = 10
            while (index <= indexMax) {
                if (index < 4 * 100) {
                    stepTimes = 10f
                } else {
                    stepTimes = 20f
                }
                val indexScale = index.roundToInt()
                scaleList.add(
                    VirtualZoomScaleView.ScaleItem(
                        indexScale, "${(index / 100)}x", itemCount == lengthInterval, itemCount == lengthInterval
                    )
                )
                if (itemCount == lengthInterval) {
                    itemCount = 1
                } else {
                    itemCount++
                }
                index += stepTimes
            }
        }
        binding.scale.setScale(scaleList)
    }


    private fun resetAllQuick() {
        binding.tvQuickMax.isSelected = false
        binding.tvQuickMax2.isSelected = false
        binding.tvQuickMax4.isSelected = false
        binding.tvQuickMax8.isSelected = false
        binding.tvQuickMax16.isSelected = false
        binding.tvQuick5.isSelected = false
        binding.tvQuick3.isSelected = false
    }

    override fun setZoomListener(listener: IZoomChangeListener) {
        this.listener = listener
    }


    override fun dismiss() {
        super.dismiss()
        binding.scale.resetLastSelectScale()
        listener?.zoomDismiss()
    }

    override fun updateZoomScale(zoomMulti100: Int, range: RangeStepValue?) {
        if (rangeStep != range) {
            this.rangeStep = range
            updateScale(rangeStep)
            updateZoomScale(zoomMulti100)
            return
        }
        updateZoomScale(zoomMulti100)
    }

    override fun showAtLocation(parent: View, gravity: Int, x: Int, y: Int) {
        super.showAtLocation(parent, gravity, x, y)
        zoomMulti100?.let {
            binding.scale.setCurrentValue(it)
        }
    }

    private fun updateZoomScale(zoomMulti100: Int) {
        resetAllQuick()
        this.zoomMulti100 = zoomMulti100
        if (!isShowing) {
            binding.scale.setCurrentValue(zoomMulti100)
        }
        when (zoomMulti100) {
            quickMax -> {
                binding.tvQuickMax.isSelected = true
            }

            quickMaxDiv2 -> {
                binding.tvQuickMax2.isSelected = true
            }

            quickMaxDiv4 -> {
                binding.tvQuickMax4.isSelected = true
            }

            quickMaxDiv8 -> {
                binding.tvQuickMax8.isSelected = true
            }

            quickMaxDiv16 -> {
                binding.tvQuickMax16.isSelected = true
            }

            QUICK_5 -> {
                binding.tvQuick5.isSelected = true
            }

            QUICK_3 -> {
                binding.tvQuick3.isSelected = true
            }
        }
    }
}