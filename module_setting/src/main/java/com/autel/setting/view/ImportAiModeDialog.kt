package com.autel.setting.view

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import com.autel.common.widget.dialog.BaseAutelDialog
import com.autel.setting.databinding.DialogImportAiModeBinding

/**
 * @author 
 * @date 2023/8/9
 * 模型导入弹框
 */
@SuppressLint("SetTextI18n")
class ImportAiModeDialog(context: Context) : BaseAutelDialog(context) {

    private val binding = DialogImportAiModeBinding.inflate(LayoutInflater.from(context))


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        window?.decorView?.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        setCanceledOnTouchOutside(true)
        binding.tvCancel.setOnClickListener { dismiss() }
    }

    /**
     * 设置内容
     */
    fun setContentText(content: String) {
        binding.tvContent.text = content
    }

    /**
     * 替换
     */
    fun setOnReplaceListener(listener: View.OnClickListener) {
        binding.tvReplace.setOnClickListener(listener)
    }

    /**
     * 跳过
     */
    fun setOnJumpToImportListener(listener: View.OnClickListener) {
        binding.tvJumpToImport.setOnClickListener(listener)
    }
}
