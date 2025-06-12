package com.autel.setting.business

import androidx.lifecycle.viewModelScope
import com.autel.common.base.BaseViewModel
import com.autel.common.constant.AppTagConst.SettingTag
import com.autel.common.sdk.KeyManagerCoroutineWrapper
import com.autel.common.sdk.exception.SdkDeviceNonExistException
import com.autel.common.utils.DeviceUtils
import com.autel.drone.sdk.libbase.error.AutelStatusCode
import com.autel.drone.sdk.libbase.error.IAutelCode
import com.autel.drone.sdk.v2.bean.LTEPrivatizationServerInfo
import com.autel.drone.sdk.v2.callback.LTELinkInfoListener
import com.autel.drone.sdk.v2.enums.LTELinkType
import com.autel.drone.sdk.v2.interfaces.ILTEManager
import com.autel.drone.sdk.vmodelx.enums.FrequencyBand
import com.autel.drone.sdk.vmodelx.manager.FrequencyBandManager
import com.autel.drone.sdk.vmodelx.manager.keyvalue.callback.CommonCallbacks
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.AirLinkKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.CameraKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.base.KeyTools
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.alink.enums.AirLinkBandModeEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.alink.enums.VideoTransMissionModeEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.camera.enums.VideoCompressStandardEnum
import com.autel.log.AutelLog
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch

class SettingHDVM : BaseViewModel() {

    /**
     * 获取图传模式
     */
    fun getALinkTransmissionMode(
        onSuccess: (VideoTransMissionModeEnum) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            onError.invoke(throwable)
        }) {
            getKeyManager()?.let {
                val key = KeyTools.createKey(AirLinkKey.KeyALinkTransmissionMode)
                onSuccess.invoke(KeyManagerCoroutineWrapper.getValue(it, key))
            }
        }
    }

    /**
     * 设置图传模式
     */
    fun setALinkTransmissionMode(mode: VideoTransMissionModeEnum) {
        viewModelScope.launch(CoroutineExceptionHandler { _, _ ->

        }) {
            getKeyManager()?.let {
                val key = KeyTools.createKey(AirLinkKey.KeyALinkTransmissionMode)
                KeyManagerCoroutineWrapper.setValue(it, key, mode)
            }
        }
    }

    /**
     * 获取图传频段
     */
    fun getBandMode(onSuccess: (AirLinkBandModeEnum) -> Unit, onError: () -> Unit) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            AutelLog.i(SettingTag, "getBandMode -> throwable=$throwable")
            onError.invoke()
        }) {
            getKeyManager()?.let {
                val key = KeyTools.createKey(AirLinkKey.KeyALinkBandMode)
                onSuccess.invoke(KeyManagerCoroutineWrapper.getValue(it, key))
            } ?: onError.invoke()
        }
    }

    /**
     * 设置频段
     */
    fun setBandMode(bandMode: AirLinkBandModeEnum, onSuccess: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            AutelLog.i(SettingTag, "setBandMode -> throwable=$throwable")
            onError.invoke()
        }) {
            getKeyManager()?.let {
                val key = KeyTools.createKey(AirLinkKey.KeyALinkBandMode)
                KeyManagerCoroutineWrapper.setValue(it, key, bandMode)
                onSuccess.invoke()
            } ?: onError.invoke()
        }
    }

    /**
     * 设置图传编码
     */
    fun setDspEncodeFormat(
        videoCompressStandardEnum: VideoCompressStandardEnum,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit,
    ) {

        val keyManager = getKeyManager()
        if (keyManager == null) {
            onError.invoke(SdkDeviceNonExistException())
        } else {
            viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
                onError.invoke(throwable)
            }) {
                val key = KeyTools.createKey(CameraKey.KeyCameraTransferPayLoadType)
                KeyManagerCoroutineWrapper.setValue(keyManager, key, videoCompressStandardEnum)
                onSuccess.invoke()
            }
        }
    }

    /**
     * 获取图传编码
     */
    fun getDspEncodeFormat(
        onSuccess: (VideoCompressStandardEnum) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val keyManager = getKeyManager()
        if (keyManager == null) {
            onError.invoke(SdkDeviceNonExistException())
        } else {
            viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
                onError.invoke(throwable)
            }) {
                val key = KeyTools.createKey(CameraKey.KeyCameraTransferPayLoadType)
                val result = KeyManagerCoroutineWrapper.getValue(keyManager, key)
                onSuccess.invoke(result)
            }
        }
    }

    /**
     * 获取当前频段的tag
     */
    fun getCurBandMode(onSuccess: (String) -> Unit, onError: () -> Unit) {
        AutelLog.i(TAG, "getCurBandMode -> ")
        FrequencyBandManager.get().getCurrFrequencyBand {
            AutelLog.i(TAG, "getCurBandMode -> result=$it")
            if (it == FrequencyBand.MODE_UNKNOWN) {
                onError.invoke()
            } else {
                onSuccess.invoke(it.tag)
            }
        }
    }

    /**
     * 设置合规频段
     */
    fun setCurBandMode(band: FrequencyBand, onSuccess: () -> Unit, onError: () -> Unit) {
        AutelLog.i(TAG, "setCurBandMode -> band=$band")
        FrequencyBandManager.get().setFrequencyBand(band, {
            AutelLog.i(TAG, "setCurBandMode -> onSuccess ")
            onSuccess.invoke()
        }, {
            AutelLog.i(TAG, "setCurBandMode -> onError=$it ")
            onError.invoke()
        })
    }

    /**
     * 获取增强图传开关
     */
    fun getHdEnhanceState(onSuccess: (Boolean) -> Unit, onError: () -> Unit) {
        AutelLog.i(TAG, "getHdEnhanceState -> ")
        val manager = getLTEManager()
        if (manager == null) {
            AutelLog.i(TAG, "getHdEnhanceState -> manager is null")
            onError.invoke()
            return
        }
        manager.getLTEEnhancedTransmissionType(object : CommonCallbacks.CompletionCallbackWithParam<LTELinkType> {
            override fun onFailure(error: IAutelCode, msg: String?) {
                AutelLog.i(TAG, "getHdEnhanceState -> onFailure error=$error msg=$msg")
                onError.invoke()
            }

            override fun onSuccess(t: LTELinkType?) {
                AutelLog.i(TAG, "getHdEnhanceState -> onSuccess t=$t")
                onSuccess.invoke(t == LTELinkType.SKY_LINK_LTE)
            }

        })
    }

    /**
     * 设置增强图传
     */
    fun setHdEnhanceState(enable: Boolean, onSuccess: () -> Unit, onError: () -> Unit) {
        AutelLog.i(TAG, "setHdEnhanceState -> enable=$enable")
        val manager = getLTEManager()
        if (manager == null) {
            AutelLog.i(TAG, "setHdEnhanceState -> manager is null")
            onError.invoke()
            return
        }
        val state = if (enable) LTELinkType.SKY_LINK_LTE else LTELinkType.SKY_LINK
        manager.setLTEEnhancedTransmissionType(state, object : CommonCallbacks.CompletionCallback {
            override fun onFailure(code: IAutelCode, msg: String?) {
                AutelLog.i(TAG, "setHdEnhanceState -> onFailure code=$code msg=$msg")
                onError.invoke()
            }

            override fun onSuccess() {
                AutelLog.i(TAG, "setHdEnhanceState -> onSuccess ")
                onSuccess.invoke()
            }

        })
    }

    /**
     * 注册LTE监听
     */
    fun addLTELinkInfoListener(listener: LTELinkInfoListener) {
        AutelLog.i(TAG, "addLTELinkInfoListener -> listener=$listener")
        val manager = getLTEManager()
        if (manager == null) {
            AutelLog.i(TAG, "addLTELinkInfoListener -> manager is null")
            return
        }
        manager.addLTELinkInfoListener(listener)
    }

    /**
     * 移除LTE监听
     */
    fun removeLTELinkInfoListener(listener: LTELinkInfoListener) {
        AutelLog.i(TAG, "removeLTELinkInfoListener -> listener=$listener")
        val manager = getLTEManager()
        if (manager == null) {
            AutelLog.i(TAG, "removeLTELinkInfoListener -> manager is null")
            return
        }
        manager.removeLTELinkInfoListener(listener)
    }

    /**
     * 设置飞行器和遥控器 私有化服务器地址
     * @param droneUrl 服务器地址信息
     * @param rcUrl 服务器地址信息
     * @param onSuccess   返回成功。
     * @param onError   返回失败。
     */
    fun saveDroneAndRcUrl(droneUrl: String, rcUrl: String, onSuccess: () -> Unit, onError: (Boolean) -> Unit) {
        AutelLog.i(TAG, "saveDroneAndRcUrl -> droneUrl=$droneUrl rcUrl=$rcUrl")
        val manager = getLTEManager()
        if (manager == null) {
            AutelLog.i(TAG, "saveDroneAndRcUrl -> manager is null")
            onError.invoke(false)
            return
        }

        manager.setLTEAircraftPrivatizationServerInfo(LTEPrivatizationServerInfo(droneUrl), object : CommonCallbacks.CompletionCallback {
            override fun onFailure(code: IAutelCode, msg: String?) {
                AutelLog.i(TAG, "saveDroneAndRcUrl -> onFailure drone code=$code msg=$msg")
                onError.invoke(code == AutelStatusCode.PARAMS_INVALID)
            }

            override fun onSuccess() {
                AutelLog.i(TAG, "saveDroneAndRcUrl -> onSuccess drone ")
                manager.setLTERemoteControllerPrivatizationServerInfo(LTEPrivatizationServerInfo(rcUrl), object : CommonCallbacks.CompletionCallback {
                    override fun onFailure(code: IAutelCode, msg: String?) {
                        AutelLog.i(TAG, "saveDroneAndRcUrl -> onFailure rc code=$code msg=$msg")
                        onError.invoke(code == AutelStatusCode.PARAMS_INVALID)
                    }

                    override fun onSuccess() {
                        AutelLog.i(TAG, "saveDroneAndRcUrl -> onSuccess rc ")
                        onSuccess.invoke()
                    }
                })
            }

        })
    }

    /**
     * 清除飞行器 LTE 私有化服务器地址
     * @param onSuccess   返回成功。
     * @param onError   返回失败。
     */
    fun resetDroneUrl(onSuccess: () -> Unit, onError: () -> Unit) {
        AutelLog.i(TAG, "resetDroneUrl -> ")
        val manager = getLTEManager()
        if (manager == null) {
            AutelLog.i(TAG, "resetDroneUrl -> manager is null")
            onError.invoke()
            return
        }

        manager.clearLTEAircraftPrivatizationServer(object : CommonCallbacks.CompletionCallback {
            override fun onFailure(code: IAutelCode, msg: String?) {
                AutelLog.i(TAG, "resetDroneUrl -> onFailure code=$code msg=$msg")
                onError.invoke()
            }

            override fun onSuccess() {
                AutelLog.i(TAG, "resetDroneUrl -> onSuccess")
                onSuccess.invoke()
            }

        })
    }

    /**
     * 清除遥控器 LTE 私有化服务器地址。
     * @param onSuccess   返回成功。
     * @param onError   返回失败。
     */
    fun resetRcUrl(onSuccess: () -> Unit, onError: () -> Unit) {
        AutelLog.i(TAG, "resetRcUrl -> ")
        val manager = getLTEManager()
        if (manager == null) {
            AutelLog.i(TAG, "resetRcUrl -> manager is null")
            onError.invoke()
            return
        }

        manager.clearLTERemoteControllerPrivatizationServer(object : CommonCallbacks.CompletionCallback {
            override fun onFailure(code: IAutelCode, msg: String?) {
                AutelLog.i(TAG, "resetRcUrl -> onFailure code=$code msg=$msg")
                onError.invoke()
            }

            override fun onSuccess() {
                AutelLog.i(TAG, "resetRcUrl -> onSuccess")
                onSuccess.invoke()
            }

        })
    }


    private fun getLTEManager(): ILTEManager? {
        return DeviceUtils.singleControlDrone()?.getLTEManager()
    }

    /**
     * 是否正在飞行
     */
    fun isFlying(): Boolean {
        val flightMode = DeviceUtils.singleControlDrone()?.getDeviceStateData()?.flightControlData?.flightMode
        return flightMode?.isFlying() == true
    }

    /**
     * 图传信号是否OK
     */
    fun isRcSignalAvailable(): Boolean{
        val rcSignalQuality = DeviceUtils.getLocalRemoteDevice().getDeviceStateData().rcStateNtfyBean.rcSignalQuality
        return rcSignalQuality > 0
    }

}