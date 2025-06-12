package com.autel.widget.widget.statusbar.wm

import android.os.SystemClock
import android.util.Log
import com.autel.common.constant.AppTagConst.RtkTag
import com.autel.common.constant.AppTagConst.StatusBarView
import com.autel.common.feature.compass.Compass
import com.autel.common.feature.location.CountryManager
import com.autel.common.feature.phone.PhoneBatteryManager
import com.autel.common.lifecycle.LiveDataBus
import com.autel.common.lifecycle.event.RTKStatusEvent
import com.autel.common.manager.AppInfoManager
import com.autel.common.manager.AutelStorageManager
import com.autel.common.manager.LawHeightDelegateManager
import com.autel.common.manager.StorageKey
import com.autel.common.manager.UomReportManager
import com.autel.common.sdk.GpsSignalLevelEnum
import com.autel.common.sdk.KeyManagerCoroutineWrapper
import com.autel.common.sdk.RemoteSignalLevelEnum
import com.autel.common.sdk.service.SettingService
import com.autel.common.utils.DeviceUtils
import com.autel.common.utils.GoogleTextToSpeechManager
import com.autel.common.utils.HandlerUtils
import com.autel.common.utils.UIConstants.DEFAULT_CRITICAL_LOW_BATTERY
import com.autel.common.utils.UIConstants.DEFAULT_LOW_BATTERY
import com.autel.common.widget.toast.AutelToast
import com.autel.drone.sdk.libbase.error.IAutelCode
import com.autel.drone.sdk.vmodelx.device.IAutelDroneListener
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.interfaces.IKeyManager
import com.autel.drone.sdk.vmodelx.interfaces.IRTKManager
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.drone.sdk.vmodelx.manager.keyvalue.callback.CommonCallbacks
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.CommonKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.FlightControlKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.base.KeyTools
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.camera.enums.CardStatusEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.common.bean.WarningAtom
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.common.enums.WaringIdEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.bean.DroneAutoBackBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.enums.AutoBackEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.enums.DroneFlightModeEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.enums.GearLevelEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.enums.RemoteIdStatusEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.rtk.bean.NestRtkStatusNotifyBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.rtk.bean.RtkReportBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.rtk.enums.RTKPositionTypeEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.rtk.enums.RTKSignalEnum
import com.autel.log.AutelLog
import com.autel.widget.R
import com.autel.widget.widget.statusbar.bean.BatteryInfo
import com.autel.widget.widget.statusbar.bean.RemoteBattery
import com.autel.widget.widget.statusbar.bean.SignalStrength
import com.autel.widget.widget.statusbar.manager.AutoCheckStorageManager
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

interface StatusBarViewInterface {
    /**
     * 强制刷新告警列表
     * */
    fun forceRefreshWarnList()
}

/**
 * 实时状态栏数据
 */
class RealTimeStatusBarWidget : StatusBarWidget(), IAutelDroneListener {
    private var rtkCallbacks = mutableMapOf<Int, IRTKManager.RTKReportInfoCallback>()
    private var scope: CoroutineScope? = null

    private var remoteCurrentBattery: Int = 0

    private var remoteTotalBattery: Int = 0

    var centerNodeDrone: IAutelDroneDevice? = null

    var listener: StatusBarViewInterface? = null

    private var runTimeWarnMap = mutableMapOf<Int, WarningAtom>()

    //rid自检状态
    private val ridCheckStatus = mutableMapOf<String, RemoteIdStatusEnum>()

    //rtk多机CP下关闭请求发起时间，控频用
    private val rtkCheckStatus = mutableMapOf<Int, Long>()

    //ads-b告警强制刷新列表去更新飞机相对距离和高度
    private var forceUpdateTimeInterval = 0L

    /**
     * 多控档位不一致下，是否正在自动同步
     * */
    private var isSyncGear = false

    /**
     * 遥控器电池电量
     * */
    private val mRcBatteryListener = PhoneBatteryManager.BatteryChangeListener { current, total ->
        remoteCurrentBattery = current
        remoteTotalBattery = total
    }

    /**
     * 根据飞机列表更新单控飞机名称
     * */
    private fun updateSingleControlName() {
        deviceName.value = currentControlledDroneName()
    }

    override fun setup() {
        super.setup()
        DeviceManager.getDeviceManager().addDroneListener(this)
        addRunTimeWarnObserver()
        PhoneBatteryManager.sPhoneBatteryManager.addBatteryChangeListener(mRcBatteryListener)
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate + CoroutineExceptionHandler { _, throwable ->
            AutelLog.w(StatusBarView, Log.getStackTraceString(throwable))
        })
        if (isMainRC()) {
            AutoCheckStorageManager.addListener()
        }
        DeviceManager.getDeviceManager().getDroneDevices().forEach {
            registerRTKCallback(it)
        }
    }

    /**
     * 实时告警处理逻辑
     * */
    private val runTimeWarnCallback = object : DeviceManager.KeyManagerListenerCallBack {
        override fun onListenerValueChanged(value: DeviceManager.DeviceListenerResult<*>) {
            (value.result as? WarningAtom)?.let { atom ->
                AutelLog.i(StatusBarView, "KeyDroneRuntimeWarning -> $atom")
                runTimeWarnMap[value.drone.getDeviceNumber()] = atom
                updateDeviceWarns()
                runTimeWarnMap.remove(value.drone.getDeviceNumber())
            }
        }
    }

    private val runTimeRemoteControlWarnCallback = object : CommonCallbacks.KeyListener<WarningAtom> {
        override fun onValueChange(oldValue: WarningAtom?, newValue: WarningAtom) {
            AutelLog.i(StatusBarView, "RC KeyDroneRuntimeWarning -> $newValue")
            newValue.let {
                runTimeWarnMap[DeviceUtils.getLocalRemoteDevice().getDeviceNumber()] = newValue
                updateDeviceWarns()
                runTimeWarnMap.remove(DeviceUtils.getLocalRemoteDevice().getDeviceNumber())
            }
        }
    }

    /**
     * 添加实时告警监听
     * */
    private fun addRunTimeWarnObserver() {
        val key = KeyTools.createKey(CommonKey.KeyDroneRuntimeWarning)
        DeviceManager.getDeviceManager().addDroneDevicesListener(key, runTimeWarnCallback)
        DeviceUtils.getLocalRemoteDevice().getKeyManager()
            .listen(key, runTimeRemoteControlWarnCallback)
    }

    private fun removeRunTimeWarnObserver() {
        val key = KeyTools.createKey(CommonKey.KeyDroneRuntimeWarning)
        DeviceManager.getDeviceManager().removeDroneDevicesListener(key, runTimeWarnCallback)
        DeviceUtils.getLocalRemoteDevice().getKeyManager()
            .cancelListen(key, runTimeRemoteControlWarnCallback)
    }


    /**
     * 获取所有设备的告警并更新给界面
     * */
    private fun updateDeviceWarns() {
        //是否需要强制刷新告警列表
        // 从遥控器不显示告警，不进行语音播报
        if (!isMainRC()) return
        var currentTimeInterval = 0L
        val atomLists = mutableListOf<DeviceWarnAtomList>()

        val remoteWarnList = mutableListOf<WarningAtom>()
        if (AppInfoManager.isSupportRemoteCompassWarn()) {
            if (Compass.getInstance().getAccuracyStatus() == false) {
                remoteWarnList.add(WarningAtom(warningId = WaringIdEnum.REMOTE_COMPASS_NEED_CALIBRATION))
            }
        }
        if (AppInfoManager.isSupportOfflineWarn()) {
            remoteWarnList.addAll(DeviceUtils.getLocalRemoteDevice().getDeviceStateData().remoteWarningData.warningAtomList)
        }
        if (UomReportManager.isNeedWarnNoNetwork()) {
            remoteWarnList.add(WarningAtom(warningId = WaringIdEnum.NO_INTERNET_FOR_UOM_REPORT))
        }
        atomLists.add(DeviceWarnAtomList(DeviceUtils.getLocalRemoteDevice(), remoteWarnList))

        if (DeviceUtils.hasDroneConnected()) {
            DeviceUtils.allOnlineDrones().forEach {
                val array = mutableListOf<WarningAtom>()
                it.getDeviceStateData().droneWarningData.warningAtomList.forEach { warningAtom ->
                    array.add(warningAtom)
                    //目前ads-b告警只显示强中告警，强中告警跟随设置走显示，弱告警始终不显示
                    if (warningAtom.warningId == WaringIdEnum.ADSB_WARN_MIDDLE ||
                        warningAtom.warningId == WaringIdEnum.ADSB_WARN_STRONG
                    ) {
                        currentTimeInterval = SystemClock.elapsedRealtime()
                    }
                }

                val sn = it.getDeviceStateData().systemInfoData.droneSN
                if (AppInfoManager.isSupportRidCheckSelf()) {
                    val status = it.getDeviceStateData().flightControlData.remoteIdStatus
                    //AutelLog.d(StatusBarView,"RID自检状态，sn=$sn isConnected=${it.isConnected()} status=$status ")
                    //RID自检处理逻辑
                    sn?.let { droneSn ->
                        //如果设备已连接
                        if (it.isConnected()) {
                            //如果设备未添加过，则先添加
                            if (ridCheckStatus[droneSn] == null) {
                                ridCheckStatus[droneSn] = RemoteIdStatusEnum.UNKNOWN
                            }

                            //RID自检错误告警
                            /*if (status == RemoteIdStatusEnum.AUTO_CHECK_ERROR) {
                                array.add(WarningAtom(warningId = WaringIdEnum.RID_CHECK_ERROR))
                            }*/

                            //当RID状态由非OPEN变化到OPEN时，认为自检通过
                            if (ridCheckStatus[droneSn] != RemoteIdStatusEnum.OPEN && status == RemoteIdStatusEnum.OPEN) {
                                AutelLog.i(StatusBarView, "RID自检成功，preStatue=${ridCheckStatus[droneSn]} sn=$sn")
                                AutelToast.normalToast(AppInfoManager.getApplicationContext(), R.string.common_text_rid_check_success)
                                GoogleTextToSpeechManager.instance()
                                    .speak(AppInfoManager.getApplicationContext().getString(R.string.common_text_rid_check_success), true)
                            }
                            ridCheckStatus[droneSn] = status
                        } else {
                            //如果离线，则置成unknown
                            if (ridCheckStatus[droneSn] != null) {
                                ridCheckStatus[droneSn] = RemoteIdStatusEnum.UNKNOWN
                            }
                        }
                    }
                }

                val warningAtom = runTimeWarnMap[it.getDeviceNumber()]
                if (warningAtom != null) array.add(warningAtom)
                if (it.getDeviceStateData().flightoperateData.bAircraftActivation == false) {
                    if (!array.any { atom -> atom.warningId == WaringIdEnum.UAV_NOT_ACTIVATED }) {
                        array.add(WarningAtom(warningId = WaringIdEnum.UAV_NOT_ACTIVATED))
                    }
                }
                val currentHeight = it.getDeviceStateData().flightControlData.altitude
                if (currentHeight > LawHeightDelegateManager.getLowHeight(CountryManager.currentCountry)) {
                    array.add(WarningAtom(warningId = WaringIdEnum.HEIGHT_EXCEEDS_LAW_HEIGHT))
                }
                atomLists.add(DeviceWarnAtomList(it, array))
            }
        } else {
            atomLists.add(getDisconnectWarn())
        }
        warningAtomList.value = atomLists
        if (currentTimeInterval != 0L) {
            //1秒钟更新一次ads-b告警数据
            if (currentTimeInterval - forceUpdateTimeInterval >= 1000) {
                listener?.forceRefreshWarnList()
                forceUpdateTimeInterval = currentTimeInterval
            }
        }
    }

    private fun isMultiControl(): Boolean {
        return !DeviceUtils.isSingleControl()
    }

    /**
     * 获取离线的告警，自己构建
     */
    private fun getDisconnectWarn(): DeviceWarnAtomList {
        val disconnectWaring = WarningAtom(warningId = WaringIdEnum.AIRCRAFT_DISCONNECT)
        return DeviceWarnAtomList(centerNodeDrone, listOf(disconnectWaring))
    }

    /**定频刷新数据*/
    override fun fixedFrequencyRefresh() {
        if (DeviceUtils.isNetMeshMatchCp()) {
            closeAllRtkForNetMeshCP()
            LiveDataBus.of(RTKStatusEvent::class.java).isEnabled().post(false)
        }
        remoteBattery.value = RemoteBattery(remoteCurrentBattery, remoteTotalBattery)
        updateSingleControlName()
        isMainRc.value = isMainRC()
        val drone = DeviceUtils.singleControlDrone()
        val remoteCacheData = DeviceUtils.getLocalRemoteDevice().getDeviceStateData()
        val isSingleControl = DeviceUtils.isSingleControl()
        val hasDroneConnected = DeviceUtils.hasDroneConnected()
        val isMainRc = DeviceUtils.isMainRC()
        val isNetMeshing = DeviceUtils.isNetMeshing()
        singleControlMode.value = isSingleControl
        droneConnectStatus.value = hasDroneConnected
        if (!hasDroneConnected) {
            deviceName.value = null
            if (isMainRc) {
                warningAtomList.value = mutableListOf(getDisconnectWarn())
            }
            signalStrength.value = SignalStrength(GpsSignalLevelEnum.GPS_SIGNAL_LEVEL_NONE, 0, RemoteSignalLevelEnum.REMOTE_SIGNAL_LEVEL_NONE)
            droneGear.value = GearLevelEnum.UNKNOWN
            batteryInfo.value = BatteryInfo(
                "",
                null,
                DEFAULT_CRITICAL_LOW_BATTERY,
                DEFAULT_LOW_BATTERY,
                null,
                null
            )

            visionStateInfo.value = false
            droneWorkMode.value = null
            mainMode.value = null
            slamConfidence.value = 0.toDouble()
            cardStatusInfo.value = CardStatusEnum.UNKNOWN
            flightMode.value = DroneFlightModeEnum.UNKNOWN

            //放在最后
            dismissRtkUI(null)
        } else {
            if (isNetMeshing) return
            updateDeviceWarns()
            if (drone == null) return
            if (!drone.isConnected()) {
                signalStrength.value = SignalStrength(GpsSignalLevelEnum.GPS_SIGNAL_LEVEL_NONE, 0, RemoteSignalLevelEnum.REMOTE_SIGNAL_LEVEL_NONE)
                droneGear.value = GearLevelEnum.UNKNOWN
                batteryInfo.value = BatteryInfo(
                    "",
                    null,
                    DEFAULT_CRITICAL_LOW_BATTERY,
                    DEFAULT_LOW_BATTERY,
                    null,
                    null
                )

                visionStateInfo.value = false
                droneWorkMode.value = null
                mainMode.value = null
                slamConfidence.value = 0.toDouble()
                cardStatusInfo.value = CardStatusEnum.UNKNOWN
                flightMode.value = DroneFlightModeEnum.UNKNOWN

                //放在最后
                dismissRtkUI(null)
                return
            }//不在线的飞机不处理
            //非单控，不执行后续罗，只显示遥控器电量
            if (!isSingleControl) return
            if (DeviceUtils.isMainDrone(drone)) {
                if (centerNodeDrone != drone) centerNodeDrone = drone
            }
            val status = drone.getDeviceStateData()
            val flight = status.flightControlData
            batteryInfo.value = BatteryInfo(
                drone.getDroneType().value,
                flight.batteryPercentage.toInt(),
                status.flightoperateData.iBatSeriousLowWarningValue,
                status.flightoperateData.iBatteryLowWarningValue,
                flight.batteryTemperature,
                flight.batteryVoltage
            )
            signalStrength.value = SignalStrength(
                GpsSignalLevelEnum.parseValue(flight.gpsStrengthPercentage),
                flight.satelliteCount,
                RemoteSignalLevelEnum.parseValue(remoteCacheData.rcStateNtfyBean.rcSignalQuality)
            )
            /**rtk*/
            //只支持点对点对频
            if (DeviceUtils.isP2PMatchCp()) {
                rtkSVCount.value = status.rtkReportData?.svCnt
                rtkFixStatus.value = status.rtkReportData?.fixSta
                val type = status.rtkReportData?.posType
                if (type != null) {
                    rtkPosType.value = type
                }
                val rtkExist = drone.getRtkManager().isConnected
                rtkSupport.value = rtkExist
                rtkEnable.value = rtkExist && drone.getRtkManager().isenableRTKLocation() == true
                LiveDataBus.of(RTKStatusEvent::class.java).isEnabled().post(rtkExist)
            }

            droneGear.value = flight.droneGear
            droneWorkMode.value = drone.getDeviceStateData().flightControlData.droneWorkMode
            mainMode.value = drone.getDeviceStateData().flightControlData.mainMode
            slamConfidence.value = drone.getDeviceStateData().flightControlData.slamConfidence
            environmentInfo.value = flight.environmentInfo
            flightMode.value = drone.getDeviceStateData().flightControlData.flightMode

            visionStateInfo.value = drone.getDeviceStateData().flightControlData.obstacleAvoidanceEnabled == true
            val storageStatus = drone.getDeviceStateData().sdcardData.sdStorageStatus
            cardStatusInfo.value = storageStatus
        }
    }

    fun landing(
        isLanding: Boolean,
        deviceId: Int,
        onSuccess: ((Boolean) -> Unit)?,
    ) {
        val drone = DeviceUtils.allOnlineDrones().firstOrNull() {
            it.getDeviceNumber() == deviceId
        }
        if (drone != null) {
            AutelLog.i(StatusBarView, "landing ${DeviceUtils.droneDeviceName(drone)} isLanding:$isLanding")
            landing(isLanding, drone, onSuccess)
        }
    }

    private fun landing(
        isLanding: Boolean,
        drone: IAutelDroneDevice,
        onSuccess: ((Boolean) -> Unit)?,
    ) {
        SettingService.getInstance().flightControlService.landing(drone, isLanding, onSuccess = {
            onSuccess?.invoke(true)
        }, onError = {
            onSuccess?.invoke(false)
        })
    }

    /**
     * 给指定飞机设置是否自动返航
     * */
    fun autoBack(
        isReturn: Boolean,
        deviceId: Int,
        onSuccess: ((Boolean) -> Unit)?,
    ) {
        val drone = DeviceUtils.allOnlineDrones().firstOrNull() {
            it.getDeviceNumber() == deviceId
        }
        if (drone != null) {
            AutelLog.i(StatusBarView, "autoBack ${DeviceUtils.droneDeviceName(drone)} isReturn:$isReturn")
            autoBack(isReturn, drone, onSuccess)
        }
    }

    private fun autoBack(
        isReturn: Boolean,
        drone: IAutelDroneDevice,
        onSuccess: ((Boolean) -> Unit)?,
    ) {
        scope?.launch(CoroutineExceptionHandler { _, _ ->
            onSuccess?.invoke(false)
        }) {
            val key = KeyTools.createKey(FlightControlKey.KeyStartStopAutoBack)
            KeyManagerCoroutineWrapper.performAction(getKeyManager(drone), key, DroneAutoBackBean(isReturn, AutoBackEnum.HOMEPOINT))
            onSuccess?.invoke(true)
        }
    }

    /**
     * 给所有飞机设置档位
     * */
    fun setFlightGear(gear: GearLevelEnum) {
        DeviceUtils.allControlDrones().forEach {
            setFlightGear(gear, it, {}, {})
        }
    }

    /**
     * 切换档位
     * */
    private fun setFlightGear(
        gear: GearLevelEnum,
        drone: IAutelDroneDevice,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        SettingService.getInstance().flightParamService.setGearLevel(drone, gear, {
            onSuccess.invoke()
        }, {
            onError.invoke(it)
            AutelLog.e(StatusBarView, "setFlightGear onError:$it $gear")
        })
    }

    /**
     * 是否需要将多个飞机的档位同步一致（只针对多控下档位不一致的情况）
     * */
    private fun isNeedSyncAircraftGear(): Boolean {
        if (!isMainRC()) return false
        if (!isSyncGear) {
            val aircraftList = DeviceUtils.allOnlineDrones()
            if (aircraftList.size > 1) {
                return aircraftList.map { it.getDeviceStateData().flightControlData.droneGear }.distinct().size != 1
            }
            return false
        } else {
            return false
        }

    }

    /**
     * 多控下同步多个飞机的档位为标准档（只针对多控下档位不一致的情况）
     * */
    private fun syncMultiControlAircraftGear() {
        if (isMultiControl()) {
            if (isNeedSyncAircraftGear()) {
                if (!isSyncGear) {
                    isSyncGear = true
                    syncAircraftGear(DeviceUtils.allOnlineDrones())
                }
            }
        }
    }

    /**
     * 同步飞机的档位为标准档（只针对多控下档位不一致的情况）
     * */
    private fun syncAircraftGear(aircraftList: List<IAutelDroneDevice>, index: Int = 0) {
        if (index < aircraftList.size) {
            var tempIndex = index
            val gear = GearLevelEnum.NORMAL
            val aircraft = aircraftList[tempIndex]
            val currentGear = aircraft.getDeviceStateData().flightControlData.droneGear
            AutelLog.i(
                StatusBarView,
                "多控下设置飞机档位,deviceName:${DeviceUtils.droneDeviceName(aircraft)} isConnected:${aircraft.isConnected()} currentGear:$currentGear"
            )
            if (aircraft.isConnected() && currentGear != gear) {//当前飞机在线，且档位不是标准档
                setFlightGear(gear, aircraft, {
                    tempIndex++
                    syncAircraftGear(aircraftList, tempIndex)
                }, {
                    scope?.launch {
                        delay(1000)
                        //请求失败可能飞机列表发生变化，所以重新开始检查一下
                        isSyncGear = false
                        syncMultiControlAircraftGear()
                    }
                })
            } else { //飞机未连接、或则档位相同，直接处理下一个飞机
                tempIndex++
                syncAircraftGear(aircraftList, tempIndex)
            }
        } else {
            isSyncGear = false
        }
    }

    /**
     * 获取飞机的KeyManager
     * */
    private fun getKeyManager(drone: IAutelDroneDevice): IKeyManager {
        return drone.getKeyManager()
    }

    override fun cleanup() {
        super.cleanup()
        DeviceManager.getDeviceManager().removeDroneListener(this)
        removeRunTimeWarnObserver()
        PhoneBatteryManager.sPhoneBatteryManager.removeBatteryChangeListener(mRcBatteryListener)
        //AutoCheckStorageManager.removeListener()
        scope?.cancel()
        scope = null
    }

    override fun onDroneChangedListener(connected: Boolean, drone: IAutelDroneDevice) {
        AutelLog.i(StatusBarView, "onDroneChangedListener ${DeviceUtils.droneDeviceName(drone)}")
        if (connected) {
            registerRTKCallback(drone)
        } else {
            unRegisterRTKCallback(drone)
        }
    }

    private fun dismissRtkUI(drone: IAutelDroneDevice?) {
        HandlerUtils.post {
            rtkSVCount.value = null
            rtkFixStatus.value = null
            rtkPosType.value = RTKPositionTypeEnum.UNKNOWN_POSITION
            rtkSupport.value = false
            val rtkSupport = drone?.getRtkManager()?.isConnected == true
            rtkEnable.value = false
            LiveDataBus.of(RTKStatusEvent::class.java).isEnabled().post(rtkSupport)
        }
    }

    private fun registerRTKCallback(drone: IAutelDroneDevice) {
        val callback = object : IRTKManager.RTKReportInfoCallback {
            override fun onRtkIniComplement() {
                AutelLog.d(RtkTag, "onRtkIniComplement")
                val port = urlPort(drone)
                if (!port.isNullOrEmpty()) {
                    drone.getRtkManager().updateNetRtkType(false, urlHost(drone), urlPort(drone)?.toInt(), updateConfigCallback)
                } else {
                    AutelLog.e(RtkTag, "port is error:$port")
                }
            }

            override fun onRtkInitialFailed(error: IAutelCode, msg: String?) {
            }

            override fun onRtkUnConnected(singnalEnum: RTKSignalEnum) {
                AutelLog.d(RtkTag, "onRtkUnConnected ${DeviceUtils.droneDeviceName(drone)} $singnalEnum")

                if (DeviceUtils.allControlDrones().size != 1) return
                if (drone.getDeviceNumber() == DeviceUtils.allControlDrones().firstOrNull()?.getDeviceNumber()) {
                    dismissRtkUI(drone)
                }
            }

            override fun onRtkMountPointList(list: ArrayList<String>?) {
                AutelLog.d(RtkTag, "RtkMountPointList:$list")
            }

            override fun onRtkReportInfo(reportInfo: RtkReportBean) {
                //AutelLog.d(RtkTag, "onRtkReportInfo:${droneDevice.getDeviceInfoBean().deviceName} $reportInfo")
            }

            override fun onNestRtkReportInfo(nestRtkReportInfo: NestRtkStatusNotifyBean) {

            }
        }
        if (rtkCallbacks[drone.getDeviceNumber()] == null) {
            AutelLog.i(RtkTag, "registerRTKCallback ${DeviceUtils.droneDeviceName(drone)}")
            rtkCallbacks[drone.getDeviceNumber()] = callback
            drone.getRtkManager().registerRtkInfoCallBack(callback)
        }
    }

    // 更新RTK配置回调
    private var updateConfigCallback = object : IRTKManager.ChangeRTKConfigCallback {
        override fun onNeedAuterInfo(singnalEnum: RTKSignalEnum, isQianxun: Boolean) {
        }

        override fun onUpdateConfigSuccess() {

        }

        override fun onUpdateConfigFailure(code: IAutelCode, msg: String?) {

        }
    }

    private fun unRegisterRTKCallback(drone: IAutelDroneDevice) {
        val callback = rtkCallbacks[drone.getDeviceNumber()]
        if (callback != null) {
            rtkCallbacks.remove(drone.getDeviceNumber())
            AutelLog.i(RtkTag, "unRegisterRTKCallback ${DeviceUtils.droneDeviceName(drone)}")
            drone.getRtkManager().unRegisterRtkInfoCallBack(callback)
        }
    }

    /**
     * 针对组网多机下，不同飞机使用不同的账号问题,所以关联账号要绑定deviceID
     * */
    private fun loginHistoryAccountKey(key: String, drone: IAutelDroneDevice): String {
        val deviceId = drone.getDeviceNumber()
        return "$deviceId" + key
    }

    private fun urlHost(drone: IAutelDroneDevice): String? {
        val key = loginHistoryAccountKey(StorageKey.PlainKey.KEY_RTK_SERVICE_ADDR, drone)
        return AutelStorageManager.getPlainStorage().getStringValue(key, "")
    }

    private fun urlPort(drone: IAutelDroneDevice): String? {
        val key = loginHistoryAccountKey(StorageKey.PlainKey.KEY_RTK_PORT, drone)
        return AutelStorageManager.getPlainStorage().getStringValue(key, "8002")
    }

    /**
     * 当前受控飞机的名称
     * */
    private fun currentControlledDroneName(): String {
        return DeviceUtils.droneDeviceName(DeviceUtils.singleControlDrone())
    }

    /**
     * 是否为主遥控器
     * */
    private fun isMainRC(): Boolean {
        return DeviceUtils.isMainRC()
    }

    fun getDrone(deviceId: Int): IAutelDroneDevice? {
        return DeviceUtils.getDrone(deviceId)
    }

    private fun closeAllRtkForNetMeshCP() {
        if (!DeviceUtils.isMainRC()) return
        if (DeviceUtils.isNetMeshMatchCp()) {
            DeviceUtils.allDrones().forEach {
                if (!it.isConnected()) return
                closeRtkForNetMeshCP(it)
            }
        }
    }

    /**
     * 多机CP下，禁用RTK
     * 主遥控器，多机CP下，禁用RTK，2秒检查一次
     * */
    private fun closeRtkForNetMeshCP(drone: IAutelDroneDevice) {
        if (DeviceUtils.isNetMeshMatchCp()) {
            val currentTime = SystemClock.elapsedRealtime()
            val deviceId = drone.getDeviceNumber()
            val lastTime = rtkCheckStatus[deviceId] ?: 0
            if (currentTime - lastTime < 2000) {
                return
            }
            val isEnable = drone.getRtkManager().isenableRTKLocation()
            if (isEnable) {
                AutelLog.i(RtkTag, "closeRtkForNetMeshCP $deviceId isEnable:$isEnable")
                drone.getRtkManager().closeCurrentRtk(object : CommonCallbacks.CompletionCallback {
                    override fun onSuccess() {
                    }

                    override fun onFailure(code: IAutelCode, msg: String?) {
                    }
                })
                drone.getRtkManager().enableRTKLocation(false, object : IRTKManager.ChangeRTKConfigCallback {
                    override fun onNeedAuterInfo(singnalEnum: RTKSignalEnum, isQianxun: Boolean) {
                        AutelLog.i(RtkTag, "closeRtkForNetMeshCP onNeedAuterInfo $singnalEnum $isQianxun")
                    }

                    override fun onUpdateConfigSuccess() {
                        AutelLog.i(RtkTag, "closeRtkForNetMeshCP onUpdateConfigSuccess")
                    }

                    override fun onUpdateConfigFailure(code: IAutelCode, msg: String?) {
                        AutelLog.i(RtkTag, "closeRtkForNetMeshCP onUpdateConfigFailure $code $msg")
                    }
                })
            }
            rtkCheckStatus[deviceId] = currentTime
        }
    }
}