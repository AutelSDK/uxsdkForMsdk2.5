package com.autel.widget.widget.compass

import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.bean.DroneAttitudeBean

/**
 * Created by  2024/7/10
 */
data class ShootingTargetModel(
    val gimbalAttitudeBean: DroneAttitudeBean,
    val homeLatitude: Double,
    val homeLongitude: Double,
    val fovH: Double,
    val fovV: Double,
    val droneLatitude: Double,
    val droneLongitude: Double,
    val droneAltitude: Float,
    val laserDistanceM: Double
)