package com.autel.widget.widget.statusbar.wm

import com.autel.common.utils.DeviceUtils
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.common.bean.WarningAtom

data class DroneWarningAtom(
    var drone: IAutelDroneDevice?,
    var warningAtom: WarningAtom?
) {
    override fun toString(): String {
        return "DroneWarningAtom" + DeviceUtils.droneDeviceName(drone) + "warningAtom:" + warningAtom
    }
}
