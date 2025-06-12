package com.autel.widget.widget.remotelocation

data class RemotePlaneLocationModel(
    val droneIsValid:Boolean, //飞机经纬度是否有效
    val droneLocationLat: String, //飞机纬度
    val droneLocationLng: String, //飞机经度

    val remoteIsValid:Boolean, //遥控器经纬度是否有效
    val remoteLat: String, //遥控器纬度
    val remoteLng: String, //遥控器经度
)