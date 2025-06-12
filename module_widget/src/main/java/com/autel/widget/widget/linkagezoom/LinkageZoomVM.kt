package com.autel.widget.widget.linkagezoom

import com.autel.common.base.widget.BaseWidgetModel
import com.autel.drone.sdk.libbase.error.IAutelCode
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.interfaces.IKeyManager
import com.autel.drone.sdk.vmodelx.manager.keyvalue.callback.CommonCallbacks
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.CameraKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.base.KeyTools
import com.autel.drone.sdk.vmodelx.module.camera.bean.GimbalTypeEnum
import com.autel.log.AutelLog
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow

class LinkageZoomVM : BaseWidgetModel() {
    private var drone: IAutelDroneDevice? = null
    private var gimbalTypeEnum: GimbalTypeEnum? = null
    private var isShow: Boolean = false

    val linkState = MutableSharedFlow<LinkageZoomStateModel>(replay = 1, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    fun updateLensInfo(drone: IAutelDroneDevice?, gimbalTypeEnum: GimbalTypeEnum?, isShow: Boolean) {
        this.drone = drone
        this.gimbalTypeEnum = gimbalTypeEnum
        this.isShow = isShow
    }

    override fun fixedFrequencyRefresh() {
        val lDrone = drone
        val lGimbalTypeEnum = gimbalTypeEnum
        if (lDrone != null && lGimbalTypeEnum != null) {
            val gimbalData = lDrone.getDeviceStateData().gimbalDataMap.get(lGimbalTypeEnum)
            val linkage = gimbalData?.cameraOperateData?.bCameraLinkageZoom ?: false
            val linkageModel = LinkageZoomStateModel(isConnected = lDrone.isConnected(), inLinkage = linkage, isShow)
            if (linkageModel != linkState.replayCache.firstOrNull()) {
                linkState.tryEmit(linkageModel)
            }
        }
    }

    /**
     * 设置联动变焦
     */
    fun setLinkageZoom(linkage: Boolean) {
        val keyManager = getKeyManager()
        AutelLog.i("LinkageZoomTag", "setLinkageZoom linkage:$linkage keyManager != null:${keyManager != null}")

        if (keyManager != null) {
            keyManager.setValue(KeyTools.createKey(CameraKey.KeyCameraLinkageZoom), linkage, object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    AutelLog.i("LinkageZoomTag", "setLinkageZoom linkage:$linkage")
                }

                override fun onFailure(error: IAutelCode, msg: String?) {
                    AutelLog.e("LinkageZoomTag", "setLinkageZoom error: ${error.code} $msg")
                }
            })
        }
    }

    /**
     * 更新联动变焦
     */
    fun getLinkageZoom() {
        val keyManager = getKeyManager()
        AutelLog.i("LinkageZoomTag", "getLinkageZoom keyManager != null:${keyManager != null}")

        keyManager?.getValue(KeyTools.createKey(CameraKey.KeyCameraLinkageZoom),
            object : CommonCallbacks.CompletionCallbackWithParam<Boolean> {
                override fun onSuccess(t: Boolean?) {
                    AutelLog.i("LinkageZoomTag", "getLinkageZoom value:$t")

                }

                override fun onFailure(error: IAutelCode, msg: String?) {
                    AutelLog.e("LinkageZoomTag", "getLinkageZoom error: ${error.code} $msg")
                }
            })
    }

    private fun getKeyManager(): IKeyManager? {
        return drone?.getKeyManager()
    }

}