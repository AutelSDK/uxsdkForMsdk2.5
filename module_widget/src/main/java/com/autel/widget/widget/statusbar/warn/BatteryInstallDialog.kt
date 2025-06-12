package com.autel.widget.widget.statusbar.warn

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import com.autel.common.widget.dialog.BaseAutelDialog
import com.autel.widget.R
import com.autel.widget.databinding.DialogBatteryInstallBinding
import com.bumptech.glide.Glide

/**
 * Created by  2025/1/11
 * H飞机电池安装弹窗
 */
class BatteryInstallDialog(context: Context) : BaseAutelDialog(context) {
    private val binding = DialogBatteryInstallBinding.inflate(LayoutInflater.from(context))
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        Glide.with(context)
            .asGif()
            .load(R.raw.common_raw_h_battery_install)
            .into(binding.ivWarnTip2)

        binding.tvWarnTips1.text = "1、"+context.getString(R.string.common_text_h_battery_check_tips1)
        binding.tvWarnTips2.text = "2、"+context.getString(R.string.common_text_h_battery_check_tips2)

        binding.ivClose.setOnClickListener {
            dismiss()
        }
    }
}