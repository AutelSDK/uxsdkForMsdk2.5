package com.autel.widget.widget.statusbar.warn

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import com.autel.common.widget.dialog.BaseAutelDialog
import com.autel.widget.R
import com.autel.widget.databinding.DialogArmUnflodBinding
import com.bumptech.glide.Glide

/**
 * Created by  2025/1/11
 * H 飞机 机臂展开弹窗
 */
class HArmUnfoldDialog(context: Context) : BaseAutelDialog(context) {
    private val binding = DialogArmUnflodBinding.inflate(LayoutInflater.from(context))
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        Glide.with(context)
            .asGif()
            .load(R.raw.common_raw_h_arm_unflod)
            .into(binding.ivWarnTip)

        binding.ivClose.setOnClickListener {
            dismiss()
        }

    }
}