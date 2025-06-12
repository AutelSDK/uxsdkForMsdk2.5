package com.autel.setting.dialog

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import com.autel.common.widget.dialog.BaseAutelDialog
import com.autel.setting.databinding.SettingDialogClearDataProgressBinding

/**
 * Created by  2024/1/8
 * 正在清除数据
 */
class ClearDataProgressDialog(context: Context) : BaseAutelDialog(context) {

    private val binding: SettingDialogClearDataProgressBinding = SettingDialogClearDataProgressBinding.inflate(LayoutInflater.from(context))


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
    }
}