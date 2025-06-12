package com.autel.setting.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import com.autel.common.base.BaseAircraftFragment
import com.autel.setting.enums.StopPropellerEnum
import com.autel.common.constant.AppTagConst.SettingTag
import com.autel.common.constant.SharedParams
import com.autel.common.listener.DisclaimerListener
import com.autel.common.manager.AppInfoManager
import com.autel.common.manager.AutelStorageManager
import com.autel.common.manager.CommonDialogManager
import com.autel.common.manager.StorageKey
import com.autel.common.sdk.business.DroneControlVM
import com.autel.common.sdk.business.SettingAdvanceVM
import com.autel.common.sdk.business.SettingObstacleAvoidanceVM
import com.autel.common.utils.DeviceUtils
import com.autel.common.utils.GoogleTextToSpeechManager
import com.autel.common.widget.dialog.CommonInputDialog
import com.autel.common.widget.dialog.CommonTwoButtonDialog
import com.autel.common.widget.toast.AutelToast
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.drone.sdk.vmodelx.manager.FlightDynamicDataManager
import com.autel.drone.sdk.vmodelx.manager.WarnShowLevelEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.alink.enums.FccCeModeEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.enums.DroneGpsEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.mission.enums.FcsTmpDisarmAirEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.rtk.enums.RTKSignalModeEnum
import com.autel.log.AutelLog
import com.autel.setting.R
import com.autel.setting.databinding.SettingAdvanceFragmentBinding
import com.autel.setting.state.SwitchStateVM

/**
 * @author 
 * @date 2023/2/15
 * 高级设置
 */
open class SettingAdvanceFragment : BaseAircraftFragment() {
    private val TAG = "SettingAdvanceFragment"

    //选中FCC模式的radiogroup下标
    private val TAG_FCC = 0

    //选中CE模式的radiogroup下标
    private val TAG_CE = 1

    private val DATA_REPORT_NUMBER_HEAD = "UAS"//国内数据上报登记号，前缀
    private val DATA_REPORT_NUMBER_LENGTH_LIMIT = 11//国内数据上报登记号，前缀


    private lateinit var binding: SettingAdvanceFragmentBinding
    private val switchVM: SwitchStateVM by activityViewModels()
    private val obstacleAvoidanceVM: SettingObstacleAvoidanceVM by activityViewModels()
    private val settingAdvanceVm: SettingAdvanceVM by activityViewModels()
    private val droneControlVM: DroneControlVM by activityViewModels()
    private var isGnssSwitch = false//gnss定位开关
    private var isDataReportSwitch = false//数据上报开关
    private var fccMode = FccCeModeEnum.FCC//抗干扰模式
    private var gnssModeStrList: ArrayList<String> = ArrayList()
    private var isOpenStopPropeller = false//是否打开紧急停桨
    private var stopPropellerIndex = 0//紧急停桨状态
    private val stopPropellerList = ArrayList<StopPropellerEnum>()
    private var circAntiIndex = 0
    private var adsbExplanationPopWindow: AdsbExplanationPopWindow? = null

    private val gnssModeList = ArrayList<DroneGpsEnum>()
    private var gnssModeIndex = 0//GNSS的默认选择
    private var isChangeGnssMode = false//是否切换GNSS模式


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = SettingAdvanceFragmentBinding.inflate(LayoutInflater.from(context))
        return binding.root
    }

    override fun getData() {
        if (AppInfoManager.isSupportAntiInterference()) {
            binding.antiInterferenceContainer.isVisible = true
            //抗干扰模式
            settingAdvanceVm.getKeyALinkFccCeMode(onSuccess = {
                fccMode = it
                circAntiIndex = if (fccMode == FccCeModeEnum.CE || fccMode == FccCeModeEnum.CE_FOREVER) 1 else 0
                binding.circAntiInterference.setRadioGroupCheck(circAntiIndex)
            }, {})
        }
        //视觉定位开关
        obstacleAvoidanceVM.getLocationStatus(
            onSuccess = {
                binding.viewVisionPositioningSwitch.setCheckedWithoutListener(it)
            }, {})
        //获取数据上报登记号（utmiss）
        obstacleAvoidanceVM.getDataReportNum(onSuccess = {
            AutelStorageManager.getPlainStorage().setStringValue(StorageKey.PlainKey.DATA_REPORT_INPUT_NUMBER, it)
            binding.citInputNumberCn.updateRightText(it)
        }, {})
        //获取紧急停桨类型
        droneControlVM.getStopPropeller(onSuccess = {
            stopPropellerIndex = getStopPropellerIndex(it)
            binding.csvStopPropeller.setDefaultText(stopPropellerIndex)
        }, {})

        if (AppInfoManager.isSupportADSB()){
            AutelLog.i(TAG, "getADSBInstallState -> ")
            droneControlVM.getADSBInstallState(onSuccess = {
                AutelLog.i(TAG, "getADSBInstallState -> onSuccess result=$it")
                setAdsbVisible(it)
            },{
                AutelLog.i(TAG, "getADSBInstallState -> error=$it")
            })
        }

        if (AppInfoManager.isSupportNoflySwitch()) {
            //禁飞区开关
            binding.cisNoflyIsSupport.setCheckedWithoutListener(true)
            droneControlVM.getOpenNfz(onSuccess = {
                binding.cisNoflyIsSupport.setCheckedWithoutListener(it)
            }, {
                AutelLog.i(TAG, "getOpenNfz -> error=$it")
            })

        }

        if (AppInfoManager.isSupportGnssSwitch()){
            droneControlVM.getGpsFlightSwitch()
        }else{
            //如果是关闭，则需求校正为打开
            if (SharedParams._gnssSwitch.value != true){
                droneControlVM.switchGpsFlightSwitch({
                    isGnssSwitch = it
                    binding.csvCheckGnssMode.isVisible = it && gnssModeList.size > 1
                },{})
            }
        }
        AutelLog.i(TAG, "getSupportGPSMode -> start")
        droneControlVM.getSupportGPSMode(onSuccess = { list ->
            AutelLog.i(TAG, "getSupportGPSMode -> onSuccess result=$list")
            //如果支持的gnss系统为空，则隐藏GNSS定位
            binding.llcGnssSystem.isVisible = list.isNotEmpty()
            if (list.isNotEmpty()) {
                if (list.size == 1) {
                    gnssModeList.clear()
                    gnssModeList.addAll(list)
                    //如果是支持开关的，GNSS定位模式显示在详情中
                    if (AppInfoManager.isSupportGnssSwitch()){
                        binding.civGnssSwitch.setContentMsg(list[0].name)
                        binding.csvCheckGnssMode.isVisible = false
                    }else{
                        binding.civGnssSwitch.getRightText().text = list[0].name
                        binding.civGnssSwitch.setContentMsg("")
                        binding.csvCheckGnssMode.isVisible = false
                    }
                } else {
                    binding.civGnssSwitch.getRightText().text = ""
                    binding.civGnssSwitch.setContentMsg(getString(getGnssContentMsg()))
                    binding.csvCheckGnssMode.isVisible = isGnssSwitch
                    //初始化列表
                    initGnssListView(list)
                    AutelLog.i(TAG,"getKeyFcsSwitchGpsMode -> ")
                    settingAdvanceVm.getKeyFcsSwitchGpsMode(onSuccess = {
                        AutelLog.i(TAG,"getKeyFcsSwitchGpsMode -> onSuccess: $it")
                        gnssModeIndex = findGnssModeIndex(it)
                        binding.csvCheckGnssMode.setDefaultText(gnssModeIndex)

                    }, onError = {
                        AutelLog.i(TAG,"getKeyFcsSwitchGpsMode -> onError: $it")
                    })
                }
            }
        }, onError = {
            AutelLog.i(TAG, "getSupportGPSMode -> onError error=$it")
            binding.llcGnssSystem.isVisible = false
        })
    }

    /**
     * 设置
     */
    private fun initGnssListView(list: List<DroneGpsEnum>) {
        AutelLog.i(TAG,"initGnssListView -> gnssModeList=$gnssModeList")
        if (checkGnssModeList(list)) return
        gnssModeList.clear()
        gnssModeList.addAll(list)
        gnssModeStrList.clear()
        gnssModeList.forEach { gnssModeStrList.add(it.name)}
        binding.csvCheckGnssMode.dataList = gnssModeStrList
        binding.csvCheckGnssMode.setSpinnerViewListener {index ->
            if (!isDroneConnect()) {
                showToast(R.string.common_text_aircraft_disconnect)
                binding.csvCheckGnssMode.setDefaultText(gnssModeIndex)
                return@setSpinnerViewListener
            }

            //相等的话，就不设置了
            if (gnssModeIndex == index) return@setSpinnerViewListener

            context?.let { context ->
                isChangeGnssMode = false
                CommonTwoButtonDialog(context)
                    .apply {
                        setMessage(getString(R.string.common_text_change_gnss_tips))
                        setRightBtnStr(getString(R.string.common_text_exchange))
                        setRightBtnListener {
                            isChangeGnssMode = true
                            // GPS模式 AUTO
                            AutelLog.i(TAG,"initGnssListView -> setKeyFcsSwitchGpsMode index=$index mode=${gnssModeList[index]}")
                            settingAdvanceVm.setKeyFcsSwitchGpsMode(gnssModeList[index], onSuccess = {
                                AutelLog.i(TAG,"initGnssListView -> setKeyFcsSwitchGpsMode success")
                                gnssModeIndex = index
                            }, onError = {
                                AutelLog.i(TAG,"initGnssListView -> setKeyFcsSwitchGpsMode error=$it")
                                showToast(R.string.common_text_set_failed)
                                binding.csvCheckGnssMode.setDefaultText(gnssModeIndex)
                            })
                        }

                        setOnDismissListener {
                            if (!isChangeGnssMode) binding.csvCheckGnssMode.setDefaultText(gnssModeIndex)
                        }
                        show()
                    }
            }
        }
    }

    /**
     * 检查列表是否相等
     * true 表示相等，false表示不一样
     */
    private fun checkGnssModeList(list: List<DroneGpsEnum>):Boolean{
        if (gnssModeList.isEmpty()) return false
        if (gnssModeList.size != list.size) return false
        for (x in gnssModeList.indices){
            if (gnssModeList[x] != list[x]) return false
        }
        return true
    }

    private var isCloseGnss = false

    override fun addListen() {
        //启用视觉定位开关
        binding.viewVisionPositioningSwitch.setOnSwitchChangeListener { checked ->
            obstacleAvoidanceVM.setLocationStatus(
                checked,
                onSuccess = {
                },
                onError = {
                    showToast(R.string.common_text_set_failed)
                    binding.viewVisionPositioningSwitch.setCheckedWithoutListener(!checked)
                }
            )
        }

        binding.civGnssSwitch.setOnSwitchChangeListener {
            if (!DeviceUtils.isSingleControlDroneConnected()) {
                showToast(R.string.common_text_aircraft_disconnect)
                binding.civGnssSwitch.setCheckedWithoutListener(isGnssSwitch)
                return@setOnSwitchChangeListener
            }

            context?.let { context ->
                if (it) {
                    setGnssSwitch(it)
                } else {
                    isCloseGnss = false
                    CommonTwoButtonDialog(context)
                        .apply {
                            setLeftBtnStr(getString(R.string.common_text_cancel))
                            setRightBtnStr(getString(R.string.common_text_close))
                            setTitle(getString(R.string.common_text_off_GNSS))
                            setMessage(getString(getCloseGnssTips()))
                            if (AppInfoManager.isSupportGnssSystemSwitch() && AppInfoManager.isSupportGnssTipsDetail()) {
                                setMessage2(strId = R.string.common_text_gnss_location_desc_a)
                            }
                            setRightBtnListener {
                                isCloseGnss = true
                                setGnssSwitch(it)
                            }
                            setOnDismissListener {
                                if (!isCloseGnss) binding.civGnssSwitch.setCheckedWithoutListener(isGnssSwitch)
                            }
                            show()
                        }
                }
            }
        }

        //抗干扰
        binding.antiInterferenceContainer.isVisible = AppInfoManager.isSupportAntiInterference()
        if (AppInfoManager.isSupportAntiInterference()) {
            //抗干扰 只有安防功能，才能使用
            binding.circAntiInterference.isVisible = true
            binding.circAntiInterference.addRadioButton(resources.getStringArray(R.array.common_text_anti_interference_set))
            binding.circAntiInterference.setOnSwitchChangeListener {
                val temMode = if (it == 1) { FccCeModeEnum.CE } else { FccCeModeEnum.FCC }
                if (it == 0) {
                    CommonDialogManager.showDisclaimerDialog(object : DisclaimerListener {
                        override fun onCallBack(isCommit: Boolean) {
                            AutelLog.i(TAG, "DisclaimerDialog -> onCallBack isCommit=$isCommit")
                            if (isCommit) {
                                settingAdvanceVm.setKeyALinkFccCeMode(temMode, onSuccess = {
                                    circAntiIndex = it
                                }, onError = {
                                    showToast(R.string.common_text_set_failed)
                                    binding.circAntiInterference.setRadioGroupCheck(circAntiIndex)
                                })
                            } else {
                                binding.circAntiInterference.setRadioGroupCheck(circAntiIndex)
                            }
                        }
                    }, true)
                } else {
                    settingAdvanceVm.setKeyALinkFccCeMode(temMode, onSuccess = {
                        circAntiIndex = it
                    }, onError = {
                        showToast(R.string.common_text_set_failed)
                        binding.circAntiInterference.setRadioGroupCheck(circAntiIndex)
                    })
                }
            }
        }

        binding.civAiRecognition.setOnClickListener {
            switchVM.addFragment(
                SettingChooseTargetFragment(),
                resources.getString(R.string.common_text_choose_ai_target),
                true
            )
        }

        //指北针
        val enableCompassNorth = AutelStorageManager.getPlainStorage().getBooleanValue(StorageKey.PlainKey.KEY_NORTH_OPEN, false)
        binding.civCompassNorth.setCheckedWithoutListener(enableCompassNorth)
        binding.civCompassNorth.setOnSwitchChangeListener {
//            getPlainStorage().setBooleanValue(StorageKey.PlainKey.KEY_NORTH_OPEN, it)
//            LiveDataBus.of(NorthCompassEvent::class.java).changeNorthCompass().post(it)A
        }


        //数据上报只支持中国 防务和标准版本
        binding.civDataReportCn.isVisible = AppInfoManager.isSupportDataReportCn()
        binding.citInputNumberCn.isVisible = AppInfoManager.isSupportDataReportCn()

        if (AppInfoManager.isSupportDataReportCn()) {
            //民航局报送开关
            val defaultDataReportCn = AutelStorageManager.getPlainStorage().getBooleanValue(StorageKey.PlainKey.DATA_REPORT_CN, true)
            binding.civDataReportCn.setCheckedWithoutListener(defaultDataReportCn)
            FlightDynamicDataManager.getInstance().setDataReportSwitch(defaultDataReportCn)
            isDataReportSwitch = defaultDataReportCn
            binding.civDataReportCn.setOnSwitchChangeListener {
                if (it) {
                    isDataReportSwitch = it
                    FlightDynamicDataManager.getInstance().setDataReportSwitch(it)
                    AutelStorageManager.getPlainStorage().setBooleanValue(StorageKey.PlainKey.DATA_REPORT_CN, it)
                } else {
                    context?.let { context ->
                        CommonTwoButtonDialog(context)
                            .apply {
                                setTitle(getString(R.string.common_text_close_data_report_title))
                                setMessage(getString(R.string.common_text_close_data_report_tips))
                                setRightBtnListener {
                                    FlightDynamicDataManager.getInstance().setDataReportSwitch(it)
                                    AutelStorageManager.getPlainStorage().setBooleanValue(StorageKey.PlainKey.DATA_REPORT_CN, it)
                                    isDataReportSwitch = it
                                }
                                setOnDismissListener {
                                    binding.civDataReportCn.setCheckedWithoutListener(isDataReportSwitch)
                                }
                                show()
                            }
                    }
                }

            }
            //实名登记号
            val defaultInputNumberCn = AutelStorageManager.getPlainStorage()
                .getStringValue(StorageKey.PlainKey.DATA_REPORT_INPUT_NUMBER, "")
            val fixInputNumberCn = if (defaultInputNumberCn.isNullOrEmpty()) getString(R.string.common_text_unfilled) else defaultInputNumberCn
            binding.citInputNumberCn.updateRightText(fixInputNumberCn)
            binding.citInputNumberCn.setOnItemClickListener {
                val dialog = CommonInputDialog(requireContext()).apply {
                    setTitle(getString(R.string.common_text_input_number))
                    setMessageHint(getString(R.string.common_text_input_number_hit))
                    setLeftBtnStr(resources.getString(R.string.common_text_cancel))
                    setRightBtnStr(resources.getString(R.string.common_text_save))
                    setAutoDismiss(false)
                    setLeftBtnListener { dismiss() }
                    setRightBtnListener {
                        if (it.length == DATA_REPORT_NUMBER_LENGTH_LIMIT && it.startsWith(DATA_REPORT_NUMBER_HEAD)) {
                            obstacleAvoidanceVM.setDataReportNum(it, {
                                AutelStorageManager.getPlainStorage().setStringValue(StorageKey.PlainKey.DATA_REPORT_INPUT_NUMBER, it)
                                binding.citInputNumberCn.updateRightText(it)
                                dismiss()
                            }, {
                                showToast(R.string.common_text_set_failed)
                            })
                        } else {
                            showToast(R.string.common_text_input_number_error_tips)
                        }
                    }
                }
                if (!defaultInputNumberCn.isNullOrEmpty()) {
                    dialog.setMessage(defaultInputNumberCn)
                }
                dialog.show()
            }
        }
        //gnss切换开关
        SharedParams.gnssSwitch.observe(this) {
            isGnssSwitch = it
            binding.civGnssSwitch.setCheckedWithoutListener(isGnssSwitch)
            binding.csvCheckGnssMode.isVisible = it && gnssModeList.size > 1
        }

        initStopPropeller()
        initNoFlySupport()
        binding.viewAdsbSwitch.setMarkVisibility(View.VISIBLE)
        binding.viewAdsbSwitch.setMarkOffset(-24)//调整
        binding.viewAdsbSwitch.setOnMarkClickListener {
            AutelLog.i(TAG, "viewAdsbSwitch -> setMarkVisibility")
            if (adsbExplanationPopWindow == null) {
                adsbExplanationPopWindow = AdsbExplanationPopWindow(requireContext())
                adsbExplanationPopWindow?.getTriangleView()?.rotation = 180f
            }
            AutelLog.i(TAG, "viewAdsbSwitch -> showAsDropDown")
            adsbExplanationPopWindow?.showAsDropDown(
                binding.viewAdsbSwitch.getMark(),
                -requireContext().resources.getDimensionPixelSize(R.dimen.common_173dp),
                -requireContext().resources.getDimensionPixelSize(R.dimen.common_250dp)
            )
        }
        binding.viewAdsbSwitch.setOnSwitchChangeListener {
            AutelLog.i(TAG, "viewAdsbSwitch -> it=$it")
            DeviceManager.getDeviceManager().getConfigManager().openAdsb(it)
            setAdsbOpen(it)
        }
        binding.viewAdsbStrongSwitch.setImage(R.drawable.common_icon_adsb_strong)
        binding.viewAdsbStrongMiddleSwitch.setImage(R.drawable.common_icon_adsb_strong_middle)
        binding.viewAdsbAllSwitch.setImage(R.drawable.common_icon_adsb_all)
        binding.viewAdsbStrongSwitch.setOnClickListener {
            AutelLog.i(TAG, "viewAdsbStrongSwitch")
            DeviceManager.getDeviceManager().getConfigManager().setWarnShowLevel(WarnShowLevelEnum.Strong)
            selectAdsbWarnLevel(WarnShowLevelEnum.Strong)
        }
        binding.viewAdsbStrongMiddleSwitch.setOnClickListener {
            AutelLog.i(TAG, "viewAdsbStrongMiddleSwitch")
            DeviceManager.getDeviceManager().getConfigManager().setWarnShowLevel(WarnShowLevelEnum.StrongAndMiddle)
            selectAdsbWarnLevel(WarnShowLevelEnum.StrongAndMiddle)
        }
        binding.viewAdsbAllSwitch.setOnClickListener {
            AutelLog.i(TAG, "viewAdsbAllSwitch")
            DeviceManager.getDeviceManager().getConfigManager().setWarnShowLevel(WarnShowLevelEnum.All)
            selectAdsbWarnLevel(WarnShowLevelEnum.All)
        }

        //警务版本是支持GNSS开关的，因此显示开关，同时隐藏右侧文本
        binding.civGnssSwitch.getSwitchBtn().isVisible = AppInfoManager.isSupportGnssSwitch()
        binding.civGnssSwitch.getRightText().isVisible = !AppInfoManager.isSupportGnssSwitch()
    }

    private fun findGnssModeIndex(mode:DroneGpsEnum):Int{
       for (x in gnssModeList.indices){
           if (mode == gnssModeList[x]) return x
       }
        //这里如果不在能力集范围内，则自动校正
        AutelLog.i(TAG,"fixGnssMode -> setKeyFcsSwitchGpsMode ${gnssModeList[0]}")
        settingAdvanceVm.setKeyFcsSwitchGpsMode(gnssModeList[0],{
            AutelLog.i(TAG,"fixGnssMode -> setKeyFcsSwitchGpsMode success")
        },{
            AutelLog.i(TAG,"fixGnssMode -> setKeyFcsSwitchGpsMode error")
        })
       return 0
    }


    /**
     * 选择ads-b告警显示级别
     * */
    private fun selectAdsbWarnLevel(level: WarnShowLevelEnum) {
        binding.viewAdsbStrongSwitch.setChecked(level == WarnShowLevelEnum.Strong)
        binding.viewAdsbStrongMiddleSwitch.setChecked(level == WarnShowLevelEnum.StrongAndMiddle)
        binding.viewAdsbAllSwitch.setChecked(level == WarnShowLevelEnum.All)
    }

    /**
     * 设置ads-b(in)开关
     * */
    private fun setAdsbOpen(isOpen: Boolean) {
        binding.showWarnLevel.isVisible = isOpen
        binding.viewAdsbSwitch.setCheckedWithoutListener(isOpen)
        if (isOpen) {
            binding.viewAdsbSwitch.background = resources.getDrawable(R.drawable.common_item_bg_only_top)
        } else {
            binding.viewAdsbSwitch.background = resources.getDrawable(R.drawable.common_item_bg_all)
        }
        val warnShowLevel = DeviceManager.getDeviceManager().getConfigManager().getWarnShowLevel()
        AutelLog.i(TAG, "setAdsbOpen -> warnShowLevel=$warnShowLevel")
        binding.viewAdsbStrongSwitch.setChecked(warnShowLevel == WarnShowLevelEnum.Strong)
        binding.viewAdsbStrongMiddleSwitch.setChecked(warnShowLevel == WarnShowLevelEnum.StrongAndMiddle)
        binding.viewAdsbAllSwitch.setChecked(warnShowLevel == WarnShowLevelEnum.All)
    }

    /**
     * 初始化禁飞区默认开关
     */
    private fun initNoFlySupport() {
        //默认是支持的
//        val isSupportNofly = AutelStorageManager.getPlainStorage().getBooleanValue(StorageKey.PlainKey.KEY_IS_SUPPORT_NO_FLY, true)
        binding.cisNoflyIsSupport.isVisible = AppInfoManager.isSupportNoflySwitch()
//        binding.cisNoflyIsSupport.setCheckedWithoutListener(isSupportNofly)
        binding.cisNoflyIsSupport.setOnSwitchChangeListener {
            AutelLog.i(TAG, "cisNoflyIsSupport -> it=$it")
            if (!it) {
                CommonDialogManager.showDisclaimerDialog(object : DisclaimerListener {
                    override fun onCallBack(isCommit: Boolean) {
                        AutelLog.i(TAG, "DisclaimerDialog -> onCallBack isCommit=$isCommit")
                        if (isCommit) {
                            AutelStorageManager.getPlainStorage().setBooleanValue(StorageKey.PlainKey.KEY_IS_SUPPORT_NO_FLY, it)
                            droneControlVM.isOpenNfz(it, {}, {})
                        } else {
                            binding.cisNoflyIsSupport.setCheckedWithoutListener(!it)
                        }
                    }
                })
            } else {
                AutelStorageManager.getPlainStorage().setBooleanValue(StorageKey.PlainKey.KEY_IS_SUPPORT_NO_FLY, it)
                droneControlVM.isOpenNfz(it, {}, {})
            }
        }
    }

    /**
     * 初始化紧急停桨
     */
    private fun initStopPropeller() {
        stopPropellerList.clear()
        stopPropellerList.add(StopPropellerEnum.OFF)
        stopPropellerList.add(StopPropellerEnum.ON)
        //stopPropellerList.add(StopPropellerEnum.ONLY_FAULT)

        val stopPropellerTags = ArrayList<String>()
        for (x in stopPropellerList) {
            stopPropellerTags.add(getString(x.id))
        }
        binding.csvStopPropeller.dataList = stopPropellerTags
        binding.csvStopPropeller.setDefaultText(0)
        binding.csvStopPropeller.setSpinnerViewListener {
            if (it >= stopPropellerList.size) return@setSpinnerViewListener
            //打开时属于危险操作，需要二次弹框
            if (stopPropellerList[it].id == R.string.common_text_light_open) {
                context?.let { context ->
                    isOpenStopPropeller = true
                    CommonTwoButtonDialog(context)
                        .apply {
                            setTitle(getString(R.string.common_text_tips_title))
                            setMessage(getString(R.string.common_text_stop_propeller_tips))
                            setRightBtnListener {
                                isOpenStopPropeller = false
                                setStopPropellerState(it)
                            }
                            setOnDismissListener {
                                if (isOpenStopPropeller) {
                                    binding.csvStopPropeller.setDefaultText(stopPropellerIndex)
                                }
                            }
                            show()
                        }
                }

            } else {
                setStopPropellerState(it)
            }
        }
        binding.llStopPropeller.isVisible = AppInfoManager.isSupportStopPropeller()
    }

    /**
     * 设置紧急停桨
     */
    private fun setStopPropellerState(index: Int) {
        droneControlVM.setStopPropeller(stopPropellerList[index].value, onSuccess = {
            stopPropellerIndex = index
        }, {
            showToast(R.string.common_text_set_failed)
            binding.csvStopPropeller.setDefaultText(stopPropellerIndex)
        })
    }

    /**
     * 获取默认的index
     */
    private fun getStopPropellerIndex(state: FcsTmpDisarmAirEnum): Int {
        for (x in stopPropellerList.indices) {
            if (stopPropellerList[x].value == state) return x
        }
        return 0
    }

    /**
     * slm强度
     * 0.7以上高，0.4到0.7中，0.4以下低
     */
    private fun getCloseGnssTips(): Int {
        //slm强度

        val slm = DeviceUtils.singleControlDrone()?.getDeviceStateData()?.flightControlData?.slamConfidence ?: 0.0
        AutelLog.i(SettingTag, "getCloseGnssTips -> slm=$slm")
        return if (slm >= 0.7) {
            R.string.common_text_turn_off_slm_strong
        } else if (slm >= 0.4 && slm < 0.7) {
            R.string.common_text_turn_off_slm_middle
        } else {
            R.string.common_text_turn_off_slm_weak
        }
    }

    /**
     * 开关gnss开关
     */
    private fun setGnssSwitch(open: Boolean) {
        droneControlVM.switchGpsFlightSwitch(onSuccess = {
            isGnssSwitch = open
            binding.csvCheckGnssMode.isVisible = open && gnssModeList.size > 1
        }, onError = {
            binding.civGnssSwitch.setCheckedWithoutListener(isGnssSwitch)
            showToast(R.string.common_text_set_failed)
        })
        // 打开和关闭GNSS语言播报
        if (open) {
            GoogleTextToSpeechManager.instance().speak(getString(R.string.common_text_open_gnss), true)
        } else {
            GoogleTextToSpeechManager.instance().speak(getString(R.string.common_text_close_gnss), true)
        }
    }

    /**
     * 飞机是否连接
     */
    private fun isDroneConnect(): Boolean {
        return DeviceManager.getDeviceManager().getFirstDroneDevice()?.isConnected() ?: false
    }

    /**
     * ADS-B项是否显示
     */
    private fun setAdsbVisible(enable: Boolean) {
        binding.viewAdsbSwitch.isVisible = enable
        if (enable) {
            val isOpenAdsb = DeviceManager.getDeviceManager().getConfigManager().adsbOpenStatus()
            AutelLog.i(TAG, "setAdsbVisible -> enable=$enable isOpenAdsb=$isOpenAdsb")
            setAdsbOpen(isOpenAdsb)
        } else {
            binding.showWarnLevel.isVisible = false
        }
    }


    /**
     * 获取GNSS提示信息
     */
    private fun getGnssContentMsg():Int{
        //GNSS定位 标准版本 显示才能带公司信息
        var gnssContentMsg =
            if (AppInfoManager.isSupportGnssTipsDetail()) R.string.common_text_gnss_location_desc_a else R.string.common_text_gnss_location_desc
        if (AppInfoManager.isSupportGnssSystemSwitch()) {
            // 切换定位系统后需重启飞机。
            gnssContentMsg = R.string.common_text_gnss_tips
        }
        return gnssContentMsg
    }
}