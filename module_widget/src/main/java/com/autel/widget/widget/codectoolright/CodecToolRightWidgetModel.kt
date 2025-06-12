package com.autel.widget.widget.codectoolright

import com.autel.common.base.widget.BaseWidgetModel
import com.autel.common.manager.MiddlewareManager
import com.autel.common.sdk.KeyManagerCoroutineWrapper
import com.autel.common.sdk.SDKResult
import com.autel.common.sdk.service.cameraSetting.CameraSettingService
import com.autel.common.utils.AutelPlaySoundUtil
import com.autel.common.utils.DeviceUtils
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.drone.sdk.vmodelx.manager.keyvalue.callback.CommonCallbacks
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.CameraKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.RemoteControllerKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.base.KeyTools
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.camera.bean.PhotoFileInfoBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.camera.bean.RecordFileInfoBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.camera.bean.RecordStatusBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.camera.bean.TakePhotoStatusBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.camera.enums.StorageTypeEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.camera.enums.TakePhotoStatusEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.enums.DroneWorkModeEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.enums.DroneWorkStateEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.remotecontrol.bean.HardwareButtonInfoBean
import com.autel.log.AutelLog
import com.autel.widget.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext

/**
 *  右侧工具状态
 */
class CodecToolRightWidgetModel : BaseWidgetModel() {

    val toolRightData = MutableSharedFlow<ToolRightStateModel>(replay = 1, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val photoFileData = MutableSharedFlow<PhotoFileInfoBean>(replay = 1, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val recordFileData = MutableSharedFlow<RecordFileInfoBean>(replay = 1, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val hardwareButtonData =
        MutableSharedFlow<HardwareButtonInfoBean>(replay = 1, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)


    private val keyPhotoFileInfo = KeyTools.createKey(CameraKey.KeyPhotoFileInfo)
    private val keyRecordFileInfo = KeyTools.createKey(CameraKey.KeyRecordFileInfo)
    private val keyTakePhotoStatus = KeyTools.createKey(CameraKey.KeyTakePhotoStatus)
    private val keyRecordStatus = KeyTools.createKey(CameraKey.KeyRecordStatus)
    private var rcHardWareInfoKey = KeyTools.createKey(RemoteControllerKey.KeyRCHardwareInfo)


    private val takePhotoStatusMap = HashMap<Int, TakePhotoStatusEnum>()
    private val recordStatusMap = HashMap<Int, RecordStatusBean>()


    private val photoFileListener = object : DeviceManager.KeyManagerListenerCallBack {

        override fun onListenerValueChanged(value: DeviceManager.DeviceListenerResult<*>) {
            if (value.drone == MiddlewareManager.codecModule.getSplitScreenDroneList()?.firstOrNull()) {
                (value.result as? PhotoFileInfoBean)?.let {
                    photoFileData.tryEmit(it)
                }
            }
        }
    }

    private val recordFileListener = object : DeviceManager.KeyManagerListenerCallBack {

        override fun onListenerValueChanged(value: DeviceManager.DeviceListenerResult<*>) {
            if (value.drone == MiddlewareManager.codecModule.getSplitScreenDroneList()?.firstOrNull()) {
                (value.result as? RecordFileInfoBean)?.let {
                    recordFileData.tryEmit(it)
                }
            }
        }
    }

    private val takePhotoStatusListener = object : DeviceManager.KeyManagerListenerCallBack {

        override fun onListenerValueChanged(value: DeviceManager.DeviceListenerResult<*>) {
            (value.result as? TakePhotoStatusBean)?.let {
                AutelLog.i("testCodeCR","keyTakePhotoStatus=$it ")
                takePhotoStatusMap[DeviceUtils.droneDeviceId(value.drone)] = it.state
                val controlDroneList = MiddlewareManager.codecModule.getSplitScreenDroneList()
                if (value.drone == controlDroneList?.firstOrNull()) {
                    if (it.state == TakePhotoStatusEnum.START) {
                        AutelPlaySoundUtil.get().play(R.raw.camera_takephoto_single)
                    }
                    fixedFrequencyRefresh()
                }
            }
        }
    }

    private val recordStatusListener = object : DeviceManager.KeyManagerListenerCallBack {

        override fun onListenerValueChanged(value: DeviceManager.DeviceListenerResult<*>) {
            (value.result as? RecordStatusBean)?.let {
                recordStatusMap[DeviceUtils.droneDeviceId(value.drone)] = it
                val controlDroneList = MiddlewareManager.codecModule.getSplitScreenDroneList()
                if (value.drone == controlDroneList?.firstOrNull()) {
                    fixedFrequencyRefresh()
                }
            }
        }
    }

    private var rcHardWareCallback = object : CommonCallbacks.KeyListener<HardwareButtonInfoBean> {
        override fun onValueChange(oldValue: HardwareButtonInfoBean?, newValue: HardwareButtonInfoBean) {
            hardwareButtonData.tryEmit(newValue)
        }
    }

    override fun setup() {
        super.setup()
        DeviceManager.getDeviceManager().addDroneDevicesListener(keyPhotoFileInfo, photoFileListener)
        DeviceManager.getDeviceManager().addDroneDevicesListener(keyRecordFileInfo, recordFileListener)
        DeviceManager.getDeviceManager().addDroneDevicesListener(keyTakePhotoStatus, takePhotoStatusListener)
        DeviceManager.getDeviceManager().addDroneDevicesListener(keyRecordStatus, recordStatusListener)
        DeviceManager.getDeviceManager().getLocalRemoteDevice().getKeyManager().listen(rcHardWareInfoKey, callback = rcHardWareCallback)
    }

    override fun cleanup() {
        super.cleanup()
        DeviceManager.getDeviceManager().removeDroneDevicesListener(keyPhotoFileInfo, photoFileListener)
        DeviceManager.getDeviceManager().removeDroneDevicesListener(keyRecordFileInfo, recordFileListener)
        DeviceManager.getDeviceManager().removeDroneDevicesListener(keyTakePhotoStatus, takePhotoStatusListener)
        DeviceManager.getDeviceManager().removeDroneDevicesListener(keyRecordStatus, recordStatusListener)
        DeviceManager.getDeviceManager().getLocalRemoteDevice().getKeyManager().cancelListen(rcHardWareInfoKey, callback = rcHardWareCallback)
    }

    private fun createToolRightModel(): ToolRightStateModel {
        val controlDroneList = MiddlewareManager.codecModule.getSplitScreenDroneList()
        val device = controlDroneList?.firstOrNull()

        val isSngleControl = DeviceUtils.isSingleControl()

        val isMainRc = DeviceUtils.isMainRC()
        if (device != null) {
            val workMode = device.getDeviceStateData().flightControlData.droneWorkMode
            val droneWorkStatus = device.getDeviceStateData().flightControlData.droneWorkStatus
            val inMission = DroneWorkModeEnum.isMissionMode(workMode) && droneWorkStatus == DroneWorkStateEnum.RUNNING
            if (!device.isConnected()) {
                takePhotoStatusMap.remove(DeviceUtils.droneDeviceId(device))
                recordStatusMap.remove(DeviceUtils.droneDeviceId(device))
            }
            return ToolRightStateModel(
                device.isConnected(),
                isSngleControl,
                isMainRc,
                takePhotoStatusMap[DeviceUtils.droneDeviceId(device)],
                recordStatusMap[DeviceUtils.droneDeviceId(device)],
                inMission
            )
        } else {
            return ToolRightStateModel(false, isSngleControl, isMainRc, null, null, false)
        }
    }


    override fun fixedFrequencyRefresh() {
        if (!DeviceUtils.isMainRC()) {
            return
        }
        val toolData = createToolRightModel()

        toolRightData.tryEmit(toolData)
    }


    suspend fun setStorageType(drone: IAutelDroneDevice, storageType: StorageTypeEnum) {
        CameraSettingService.getInstance().generaSetting.setStorageType(drone, storageType)
    }

    suspend fun formatSDCard(drone: IAutelDroneDevice) {
        CameraSettingService.getInstance().generaSetting.formatSDCard(drone, {}) {}
    }


    /**
     * 开始拍照
     */
    fun startTakePhoto(droneList: List<IAutelDroneDevice>) {
        droneList.map {
            CameraSettingService.getInstance().generaSetting.startTakingPhoto(it)
        }
    }

    suspend fun startRecord(
        droneList: List<IAutelDroneDevice>
    ): List<SDKResult<Void?>> {
        return withContext(Dispatchers.IO) {
            droneList.map {
                it to async {
                    kotlin.runCatching {
                        KeyManagerCoroutineWrapper.performAction(it.getKeyManager(), KeyTools.createKey(CameraKey.KeyStartRecord))
                    }
                }
            }.map {
                val (drone, result) = it
                SDKResult(drone, result.await())
            }
        }
    }

    suspend fun stopRecord(
        droneList: List<IAutelDroneDevice>
    ): List<SDKResult<Void?>> {
        return withContext(Dispatchers.IO) {
            droneList.map {
                it to async {
                    kotlin.runCatching {
                        KeyManagerCoroutineWrapper.performAction(it.getKeyManager(), KeyTools.createKey(CameraKey.KeyStopRecord))
                    }
                }
            }.map {
                val (drone, result) = it
                SDKResult(drone, result.await())
            }
        }
    }
}