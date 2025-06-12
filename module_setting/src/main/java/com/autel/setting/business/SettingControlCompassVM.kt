package com.autel.setting.business

import androidx.lifecycle.viewModelScope
import com.autel.common.base.BaseViewModel
import com.autel.common.manager.AppInfoManager
import com.autel.common.manager.appinfo.AppRunningDeviceEnum
import com.autel.common.sdk.KeyManagerCoroutineWrapper
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.RemoteControllerKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.base.KeyTools
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.remotecontrol.bean.Rc60ImuCalibMessageBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.remotecontrol.enums.ImuCalibrationCommandEnum
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch

/**
 * 遥控器指南针校准
 */
class SettingControlCompassVM : BaseViewModel() {

    /**
     * 进入遥控器指南针校准模式
     */
    fun enterRCCalibration(
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        if (AppInfoManager.getAppRunningDevice() == AppRunningDeviceEnum.AutelRemotePad6_0) {
            viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
                onError.invoke(throwable)
            }) {
                val key = KeyTools.createKey(RemoteControllerKey.KeyRc60IMUCalibrate)
                getRemoteKeyManager()?.let {
                    KeyManagerCoroutineWrapper.performAction(
                        it,
                        key,
                        Rc60ImuCalibMessageBean(ImuCalibrationCommandEnum.ENTER_CALIBRATION_MODE)
                    )
                }
                onSuccess.invoke()
            }
        } else {
            onSuccess.invoke()
        }
    }

    /**
     * 退出遥控器指南针校准模式
     */
    fun exitRCCalibration() {
        if (AppInfoManager.getAppRunningDevice() == AppRunningDeviceEnum.AutelRemotePad6_0) {
            viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->

            }) {
                val key = KeyTools.createKey(RemoteControllerKey.KeyRc60IMUCalibrate)
                getRemoteKeyManager()?.let {
                    KeyManagerCoroutineWrapper.performAction(
                        it,
                        key,
                        Rc60ImuCalibMessageBean(ImuCalibrationCommandEnum.EXIT_CALIBRATION_MODE)
                    )
                }
            }
        }
    }
}