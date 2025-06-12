package com.autel.setting.view

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import com.autel.common.manager.SameResourceHelper
import com.autel.common.widget.dialog.BaseAutelDialog
import com.autel.setting.databinding.SettingDialogDevicePairTipBinding

/**
 * Created by  2023/3/4
 */
class DevicePairTipDialog(context: Context) : BaseAutelDialog(context) {
    private var uiBinding = SettingDialogDevicePairTipBinding.inflate(LayoutInflater.from(context))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(uiBinding.root)
        setCanceledOnTouchOutside(true)
        uiBinding.failLogo.setBackgroundResource(SameResourceHelper.getPairResources())
    }

    fun setCloseListener(listener: () -> Unit) {
        uiBinding.failClose.setOnClickListener {
            listener.invoke()
        }
    }
}