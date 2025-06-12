package com.autel.setting.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.OnClickListener
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.autel.common.base.BaseAircraftFragment
import com.autel.common.constant.AppTagConst.SettingTag
import com.autel.common.constant.SharedParams
import com.autel.common.feature.location.CountryManager
import com.autel.common.feature.phone.AutelPhoneLocationManager
import com.autel.common.listener.DisclaimerListener
import com.autel.common.manager.AppInfoManager
import com.autel.common.manager.AutelStorageManager
import com.autel.common.manager.CommonDialogManager
import com.autel.common.manager.LawHeightDelegateManager
import com.autel.common.manager.MiddlewareManager
import com.autel.common.manager.StorageKey
import com.autel.common.manager.unit.DistanceSpeedUnitEnum
import com.autel.common.manager.unit.UnitManager
import com.autel.common.sdk.ModelXDroneConst
import com.autel.common.utils.DeviceUtils
import com.autel.common.utils.TransformUtils
import com.autel.common.widget.CommonItemEditText
import com.autel.common.widget.ExplainPopupWindow
import com.autel.common.widget.GearLevelSwitchDialog
import com.autel.common.widget.dialog.CommonTwoButtonDialog
import com.autel.common.widget.toast.AutelToast
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.bean.HomeLocation
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.enums.DroneLostActionEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.enums.DroneWorkModeEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.enums.GearLevelEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.enums.LocationTypeEnum
import com.autel.log.AutelLog
import com.autel.map.bean.AutelLatLng
import com.autel.setting.R
import com.autel.setting.bean.GearModeBean
import com.autel.setting.business.SettingFlyControllerVM
import com.autel.setting.databinding.SettingFlyControllerFragmentBinding
import com.autel.setting.state.SwitchStateVM
import com.autel.utils.ScreenUtils

/**
 * @Author create by LJ
 * @Date 2022/9/1 10:16
 * 飞控参数设置
 */
class SettingFlyControllerFragment : BaseAircraftFragment(), OnClickListener {
    companion object {
        const val TAG = "SettingFlyControllerFragment"
    }

    private val switchVM: SwitchStateVM by activityViewModels()
    private val settingFlyControllerVM: SettingFlyControllerVM by viewModels()
    private lateinit var binding: SettingFlyControllerFragmentBinding
    private val gearModeList = ArrayList<GearModeBean>()//档位
    private var isOpenNecessity = false//是否开启紧急避险
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = SettingFlyControllerFragmentBinding.inflate(LayoutInflater.from(context))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        refreshInitUnit()
    }

    private fun refreshInitUnit() {
        if (UnitManager.getSelectSpeedUnit() == DistanceSpeedUnitEnum.Imperial) {
            val goHomeHeightMin =
                TransformUtils.meter2feet(ModelXDroneConst.DRONE_GO_HOME_HEIGHT_MIN.toDouble(), 0)
                    .toInt()
            val goHomeHeightMax =
                TransformUtils.meter2feet(ModelXDroneConst.DRONE_GO_HOME_HEIGHT_MAX.toDouble(), 0)
                    .toInt()

            binding.cetGoHomeHeight.setMinValue(goHomeHeightMin)
            binding.cetGoHomeHeight.setMaxValue(goHomeHeightMax)
            binding.cetGoHomeHeight.setTitle(
                resources.getString(
                    R.string.common_text_go_home_height,
                    "$goHomeHeightMin",
                    "${goHomeHeightMax}ft"
                )
            )

            val limitHeightMin =
                TransformUtils.meter2feet(ModelXDroneConst.DRONE_LIMIT_HEIGHT_MIN.toDouble(), 0)
                    .toInt()
            val limitHeightMax =
                TransformUtils.meter2feet(ModelXDroneConst.DRONE_LIMIT_HEIGHT_MAX.toDouble(), 0)
                    .toInt()

            binding.cetLimitHeight.setMinValue(limitHeightMin)
            binding.cetLimitHeight.setMaxValue(limitHeightMax)
            binding.cetLimitHeight.setTitle(
                resources.getString(
                    R.string.common_text_limit_height,
                    "$limitHeightMin",
                    "${limitHeightMax}ft"
                )
            )

            val limitDistanceMin =
                TransformUtils.meter2feet(ModelXDroneConst.DRONE_LIMIT_DISTANCE_MIN.toDouble(), 0)
                    .toInt()
            val limitDistanceMax =
                TransformUtils.meter2feet(ModelXDroneConst.DRONE_LIMIT_DISTANCE_MAX.toDouble(), 0)
                    .toInt()

            binding.cetLimitDistance.setMinValue(limitDistanceMin)
            binding.cetLimitDistance.setMaxValue(limitDistanceMax)
            binding.cetLimitDistance.setTitle(
                resources.getString(
                    R.string.common_text_limit_distance,
                    "$limitDistanceMin",
                    "${limitDistanceMax}ft"
                )
            )


        } else {
            binding.cetGoHomeHeight.setMinValue(ModelXDroneConst.DRONE_GO_HOME_HEIGHT_MIN)
            binding.cetGoHomeHeight.setMaxValue(ModelXDroneConst.DRONE_GO_HOME_HEIGHT_MAX)
            binding.cetGoHomeHeight.setTitle(
                resources.getString(
                    R.string.common_text_go_home_height,
                    "${ModelXDroneConst.DRONE_GO_HOME_HEIGHT_MIN}",
                    "${ModelXDroneConst.DRONE_GO_HOME_HEIGHT_MAX}m"
                )
            )

            binding.cetLimitHeight.setMinValue(ModelXDroneConst.DRONE_LIMIT_HEIGHT_MIN)
            binding.cetLimitHeight.setMaxValue(ModelXDroneConst.DRONE_LIMIT_HEIGHT_MAX)
            binding.cetLimitHeight.setTitle(
                resources.getString(
                    R.string.common_text_limit_height,
                    "${ModelXDroneConst.DRONE_LIMIT_HEIGHT_MIN}",
                    "${ModelXDroneConst.DRONE_LIMIT_HEIGHT_MAX}m"
                )
            )

            binding.cetLimitDistance.setMinValue(ModelXDroneConst.DRONE_LIMIT_DISTANCE_MIN)
            binding.cetLimitDistance.setMaxValue(ModelXDroneConst.DRONE_LIMIT_DISTANCE_MAX)
            binding.cetLimitDistance.setTitle(
                resources.getString(
                    R.string.common_text_limit_distance,
                    "${ModelXDroneConst.DRONE_LIMIT_DISTANCE_MIN}",
                    "${ModelXDroneConst.DRONE_LIMIT_DISTANCE_MAX}m"
                )
            )
        }
    }

    override fun addListen() {
        settingFlyControllerVM.backHeightLD.observe(this, Observer {
            if (UnitManager.getSelectSpeedUnit() == DistanceSpeedUnitEnum.Imperial) {
                binding.cetGoHomeHeight.updateContent(
                    TransformUtils.meter2feet(it.toDouble(), 0).toInt()
                )
            } else {
                binding.cetGoHomeHeight.updateContent(it.toInt())
            }
        })

        settingFlyControllerVM.maxHeightLD.observe(this, Observer {
            if (AppInfoManager.isSupportLimitHeight()) {//标准产品
                binding.cetLimitHeight.visibility = VISIBLE
                updateLimitHeight(it)
            } else {
                if (it >= ModelXDroneConst.DRONE_LIMIT_HEIGHT_VALUE_MAX) {
                    binding.tvDistanceHeightOpen.setCheckedWithoutListener(false)
                    binding.cetLimitHeight.visibility = GONE
                } else {
                    binding.tvDistanceHeightOpen.setCheckedWithoutListener(true)
                    binding.cetLimitHeight.visibility = VISIBLE
                    updateLimitHeight(it)
                }
            }
        })

        settingFlyControllerVM.gearLevelLD.observe(this, Observer {
            updateGearLevel(it)
        })

        settingFlyControllerVM.maxRadiusLD.observe(this) {
            if (it >= ModelXDroneConst.MAX_DISTANCE_LIMIT) {
                binding.tvDistanceLimitOpen.setCheckedWithoutListener(false)
                binding.cetLimitDistance.visibility = GONE
            } else {
                binding.tvDistanceLimitOpen.setCheckedWithoutListener(true)
                binding.cetLimitDistance.visibility = VISIBLE
                updateLimitDistance(it)
            }
        }

        settingFlyControllerVM.rcLostAction.observe(this) {
            updateLostControlSpinnerText(it)
        }
    }

    /**
     * 更新限高
     */
    private fun updateLimitHeight(limitHeight: Float) {
        if (UnitManager.getSelectSpeedUnit() == DistanceSpeedUnitEnum.Imperial) {
            binding.cetLimitHeight.updateContent(
                TransformUtils.meter2feet(
                    limitHeight.toDouble(),
                    0
                ).toInt()
            )
        } else {
            binding.cetLimitHeight.updateContent(limitHeight.toInt())
        }
    }

    /**
     * 更新限远
     */
    private fun updateLimitDistance(limitDistance: Float) {
        AutelStorageManager.getPlainStorage()
            .setFloatValue(StorageKey.PlainKey.KEY_LAST_DISTANCE_LIMIT_VALUE, limitDistance)
        if (UnitManager.getSelectSpeedUnit() == DistanceSpeedUnitEnum.Imperial) {
            binding.cetLimitDistance.updateContent(
                TransformUtils.meter2feet(
                    limitDistance.toDouble(),
                    0
                ).toInt()
            )
        } else {
            binding.cetLimitDistance.updateContent(limitDistance.toInt())
        }
    }

    /**
     * 初始化UI操作事件
     */
    private fun initView() {
        if (AppInfoManager.isSupportLimitHeight()) {
            binding.cetLimitHeight.visibility = VISIBLE
            binding.tvDistanceHeightOpen.visibility = GONE
        }
        binding.cetGoHomeHeight.setOnEditTextChangedListener(object :
            CommonItemEditText.CommonEditChangeListener {
            override fun onEditTextChanged(value: String) {
                if (!DeviceUtils.isSingleControlDroneConnected()) {
                    showToast(R.string.common_text_aircraft_disconnect)
                    return
                }
                val valueFloat = value.toFloatOrNull()
                val goHomeHeight = if (valueFloat == null) {
                    20f
                } else {
                    getDistanceFromInput(valueFloat)
                }
                if (goHomeHeight > (settingFlyControllerVM.maxHeightLD.value ?: 30f)) {
                    AutelToast.normalToast(
                        requireContext(),
                        R.string.common_text_back_height_need_small_max_height
                    )
                    binding.cetGoHomeHeight.post {
                        val resetValue = settingFlyControllerVM.backHeightLD.value
                            ?: ModelXDroneConst.DRONE_LIMIT_HEIGHT_MIN.toFloat()
                        binding.cetGoHomeHeight.updateContent(getDistanceToShow(resetValue).toInt())
                    }
                    return
                }
                settingFlyControllerVM.setMissionManagerBackHeight(goHomeHeight,
                    onSuccess = {

                    },
                    onError = {
                        showToast(R.string.common_text_set_failed)
                    })
            }
        })
        binding.cetLimitHeight.setOnEditTextChangedListener(object :
            CommonItemEditText.CommonEditChangeListener {
            override fun onEditTextChanged(value: String) {
                if (!DeviceUtils.isSingleControlDroneConnected()) {
                    showToast(R.string.common_text_aircraft_disconnect)
                    return
                }
                val valueFloat = value.toFloatOrNull()
                val maxHeight = if (valueFloat == null) {
                    LawHeightDelegateManager.getLowHeight(CountryManager.currentCountry)
                } else {
                    getDistanceFromInput(valueFloat)
                }
                if (maxHeight < (settingFlyControllerVM.backHeightLD.value
                        ?: ModelXDroneConst.DRONE_LIMIT_HEIGHT_MIN.toFloat())
                ) {
                    AutelToast.normalToast(
                        requireContext(),
                        R.string.common_text_max_height_need_max_back_height
                    )
                    binding.cetLimitHeight.post {
                        val resetValue = settingFlyControllerVM.maxHeightLD.value
                            ?: ModelXDroneConst.DRONE_LIMIT_HEIGHT_MIN.toFloat()
                        binding.cetLimitHeight.updateContent(getDistanceToShow(resetValue).toInt())
                    }
                    return
                }
                //超过120米弹框提示
                if (maxHeight > LawHeightDelegateManager.getLowHeight(CountryManager.currentCountry)) {
                    CommonDialogManager.showLimitHeightDialog(object : DisclaimerListener {
                        override fun onCallBack(isCommit: Boolean) {
                            if (isCommit) {
                                setLimitHeight(maxHeight)
                            } else {
                                settingFlyControllerVM.maxHeightLD.value?.let { updateLimitHeight(it) }
                            }
                        }

                    })
                } else {
                    setLimitHeight(maxHeight)
                }
            }
        })
        binding.cetLimitDistance.setOnEditTextChangedListener(object :
            CommonItemEditText.CommonEditChangeListener {
            override fun onEditTextChanged(value: String) {
                var limitDistance = value.toFloat()
                if (UnitManager.getSelectSpeedUnit() == DistanceSpeedUnitEnum.Imperial) {
                    limitDistance = TransformUtils.feet2meter(limitDistance.toDouble(), 6).toFloat()
                }
                settingFlyControllerVM.setFlightParamsMaxRadius(limitDistance,
                    onSuccess = {
                        //指令成功处理
                        AutelStorageManager.getPlainStorage().setFloatValue(
                            StorageKey.PlainKey.KEY_LAST_DISTANCE_LIMIT_VALUE,
                            limitDistance
                        )
                    },
                    onError = {
                        showToast(R.string.common_text_set_failed)
                        settingFlyControllerVM.maxRadiusLD.value?.let { updateLimitDistance(it) }
                    })
            }
        })
        binding.tvDistanceHeightOpen.setOnSwitchChangeListener {
            ScreenUtils.hideSoftInput(binding.tvDistanceLimitOpen)
            if (it) {
                binding.cetLimitHeight.visibility = VISIBLE
                var height = AutelStorageManager.getPlainStorage()
                    .getFloatValue(
                        StorageKey.PlainKey.KEY_LAST_HEIGHT_LIMIT_VALUE,
                        LawHeightDelegateManager.getLowHeight(CountryManager.currentCountry)
                    )
                val returnHeight = settingFlyControllerVM.backHeightLD.value ?: 0.0f
                AutelLog.i(TAG, "tvDistanceHeightOpen -> returnHeight$returnHeight height=$height")
                //兼容校正原来缓存的错误值
                if (height > ModelXDroneConst.DRONE_GO_HOME_HEIGHT_MAX){
                    height = ModelXDroneConst.DRONE_GO_HOME_HEIGHT_MAX.toFloat()
                    AutelStorageManager.getPlainStorage()
                        .setFloatValue(
                            StorageKey.PlainKey.KEY_LAST_HEIGHT_LIMIT_VALUE,
                            ModelXDroneConst.DRONE_GO_HOME_HEIGHT_MAX.toFloat())
                }
                var fixHeight = height
                if (returnHeight > height) {
                    fixHeight = returnHeight
                    showToast(R.string.common_text_return_fix_limit)
                }
                settingFlyControllerVM.setFlightParamsMaxHeight(
                    fixHeight,
                    {},
                    onError = {
                        binding.tvDistanceHeightOpen.setCheckedWithoutListener(false)
                        binding.cetLimitHeight.visibility = GONE
                        showToast(R.string.common_text_set_failed)
                        settingFlyControllerVM.maxHeightLD.value?.let { updateLimitHeight(it) }
                    })
            } else {
                CommonDialogManager.showOffLimitHeightDialog(object : DisclaimerListener {
                    override fun onCallBack(isCommit: Boolean) {
                        AutelLog.i(TAG, "DisclaimerDialog -> onCallBack isCommit=$isCommit")
                        if (isCommit) {
                            binding.cetLimitHeight.visibility = GONE
                            settingFlyControllerVM.setFlightParamsMaxHeight(
                                ModelXDroneConst.DRONE_LIMIT_HEIGHT_VALUE_MAX.toFloat(),
                                {},
                                onError = {
                                    binding.tvDistanceHeightOpen.setCheckedWithoutListener(true)
                                    binding.cetLimitHeight.visibility = VISIBLE
                                    showToast(R.string.common_text_set_failed)
                                    settingFlyControllerVM.maxHeightLD.value?.let {
                                        updateLimitHeight(
                                            it
                                        )
                                    }
                                })
                        } else {
                            binding.tvDistanceHeightOpen.setCheckedWithoutListener(true)
                            binding.cetLimitHeight.visibility = VISIBLE
                            settingFlyControllerVM.maxHeightLD.value?.let { updateLimitHeight(it) }
                        }
                    }
                })
            }
        }
        //是否开启距离限制
        binding.tvDistanceLimitOpen.setOnSwitchChangeListener {
            ScreenUtils.hideSoftInput(binding.tvDistanceLimitOpen)
            if (it) {
                binding.cetLimitDistance.visibility = VISIBLE
                val height = AutelStorageManager.getPlainStorage()
                    .getFloatValue(
                        StorageKey.PlainKey.KEY_LAST_DISTANCE_LIMIT_VALUE,
                        ModelXDroneConst.DEFAULT_DISTANCE_LIMIT
                    )
                settingFlyControllerVM.setFlightParamsMaxRadius(
                    height,
                    onSuccess = {
                        //指令成功处理
                        updateLimitDistance(height)
                    },
                    onError = {
                        binding.tvDistanceLimitOpen.setCheckedWithoutListener(false)
                        binding.cetLimitDistance.visibility = GONE
                        showToast(R.string.common_text_set_failed)
                        settingFlyControllerVM.maxRadiusLD.value?.let { updateLimitDistance(it) }
                    })
            } else {
                binding.cetLimitDistance.visibility = GONE
                settingFlyControllerVM.setFlightParamsMaxRadius(
                    ModelXDroneConst.MAX_DISTANCE_LIMIT.toFloat(),
                    onSuccess = {
                        //指令成功处理
                    },
                    onError = {
                        binding.tvDistanceLimitOpen.setCheckedWithoutListener(true)
                        binding.cetLimitDistance.visibility = VISIBLE
                        showToast(R.string.common_text_set_failed)
                        settingFlyControllerVM.maxRadiusLD.value?.let { updateLimitDistance(it) }
                    })
            }

        }
        //初始化飞行档位
        loadGearControl()

        //设置返航点
        binding.cisCoordinateTurn.setOnMarkClickListener {
            ExplainPopupWindow(requireContext()).showExplain(
                it,
                getString(R.string.common_text_coordinated_turn_tips)
            )
        }
        //协调转弯
        binding.cisCoordinateTurn.setOnSwitchChangeListener {
            settingFlyControllerVM.setFlightCoordinatedTurnMsg(
                it,
                onSuccess = {
                    if (it) {
                        showToast(R.string.common_text_on_trun)
                    } else {
                        showToast(R.string.common_text_off_trun)
                    }
                },
                onError = { e ->
                    binding.cisCoordinateTurn.setCheckedWithoutListener(!it)
                    showToast(R.string.common_text_set_failed)
                })
        }
        binding.citCompassCalibration.isVisible = AppInfoManager.isSupportCompassCal()
        binding.citCompassCalibration.setOnItemClickListener {
            DeviceUtils.singleControlDrone()?.let {
                MiddlewareManager.settingModule.jumpCompassCalibration(
                    requireContext(),
                    it
                )
            }
        }

        binding.citImuCalibration.isVisible = AppInfoManager.isSupportIMUCal()
        binding.citImuCalibration.setOnItemClickListener {
            DeviceUtils.singleControlDrone()
                ?.let { MiddlewareManager.settingModule.jumpIMUCalibration(requireContext(), it) }
        }

        //失控行为
        loadLostControl()

        binding.citSensitivitySetting.setOnItemClickListener {
            switchVM.addFragment(
                SettingFlySensitivityFragment(),
                getString(R.string.common_text_sensitivity_setting),
                true
            )
        }

        val gpsLocationType = resources.getStringArray(R.array.common_text_gps_location_system)

        binding.csvGpsLocation.dataList = gpsLocationType.toList()
        binding.csvGpsLocation.setDefaultText(getString(R.string.common_text_gps_location_1))
        binding.csvGpsLocation.setSpinnerViewListener { position ->
            //TODO 卫星定位系统
            Log.e(TAG, gpsLocationType[position])
        }

        binding.rbControllerLocation.setOnClickListener(this)
        binding.rbHomeLocation.setOnClickListener(this)
        binding.cisReturnDetour.setOnSwitchChangeListener { open ->
            val title =
                if (open) R.string.common_text_return_detour_open else R.string.common_text_return_detour_close
            val msg =
                if (open) R.string.common_text_return_detour_open_tips else R.string.common_text_return_detour_close_tips
            var isCommit = false
            context?.let { context ->
                CommonTwoButtonDialog(context).apply {
                    setTitle(getString(title))
                    setMessage(getString(msg))
                    setRightBtnListener {
                        isCommit = true
                        settingFlyControllerVM.setReturnDetourSwitch(open, {}, {
                            showToast(R.string.common_text_set_failed)
                            binding.cisReturnDetour.setCheckedWithoutListener(!open)
                        })
                    }
                    setOnDismissListener {
                        if (!isCommit) binding.cisReturnDetour.setCheckedWithoutListener(!open)
                    }
                    show()
                }
            }
        }
        loadEmergencyEvasion()

        initRadarCal()
    }

    private fun initRadarCal() {
        binding.citRadarCalibration.isVisible = AppInfoManager.isSupportRadarCal()
        binding.citRadarCalibration.setOnItemClickListener {
            startActivity(Intent(activity,SettingRadarCalibrationActivity::class.java))
        }
    }

    /**
     * 输入的值转换成米
     * @param value 输入的值，
     * @return 输出米
     */
    private fun getDistanceFromInput(value: Float): Float {
        return if (UnitManager.getSelectSpeedUnit() == DistanceSpeedUnitEnum.Imperial) {
            TransformUtils.feet2meter(value.toDouble(), 6).toFloat()
        } else {
            value
        }
    }

    /**
     * 米转显示的值
     * @param value 米
     * @return 根绝单位显示的值
     */
    private fun getDistanceToShow(value: Float): Float {
        return if (UnitManager.getSelectSpeedUnit() == DistanceSpeedUnitEnum.Imperial) {
            TransformUtils.meter2feet(value.toDouble(), 6).toFloat()
        } else {
            value
        }
    }

    private fun setLimitHeight(limitHeight: Float) {
        settingFlyControllerVM.setFlightParamsMaxHeight(limitHeight,
            onSuccess = {
                //小于800，则为有效限高
                if (limitHeight <= ModelXDroneConst.DRONE_FLIGHT_HEIGHT_MAX) {
                    AutelStorageManager.getPlainStorage()
                        .setFloatValue(StorageKey.PlainKey.KEY_LAST_HEIGHT_LIMIT_VALUE, limitHeight)
                }
            },
            onError = {
                settingFlyControllerVM.maxHeightLD.value?.let { updateLimitHeight(it) }
                showToast(R.string.common_text_set_failed)
            })
    }

    private fun loadEmergencyEvasion() {
        //紧急避险功能
        binding.cisNecessity.isVisible = AppInfoManager.isSupportNecessity()
        binding.cisNecessity.setOnSwitchChangeListener { open ->
            if (open) {
                context?.let { context ->
                    CommonTwoButtonDialog(context)
                        .apply {
                            setTitle(getString(R.string.common_text_tips_title))
                            setMessage(getString(R.string.common_text_necessity_tips))
                            setRightBtnListener { setNecessitySwitch(open) }
                            setOnDismissListener { refreshNecessityView() }
                            show()
                        }
                }
            } else {
                setNecessitySwitch(open)
            }
        }
    }

    /**
     * 设置紧急避险开关
     */
    private fun setNecessitySwitch(open: Boolean) {
        DeviceUtils.singleControlDrone()?.let { droneDevice ->
            settingFlyControllerVM.setNecessitySwitch(droneDevice, open, {
                isOpenNecessity = open
                binding.cisNecessity.setCheckedWithoutListener(isOpenNecessity)
                refreshNecessityView()
            }, {
                showToast(R.string.common_text_set_failed)
                refreshNecessityView()
            })
        }
    }

    /**
     * 刷新紧急避险相关控件
     */
    private fun refreshNecessityView() {
        binding.cisNecessity.setCheckedWithoutListener(isOpenNecessity)
        binding.csisvLossController.isVisible = !isOpenNecessity
    }

    /**
     * 加载失联动作设置
     * */
    private fun loadLostControl() {
        val lostController =
            resources.getStringArray(R.array.common_fly_loss_controller).toMutableList()
        if (AppInfoManager.isSupportAutoClimb()) {
            lostController.add(
                String.format(
                    requireContext().getString(R.string.common_text_climb_1000m),
                    TransformUtils.getLengthWithUnit(1200.0)
                )
            )
        }
        binding.csisvLossController.updateSettingTitle(resources.getString(R.string.common_text_loss_controller))
        binding.csisvLossController.updateSpinnerData(lostController.toList())
        binding.csisvLossController.updateSpinnerTitle(resources.getString(R.string.common_text_go_home))
        binding.csisvLossController.setSpinnerSelectedListener { position ->
            AutelLog.i(TAG, "csisvLossController position=$position")
            //position 0：返航，1：悬停 2 降落
            val actionEnum = when (position) {
                1 -> DroneLostActionEnum.HOVER
                2 -> DroneLostActionEnum.LANDING
                3 -> DroneLostActionEnum.CLIMB_1000M
                else -> DroneLostActionEnum.BACK
            }
            AutelLog.d(TAG, "setFlightParamsRCLostActionMsg -> actionEnum=$actionEnum")
            settingFlyControllerVM.setFlightParamsRCLostActionMsg(actionEnum,
                onSuccess = { showToast(R.string.common_text_setting_success) },
                onError = { showToast(R.string.common_text_set_failed) })
        }
    }

    /**
     * 更新失联动作SpinnerText
     * */
    private fun updateLostControlSpinnerText(lostAction: DroneLostActionEnum) {
        AutelLog.i(TAG, "updateLostControlSpinnerText -> lostAction=$lostAction")
        val lostController = when (lostAction) {
            DroneLostActionEnum.HOVER -> getString(R.string.common_text_hover)
            DroneLostActionEnum.LANDING -> getString(R.string.common_text_land)
            DroneLostActionEnum.CLIMB_1000M -> {
                String.format(
                    requireContext().getString(R.string.common_text_climb_1000m),
                    TransformUtils.getLengthWithUnit(1200.0)
                )
            }

            else -> getString(R.string.common_text_go_home)
        }
        binding.csisvLossController.updateSpinnerTitle(lostController)
    }


    /**
     * 初始化飞行档位
     */
    private var curGearLevel = GearLevelEnum.UNKNOWN//当前档位
    private var isCommitGear = false
    private fun loadGearControl() {
        gearModeList.clear()
        gearModeList.add(
            GearModeBean(
                GearLevelEnum.LOW_SPEED,
                getString(R.string.common_text_gear_low_speed)
            )
        )
        gearModeList.add(
            GearModeBean(
                GearLevelEnum.SMOOTH,
                getString(R.string.common_text_comfort_gear)
            )
        )
        gearModeList.add(
            GearModeBean(
                GearLevelEnum.NORMAL,
                getString(R.string.common_text_standard_gear)
            )
        )
        gearModeList.add(
            GearModeBean(
                GearLevelEnum.SPORT,
                getString(R.string.common_text_sport_gear)
            )
        )

        val gearList = ArrayList<String>()
        for (x in gearModeList) {
            gearList.add(x.name)
        }
        binding.csisvGear.updateSettingTitle(resources.getString(R.string.common_text_fly_gear))
        binding.csisvGear.updateSpinnerData(gearList)
        binding.csisvGear.updateSpinnerTitleIndex(0)
        binding.csisvGear.setSpinnerSelectedListener { position ->
            AutelLog.i(SettingTag, "position=$position gearModeList.size=${gearModeList.size}")
            if (position < gearModeList.size) {
                val targetLevel = gearModeList[position].mode
                AutelLog.i(SettingTag, "targetLevel=$targetLevel")
                if (targetLevel == GearLevelEnum.SPORT) {
                    isCommitGear = true
                    GearLevelSwitchDialog(requireContext()).apply {
                        setOnConfirmBtnClick {
                            isCommitGear = false
                            settingFlyControllerVM.setFlightGear(targetLevel,
                                onSuccess = {
                                    //指令成功处理
                                    curGearLevel = targetLevel
                                    updateGearLevel(curGearLevel)
                                },
                                onError = {
                                    //指令失败处理
                                    showToast(R.string.common_text_set_failed)
                                    updateGearLevel(curGearLevel)
                                })
                        }
                        setOnDismissListener {
                            if (isCommitGear) {
                                updateGearLevel(curGearLevel)
                            }
                        }
                        show()
                    }
                } else {
                    settingFlyControllerVM.setFlightGear(targetLevel,
                        onSuccess = {
                            //指令成功处理
                            curGearLevel = targetLevel
                            updateGearLevel(curGearLevel)
                        },
                        onError = {
                            //指令失败处理
                            showToast(R.string.common_text_set_failed)
                            updateGearLevel(curGearLevel)
                        })
                }
            }
        }
    }

    /**
     * 更新档位显示
     * */
    private fun updateGearLevel(gear: GearLevelEnum) {
        if (gear != null) {
            if (gear.value > 0) {
                binding.csisvGear.updateSpinnerTitleIndex(getGearIndex(gear))
            } else {
                binding.csisvGear.updateSpinnerTitleIndex(1)
            }
        }
    }

    /**
     * 通过档位获取index
     */
    private fun getGearIndex(gear: GearLevelEnum): Int {
        for (x in gearModeList.indices) {
            if (gear == gearModeList[x].mode) return x
        }
        return 0
    }


    override fun getData() {
        settingFlyControllerVM.getDroneControlParams()
        //隐藏返航避障功能
        /* settingFlyControllerVM.getReturnDetourSwitch(onSuccess = {
             binding.cisReturnDetour.setCheckedWithoutListener(it)
         }, {})*/
        //是否支持紧急避险
        if (AppInfoManager.isSupportNecessity()) {
            DeviceUtils.singleControlDrone()?.let { droneDevice ->
                settingFlyControllerVM.getNecessitySwitch(droneDevice, onSuccess = { open ->
                    isOpenNecessity = open
                    refreshNecessityView()
                }, {})
            }
        }
    }

    /**
     * 检查经纬度
     */
    private fun checkLocation(location: AutelLatLng?, isDrone: Boolean): Boolean {
        if (!DeviceUtils.isSingleControlDroneConnected()) {
            showToast(R.string.common_text_aircraft_disconnect)
            return false
        }
        if (settingFlyControllerVM.dronWorkMode == DroneWorkModeEnum.RETURN) {
            showToast(R.string.common_text_unable_update_retrun_point)
            return false
        }
        if (location == null || location.isInvalid()) {
            val text =
                if (isDrone) getString(R.string.common_text_get_drone_location_failure) else getString(
                    R.string.common_text_get_remoter_location_failure
                )
            showToast(text)
            return false
        }
        return true
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.rb_home_location -> {
                val location = AutelLatLng(
                    settingFlyControllerVM.droneLatitude,
                    settingFlyControllerVM.droneLongitude
                )
                if (!checkLocation(location, true)) return
                setReturnHomePoint(
                    location,
                    getString(R.string.common_text_refresh_return_home_drone),
                    true
                )
            }

            R.id.rb_controller_location -> {
                val location = AutelPhoneLocationManager.locationLiveData.value ?: AutelLatLng()
                if (!checkLocation(location, false)) return
                setReturnHomePoint(
                    location,
                    getString(R.string.common_text_refresh_return_home_remoter),
                    false
                )
            }
        }
    }

    /**
     * 设置返航点
     */
    private fun setReturnHomePoint(location: AutelLatLng, tips: String, isDrone: Boolean) {
        //自定义home点数值需要转换
        val fixLatitude = location.latitude * 10000000
        val fixLongitude = location.longitude * 10000000
        val fixAltitude = location.altitude * 1000
        val homeLocation =
            HomeLocation(fixLatitude.toLong(), fixLongitude.toLong(), fixAltitude.toLong())
        homeLocation.type = if (isDrone) LocationTypeEnum.DRONE_CURRENT else LocationTypeEnum.RC
        CommonTwoButtonDialog(requireContext()).apply {
            setTitle(getString(R.string.common_text_refresh_return_home_title))
            setMessage(tips)
            setRightBtnListener {
                settingFlyControllerVM.setCustomHomeLocation(
                    homeLocation,
                    onSuccess = {
                        //指令成功处理
                        showToast(R.string.common_text_set_home_point_success)
                    },
                    onError = {
                        //指令失败处理
                        showToast(R.string.common_text_set_home_point_failed)
                    })
            }
            show()
        }
    }
}