package com.autel.setting.view.rid

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import com.autel.common.manager.AutelStorageManager
import com.autel.common.manager.StorageKey
import com.autel.common.widget.dialog.BaseDialogFragment
import com.autel.common.widget.toast.AutelToast
import com.autel.setting.R
import com.autel.setting.databinding.SettingDialogPilotIdBinding

/**
 * 飞手ID
 */
class PilotIdDialog : BaseDialogFragment() {

    private lateinit var uiBinding: SettingDialogPilotIdBinding

    private val remoteIdVM: com.autel.setting.business.RemoteIdVM by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        uiBinding = SettingDialogPilotIdBinding.inflate(layoutInflater)
        return uiBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val id = AutelStorageManager.getPlainStorage().getStringValue(StorageKey.PlainKey.KEY_RID_PILOT_ID)
        id?.let {
            uiBinding.pilotId.setText(id)
        }
//        uiBinding.pilotId.isEnabled = !id.isNullOrEmpty()
        uiBinding.save.setOnClickListener {
            val pilotId = uiBinding.pilotId.text?.toString() ?: return@setOnClickListener
            remoteIdVM.setOperatorIdInfo(pilotId) {
                if (it) {
                    AutelToast.normalToast(context ?: return@setOperatorIdInfo, R.string.common_text_setting_success)
                    dismiss()
                } else {
                    AutelToast.normalToast(context ?: return@setOperatorIdInfo, R.string.common_text_set_failed)
                }
            }
        }

        uiBinding.cancel.setOnClickListener { dismiss() }

        uiBinding.remember.setOnCheckedChangeListener { _, isChecked ->
            AutelStorageManager.getPlainStorage().setBooleanValue(StorageKey.PlainKey.KEY_RID_NOT_TIPS_REMEMBER, isChecked)
        }

        uiBinding.pilotId.addTextChangedListener {
            val str = it.toString()
            uiBinding.save.isEnabled = !str.isNullOrEmpty()
        }
    }
}