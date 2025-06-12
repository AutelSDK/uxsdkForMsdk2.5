package com.autel.setting.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.autel.common.base.BaseAircraftFragment
import com.autel.common.manager.AppInfoManager
import com.autel.common.manager.AutelStorageManager
import com.autel.common.manager.SameResourceHelper
import com.autel.common.manager.StorageKey
import com.autel.setting.R
import com.autel.setting.databinding.SettingControllerCustomKeyFragmentBinding
import com.autel.common.utils.CustomKeyUtils

class SettingControllerCustomKeyFragment : BaseAircraftFragment() {

    companion object {
        const val TAG = "SettingControllerCustomKeyFragment"
        const val DEFAULT_POSITION = 0
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = SettingControllerCustomKeyFragmentBinding.inflate(inflater, container, false)
        initView(rootView)
        return rootView.root
    }

    private fun initView(rootView: SettingControllerCustomKeyFragmentBinding) {

        val customKeys = ArrayList<String>()
        CustomKeyUtils.getCustomKeyList().forEach {
            customKeys.add(getString(it.id))
        }

        //自定义C1按键
        rootView.tvSelectC1Spinner.dataList = customKeys
        rootView.tvSelectC1Spinner.setDefaultText(CustomKeyUtils.getDefineCustomC1Index())
        rootView.tvSelectC1Spinner.setSpinnerViewListener { position ->
            AutelStorageManager.getPlainStorage()
                .setIntValue(StorageKey.PlainKey.KEY_C1_CUSTOM_DEFINE_KEY, CustomKeyUtils.getCustomKeyList()[position].value)
        }

        //自定义C2按键
        rootView.tvSelectC2Spinner.dataList = customKeys
        rootView.tvSelectC2Spinner.setDefaultText(CustomKeyUtils.getDefineCustomC2Index())
        rootView.tvSelectC2Spinner.setSpinnerViewListener { position ->
            AutelStorageManager.getPlainStorage()
                .setIntValue(StorageKey.PlainKey.KEY_C2_CUSTOM_DEFINE_KEY, CustomKeyUtils.getCustomKeyList()[position].value)
        }

        //小屏适配
        rootView.tvKeyDefine.text =
            getString(if (AppInfoManager.isLargeScreen()) R.string.common_text_c1_c2_key_define else R.string.common_text_c_key_define)
        rootView.llCustomRight.isVisible = AppInfoManager.isLargeScreen()
        rootView.tvCustomLeft.isVisible = AppInfoManager.isLargeScreen()
        rootView.ivKeyDefine.setImageResource(SameResourceHelper.getRemoteCustomDefineRes())
    }

    override fun getData() {

    }

    override fun addListen() {

    }

}