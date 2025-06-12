package com.autel.widget.widget.focusAndZoom

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.autel.common.base.BaseViewModel
import com.autel.common.model.splitscreen.AircraftScreenItem
import com.autel.common.model.splitscreen.toLensTypeEnum
import com.autel.common.sdk.KeyManagerCoroutineWrapper
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.CameraKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.base.KeyTools
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.camera.bean.MeteringPointBean
import com.autel.drone.sdk.vmodelx.module.camera.bean.LensTypeEnum
import com.autel.log.AutelLog
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch

class FocusViewModel: BaseViewModel() {

    private var aircraftScreenItem: AircraftScreenItem? = null

    val meterPointLiveData: MutableLiveData<MeteringPointBean> = MutableLiveData<MeteringPointBean>()

    val aELockLiveData: MutableLiveData<Boolean> = MutableLiveData<Boolean>()

    private var localLock: Boolean = false
    private var setPointIng: Boolean = false
    private var setLocking: Boolean = false

    fun attachAircraftItem(aircraftScreenItem: AircraftScreenItem) {
        this.aircraftScreenItem = aircraftScreenItem
    }

    fun getFocusData(){
        getFocusPoint {
            getAeLock()
        }
    }


    private fun getFocusPoint(onSuccess: () -> Unit) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
        }) {
            val key = KeyTools.createKey(CameraKey.KeyMeteringPoint, getCameraId())
            aircraftScreenItem?.drone?.getKeyManager()?.let {
                val bean = KeyManagerCoroutineWrapper.getValue(it, key)
                meterPointLiveData.value = bean
                onSuccess.invoke()
            }
        }
    }

    private fun getAeLock() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
        }) {
            val key = KeyTools.createKey(CameraKey.KeyAELock, getCameraId())
            aircraftScreenItem?.drone?.getKeyManager()?.let {
                val lock = KeyManagerCoroutineWrapper.getValue(it, key)
                localLock = lock
                aELockLiveData.value = lock
            }
        }
    }


    /**
     * 设置测光点
     */
    fun setFocusPoint(x: Int, y: Int) {
        val bean = MeteringPointBean(x, y)
        setPointIng = true
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            setPointIng = false
        }) {
            aircraftScreenItem?.drone?.getKeyManager()?.let {
                val key = KeyTools.createKey(CameraKey.KeyMeteringPoint, getCameraId())
                val spotAreaKey = KeyTools.createKey(CameraKey.KeyCameraFocusSpotArea, getCameraId())
                KeyManagerCoroutineWrapper.setValue(it, key, bean)
                meterPointLiveData.value = bean
                KeyManagerCoroutineWrapper.setValue(it, spotAreaKey, bean)
                setPointIng = false
                if (!setLocking) {
                    aELockLiveData.value = localLock
                }
            }
        }
    }

    /**
     * 设置AELock
     */
    fun setAELock(keyLock: Boolean) {
        setLocking = true
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            setLocking = false
        }) {
            val key = KeyTools.createKey(CameraKey.KeyAELock, getCameraId())
            aircraftScreenItem?.drone?.getKeyManager()?.let {
                KeyManagerCoroutineWrapper.setValue(it, key, keyLock)
                setLocking = false
                localLock = keyLock
                if (!setPointIng) {
                    aELockLiveData.value = keyLock
                }

            }
        }
    }

    /**
     * 是否支持锁焦
     */
    fun isSupportAFAELock(): Boolean {
        val len = aircraftScreenItem?.widgetType?.toLensTypeEnum()?: LensTypeEnum.Zoom
        val isSupport = aircraftScreenItem?.drone?.getCameraAbilitySetManger()?.getCameraSupport2()?.isAFAELockSupport(len) ?: false
        AutelLog.i(TAG,"isSupportAFAELock:$isSupport , lens : $len")
        return isSupport
    }


    private fun getCameraId() : Int {
        aircraftScreenItem?.drone?.let {
            return it.getCameraAbilitySetManger().getLenId(aircraftScreenItem?.widgetType?.toLensTypeEnum() ?: LensTypeEnum.Zoom, it.getGimbalDeviceType()) ?: 0
        }
        return 0
    }
}