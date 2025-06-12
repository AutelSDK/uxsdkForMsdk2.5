package com.autel.widget.radar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.autel.common.base.BaseAircraftFragment
import com.autel.common.bean.RADAR_LEVEL_1
import com.autel.common.bean.RADAR_LEVEL_2
import com.autel.common.bean.RADAR_LEVEL_3
import com.autel.common.bean.RADAR_LEVEL_NONE
import com.autel.common.bean.RadarWarn
import com.autel.common.lifecycle.LiveDataBus
import com.autel.common.lifecycle.event.RadarSwitchEvent
import com.autel.common.lifecycle.event.RadarVoiceEvent
import com.autel.common.manager.AutelStorageManager
import com.autel.common.manager.StorageKey
import com.autel.common.sdk.responsity.VersionRepository
import com.autel.common.utils.DeviceUtils
import com.autel.common.utils.TimerEventListener
import com.autel.common.utils.TimerManager
import com.autel.common.utils.TransformUtils
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.vision.bean.VisionRadarInfoBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.vision.enums.VisionSensorPositionEnum
import com.autel.log.AutelLog
import com.autel.widget.R
import com.autel.widget.databinding.MissionFragmentRadarBinding



class RadarFragment : BaseAircraftFragment() {

    companion object {
        const val TAG = "RadarFragment"
        const val ALERT_SERIOUS_LIMIT = 2//严重红色告警距离线
        const val ALERT_READ_LIMIT = 5//普通红色告警距离线
        const val INVALID_VALUE = 10000f//非法值,用作初始基准判断
        const val RADAR_INVALID_VALUE = -1f//非法值
        const val RESTORE_TIME = 2300//暂留时间
    }

    private lateinit var binding: MissionFragmentRadarBinding
    private var isRadarSwitch = false//雷达图是否开启
    private var isRadarVoice = false//雷达图声音是否开启
    private var isUIVisible = false//界面是否显示
    private var isRunning = false//飞机是否启动
    private var curTime = 0L//日志写入控制
    private var curAliveTime = 0L//雷达信息活跃时间

    private val mRadarWarn: RadarWarn by lazy { RadarWarn(requireContext()) }
    private var visionRadarCountMap = mutableMapOf<VisionSensorPositionEnum, Long>()
    private val VISION_RADAR_FIX_COUNT = 5 //雷达图触发连续上报次数
    private val directionUiModelMap = mutableMapOf<VisionSensorPositionEnum, FourDirectionRadarUIModel>()
    private val directionModelMap = mutableMapOf<VisionSensorPositionEnum, FourDirectionRadarModel>()
    private val topBottomUiModelMap = mutableMapOf<VisionSensorPositionEnum, TopBottomRadarUIModel>()
    private val topBottomModelMap = mutableMapOf<VisionSensorPositionEnum, TopBottomRadarModel>()
    private var lastRadarModel: RadarModel? = null

    private var timerEventListener = object : TimerEventListener(this.javaClass.simpleName) {
        override fun onEventChanged() {
            updateRadarData()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = MissionFragmentRadarBinding.inflate(inflater, container, false)

        directionUiModelMap.put(
            VisionSensorPositionEnum.LEFT,
            FourDirectionRadarUIModel(
                binding.layoutLeft,
                binding.radarLeftOne,
                binding.radarLeftTwo,
                binding.radarLeftThree,
                null,
                binding.tvLeft
            )
        )
        directionUiModelMap.put(
            VisionSensorPositionEnum.RIGHT,
            FourDirectionRadarUIModel(
                binding.layoutRight,
                binding.radarRightOne,
                binding.radarRightTwo,
                binding.radarRightThree,
                null,
                binding.tvRight
            )
        )
        directionUiModelMap.put(
            VisionSensorPositionEnum.FRONT,
            FourDirectionRadarUIModel(
                binding.layoutFront,
                binding.radarFrontOne,
                binding.radarFrontTwo,
                binding.radarFrontThree,
                binding.radarFrontFour,
                binding.tvFront
            )
        )
        directionUiModelMap.put(
            VisionSensorPositionEnum.REAR,
            FourDirectionRadarUIModel(
                binding.layoutRear,
                binding.radarRearOne,
                binding.radarRearTwo,
                binding.radarRearThree,
                binding.radarRearFour,
                binding.tvRear
            )
        )
        topBottomUiModelMap.put(VisionSensorPositionEnum.TOP, TopBottomRadarUIModel(binding.layoutUp, binding.tvTop))
        topBottomUiModelMap.put(VisionSensorPositionEnum.BOTTOM, TopBottomRadarUIModel(binding.layoutBottom, binding.tvBottom))

        isRadarSwitch = AutelStorageManager.getPlainStorage().getBooleanValue(StorageKey.PlainKey.KEY_RADAR_SWITCH, true)
        isRadarVoice = AutelStorageManager.getPlainStorage().getBooleanValue(StorageKey.PlainKey.KEY_RADAR_VOICE, true)
        return binding.root
    }

    override fun getData() {
        //刷新告警距离
        VersionRepository.getTopWarningDistance {}
        VersionRepository.getBottomWarningDistance {}
        VersionRepository.getHorizontalWarningDistance {}
    }

    override fun onVisible() {
        super.onVisible()
        TimerManager.addTimerEventListener(timerEventListener)
        isUIVisible = true
    }

    override fun onInvisible() {
        TimerManager.removeTimerEventListener(timerEventListener)
        super.onInvisible()
        isUIVisible = false
    }

    override fun onDroneChangedListener(connected: Boolean, drone: IAutelDroneDevice) {
        if (!connected) showRadar(false)
    }

    /**
     * 刷新雷达数据
     * */
    fun updateRadarData() {
        val drone = DeviceUtils.singleControlDrone()

        val radarModel = if (drone != null) {
            RadarModel(
                drone.isConnected(),
                DeviceUtils.isDroneFlying(drone),
                mutableListOf<VisionRadarInfoBean>().apply {
                    addAll(drone.getDeviceStateData().visionObsacleData.visionRadarInfos)
                },
                drone.getDeviceStateData().visionObsacleData.isVisionValidate()
            )
        } else {
            null
        }

        val radarModelNotAvailable = radarModel == null ||
                !radarModel.connected ||
                !radarModel.isVisionValidate ||
                !radarModel.isDroneFlying ||
                radarModel.radarInfoBeanList.isEmpty()

        if (isRadarSwitch && lastRadarModel != radarModel) {
            lastRadarModel = radarModel
            if (radarModel == null || radarModelNotAvailable) {
                showRadar(false)
            } else {
                for (x in radarModel.radarInfoBeanList) {
                    when (x.position) {
                        VisionSensorPositionEnum.FRONT,
                        VisionSensorPositionEnum.REAR,
                        VisionSensorPositionEnum.LEFT,
                        VisionSensorPositionEnum.RIGHT -> {
                            dealFourDirectionRadarData(directionUiModelMap.get(x.position)!!, directionModelMap.get(x.position), x)
                        }

                        VisionSensorPositionEnum.TOP,
                        VisionSensorPositionEnum.BOTTOM -> {
                            dealTopBottomRadarData(topBottomUiModelMap.get(x.position)!!, topBottomModelMap.get(x.position), x)
                        }

                        else -> {}
                    }
                }

            }
        }
        if (isRadarVoice && radarModel != null && !radarModelNotAvailable && visionRadarCountMap.values.find { it > VISION_RADAR_FIX_COUNT } != null) {
            var min = INVALID_VALUE
            for (x in radarModel.radarInfoBeanList) {
                val temMin = getMinValue(x.distances)
                if (temMin > 0 && temMin < min) {
                    min = temMin
                }
            }
            if (min == INVALID_VALUE) min = RADAR_INVALID_VALUE
            curWarnLevel = getWarnLevel(min)
            startWarn(curWarnLevel)
        } else {
            stopPlayWarn()
        }
    }

    override fun addListen() {

        //监听雷达开关
        LiveDataBus.of(RadarSwitchEvent::class.java).showRadarAdjust().observe(this) {
            AutelLog.i(TAG, "RadarSwitchEvent -> $it")
            showRadar(it)
            isRadarSwitch = it
        }

        //监听雷达声音开关
        LiveDataBus.of(RadarVoiceEvent::class.java).isOpenRadarVoice().observe(this) {
            AutelLog.i(TAG, "RadarVoiceEvent -> $it")
            isRadarVoice = it

            if (isRadarVoice && isRadarSwitch) {
                startWarn(curWarnLevel)
            } else {
                stopPlayWarn()
            }
        }

    }

    private var curWarnLevel = RADAR_LEVEL_NONE//默认等级

    /**
     * 处理四项雷达的UI显示
     */
    private fun dealFourDirectionRadarData(uiModel: FourDirectionRadarUIModel, radarModel: FourDirectionRadarModel?, data: VisionRadarInfoBean) {
        val copyRadarModel = radarModel?.copy() ?: FourDirectionRadarModel()
        calculateFourDirectionRadarModel(copyRadarModel, data)
        directionModelMap.put(data.position, copyRadarModel)
        if (radarModel?.radarGroupShow != copyRadarModel.radarGroupShow) {
            copyRadarModel.radarGroupShow?.let {
                uiModel.radarGroup.isVisible = it
            }
        }

        if (radarModel?.oneShow != copyRadarModel.oneShow) {
            copyRadarModel.oneShow?.let {
                uiModel.one.isInvisible = !it
            }
        }

        if (radarModel?.oneImageResource != copyRadarModel.oneImageResource) {
            copyRadarModel.oneImageResource?.let {
                uiModel.one.setBackgroundResource(it)
            }
        }

        if (radarModel?.twoShow != copyRadarModel.twoShow) {
            copyRadarModel.twoShow?.let {
                uiModel.two.isInvisible = !it
            }
        }

        if (radarModel?.twoImageResource != copyRadarModel.twoImageResource) {
            copyRadarModel.twoImageResource?.let {
                uiModel.two.setBackgroundResource(it)
            }
        }

        if (radarModel?.threeShow != copyRadarModel.threeShow) {
            copyRadarModel.threeShow?.let {
                uiModel.three.isInvisible = !it
            }
        }

        if (radarModel?.threeImageResource != copyRadarModel.threeImageResource) {
            copyRadarModel.threeImageResource?.let {
                uiModel.three.setBackgroundResource(it)
            }
        }

        if (radarModel?.fourShow != copyRadarModel.fourShow) {
            copyRadarModel.fourShow?.let {
                uiModel.four?.isInvisible = !it
            }
        }

        if (radarModel?.fourImageResource != copyRadarModel.fourImageResource) {
            copyRadarModel.fourImageResource?.let {
                uiModel.four?.setBackgroundResource(it)
            }
        }

        if (radarModel?.tvDistance != copyRadarModel.tvDistance) {
            copyRadarModel.tvDistance?.let {
                uiModel.tvDistance.setText(it)
            }
        }

        if (radarModel?.tvDistanceShow != copyRadarModel.tvDistanceShow) {
            copyRadarModel.tvDistanceShow?.let {
                uiModel.tvDistance.isVisible = it
            }
        }

        if (radarModel?.tvDistanceColor != copyRadarModel.tvDistanceColor) {
            copyRadarModel.tvDistanceColor?.let {
                uiModel.tvDistance.setTextColor(it)
            }
        }

    }

    /**
     * 计算前后左右的雷达模型
     */
    private fun calculateFourDirectionRadarModel(
        radarModel: FourDirectionRadarModel,
        data: VisionRadarInfoBean
    ) {
        val sensorPositionEnum = data.position
        if (data.timeStamp == 0L) {
            AutelLog.i(TAG, "dealRadarData -> timeStamp is 0 ，data need throw away")
            radarModel.radarGroupShow = false
            return
        }

        //设置字体颜色和值
        val min = getMinValue(data.distances)
        if (min > 0 && min <= getHorizontalWarningDistance()) {
            radarModel.tvDistanceShow = true

            radarModel.tvDistance = getFormatString(data.position, min)
            val count = (visionRadarCountMap[sensorPositionEnum] ?: 0) + 1
            visionRadarCountMap[sensorPositionEnum] = count
            if (count > VISION_RADAR_FIX_COUNT) {
                radarModel.radarGroupShow = true
            } else {
                radarModel.radarGroupShow = false
            }
        } else {
            visionRadarCountMap[sensorPositionEnum] = 0
            radarModel.tvDistanceShow = false
            radarModel.radarGroupShow = false
        }
        val textColor = getRadarTextColor(min)
        if (textColor != -1) {
            radarModel.tvDistanceColor = textColor
        }

        //显示雷达图
        val distances = data.distances
        if (distances != null) {

            val isLeftOrRight = data.position == VisionSensorPositionEnum.LEFT || data.position == VisionSensorPositionEnum.RIGHT

            val front1 = getRadarBgRes(distances.getOrNull(0), !isLeftOrRight, isLeftOrRight)
            if (front1 != -1) {
                radarModel.oneShow = true
                radarModel.oneImageResource = front1
            } else {
                radarModel.oneShow = false
            }

            val front2 = getRadarBgRes(distances.getOrNull(1), false, isLeftOrRight)
            if (front2 != -1) {
                radarModel.twoShow = true
                radarModel.twoImageResource = front2
            } else {
                radarModel.twoShow = false
            }


            val front3 = getRadarBgRes(distances.getOrNull(2), false, isLeftOrRight)
            if (front3 != -1) {
                radarModel.threeShow = true
                radarModel.threeImageResource = front3
            } else {
                radarModel.threeShow = false
            }

            val front4 = getRadarBgRes(distances.getOrNull(3), !isLeftOrRight, isLeftOrRight)
            if (front4 != -1) {
                radarModel.fourShow = true
                radarModel.fourImageResource = front4
            } else {
                radarModel.fourShow = false
            }
        } else {
            radarModel.oneShow = false
            radarModel.twoShow = false
            radarModel.threeShow = false
            radarModel.fourShow = false
        }
    }

    /**
     * 处理上下的UI显示
     */
    private fun dealTopBottomRadarData(uiModel: TopBottomRadarUIModel, radarModel: TopBottomRadarModel?, data: VisionRadarInfoBean) {
        val copyRadarModel = radarModel?.copy() ?: TopBottomRadarModel()
        calculateTopBottomRadarModel(copyRadarModel, data)
        topBottomModelMap.put(data.position, copyRadarModel)
        if (radarModel?.radarGroupShow != copyRadarModel.radarGroupShow) {
            copyRadarModel.radarGroupShow?.let {
                uiModel.radarGroup.isVisible = it
            }
        }
        if (radarModel?.radarBackgroundResource != copyRadarModel.radarBackgroundResource) {
            copyRadarModel.radarBackgroundResource?.let {
                uiModel.radarGroup.setBackgroundResource(it)
            }
        }
        if (radarModel?.tvDistanceShow != copyRadarModel.tvDistanceShow) {
            copyRadarModel.tvDistanceShow?.let {
                uiModel.tvDistance.isVisible = it
            }
        }
        if (radarModel?.tvDistance != copyRadarModel.tvDistance) {
            copyRadarModel.tvDistance?.let {
                uiModel.tvDistance.text = it
            }
        }
        if (radarModel?.textDistanceColor != copyRadarModel.textDistanceColor) {
            copyRadarModel.textDistanceColor?.let {
                uiModel.tvDistance.setTextColor(it)
            }
        }
    }

    /**
     * 计算上下的雷达模型
     */
    private fun calculateTopBottomRadarModel(
        radarModel: TopBottomRadarModel,
        data: VisionRadarInfoBean
    ) {
        val sensorPositionEnum = data.position
        if (data.timeStamp == 0L) {
            AutelLog.i(TAG, "dealTopBottomRadarData -> timeStamp is 0 ，data need throw away")
            radarModel.radarGroupShow = false
            return
        }

        //设置字体颜色和值
        val min = getMinValue(data.distances)
        if (min > 0 && min <= getHorizontalWarningDistance()) {
            radarModel.tvDistance = getFormatString(data.position, min)
            val count = (visionRadarCountMap[sensorPositionEnum] ?: 0) + 1
            visionRadarCountMap[sensorPositionEnum] = count
            if (count > VISION_RADAR_FIX_COUNT) {
                radarModel.radarGroupShow = true
            } else {
                radarModel.radarGroupShow = false
            }
        } else {
            visionRadarCountMap[sensorPositionEnum] = 0
            radarModel.radarGroupShow = false
        }
        val textColor = getRadarTextColor(min)
        if (textColor != -1) {
            radarModel.textDistanceColor = textColor
        }

        //显示雷达图
        if (data.distances != null) {
            val distances = data.distances!!
            if (distances.isNotEmpty()) {
                val front1 = getRadarBgRes(distances[0], isInside = true)
                if (front1 != -1) {
                    radarModel.radarGroupShow = true
                    radarModel.radarBackgroundResource = front1
                } else {
                    radarModel.radarGroupShow = false
                }
            } else {
                radarModel.radarGroupShow = false
            }
        } else {
            radarModel.radarGroupShow = false
        }
    }

    /**
     * 获取最小的值
     * 如果有有效值，则返回有效值，如果没有有效值，则返回-1
     */
    private fun getMinValue(list: List<Float>?): Float {
        var min = INVALID_VALUE
        list?.let {
            //赋初始值
            for (x in it) {
                if (x < min && x > 0) {
                    min = x
                }
            }
        }
        return if (min == INVALID_VALUE) {
            RADAR_INVALID_VALUE
        } else {
            min
        }
    }

    /**
     * 通过距离获取雷达字体颜色
     * 只处理0-5 5-无限 颜色
     */
    @ColorInt
    private fun getRadarTextColor(float: Float): Int {
        context?.let {
            if (float > 0 && float <= ALERT_READ_LIMIT) {
                return it.getColor(R.color.common_color_red)
            } else if (float > ALERT_READ_LIMIT) {
                return it.getColor(R.color.common_color_FEE15D)
            }
        }
        return -1
    }

    /**
     * 根据告警值获取背景
     * @param float 告警值
     * @param isCorner  true 带弯角，false 不带弯角
     * @param isSide  true 是否是边，false 不是
     * @param isInside true 上下方向 false 其他方向
     */
    @DrawableRes
    private fun getRadarBgRes(float: Float?, isCorner: Boolean = false, isSide: Boolean = false, isInside: Boolean = false): Int {
        var resId = -1
        if (context == null || float == null) {
            return resId
        }
        if (float > 0 && float <= ALERT_SERIOUS_LIMIT) {
            if (isCorner) {
                resId = R.drawable.mission_icon_radar_warn_crimson_edge
            } else {
                if (isSide) {
                    resId = R.drawable.mission_icon_radar_warn_crimson_side
                } else {
                    if (isInside) {
                        resId = R.drawable.mission_icon_radar_warn_crimson_center
                    } else {
                        resId = R.drawable.mission_icon_radar_warn_crimson_middle
                    }
                }
            }
        } else if (float > ALERT_SERIOUS_LIMIT && float <= ALERT_READ_LIMIT) {
            if (isCorner) {
                resId = R.drawable.mission_icon_radar_warn_red_edge
            } else {
                if (isSide) {
                    resId = R.drawable.mission_icon_radar_warn_red_side
                } else {
                    if (isInside) {
                        resId = R.drawable.mission_icon_radar_warn_red_center
                    } else {
                        resId = R.drawable.mission_icon_radar_warn_red_middle
                    }
                }
            }
        } else if (float > ALERT_READ_LIMIT) {
            if (isCorner) {
                resId = R.drawable.mission_icon_radar_warn_yellow_edge
            } else {
                if (isSide) {
                    resId = R.drawable.mission_icon_radar_warn_yellow_side
                } else {
                    if (isInside) {
                        resId = R.drawable.mission_icon_radar_warn_yellow_center
                    } else {
                        resId = R.drawable.mission_icon_radar_warn_yellow_middle
                    }
                }
            }
        }
        //如果float大于告警距离，则直接为-1校正值
        if (float > getHorizontalWarningDistance()) {
            resId = -1
        }
        return resId
    }

    /**
     * 是否显示雷达图
     * @param enable true 显示雷达图 false 隐藏雷达图
     */
    private fun showRadar(enable: Boolean) {
        topBottomModelMap.clear()
        directionModelMap.clear()
        binding.layoutFront.isInvisible = !enable
        binding.layoutRear.isInvisible = !enable
        binding.layoutLeft.isInvisible = !enable
        binding.layoutRight.isInvisible = !enable
        binding.layoutUp.isInvisible = !enable
        binding.layoutBottom.isInvisible = !enable

        binding.tvFront.isInvisible = !enable
        binding.tvRear.isInvisible = !enable
        binding.tvLeft.isInvisible = !enable
        binding.tvRight.isInvisible = !enable
        //如果隐藏雷达图，则关闭声音,清空缓存
        if (!enable) {
            stopPlayWarn()
            visionRadarCountMap.clear()
        }
    }

    /**
     * 显示距离，保留两位小数
     * 单位暂时为公制
     */
    private fun getFormatString(positionEnum: VisionSensorPositionEnum, value: Float): String {
        return when (positionEnum) {
            VisionSensorPositionEnum.FRONT -> getString(R.string.common_text_radar_front, TransformUtils.getLengthWithUnit(value.toDouble()))
            VisionSensorPositionEnum.REAR -> getString(R.string.common_text_radar_back, TransformUtils.getLengthWithUnit(value.toDouble()))
            VisionSensorPositionEnum.BOTTOM -> getString(R.string.common_text_radar_bottom, TransformUtils.getLengthWithUnit(value.toDouble()))
            VisionSensorPositionEnum.RIGHT -> "${TransformUtils.getLengthWithUnit(value.toDouble())}"
            VisionSensorPositionEnum.LEFT -> "${TransformUtils.getLengthWithUnit(value.toDouble())}"
            VisionSensorPositionEnum.TOP -> getString(R.string.common_text_radar_top, TransformUtils.getLengthWithUnit(value.toDouble()))
            else -> ""
        }
    }

    /**
     * 获取声音等级
     */
    private fun getWarnLevel(float: Float): Int {
        var level = if (float > 0 && float <= ALERT_SERIOUS_LIMIT) {
            RADAR_LEVEL_3
        } else if (float > ALERT_SERIOUS_LIMIT && float <= ALERT_READ_LIMIT) {
            RADAR_LEVEL_2
        } else if (float > ALERT_READ_LIMIT) {
            RADAR_LEVEL_1
        } else {
            RADAR_LEVEL_NONE
        }
        //如果float大于告警距离，则直接为-1校正值
        if (float > getHorizontalWarningDistance()) {
            level = RADAR_LEVEL_NONE
        }
        return level
    }

    /**
     * 播放雷达警告声音
     */
    private fun startWarn(topLevel: Int) {
        try {
            if (!isUIVisible || !isRadarVoice || topLevel == RADAR_LEVEL_NONE) {
                stopPlayWarn()
                return
            }
            AutelLog.i(TAG, "startWarn -> isUIVisible:$isUIVisible isRadarVoice=$isRadarVoice curWarnLevel=$curWarnLevel")
            mRadarWarn.playWarn(topLevel)
        } catch (e: Exception) {
            AutelLog.i(TAG, "startWarn -> Exception:$e")
            e.printStackTrace()
        }
    }

    private fun stopPlayWarn() {
        mRadarWarn.stopWarn()
    }

    /**
     * 水平告警距离
     */
    private fun getHorizontalWarningDistance(): Int {
        return VersionRepository.horizontalRadarDistance.value ?: VersionRepository.DISTANCE_DEFAULT
    }

}