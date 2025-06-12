package com.autel.widget.widget.statusbar.wm

import androidx.lifecycle.MutableLiveData
import com.autel.common.base.widget.BaseWidgetModel
import com.autel.common.sdk.GpsSignalLevelEnum
import com.autel.common.sdk.RemoteSignalLevelEnum
import com.autel.common.utils.UIConstants
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.camera.enums.CardStatusEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.enums.DroneFlightModeEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.enums.DroneWorkModeEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.enums.EnvironmentEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.enums.FlightControlMainModeEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.enums.GearLevelEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.rtk.enums.RTKPositionTypeEnum
import com.autel.widget.widget.statusbar.bean.BatteryInfo
import com.autel.widget.widget.statusbar.bean.RemoteBattery
import com.autel.widget.widget.statusbar.bean.SignalStrength
import kotlinx.coroutines.flow.MutableStateFlow

abstract class StatusBarWidget : BaseWidgetModel() {

    /**
     * 避障相关信息
     */
    val visionStateInfo = MutableStateFlow(false)

    /**
     * 飞机是否已连接
     * 组网下为中继飞机是否已连接
     */
    val droneConnectStatus: MutableStateFlow<Boolean?> = MutableStateFlow(null)

    val droneWorkMode: MutableStateFlow<DroneWorkModeEnum?> = MutableStateFlow(null)

    val mainMode: MutableStateFlow<FlightControlMainModeEnum?> = MutableStateFlow(null)

    val slamConfidence: MutableStateFlow<Double?> = MutableStateFlow(null)

    val flightMode: MutableStateFlow<DroneFlightModeEnum?> = MutableStateFlow(null)

    val rtkSVCount: MutableStateFlow<Int?> = MutableStateFlow(null)
    val rtkFixStatus: MutableStateFlow<Int?> = MutableStateFlow(null)
    val rtkPosType: MutableStateFlow<RTKPositionTypeEnum> = MutableStateFlow(RTKPositionTypeEnum.UNKNOWN_POSITION)
    /**是否开启RTK定位*/
    val rtkEnable: MutableStateFlow<Boolean> = MutableStateFlow(false)
    /**是否插入RTK*/
    val rtkSupport: MutableStateFlow<Boolean> = MutableStateFlow(false)
    /**
     * 飞机档位
     */
    val droneGear = MutableStateFlow(GearLevelEnum.UNKNOWN)
    /**
     * 单机模式（非全控、群控、未知）
     * */
    val singleControlMode: MutableStateFlow<Boolean?> = MutableStateFlow(null)

    /**
     * 信号强度信息
     */
    val signalStrength = MutableStateFlow(SignalStrength(GpsSignalLevelEnum.GPS_SIGNAL_LEVEL_NONE, 0, RemoteSignalLevelEnum.REMOTE_SIGNAL_LEVEL_NONE))

    /**
     * 电量信息，飞机与遥控器
     */
    val batteryInfo =
        MutableStateFlow(BatteryInfo("", 0, UIConstants.DEFAULT_CRITICAL_LOW_BATTERY, UIConstants.DEFAULT_LOW_BATTERY, 0f, 0f))

    val remoteBattery = MutableStateFlow(RemoteBattery(100, 100))
    /**
     * 飞机环境亮度
     */
    val environmentInfo = MutableStateFlow(EnvironmentEnum.NORMAL_BRIGHTNESS)

    /**
     * SD卡状态
     */
    val cardStatusInfo = MutableStateFlow(CardStatusEnum.UNKNOWN)


    /**
     * warningAtomList:告警列表
     * */
    val warningAtomList: MutableLiveData<MutableList<DeviceWarnAtomList>?> = MutableLiveData(null)

    val deviceName: MutableStateFlow<String?> = MutableStateFlow(null)

    val isMainRc: MutableStateFlow<Boolean?> = MutableStateFlow(null)

}