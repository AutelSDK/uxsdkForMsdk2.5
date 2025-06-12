package com.autel.setting.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.autel.common.base.BaseAircraftFragment
import com.autel.common.base.BaseFragment
import com.autel.setting.databinding.SettingCurrencyFragmentBinding

/**
 * @Author create by LJ
 * @Date 2023/2/14 17
 */
class SettingCurrencyFragment : BaseAircraftFragment() {

    private lateinit var settingCurrencyFragment: SettingCurrencyFragmentBinding
    override fun getData() {

    }

    override fun addListen() {

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        settingCurrencyFragment = SettingCurrencyFragmentBinding.inflate(LayoutInflater.from(context))
        return super.onCreateView(inflater, container, savedInstanceState)
    }
}