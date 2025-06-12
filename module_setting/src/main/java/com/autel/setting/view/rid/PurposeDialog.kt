package com.autel.setting.view.rid

import android.os.Bundle
import android.text.InputFilter
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
import com.autel.setting.databinding.SettingDialogPurposeBinding

/**
 * 飞行目的
 */
//@Route(path = RouterConst.PathConst.ACTIVITY_URL_RID_PURPOSE)
class PurposeDialog : BaseDialogFragment() {

    private lateinit var uibinding: SettingDialogPurposeBinding

    private val remoteIdVM: com.autel.setting.business.RemoteIdVM by activityViewModels()

    private val MAX_PURPOSE = 23

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        uibinding = SettingDialogPurposeBinding.inflate(layoutInflater)
        return uibinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        uibinding.purpose.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(MAX_PURPOSE))
        uibinding.purpose.addTextChangedListener {
            uibinding.length.text = "${it?.length ?: "0"}/$MAX_PURPOSE"
        }

        uibinding.purpose.setText(AutelStorageManager.getPlainStorage().getStringValue(StorageKey.PlainKey.KEY_RID_PURPOSE, ""))
        uibinding.save.setOnClickListener {
            val msg = uibinding.purpose.text?.toString() ?: ""
            remoteIdVM.setPurposeOfFlight(uibinding.purpose.text?.toString() ?: "") {
                if (it) {
                    AutelStorageManager.getPlainStorage().setStringValue(StorageKey.PlainKey.KEY_RID_PURPOSE, msg)
                    AutelToast.normalToast(context ?: return@setPurposeOfFlight, R.string.common_text_setting_success)
                    dismiss()
                } else {
                    AutelToast.normalToast(context ?: return@setPurposeOfFlight, R.string.common_text_set_failed)
                }
            }
        }

        uibinding.cancel.setOnClickListener { dismiss() }
    }

}