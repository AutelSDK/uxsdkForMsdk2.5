package com.autel.ux.widget.gearlevel

import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.FlightControlKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.ext.create
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.enums.GearLevelEnum
import com.autel.ux.core.base.AutelSDKModel
import com.autel.ux.core.base.WidgetModel
import com.autel.ux.core.utils.DataProcessor
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine

class GearLevelModel(private val sdkModel: AutelSDKModel) : WidgetModel(sdkModel) {

    private val deviceGearLevelProcessor = DataProcessor.create(GearLevelEnum.UNKNOWN)
    val deviceGearLevel = deviceGearLevelProcessor.toFlow()

    override fun inSetupWithDrone() {
        bindDataProcessor(FlightControlKey.KeyGearLevel.create(), deviceGearLevelProcessor)
    }

    /**
     * 设置飞机档位
     */
    fun setFlightGearLevel(gearLevel: GearLevelEnum) {
        val action = controlMode.value.controlDrones.map {
            sdkModel.setValue(it, FlightControlKey.KeyGearLevel.create(), gearLevel)
        }
        combine(action) {
            it.all { it == gearLevel }
        }.catch {
            loge("set gear error ", it)
        }.collectInModel {
            logi("set gear level success :$it")
        }
    }

    override fun inCleanup() {

    }
}