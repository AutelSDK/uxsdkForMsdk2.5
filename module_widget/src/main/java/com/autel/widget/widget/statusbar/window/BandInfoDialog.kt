package com.autel.widget.widget.statusbar.window

import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import androidx.core.view.isVisible
import com.autel.common.feature.location.CountryManager
import com.autel.common.manager.AppInfoManager
import com.autel.common.manager.AutelStorageManager
import com.autel.common.manager.StorageKey
import com.autel.common.widget.dialog.BaseAutelDialog
import com.autel.common.widget.dialog.CommonLoadingDialog
import com.autel.drone.sdk.vmodelx.enums.FrequencyBand
import com.autel.drone.sdk.vmodelx.manager.FrequencyBandManager
import com.autel.drone.sdk.vmodelx.manager.SDKConfigManager
import com.autel.drone.sdk.vmodelx.manager.frequency.OnFrequencyBandListener
import com.autel.drone.sdk.vmodelx.utils.ToastUtils
import com.autel.log.AutelLog
import com.autel.widget.R
import com.autel.widget.databinding.DialogBandInfoBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * @author 
 * @date 2024/2/21
 * 频段详情弹框
 */
class BandInfoDialog(context: Context) : BaseAutelDialog(context) {

    private val binding = DialogBandInfoBinding.inflate(LayoutInflater.from(context))
    private var curCode = "XX"
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
    private var loadingDialog: CommonLoadingDialog ?= null

    init {
        loadingDialog = CommonLoadingDialog(context)
        binding.tvConfirm.setOnClickListener {
            val code = binding.etInputCode.text.toString().trim()
            AutelLog.i("BandInfoDialog", "tvConfirm -> code=$code")
            if (TextUtils.isEmpty(code)){
                ToastUtils.showToast("国家码不能为空")
                return@setOnClickListener
            }
            loadingDialog?.show()
            scope.launch{
                delay(5000)
                scope.launch(Dispatchers.Main){ loadingDialog?.dismiss() }
            }
            SDKConfigManager.setCountryCode(code)
            CountryManager.notifyCountryChange(code, true)
        }
        binding.tvClose.setOnClickListener { dismiss() }
        binding.civSwitch.setCheckedWithoutListener(AppInfoManager.isSupportCountryModify())

        binding.civSwitch.setOnSwitchChangeListener {
            AutelStorageManager.getPlainStorage().setBooleanValue(StorageKey.PlainKey.SP_KEY_MODIFY_COUNTRY_CODE, it)
            refreshEditView()
            if (!it) SDKConfigManager.setCountryCode("")
        }

        refreshEditView()
    }

    private val frequencyBandListener = object : OnFrequencyBandListener {
        override fun onChange(country: String?, list: List<FrequencyBand>) {
            loadingDialog?.dismiss()
            AutelLog.i("BandInfoDialog", "frequencyBand onChange -> country=$country list=$list")
            if (curCode != country) {
                curCode = country ?: ""
                binding.tvCountryCode.text = "国家码：${curCode.ifEmpty { "空" }}"
                var bandMode = ""
                list.forEach { bandMode = "${if (TextUtils.isEmpty(bandMode)) "" else "$bandMode _ "}${it.tag}" }
                binding.tvBandMode.text = "合规频段：$bandMode"
            }
        }
    }

    private fun refreshEditView() {
        val isSupport = AppInfoManager.isSupportCountryModify()
        binding.etInputCode.isVisible = isSupport
        binding.tvConfirm.isEnabled = isSupport
        binding.tvConfirm.setTextColor(context.getColor(if (isSupport) R.color.common_color_007aff else R.color.common_color_61646A))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setCanceledOnTouchOutside(false)
        FrequencyBandManager.get().addListener(frequencyBandListener)
    }

    override fun dismiss() {
        super.dismiss()
        FrequencyBandManager.get().removeListener(frequencyBandListener)
    }
}