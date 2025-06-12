package com.autel.setting.provider.delegate

import android.os.SystemClock
import android.text.TextUtils
import com.autel.common.constant.AppTagConst.FlyInitTag
import com.autel.common.delegate.application.AbsInitDeviceSwitch
import com.autel.common.feature.location.CountryManager
import com.autel.common.manager.AppInfoManager
import com.autel.common.manager.AutelStorageManager
import com.autel.common.manager.AutoDroneManager
import com.autel.common.manager.CommonDialogManager
import com.autel.common.manager.MiddlewareManager
import com.autel.common.manager.StorageKey
import com.autel.common.sdk.ModelXDroneConst
import com.autel.common.sdk.service.SettingService
import com.autel.common.utils.AutelDirPathUtils
import com.autel.common.utils.DeviceUtils
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.manager.CloudServiceManager
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.CommonKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.base.KeyTools
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.alink.enums.FccCeModeEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.bean.DroneSystemStateHFNtfyBean
import com.autel.log.AutelLog
import com.autel.map.bean.AutelLatLng
import com.autel.player.VideoType
import com.autel.player.player.AutelPlayerManager
import com.autel.setting.enums.CountryCodeEnum
import com.autel.storage.AutelDefaultStorageUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 用于初始化飞机连接成功后,初始化飞机参数,例如频段,RID等
 */
class FlyInitDelegateWrapper() : AbsInitDeviceSwitch(), CountryManager.OnCountryChangedListener {
    private val flyInitDataVM: FlyInitDataVM = FlyInitDataVM()
    private val droneSaveTime: HashMap<Int, Long> = HashMap<Int, Long>()

    init {
        DeviceManager.getDeviceManager().addDroneDevicesListener(KeyTools.createKey(
            CommonKey.KeyDroneSystemStatusHFNtfy
        ), object : DeviceManager.KeyManagerListenerCallBack {
            override fun onListenerValueChanged(value: DeviceManager.DeviceListenerResult<*>) {
                val result = value.result as? DroneSystemStateHFNtfyBean
                result?.let {
                    val lastTime = droneSaveTime.get(value.drone.deviceNumber()) ?: 0L
                    val currentTime = SystemClock.elapsedRealtime()
                    if (currentTime - lastTime > 2000) {
                        droneSaveTime.put(value.drone.deviceNumber(), currentTime)
                        val curLatLng =
                            AutelLatLng(it.droneLatitude, it.droneLongitude, it.altitude.toDouble())
                    }
                }
            }
        })
        //国家码发生变化时，需要校正
        CountryManager.addOnCountryChangedListener(this)
    }

    /**
     * 是否已经获取过飞机初始化数据
     * */
    private var flyInitDataCachedMap = HashMap<IAutelDroneDevice, Boolean>()

    private fun getFlyInitData(drone: IAutelDroneDevice) {
        AutelLog.i(FlyInitTag, "getFlyInitData deviceName:${DeviceUtils.droneDeviceName(drone)}")
        if (isFlyInitDataCached(drone)) return
        flyInitDataVM.getFlightParamsBatSeriousLowWarning(drone)
        flyInitDataVM.getFlightBatteryLowWarning(drone)
        flyInitDataVM.getDroneControlParams(drone)
        flyInitDataVM.getMaxRadius(drone)
        updateFlyInitDataCacheStatus(drone, true)
    }

    /**
     * 禁飞区使能校正
     */
    private fun fixNfzEnable(drone: IAutelDroneDevice) {
        if (!AppInfoManager.isSupportNoflySwitch()) {
            flyInitDataVM.setNoFlyEnable(drone, true)
        }
    }

    /**
     * 飞机初始化数据是否已经获取
     */
    private fun isFlyInitDataCached(drone: IAutelDroneDevice): Boolean {
        val cached = flyInitDataCachedMap[drone] != null || flyInitDataCachedMap[drone] == true
        AutelLog.i(
            FlyInitTag,
            "isFlyInitDataCached deviceName:${DeviceUtils.droneDeviceName(drone)} cached: $cached"
        )
        return cached
    }

    /**
     * 更新飞机初始化数据缓存状态;
     * cached: true:已经获取过初始化数据，false:未获取过初始化数据
     * */
    private fun updateFlyInitDataCacheStatus(drone: IAutelDroneDevice, cached: Boolean) {
        AutelLog.i(
            FlyInitTag,
            "updateFlyInitDataCacheStatus deviceName:${DeviceUtils.droneDeviceName(drone)} cached: $cached"
        )
        flyInitDataCachedMap[drone] = cached
    }


    override fun onDroneChangedListener(connected: Boolean, drone: IAutelDroneDevice) {
        if (connected) {
//            MiddlewareManager.workerHiddenProvider.refreshCpKey()
        } else {
            //飞机断连的时候把最后一个坐标存下来
            val lastLocation = AutelLatLng(
                drone.getDeviceStateData().flightControlData.droneLatitude,
                drone.getDeviceStateData().flightControlData.droneLongitude
            )
            //SaveLastLatLngUtils.saveLastLatLng(drone, lastLocation, true)
            updateFlyInitDataCacheStatus(drone, false)
        }
    }

    override fun onCameraAbilityFetchListener(
        localFetched: Boolean,
        remoteFetched: Boolean,
        drone: IAutelDroneDevice
    ) {
        AutelLog.i(
            FlyInitTag,
            "[onFlyAbilityFetchListener] localFetched: $localFetched, remoteFetched: $remoteFetched, drone: ${
                DeviceUtils.droneDeviceName(
                    drone
                )
            }"
        )
        //只做
        if (localFetched) initTimeRetroactive(drone)
        if (remoteFetched) initTimeRetroactive(drone)
    }

    /**
     * 默认开启30s的时光回溯功能，用于找飞机
     */
    private fun initTimeRetroactive(drone: IAutelDroneDevice) {
        AutelLog.i(FlyInitTag, "initTimeRetroactive -> drone=${drone.getName()} type=${DeviceUtils.getLookDroneVideoType()}")
        AutelPlayerManager.getInstance().setVideoCacheFileName("vl_video.mp4")
        AutelPlayerManager.getInstance().setCachePath(AutelDirPathUtils.getLookFlightVideoCachePath())
    }

    override fun onMainServiceValid(valid: Boolean, drone: IAutelDroneDevice) {
        AutelLog.d(
            FlyInitTag,
            "[onMainServiceValid] valid: $valid, drone: $drone, droneType=${drone.getDroneType()}"
        )
        if (drone.isConnected()) {
            getFlyInitData(drone)
        }
        if (valid) {
            //初始化云服务
            if (AppInfoManager.isSupportAiCloudService()) {
                CloudServiceManager.getInstance().getAiServiceManager().initObtainExtendDetect()
            }
            initCountryCode(drone)
            flyInitDataVM.initHotArea(drone)
            AutoDroneManager.setDroneType(drone.getDroneType())
            fixVisionPositioning(drone)
            CommonDialogManager.setNeedBandModeTips(true)
            if (AppInfoManager.isSupportUOM()) {
                flyInitDataVM.getAircraftUom(drone, {}, {})
            }
            fixStealthMode(drone)
            fixGnssSwitchMode(drone)
            fixNfzEnable(drone)
            fixAntiInterference()
            fixFlyHeight(drone)
        }
    }

    private fun fixFlyHeight(drone: IAutelDroneDevice) {
        if (AppInfoManager.isSupportLimitHeight()) {
            flyInitDataVM.getMaxHeight(drone) {
                if (it > ModelXDroneConst.DRONE_LIMIT_HEIGHT_MAX) {
                    flyInitDataVM.setMaxHeight(drone, 120f)
                }
            }
        }
    }

    /**
     * 抗干扰模式校正
     */
    private fun fixAntiInterference() {
        if (!AppInfoManager.isSupportAntiInterference()) {
            flyInitDataVM.setKeyALinkFccCeMode(FccCeModeEnum.FCC, {}, {})
        }
    }

    /***
     * 校正GNSS开关状态
     */
    private fun fixGnssSwitchMode(drone: IAutelDroneDevice) {
        //如果是非警务版本，则必须打开GNSS开关，防止有人串版本安装
        if (!AppInfoManager.isSupportGnssSwitch()) {
            flyInitDataVM.setGpsFlightSwitch(drone, true)
        }
    }

    /**
     * 视觉定位校准
     */
    private fun fixVisionPositioning(drone: IAutelDroneDevice) {
        if (!AppInfoManager.isSupportVisionPositioningCache()) return
        val isOpen = AutelStorageManager.getPlainStorage()
            .getBooleanValue(StorageKey.PlainKey.IS_VISION_SWITCH, true)
        AutelLog.i(FlyInitTag, "fixVisionPositioning ->　isOpen=$isOpen")
        flyInitDataVM.getLocationStatus(drone, onSuccess = {
            //如果不相等，则需要校正
            if (it != isOpen) {
                flyInitDataVM.setLocationStatus(drone, isOpen, {}, {})
            }
        }, {})
    }

    /**
     * 设置国家码给图传，让他配置屏蔽频段
     */
    private fun initCountryCode(drone: IAutelDroneDevice) {
        //必须使用缓存进去的国家码
        val countryCode = AutelDefaultStorageUtil.getInstance()
            .getStringValue(StorageKey.PlainKey.KEY_CURRENT_COUNTRY) ?: CountryCodeEnum.Other.code
        val isSupport = AppInfoManager.isSupportSetCountryCode()
        AutelLog.i(FlyInitTag, "initCountryCode -> isSupport=$isSupport countryCode=$countryCode")
        if (!isSupport || TextUtils.isEmpty(countryCode)) return

        flyInitDataVM.setCountryCode(drone, countryCode, onError = {
            CoroutineScope(Dispatchers.IO).launch {
                delay(1000)
                initCountryCode(drone)
            }
        })
    }

    override fun onDroneDestroy(drone: IAutelDroneDevice) {
        super.onDroneDestroy(drone)
        flyInitDataCachedMap.remove(drone)
    }

    override fun onCountryChanged(country: String) {
        DeviceManager.getDeviceManager().getDroneDevices().forEach {
            initCountryCode(it)
            //BandModeManager.startCheckBandMode(1)
            fixStealthMode(it)
        }
    }

    /**
     * 隐蔽模式
     */
    private fun fixStealthMode(drone: IAutelDroneDevice) {
        AutelLog.i(FlyInitTag, "fixStealthMode -> isSupportStealthMode=${AppInfoManager.isSupportStealthMode()} ${drone.getName()}")
        //如果不支持隐蔽模式时，需要强制把隐蔽模式关闭
        if (!AppInfoManager.isSupportStealthMode()) {
            SettingService.getInstance().lightSettingService.setSilentMode(drone, false, {
                AutelLog.i(FlyInitTag, "fixStealthMode -> success ${drone.getName()}")
            }, {
                AutelLog.i(FlyInitTag, "fixStealthMode -> failure ${drone.getName()}")
            })
        }
    }

}