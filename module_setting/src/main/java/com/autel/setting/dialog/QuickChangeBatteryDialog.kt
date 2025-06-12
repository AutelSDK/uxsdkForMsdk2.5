package com.autel.setting.dialog

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.autel.common.R
import com.autel.common.widget.dialog.BaseDialogFragment
import com.autel.setting.databinding.DialogQuickChangeBatteryBinding

/**
 * @author 
 * @date 2023/2/9
 * 快速换电弹框
 */
class QuickChangeBatteryDialog(val dialogShow: (Boolean) -> Unit,val isFlying: Boolean ?= false) : BaseDialogFragment() {
    private lateinit var uiBinding: DialogQuickChangeBatteryBinding

    init {
        val bundle = Bundle()
        arguments = bundle
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        uiBinding = DialogQuickChangeBatteryBinding.inflate(inflater, container, false)
        return uiBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        uiBinding.tvConfirm.setOnClickListener { dismiss() }
        val msgId = if (isFlying == true)R.string.common_text_battery_no_reign_1_x_flying else R.string.common_text_battery_no_reign_1_x
        context?.let {
            uiBinding.tvMsg.text = it.getString(msgId)
        }
    }

    override fun onResume() {
        super.onResume()
        dialog?.setCanceledOnTouchOutside(false)
        dialogShow.invoke(true)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        dialogShow.invoke(false)
    }

    override fun dismiss() {
        dismissAllowingStateLoss()
    }
}