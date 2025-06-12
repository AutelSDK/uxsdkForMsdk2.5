package com.autel.setting.widget

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.autel.setting.databinding.SettingLayoutGimbalCalibrationProgressBinding
import com.autel.setting.databinding.SettingViewCalibrationStatusBinding
import com.autel.ui.multistatus.MultiState
import com.autel.ui.multistatus.MultiStateContainer

/**
 * Created by  2022/10/13
 */
class SettingGimbalCalibrationResultMultiState : MultiState() {
    override fun onCreateMultiStateView(context: Context, inflater: LayoutInflater, container: MultiStateContainer): View {
        return SettingViewCalibrationStatusBinding.inflate(inflater, container, false).root
    }

    override fun onMultiStateViewCreated() {

    }
}