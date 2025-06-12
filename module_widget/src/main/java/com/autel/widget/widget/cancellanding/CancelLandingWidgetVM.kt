package com.autel.widget.widget.cancellanding

import com.autel.common.base.widget.BaseWidgetModel
import com.autel.common.sdk.service.SettingService
import com.autel.common.utils.DeviceUtils
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.enums.DroneWorkModeEnum
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow

class CancelLandingWidgetVM : BaseWidgetModel() {
    val cancelLandingData =
        MutableSharedFlow<CancelLandingStateModel>(replay = 1, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override fun fixedFrequencyRefresh() {
        if (!DeviceUtils.isMainRC()) {
            return
        }

        /**
         * 只要存在可返航、可降落的飞机，就显示取消返航和降落的按钮
         * 同时记录所有可取消降落的飞机的设备ID
         * */
        var showCancelReturn = false
        var showCancelLand = false

        val landDrones = mutableListOf<IAutelDroneDevice>()
        val returnDrones = mutableListOf<IAutelDroneDevice>()
        DeviceUtils.allControlDrones().forEach {
            if (it.isConnected()) {
                val data = it.getDeviceStateData()
                val batteryPercentage = data.flightControlData.batteryPercentage
                val batSeriousLowWarningValue = data.flightoperateData.iBatSeriousLowWarningValue
                val isSupport = batteryPercentage > batSeriousLowWarningValue
                val workMode = data.flightControlData.droneWorkMode
                val deviceId = DeviceUtils.droneDeviceId(it)
                if (workMode == DroneWorkModeEnum.LAND && isSupport) {
                    showCancelLand = true
                    if (!(landDrones.any { droneDevice -> DeviceUtils.droneDeviceId(droneDevice) == deviceId })) {
                        landDrones.add(it)
                    }
                }
                if (workMode == DroneWorkModeEnum.RETURN && isSupport) {
                    showCancelReturn = true
                    if (!(returnDrones.any { droneDevice -> DeviceUtils.droneDeviceId(droneDevice) == deviceId })) {
                        returnDrones.add(it)
                    }
                }
            }
        }
        val cancelLandingModel = CancelLandingStateModel(showCancelReturn, showCancelLand, returnDrones, landDrones)
        if (cancelLandingData.replayCache.firstOrNull() != cancelLandingModel) {
            cancelLandingData.tryEmit(cancelLandingModel)
        }
    }

    fun cancelLanding(drones: List<IAutelDroneDevice>, onSuccess: ((IAutelDroneDevice) -> Unit)) {
        drones.forEach {
            SettingService.getInstance().flightControlService.landing(it, false, {
                onSuccess.invoke(it)
            }, {})
        }
    }

    fun cancelReturn(drones: List<IAutelDroneDevice>, onSuccess: ((IAutelDroneDevice) -> Unit)) {
        drones.forEach {
            SettingService.getInstance().flightControlService.landing(it, false, {
                onSuccess.invoke(it)
            }, {})
        }
    }

}