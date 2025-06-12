package com.external.uxdemo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Process
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.autel.common.base.AppActivityManager
import com.autel.common.base.BaseApp
import com.autel.common.constant.AppTagConst
import com.autel.common.feature.route.RouteManager
import com.autel.common.feature.route.RouterConst
import com.autel.common.feature.route.RouterDataKey
import com.autel.common.manager.AppInfoManager
import com.autel.common.manager.AutelStorageManager
import com.autel.common.manager.StorageKey
import com.autel.common.manager.appinfo.AppRunningDeviceEnum
import com.autel.common.sdk.ModelXDroneConfig
import com.autel.common.sdk.ModelXDroneConst
import com.autel.common.utils.AutelDirPathUtils
import com.autel.common.utils.AutelPlaySoundUtil
import com.autel.common.utils.DeviceUtils
import com.autel.common.utils.GoogleTextToSpeechManager
import com.autel.common.utils.HookUtils
import com.autel.data.database.DBManager
import com.autel.drone.sdk.vmodelx.SDKInitConfig
import com.autel.drone.sdk.vmodelx.SDKManager
import com.autel.drone.sdk.vmodelx.device.IAutelDroneListener
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.interfaces.IMeshDeviceChangedListener
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.nest.enums.ModemModeEnum
import com.autel.log.AutelLog
import com.autel.map.util.MapBoxNetWorksUtils
import com.autel.storage.AutelStorage
import com.autel.utils.ScreenUtils
import dagger.hilt.android.HiltAndroidApp
import com.autel.player.player.render.RenderMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.external.uxdemo.view.UxsdkDemoActivity


@HiltAndroidApp
open class Application : BaseApp(), IAutelDroneListener {

    override fun onCreate() {
        super.onCreate()
        init()
    }

    private fun init() {
        initNetworkRefresh()
        AppInfoManager.setApplicationContext(this)
        initComponent()
        initAppInfo()
        initSdk()

        initStorageVersion()
        CoroutineScope(Dispatchers.IO).launch {
            //数据库初始化
            DBManager.initDB(this@Application)
            //语音服务初始化
            GoogleTextToSpeechManager.instance()
            AutelPlaySoundUtil.get().init(this@Application)
            //Android 8.0系统针对系统应用使用webView适配
            HookUtils.hookWebView()
            registerRestartReceiver()
        }
    }



    private fun initNetworkRefresh() {
        MapBoxNetWorksUtils.isInternetAvailable()
    }

    private fun initStorageVersion() {
        if (StorageKey.KeyVersion.KEY_SCREEN_STATE_VERSION > AutelStorageManager.getPlainStorage()
                .getIntValue(StorageKey.PlainKey.KEY_SCREEN_STATE_VERSION, 0)
        ) {
            AutelStorageManager.getPlainStorage()
                .removeValueForKey(StorageKey.PlainKey.KEY_ALL_SCREEN_STATE)
            AutelStorageManager.getPlainStorage()
                .removeValueForKey(StorageKey.PlainKey.KEY_TWO_SCREEN_STATE)
            AutelStorageManager.getPlainStorage()
                .removeValueForKey(StorageKey.PlainKey.KEY_THREE_SCREEN_STATE)
            AutelStorageManager.getPlainStorage()
                .removeValueForKey(StorageKey.PlainKey.KEY_FOURTH_SCREEN_STATE)

            AutelStorageManager.getPlainStorage()
                .setIntValue(
                    StorageKey.PlainKey.KEY_SCREEN_STATE_VERSION,
                    StorageKey.KeyVersion.KEY_SCREEN_STATE_VERSION
                )
        }
    }


    /**
     * 初始化AppInfoManager
     */
    private fun initAppInfo() {
        if (AppInfoManager.isAutelControl()) {
            val deviceEnum = if (2000 == ScreenUtils.getScreenWidth(this)) {
                AppRunningDeviceEnum.AutelRemotePad10_9
            } else if (2340 == ScreenUtils.getScreenWidth(this)) {
                AppRunningDeviceEnum.AutelRemotePad6_4
            } else if (1440 == ScreenUtils.getScreenWidth(this)) {
                AppRunningDeviceEnum.AutelRemotePad6_0
            } else {
                AppRunningDeviceEnum.AutelRemotePad7_9
            }
            AppInfoManager.setAppRunningDevice(deviceEnum)
        } else {
            AutelLog.i(AppTagConst.AppInfo, " is Phone ")
            AppInfoManager.setAppRunningDevice(AppRunningDeviceEnum.Phone)
        }

    }

    /**
     * ModelX 变量配置
     */
    private fun initModelXConfig() {
        // 配置航线最大速度
        ModelXDroneConst.DRONE_FLIGHT_SPEED_LIMIT_MAX =
            if (AppInfoManager.isSupportMissionV15()) 15 else 10

        //航线任务是否支持悬停
        ModelXDroneConfig.isSupportMissionHover = AppInfoManager.isSupportMissionHover()

        //飞机最大返航高度
        ModelXDroneConst.DRONE_GO_HOME_HEIGHT_MAX =
            if (AppInfoManager.isSupportLimitHeight1500()) 1500 else 800

        //飞机最大飞行高度
        ModelXDroneConst.DRONE_LIMIT_HEIGHT_MAX =
            if (AppInfoManager.isSupportLimitHeight1500()) 1500 else 800
    }









    /** 初始化组件 */
    private fun initComponent() {
        AutelLog.init(
            AppInfoManager.isBuildTypeDebug(),
            AutelDirPathUtils.getAppLogDir(),
            AutelDirPathUtils.getLogCacheDir()
        )
        AutelStorage.initStorageManager(this)
    }

    /** 初始化SDK */
    private fun initSdk() {

        val sdkInitConfig = SDKInitConfig().apply {
            debug = AppInfoManager.isBuildTypeDebug()
            bRender = true
            renderMode = RenderMode.RenderWithSurface
            log = AppLog()
            storage = AppStorage()
            bAutoTimeZone = true
        }

        SDKManager.get().init(applicationContext, sdkInitConfig)

        SDKManager.get()
            .init(applicationContext, AppInfoManager.isBuildTypeDebug(), AppStorage(), AppLog())
        SDKManager.get().getDeviceManager().addDroneListener(this)
    }


    override fun onDroneCreate(drone: IAutelDroneDevice) {
        super.onDroneCreate(drone)
        AutelLog.i(
            AppTagConst.SDKData,
            "onDroneCreate, drone =${DeviceUtils.droneDeviceName(drone)}"
        )
    }

    override fun onDroneChangedListener(
        connected: Boolean,
        drone: IAutelDroneDevice,
    ) {
        AutelLog.i(
            AppTagConst.SDKData,
            "drone connected changed,connected = $connected, drone =${
                DeviceUtils.droneDeviceName(drone)
            }"
        )
    }

    override fun onMainServiceValid(valid: Boolean, drone: IAutelDroneDevice) {
        AutelLog.i(
            AppTagConst.SDKData,
            "on main service valid, valid = $valid, drone =${DeviceUtils.droneDeviceName(drone)}"
        )
    }

    override fun onCameraAbilityFetchListener(
        localFetched: Boolean,
        remoteFetched: Boolean,
        drone: IAutelDroneDevice,
    ) {
        AutelLog.i(
            AppTagConst.SDKData,
            "on camera ability fetched, the localFetched = $localFetched , remoteFetched= $remoteFetched, drone =${
                DeviceUtils.droneDeviceName(
                    drone
                )
            }"
        )
    }

    override fun onDroneDestroy(drone: IAutelDroneDevice) {
        super.onDroneDestroy(drone)
        AutelLog.i(
            AppTagConst.SDKData,
            "onDroneDestroy, drone =${DeviceUtils.droneDeviceName(drone)}"
        )
    }

    /**
     * 对频模式变化：启动或者释放中继模式
     */
    override fun onModemModeChange(mode: ModemModeEnum) {
        AutelLog.i(AppTagConst.SDKData, "onModemModeChange=$mode")
        when (mode) {
            ModemModeEnum.MESH_MODE -> { //组网
                DeviceManager.getDeviceManager().unInitRCHidden()
            }

            ModemModeEnum.P2P_MASTER_SLAVE_MODE -> { //点对点
                DeviceManager.getDeviceManager().initRCHidden()
            }

            else -> {

            }
        }
    }


    /** 切换语言需要重启APP，简单重启生命周期会异常 */
    private fun registerRestartReceiver() {
        val intentFilter = IntentFilter(RouterDataKey.RESTART_ACTION)
        LocalBroadcastManager.getInstance(this).registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (RouterDataKey.RESTART_ACTION == intent?.action) {
                    AppActivityManager.instance().clearAllActivity()
                    val intent = Intent(this@Application, UxsdkDemoActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    this@Application.startActivity(intent)
                    Process.killProcess(Process.myPid())
                }
            }
        }, intentFilter)
    }

    private fun restartApp() {
        AutelLog.i(AppTagConst.AppInfo, "restartApp")
        Handler().postDelayed({
            val startIntent = Intent(Intent(RouterDataKey.RESTART_ACTION))
            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(startIntent)
            // 在这里进行基于 context 的操作
        }, 2000)
    }


    private val iDeviceListChangedListener = object : IMeshDeviceChangedListener {
        override fun onRemoterRoleChange(isMain: Boolean) {
            super.onRemoterRoleChange(isMain)
            AutelLog.i(AppTagConst.AppInfo, "遥控器主从角色变化 newValue: $isMain")
            restartApp()
        }
    }
}