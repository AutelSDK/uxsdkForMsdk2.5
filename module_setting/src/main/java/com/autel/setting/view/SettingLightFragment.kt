package com.autel.setting.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import com.autel.common.base.BaseAircraftFragment
import com.autel.common.listener.DisclaimerListener
import com.autel.common.manager.AppInfoManager
import com.autel.common.manager.CommonDialogManager
import com.autel.common.sdk.business.DroneLightVM
import com.autel.common.utils.DeviceUtils
import com.autel.common.widget.dialog.CommonNoTitleTwoButtonDialog
import com.autel.common.widget.dialog.CommonTwoButtonDialog
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.common.bean.LampLanguageCommandBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.common.enums.DroneLedStatusEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.common.enums.LampDirectCtrlEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.common.enums.LampLanguageTypeEnum
import com.autel.log.AutelLog
import com.autel.setting.R
import com.autel.setting.databinding.SettingLightFragmentBinding

/**
 * @Author create by LJ
 * @Date 2022/09/13 13:45
 * 灯光设置
 */
class SettingLightFragment : BaseAircraftFragment() {
    companion object {
        const val TAG = "SettingLightFragment"
    }

    private lateinit var binding: SettingLightFragmentBinding
    private val droneLightVm: DroneLightVM by activityViewModels()

    //下视补光灯默认选择
    private var currentBottomLightPosition = 0
    private var silentModeHintDialog: CommonNoTitleTwoButtonDialog? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = SettingLightFragmentBinding.inflate(LayoutInflater.from(context))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    override fun getData() {
        getAllLedLight()
        DeviceUtils.singleControlDrone()?.let { droneDevice ->
            droneLightVm.getSilentMode(droneDevice, {
                binding.lightCisConcealment.setCheckedWithoutListener(it)
                initLedEnable(!it)
            }, onError = {})
        }
    }

    /**
     * 获取所有灯光的状态
     */
    private fun getAllLedLight() {
        droneLightVm.queryAllLedLight()
    }

    /**
     * 下视补光灯
     */
    private fun initBottomLed(ledStatus: DroneLedStatusEnum) {
        when (ledStatus) {
            DroneLedStatusEnum.OPEN -> {
                currentBottomLightPosition = 1
                binding.csvBottomLight.setDefaultText(1)
            }

            DroneLedStatusEnum.CLOSE -> {
                currentBottomLightPosition = 2
                binding.csvBottomLight.setDefaultText(2)
            }

            else -> {
                currentBottomLightPosition = 0
                binding.csvBottomLight.setDefaultText(0)
            }
        }
    }

    override fun addListen() {
        droneLightVm.bottomLightLD.observe(viewLifecycleOwner) {
            initBottomLed(it)
        }
        droneLightVm.silenceModeStatusLD.observe(viewLifecycleOwner) {
            binding.lightCisConcealment.setCheckedWithoutListener(it)
            initLedEnable(!it)
        }
        droneLightVm.navigationLightLD.observe(viewLifecycleOwner) {
            binding.lightCisNight.setCheckedWithoutListener(it)
        }
        droneLightVm.armLampStatusLD.observe(viewLifecycleOwner) {
            binding.cisArmLight.setCheckedWithoutListener(it != LampDirectCtrlEnum.ALL_OFF)
        }
    }

    private fun initView() {
        binding.lightCisConcealment.isVisible = AppInfoManager.isSupportStealthMode()
        if (!AppInfoManager.isSupportStealthMode()){
            initLedEnable(true)
        }

        binding.lightCisConcealment.setOnSwitchChangeListener { checked ->
            if (!DeviceUtils.isSingleControlDroneConnected()) {
                binding.lightCisConcealment.setCheckedWithoutListener(!checked)
                showToast(R.string.common_text_aircraft_disconnect)
                return@setOnSwitchChangeListener
            }
            //需要二次弹框
            if (checked) {
                CommonDialogManager.showStealthModeDialog(object : DisclaimerListener {
                    override fun onCallBack(isCommit: Boolean) {
                        AutelLog.i(TAG, "DisclaimerDialog -> onCallBack isCommit=$isCommit")
                        if (isCommit) {
                            setSilentMode(checked)
                        } else {
                            binding.lightCisConcealment.setCheckedWithoutListener(!checked)
                        }
                    }
                })
            } else {
                setSilentMode(checked)
            }
        }

        binding.lightCisNight.setOnSwitchChangeListener { checked ->
            droneLightVm.setNavigationLight(
                checked,
                onSuccess = {

                },
                onError = {
                    showToast(R.string.common_text_set_failed)
                    binding.lightCisNight.setCheckedWithoutListener(!checked)
                })
        }

        binding.cisArmLight.setOnSwitchChangeListener {
            if (it) {
                droneLightVm.switchArmLightLEDStatusLD(LampLanguageCommandBean(LampLanguageTypeEnum.DIRECT_CTRL, LampDirectCtrlEnum.ALL_ON))
            } else {
                droneLightVm.switchArmLightLEDStatusLD(LampLanguageCommandBean(LampLanguageTypeEnum.DIRECT_CTRL, LampDirectCtrlEnum.ALL_OFF))
            }
        }

        val radioButtons = resources.getStringArray(R.array.common_text_light_setting)

        binding.csvBottomLight.dataList = radioButtons.toList()
        binding.csvBottomLight.setDefaultText(resources.getString(R.string.common_text_auto))
        binding.csvBottomLight.setSpinnerViewListener { position ->
            //如果是打开的话，需要二次弹框
            if (position == 1) {
                context?.let {
                    CommonTwoButtonDialog(it)
                        .apply {
                            setTitle(it.getString(R.string.common_text_open_bottom_light_title))
                            setMessage(it.getString(R.string.common_text_open_bottom_light_tips))
                            setRightBtnListener {
                                setLedLight(position)
                            }
                            setLeftBtnListener {
                                binding.csvBottomLight.setDefaultText(currentBottomLightPosition)
                            }
                            show()
                        }
                }
            } else {
                setLedLight(position)
            }

        }
    }

    /**
     * 设置下视补光
     */
    private fun setLedLight(position: Int) {
        //0：自动，1：打开，2：关闭
        val ledStatus = when (position) {
            0 -> DroneLedStatusEnum.AUTO
            1 -> DroneLedStatusEnum.OPEN
            else -> DroneLedStatusEnum.CLOSE
        }
        droneLightVm.setLedLightStatus(
            ledStatus,
            onSuccess = {
                currentBottomLightPosition = position
                showToast(R.string.common_text_setting_success)
            },
            onError = {
                showToast(R.string.common_text_set_failed)
                binding.csvBottomLight.setDefaultText(currentBottomLightPosition)
            })
    }

    /**
     * 设置隐蔽模式
     */
    private fun setSilentMode(enable: Boolean) {
        DeviceUtils.singleControlDrone()?.let {
            droneLightVm.setSilenceModeStatus(it, enable, {
                //打开时，需要刷新夜航灯和下视补光灯的状态
                if (enable) getAllLedLight()
                initLedEnable(!enable)
            }, onError = {
                showToast(R.string.common_text_set_failed)
                binding.lightCisConcealment.setCheckedWithoutListener(!enable)
            })
        }
    }

    /**
     * 开启隐蔽模式之后，不允许开启夜航灯和下视补光灯
     */
    private fun initLedEnable(enable: Boolean) {
        binding.lightCisNight.isVisible = enable && AppInfoManager.isSupportNavigationLight()
        binding.llBottomLight.isVisible = enable && AppInfoManager.isSupportDownFillLight()
        binding.cisArmLight.isVisible = enable && AppInfoManager.isSupportArmLight()
    }
}