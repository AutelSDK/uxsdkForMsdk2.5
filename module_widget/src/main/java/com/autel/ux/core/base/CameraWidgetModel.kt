package com.autel.ux.core.base

import com.autel.drone.sdk.vmodelx.interfaces.IBaseDevice
import com.autel.drone.sdk.vmodelx.module.camera.bean.LensTypeEnum

abstract class CameraWidgetModel(autelSdkModel: AutelSDKModel, device: IBaseDevice? = null) : WidgetModel(autelSdkModel, device) {

    var cameraIndex: Int = -1
        private set

    var lensType: LensTypeEnum = LensTypeEnum.Zoom
        private set

    /**
     * 当相机镜头发生改变时,是否需要重新加载数据
     */
    open fun restartWhenCameraChange(): Boolean {
        /**
         * 默认需要在相机变化时重新加载数据
         */
        return true
    }

    fun updateCameraSource(
        cameraIndex: Int,
        lensType: LensTypeEnum,
    ) {
        this.cameraIndex = cameraIndex
        this.lensType = lensType
        if (restartWhenCameraChange()) {
            restart()
        }
    }
}