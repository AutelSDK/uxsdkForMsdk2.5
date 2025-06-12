package com.autel.setting.provider.delegate

import android.content.Context
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import com.autel.common.base.AppActivityManager
import com.autel.common.bean.CustomRemoteKeyEnum
import com.autel.common.constant.AppTagConst
import com.autel.common.delegate.IMainProvider
import com.autel.common.delegate.activity.AbsDelegateActivity
import com.autel.common.feature.location.CountryManager
import com.autel.common.lifecycle.LiveDataBus
import com.autel.common.listener.CommonDialogListener
import com.autel.common.manager.CommonDialogManager
import com.autel.common.manager.MiddlewareManager
import com.autel.common.model.splitscreen.AircraftScreenItem
import com.autel.common.model.splitscreen.toLensTypeEnum
import com.autel.common.sdk.service.SettingService
import com.autel.common.utils.BusKey
import com.autel.common.utils.DeviceUtils
import com.autel.common.widget.GearLevelSwitchDialog
import com.autel.common.widget.toast.AutelToast
import com.autel.drone.sdk.libbase.error.IAutelCode
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.interfaces.IKeyManager
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.drone.sdk.vmodelx.manager.keyvalue.callback.CommonCallbacks
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.CameraKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.RemoteControllerKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.base.KeyTools
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.enums.DroneFlightModeEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.enums.DroneWorkModeEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.enums.DroneWorkStateEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.enums.GearLevelEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.enums.RemoteIdStatusEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.gimbal.enums.GimbalOrientationEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.remotecontrol.bean.HardwareButtonInfoBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.remotecontrol.enums.RCButtonTypeEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.remotecontrol.enums.RcButtonStateEnum
import com.autel.log.AutelLog
import com.autel.setting.R
import com.autel.setting.dialog.OneClickTakeoffDialog
import com.autel.common.utils.CustomKeyUtils


/**
 * Created by  2023/6/8
 *  遥控器代理Activity
 */
class RemoteDelegateActivity(mainProvider: IMainProvider) : AbsDelegateActivity(mainProvider) {
    private var isGearLevelDialogShowing = false
    private var destroyed = false
    private var rcHardWareInfoKey = KeyTools.createKey(RemoteControllerKey.KeyRCHardwareInfo)
    private var rcHardWareCallback = object : CommonCallbacks.KeyListener<HardwareButtonInfoBean> {
        override fun onValueChange(oldValue: HardwareButtonInfoBean?, newValue: HardwareButtonInfoBean) {
            if (!DeviceUtils.isMainRC()) {
                return
            }
            hardwareInfoBean(newValue)
        }
    }

    private var singleTakeoffDialog: OneClickTakeoffDialog? = null

    private fun hardwareInfoBean(infoBean: HardwareButtonInfoBean) {
        //波轮
        AutelLog.i(AppTagConst.RemoteControl, "receive remote upload, infoBean = $infoBean")
        if (infoBean.clickType == RcButtonStateEnum.ROLL) {
            val state = MiddlewareManager.codecModule.getPageScreenState()

            val splitWidgetList = state.getAllSplitWidgetList()
            val map = mutableMapOf<IAutelDroneDevice, AircraftScreenItem>()
            splitWidgetList.forEach {
                val drone = it.drone
                if (drone != null) {
                    val result = map.get(drone)
                    if (result == null) {
                        map.put(drone, it)
                    }
                }
            }
            map.values.forEach {
                val drone = it.drone
                val lensType = it.widgetType.toLensTypeEnum()
                if (drone != null && lensType != null) {
                    if (drone.getDeviceStateData().flightoperateData.bAircraftActivation == true) {
                        val cameraId = drone.getCameraAbilitySetManger().getLenId(lensType, drone.getGimbalDeviceType()) ?: 0
                        when (infoBean.buttonType) {
                            RCButtonTypeEnum.ZOOM_IN -> {
                                setSmoothZoom(drone.getKeyManager(), cameraId, true, infoBean.thumbWheelValue)
                            }

                            RCButtonTypeEnum.ZOOM_OUT -> {
                                setSmoothZoom(drone.getKeyManager(), cameraId, false, infoBean.thumbWheelValue)
                            }

                            else -> {}
                        }
                    } else {
                        AutelLog.i(AppTagConst.RemoteControl, "aircraft un active")
                    }
                }
            }
        } else {
            AutelLog.i(AppTagConst.RemoteControl, "clickType:${infoBean}")
        }
        //自定义按键
        when (infoBean.buttonType) {
            RCButtonTypeEnum.LEFT_CUSTOM -> {
                val key = CustomKeyUtils.getDefineCustomC1()
                performCustomAction(key)
            }

            RCButtonTypeEnum.RIGHT_CUSTOM -> {
                val key = CustomKeyUtils.getDefineCustomC2()
                performCustomAction(key)
            }

            RCButtonTypeEnum.GO_HOME -> {// 一键起飞按钮
                showTakeOffDialog()
            }

            else -> {

            }
        }
    }

    private fun showTakeOffDialog() {
        if (!DeviceUtils.isMainRC()) return
        if (!DeviceManager.getDeviceManager().isConnected()) return
        AppActivityManager.INSTANCE.topActiveActivity()?.let {
            if (DeviceUtils.isSingleControl()) { // 单机
                showSingleTakeOffDialog()
            } else { // 多机
                showMultiTakeOffDialog()
            }
        }
    }

    /**
     * 单机一键起飞弹框
     */
    private fun showSingleTakeOffDialog() {
        if (singleTakeoffDialog?.isAdded == true) return
        val drone = DeviceUtils.singleControlDrone() ?: return
        val flightMode = drone.getDeviceStateData().flightControlData.flightMode
        if (flightMode <= DroneFlightModeEnum.LANDED) {
            AppActivityManager.INSTANCE.topActiveActivity()?.let {
                if (it is FragmentActivity) {
                    OneClickTakeoffDialog(flightMode).also {
                        singleTakeoffDialog = it
                    }.show(it.supportFragmentManager, "OneClickTakeoffDialog")
                }
            }
        }
    }

    /**
     * 多机一键起飞框
     */
    private fun showMultiTakeOffDialog() {
        if (singleTakeoffDialog?.isAdded == true) return
        val controlledDrones = DeviceUtils.getMultiTakeoffDrone()
        if (controlledDrones.isEmpty()) {
            return
        }
        if (controlledDrones.any { it.getDeviceStateData().flightControlData.flightMode > DroneFlightModeEnum.LANDED }) {
            // 当有一台飞机正在飞行时，不显示起飞弹框
            return
        }
        AppActivityManager.INSTANCE.topActiveActivity()?.let {
            if (it is FragmentActivity) {
                val mode = if (controlledDrones.any { it.getDeviceStateData().flightControlData.flightMode == DroneFlightModeEnum.LANDED }) {
                    DroneFlightModeEnum.LANDED
                } else {
                    DroneFlightModeEnum.DISARM
                }
                AutelLog.i("OneClickTakeoff", "showMultiTakeOffDialog mode:$mode")
                OneClickTakeoffDialog(mode).also {
                    singleTakeoffDialog = it
                }.show(it.supportFragmentManager, "OneClickTakeoffDialog")
            }
        }
    }

    /**
     * 多机情况下,只要有一台禁止起飞,就不提示
     */
    private fun isCanTakeoff(drone: IAutelDroneDevice): Boolean {
        // 目前仅处理美国RID异常时,不显示起飞,其他暂不考虑
        val isRidError =
            if (CountryManager.isUsZone()) drone.getDeviceStateData().flightControlData.remoteIdStatus == RemoteIdStatusEnum.AUTO_CHECK_ERROR else false
        AutelLog.i(
            "OnClickTakeoff",
            "isCanTakeoff isRidError:$isRidError , country:${CountryManager.currentCountry} , remoteIdStatus:${drone.getDeviceStateData().flightControlData.remoteIdStatus}"
        )
        if (drone.getDeviceStateData().flightControlData.flightMode.isFlying()) {
            return false
        }
        return !isRidError
    }

    private fun performCustomAction(key: CustomRemoteKeyEnum) {
        val controlledDrones = DeviceUtils.allControlDrones().filter {
            it.getDeviceStateData().flightoperateData.bAircraftActivation == true && it.isConnected()
        }
        //todo：目前全选下没有长机，等张瑞平改好再替换为获取长机
        val firstDroneDevice = controlledDrones.firstOrNull()
        AutelLog.i(AppTagConst.RemoteControl, "performCustomAction firstDroneDevice:$firstDroneDevice key:$key")
        //取第一个飞机的避障状态进行设置，后续飞机的避障状态设置与第一个飞机的避障状态保持一致
        val obstacleAvoidanceEnabled = firstDroneDevice?.getDeviceStateData()?.flightControlData?.obstacleAvoidanceEnabled == true
        //取第一个飞机的云台角度进行设置，后续飞机的云台角度设置与第一个飞机的云台角度保持一致
        val gimbalAngle = firstDroneDevice?.getDeviceStateData()?.flightControlData?.gimbalAttitudePitch ?: 0f
        var orientationEnum: GimbalOrientationEnum = GimbalOrientationEnum.DOWN_45
        orientationEnum = if (gimbalAngle == 0f) {
            GimbalOrientationEnum.DOWN_45
        } else if (gimbalAngle > -45f) {
            GimbalOrientationEnum.DOWN
        } else {
            GimbalOrientationEnum.FORWARD
        }
        when (key) {
            CustomRemoteKeyEnum.MAP_FPV_SWITCH -> {
                MiddlewareManager.codecModule.enlargeBottomLeftScreen()
            }

            else -> {}
        }
        //如果没有飞机在线且激活，不往下执行
        if (controlledDrones.isEmpty()) {
            AutelLog.i(AppTagConst.RemoteControl, "飞机未在线且激活，不能使用拍照录像键、C1 C2 拨轮")
            return
        }
        AutelLog.i(AppTagConst.RemoteControl, "Current thread: ${Thread.currentThread()}") // 打印当前线程对象
        when (key) {
            CustomRemoteKeyEnum.VISUAL_OBSTACLE_AVOIDANCE -> {
                AutelLog.i(AppTagConst.RemoteControl, "VISUAL_OBSTACLE_AVOIDANCE $destroyed")
                if (destroyed) return
                var isObstacleAvoidanceOpened = obstacleAvoidanceEnabled
                if (isObstacleAvoidanceOpened) {
                    CommonDialogManager.showOABehaviorDialog(delegateProvider.getMainContext(), object : CommonDialogListener {
                        override fun onDismiss() {}

                        override fun onLeftBtnClick() {}

                        override fun onRightBtnClick() {
                            isObstacleAvoidanceOpened = !isObstacleAvoidanceOpened
                            switchObstacleAvoidance(controlledDrones, isObstacleAvoidanceOpened)
                        }

                    })
                } else {
                    isObstacleAvoidanceOpened = !isObstacleAvoidanceOpened
                    switchObstacleAvoidance(controlledDrones, isObstacleAvoidanceOpened)
                }
            }

            CustomRemoteKeyEnum.GIMBAL_ANGLE -> {
                switchGimbalAngel(controlledDrones, orientationEnum)
            }

            CustomRemoteKeyEnum.GEAR_MODE_SWITCH -> {
                switchGearLevel(controlledDrones, delegateProvider.getMainContext())
            }

            else -> {

            }
        }

    }

    private fun setSmoothZoom(keyManager: IKeyManager, cameraId: Int, zoomIn: Boolean, thumbValue: Int) {
        val key = KeyTools.createKey(CameraKey.KeySmoothZoom, cameraId)
        val wheel = if (zoomIn) {
            thumbValue / 25.5 * 100
        } else {
            -thumbValue / 25.5 * 100
        }.toInt()

        keyManager.setValue(key, wheel, object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                AutelLog.i(AppTagConst.RemoteControl, "set smooth zoom success, the wheel = $wheel")
            }

            override fun onFailure(code: IAutelCode, msg: String?) {
                AutelLog.e(AppTagConst.RemoteControl, "set smooth zoom failed, the wheel = $wheel, code = $code")
            }
        })
    }


    private val hardwareButtonEventObserver = Observer<HardwareButtonInfoBean> {
        AutelLog.i(AppTagConst.RemoteControl, "oneKey hardwareInfoBean:$it")
        hardwareInfoBean(it)
    }

    override fun onCreate() {
        super.onCreate()
        AutelLog.i(AppTagConst.RemoteControl, "onCreate")
        destroyed = false
        DeviceManager.getDeviceManager().getLocalRemoteDevice().getKeyManager().listen(rcHardWareInfoKey, callback = rcHardWareCallback)
        
        // 添加LiveDataBus监听
        LiveDataBus.of<HardwareButtonInfoBean>(BusKey.KEY_HARDWARE_BUTTON_EVENT).observeForever(hardwareButtonEventObserver)
        AutelLog.i(AppTagConst.RemoteControl, "oneKey addObserveForever")
    }

    override fun onDestroy() {
        super.onDestroy()
        AutelLog.i(AppTagConst.RemoteControl, "onDestroy")
        destroyed = true
        DeviceManager.getDeviceManager().getLocalRemoteDevice().getKeyManager().cancelListen(rcHardWareInfoKey, callback = rcHardWareCallback)
        LiveDataBus.of<HardwareButtonInfoBean>(BusKey.KEY_HARDWARE_BUTTON_EVENT).removeObserveForever(hardwareButtonEventObserver)
        AutelLog.i(AppTagConst.RemoteControl, "oneKey removeObserveForever")
    }

    /**
     * 切换避障开关
     * */
    private fun switchObstacleAvoidance(controlledDrones: List<IAutelDroneDevice>, openObstacleAvoidance: Boolean) {
        val isTips = false//只提示一次
        controlledDrones.forEach {
            setObstacleAvoidance(it, openObstacleAvoidance, {
                if (!isTips) {
                    val id = if (openObstacleAvoidance) R.string.common_text_oa_behavior_stopped else R.string.common_text_oa_behavior_closed
                    AutelToast.normalToast(delegateProvider.getMainContext(), id)
                }
            }, {
                if (!isTips) {
                    AutelToast.normalToast(delegateProvider.getMainContext(), R.string.common_text_set_failed)
                }
            })
        }
    }

    /**
     * 切换云台角度
     * */
    private fun switchGimbalAngel(controlledDrones: List<IAutelDroneDevice>, orientationEnum: GimbalOrientationEnum) {
        controlledDrones.forEach {
            val workMode = it.getDeviceStateData().flightControlData.droneWorkMode
            val droneWorkStatus = it.getDeviceStateData().flightControlData.droneWorkStatus
            val isMissionRunning = DroneWorkModeEnum.isMissionMode(workMode) && droneWorkStatus == DroneWorkStateEnum.RUNNING
            AutelLog.i(
                AppTagConst.RemoteControl,
                "switchGimbalAngel orientationEnum:$orientationEnum workMode:$workMode droneWorkStatus:$droneWorkStatus isMissionRunning:$isMissionRunning"
            )
            if (!isMissionRunning) {
                switchGimbalOrientation(it, orientationEnum, {}, {})
            }
        }
    }

    private fun switchGearLevel(controlledDrones: List<IAutelDroneDevice>, context: Context) {
        val droneDevice = controlledDrones.firstOrNull()
        when (droneDevice?.getDeviceStateData()?.flightControlData?.droneGear) {
            GearLevelEnum.NORMAL -> {
                if (isGearLevelDialogShowing) return
                isGearLevelDialogShowing = true
                GearLevelSwitchDialog(context).apply {
                    setOnConfirmBtnClick {
                        controlledDrones.forEach {
                            switchGearLevel(it, GearLevelEnum.SPORT, {}, {})
                        }
                    }
                    setOnDismissListener {
                        isGearLevelDialogShowing = false
                    }
                    show()
                }
            }

            GearLevelEnum.SPORT -> {
                controlledDrones.forEach {
                    switchGearLevel(it, GearLevelEnum.LOW_SPEED, {}, {})
                }
            }

            GearLevelEnum.LOW_SPEED -> {
                controlledDrones.forEach {
                    switchGearLevel(it, GearLevelEnum.SMOOTH, {}, {})
                }
            }

            GearLevelEnum.SMOOTH -> {
                controlledDrones.forEach {
                    switchGearLevel(it, GearLevelEnum.NORMAL, {}, {})
                }
            }

            else -> {
                switchGearLevel(droneDevice, GearLevelEnum.SMOOTH, {}, {})
            }
        }
    }

    private fun switchGearLevel(droneDevice: IAutelDroneDevice?, gear: GearLevelEnum, onSuccess: () -> Unit, onError: (Throwable) -> Unit) {
        droneDevice?.let { SettingService.getInstance().flightParamService.setGearLevel(it, gear, onSuccess, onError) }
    }

    private fun switchGimbalOrientation(
        droneDevice: IAutelDroneDevice?,
        orientationEnum: GimbalOrientationEnum,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        droneDevice?.let { SettingService.getInstance().flightControlService.switchGimbalOrientation(it, orientationEnum, onSuccess, onError) }
    }

    private fun setObstacleAvoidance(
        droneDevice: IAutelDroneDevice?,
        openObstacleAvoidance: Boolean,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        droneDevice?.let { SettingService.getInstance().flightParamService.setObstacleAvoidance(it, openObstacleAvoidance, onSuccess, onError) }
    }

}