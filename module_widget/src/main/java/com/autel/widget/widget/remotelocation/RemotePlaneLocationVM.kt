package com.autel.widget.widget.remotelocation

import com.autel.common.base.widget.BaseWidgetModel
import com.autel.common.feature.phone.AutelPhoneLocationManager
import com.autel.common.utils.LatLngUtil
import com.autel.drone.sdk.vmodelx.device.ModelXDevice
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow

class RemotePlaneLocationVM : BaseWidgetModel() {

    private var drone: IAutelDroneDevice? = null

    val remotePlaneLocationFlow = MutableSharedFlow<RemotePlaneLocationModel>(replay = 1, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)


    override fun fixedFrequencyRefresh() {
        val device = drone ?: return
        var remoteLocation = arrayOf<String>("", "")
        var remoteIsValid = false
        AutelPhoneLocationManager.locationLiveData.value?.let {
            remoteLocation = LatLngUtil.getLatLngWithUnit(it.longitude, it.latitude)
            remoteIsValid = true
        }


        device as ModelXDevice
        val data = device.getDeviceStateData().flightControlData
        var droneIsValid = !(data.droneLatitude == 0.0 && data.droneLongitude == 0.0)

        val droneLocation =
            LatLngUtil.getLatLngWithUnit(data.droneLongitude, data.droneLatitude)

        val value = RemotePlaneLocationModel(
            droneIsValid,
            droneLocation[0],
            droneLocation[1],
            remoteIsValid,
            remoteLocation[0],
            remoteLocation[1]
        )
        if (remotePlaneLocationFlow.replayCache.firstOrNull() != value) {
            remotePlaneLocationFlow.tryEmit(value)
        }
    }

    fun updateDevice(drone: IAutelDroneDevice?) {
        this.drone = drone
    }
}