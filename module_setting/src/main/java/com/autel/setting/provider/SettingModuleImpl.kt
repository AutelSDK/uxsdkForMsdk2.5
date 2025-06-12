package com.autel.setting.provider

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.annotation.Keep
import com.alibaba.android.arouter.facade.annotation.Route
import com.autel.common.constant.StringConstants.Companion.ARGS_DRONE_DEVICE_ID
import com.autel.common.delegate.IMainProvider
import com.autel.common.delegate.activity.AbsDelegateActivity
import com.autel.common.delegate.function.AbsDelegateFunction
import com.autel.common.feature.route.RouteManager
import com.autel.common.feature.route.RouterConst
import com.autel.common.manager.AppInfoManager
import com.autel.common.manager.MiddlewareManager
import com.autel.common.manager.module.ISettingModule
import com.autel.common.utils.BusinessType
import com.autel.common.utils.DeviceUtils
import com.autel.common.widget.BasePopWindow
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.payload.bean.tracer.main
import com.autel.log.AutelLog
import com.autel.setting.provider.delegate.AiTargetDelegateActivity
import com.autel.setting.provider.delegate.GimbalAdjustDelegateActivity
import com.autel.setting.provider.delegate.RemoteDelegateActivity
import com.autel.setting.provider.delegate.RemoteDroneLocationDelegateActivity
import com.autel.setting.provider.function.CompassFunctionEntry
import com.autel.setting.provider.function.DataSecurityFunctionEntry
import com.autel.setting.provider.function.DownFillLightFunctionEntry
import com.autel.setting.provider.function.GNSSFunctionEntry
import com.autel.setting.provider.function.GimbalFillLightFunctionEntry
import com.autel.setting.provider.function.NavigationLightFunctionEntry
import com.autel.setting.provider.function.ObstacleAvoidanceFunctionEntry
import com.autel.setting.provider.function.RangingFunctionEntry
import com.autel.setting.provider.function.SettingFunctionEntry
import com.autel.setting.provider.function.SilenceModeFunctionEntry
import com.autel.setting.provider.function.SingleMatchFunctionEntry
import com.autel.setting.utils.matchUtils.MatchUtils
import com.autel.setting.view.FileSelectorActivity
import com.autel.setting.view.SettingCompassCalibrationActivity
import com.autel.setting.view.SettingControllerCalibrationActivity
import com.autel.setting.view.SettingIMUCalibrationActivity
import com.autel.setting.view.SettingLookupDronePopupWindow
import com.autel.setting.view.SettingRemoteCompassCalibrationActivity
import com.autel.setting.view.rid.RemoteIDActivity

/**
 * @author 
 * @date 2023/4/14
 * 设置模块实现类
 */
@Keep
@Route(path = RouterConst.ModuleService.MODULE_SERVICE_SETTING)
class SettingModuleImpl : ISettingModule {

    override fun jumpIMUCalibration(context: Context, droneDevice: IAutelDroneDevice) {
        AutelLog.i(getModuleName(), "jump to imu calibration device = $droneDevice context = $context")
        val intent = Intent(context, SettingIMUCalibrationActivity::class.java)
        intent.putExtra(ARGS_DRONE_DEVICE_ID, DeviceUtils.droneDeviceId(droneDevice))
        context.startActivity(intent)
    }

    override fun jumpRemoteCalibration(context: Context, droneDevice: IAutelDroneDevice?) {
        AutelLog.i(getModuleName(), "jump to remote calibration device = $droneDevice context = $context")
        val intent = Intent(context, SettingControllerCalibrationActivity::class.java)
        intent.putExtra(ARGS_DRONE_DEVICE_ID, if (droneDevice == null) -1 else DeviceUtils.droneDeviceId(droneDevice))
        context.startActivity(intent)
    }

    override fun jumpRemoteCompassCalibration(context: Context) {
        val intent = Intent(context, SettingRemoteCompassCalibrationActivity::class.java)
        context.startActivity(intent)
    }

    override fun jumpCompassCalibration(context: Context, droneDevice: IAutelDroneDevice) {
        AutelLog.i(getModuleName(), "jump to compass calibration device = $droneDevice context = $context")
        val intent = Intent(context, SettingCompassCalibrationActivity::class.java)
        intent.putExtra(ARGS_DRONE_DEVICE_ID, DeviceUtils.droneDeviceId(droneDevice))
        context.startActivity(intent)
    }

    override fun startRemoteControlMatch(context: Context) {
        val isP2PMatchCp = DeviceUtils.isP2PMatchCp()
        val isSingle = DeviceUtils.isSingle()
        AutelLog.i(getModuleName(), "startRemoteControlMatch isSingle=$isSingle isP2PMatchCp = $isP2PMatchCp " +
                "isSupportMultiPair=${AppInfoManager.isSupportMultiPair()}")
        if (!isSingle && AppInfoManager.isSupportMultiPair() && !isP2PMatchCp){
            //组网下，如果是组网Cp，进入组网入口
//            MiddlewareManager.netmeshModule.enterNetMeshEntrance(context)
        }else{
            MatchUtils.startRcMatch(context) {
                AutelLog.i(getModuleName(), "startRemoteControlMatch result=$it")
            }
        }
    }

    override fun jumpRemoteIdSetting(context: Context) {
        context.startActivity(Intent(context, RemoteIDActivity::class.java))
    }

    override fun getLookUpDroneWindow(context: Context, droneDevice: IAutelDroneDevice): BasePopWindow {
        return SettingLookupDronePopupWindow(context, droneDevice)
    }

    override fun getDelegateActivityList(mainProvider: IMainProvider): List<AbsDelegateActivity> {
        return listOf(
            AiTargetDelegateActivity(mainProvider),
            GimbalAdjustDelegateActivity(mainProvider),
            RemoteDelegateActivity(mainProvider),
            RemoteDroneLocationDelegateActivity(mainProvider),
        )
    }

    override fun getFunctionEntryList(mainProvider: IMainProvider): List<AbsDelegateFunction> {
        val list = mutableListOf<AbsDelegateFunction>()
        list.add(SettingFunctionEntry(mainProvider))
        if (DeviceUtils.isBusinessTypeValid(BusinessType.NETMESH)) {
            list.add(SingleMatchFunctionEntry(mainProvider))
        }
        if (DeviceUtils.isMainRC()) {
            if (AppInfoManager.isSupportNavigationLight()) {
                val bottomLightEntry = DownFillLightFunctionEntry(mainProvider)
                list.add(bottomLightEntry)
            }
            //隐蔽模式
            if (AppInfoManager.isSupportStealthMode()) {
                list.add(SilenceModeFunctionEntry(mainProvider))
            }

            if (AppInfoManager.isSupportNavigationLight()) {
                val navigationLightFunctionEntry = NavigationLightFunctionEntry(mainProvider)
                list.add(navigationLightFunctionEntry)
            }
            if (AppInfoManager.isSupportAvoidanceWay()) {
                list.add(ObstacleAvoidanceFunctionEntry(mainProvider))
            }
            list.add(DataSecurityFunctionEntry(mainProvider))
            if (AppInfoManager.isSupportGNSSShortCut()) {
                list.add(GNSSFunctionEntry(mainProvider))
            }
            if (AppInfoManager.isSupportNorthCompass()) {
                list.add(CompassFunctionEntry(mainProvider))
            }
            if (AppInfoManager.isSupportLaserDistance()) {
                list.add(RangingFunctionEntry(mainProvider))
            }
            list.add(GimbalFillLightFunctionEntry(mainProvider))
        }

        return list
    }

    override fun startFileSelectorActivity(
        context: Activity,
        titleRes: Int,
        path: String,
        fileSuffix: String?,
        isMultipleChoice: Boolean,
        result: (List<String>?) -> Unit,
    ) {
        RouteManager.routeToForResult(RouterConst.ModuleService.ACTIVITY_URL_FILE_SELECTOR, context, {
            withString(FileSelectorActivity.KEY_FILE_TYPE, fileSuffix)
            withBoolean(FileSelectorActivity.KEY_MULTIPLE_CHOICE, isMultipleChoice)
            withInt(FileSelectorActivity.KEY_TITLE_RES, titleRes)
            withString(FileSelectorActivity.KEY_BASE_PATH, path)
        }, onSuccess = {
            val paths = it.getStringArrayListExtra("path")
            result.invoke(paths)
            AutelLog.d("SettingProviderImpl", "paths = ${paths?.joinToString { it }}")
        })
    }

    override fun startFileSelectorActivityWithMultipleSuffixes(
        context: Activity,
        titleRes: Int,
        path: String,
        fileSuffixes: List<String>,
        isMultipleChoice: Boolean,
        newImportUI: Boolean,
        result: (List<String>?) -> Unit,
    ) {
        RouteManager.routeToForResult(RouterConst.ModuleService.ACTIVITY_URL_FILE_SELECTOR, context, {
            withStringArrayList(FileSelectorActivity.KEY_FILE_TYPES, ArrayList(fileSuffixes))
            withBoolean(FileSelectorActivity.KEY_MULTIPLE_CHOICE, isMultipleChoice)
            withInt(FileSelectorActivity.KEY_TITLE_RES, titleRes)
            withString(FileSelectorActivity.KEY_BASE_PATH, path)
            withBoolean(FileSelectorActivity.KEY_NEW_IMPORT_UI, newImportUI)
        }, onSuccess = {
            val paths = it.getStringArrayListExtra("path")
            result.invoke(paths)
            AutelLog.d("SettingProviderImpl", "paths = ${paths?.joinToString { it }}")
        })
    }

    override fun getModuleName(): String {
        return "SettingModule"
    }

    override fun release() {
        AutelLog.i(getModuleName(), "module is release")
    }

    override fun init(context: Context?) {
        AutelLog.i(getModuleName(), "module is init")
    }

}