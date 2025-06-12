package com.autel.widget.widget.statusbar.bean

import android.content.Context
import android.os.Looper
import com.autel.common.utils.DeviceUtils
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.enums.AiServiceStatueEnum.AI_RECOGNITION
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.enums.AiServiceStatueEnum.INTELLIGENT_TRACKING
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.enums.DroneWorkModeEnum
import com.autel.ux.core.model.FlyModeEnum
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FscWightModel {

    private val _flightMode = MutableStateFlow<FlyModeEnum?>(null)
    val flightMode: Flow<FlyModeEnum?> = _flightMode

    private var context: Context? = null

    private val handler = android.os.Handler(Looper.getMainLooper()) {
        postNext()
        if (!DeviceUtils.isSingleControlDroneConnected()) {
            _flightMode.value = null
            return@Handler true
        }
        if (!DeviceUtils.isSingleControl()) {
            _flightMode.value = null
            return@Handler true
        }
        // 第一优先级 , 返航中, 降落中
        val controlledDrone = DeviceUtils.singleControlDrone()
        val droneWorkMode = controlledDrone?.getDeviceStateData()?.flightControlData?.droneWorkMode
        val aiFunctionEnable = controlledDrone?.getDeviceStateData()?.flightControlData?.aiEnableFunc
        /**
         * 当设置为true，优先显示AI识别的状态
         * */
        var isNeedShowAIStatus = false
        _flightMode.value = FlyModeEnum.DEFAULT
        droneWorkMode.let {
            when (it) {
                DroneWorkModeEnum.SMART_MODE_ORBIT_MODELING -> {
                    _flightMode.value = FlyModeEnum.SINGLE_SURROUND
                    return@Handler true
                }
                DroneWorkModeEnum.RETURN -> {
                    _flightMode.value = FlyModeEnum.RETURN_HOME
                    return@Handler true
                }
                DroneWorkModeEnum.LAND, DroneWorkModeEnum.LAND_MANUAL -> {
                    _flightMode.value = FlyModeEnum.LANDING
                    return@Handler true
                }
                DroneWorkModeEnum.WAYPOINT -> {
                    _flightMode.value = FlyModeEnum.POINT_TASK
                    return@Handler true
                }
                DroneWorkModeEnum.RECTANGLE -> {
                    _flightMode.value = FlyModeEnum.RECT_TASK
                    return@Handler true
                }
                DroneWorkModeEnum.POLYGON -> {
                    _flightMode.value = FlyModeEnum.POLYGON_TASK
                    return@Handler true
                }
                DroneWorkModeEnum.PTHOTOGRAPHY -> {
                    _flightMode.value = FlyModeEnum.OBLIQUE_TASK
                    return@Handler true
                }
                DroneWorkModeEnum.AIR_STRIP -> {
                    _flightMode.value = FlyModeEnum.BELT_TASK
                    return@Handler true
                }
                DroneWorkModeEnum.POLYGONAL_IMITATION -> {
                    _flightMode.value = FlyModeEnum.EARTH_IMITATING
                    return@Handler true
                }
                DroneWorkModeEnum.MONOMER_SURROUND -> {
                    _flightMode.value = FlyModeEnum.SINGLE_SURROUND
                    return@Handler true
                }
                DroneWorkModeEnum.DISARM -> {
                    isNeedShowAIStatus = true
                    _flightMode.value = FlyModeEnum.DEFAULT
                }
                DroneWorkModeEnum.INTEREST_POINT -> {
                    _flightMode.value = FlyModeEnum.QUICK_TASK
                    return@Handler true
                }
                DroneWorkModeEnum.TRACK -> {
                    _flightMode.value = FlyModeEnum.LOCK_TARGET
                    return@Handler true
                }
                DroneWorkModeEnum.INFRARED_TRACK -> {
                    _flightMode.value = FlyModeEnum.LOCK_TARGET
                    return@Handler true
                }
                DroneWorkModeEnum.SMART_MODE_KML -> {
                    _flightMode.value = FlyModeEnum.WAYLINE_MISSION
                    return@Handler true
                }
                DroneWorkModeEnum.MANUAL_NORMAL -> {
                    _flightMode.value = FlyModeEnum.DEFAULT
                    isNeedShowAIStatus = true
                }

                else -> {}
            }
        }

        aiFunctionEnable.let {
            when (it) {
                AI_RECOGNITION -> {
                    if (isNeedShowAIStatus) {
                        _flightMode.value = FlyModeEnum.AI
                    }
                    return@Handler true
                }
                INTELLIGENT_TRACKING -> {
                    if (isNeedShowAIStatus) {
                        _flightMode.value = FlyModeEnum.LOCK_TARGET
                    }
                    return@Handler true
                }
                else -> {}
            }
        }

        // 最后是手动飞行
        if (DeviceUtils.isSingleControlDroneConnected()) {
            _flightMode.value = FlyModeEnum.DEFAULT
            return@Handler true
        }
        _flightMode.value = null
        true
    }

    /**
     * 注册各种状态监听
     */
    fun onAttached(context: Context) {
        this.context = context
        handler.removeCallbacksAndMessages(null)
        handler.sendEmptyMessage(1)
    }

    /**
     * 取消各种状态监听
     */
    fun onDetached() {
        context = null
        handler.removeCallbacksAndMessages(null)
    }

    private fun postNext() {
        handler.removeCallbacksAndMessages(null)
        handler.sendEmptyMessageDelayed(1, 1000)
    }
}