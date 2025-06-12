package com.autel.setting.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import com.autel.common.base.BaseAircraftFragment
import com.autel.common.bean.VisionStateInfo
import com.autel.common.lifecycle.LiveDataBus
import com.autel.common.lifecycle.event.RadarSwitchEvent
import com.autel.common.lifecycle.event.RadarVoiceEvent
import com.autel.common.listener.CommonDialogListener
import com.autel.common.manager.AppInfoManager
import com.autel.common.manager.AutelStorageManager
import com.autel.common.manager.CommonDialogManager
import com.autel.common.manager.StorageKey
import com.autel.common.manager.unit.DistanceSpeedUnitEnum
import com.autel.common.manager.unit.UnitManager
import com.autel.common.sdk.ModelXDroneConst
import com.autel.common.sdk.business.SettingObstacleAvoidanceVM
import com.autel.common.utils.DeviceUtils
import com.autel.common.utils.TransformUtils
import com.autel.common.widget.dialog.CommonTwoButtonDialog
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.vision.enums.ObstacleAvoidActionEnum
import com.autel.log.AutelLog
import com.autel.setting.R
import com.autel.setting.databinding.SettingAvoidingObstaclesFragmentBinding
import kotlin.math.roundToInt

/**
 * @author
 * @date 2023/3/23
 * 避障设置-UI改版
 */
class SettingAvoidingObstaclesFragment : BaseAircraftFragment() {

    companion object {
        const val TAG = "SettingAvoidingObstaclesFragment"
    }

    private val obstacleAvoidanceVM: SettingObstacleAvoidanceVM by activityViewModels()
    private lateinit var binding: SettingAvoidingObstaclesFragmentBinding

    /**
     * 水平、上、下三个避障，
     * 避障图标逻辑
     * 1、只要三个避障有一个打开了，就显示绿色，所有关闭就显示红色
     * 2、飞机未连接的时候，显示灰色
     * 3、绿色的时候点击，调用3个避障关闭（不管接口是否成功），按钮变红色
     * 4、当按钮红色的时候，调用3个避障开启（不管接口是否成功），按钮变绿色
     * 5、避障开关，和雷达图不相关（逻辑独立）
     */
    private var isOpenVision = false//是否打开避障开关
    private val curVisionInfo = VisionStateInfo()
    private var isShowing = false//避障弹框是否显示

    //刹停距离，会动态刷新
    private var stoppingDistanceMax =
        ModelXDroneConst.OBSTACLE_AVOIDANCE_STOPPING_DISTANCE_MAX.toFloat()

    //当前刹停距离
    private var curStoppingDistance =
        ModelXDroneConst.OBSTACLE_AVOIDANCE_STOPPING_DISTANCE_MIN.toFloat()
    private var avoidanceBehaviorIndex = 0//默认避障行为

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = SettingAvoidingObstaclesFragmentBinding.inflate(LayoutInflater.from(context))
        return binding.root
    }

    override fun addListen() {
        //初始化避障系统
        binding.cisObstacleAvoidanceSystem.setOnSwitchChangeListener {
            if (!DeviceUtils.isSingleControlDroneConnected()) {
                showToast(R.string.common_text_aircraft_disconnect)
                binding.cisObstacleAvoidanceSystem.setCheckedWithoutListener(isOpenVision)
                return@setOnSwitchChangeListener
            }
            dealSetAllVersion()
        }

        //初始化避障行为
        binding.circAvoidanceBehavior.addRadioButton(resources.getStringArray(R.array.common_avoidance_behavior))
        binding.circAvoidanceBehavior.setOnSwitchChangeListener {
            dealSetAvoidanceBehavior(it)
        }
        //雷达显示开关
        val radarSwitch = AutelStorageManager.getPlainStorage()
            .getBooleanValue(StorageKey.PlainKey.KEY_RADAR_SWITCH, true)
        binding.cisRadarSwitch.setCheckedWithoutListener(radarSwitch)

        //雷达声音开关，如果雷达开关是关闭的，则关闭声音，如果是打开的，怎么不管
        val radarVoice = AutelStorageManager.getPlainStorage()
            .getBooleanValue(StorageKey.PlainKey.KEY_RADAR_VOICE, true)
        if (!radarSwitch) {
            binding.cisRadarVoiceSwitch.setCheckedWithoutListener(false)
            AutelStorageManager.getPlainStorage()
                .setBooleanValue(StorageKey.PlainKey.KEY_RADAR_VOICE, false)
        } else {
            binding.cisRadarVoiceSwitch.setCheckedWithoutListener(radarVoice)
        }

        //雷达开关切换，开启时需要显示雷达图
        binding.cisRadarSwitch.setOnSwitchChangeListener {
            AutelStorageManager.getPlainStorage()
                .setBooleanValue(StorageKey.PlainKey.KEY_RADAR_SWITCH, it)
            LiveDataBus.of(RadarSwitchEvent::class.java).showRadarAdjust().post(it)
            //关闭时，需要把声音关掉
            if (!it) {
                AutelStorageManager.getPlainStorage()
                    .setBooleanValue(StorageKey.PlainKey.KEY_RADAR_VOICE, false)
                LiveDataBus.of(RadarVoiceEvent::class.java).isOpenRadarVoice().post(false)
                binding.cisRadarVoiceSwitch.setCheckedWithoutListener(false)
                binding.cisRadarVoiceSwitch.isEnabled = false
            } else {
                binding.cisRadarVoiceSwitch.isEnabled = true
            }
        }

        //雷达声音开关
        binding.cisRadarVoiceSwitch.setOnSwitchChangeListener {
            AutelStorageManager.getPlainStorage()
                .setBooleanValue(StorageKey.PlainKey.KEY_RADAR_VOICE, it)
            LiveDataBus.of(RadarVoiceEvent::class.java).isOpenRadarVoice().post(it)
        }

        binding.cssvStoppingDistance.setOnSeekBarStoppedListener {
            var value = it
            if (UnitManager.getSelectSpeedUnit() == DistanceSpeedUnitEnum.Imperial) {
                value = TransformUtils.feet2meter(it.toDouble(), 5).toFloat()
            }
            setAllStopDistance(value)
        }

        binding.cssvAlarmDistance.setOnSeekBarStoppedListener {
            var value = it
            if (UnitManager.getSelectSpeedUnit() == DistanceSpeedUnitEnum.Imperial) {
                value = TransformUtils.feet2meter(it.toDouble(), 5).toFloat()
            }
            setAllWarning(value)
        }
        //降落保护
        binding.cisLandProtectSwitch.setOnSwitchChangeListener {
            AutelLog.i(TAG, "cisLandProtectSwitch -> $it")
            val isOpen = it
            if (!DeviceUtils.isSingleControlDroneConnected()) {
                showToast(R.string.common_text_aircraft_disconnect)
                binding.cisLandProtectSwitch.setCheckedWithoutListener(!isOpen)
                return@setOnSwitchChangeListener
            }
            obstacleAvoidanceVM.setLandProtect(isOpen, {
                showToast(R.string.common_text_setting_success)
            }, onError = {
                showToast(R.string.common_text_set_failed)
                binding.cisLandProtectSwitch.setCheckedWithoutListener(!isOpen)
            })
        }
        loadObstacleAvoidanceSetting()

    }

    /**
     * 处理避障行为
     */
    private fun dealSetAvoidanceBehavior(index: Int) {
        if (!DeviceUtils.isSingleControlDroneConnected()) {
            showToast(R.string.common_text_aircraft_disconnect)
            binding.circAvoidanceBehavior.setRadioGroupCheck(avoidanceBehaviorIndex)
            return
        }

        when (index) {
            //关闭需要二次弹框
            0 -> {
                CommonDialogManager.showOABehaviorDialog(requireContext(),object :CommonDialogListener{
                    override fun onDismiss() {
                        refreshAvoidanceBehaviorUI()
                    }

                    override fun onLeftBtnClick() {
                        binding.circAvoidanceBehavior.setRadioGroupCheck(avoidanceBehaviorIndex)
                    }

                    override fun onRightBtnClick() {
                        obstacleAvoidanceVM.setAvoidanceBehavior(
                            ObstacleAvoidActionEnum.CLOSE,
                            onSuccess = {
                                avoidanceBehaviorIndex = 0
                                refreshAvoidanceBehaviorUI()
                                showToast(R.string.common_text_oa_behavior_closed)
                            },
                            {
                                showToast(R.string.common_text_set_failed)
                                refreshAvoidanceBehaviorUI()
                                binding.circAvoidanceBehavior.setRadioGroupCheck(avoidanceBehaviorIndex)
                            })
                    }

                })
            }
            //刹停，绕障
            1, 2 -> {
                val action =
                    if (index == 1) ObstacleAvoidActionEnum.STOP else ObstacleAvoidActionEnum.BYPASS
                obstacleAvoidanceVM.setAvoidanceBehavior(action, onSuccess = {
                    avoidanceBehaviorIndex = index
                    refreshAvoidanceBehaviorUI()
                    val tips = if (index == 1) R.string.common_text_oa_behavior_stopped else R.string.common_text_oa_behavior_bypassed
                    showToast(tips)
                }, {
                    showToast(R.string.common_text_set_failed)
                    refreshAvoidanceBehaviorUI()
                    binding.circAvoidanceBehavior.setRadioGroupCheck(avoidanceBehaviorIndex)
                })
            }
        }
    }

    /**
     * 加载避障方式设置
     * */
    private fun loadObstacleAvoidanceSetting() {
        binding.csisvObstacleAvoidance.isVisible = AppInfoManager.isSupportAvoidanceWay()
        binding.csisvObstacleAvoidance.setSpinnerSelectedListener {
            obstacleAvoidanceVM.switchAvoidObstaclesMode {
                showToast(R.string.common_text_set_failed)
            }
        }
        //避障方式，ture：绕障开启，false 绕障关闭（悬停），初始化时0 悬停，1 绕障
        val avoidanceWayList = ArrayList<String>()
        avoidanceWayList.add(getString(R.string.common_text_hover))
        avoidanceWayList.add(getString(R.string.common_text_avoid_obstacles))
        binding.csisvObstacleAvoidance.updateSpinnerData(avoidanceWayList)
        binding.csisvObstacleAvoidance.updateSpinnerTitleIndex(0)
        obstacleAvoidanceVM.avoidObstaclesModeLd.observe(viewLifecycleOwner) {
            binding.csisvObstacleAvoidance.updateSpinnerTitleIndex(if (it) 1 else 0)
        }
    }

    /**
     * 刷新避障设置UI
     */
    private fun refreshSystemUI() {
        isOpenVision = curVisionInfo.isOneMore()
        val bgId =
            if (isOpenVision) R.drawable.common_shape_mission_1c1c1e_top_9 else R.drawable.common_shape_rect_solid_color_1c1c1e_radius_9
        binding.cisObstacleAvoidanceSystem.setBackgroundResource(bgId)
        binding.cisObstacleAvoidanceSystem.setBottomLineVisible(if (isOpenVision) View.VISIBLE else View.GONE)
        binding.cssvStoppingDistance.isVisible = curVisionInfo.isOneMore()
        binding.cisObstacleAvoidanceSystem.setCheckedWithoutListener(curVisionInfo.isOneMore())
    }

    /**
     * 刷新避障行为UI
     */
    private fun refreshAvoidanceBehaviorUI() {
        val bgId =
            if (avoidanceBehaviorIndex == 0) R.drawable.common_shape_mission_1c1c1e_bottom_9 else R.drawable.common_shape_mission_1c1c1e_none_9
        binding.cssvStoppingDistance.isVisible = avoidanceBehaviorIndex != 0
        binding.circAvoidanceBehavior.setBackgroundResource(bgId)
    }


    override fun getData() {
        val open = DeviceUtils.singleControlDrone()
            ?.getDeviceStateData()?.flightControlData?.obstacleAvoidanceEnabled == true
        curVisionInfo.horizontal = open
        curVisionInfo.top = open
        curVisionInfo.bottom = open

        refreshSystemUI()
        //告警距离
        obstacleAvoidanceVM.getHorizontalWarningDistance(
            onSuccess = {
                var value = it.toInt()
                if (UnitManager.getSelectSpeedUnit() == DistanceSpeedUnitEnum.Imperial) {
                    value = TransformUtils.meter2feet(it.toDouble(), 0).toInt()
                }
                binding.cssvAlarmDistance.setProgress(value)

                //如果设置的避障距离距离大于7 ，则刹停距离为7
                stoppingDistanceMax =
                    if (it >= ModelXDroneConst.OBSTACLE_AVOIDANCE_STOPPING_DISTANCE_MAX) {
                        ModelXDroneConst.OBSTACLE_AVOIDANCE_STOPPING_DISTANCE_MAX.toFloat()
                    } else {
                        it
                    }

                getStoppingDistance()
            }, {})
        obstacleAvoidanceVM.getLandProtect(onSuccess = {
            binding.cisLandProtectSwitch.setCheckedWithoutListener(it)
        }, {})

        obstacleAvoidanceVM.getAvoidObstaclesMode()

        obstacleAvoidanceVM.getAvoidanceBehavior(onSuccess = {
            //这里需要保证value和index一样
            binding.circAvoidanceBehavior.setRadioGroupCheck(it.value)
            avoidanceBehaviorIndex = it.value
            refreshAvoidanceBehaviorUI()
        }, {})
    }

    /**
     * 获取刹停距离
     */
    private fun getStoppingDistance() {
        //避障距离就取用水平避障 刹停距离
        obstacleAvoidanceVM.getHorizontalBrakeDistance(
            onSuccess = {
                //当刹停距离大于避障距离时，将校正刹停距离
                curStoppingDistance = if (it > stoppingDistanceMax) {
                    AutelLog.i(
                        TAG,
                        "getHorizontalBrakeDistance -> result=$it stoppingDistanceMax=$stoppingDistanceMax"
                    )
                    setAllStopDistance(stoppingDistanceMax)
                    stoppingDistanceMax
                } else {
                    it
                }
                refreshStoppingDistance()
            }, {})
    }

    /**
     * 设置所有避障开关
     */
    private fun setAllVersion() {
        //todo  没有了
        //obstacleAvoidanceVM.setBottomObstacleAvoidance(isOpenVision, {}, {})
        // obstacleAvoidanceVM.setTopObstacleAvoidance(isOpenVision, {//}, {})
        // obstacleAvoidanceVM.setObstacleEvasion(isOpenVision, {}, {})

    }

    /**
     * 设置所有刹停距离
     */
    private fun setAllStopDistance(value: Float) {
        obstacleAvoidanceVM.setTopBrakeDistance(value, {}, {})
        obstacleAvoidanceVM.setBottomBrakeDistance(value, {}, {})
        obstacleAvoidanceVM.setHorizontalBrakeDistance(value, {
            curStoppingDistance = value
        }, {
            showToast(R.string.common_text_set_failed)
        })
    }

    /**
     * 设置所有告警距离
     */
    private fun setAllWarning(value: Float) {
        obstacleAvoidanceVM.setTopWarningDistance(value, {}, {})
        obstacleAvoidanceVM.setBottomWarningDistance(value, {}, {})
        obstacleAvoidanceVM.setHorizontalWarningDistance(value, {
            //当告警距离小于7
            if (value < ModelXDroneConst.OBSTACLE_AVOIDANCE_STOPPING_DISTANCE_MAX) {
                //当告警距离小于刹停上限时
                if (value < stoppingDistanceMax) {
                    stoppingDistanceMax = value
                    if (value < curStoppingDistance) {
                        setAllStopDistance(value)
                        curStoppingDistance = value
                    }
                } else {
                    stoppingDistanceMax = value
                }
                refreshStoppingDistance()
            } else {
                //当刹停距离小于7
                if (stoppingDistanceMax < ModelXDroneConst.OBSTACLE_AVOIDANCE_STOPPING_DISTANCE_MAX) {
                    stoppingDistanceMax =
                        ModelXDroneConst.OBSTACLE_AVOIDANCE_STOPPING_DISTANCE_MAX.toFloat()
                    refreshStoppingDistance()
                }
            }
        }, {
            showToast(R.string.common_text_set_failed)
        })
    }

    /**
     * 处理开关所有避障
     */
    private fun dealSetAllVersion() {
        if (isOpenVision) {
            if (isShowing) return
            isShowing = true
            CommonTwoButtonDialog(requireContext()).apply {
                setWarningIcon(R.drawable.common_ic_important_tips)
                setTitle(getString(R.string.common_text_close_avoidance_behavior))
                setMessage(getString(R.string.common_text_close_all_version_tips))
                setRightBtnStr(getString(R.string.common_text_close))
                setAutoDismiss(true)
                setOnDismissListener {
                    this@SettingAvoidingObstaclesFragment.isShowing = false
                    refreshSystemUI()
                }
                setRightBtnListener {
                    isOpenVision = !isOpenVision
                    curVisionInfo.reset(isOpenVision)
                    refreshSystemUI()
                    AutelLog.d(TAG, "isOpenVision=$isOpenVision")
                    setAllVersion()
                }
            }.show()
        } else {
            isOpenVision = !isOpenVision
            curVisionInfo.reset(isOpenVision)
            refreshSystemUI()
            AutelLog.d(TAG, "isOpenVision=$isOpenVision")
            setAllVersion()
        }
    }

    override fun onVisible() {
        super.onVisible()
        refreshUnit()
    }

    /**
     * 刷新单位
     */
    private fun refreshUnit() {
        val alarmDistanceMin =
            Math.round(TransformUtils.getDistanceValueWithmWithoutUnit(ModelXDroneConst.OBSTACLE_AVOIDANCE_ALARM_DISTANCE_MIN * 1.0))
                .toInt()
        val alarmDistanceMax =
            Math.round(TransformUtils.getDistanceValueWithmWithoutUnit(ModelXDroneConst.OBSTACLE_AVOIDANCE_ALARM_DISTANCE_MAX * 1.0))
                .toInt()
        val alarmDistanceMaxStr =
            TransformUtils.getDistanceValueWithm(ModelXDroneConst.OBSTACLE_AVOIDANCE_ALARM_DISTANCE_MAX * 1.0)

        binding.cssvAlarmDistance.initSeekBar(alarmDistanceMin, alarmDistanceMax)
        binding.cssvAlarmDistance.setTitle(
            String.format(
                getString(R.string.common_text_alarm_distance),
                "$alarmDistanceMin",
                alarmDistanceMaxStr
            )
        )

        refreshStoppingDistance()
    }

    private var stopDistanceMin = 0
    private var stopDistanceMaxStr = ""

    /**
     * 初始化刹停距离并设置
     */
    private fun refreshStoppingDistance() {
        AutelLog.i(
            TAG,
            "refreshStoppingDistance -> curStoppingDistance=$curStoppingDistance stoppingDistanceMax=$stoppingDistanceMax"
        )
        stopDistanceMin =
            TransformUtils.getDistanceValueWithmWithoutUnit(ModelXDroneConst.OBSTACLE_AVOIDANCE_STOPPING_DISTANCE_MIN * 1.0)
                .roundToInt()
        val stopDistanceMax =
            TransformUtils.getDistanceValueWithmWithoutUnit(stoppingDistanceMax * 1.0).roundToInt()
        stopDistanceMaxStr = TransformUtils.getDistanceValueWithm(stoppingDistanceMax * 1.0)

        binding.cssvStoppingDistance.initSeekBar(stopDistanceMin, stopDistanceMax)
        binding.cssvStoppingDistance.setTitle(
            String.format(
                getString(R.string.common_text_safety_distance),
                "$stopDistanceMin",
                stopDistanceMaxStr
            )
        )

        var value = curStoppingDistance.toInt()
        if (UnitManager.getSelectSpeedUnit() == DistanceSpeedUnitEnum.Imperial) {
            value = TransformUtils.meter2feet(curStoppingDistance.toDouble(), 5).toInt()
        }
        binding.cssvStoppingDistance.setProgress(value)

    }
}