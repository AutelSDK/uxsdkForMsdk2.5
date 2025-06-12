package com.autel.widget.widget.map

/**
 *@Author autel
 *@Date 2025/5/30
 *
 */
data class DroneInfoModel(
    val id: Int = 0,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val height: Float = 0f,
    val heading: Float = 0f,
    val homeLatitude: Double = 0.0,
    val homeLongitude: Double = 0.0,
)