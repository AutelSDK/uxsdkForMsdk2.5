package com.autel.ux.widget.obstacleavoidance

import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.CommonKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.FlightControlKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.ext.create
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.enums.GearLevelEnum
import com.autel.ux.core.base.AutelSDKModel
import com.autel.ux.core.base.WidgetModel
import com.autel.ux.core.utils.DataProcessor

class ObstacleAvoidanceModel(sdkModel: AutelSDKModel) : WidgetModel(sdkModel) {

    private val obstacleAvoidanceProcessor = DataProcessor.create(false)
    private val deviceGearLevelProcessor = DataProcessor.create(GearLevelEnum.UNKNOWN)

    val obstacleAvoidanceEnabled = obstacleAvoidanceProcessor.toFlow()
    val deviceGearLevel = deviceGearLevelProcessor.toFlow()

    override fun inSetupWithDrone() {
        bindDataProcessor(CommonKey.KeyDroneWorkStatusInfoReport.create(), obstacleAvoidanceProcessor, { it.obstacleAvoidanceEnabled })
        bindDataProcessor(FlightControlKey.KeyGearLevel.create(), deviceGearLevelProcessor)
    }

    override fun inCleanup() {

    }
}