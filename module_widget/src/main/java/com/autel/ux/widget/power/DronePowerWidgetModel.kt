package com.autel.ux.widget.power

import com.autel.drone.sdk.vmodelx.device.statenew.key.BatteryKey
import com.autel.drone.sdk.vmodelx.dronestate.FlightOperateData
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.FlightControlKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.FlightPropertyKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.ext.create
import com.autel.ux.core.base.AutelSDKModel
import com.autel.ux.core.base.WidgetModel
import com.autel.ux.core.utils.DataProcessor

class DronePowerWidgetModel(private val sdkModel: AutelSDKModel) : WidgetModel(sdkModel) {

    private val dronePowerProcessor = DataProcessor.create(-1)
    private val droneCriticalLowBatteryProcessor = DataProcessor.create(15)
    private val droneLowBatteryProcessor = DataProcessor.create(20)

    val dronePower = dronePowerProcessor.toFlow()

    override fun inSetupWithDrone() {
        bindDataProcessor(BatteryKey.KeyChargeRemainingInPercent.create(),dronePowerProcessor)
//        bindDataProcessor(FlightPropertyKey.)

        productConnected.collectInModel {
            if (!it){
                dronePowerProcessor.emit(-1)
            }
        }
    }

    override fun inCleanup() {

    }


}