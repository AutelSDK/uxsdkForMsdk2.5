package com.autel.widget.widget.compass

import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.bean.DroneAttitudeBean

/**
 * Created by  2024/7/9
 */
data class Compass3DModel(
    val gimbalAttitudeBean: DroneAttitudeBean,
    val droneAttitudeBean: DroneAttitudeBean,
    val homeLatitude: Double,
    val homeLongitude: Double,
    val fovH: Double,
    val fovV: Double,
    val droneLatitude: Double,
    val droneLongitude: Double,
    val droneAltitude: Float,
    var droneToHomeDistance: Float,
    val laserDistanceM: Double
) {

    companion object {
        /**
         * 判断重置精准校射改变需要的距离
         */
        private const val CHANGE_DISTANCE = 1

        /**
         * 判断重置精准校射改变需要的角度
         */
        private const val CHANGE_ANGLE = 2

    }

    /**
     * 是否需要重置精准校射模型
     */
    fun needResetPreciseCalibration(data: Compass3DModel?): Boolean {
        if (data == null) {
            return true
        }
        val localDroneAttitudeBean = data.droneAttitudeBean
        val localGimbalAttitudeBean = data.gimbalAttitudeBean
        val localDroneToHomeDistance = data.droneToHomeDistance
        val localDroneAltitude = data.droneAltitude

        val dronePitchChange = Math.abs(localDroneAttitudeBean.getPitchDegree() - droneAttitudeBean.getPitchDegree()) > CHANGE_ANGLE
        if (dronePitchChange) {
            return true
        }

        val droneRollChange = Math.abs(localDroneAttitudeBean.getRollDegree() - droneAttitudeBean.getRollDegree()) > CHANGE_ANGLE
        if (droneRollChange) {
            return true
        }

        val droneYawChange = Math.abs(localDroneAttitudeBean.getYawDegree() - droneAttitudeBean.getYawDegree()) > CHANGE_ANGLE
        if (droneYawChange) {
            return true
        }


        val gimbalPitchChange = Math.abs(localGimbalAttitudeBean.getPitchDegree() - gimbalAttitudeBean.getPitchDegree()) > CHANGE_ANGLE
        if (gimbalPitchChange) {
            return true
        }

        val gimbalRollChange = Math.abs(localGimbalAttitudeBean.getRollDegree() - gimbalAttitudeBean.getRollDegree()) > CHANGE_ANGLE
        if (gimbalRollChange) {
            return true
        }

        val gimbalYawChange = Math.abs(localGimbalAttitudeBean.getYawDegree() - gimbalAttitudeBean.getYawDegree()) > CHANGE_ANGLE
        if (gimbalYawChange) {
            return true
        }

        if (Math.abs(localDroneToHomeDistance - droneToHomeDistance) > CHANGE_DISTANCE) {
            return true
        }

        if (data.fovV != fovV) {
            return true
        }

        if (data.fovH != fovH) {
            return true
        }

        if (Math.abs(localDroneAltitude - droneAltitude) > CHANGE_DISTANCE) {
            return true
        }
        return false
    }

    /**
     * 是否需要通知UI刷新指南针
     */
    fun needRefreshCompass(data: Compass3DModel?): Boolean {
        if (data == null) {
            return true
        }
        if (gimbalAttitudeBean != data.gimbalAttitudeBean) {
            return true
        }
        if (homeLatitude != data.homeLatitude) {
            return true
        }
        if (homeLongitude != data.homeLongitude) {
            return true
        }
        if (fovH != data.fovH) {
            return true
        }
        if (fovV != data.fovV) {
            return true
        }
        if (droneLatitude != data.droneLatitude) {
            return true
        }
        if (droneLongitude != data.droneLongitude) {
            return true
        }
        if (droneAltitude != data.droneAltitude) {
            return true
        }
        if (laserDistanceM != data.laserDistanceM) {
            return true
        }
        return false
    }

}