package com.autel.setting.dialog

import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.autel.common.R
import com.autel.common.databinding.CommonLayoutAiLoginDialogBinding
import com.autel.common.listener.AiLoginListener
import com.autel.common.manager.AiServiceManager
import com.autel.common.manager.AutelStorageManager
import com.autel.common.manager.StorageKey
import com.autel.common.widget.dialog.BaseAutelDialog
import com.autel.common.widget.dialog.CommonLoadingDialog
import com.autel.common.widget.toast.AutelToast
import com.autel.drone.sdk.vmodelx.manager.CloudServiceManager
import com.autel.log.AutelLog
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * @author 
 * @date 2023/6/21
 * AI开放平台登录弹框
 */
class AiLoginDialog(context: Context) : BaseAutelDialog(context) {
    private val TAG = "AiLoginDialog"
    private val binding = CommonLayoutAiLoginDialogBinding.inflate(LayoutInflater.from(context))
    private var listener: AiLoginListener? = null
    private var loadingDialog: CommonLoadingDialog = CommonLoadingDialog.Builder(context).setMessage("").builder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        setCanceledOnTouchOutside(true)
        binding.tvCancel.setOnClickListener {
            dismiss()
        }

        binding.tvConfirm.setOnClickListener {
            val account = binding.etAccount.text.toString().trim()
            val pwd = binding.etPwd.text.toString().trim()
            if (TextUtils.isEmpty(account)) {
                AutelToast.Companion.normalToast(context, context.getString(R.string.common_text_rtk_input_account))
                return@setOnClickListener
            }

            if (TextUtils.isEmpty(pwd)) {
                AutelToast.Companion.normalToast(context, context.getString(R.string.common_text_rtk_input_password))
                return@setOnClickListener
            }
            loadingDialog.show()
            CoroutineScope(Dispatchers.IO).launch(CoroutineExceptionHandler { _, throwable ->
                AutelLog.e(TAG, "loginByPassWord -> e=$throwable")
            }) {
                val result = CloudServiceManager.Companion.getInstance().getAiServiceManager().loginByPassWord(account, pwd)
                AutelLog.e(TAG, "loginByPassWord -> result=$result")
                CoroutineScope(Dispatchers.Main).launch {
                    if (result != null) {
                        AutelStorageManager.getPlainStorage().setStringValue(StorageKey.PlainKey.AI_USER_ID, result.userId)
                        AutelStorageManager.getPlainStorage().setStringValue(StorageKey.PlainKey.AI_USER_TOKEN, result.accessToken)
                        AiServiceManager.initAiService()
                        listener?.loginSuccess()
                        AutelToast.Companion.normalToast(context, context.getString(R.string.common_text_flight_record_login_success))
                        dismiss()
                    } else {
                        AutelToast.Companion.normalToast(context, context.getString(R.string.common_text_login_error))
                    }
                    loadingDialog.dismiss()
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
        })

        binding.tbEye.isChecked = false
        binding.etPwd.transformationMethod = PasswordTransformationMethod.getInstance()
    }

    /**
     * 设置登录成功监听
     */
    fun setAiLoginListener(listener: AiLoginListener): AiLoginDialog {
        this.listener = listener
        return this
    }
}