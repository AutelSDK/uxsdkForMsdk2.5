package com.autel.ux.widget.sdcard

import com.autel.drone.sdk.vmodelx.device.statenew.key._FlightControlKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.CameraKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.ext.create
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.camera.enums.CardStatusEnum
import com.autel.ux.core.base.AutelSDKModel
import com.autel.ux.core.base.WidgetModel
import com.autel.ux.core.utils.DataProcessor

class SDCardModel(private val sdkModel: AutelSDKModel) : WidgetModel(sdkModel) {

    private val cardStatusProcessor = DataProcessor.create(CardStatusEnum.UNKNOWN)

    val cardStatus = cardStatusProcessor.toFlow()

    override fun inSetupWithDrone() {
        bindDataProcessor(CameraKey.KeySDCardStatus.create(), cardStatusProcessor, { it.storageStatus })
    }

    override fun inCleanup() {
        TODO("Not yet implemented")
    }
}