package com.autel.widget.widget.ranging

import com.autel.aiso.AIJni
import com.autel.common.base.widget.BaseWidgetModel
import com.autel.common.extension.toRadian
import com.autel.common.feature.phone.AutelPhoneLocationManager
import com.autel.common.utils.LatLngUtil
import com.autel.common.utils.TransformUtils
import com.autel.drone.sdk.vmodelx.device.ModelXDevice
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.map.bean.AutelLatLng
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Created by  2024/11/12
 */
class RangingVM : BaseWidgetModel() {

    private var drone: IAutelDroneDevice? = null

    val rangingModelFlow = MutableSharedFlow<RangingModel>(replay = 1, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override fun fixedFrequencyRefresh() {
        val device = drone ?: return

        device as ModelXDevice
        val data = device.getDeviceStateData().flightControlData

        val targetLatLng = getRangingLatLng(device)
        val targetLocation = LatLngUtil.getLatLngWithUnit(targetLatLng.longitude, targetLatLng.latitude)
        var isRtkData = false
        val rtkBean = device.getDeviceStateData().rtkReportData
        if (rtkBean != null) {
            val fixed = rtkBean.fixSta
            if (fixed == 1) {
                isRtkData = true
            }
        }
        var rngValue = ""
        var aslValue = ""
        if (data.laserDistanceIsValid) {
            val aircraftAltMsl = data.altitudeMSL
            val laserDistanceM = data.laserDistance / 100.0
            rngValue = TransformUtils.getDistanceValueWithm(laserDistanceM, 1)
            //不需要考虑飞机的角度，云台的角度是相对水平面，并非相对飞机
            aslValue = TransformUtils.getDistanceValueWithm(
                (aircraftAltMsl + laserDistanceM * Math.sin(data.gimbalAttitudePitch.toRadian().toDouble())), 1
            )
        }
        val value = RangingModel(
            data.laserDistanceIsValid,
            rngValue,
            aslValue,
            targetLatLng.isInvalid(),
            targetLocation[0],
            targetLocation[1],
            isRtkData
        )
        if (rangingModelFlow.replayCache.firstOrNull() != value) {
            rangingModelFlow.tryEmit(value)
        }
    }

    fun updateDevice(drone: IAutelDroneDevice?) {
        this.drone = drone
    }


    //获取目标的经纬度
    private fun getRangingLatLng(device: ModelXDevice): AutelLatLng {
        val eul = getEul(device)
        val droneLocation = getDroneLocation(device)
        val droneLatLng = AutelLatLng(droneLocation[0], droneLocation[1])
        if (droneLatLng.isInvalid() ) {
            return AutelLatLng(0.0, 0.0)
        }
        val laserDis = getLaserDis(device)
        val relateHeight = device.getDeviceStateData().flightControlData.altitude
        val targetFromScreen = AIJni.targetFromScreen(eul, droneLocation, relateHeight.toDouble(), laserDis)

        val targetLatLng = AutelLatLng()
        targetLatLng.altitude = targetFromScreen.ret_alt
        targetLatLng.latitude = targetFromScreen.ret_lat
        targetLatLng.longitude = targetFromScreen.ret_lon
        return targetLatLng
    }

    private fun getEul(device: ModelXDevice): DoubleArray {
        val eul = DoubleArray(3)
        device.getDeviceStateData().flightControlData.let {
            eul[0] = it.gimbalAttitudeYaw.toRadian().toDouble()
            eul[1] = it.gimbalAttitudePitch.toRadian().toDouble()
            eul[2] = it.gimbalAttitudeRoll.toRadian().toDouble()
        }
        return eul
    }

    private fun getDroneLocation(device: ModelXDevice): DoubleArray {
        val droneLntLon = DoubleArray(3)
        device.getDeviceStateData().flightControlData.let {
            droneLntLon[0] = (it.droneLatitude)
            droneLntLon[1] = (it.droneLongitude)
            droneLntLon[2] = (it.altitudeMSL.toDouble())
        }
        return droneLntLon
    }

    private fun getHomeLocation(device: ModelXDevice): DoubleArray {
        val homeLntLon = DoubleArray(3)
        device.getDeviceStateData().flightControlData.let {
            homeLntLon[0] = (it.homeLatitude)
            homeLntLon[1] = (it.homeLongitude)
            homeLntLon[2] = (0.0)
        }
        return homeLntLon
    }

    private fun getLaserDis(device: ModelXDevice): Double {
        var laserDis = 0.0
        device.getDeviceStateData().flightControlData.let {
            if (it.laserDistanceIsValid) {
                laserDis = it.laserDistance / 100.0
            }
        }
        return laserDis
    }


}