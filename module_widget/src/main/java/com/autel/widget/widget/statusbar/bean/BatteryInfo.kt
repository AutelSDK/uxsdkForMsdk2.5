package com.autel.widget.widget.statusbar.bean

data class BatteryInfo(
    // 飞机电量
    val droneType: String,
    val droneBatteryPercentage: Int?,
    val criticalLowBattery: Int?,
    val lowBattery: Int?,
    val batteryTemperature: Float?,
    val batteryVoltage: Float?,
)

data class RemoteBattery(
    val current: Int,
    val total: Int
)