package com.autel.setting.provider.delegate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autel.common.constant.AppTagConst
import com.autel.common.constant.AppTagConst.FlyInitTag
import com.autel.common.constant.SharedParams
import com.autel.common.manager.AppInfoManager
import com.autel.common.manager.AutelStorageManager
import com.autel.common.manager.StorageKey
import com.autel.common.manager.appinfo.AppTypeEnum
import com.autel.common.sdk.KeyManagerCoroutineWrapper
import com.autel.common.utils.AutelDirPathUtils
import com.autel.common.utils.DeviceUtils
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.interfaces.IKeyManager
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.AirLinkKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.CommonKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.FlightControlKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.FlightPropertyKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.MissionManagerKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.VisionKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.base.KeyTools
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.alink.enums.FccCeModeEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.common.bean.DroneRunEnvInfoBean
import com.autel.log.AutelLog

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Created by  2023/10/18
 * 飞机初始化数据VM
 */
class FlyInitDataVM : ViewModel() {
    /**
     * 获取飞机的keyManager
     * */
    private fun getKeyManager(droneDevice: IAutelDroneDevice?): IKeyManager? {
        if (droneDevice?.isConnected() == false || droneDevice == null) {
            AutelLog.i(AppTagConst.CameraInitTag, "getKeyManager is null")
            return null
        }
        return droneDevice.getKeyManager()
    }

    /**
     * 获取严重低电量告警
     */
    fun getFlightParamsBatSeriousLowWarning(droneDevice: IAutelDroneDevice) {
        val keyManager = getKeyManager(droneDevice)
        if (keyManager != null) {
            viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
                AutelLog.i(FlyInitTag, "initBandMode -> throwable=$throwable")
            }) {
                val key = KeyTools.createKey(FlightPropertyKey.KeyBatSeriousLowWarning)
                KeyManagerCoroutineWrapper.getValue(keyManager, key)
            }
        }
    }

    /**
     * 获取严重低电量
     */
    fun getFlightBatteryLowWarning(droneDevice: IAutelDroneDevice) {
        val keyManager = getKeyManager(droneDevice)
        if (keyManager != null) {
            viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
                AutelLog.i(FlyInitTag, "initBandMode -> throwable=$throwable")
            }) {
                val key = KeyTools.createKey(FlightPropertyKey.KeyBatteryLowWarning)
                KeyManagerCoroutineWrapper.getValue(keyManager, key)
            }
        }
    }

    /**
     * 获取飞机基本参数
     */
    fun getDroneControlParams(drone: IAutelDroneDevice) {
        val keyManager = getKeyManager(drone)
        if (keyManager != null) {
            viewModelScope.launch(CoroutineExceptionHandler { _, _ ->
                AutelLog.e(FlyInitTag, "getDroneControlParams error ${DeviceUtils.droneDeviceName(drone)}")
            }) {
                AutelLog.i(FlyInitTag, "getDroneControlParams ${DeviceUtils.droneDeviceName(drone)}")
                var key = KeyTools.createKey(FlightControlKey.KeyGetCommonParams)
                KeyManagerCoroutineWrapper.performAction(keyManager, key)
            }
        }
    }


    /**
     * 同步禁飞区开启关闭功能
     */
    fun setNoFlyEnable(drone: IAutelDroneDevice, isOpen: Boolean) {
        AutelLog.i(FlyInitTag, "setNoFlyEnable -> isOpen:$isOpen")
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            AutelLog.e(FlyInitTag, "setNoFlyEnable -> throwable:$throwable")
        }) {
            val keyManager = getKeyManager(drone)
            if (keyManager != null) {
                AutelLog.i(FlyInitTag, "is OpenNfz su:$isOpen")
                val key = KeyTools.createKey(MissionManagerKey.KeyNoFlyEnableMsg)
                KeyManagerCoroutineWrapper.setValue(keyManager, key, isOpen)
                AutelLog.e(FlyInitTag, "setNoFlyEnable -> success ")
            }
        }
    }

    /**
     * 设置视觉定位开关
     */
    fun setLocationStatus(drone: IAutelDroneDevice, open: Boolean, onSuccess: () -> Unit, onError: (Throwable) -> Unit) {
        AutelLog.i(FlyInitTag, "setLocationStatus ->　isOpen=$open")
        val keyManager = drone.getKeyManager()
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            onError.invoke(throwable)
            AutelLog.i(FlyInitTag, "setLocationStatus ->　throwable=$throwable")
        }) {
            val key = KeyTools.createKey(VisionKey.KeyAutonomyMifWorkStatus)
            KeyManagerCoroutineWrapper.setValue(keyManager, key, open)
            onSuccess.invoke()
            AutelStorageManager.getPlainStorage().setBooleanValue(StorageKey.PlainKey.IS_VISION_SWITCH, open)
            AutelLog.i(FlyInitTag, "setLocationStatus ->　onSuccess open=$open")
        }
    }

    /**
     * 获取视觉定位开关
     */
    fun getLocationStatus(drone: IAutelDroneDevice, onSuccess: (Boolean) -> Unit, onError: (Throwable) -> Unit) {
        AutelLog.i(FlyInitTag, "getLocationStatus ->　")
        val keyManager = drone.getKeyManager()
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            onError.invoke(throwable)
        }) {
            val key = KeyTools.createKey(VisionKey.KeyAutonomyMifWorkStatus)
            val result = KeyManagerCoroutineWrapper.getValue(keyManager, key)
            onSuccess.invoke(result)
            AutelLog.i(FlyInitTag, "getLocationStatus ->　onSuccess result=$result")
        }
    }

    /**
     * 设置国家码
     */
    fun setCountryCode(drone: IAutelDroneDevice, code: String, onError: (Throwable) -> Unit) {
        AutelLog.i(FlyInitTag, "initCountryCode ->　code=$code")
        val keyManager = drone.getKeyManager()
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            AutelLog.i(FlyInitTag, "initCountryCode -> throwable=$throwable")
            onError.invoke(throwable)
        }) {
            keyManager.let {
                val droneRunEnv = DroneRunEnvInfoBean(AppTypeEnum.getProductType(AppInfoManager.getAppType()), code)
                val key = KeyTools.createKey(CommonKey.KeySetDroneRunEnv)
                KeyManagerCoroutineWrapper.performAction(it, key, droneRunEnv)
                AutelLog.i(FlyInitTag, "initCountryCode ->　onSuccess code=$code")
            }
        }
    }

    /**
     * 禁飞区热区功能生效
     */
    fun initHotArea(drone: IAutelDroneDevice) {
        AutelLog.i(FlyInitTag, "initHotArea -> isNeedHotArea=${AppInfoManager.isSupportHotArea()}")
        val keyManager = drone.getKeyManager()
        CoroutineScope(Dispatchers.IO).launch(CoroutineExceptionHandler { _, throwable ->
            AutelLog.i(FlyInitTag, "initHotArea -> throwable=$throwable")
        }) {
            keyManager.let {
                val key = KeyTools.createKey(MissionManagerKey.KeyHotAreaEnable)
                KeyManagerCoroutineWrapper.setValue(it, key, AppInfoManager.isSupportHotArea())
                AutelLog.i(FlyInitTag, "initHotArea -> success")
            }
        }
    }

    /**
     * 获取飞机上的UOM注册状态
     * */
    fun getAircraftUom(drone: IAutelDroneDevice, onSuccess: (Boolean) -> Unit, onError: (Throwable) -> Unit) {
        AutelLog.i(FlyInitTag, "getAircraftUom ->　drone=${drone.getDeviceNumber()}")
        val keyManager = drone.getKeyManager()
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            AutelLog.e(FlyInitTag, "getAircraftUom -> throwable=$throwable")
            onError.invoke(throwable)
        }) {
            val key = KeyTools.createKey(FlightPropertyKey.KeyAircraftUOM)
            val result = KeyManagerCoroutineWrapper.getValue(keyManager, key)
            onSuccess.invoke(result)
            AutelLog.i(FlyInitTag, "getAircraftUom ->　onSuccess result=$result")
        }
    }

    /**
     * 获取最大飞行距离
     */
    fun getMaxRadius(drone:IAutelDroneDevice) {
        val keyManager = drone.getKeyManager()
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->

        }) {
            val key = KeyTools.createKey(FlightPropertyKey.KeyMaxRadius)
            KeyManagerCoroutineWrapper.getValue(keyManager, key)
        }
    }


    /**
     * 获取最大飞行距离
     */
    fun getMaxHeight(drone: IAutelDroneDevice, onSuccess: (Float) -> Unit) {
        val keyManager = drone.getKeyManager()
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->

        }) {
            val key = KeyTools.createKey(FlightPropertyKey.KeyMaxHeight)
            onSuccess(KeyManagerCoroutineWrapper.getValue(keyManager, key))
        }
    }

    /**
     * 获取最大飞行距离
     */
    fun setMaxHeight(drone: IAutelDroneDevice, height: Float) {
        val keyManager = drone.getKeyManager()
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->

        }) {
            val key = KeyTools.createKey(FlightPropertyKey.KeyMaxHeight)
            KeyManagerCoroutineWrapper.setValue(keyManager, key, height)
        }
    }

    /**
     * GNSS开关
     */
    fun setGpsFlightSwitch(drone: IAutelDroneDevice,enable:Boolean){
        AutelLog.i(FlyInitTag, "setGpsFlightSwitch -> enable=$enable")
        val keyManager = drone.getKeyManager()
        viewModelScope.launch(CoroutineExceptionHandler { _, e ->
            AutelLog.i(FlyInitTag, "setGpsFlightSwitch -> error=$e")
        }) {
            val key = KeyTools.createKey(FlightPropertyKey.KeyFcsEnGpsMode)
            KeyManagerCoroutineWrapper.setValue(keyManager, key, enable)
            SharedParams._gnssSwitch.postValue(enable)
            AutelLog.i(FlyInitTag, "setGpsFlightSwitch -> success")
        }
    }

    /**
     * 设置抗干扰模式
     */
    fun setKeyALinkFccCeMode(mode: FccCeModeEnum, onSuccess: () -> Unit, onError: () -> Unit) {
        AutelLog.i(FlyInitTag, "setKeyALinkFccCeMode -> mode=$mode")
        val keyManager = DeviceManager.getDeviceManager().getLocalRemoteDevice().getATKeyManager()
        keyManager.let {
            viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
                onError.invoke()
                AutelLog.e(FlyInitTag, "setKeyALinkFccCeMode -> throwable=$throwable")
            }) {
                val key = KeyTools.createKey(AirLinkKey.KeyALinkFccCeMode)
                KeyManagerCoroutineWrapper.setValue(it, key, mode)
                onSuccess.invoke()
                AutelLog.e(FlyInitTag, "setKeyALinkFccCeMode -> success")
            }
        }
    }

}