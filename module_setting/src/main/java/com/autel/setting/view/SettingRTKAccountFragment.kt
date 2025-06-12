package com.autel.setting.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.autel.common.base.BaseAircraftFragment
import com.autel.setting.databinding.SettingRtkAccountFragmentBinding

/**
 * com.autel.setting.view
 *
 * Copyright: Autel Robotics
 *
 * @author R22711 on 2023/4/21.
 */
class SettingRTKAccountFragment: BaseAircraftFragment()  {

    private lateinit var rtkFragmentBinding: SettingRtkAccountFragmentBinding


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        rtkFragmentBinding= SettingRtkAccountFragmentBinding.inflate(LayoutInflater.from(context))
        initView()
        return rtkFragmentBinding.root
    }

    private fun initView() {

    }
    override fun getData() {

    }

    override fun addListen() {

    }


}