package com.autel.setting.utils.matchUtils

import android.app.Activity
import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.autel.common.R
import com.autel.common.constant.AppTagConst
import com.autel.common.manager.AutelStorageManager
import com.autel.common.manager.MiddlewareManager
import com.autel.common.manager.StorageKey
import com.autel.common.sdk.service.SettingService
import com.autel.common.utils.UIUtils
import com.autel.common.widget.toast.AutelToast
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.drone.sdk.vmodelx.manager.keyvalue.callback.CommonCallbacks
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.AirLinkKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.base.KeyTools
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.alink.enums.AirLinkMatchStatusEnum
import com.autel.log.AutelLog
import com.autel.setting.view.DevicePairTipDialog

object MatchUtils {
    private var devicePairTipDialog: DevicePairTipDialog? = null
    /**
     * 开始对频
     * */
    fun startRcMatch(context: Context, callback: (Boolean) -> Unit) {
        startListenMatchStatus()
        airLinkMatchStatusData.value = AirLinkMatchStatusEnum.STATUS_UNKNOWN
        if (devicePairTipDialog == null) {
            devicePairTipDialog = DevicePairTipDialog(context)
        }
        startMatch({}, {})
        if (devicePairTipDialog?.isShowing == true) {
            callback(false)
            return
        }
        if (context is Activity) {
            if (context.isFinishing || context.isDestroyed) {
                callback(false)
                devicePairTipDialog = null
                return
            }
        }
        devicePairTipDialog?.show()
        devicePairTipDialog?.setCloseListener {
            stopListenMatchStatus()
            devicePairTipDialog?.dismiss()
            devicePairTipDialog = null
//            MiddlewareManager.netmeshModule.resetTeam {
//                if (it) {
//                    AutelLog.i(AppTagConst.RemoteControl, "resetTeam result: $it")
//                } else {
//                    AutelLog.e(AppTagConst.RemoteControl, "resetTeam result: $it")
//                }
//                callback(false)
//            }
        }
        AutelLog.i(AppTagConst.RemoteControl, "startRcMatch")
        airLinkMatchStatusData.observeForever{ matchStatusEnum ->
            AutelLog.i(AppTagConst.RemoteControl, "matchStatusEnum:$matchStatusEnum")
            when (matchStatusEnum) {
                AirLinkMatchStatusEnum.STATUS_FINISH -> {}
                AirLinkMatchStatusEnum.STATUS_UNKNOWN -> {}
                AirLinkMatchStatusEnum.STATUS_PAIRING -> {}
                AirLinkMatchStatusEnum.STATUS_SUC -> {
                    stopListenMatchStatus()
                    AutelToast.normalToast(context, UIUtils.getString(R.string.common_text_start_frequency_success))
                    devicePairTipDialog?.dismiss()
                    devicePairTipDialog = null
                    AutelStorageManager.getPlainStorage()
                        .setBooleanValue(StorageKey.PlainKey.KEY_FIRST_CONNECT_AIRCRAFT, true)
                    callback(true)
                }

                AirLinkMatchStatusEnum.STATUS_FAILED -> {
                    stopListenMatchStatus()
                    AutelToast.normalToast(context, UIUtils.getString(R.string.common_text_start_frequency_failed))
                    devicePairTipDialog?.dismiss()
                    devicePairTipDialog = null
                    callback(false)
                }
            }
        }
    }

    /**
     * 开始对频
     */
    private fun startMatch(onSuccess: (() -> Unit), onError: ((Throwable) -> Unit)) {
        SettingService.getInstance().remoteService.startMatch(onSuccess, onError)
    }

    /**
     * 开启配对监听
     * */
    private fun startListenMatchStatus() {
        DeviceManager.getDeviceManager().getLocalRemoteDevice().getKeyManager().listen(KeyTools.createKey(AirLinkKey.KeyALinkMatchingStatus), listenCallBack)
    }
    /**
     * 结束配对监听
     * */
    private fun stopListenMatchStatus() {
        DeviceManager.getDeviceManager().getLocalRemoteDevice().getKeyManager().cancelListen(KeyTools.createKey(AirLinkKey.KeyALinkMatchingStatus), listenCallBack)
    }
    val airLinkMatchStatusData = MutableLiveData<AirLinkMatchStatusEnum>()
    /**
     * 对频状态监听回调
     * */
    private val listenCallBack: CommonCallbacks.KeyListener<AirLinkMatchStatusEnum> = object : CommonCallbacks.KeyListener<AirLinkMatchStatusEnum> {
        override fun onValueChange(oldValue: AirLinkMatchStatusEnum?, newValue: AirLinkMatchStatusEnum) {
            newValue.let {
                airLinkMatchStatusData.postValue(newValue)
                AutelLog.i(AppTagConst.MatchTag, "keyLinkMatch -> $newValue")
            }
        }
    }
}