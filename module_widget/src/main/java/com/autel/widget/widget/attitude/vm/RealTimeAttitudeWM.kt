package com.autel.widget.widget.attitude.vm

import com.autel.common.feature.compass.Compass
import com.autel.common.feature.phone.AutelPhoneLocationManager
import com.autel.common.utils.DeviceUtils
import com.autel.common.utils.TransformUtils
import com.autel.drone.sdk.vmodelx.device.ModelXDevice
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.map.bean.AutelLatLng
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

/**
 * 实时姿态数据
 */

class RealTimeAttitudeWM : AttitudeWM() {

    override fun fixedFrequencyRefresh() {
        val device = controlledDrone()
        if (!DeviceUtils.isSingleControlDroneConnected()
            || !DeviceUtils.isSingleControl()
            || device == null
        ) {
            val bean = AttitudeDataBean().apply {
                connected = false
                compassDegree = Compass.getInstance().getAzimuth() ?: 0f
            }
            if (!bean.sameContent(droneAttitudeData.replayCache.firstOrNull())) {
                droneAttitudeData.tryEmit(bean)
            }
            return
        }

        if (device is ModelXDevice) {
            val data = device.getDeviceStateData()
            val flightOperateData = data.flightoperateData
            val flightControlData = data.flightControlData

            val horizontalSpeed = TransformUtils.getHorizontalSpeed(flightControlData.velocityX.toDouble(), flightControlData.velocityY.toDouble())

            val verticalSpeed = TransformUtils.getSpeedWithUnit(abs(flightControlData.velocityZ).toDouble())
            val distance = TransformUtils.getLengthWithUnit(flightControlData.distance.toDouble(),1)

            val droneYaw = if (flightControlData.droneAttitudeYaw == 0f) 0f else flightControlData.droneAttitudeYaw
            val gimbalYaw = if (flightControlData.gimbalAttitudeYaw == 0f) 0f else flightControlData.gimbalAttitudeYaw

            val rollDegree = flightControlData.droneAttitudeRoll
            val pitchDegree = flightControlData.droneAttitudePitch
            val horizontalSpeedString = TransformUtils.getSpeedWithUnit(horizontalSpeed)

            var mslHeight = flightControlData.altitudeMSL
            val rtkBean = device.getDeviceStateData().rtkReportData
            var isRtkData = false
            if (rtkBean != null) {
                val format = DecimalFormat.getInstance(Locale.ENGLISH) as DecimalFormat
                //高度
                format.applyPattern("0.000")
                val hgt = format.format(rtkBean.hgt / 10000000)
                val fixed = rtkBean.fixSta
                if (fixed == 1) {
                    mslHeight = hgt.toFloat()
                    isRtkData = true
                }
            }
            val bean = AttitudeDataBean(
                true,
                flightControlData.altitude,
                horizontalSpeed,
                horizontalSpeedString,
                verticalSpeed,
                distance,
                droneYaw,
                gimbalYaw,
                rollDegree,
                pitchDegree,
                AutelLatLng(flightControlData.droneLatitude, flightControlData.droneLongitude),
                AutelLatLng(flightControlData.homeLatitude, flightControlData.homeLongitude),
                AutelPhoneLocationManager.locationLiveData.value ?: AutelLatLng(),
                mslHeight,
                flightControlData.radarHight,
                flightControlData.batteryPercentage,
                flightControlData.remainingFlightTime,
                flightOperateData.iBatteryLowWarningValue,
                flightOperateData.iBatSeriousLowWarningValue,
                isRtkData,
                Compass.getInstance().getAzimuth() ?: 0f
            )
            if (!bean.sameContent(droneAttitudeData.replayCache.firstOrNull())) {
                droneAttitudeData.tryEmit(bean)
            }
        }
    }

    fun getString(floatValue: Float): String {
        val decimalFormat = DecimalFormat("#.0", DecimalFormatSymbols(Locale.ENGLISH)) // 设置格式化输出的模板
        return decimalFormat.format(floatValue) // 将字符串转换成double类型返回
    }

    /**
     * 当前受控封飞机
     * */
    private fun controlledDrone(): IAutelDroneDevice? {
        //受控飞机列表不为空时，说明有飞机受控，以受控飞机列表的第一个飞机为准
        return DeviceUtils.singleControlDrone()
    }

}