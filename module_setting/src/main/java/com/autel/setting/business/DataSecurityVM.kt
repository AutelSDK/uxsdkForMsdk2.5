package com.autel.setting.business

import androidx.lifecycle.ViewModel
import com.autel.common.sdk.KeyManagerCallbackWrapper
import com.autel.drone.sdk.libbase.error.IAutelCode
import com.autel.drone.sdk.vmodelx.interfaces.IKeyManager
import com.autel.drone.sdk.vmodelx.manager.keyvalue.callback.CommonCallbacks
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.HardwareDataSecurityKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.RemoteControllerKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.base.KeyTools
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.harddatasecurity.bean.UavDataDestroyListBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.harddatasecurity.bean.UavDataEncryptionBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.harddatasecurity.bean.UavDataGenerationLimitBean

/**
 * Created by  2024/1/5
 * 数据安全加密VM
 */
class DataSecurityVM : ViewModel() {

    /**
     * 获取遥控器数据加密
     */
    fun getRemoteEncryption(keyManager: IKeyManager, callbacks: CommonCallbacks.CompletionCallbackWithParam<UavDataEncryptionBean>) {
        KeyManagerCallbackWrapper.performAction(keyManager,
            KeyTools.createKey(RemoteControllerKey.KeyUavDataEncryptionGet),
            object : CommonCallbacks.CompletionCallbackWithParam<UavDataEncryptionBean> {
                override fun onSuccess(result: UavDataEncryptionBean?) {
                    callbacks.onSuccess(result)
                }

                override fun onFailure(error: IAutelCode, msg: String?) {
                    callbacks.onFailure(error, msg)
                }
            })
    }

    /**
     * 获取数据限定生成Bean
     */
    fun getKeyLimitDataGeneration(keyManager: IKeyManager, callbacks: CommonCallbacks.CompletionCallbackWithParam<UavDataGenerationLimitBean>) {
        KeyManagerCallbackWrapper.performAction(keyManager,
            KeyTools.createKey(HardwareDataSecurityKey.KeyLimitDataGenerationGet),
            object : CommonCallbacks.CompletionCallbackWithParam<UavDataGenerationLimitBean> {
                override fun onSuccess(t: UavDataGenerationLimitBean?) {
                    callbacks.onSuccess(t)
                }

                override fun onFailure(error: IAutelCode, msg: String?) {
                    callbacks.onFailure(error, msg)
                }
            })
    }


    /**
     * 获取Uav数据销毁
     */
    fun getKeyUavDataDestroy(keyManager: IKeyManager, callbacks: CommonCallbacks.CompletionCallbackWithParam<UavDataDestroyListBean>) {
        KeyManagerCallbackWrapper.performAction(keyManager,
            KeyTools.createKey(HardwareDataSecurityKey.KeyUavDataDestroyGet),
            object : CommonCallbacks.CompletionCallbackWithParam<UavDataDestroyListBean> {
                override fun onSuccess(t: UavDataDestroyListBean?) {
                    callbacks.onSuccess(t)
                }

                override fun onFailure(error: IAutelCode, msg: String?) {
                    callbacks.onFailure(error, msg)
                }
            })
    }

    /**
     * 更新飞机对频加密数据
     */
    fun updateDataEncryptionBean(keyManager: IKeyManager, bean: UavDataEncryptionBean) {
        KeyManagerCallbackWrapper.performAction(
            keyManager,
            KeyTools.createKey(RemoteControllerKey.KeyUavDataEncryptionSet),
            bean,
            object : CommonCallbacks.CompletionCallbackWithParam<UavDataEncryptionBean> {
                override fun onSuccess(t: UavDataEncryptionBean?) {

                }

                override fun onFailure(error: IAutelCode, msg: String?) {

                }
            })
    }

    /**
     * 更新数据不生成的bean
     */
    fun updateDataGenerationLimitBean(keyManager: IKeyManager, bean: UavDataGenerationLimitBean) {
        KeyManagerCallbackWrapper.performAction(
            keyManager,
            KeyTools.createKey(HardwareDataSecurityKey.KeyLimitDataGenerationSet),
            bean,
            object : CommonCallbacks.CompletionCallbackWithParam<UavDataGenerationLimitBean> {
                override fun onSuccess(t: UavDataGenerationLimitBean?) {

                }

                override fun onFailure(error: IAutelCode, msg: String?) {

                }
            })
    }

    /**
     * 更新数据销毁的bean
     */
    fun updateDataDestroyListBean(keyManager: IKeyManager, bean: UavDataDestroyListBean) {
        KeyManagerCallbackWrapper.performAction(
            keyManager,
            KeyTools.createKey(HardwareDataSecurityKey.KeyUavDataDestroySet),
            bean,
            object : CommonCallbacks.CompletionCallbackWithParam<Void> {
                override fun onSuccess(t: Void?) {

                }

                override fun onFailure(error: IAutelCode, msg: String?) {

                }
            })
    }


}