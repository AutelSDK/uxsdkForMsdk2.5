package com.autel.widget.widget.ranging

/**
 * Created by  2024/8/1
 */
data class RangingModel(
    val laserDistanceIsValid: Boolean, //测距是否有效
    val targetRng: String, //目标距离
    val targetAsl: String, //目标海拔
    val targetIsInvalid: Boolean, //目标经纬度是否有效
    val targetLat: String, //目标纬度
    val targetLng: String, //目标经度
    val isRtkFix: Boolean //是否是RTK数据
)