package com.autel.widget.widget.gimbalpitch

import com.autel.common.base.widget.BaseWidgetModel
import com.autel.common.bean.CustomRemoteKeyEnum
import com.autel.common.sdk.KeyManagerCoroutineWrapper
import com.autel.common.utils.CustomKeyUtils
import com.autel.common.utils.DeviceUtils
import com.autel.common.utils.UIConstants
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.drone.sdk.vmodelx.manager.keyvalue.callback.CommonCallbacks
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.GimbalKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.RemoteControllerKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.base.KeyTools
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.remotecontrol.bean.HardwareButtonInfoBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.remotecontrol.enums.RCButtonTypeEnum
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Created by  2023/5/28
 * 云台角度VM
 */
class GimbalPitchVM : BaseWidgetModel() {

    val gimbalPitchData = MutableSharedFlow<GimbalData>(replay = 1, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val connectedData = MutableSharedFlow<Boolean>(replay = 1, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val pbDismissState = MutableSharedFlow<Boolean>(replay = 1, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private var rcHardWareInfoKey = KeyTools.createKey(RemoteControllerKey.KeyRCHardwareInfo)
    private var rcHardWareCallback = object : CommonCallbacks.KeyListener<HardwareButtonInfoBean> {
        override fun onValueChange(oldValue: HardwareButtonInfoBean?, newValue: HardwareButtonInfoBean) {
            if ((newValue.buttonType == RCButtonTypeEnum.LEFT_CUSTOM && CustomKeyUtils.getDefineCustomC1() == CustomRemoteKeyEnum.MAP_FPV_SWITCH) ||
                (newValue.buttonType == RCButtonTypeEnum.RIGHT_CUSTOM && CustomKeyUtils.getDefineCustomC2() == CustomRemoteKeyEnum.MAP_FPV_SWITCH)
            ) {
                pbDismissState.tryEmit(true)
            }
        }
    }

    override fun fixedFrequencyRefresh() {
        if (DeviceUtils.isSingleControl()) {
            val device = DeviceUtils.singleControlDrone()
            val connected: Boolean
            val gimbalPitch = device?.getDeviceStateData()?.flightControlData?.gimbalAttitudePitch ?: 0f
            val lastPitch = gimbalPitchData.replayCache.firstOrNull()
            val gimbalAngleConstraintBean = device?.getDeviceStateData()?.gimbalDataMap?.get(device.getGimbalDeviceType())?.gimbalAngleConstraintBean
            val gimbalDataValue = GimbalData(gimbalPitch, gimbalAngleConstraintBean?.pitchMin ?: -90, gimbalAngleConstraintBean?.pitchMax ?: 0)
            if (device != null && device.isConnected()) {
                connected = true
                if (!gimbalDataValue.sameEquals(lastPitch)) {
                    gimbalPitchData.tryEmit(gimbalDataValue)
                }
            } else {
                connected = false
            }
            if (connectedData.replayCache.firstOrNull() != connected) {
                connectedData.tryEmit(connected)
                gimbalPitchData.tryEmit(gimbalDataValue)
            }
        }
    }

    suspend fun setGimbalAngel(param: Float) {
        if (DeviceUtils.isSingleControl()) {
            val device = DeviceUtils.singleControlDrone()
            if (device != null && device.isConnected()) {
                KeyManagerCoroutineWrapper.performAction(
                    device.getKeyManager(),
                    KeyTools.createKey(GimbalKey.KeyAngleDegreeControl),
                    param
                )
            }
        }
    }



    data class GimbalData(
        val current: Float,
        val min: Int,
        val max: Int
    ) {
        fun sameEquals(other: GimbalData?): Boolean {
            if (other == null) {
                return false
            }
            if (Math.abs(current - other.current) > UIConstants.MIN_GIMBAL_EFFECTIVE_CHANGE_DEGREE ||
                min != other.min ||
                max != other.max
            ) {
                return false
            }
            return true
        }
    }

    override fun setup() {
        super.setup()
        DeviceManager.getDeviceManager().getLocalRemoteDevice().getKeyManager().listen(rcHardWareInfoKey, callback = rcHardWareCallback)
    }

    override fun cleanup() {
        super.cleanup()
        DeviceManager.getDeviceManager().getLocalRemoteDevice().getKeyManager().cancelListen(rcHardWareInfoKey, callback = rcHardWareCallback)
    }
}