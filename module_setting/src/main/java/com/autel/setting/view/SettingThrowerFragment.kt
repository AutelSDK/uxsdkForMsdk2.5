package com.autel.setting.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.autel.common.base.BaseAircraftFragment
import com.autel.common.manager.AppInfoManager
import com.autel.common.manager.AutelStorageManager
import com.autel.common.manager.StorageKey
import com.autel.log.AutelLog
import com.autel.setting.R
import com.autel.setting.databinding.SettingThrowerFragmentBinding

/**
 * 抛投器设置页面
 */
class SettingThrowerFragment : BaseAircraftFragment() {

    companion object {
        const val TAG = "SettingThrowerFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val rootView = SettingThrowerFragmentBinding.inflate(LayoutInflater.from(context))
        initView(rootView)
        return rootView.root
    }

    private fun initView(rootView: SettingThrowerFragmentBinding) {

        rootView.viewHistogram.setCheckedWithoutListener(AutelStorageManager.getPlainStorage().getBooleanValue(
            StorageKey.PlainKey.KEY_THROWER_AUTO_RECORD, true))
        rootView.viewHistogram.setOnSwitchChangeListener {
            AutelStorageManager.getPlainStorage().setBooleanValue(
                StorageKey.PlainKey.KEY_THROWER_AUTO_RECORD, it)
        }

        val selType = AutelStorageManager.getPlainStorage().getIntValue(StorageKey.PlainKey.KEY_FIRE_STRIKE_TYPE, 0)
        val strikeTypes = resources.getStringArray(R.array.common_fire_strike_type)
        rootView.csvStrikeType.dataList = strikeTypes.toList()
        rootView.csvStrikeType.setDefaultText(selType)
        rootView.csvStrikeType.setSpinnerViewListener {
            AutelLog.i(TAG, "设置抛投类型：$it")
            AutelStorageManager.getPlainStorage().setIntValue(StorageKey.PlainKey.KEY_FIRE_STRIKE_TYPE, it)
        }
        rootView.clStrike.isVisible = AppInfoManager.isSupportIntelligentThrowing()
    }



    override fun getData() {

    }

    override fun addListen() {

    }
}