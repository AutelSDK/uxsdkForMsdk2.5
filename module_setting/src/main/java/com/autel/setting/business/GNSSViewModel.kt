package com.autel.setting.business

import com.autel.common.constant.AppTagConst
import com.autel.common.constant.SharedParams
import com.autel.common.model.function.FunctionBaseViewModel
import com.autel.drone.sdk.libbase.error.IAutelCode
import com.autel.drone.sdk.vmodelx.device.IAutelDroneListener
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.interfaces.IControlDroneListener
import com.autel.drone.sdk.vmodelx.manager.keyvalue.callback.CommonCallbacks
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.FlightPropertyKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.base.KeyTools
import com.autel.log.AutelLog

class GNSSViewModel : FunctionBaseViewModel(), IControlDroneListener, IAutelDroneListener {

    override fun setDroneOpen(droneDevice: IAutelDroneDevice, open: Boolean) {
        val key = KeyTools.createKey(FlightPropertyKey.KeyFcsEnGpsMode)
        droneDevice.getKeyManager().setValue(key, open, object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                SharedParams._gnssSwitch.postValue(open)
                onSetSuccess(droneDevice, open)
            }

            override fun onFailure(error: IAutelCode, msg: String?) {
                AutelLog.e(AppTagConst.GnssTag, "setDroneGNSSStatus onFailure code:$error msg:$msg")
            }
        })
    }


    override fun getOpenStatus(droneDevice: IAutelDroneDevice, success: (Boolean) -> Unit, failure: (Throwable) -> Unit) {
        val key = KeyTools.createKey(FlightPropertyKey.KeyFcsEnGpsMode)
        droneDevice.getKeyManager().getValue(key, object: CommonCallbacks.CompletionCallbackWithParam<Boolean> {
            override fun onSuccess(t: Boolean?) {
                AutelLog.i(AppTagConst.GnssTag, "getDroneGNSSStatus onSuccess: $t -->DeviceId:${droneDevice.getDeviceNumber()}")
                if (t != null) {
                    onGetSuccess(droneDevice, t)
                    SharedParams._gnssSwitch.postValue(t)
                    success.invoke(t)
                } else {
                    failure.invoke(Throwable("onSuccess but value is null"))
                }
            }

            override fun onFailure(error: IAutelCode, msg: String?) {
                failure.invoke(Throwable("onFailure code:$error msg:$msg"))
            }

        })
    }

}