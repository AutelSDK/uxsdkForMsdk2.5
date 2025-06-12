package com.autel.setting.business

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.autel.common.base.BaseViewModel
import com.autel.common.constant.SharedParams
import com.autel.common.extension.asLiveData
import com.autel.common.sdk.KeyManagerCoroutineWrapper
import com.autel.common.sdk.exception.SdkDeviceNonExistException
import com.autel.common.sdk.service.SettingService
import com.autel.common.utils.DeviceUtils
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.FlightControlKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.FlightPropertyKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.MissionManagerKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.base.KeyTools
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.bean.HomeLocation
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.enums.DroneLostActionEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.enums.DroneWorkModeEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.enums.GearLevelEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.mission.enums.FcsCustomModeEnum
import com.autel.log.AutelLog
import kotlinx.coroutines.*

class SettingFlyControllerVM : BaseViewModel() {

    private val _backHeightLD = MutableLiveData<Float>()
    val backHeightLD = _backHeightLD.asLiveData()

    private val _maxHeightLD = MutableLiveData<Float>()
    val maxHeightLD = _maxHeightLD.asLiveData()

    private val _maxRadiusLD = MutableLiveData<Float>()
    val maxRadiusLD = _maxRadiusLD.asLiveData()

    private val _gearLevelLD = MutableLiveData<GearLevelEnum>()
    val gearLevelLD = _gearLevelLD.asLiveData()

    private val _rcLostAction = MutableLiveData<DroneLostActionEnum>()
    val rcLostAction = _rcLostAction.asLiveData()

    var dronWorkMode: DroneWorkModeEnum = DroneWorkModeEnum.UNKNOWN
    var droneLatitude = 0.0
    var droneLongitude = 0.0

    override fun fixedFrequencyRefresh() {
        super.fixedFrequencyRefresh()
        if (DeviceUtils.isSingleControl()) {
            val data = DeviceUtils.singleControlDrone()?.getDeviceStateData()?.flightControlData
            data?.let {
                val mode = it.droneWorkMode
                dronWorkMode = mode
                val latitude = it.droneLatitude
                val longitude = it.droneLongitude
                droneLatitude = latitude
                droneLongitude = longitude
                _gearLevelLD.value = it.droneGear
            }
        }
    }

    fun getDroneControlParams(retryCount: Int = 3) {
        val keyManager = getKeyManager()
        if (keyManager == null) {

        } else {
            viewModelScope?.launch(CoroutineExceptionHandler { _, throwable ->
                GlobalScope.launch {
                    delay(1000)
                    if (retryCount > 0) {
                        getDroneControlParams(retryCount - 1)
                    }
                }
            }) {

                var key = KeyTools.createKey(FlightControlKey.KeyGetCommonParams)
                val value = KeyManagerCoroutineWrapper.performAction(keyManager, key)
                AutelLog.d("getDroneControlParams", "KeyGetCommonParams:$value")
                _backHeightLD.value = value?.backHeight
                _maxHeightLD.value = value?.maxHeight
                _maxRadiusLD.value = value?.maxRange
                _rcLostAction.value = value?.lostAction
                _gearLevelLD.value = value?.gearLevel
            }
        }
    }

    fun setMissionManagerBackHeight(height: Float, onSuccess: () -> Unit, onError: (Throwable) -> Unit) {
        DeviceUtils.singleControlDrone()?.let { SettingService.getInstance().flightParamService.setBackHeight(it, height, {
            _backHeightLD.value = height
            onSuccess.invoke()
        }, onError) }
    }

    fun setFlightParamsMaxHeight(height: Float, onSuccess: () -> Unit, onError: (Throwable) -> Unit) {
        DeviceUtils.singleControlDrone()?.let {
            SettingService.getInstance().flightParamService.setMaxHeight(it, height, {
                _maxHeightLD.value = height
                onSuccess.invoke()
            }, onError)
        }
    }

    fun setFlightParamsMaxRadius(height: Float, onSuccess: () -> Unit, onError: (Throwable) -> Unit) {
        DeviceUtils.singleControlDrone()?.let {
            SettingService.getInstance().flightParamService.setMaxRadius(it, height, {
                _maxRadiusLD.value = height
                onSuccess.invoke()
            }, onError)
        }
    }

    /**
     * 设置HOME点为返航点
     */
    fun setHomeLocation(onSuccess: () -> Unit, onError: (Throwable) -> Unit) {
        val keyManager = getKeyManager()
        if (keyManager == null) {
            onError.invoke(SdkDeviceNonExistException())
        } else {
            viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
                onError.invoke(throwable)
            }) {
                val key = KeyTools.createKey(FlightControlKey.KeySetHomeLocation)
                KeyManagerCoroutineWrapper.performAction(keyManager, key)
                onSuccess.invoke()
            }
        }
    }

    /**
     * 自定义返航点
     */
    fun setCustomHomeLocation(homeLocation: HomeLocation, onSuccess: () -> Unit, onError: (Throwable) -> Unit) {
        val keyManager = getKeyManager()
        if (keyManager == null) {
            onError.invoke(SdkDeviceNonExistException())
        } else {
            viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
                onError.invoke(throwable)
            }) {
                val key = KeyTools.createKey(FlightControlKey.KeyCustomHomeLocation)
                KeyManagerCoroutineWrapper.performAction(keyManager, key, homeLocation)
                onSuccess.invoke()
            }
        }
    }

    fun setFlightCoordinatedTurnMsg(isTurn: Boolean, onSuccess: () -> Unit, onError: (Throwable) -> Unit) {
        val keyManager = getKeyManager()
        if (keyManager == null) {
            onError.invoke(SdkDeviceNonExistException())
        } else {
            viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
                onError.invoke(throwable)
            }) {
                val key = KeyTools.createKey(FlightPropertyKey.KeyCoordinatedTurn)
                KeyManagerCoroutineWrapper.setValue(keyManager, key, isTurn)
                onSuccess.invoke()
            }
        }
    }

    fun setFlightParamsRCLostActionMsg(action: DroneLostActionEnum, onSuccess: () -> Unit, onError: (Throwable) -> Unit) {
        val keyManager = getKeyManager()
        if (keyManager == null) {
            onError.invoke(SdkDeviceNonExistException())
        } else {
            viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
                onError.invoke(throwable)
            }) {
                val key = KeyTools.createKey(FlightPropertyKey.KeyRCLostAction)
                KeyManagerCoroutineWrapper.setValue(keyManager, key, action)
                onSuccess.invoke()
            }
        }
    }


    /**
     * 设置灵敏度
     * type:0,偏航行程  1，姿态 2，刹车
     * value:设置的值
     */
    fun setSensitivityMsg(type: Int, value: Float, onSuccess: () -> Unit, onError: (Throwable) -> Unit) {
        val keyManager = getKeyManager()
        if (keyManager == null) {
            onError.invoke(SdkDeviceNonExistException())
        } else {
            viewModelScope.launch(CoroutineExceptionHandler { _, e ->
                onError.invoke(e)
            }) {
                when (type) {
                    0 -> {
                        val key = KeyTools.createKey(FlightPropertyKey.KeyYawTripSensitivity)
                        KeyManagerCoroutineWrapper.setValue(keyManager, key, value)
                        onSuccess.invoke()
                    }
                    1 -> {
                        val key = KeyTools.createKey(FlightPropertyKey.KeyAttitudeSensitivity)
                        KeyManagerCoroutineWrapper.setValue(keyManager, key, value)
                        onSuccess.invoke()
                    }
                    2 -> {
                        val key = KeyTools.createKey(FlightPropertyKey.KeyBrakeSensitivity)
                        KeyManagerCoroutineWrapper.setValue(keyManager, key, value)
                        onSuccess.invoke()
                    }
                }
            }
        }

    }

    /**
     * 获取灵敏度
     * type:0,偏航行程  1，姿态 2，刹车
     *
     */
    fun getSensitivityMsg(type: Int, onSuccess: (value: Float) -> Unit, onError: (Throwable) -> Unit) {
        val keyManager = getKeyManager()
        if (keyManager == null) {
            onError.invoke(SdkDeviceNonExistException())
        } else {
            viewModelScope.launch(CoroutineExceptionHandler { _, e ->
                onError.invoke(e)
            }) {
                when (type) {
                    0 -> {
                        val key = KeyTools.createKey(FlightPropertyKey.KeyYawTripSensitivity)
                        onSuccess.invoke(KeyManagerCoroutineWrapper.getValue(keyManager, key))
                    }
                    1 -> {
                        val key = KeyTools.createKey(FlightPropertyKey.KeyAttitudeSensitivity)
                        onSuccess.invoke(KeyManagerCoroutineWrapper.getValue(keyManager, key))
                    }
                    2 -> {
                        val key = KeyTools.createKey(FlightPropertyKey.KeyBrakeSensitivity)
                        onSuccess.invoke(KeyManagerCoroutineWrapper.getValue(keyManager, key))
                    }
                }

            }
        }
    }


    /**
     * 获取返航绕障开关
     */
    fun getReturnDetourSwitch(onSuccess: (Boolean) -> kotlin.Unit, onError: () -> Unit) {
        val keyManager = getKeyManager()
        if (keyManager == null) {
            onError.invoke()
        } else {
            viewModelScope.launch(CoroutineExceptionHandler { _, e ->
                onError.invoke()
            }) {
                val key = KeyTools.createKey(MissionManagerKey.KeyReturnObsEnable)
                onSuccess(KeyManagerCoroutineWrapper.getValue(keyManager, key))
            }
        }
    }

    /**
     * 设置返航绕障开关
     */
    fun setReturnDetourSwitch(open: Boolean, onSuccess: () -> Unit, onError: () -> Unit) {
        val keyManager = getKeyManager()
        if (keyManager == null) {
            onError.invoke()
        } else {
            viewModelScope.launch(CoroutineExceptionHandler { _, e ->
                onError.invoke()
            }) {
                val key = KeyTools.createKey(MissionManagerKey.KeyReturnObsEnable)
                KeyManagerCoroutineWrapper.setValue(keyManager, key, open)
                onSuccess.invoke()
            }
        }
    }


    fun setFlightGear(
        gear: GearLevelEnum,
        onSuccess: () -> Unit,
        onError: (throwable: Throwable) -> Unit
    ) {
        DeviceUtils.singleControlDrone()?.let { SettingService.getInstance().flightParamService.setGearLevel(it, gear, onSuccess, onError) }
    }

    /**
     * 获取紧急避险开关
     */
    fun getNecessitySwitch(droneDevice: IAutelDroneDevice, onSuccess: (Boolean) -> Unit, onError: (Throwable) -> Unit) {
        SettingService.getInstance().flightParamService.getEmergencyEvasion(droneDevice, onSuccess, onError)
    }

    /**
     * 设置紧急避险开关
     */
    fun setNecessitySwitch(droneDevice: IAutelDroneDevice, open: Boolean, onSuccess: () -> Unit, onError: (Throwable) -> Unit) {
        SettingService.getInstance().flightParamService.setEmergencyEvasion(droneDevice, open, onSuccess, onError)
    }
}