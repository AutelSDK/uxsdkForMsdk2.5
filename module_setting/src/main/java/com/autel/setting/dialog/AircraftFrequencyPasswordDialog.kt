package com.autel.setting.dialog

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import com.autel.common.widget.dialog.BaseAutelDialog
import com.autel.setting.R
import com.autel.setting.databinding.SettingDialogAircraftFrequencyPasswordBinding
import java.util.regex.Pattern

/**
 * 飞机对频密码弹窗
 */
class AircraftFrequencyPasswordDialog(context: Context) : BaseAutelDialog(context) {
    private val binding = SettingDialogAircraftFrequencyPasswordBinding.inflate(LayoutInflater.from(context))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setCanceledOnTouchOutside(false)
        setCancelable(false)
        initView()
        initListener()
    }

    private fun initView() {
        binding.agreementSubmitTv.isEnabled = false
    }

    private fun initListener() {
        binding.aetInputPwd.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (binding.aetInputPwd.text!!.isNotEmpty()) {
                    binding.btnPwd.visibility = View.VISIBLE
                    binding.btnPwd.setOnCheckedChangeListener { _, b ->
                        if (b) {
                            binding.aetInputPwd.transformationMethod =
                                HideReturnsTransformationMethod.getInstance()
                        } else {
                            binding.aetInputPwd.transformationMethod =
                                PasswordTransformationMethod.getInstance()
                        }
                        binding.aetInputPwd.setSelection(binding.aetInputPwd.text.toString().length)
                    }
                } else {
                    binding.btnPwd.visibility = View.INVISIBLE
                }
                // 改变确认按钮可点击状态

                var enableValue = binding.aetInputPwd.text!!.isNotEmpty() && binding.aetConfirmPwd.text!!.isNotEmpty()
                changeSubmitTvState(enableValue)

            }

            override fun afterTextChanged(p0: Editable?) {
            }

        })
        binding.aetConfirmPwd.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (binding.aetConfirmPwd.text!!.isNotEmpty()) {
                    binding.btnConfigPwd.visibility = View.VISIBLE
                    binding.btnConfigPwd.setOnCheckedChangeListener { _, b ->
                        if (b) {
                            binding.aetConfirmPwd.transformationMethod =
                                HideReturnsTransformationMethod.getInstance()
                        } else {
                            binding.aetConfirmPwd.transformationMethod =
                                PasswordTransformationMethod.getInstance()
                        }
                        binding.aetConfirmPwd.setSelection(binding.aetConfirmPwd.text.toString().length)
                    }
                } else {
                    binding.btnConfigPwd.visibility = View.INVISIBLE
                }
                // 改变确认按钮可点击状态

                val enableValue = binding.aetInputPwd.text!!.isNotEmpty() && binding.aetConfirmPwd.text!!.isNotEmpty()
                changeSubmitTvState(enableValue)
            }

            override fun afterTextChanged(p0: Editable?) {
            }

        })
    }

    private fun changeSubmitTvState(enableValue: Boolean) {
        binding.agreementSubmitTv.isEnabled = enableValue
        if (enableValue) {
            binding.agreementSubmitTv.setBackgroundResource(R.drawable.common_shape_btn_bg_blue)
        } else {
            binding.agreementSubmitTv.setBackgroundResource(R.drawable.common_shape_btn_bg_blue_50)
        }
    }


    fun setOnCancelListener(listener: () -> Unit) {
        binding.passwordCancelTv.setOnClickListener {
            dismiss()
            listener.invoke()
        }
    }

    fun setOnConfirmListener(listener: (password: String) -> Unit) {
        binding.agreementSubmitTv.setOnClickListener {
            val inputPwd = binding.aetInputPwd.text.toString()
            val confirmPwd = binding.aetConfirmPwd.text.toString()
            if (inputPwd == confirmPwd) {
                if (validatePassword(inputPwd)) {
                    binding.passwordErrorTip.visibility = View.GONE
                    binding.rlConfirmPwd.background = context.getDrawable(R.drawable.common_shape_edit_text_normal)
                    binding.rlInputPassword.background = context.getDrawable(R.drawable.common_shape_edit_text_normal)
                    binding.aetInputPwd.setTextColor(context.getColor(R.color.common_color_33))
                    binding.aetConfirmPwd.setTextColor(context.getColor(R.color.common_color_33))
                    dismiss()
                    listener.invoke(inputPwd)
                } else {
                    binding.passwordErrorTip.visibility = View.VISIBLE
                    binding.rlInputPassword.background = context.getDrawable(R.drawable.common_shape_edit_text_error)
                    binding.passwordErrorTip.text = context.getString(R.string.common_text_set_frequency_password_length_tip)
                    binding.aetInputPwd.setTextColor(context.getColor(R.color.common_color_red))
                    binding.aetConfirmPwd.setTextColor(context.getColor(R.color.common_color_33))
                    binding.rlConfirmPwd.background = context.getDrawable(R.drawable.common_shape_edit_text_normal)
                }
            } else {
                binding.passwordErrorTip.visibility = View.VISIBLE
                binding.rlConfirmPwd.background = context.getDrawable(R.drawable.common_shape_edit_text_error)
                binding.passwordErrorTip.text = context.getString(R.string.common_text_make_sure_same_password)
                binding.aetConfirmPwd.setTextColor(context.getColor(R.color.common_color_red))
            }
        }
    }

    private fun validatePassword(password: String): Boolean {
        // 密码长度至少为8位
        if (password.length < 8) {
            return false
        }

        // 密码只能包含字母和数字
        val pattern = "^[a-zA-Z0-9]+$"
        return Pattern.matches(pattern, password)
    }


}