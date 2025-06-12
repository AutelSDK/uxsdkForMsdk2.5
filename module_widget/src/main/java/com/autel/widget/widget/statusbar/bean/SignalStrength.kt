package com.autel.widget.widget.statusbar.bean

import com.autel.common.sdk.GpsSignalLevelEnum
import com.autel.common.sdk.RemoteSignalLevelEnum

data class SignalStrength(
    val gpsSignalLevel: GpsSignalLevelEnum,
    val gpsCount: Int,
    val rcSignalLevel: RemoteSignalLevelEnum
)
