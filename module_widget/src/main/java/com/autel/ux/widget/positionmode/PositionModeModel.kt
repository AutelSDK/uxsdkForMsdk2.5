package com.autel.ux.widget.positionmode

import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.CommonKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.ext.create
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.enums.FlightControlMainModeEnum
import com.autel.ux.core.base.AutelSDKModel
import com.autel.ux.core.base.WidgetModel
import com.autel.ux.core.utils.DataProcessor

class PositionModeModel(autelSdkModel: AutelSDKModel) : WidgetModel(autelSdkModel) {

    private val flightControlMainModeProcessor = DataProcessor.create(FlightControlMainModeEnum.UNKNOWN)

    val flightControlMainMode = flightControlMainModeProcessor.toFlow()

    override fun inSetupWithDrone() {
        bindDataProcessor(CommonKey.KeyDroneSystemStatusHFNtfy.create(), flightControlMainModeProcessor, { it.mainMode })
    }

    override fun inCleanup() {
        TODO("Not yet implemented")
    }
}