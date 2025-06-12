package com.autel.setting.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.autel.common.base.BaseAircraftFragment
import com.autel.common.manager.MiddlewareManager
import com.autel.common.widget.dialog.CommonTwoButtonDialog
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.remotecontrol.enums.RcOperateModeEnum
import com.autel.log.AutelLog
import com.autel.setting.R
import com.autel.setting.business.SettingControlVM
import com.autel.setting.databinding.SettingControllerModelBinding
import com.autel.setting.databinding.SettingControllerModelFragmentBinding

/**
 * @Author create by LJ
 * @Date 2022/09/08 15:52
 * 遥控器模式设置  0：日本手、1:美国手、2:中国手
 */
class SettingControllerModelFragment : BaseAircraftFragment() {
    private lateinit var binding: SettingControllerModelFragmentBinding
    private val settingControlVM: SettingControlVM by viewModels()
    private var lastModel: RcOperateModeEnum = RcOperateModeEnum.AMERICA_HAND

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = SettingControllerModelFragmentBinding.inflate(LayoutInflater.from(context))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    private fun initView() {
        context?.resources?.getStringArray(R.array.common_text_controller_model)
            ?.let { binding.settingCrcSelectorControllerModel.addRadioButton(it) }

        binding.settingCrcSelectorControllerModel.setOnSwitchChangeListener {
            CommonTwoButtonDialog(requireContext()).apply {
                setMessage(getString(R.string.common_text_sure_change_controller_model))
                setRightBtnListener { refreshRCMode(it, true) }
                setLeftBtnListener {
                    setRadioGroupCheck(lastModel)
                }
                show()
            }
        }

    }

    private fun refreshRCMode(index: Int, isChange: Boolean) {
        when (index) {
            0 -> choiceChina(binding.settingIncludeControllModel, isChange)
            1 -> choiceUSA(binding.settingIncludeControllModel, isChange)
            2 -> choiceJapan(binding.settingIncludeControllModel, isChange)
        }
    }

    override fun getData() {
    }

    override fun onResume() {
        super.onResume()
        settingControlVM.getRCRockerControlMode(
            onSuccess = {
                //指令成功处理
                lastModel = it
                setRadioGroupCheck(lastModel)
                refreshRCMode(getIndexByMode(lastModel), false)
            },
            onError = {
                //指令失败处理
            })
    }

    /**
     * 根据模式获取其index
     */
    private fun getIndexByMode(mode: RcOperateModeEnum): Int {
        return when (mode) {
            RcOperateModeEnum.CHINESE_HAND -> 0
            RcOperateModeEnum.AMERICA_HAND -> 1
            RcOperateModeEnum.JAPANESE_HAND -> 2
        }
    }

    private fun setRadioGroupCheck(modeEnum: RcOperateModeEnum) {
        binding.settingCrcSelectorControllerModel.setRadioGroupCheck(getIndexByMode(modeEnum))
    }

    /**
     * 日本手
     */
    private fun choiceJapan(settingIncludeControlModel: SettingControllerModelBinding, isChange: Boolean) {
        context?.apply {

            settingIncludeControlModel.settingIvLeftMode.background = getDrawable(R.drawable.setting_japan_left)
            settingIncludeControlModel.settingTvLeftTop.text = getString(R.string.common_text_controller_model_forward_title)
            settingIncludeControlModel.settingTvLeftRight.text = getString(R.string.common_text_controller_model_turn_right_title)
            settingIncludeControlModel.settingTvLeftBottom.text = getString(R.string.common_text_controller_model_back_title)
            settingIncludeControlModel.settingTvLeftLeft.text = getString(R.string.common_text_controller_model_turn_left_title)

            settingIncludeControlModel.settingIvRightMode.background = getDrawable(R.drawable.setting_japan_right)
            settingIncludeControlModel.settingTvRightTop.text = getText(R.string.common_text_controller_model_up_title)
            settingIncludeControlModel.settingTvRightRight.text = getText(R.string.common_text_controller_model_right_title)
            settingIncludeControlModel.settingTvRightBottom.text = getText(R.string.common_text_controller_model_down_title)
            settingIncludeControlModel.settingTvRightLeft.text = getText(R.string.common_text_controller_model_left_title)
            if (isChange) setRCRockerControlMode(RcOperateModeEnum.JAPANESE_HAND)
        }

    }

    /**
     * 中国手
     */
    private fun choiceChina(settingIncludeControlModel: SettingControllerModelBinding, isChange: Boolean) {
        context?.apply {
            settingIncludeControlModel.settingIvLeftMode.background = getDrawable(R.drawable.setting_usa_right)
            settingIncludeControlModel.settingTvLeftTop.text = getString(R.string.common_text_controller_model_forward_title)
            settingIncludeControlModel.settingTvLeftRight.text = getString(R.string.common_text_controller_model_right_title)
            settingIncludeControlModel.settingTvLeftBottom.text = getString(R.string.common_text_controller_model_back_title)
            settingIncludeControlModel.settingTvLeftLeft.text = getString(R.string.common_text_controller_model_left_title)

            settingIncludeControlModel.settingIvRightMode.background = getDrawable(R.drawable.setting_usa_left)
            settingIncludeControlModel.settingTvRightTop.text = getText(R.string.common_text_controller_model_up_title)
            settingIncludeControlModel.settingTvRightRight.text = getText(R.string.common_text_controller_model_turn_right_title)
            settingIncludeControlModel.settingTvRightBottom.text = getText(R.string.common_text_controller_model_down_title)
            settingIncludeControlModel.settingTvRightLeft.text = getText(R.string.common_text_controller_model_turn_left_title)
            if (isChange) setRCRockerControlMode(RcOperateModeEnum.CHINESE_HAND)
        }
    }

    /**
     * 美国手
     */
    private fun choiceUSA(settingIncludeControlModel: SettingControllerModelBinding, isChange: Boolean) {
        context?.apply {
            settingIncludeControlModel.settingIvLeftMode.background = getDrawable(R.drawable.setting_usa_left)
            settingIncludeControlModel.settingTvLeftTop.text = getString(R.string.common_text_controller_model_up_title)
            settingIncludeControlModel.settingTvLeftRight.text = getString(R.string.common_text_controller_model_turn_right_title)
            settingIncludeControlModel.settingTvLeftBottom.text = getString(R.string.common_text_controller_model_down_title)
            settingIncludeControlModel.settingTvLeftLeft.text = getString(R.string.common_text_controller_model_turn_left_title)

            settingIncludeControlModel.settingIvRightMode.background = getDrawable(R.drawable.setting_usa_right)
            settingIncludeControlModel.settingTvRightTop.text = getText(R.string.common_text_controller_model_forward_title)
            settingIncludeControlModel.settingTvRightRight.text = getText(R.string.common_text_controller_model_right_title)
            settingIncludeControlModel.settingTvRightBottom.text = getText(R.string.common_text_controller_model_back_title)
            settingIncludeControlModel.settingTvRightLeft.text = getText(R.string.common_text_controller_model_left_title)
            if (isChange) setRCRockerControlMode(RcOperateModeEnum.AMERICA_HAND)
        }
    }

    private fun setRCRockerControlMode(newModel: RcOperateModeEnum) {
        settingControlVM.setRCRockerControlMode(
            newModel,
            onSuccess = {
                lastModel = newModel
            },
            onError = {
                setRadioGroupCheck(lastModel)
                refreshRCMode(lastModel.value, false)
            })
    }

    override fun addListen() {

    }

}