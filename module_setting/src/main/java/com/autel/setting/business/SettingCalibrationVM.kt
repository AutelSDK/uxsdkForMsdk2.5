package com.autel.setting.business

import androidx.lifecycle.viewModelScope
import com.autel.common.base.BaseViewModel
import com.autel.common.sdk.KeyManagerCoroutineWrapper
import com.autel.common.sdk.exception.SdkDeviceNonExistException
import com.autel.drone.sdk.libbase.error.IAutelCode
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.interfaces.IKeyManager
import com.autel.drone.sdk.vmodelx.manager.keyvalue.callback.CommonCallbacks
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.CommonKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.base.KeyTools
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.common.bean.CalibrationCommandBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.common.bean.CalibrationEventBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.common.bean.CalibrationScheduleBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.common.enums.CalibrationEventEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.common.enums.CalibrationTypeEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.common.enums.CmdEnum
import com.autel.log.AutelLog
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * @Author create by LJ
 * @Date 2022/10/25 08
 * IMU校准
 */
class SettingCalibrationVM : BaseViewModel() {

    companion object {
        const val TAG = "SettingCalibrationVM"
    }

    /**
     * 校准状态
     */
    private val _calibrationStatus = MutableStateFlow(CalibrationEventEnum.UNKNOWN)
    val calibrationStatus: Flow<CalibrationEventEnum> = _calibrationStatus

    /**
     * 校准步骤
     */
    private val _calibrationStep = MutableStateFlow(CalibrationScheduleBean())
    val calibrationStep: Flow<CalibrationScheduleBean> = _calibrationStep

    private var currentCalType: CalibrationTypeEnum = CalibrationTypeEnum.UNKNOWN

    private var droneDevice: IAutelDroneDevice? = null

    fun updateDroneDevice(droneDevice: IAutelDroneDevice) {
        this.droneDevice = droneDevice
    }

    private fun getCurrentDroneKeyManager(): IKeyManager? {
        if (droneDevice == null) {
            return getKeyManager()
        }
        return droneDevice?.getKeyManager()
    }

    /**
     * 开始校准
     */
    fun startCalibration(calType: CalibrationTypeEnum, onSuccess: () -> Unit, onError: (Throwable) -> Unit) {
        currentCalType = calType
        listenCalibrationStatus()
        listenCalibrationStep()
        val keyManager = getCurrentDroneKeyManager()
        if (keyManager == null) {
            onError.invoke(SdkDeviceNonExistException())
        } else {
            viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
                onError.invoke(throwable)
            }) {
                val key = KeyTools.createKey(CommonKey.KeyDroneCalibrationCommand)
                KeyManagerCoroutineWrapper.performAction(
                    keyManager,
                    key,
                    CalibrationCommandBean(calType, CmdEnum.START)
                )
                onSuccess.invoke()
            }
        }
    }

    /**
     * 监听校准状态
     */
    private fun listenCalibrationStatus() {
        val key = KeyTools.createKey(CommonKey.KeyDroneCalibrationEventNtfy)
        getCurrentDroneKeyManager()?.listen(key, listenCalibrationStatus)
    }

    /**
     * 校准装填监听
     */
    private val listenCalibrationStatus = object : CommonCallbacks.KeyListener<CalibrationEventBean> {
        override fun onValueChange(oldValue: CalibrationEventBean?, newValue: CalibrationEventBean) {
            AutelLog.i(TAG, "CalibrationStatus -> $newValue")
            if (newValue.calibrationType == currentCalType) {
                _calibrationStatus.value = newValue.calibrationEvent
            }
        }
    }

    /**
     * 监听校准步骤
     */
    private fun listenCalibrationStep() {
        val key = KeyTools.createKey(CommonKey.KeyDroneCalibrationScheduleNtfy)
        getCurrentDroneKeyManager()?.listen(key, listenCalibrationStep)
    }

    /**
     * 校准步骤监听
     */
    private val listenCalibrationStep = object : CommonCallbacks.KeyListener<CalibrationScheduleBean> {
        override fun onValueChange(oldValue: CalibrationScheduleBean?, newValue: CalibrationScheduleBean) {
            AutelLog.i(TAG, "CalibrationStep -> $newValue")
            if (newValue.calibrationType == currentCalType) {
                _calibrationStep.value =
                    CalibrationScheduleBean(newValue.imcStep, newValue.compassStep,newValue.gimbalStep, newValue.calibrationPercent,  newValue.calibrationType)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        AutelLog.i(TAG, "----------onClear--------")
        getCurrentDroneKeyManager()?.performAction(
            KeyTools.createKey(CommonKey.KeyDroneCalibrationCommand),
            CalibrationCommandBean(currentCalType, CmdEnum.STOP),
            object : CommonCallbacks.CompletionCallbackWithParam<Void> {
                override fun onSuccess(t: Void?) {
                }

                override fun onFailure(error: IAutelCode, msg: String?) {
                }
            })
        val key = KeyTools.createKey(CommonKey.KeyDroneCalibrationScheduleNtfy)
        val key1 = KeyTools.createKey(CommonKey.KeyDroneCalibrationEventNtfy)
        getCurrentDroneKeyManager()?.cancelListen(key, listenCalibrationStep)
        getCurrentDroneKeyManager()?.cancelListen(key1, listenCalibrationStatus)
    }
}