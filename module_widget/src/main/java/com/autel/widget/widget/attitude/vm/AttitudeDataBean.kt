package com.autel.widget.widget.attitude.vm

import com.autel.common.extension.sameContent
import com.autel.common.utils.UIConstants
import com.autel.map.bean.AutelLatLng

data class AttitudeDataBean(
    var connected: Boolean,
    var altitude: Float,
    var horizontalSpeed: Double,
    var horizontalSpeedStr: String,
    var verticalSpeed: String,
    var distance: String,
    var droneYaw: Float,
    var gimbalYaw: Float,
    var rollDegree: Float,
    var pitchDegree: Float,
    var droneLatLng: AutelLatLng,
    var homeLatLng: AutelLatLng,
    var remoteLatLng: AutelLatLng,
    var altitudeMSL: Float,
    var radarHeight: Double?,
    var droneBatteryPercent: Float,
    var remainFlightTime: Float,
    var lowBatteryWarningValue: Int,
    var seriousLowBatteryWarningValue: Int,
    var isRtkData: Boolean,
    var compassDegree: Float

) {

    constructor() : this(
        false, 0f, 0.0, "", "", "", 0f, 0f, 0f, 0f, AutelLatLng(), AutelLatLng(), AutelLatLng(),
        0f, 0.0, 0f, 0f, 0, 0, false, 0f
    )



    /**
     * 内容是否相似
     */
    fun sameContent(other: AttitudeDataBean?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AttitudeDataBean
        if (connected != other.connected) return false
        if (altitude != other.altitude) return false
        if (horizontalSpeed != other.horizontalSpeed) return false
        if (verticalSpeed != other.verticalSpeed) return false
        if (distance != other.distance) return false
        if (Math.abs(droneYaw - other.droneYaw) > UIConstants.MIN_EFFECTIVE_CHANGE_DEGREE) return false
        if (Math.abs(gimbalYaw - other.gimbalYaw) > UIConstants.MIN_EFFECTIVE_CHANGE_DEGREE) return false
        if (Math.abs(rollDegree - other.rollDegree) > UIConstants.MIN_EFFECTIVE_CHANGE_DEGREE) return false
        if (Math.abs(pitchDegree - other.pitchDegree) > UIConstants.MIN_EFFECTIVE_CHANGE_DEGREE) return false
        if (!droneLatLng.sameContent(other.droneLatLng)) return false
        if (!homeLatLng.sameContent(other.homeLatLng)) return false
        if (!remoteLatLng.sameContent(other.remoteLatLng)) return false
        if (altitudeMSL != other.altitudeMSL) return false
        if (radarHeight != other.radarHeight) return false
        if (droneBatteryPercent != other.droneBatteryPercent) return false
        if (remainFlightTime != other.remainFlightTime) return false
        if (lowBatteryWarningValue != other.lowBatteryWarningValue) return false
        if (seriousLowBatteryWarningValue != other.seriousLowBatteryWarningValue) return false
        if (isRtkData != other.isRtkData) return false
        if (compassDegree != other.compassDegree) return false

        return true
    }

    override fun toString(): String {
        return "AttitudeDataBean(altitude='$altitude', horizontalSpeed='$horizontalSpeed', verticalSpeed='$verticalSpeed', distance='$distance', droneYaw=$droneYaw, gimbalYaw=$gimbalYaw, rollDegree=$rollDegree, pitchDegree=$pitchDegree, droneLatLng=$droneLatLng, homeLatLng=$homeLatLng, remoteLatLng=$remoteLatLng, altitudeMSL=$altitudeMSL, radarHeight=$radarHeight, droneBatteryPercent=$droneBatteryPercent, remainFlightTime=$remainFlightTime, lowBatteryWarningValue=$lowBatteryWarningValue, seriousLowBatteryWarningValue=$seriousLowBatteryWarningValue)"
    }
}