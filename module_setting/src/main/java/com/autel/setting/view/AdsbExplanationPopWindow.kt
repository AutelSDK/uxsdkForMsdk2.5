package com.autel.setting.view

import android.content.Context
import android.view.LayoutInflater
import com.autel.common.widget.BasePopWindow
import com.autel.setting.databinding.FragmentAdsbExplanationBinding

/**
 * @date 2022/9/7.
 * @author maowei
 * @description 告警信息弹框
 */
class AdsbExplanationPopWindow(context: Context) : BasePopWindow(context) {

    private val binding = FragmentAdsbExplanationBinding.inflate(LayoutInflater.from(context))

    init {
        contentView = binding.root
    }

    fun  getTriangleView() = binding.triangle

    fun getContentBg() = binding.clContentBg

}
