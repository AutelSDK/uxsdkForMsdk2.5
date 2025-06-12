package com.autel.widget.widget.statusbar.wm

import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.interfaces.IAutelRemoteDevice
import com.autel.drone.sdk.vmodelx.interfaces.IBaseDevice
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.common.bean.WarningAtom
import java.util.Objects

data class DeviceWarnAtomList(
    var drone: IBaseDevice?,
    var warningAtomList: List<WarningAtom>?,
) {
    override fun toString(): String {
        return when (drone) {
            is IAutelDroneDevice -> {
                "DeviceWarnAtomList" + (drone as IAutelDroneDevice).getDeviceNumber() + "warningAtomList:" + warningAtomList
            }

            is IAutelRemoteDevice -> {
                "DeviceWarnAtomList" + (drone as IAutelRemoteDevice).getDeviceNumber() + "warningAtomList:" + warningAtomList
            }

            else -> {
                "warningAtomList:$warningAtomList"
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (drone != (other as DeviceWarnAtomList).drone) return false
        if (drone != null) {
            if (drone is IAutelDroneDevice && other.drone is IAutelDroneDevice) {
                val curDrone = drone as? IAutelDroneDevice
                val otherDrone = other.drone as? IAutelDroneDevice
                // 有些告警是需要根据飞行状态区分的,所以这里需要添加飞行模式判断
                if (curDrone?.getDeviceStateData()?.flightControlData?.flightMode != otherDrone?.getDeviceStateData()?.flightControlData?.flightMode) {
                    return false
                }
            }
        }
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return Objects.hash(drone, warningAtomList)
    }
}
