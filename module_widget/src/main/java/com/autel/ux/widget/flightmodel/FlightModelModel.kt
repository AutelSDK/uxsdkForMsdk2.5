package com.autel.ux.widget.flightmodel

import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.CommonKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.ext.create
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.enums.AiServiceStatueEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.enums.DroneWorkModeEnum
import com.autel.ux.core.base.AutelSDKModel
import com.autel.ux.core.base.WidgetModel
import com.autel.ux.core.utils.DataProcessor

/**
 *
 */
class FlightModelModel(private val sdkModel: AutelSDKModel) : WidgetModel(sdkModel) {

    private val flightModeProcessor = DataProcessor.create(DroneWorkModeEnum.UNKNOWN)
    private val aiServiceStatueProcessor = DataProcessor.create(AiServiceStatueEnum.UNKNOWN)

    val flightMode = flightModeProcessor.toFlow()
    val aiServiceStatus = aiServiceStatueProcessor.toFlow()

    override fun inSetupWithDrone() {
        bindDataProcessor(CommonKey.KeyDroneSystemStatusHFNtfy.create(), flightModeProcessor, { it.droneWorkMode })
        bindDataProcessor(CommonKey.KeyDroneSystemStatusHFNtfy.create(), aiServiceStatueProcessor, { it.aiEnableFunc })
    }

    override fun inCleanup() {

    }

}