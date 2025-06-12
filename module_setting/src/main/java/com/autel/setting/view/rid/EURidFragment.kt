package com.autel.setting.view.rid

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import com.autel.common.base.BaseAircraftFragment
import com.autel.common.manager.AutelStorageManager
import com.autel.common.manager.StorageKey
import com.autel.common.widget.dialog.CommonTwoButtonDialog
import com.autel.drone.sdk.vmodelx.manager.uas.UASRemoteIDManager
import com.autel.log.AutelLog
import com.autel.setting.R
import com.autel.setting.business.RemoteIdVM
import com.autel.setting.databinding.SettingActivityEuRidBinding

class EURidFragment : BaseAircraftFragment() {

    private val TAG = "EURidFragment"

    private val viewModel: RemoteIdVM by viewModels()

    private lateinit var binding: SettingActivityEuRidBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = SettingActivityEuRidBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.back.setOnClickListener { activity?.finish() }
        binding.input.addTextChangedListener {
            val isCheck = viewModel.checkEUFlyId(it?.toString() ?: "")
            binding.login.isEnabled = isCheck == 0
            binding.ivClearEdittext.isVisible = it?.isNotEmpty() ?: false
            if (isCheck != 0) {
                binding.inputTips.setText(if (isCheck == -1) R.string.common_text_please_enter_the_correct_pilot_id else R.string.common_text_please_enter_the_correct_country_code)
                binding.inputTips.setTextColor(ContextCompat.getColor(requireContext(), R.color.common_color_fd2d55))
            } else {
                binding.inputTips.setText(R.string.common_text_input_pilot_id_2_tips)
                binding.inputTips.setTextColor(ContextCompat.getColor(requireContext(), R.color.common_color_808080))
            }
        }

        binding.login.setOnClickListener {
            val str = binding.input.text.toString()
            AutelLog.i(TAG, "setOperatorIdInfo id = $str")
            UASRemoteIDManager.get().setUASPilotID(str, null)
            AutelStorageManager.getPlainStorage().setStringValue(StorageKey.PlainKey.RID_OPERATOR_ID, str)
            showToast(R.string.common_text_setting_success)
            activity?.finish()
        }

        binding.ivClearEdittext.setOnClickListener {
            binding.input.setText("")
        }

        binding.clear.setOnClickListener {
            val dialog = CommonTwoButtonDialog(requireContext())
            dialog.setTitle(getString(R.string.common_text_clear_flyer_id))
            dialog.setMessage(getString(R.string.common_text_clear_flyer_id_tips))
            dialog.setLeftBtnStr(getString(R.string.common_text_cancel))
            dialog.setRightBtnStr(getString(R.string.common_text_clear))
            dialog.setRightBtnStrColor(ContextCompat.getColor(requireContext(), R.color.common_color_fd2d55))
            dialog.setLeftBtnListener {
                dialog.dismiss()
            }
            dialog.setRightBtnListener {
                AutelStorageManager.getPlainStorage().setStringValue(StorageKey.PlainKey.RID_OPERATOR_ID, "")
                UASRemoteIDManager.get().setUASPilotID("", null)
                dialog.dismiss()
            }
            dialog.show()
        }

        val id = AutelStorageManager.getPlainStorage().getStringValue(StorageKey.PlainKey.RID_OPERATOR_ID, "") ?: ""
        binding.input.setText(id)

        /* debug code
        if (BuildConfig.DEBUG) {
            binding.input.setText("FIN87astrdge12k8-xyz")
        }*/
    }

    override fun getData() {
        // NO-OP
    }

}