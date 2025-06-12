package com.autel.setting.view

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.autel.common.base.BaseFragment
import com.autel.setting.R
import com.autel.setting.business.SettingHDVM
import com.autel.setting.databinding.SettingServerConfigBinding
import com.autel.setting.state.SwitchStateVM


/**
 * @author
 * @date 2025/4/9
 * 私有化服务器配置
 */
class SettingServerConfigFragment : BaseFragment() {
    private var binding: SettingServerConfigBinding? = null
    private val switchVM: SwitchStateVM by activityViewModels()
    private val settingHDVM: SettingHDVM by viewModels()
    private var defaultDroneUrl = "" //默认飞机地址
    private var defaultRcUrl = "" //默认遥控器地址
    private var curDroneUrl = ""
    private var curRcUrl = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return SettingServerConfigBinding.inflate(inflater).also { binding = it }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        defaultDroneUrl = arguments?.getString("droneConfigUrl", "") ?: ""
        defaultRcUrl = arguments?.getString("rcConfigUrl", "") ?: ""
        binding?.etDroneServer?.setText(defaultDroneUrl)
        binding?.etRcServer?.setText(defaultRcUrl)

        binding?.etDroneServer?.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(edit: Editable?) {
                curDroneUrl = edit.toString().trim()
                refreshSavaView()
            }

        })

        binding?.etRcServer?.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(edit: Editable?) {
                curRcUrl = edit.toString().trim()
                refreshSavaView()
            }

        })

        binding?.tvResetDrone?.setOnClickListener {
            settingHDVM.resetDroneUrl(onSuccess = {
                showToast(R.string.common_text_reset_succeeded)
                binding?.etDroneServer?.setText("")
            }, onError = {
                showToast(R.string.common_text_reset_failed)
            })
        }
        binding?.tvResetRc?.setOnClickListener {
            settingHDVM.resetRcUrl(onSuccess = {
                showToast(R.string.common_text_reset_succeeded)
                binding?.etRcServer?.setText("")
            }, onError = {
                showToast(R.string.common_text_reset_failed)
            })
        }

        binding?.tvSave?.setOnClickListener {
            val isSame = defaultDroneUrl == curDroneUrl && defaultRcUrl == curRcUrl
            if (isSame) return@setOnClickListener

            val droneUrl = binding?.etDroneServer?.text.toString().trim()
            val rcUrl = binding?.etRcServer?.text.toString().trim()
            settingHDVM.saveDroneAndRcUrl(droneUrl, rcUrl, onSuccess = {
                switchVM.back()
                showToast(R.string.common_text_save_success_title)
            }, onError = {
                val id = if (it) R.string.common_text_sava_failure_params else R.string.common_text_try_to_save
                showToast(id)
            })
        }
        refreshSavaView()
    }

    /**
     * 刷新按钮
     */
    private fun refreshSavaView() {
        val isSame = defaultDroneUrl == curDroneUrl && defaultRcUrl == curRcUrl
        binding?.tvSave?.setBackgroundResource(if (isSame) R.drawable.common_shape_rect_white50_r9 else R.drawable.common_shape_rect_white_r9)
    }
}