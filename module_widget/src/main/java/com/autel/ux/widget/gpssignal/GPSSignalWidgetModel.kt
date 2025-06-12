package com.autel.ux.widget.gpssignal

import com.autel.common.sdk.GpsSignalLevelEnum
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.CommonKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.ext.create
import com.autel.ux.core.base.AutelSDKModel
import com.autel.ux.core.base.WidgetModel
import com.autel.ux.core.utils.DataProcessor
import kotlinx.coroutines.flow.Flow

class GPSSignalWidgetModel(private val sdkModel: AutelSDKModel) : WidgetModel(sdkModel) {

    private val gpsSignalLevelProcessor = DataProcessor.create(GpsSignalLevelEnum.GPS_SIGNAL_LEVEL_NONE)

    private val gpsSatelliteCountProcessor = DataProcessor.create(0)

    val gpsSignalLevel: Flow<GpsSignalLevelEnum>
        get() = gpsSignalLevelProcessor.toFlow()

    val gpsSatelliteCount: Flow<Int>
        get() = gpsSatelliteCountProcessor.toFlow()

    override fun inSetupWithDrone() {
        bindDataProcessor(CommonKey.KeyDroneSystemStatusLFNtfy.create(), gpsSignalLevelProcessor, {
            GpsSignalLevelEnum.parseValue(it.gpsStrengthPercentage)
        }, DeviceManager.getFirstDroneDevice())
        bindDataProcessor(CommonKey.KeyDroneSystemStatusLFNtfy.create(), gpsSatelliteCountProcessor, {
            it.satelliteCount
        }, DeviceManager.getFirstDroneDevice())

        productConnected.collectInModel {
            if (!it) {
                gpsSignalLevelProcessor.emit(GpsSignalLevelEnum.GPS_SIGNAL_LEVEL_NONE)
                gpsSatelliteCountProcessor.emit(0)
            }
        }
    }

    override fun inCleanup() {

    }
}