package com.autel.setting.view

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import com.autel.common.base.BaseAircraftFragment
import com.autel.common.constant.AppTagConst.SettingTag
import com.autel.common.manager.AppInfoManager
import com.autel.common.sdk.ModelXDroneConst
import com.autel.common.sdk.business.SettingBatteryVM
import com.autel.common.utils.DeviceUtils
import com.autel.common.utils.NumberParseUtil
import com.autel.common.utils.TransformUtils
import com.autel.drone.sdk.vmodelx.dronestate.FlightControlData
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.bean.BatteryInfoBean
import com.autel.log.AutelLog
import com.autel.setting.R
import com.autel.setting.databinding.SettingFlyBatteryFragmentBinding
import com.autel.setting.widget.DoubleThumbBar
import java.util.*

/**
 * @Author create by LJ
 * @Date 2022/9/1 10:32
 * 飞机电量
 */
class SettingFlyBatteryFragment : BaseAircraftFragment() {
    private lateinit var binding: SettingFlyBatteryFragmentBinding
    private val settingBatteryVM: SettingBatteryVM by activityViewModels()
    private var isSuperCapEnable = false//快速换电开关是否打开

    companion object {
        const val TAG = "SettingFlyBatteryFragment"
        const val TEMP_LOW_SERIOUS = ModelXDroneConst.BATTERY_TEMPERATURE_EXTREMELY_MIN//严重低温阈值
        const val TEMP_LOW = ModelXDroneConst.BATTERY_TEMPERATURE_MIN//低温阈值
        const val TEMP_HEIGHT_SERIOUS = ModelXDroneConst.BATTERY_TEMPERATURE_MAX//严重高温阈值
        const val DISCHARGE_TIMES_MAX = 200//放电次数阈值
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = SettingFlyBatteryFragmentBinding.inflate(LayoutInflater.from(context))
        return binding.root
    }

    override fun onVisible() {
        super.onVisible()
        if (!DeviceUtils.isSingleControlDroneConnected()) {
            showToast(R.string.common_text_aircraft_disconnect)
            return
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        binding.batterySeekBar.postInvalidate()
    }

    override fun getData() {
        settingBatteryVM.getFlightParamsBatteryLowWarning()
        settingBatteryVM.getFlightParamsBatSeriousLowWarning()
        //快速换电需求不上
        if (AppInfoManager.isSupportQuickChangeBatter()) {
            settingBatteryVM.getFCSEnSuperCap({
                isSuperCapEnable = it
                binding.cisQuickPowerChange.setCheckedWithoutListener(isSuperCapEnable)
            }, {
                AutelLog.i(TAG, "getFCSEnSuperCap is Error $it")
            })
        }
        AutelLog.i(TAG, "getData -> mDroneType=${DeviceUtils.singleControlDrone()?.getDroneType()}")
    }

    @SuppressLint("SetTextI18n")
    override fun addListen() {
        //观察低电量
        settingBatteryVM.batteryLowWarnLD.observe(viewLifecycleOwner) {
            settingBatteryVM.batteryLowWarnLD.value?.let { secondValue ->
                settingBatteryVM.batterySeriousLowWarn.value?.let { firstValue ->
                    AutelLog.i(SettingTag, "batterySeriousLowWarn: $firstValue, batteryLowWarnLD: $secondValue")
                    binding.batterySeekBar.firstProgress = firstValue
                    binding.batterySeekBar.secondaryProgress = secondValue
                }
            }
        }

        //观察严重低电量
        settingBatteryVM.batterySeriousLowWarn.observe(viewLifecycleOwner) {
            settingBatteryVM.batteryLowWarnLD.value?.let { secondValue ->
                settingBatteryVM.batterySeriousLowWarn.value?.let { firstValue ->
                    AutelLog.i(SettingTag, "batterySeriousLowWarn: $firstValue, batteryLowWarnLD: $secondValue")
                    binding.batterySeekBar.firstProgress = firstValue
                    binding.batterySeekBar.secondaryProgress = secondValue
                }
            }
        }

        //观察飞机上报的消息,更新电池信息
        settingBatteryVM.droneSystemState.observe(viewLifecycleOwner) { bean ->
            refreshBatteryInfo(bean)
        }

        initView()
    }

    private fun initView() {
        //低电量滑动监听
        binding.batterySeekBar.setOnProgressListener(object : DoubleThumbBar.OnProgressListener {
            override fun onProgressChanged(firstProgress: Int, secondProgress: Int) {
                setLowWarning(secondProgress)
                setSeriousLowWarning(firstProgress)
            }
        })

        binding.cisQuickPowerChange.setOnSwitchChangeListener {
            settingBatteryVM.setFCSEnSuperCap(it, {
                isSuperCapEnable = it
            }, {
                binding.cisQuickPowerChange.setCheckedWithoutListener(isSuperCapEnable)
                showToast(R.string.common_text_set_failed)
            })
        }

        binding.cisQuickPowerChange.isVisible = AppInfoManager.isSupportQuickChangeBatter()
        //快速换电开关监听
        binding.cisQuickPowerChange.setOnSwitchChangeListener {
            settingBatteryVM.setFCSEnSuperCap(it, {
                isSuperCapEnable = it
            }, {
                binding.cisQuickPowerChange.setCheckedWithoutListener(isSuperCapEnable)
                showToast(R.string.common_text_set_failed)
            })
        }

        //如果飞机未连接，则重置数据
        if (!DeviceUtils.isSingleControlDroneConnected()) {
            resetStatus()
        }
    }

    /**设置严重低电量告警阈值，重试3次*/
    private fun setSeriousLowWarning(progress: Int, retryCount: Int = 3) {
        settingBatteryVM.setFlightParamsBatSeriousLowWarning(progress, {
            AutelLog.i(TAG, "setSeriousLowWarning Success, progress: $progress")
        }, { e ->
            binding.batterySeekBar.firstProgress = settingBatteryVM.getCriticalLowBatteryData()
            AutelLog.i(TAG, "setSeriousLowWarning is Error $e")
        })
    }

    /**设置低电量告警阈值，重试3次*/
    private fun setLowWarning(progress: Int, retryCount: Int = 3) {
        settingBatteryVM.setFlightParamsBatteryLowWarning(progress, {
            AutelLog.i(TAG, "setLowWarning Success, progress: $progress")
        }, { e ->
            binding.batterySeekBar.secondaryProgress = settingBatteryVM.getLowBatteryData()
            AutelLog.i(TAG, "setLowWarning is Error $e")
        })
    }

    override fun onDroneChangedListener(connected: Boolean, drone: IAutelDroneDevice) {
        super.onDroneChangedListener(connected, drone)
        if (!connected && drone == DeviceUtils.singleControlDrone()) {
            resetStatus()
        }
    }

    /**
     * 重置状态
     */
    private fun resetStatus() {
        binding.tvEstimatedFlightTime.text = getString(R.string.common_text_no_value)
        binding.tvEstimatedFlightTime.setTextColor(resources.getColor(R.color.common_color_bd))

        binding.tvBatteryPercentage.text = getString(R.string.common_text_no_value)
        binding.tvBatteryPercentageAll.text = getString(R.string.common_text_no_value)
        binding.tvBatteryTemperature.text = getString(R.string.common_text_no_value)
        binding.tvBatteryVoltage.text = getString(R.string.common_text_no_value)
        binding.tvBatteryDischargeTimes.text = getString(R.string.common_text_no_value)
        binding.tvBatteryPercentage.setTextColor(resources.getColor(R.color.common_color_bd))
        binding.tvBatteryPercentageAll.setTextColor(resources.getColor(R.color.common_color_bd))

        binding.tvBatteryTemperature.setTextColor(resources.getColor(R.color.common_color_bd))
        binding.tvBatteryVoltage.setTextColor(resources.getColor(R.color.common_color_bd))
        binding.tvBatteryDischargeTimes.setTextColor(resources.getColor(R.color.common_color_bd))

        binding.tvBatteryState.setTextColor(resources.getColor(R.color.common_color_bd))
        binding.tvBatteryState.text = getString(R.string.common_text_no_value)

        binding.line1.isVisible = false
        binding.rlState1.isVisible = false
        binding.llStateInfo1.isVisible = false
    }

    /**
     * 刷新电池信息
     */
    private fun refreshBatteryInfo(bean: FlightControlData) {
        //当为多电池位时
        binding.line1.isVisible = bean.batteryModeFlag == 1
        binding.rlState1.isVisible = bean.batteryModeFlag == 1
        binding.llStateInfo1.isVisible = bean.batteryModeFlag == 1

        //多电池志位 为1是为多电池位，其他为单电池位
        if (bean.batteryModeFlag == 1) {
            dealMultipleBattery(bean)
        } else {
            dealSingleBattery(bean)
        }
    }

    /**
     * 处理多电池信息
     */
    private fun dealMultipleBattery(bean: FlightControlData) {
        context?.let {
            //电池方位
            val deviceType = DeviceUtils.singleControlDrone()?.getDroneType()?.value ?: ""
            val firstId = if ("ModelH".equals(deviceType, true)) R.string.common_text_battery_left else R.string.common_text_battery_front
            val secondId = if ("ModelH".equals(deviceType, true)) R.string.common_text_battery_right else R.string.common_text_battery_back
            binding.tvBatteryDirct.text = getString(firstId)
            binding.tvBatteryDirct1.text = getString(secondId)

            //检查电池插入情况
            checkMultipleBattery(bean.batteryInfoList)

            //不为空
            if (bean.batteryInfoList.isNotEmpty()) {
                for (x in bean.batteryInfoList) {
                    when (x.batteryId) {
                        0 -> dealMultipleFusionBattery(x)
                        1 -> dealMultipleFirstBattery(x)
                        2 -> dealMultipleSecondBattery(x)
                    }
                }
            }
        }
    }

    /**
     * 检查电池是否插入
     */
    private fun checkMultipleBattery(list: List<BatteryInfoBean>) {
        var hasFirst = false
        var hasSecond = false

        for (x in list) {
            if (x.batteryId == 1) hasFirst = true
            if (x.batteryId == 2) hasSecond = true
        }
        if (!hasFirst) {
            context?.let {
                //状态
                binding.tvBatteryState.setTextColor(resources.getColor(R.color.common_color_d80000))
                binding.tvBatteryState.text = getString(R.string.common_text_camera_setting_sdcard_not_inserted)

                //电量
                binding.tvBatteryPercentage.text = getString(R.string.common_text_no_value)
                binding.tvBatteryPercentage.setTextColor(it.getColor(R.color.common_color_white))

                //温度
                binding.tvBatteryTemperature.text = getString(R.string.common_text_no_value)
                binding.tvBatteryTemperature.setTextColor(it.getColor(R.color.common_color_white))

                //电压
                binding.tvBatteryVoltage.text = getString(R.string.common_text_no_value)
                binding.tvBatteryVoltage.setTextColor(it.getColor(R.color.common_color_white))

                //放电次数
                binding.tvBatteryDischargeTimes.text = getString(R.string.common_text_no_value)
                binding.tvBatteryDischargeTimes.setTextColor(it.getColor(R.color.common_color_white))
            }
        }
        if (!hasSecond) {
            context?.let {
                //状态
                binding.tvBatteryState1.setTextColor(resources.getColor(R.color.common_color_d80000))
                binding.tvBatteryState1.text = getString(R.string.common_text_camera_setting_sdcard_not_inserted)

                //电量
                binding.tvBatteryPercentage1.text = getString(R.string.common_text_no_value)
                binding.tvBatteryPercentage1.setTextColor(it.getColor(R.color.common_color_white))

                //温度
                binding.tvBatteryTemperature1.text = getString(R.string.common_text_no_value)
                binding.tvBatteryTemperature1.setTextColor(it.getColor(R.color.common_color_white))

                //电压
                binding.tvBatteryVoltage1.text = getString(R.string.common_text_no_value)
                binding.tvBatteryVoltage1.setTextColor(it.getColor(R.color.common_color_white))

                //放电次数
                binding.tvBatteryDischargeTimes1.text = getString(R.string.common_text_no_value)
                binding.tvBatteryDischargeTimes1.setTextColor(it.getColor(R.color.common_color_white))
            }
        }
    }

    /**
     * 处理多电池位第二个电池
     */
    private fun dealMultipleSecondBattery(bean: BatteryInfoBean) {
        context?.let {
            //电量
            binding.tvBatteryPercentage1.text = "${bean.batteryPercentage.toInt()}%"
            binding.tvBatteryPercentage1.setTextColor(it.getColor(getBatteryColor(bean.batteryPercentage.toInt())))

            //温度
            binding.tvBatteryTemperature1.text = TransformUtils.centigrade2Defalut(bean.batteryTemperature)
            binding.tvBatteryTemperature1.setTextColor(it.getColor(getTempColor(bean.batteryTemperature)))

            //电压
            binding.tvBatteryVoltage1.text = "${NumberParseUtil.formatFloat(bean.batteryVoltage, 1)}V"
            binding.tvBatteryVoltage1.setTextColor(it.getColor(getVoltageColor(bean.batteryVoltage)))

            //放电次数
            val numberOfDischarge = bean.numberOfDischarge
            binding.tvBatteryDischargeTimes1.text = "$numberOfDischarge"
            binding.tvBatteryDischargeTimes1.setTextColor(it.getColor(getDischargeTimesColor(numberOfDischarge)))

            //显示温度电压异常状态
            if (bean.batteryTemperature <= TEMP_LOW_SERIOUS
                || bean.batteryTemperature >= TEMP_HEIGHT_SERIOUS
                || bean.batteryVoltage < ModelXDroneConst.getVoltageLow(DeviceUtils.singleControlDrone()?.getDroneType()?.value.orEmpty())
            ) {
                if (DeviceUtils.isSingleControlDroneConnected()) {
                    binding.tvBatteryState1.setTextColor(resources.getColor(R.color.common_color_d80000))
                    binding.tvBatteryState1.text = getString(R.string.common_text_battery_text_exception)
                } else {
                    binding.tvBatteryState1.setTextColor(resources.getColor(R.color.common_color_bd))
                    binding.tvBatteryState1.text = getString(R.string.common_text_no_value)
                }

            } else {
                binding.tvBatteryState1.setTextColor(resources.getColor(R.color.common_battery_setting_safe))
                binding.tvBatteryState1.text = getString(R.string.common_text_battery_text_norml)
            }
        }
    }

    /**
     * 处理多电池位第一个电池
     */
    private fun dealMultipleFirstBattery(bean: BatteryInfoBean) {
        context?.let {
            //电量
            binding.tvBatteryPercentage.text = "${bean.batteryPercentage.toInt()}%"
            binding.tvBatteryPercentage.setTextColor(it.getColor(getBatteryColor(bean.batteryPercentage.toInt())))

            //温度
            binding.tvBatteryTemperature.text = TransformUtils.centigrade2Defalut(bean.batteryTemperature)
            binding.tvBatteryTemperature.setTextColor(it.getColor(getTempColor(bean.batteryTemperature)))

            //电压
            binding.tvBatteryVoltage.text = "${NumberParseUtil.formatFloat(bean.batteryVoltage, 1)}V"
            binding.tvBatteryVoltage.setTextColor(it.getColor(getVoltageColor(bean.batteryVoltage)))

            //放电次数
            val numberOfDischarge = bean.numberOfDischarge
            binding.tvBatteryDischargeTimes.text = "$numberOfDischarge"
            binding.tvBatteryDischargeTimes.setTextColor(it.getColor(getDischargeTimesColor(numberOfDischarge)))

            //显示温度电压异常状态
            if (bean.batteryTemperature <= TEMP_LOW_SERIOUS
                || bean.batteryTemperature >= TEMP_HEIGHT_SERIOUS
                || bean.batteryVoltage < ModelXDroneConst.getVoltageLow(DeviceUtils.singleControlDrone()?.getDroneType()?.value.orEmpty())
            ) {
                if (DeviceUtils.isSingleControlDroneConnected()) {
                    binding.tvBatteryState.setTextColor(resources.getColor(R.color.common_color_d80000))
                    binding.tvBatteryState.text = getString(R.string.common_text_battery_text_exception)
                } else {
                    binding.tvBatteryState.setTextColor(resources.getColor(R.color.common_color_bd))
                    binding.tvBatteryState.text = getString(R.string.common_text_no_value)
                }

            } else {
                binding.tvBatteryState.setTextColor(resources.getColor(R.color.common_battery_setting_safe))
                binding.tvBatteryState.text = getString(R.string.common_text_battery_text_norml)
            }
        }
    }

    /**
     * 处理融合电池信息
     */
    private fun dealMultipleFusionBattery(bean: BatteryInfoBean) {
        context?.let {
            //总电量
            binding.tvBatteryPercentageAll.text = "${bean.batteryPercentage.toInt()}%"
            binding.tvBatteryPercentageAll.setTextColor(it.getColor(getBatteryColor(bean.batteryPercentage.toInt())))

            //预计飞行时间
            val remainFlightTime = bean.remainingFlightTime.toInt()
            if (DeviceUtils.isSingleControlDroneConnected() && remainFlightTime >= 0) {
                binding.tvEstimatedFlightTime.text = String.format(Locale.ENGLISH, "%02d'%02d''", remainFlightTime / 60, remainFlightTime % 60)
            } else {
                binding.tvEstimatedFlightTime.text = getString(R.string.common_text_no_value)
            }
        }
    }

    /**
     * 处理单电池信息
     */
    private fun dealSingleBattery(bean: FlightControlData) {
        context?.let {
            //电池方位
            binding.tvBatteryDirct.text = getString(R.string.common_text_right_radio_btn_battery)

            //总电量
            binding.tvBatteryPercentageAll.text = "${bean.batteryPercentage.toInt()}%"
            binding.tvBatteryPercentageAll.setTextColor(it.getColor(getBatteryColor(bean.batteryPercentage.toInt())))

            //预计飞行时间
            val remainFlightTime = bean.remainingFlightTime.toInt()
            if (DeviceUtils.isSingleControlDroneConnected() && remainFlightTime >= 0) {
                binding.tvEstimatedFlightTime.text = String.format(Locale.ENGLISH, "%02d'%02d''", remainFlightTime / 60, remainFlightTime % 60)
            } else {
                binding.tvEstimatedFlightTime.text = getString(R.string.common_text_no_value)
            }

            //电量
            binding.tvBatteryPercentage.text = "${bean.batteryPercentage.toInt()}%"
            binding.tvBatteryPercentage.setTextColor(it.getColor(getBatteryColor(bean.batteryPercentage.toInt())))

            //温度
            binding.tvBatteryTemperature.text = TransformUtils.centigrade2Defalut(bean.batteryTemperature)
            binding.tvBatteryTemperature.setTextColor(it.getColor(getTempColor(bean.batteryTemperature)))

            //电压
            binding.tvBatteryVoltage.text = "${NumberParseUtil.formatFloat(bean.batteryVoltage, 1)}V"
            binding.tvBatteryVoltage.setTextColor(it.getColor(getVoltageColor(bean.batteryVoltage)))

            //放电次数
            val numberOfDischarge = bean.numberOfDischarge
            binding.tvBatteryDischargeTimes.text = "$numberOfDischarge"
            binding.tvBatteryDischargeTimes.setTextColor(it.getColor(getDischargeTimesColor(numberOfDischarge)))

            //显示温度电压异常状态
            if (bean.batteryTemperature <= TEMP_LOW_SERIOUS
                || bean.batteryTemperature >= TEMP_HEIGHT_SERIOUS
                || bean.batteryVoltage < ModelXDroneConst.getVoltageLow(DeviceUtils.singleControlDrone()?.getDroneType()?.value.orEmpty())
            ) {
                if (DeviceUtils.isSingleControlDroneConnected()) {
                    binding.tvBatteryState.setTextColor(resources.getColor(R.color.common_color_d80000))
                    binding.tvBatteryState.text = getString(R.string.common_text_battery_text_exception)
                } else {
                    binding.tvBatteryState.setTextColor(resources.getColor(R.color.common_color_bd))
                    binding.tvBatteryState.text = getString(R.string.common_text_no_value)
                }

            } else {
                binding.tvBatteryState.setTextColor(resources.getColor(R.color.common_battery_setting_safe))
                binding.tvBatteryState.text = getString(R.string.common_text_battery_text_norml)
            }
        }
    }

    /**
     * 获取电量颜色
     */
    private fun getBatteryColor(percent: Int): Int {
        if (settingBatteryVM.getLowBatteryData() == 0 || settingBatteryVM.getCriticalLowBatteryData() == 0) {
            return R.color.common_color_bd
        }
        if (percent <= settingBatteryVM.getCriticalLowBatteryData()) {
            return R.color.common_color_d80000
        }
        if (percent <= settingBatteryVM.getLowBatteryData()) {
            return R.color.common_color_secondary_fa6400
        }
        return R.color.common_battery_setting_safe
    }

    /**
     * 获取温度颜色
     */
    private fun getTempColor(temp: Float): Int {
        if (temp <= TEMP_LOW_SERIOUS || temp >= TEMP_HEIGHT_SERIOUS) {
            return R.color.common_color_d80000
        }
        if (temp <= TEMP_LOW) {
            return R.color.common_color_secondary_fa6400
        }
        return R.color.common_color_white
    }

    /**
     * 获取电压颜色
     */
    private fun getVoltageColor(v: Float): Int {
        return if (v < ModelXDroneConst.getVoltageLow(
                DeviceUtils.singleControlDrone()?.getDroneType()?.value.orEmpty()
            )
        ) R.color.common_color_d80000 else R.color.common_color_white
    }

    /**
     * 获取放电次数颜色
     */
    private fun getDischargeTimesColor(times: Int): Int {
        if (times >= DISCHARGE_TIMES_MAX) {
            return R.color.common_color_d80000
        }
        return R.color.common_color_white
    }
}