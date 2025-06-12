package com.autel.setting.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.autel.common.databinding.DialogInputPwdBinding
import com.autel.common.listener.CheckPwdListener
import com.autel.common.utils.EncryptUtil
import com.autel.common.widget.dialog.BaseAutelDialog
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.log.AutelLog

/**
 * @author 
 * @date 2024/12/30
 * 校验密码弹框
 */
class CheckPwdDialog(context: Context) : BaseAutelDialog(context) {
    private val TAG = "CheckPwdDialog"
    private val binding = DialogInputPwdBinding.inflate(LayoutInflater.from(context))
    private var listener: CheckPwdListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        getWindow()?.decorView?.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        setCanceledOnTouchOutside(false)
        binding.tvCancel.setOnClickListener {
            dismiss()
        }

        binding.tvConfirm.setOnClickListener {
            AutelLog.i(TAG, "tvConfirm -> OnClick")
            DeviceManager.getDeviceManager().getLocalRemoteDevice().getRemoteSn { sn ->
                val pwd = binding.etPwd.text.toString().trim()
                AutelLog.i(TAG, "tvConfirm -> sn=$sn pwd=$pwd")
                val result = EncryptUtil.checkPassword(sn, pwd)
                binding.tvError.isVisible = !result
                if (result) {
                    listener?.onSuccess()
                    AutelLog.i(TAG, "tvConfirm -> sn=$sn pwd=$pwd")
                    dismiss()
                }
            }
        }
        binding.tbEye.setOnCheckedChangeListener { _, b ->
            if (b) {
                binding.etPwd.transformationMethod = HideReturnsTransformationMethod.getInstance()
            } else {
                binding.etPwd.transformationMethod = PasswordTransformationMethod.getInstance()
            }
            binding.etPwd.setSelection(binding.etPwd.text.toString().length)
        }

        binding.etPwd.addTextChangedListener(afterTextChanged = {
            binding.tbEye.isVisible = binding.etPwd.text.isNotEmpty()
            //如果密码删除完毕，则隐藏错误提示
            if (binding.etPwd.text.toString().isEmpty()) {
                binding.tvError.isVisible = false
            }
        })



        binding.tbEye.isChecked = false
        binding.etPwd.transformationMethod = PasswordTransformationMethod.getInstance()
    }

    fun setOnConfirmListener(listener: CheckPwdListener) {
        this.listener = listener
    }

}