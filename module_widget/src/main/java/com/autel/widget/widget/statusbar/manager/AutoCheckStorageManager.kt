package com.autel.widget.widget.statusbar.manager

import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.autel.common.base.AppActivityManager
import com.autel.common.constant.AppTagConst.SdCardCheckerTag
import com.autel.common.feature.route.RouteManager
import com.autel.common.feature.route.RouterConst
import com.autel.common.lifecycle.LiveDataBus
import com.autel.common.lifecycle.event.StorageLocationEvent
import com.autel.common.manager.AppInfoManager
import com.autel.common.sdk.service.cameraSetting.CameraSettingService
import com.autel.common.utils.*
import com.autel.common.utils.UIUtils.getString
import com.autel.common.widget.dialog.CommonTwoButtonDialog
import com.autel.common.widget.toast.AutelToast
import com.autel.drone.sdk.vmodelx.dronestate.CameraData
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.camera.enums.CardStatusEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.camera.enums.StorageTypeEnum
import com.autel.log.AutelLog
import com.autel.widget.widget.statusbar.bean.StorageEventEnum
import com.autel.widget.R


/**
 * @author 
 * @date 2023/2/10
 * 自动检测存储管理类管理类
 */
object AutoCheckStorageManager {
    private val TAG_DIALOG = "AutoCheckStorageDialog"

    //SD卡状态
    private var curSdcardStatusMap = mutableMapOf<String, CardStatusEnum>()
    private var curEmmcStatusMap = mutableMapOf<String, CardStatusEnum>()

    private var isShowDialogMap = mutableMapOf<String, Boolean>()
    private var isAdd = false
    private val timeListener = object : TimerEventListener(this.javaClass.simpleName) {
        override fun onEventChanged() {
            //只有主遥控器才检测sd卡状态
            if (DeviceUtils.isMainRC()) {
                //组网环境下，组网中不显示弹窗
                if (DeviceUtils.isNetMeshing()) {
                    return
                }
                DeviceUtils.allOnlineDrones().forEach { droneDevice ->
                    if (droneDevice.isConnected()) {//不在线的飞机不做处理
                        val data = droneDevice.getDeviceStateData().gimbalDataMap[droneDevice.getGimbalDeviceType()]?.cameraData
                        data?.let { checkSDCardStatus(droneDevice, it) }
                        data?.let { checkMMCStatus(droneDevice, it) }
                    }
                }
            }
        }
    }

    /**
     * 检查sdCard状态
     * */
    private fun checkSDCardStatus(droneDevice: IAutelDroneDevice, data: CameraData) {
        val storage = data.storageType
        val emmcStorageStatus = droneDevice.getDeviceStateData().emmcData.mMCStorageStatus
        val sdStorageStatus = droneDevice.getDeviceStateData().sdcardData.sdStorageStatus
        val curSdcardStatus = curSdcardStatusMap[droneDevice.getDeviceNumber().toString()] ?: CardStatusEnum.UNKNOWN
//        AutelLog.d(SdCardCheckerTag, "checkSDCardStatus storage=$storage sdStorageStatus=$sdStorageStatus curSdcardStatus=$curSdcardStatus emmcStorageStatus=$emmcStorageStatus")
        if (storage == StorageTypeEnum.SD) {
            //SD卡已拔出
            if (CardStatusEnum.isEnable(curSdcardStatus) && sdStorageStatus == CardStatusEnum.NO_CARD) {
                AutelLog.i(SdCardCheckerTag, "checkSDCardStatus -> ${droneDevice.toSampleString()}  storage=$storage sdStorageStatus=$sdStorageStatus curSdcardStatus=$curSdcardStatus")
                showStorageEventDialog(droneDevice, StorageEventEnum.SDCARD_OUT_TO_CHANGE_EMMC)
                //仅仅优化,避免状态没更新时一直弹出
                droneDevice.getDeviceStateData().sdcardData.sdStorageStatus = CardStatusEnum.UNKNOWN
            }
            //TF已满,机载闪存ready，切换到机载闪存
            if (sdStorageStatus == CardStatusEnum.FULL && emmcStorageStatus == CardStatusEnum.READY && curSdcardStatus != CardStatusEnum.FULL) {
                AutelLog.i(SdCardCheckerTag, "checkSDCardStatus -> ${droneDevice.toSampleString()}  storage=$storage sdStorageStatus=$sdStorageStatus curSdcardStatus=$curSdcardStatus")
                showStorageEventDialog(droneDevice, StorageEventEnum.SDCARD_FULL_TO_CHANGE_EMMC)
            }
            //TF已满，机载闪存已满，则清理空间
            if (sdStorageStatus == CardStatusEnum.FULL && emmcStorageStatus == CardStatusEnum.FULL && curSdcardStatus != CardStatusEnum.FULL) {
                AutelLog.i(SdCardCheckerTag, "checkSDCardStatus -> ${droneDevice.toSampleString()}  storage=$storage sdStorageStatus=$sdStorageStatus curSdcardStatus=$curSdcardStatus")
                showStorageEventDialog(droneDevice, StorageEventEnum.CLEAN_ALBUM)
            }

        } else if (storage == StorageTypeEnum.EMMC) {
            //检测到SD卡已插入，并且存储位置在机载闪存
            if (CardStatusEnum.isEnable(sdStorageStatus) && !CardStatusEnum.isEnable(curSdcardStatus)) {
                AutelLog.i(SdCardCheckerTag, "checkMMCardStatus -> ${droneDevice.toSampleString()} storage=$storage sdStorageStatus=$sdStorageStatus curSdcardStatus=$curSdcardStatus")
                showStorageEventDialog(droneDevice, StorageEventEnum.DETECTED_SDCARD_TO_CHANGE)
            }
        }
        curSdcardStatusMap[droneDevice.getDeviceNumber().toString()] = sdStorageStatus
    }

    private fun checkMMCStatus(droneDevice: IAutelDroneDevice, data: CameraData) {
        val storage = data.storageType
        val emmcStorageStatus = droneDevice.getDeviceStateData().emmcData.mMCStorageStatus
        val curSdcardStatus = curSdcardStatusMap[droneDevice.getDeviceNumber().toString()] ?: CardStatusEnum.UNKNOWN
        val curEmmcStatus = curEmmcStatusMap[droneDevice.getDeviceNumber().toString()] ?: CardStatusEnum.UNKNOWN
        //机载闪存已满，TF卡ready，则切换到机载闪存
        if (emmcStorageStatus == CardStatusEnum.FULL && curSdcardStatus == CardStatusEnum.READY && curEmmcStatus != CardStatusEnum.FULL) {
            showStorageEventDialog(droneDevice, StorageEventEnum.EMMC_CHANGE_TO_TF)
        }

        //机载闪存已满，TF卡无卡，则提示清理
        if (emmcStorageStatus == CardStatusEnum.FULL && CardStatusEnum.isUnAvailable(curSdcardStatus) && curEmmcStatus != CardStatusEnum.FULL) {
            showStorageEventDialog(droneDevice, StorageEventEnum.CLEAN_ALBUM)
        }

        //机载闪存已满，TF卡已满，则提示清理
        if (emmcStorageStatus == CardStatusEnum.FULL && curSdcardStatus == CardStatusEnum.FULL && curEmmcStatus != CardStatusEnum.FULL) {
            showStorageEventDialog(droneDevice, StorageEventEnum.CLEAN_ALBUM)
        }
        curEmmcStatusMap[droneDevice.getDeviceNumber().toString()] = emmcStorageStatus
    }

    /**
     * 添加监听
     */
    fun addListener() {
        if (isAdd) return
        isAdd = true
        //由1s改为3s，没必要那么实时
        TimerManager.addTimer3sEventListener(timeListener)
    }

    fun removeListener() {
        if (!isAdd) return
        isAdd = false
        TimerManager.removeTimerEventListener(timeListener)
    }

    /**
     * 切换存储位置弹框
     * @param event 存储事件
     */
    private fun showStorageEventDialog(droneDevice: IAutelDroneDevice, event: StorageEventEnum) {
        AutelLog.i(SdCardCheckerTag, "showDialog -> $event")
        if (AppInfoManager.isOnOTA()) {
            AutelLog.i(SdCardCheckerTag, "showDialog -> is on OTA, no need dialog")
            return
        }
        if (isShowDialog(droneDevice.getDeviceNumber())) return
        if (event == StorageEventEnum.UNKNOWN) return
        ((AppActivityManager.INSTANCE.getCurrentActivity()) as? FragmentActivity)?.let {
            updateShowDialog(droneDevice.getDeviceNumber(), true)
            CommonTwoButtonDialog(it).apply {
                if (DeviceUtils.allDrones().size > 1) {
                    val deviceName = droneDevice.getDeviceInfoBean()?.deviceName
                    if (deviceName?.isNotEmpty() == true) {
                        setTitle(it.getString(R.string.common_text_land_risk_title, deviceName))
                    }
                }
                setMessage(getDialogTips(event))
                setLeftBtnStr(getString(R.string.common_text_cancel))
                setRightBtnStr(getString(R.string.common_text_confirm))
                setLeftBtnListener {
                    setDialogStatus(droneDevice.getDeviceNumber(), false)
                }
                setRightBtnListener {
                    AutelLog.i(SdCardCheckerTag, "showDialog -> $event 点击确定")
                    dealConfirm(it, droneDevice, event)
                    updateShowDialog(droneDevice.getDeviceNumber(), false)
                }
            }.show()
            AutelLog.i(SdCardCheckerTag, "showDialog -> $event 弹框成功")
        }
    }

    private fun getDialogTips(storage: StorageEventEnum): String {
        when (storage) {
            StorageEventEnum.SDCARD_OUT_TO_CHANGE_EMMC -> {
                return getString(R.string.common_text_sdcard_out_to_change_emmc)
            }
            StorageEventEnum.DETECTED_SDCARD_TO_CHANGE -> {
                return getString(R.string.common_text_detected_sdcard_to_change)
            }
            StorageEventEnum.SDCARD_ERROR_TO_CHANGE_EMMC -> {
                return getString(R.string.common_text_tf_error_to_change_emmc)
            }
            StorageEventEnum.SDCARD_FULL_TO_CHANGE_EMMC -> {
                return getString(R.string.common_text_tf_full_to_change_emmc)
            }
            StorageEventEnum.SDCARD_UNKNOWN_FILESYSTEM_TO_CHANGE_EMMC -> {
                return getString(R.string.common_text_sdcard_unknown_filesystem_to_change_emmc)
            }
            StorageEventEnum.CLEAN_ALBUM -> {
                return getString(R.string.common_text_all_full_to_clean)
            }
            StorageEventEnum.EMMC_CHANGE_TO_TF -> {
                return getString(R.string.common_text_emmc_full_to_change_tf)
            }
            else -> {
                AutelLog.i(SdCardCheckerTag, "getDialogTips -> unknown storage event $storage")
                return ""
            }
        }
    }

    /**
     * 从缓存中读取是否已展示弹窗的数据
     * */
    private fun isShowDialog(deviceId: Int): Boolean {
        val key = deviceId.toString()
        if (isShowDialogMap.containsKey(key)) {
            return isShowDialogMap[key] ?: false
        }
        return false
    }

    /**
     * 更新是否已展示弹窗的数据缓存
     * */
    private fun updateShowDialog(deviceId: Int, show: Boolean) {
        val key = deviceId.toString()
        isShowDialogMap[key] = show
    }

    /**
     * 处理确定事件
     */
    private fun dealConfirm(context: Context, droneDevice: IAutelDroneDevice, event: StorageEventEnum, successBlock: (() -> Unit)? = null) {
        when (event) {
            StorageEventEnum.SDCARD_OUT_TO_CHANGE_EMMC,
            StorageEventEnum.SDCARD_ERROR_TO_CHANGE_EMMC,
            StorageEventEnum.SDCARD_FULL_TO_CHANGE_EMMC,
            -> {
                setStorageType(context, droneDevice, StorageTypeEnum.EMMC)
            }
            StorageEventEnum.DETECTED_SDCARD_TO_CHANGE -> {
                setStorageType(context, droneDevice, StorageTypeEnum.SD, successBlock)
            }
            StorageEventEnum.EMMC_CHANGE_TO_TF -> {
                setStorageType(context, droneDevice, StorageTypeEnum.SD)
            }
            StorageEventEnum.CLEAN_ALBUM -> {
                RouteManager.routeTo(context, RouterConst.PathConst.ACTIVITY_URL_ALBUM)
            }
            else -> {}
        }
    }

    /**
     * 切换存储位置卡
     */
    private fun setStorageType(context: Context, droneDevice: IAutelDroneDevice, storage: StorageTypeEnum, successBlock: (() -> Unit)? = null) {
        CameraSettingService.getInstance().generaSetting.setStorageType(droneDevice, storage, {
            successBlock?.invoke()
            AutelToast.normalToast(context, context.getString(R.string.common_text_setting_success))
            LiveDataBus.of(StorageLocationEvent::class.java).storageLocationChanged().post(storage)
        }, { throwable ->
            AutelLog.e(SdCardCheckerTag, "setStorageType: $throwable")
        })
    }

    fun setDialogStatus(deviceId: Int, show: Boolean) {
        updateShowDialog(deviceId, show)
    }
}