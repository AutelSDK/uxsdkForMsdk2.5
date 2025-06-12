package com.autel.setting.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.autel.common.DroneConst
import com.autel.common.feature.location.CountryManager
import com.autel.common.feature.phone.AutelPhoneLocationManager
import com.autel.common.manager.AppInfoManager
import com.autel.common.manager.LawHeightDelegateManager
import com.autel.common.sdk.ModelXDroneConst
import com.autel.common.utils.DeviceUtils
import com.autel.common.utils.TransformUtils
import com.autel.common.utils.toLength
import com.autel.common.utils.toLengthMetricValue
import com.autel.common.widget.dialog.BaseDialogFragment
import com.autel.common.widget.dialog.CommonTwoButtonDialog
import com.autel.drone.sdk.libbase.error.IAutelCode
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.manager.keyvalue.callback.CommonCallbacks
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.CommonKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.FlightControlKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.ext.create
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.bean.DroneSystemStateHFNtfyBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.enums.DroneFlightModeEnum
import com.autel.log.AutelLog
import com.autel.map.bean.AutelLatLng
import com.autel.map.util.MapBoxUtils
import com.autel.setting.R
import com.autel.setting.databinding.SettingDialogSingleTakeOffBinding


class OneClickTakeoffDialog(private val mode: DroneFlightModeEnum) : BaseDialogFragment() {

    private val TAG = "OneClickTakeoffDialog"

    private var binding: SettingDialogSingleTakeOffBinding? = null

    private val controlDrones = mutableListOf<IAutelDroneDevice>()

    //M为1.8m，H为1.5m，X、SL、V3均为1.2m  日期:2025-01-11 , 由黄小凤确认
    private var takeOffHeight: Float = when {
        AppInfoManager.isModelM() -> 1.8f
        AppInfoManager.isModelH() -> 1.5f
        AppInfoManager.isModelS() -> 1.2f
        else -> 1.2f
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = SettingDialogSingleTakeOffBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        controlDrones.clear()
        controlDrones += DeviceUtils.getMultiTakeoffDrone()
        binding?.root?.setOnClickListener { dismissAllowingStateLoss() }
        binding?.contentLayout?.setOnClickListener {
            // nothing
        }

        if (!isSingleControl()) {
            binding?.tips?.text = getString(
                R.string.common_text_multiple_take_off_tips,
                "${TransformUtils.getLengthWithoutUnit(DroneConst.DRONE_TO_DRONE_DISTANCE.toDouble()).toInt()}", TransformUtils.getLengthUnit(),
                "${TransformUtils.getLengthWithoutUnit(DroneConst.REMOTE_TO_DRONE_DISTANCE.toDouble()).toInt()}", TransformUtils.getLengthUnit(),
                "${TransformUtils.getLengthRoundToOneDecimalWithoutUnit(takeOffHeight.toDouble())}", TransformUtils.getLengthUnit(),
            )
        }

        binding?.takeoff?.setOnTriggerListener {
            //TOD 发送起飞指令,
            if (isSingleControl()) {
                // 单机起飞
                singleControlTakeOff()
            } else {
                //多机起飞
                multiControlTakeOff()
            }
            dismissAllowingStateLoss()
        }

        AutelLog.i(TAG, "controlDrones size = ${controlDrones.size} , isSingleControl :${isSingleControl()} , model :$mode")
        listenerDroneFlyMode()

        binding?.takeoffHeight?.run {
            isVisible = isSingleControl()
            val min = takeOffHeight.toLength()
            val maxHeight =
                DeviceUtils.singleControlDrone()?.getDeviceStateData()?.flightoperateData?.fMaxHeight ?: LawHeightDelegateManager.getLowHeight(
                    CountryManager.currentCountry
                )
            // 是否不限高
            val notLimitHeight = maxHeight >= ModelXDroneConst.DRONE_LIMIT_HEIGHT_VALUE_MAX
            val max = if (notLimitHeight) 100000f else maxHeight.toLength()
            val title = if (notLimitHeight) {
                getString(
                    R.string.common_text_hover_height,
                    min.toString(),
                    TransformUtils.getLengthUnit()
                )
            } else {
                getString(
                    R.string.common_text_move_take_off_height_title,
                    min.toString(),
                    max.toString(),
                    TransformUtils.getLengthUnit()
                )
            }
            setTitleName(title)
            setValueRange(min, max)
            setHeightData(min)
            updateInputTypeDecimal()
            setOnHeightFloatChangeListener {
                takeOffHeight = it.toLengthMetricValue()
            }
        }
    }

    private fun isSingleControl(): Boolean {
        return controlDrones.size <= 1
    }

    private fun listenerDroneFlyMode() {
        if (isSingleControl()) {
            AutelLog.i(TAG, "listenerDroneFlyMode singleControlDrone")
            DeviceUtils.singleControlDrone()?.getKeyManager()
                ?.listen(CommonKey.KeyDroneSystemStatusHFNtfy.create(), this, flightModeListener)
        } else {
            AutelLog.i(TAG, "listenerDroneFlyMode multiControlDrone")
            for (controlledDrone in controlDrones) {
                controlledDrone.getKeyManager().listen(CommonKey.KeyDroneSystemStatusHFNtfy.create(), this, flightModeListener)
            }
        }
    }

    private fun removeAllListener() {
        if (isSingleControl()) {
            DeviceUtils.singleControlDrone()?.getKeyManager()?.cancelListen(this)
        } else {
            for (controlledDrone in controlDrones) {
                controlledDrone.getKeyManager().cancelListen(this)
            }
        }
    }

    private val flightModeListener = object : CommonCallbacks.KeyListener<DroneSystemStateHFNtfyBean> {
        override fun onValueChange(oldValue: DroneSystemStateHFNtfyBean?, newValue: DroneSystemStateHFNtfyBean) {
            // 当弹出一键起飞后, 手动内外八打杆,需要关闭一键起飞弹框,
            // 手动起飞后,同样需要关闭一键起飞弹框
            if (mode == DroneFlightModeEnum.LANDED) {
                if (newValue.flightMode > DroneFlightModeEnum.LANDED) {
                    AutelLog.i(TAG, "landed --> drone is fly $newValue")
                    //起飞成功
                    dismissAllowingStateLoss()
                }
            } else {
                if (newValue.flightMode > DroneFlightModeEnum.DISARM) {
                    //起飞成功
                    AutelLog.i(TAG, "disarm --> drone is fly $newValue")
                    dismissAllowingStateLoss()
                }
            }
        }

    }

    /**
     * 单机起飞
     */
    private fun singleControlTakeOff() {
        val key = FlightControlKey.KeyTakeOffAirCraftWithAttitude.create()
        AutelLog.i(TAG, " singleControlTakeOff() takeOffHeight : $takeOffHeight")
        DeviceUtils.singleControlDrone()?.getKeyManager()
            ?.performAction(key, param = (takeOffHeight * 1000).toInt(), callback = object : CommonCallbacks.CompletionCallbackWithParam<Void> {
                override fun onSuccess(t: Void?) {
                    AutelLog.i(TAG, "take off success")
                }

                override fun onFailure(error: IAutelCode, msg: String?) {
                    AutelLog.i(TAG, "take off fail :$error")
                }
            })
    }

    /**
     * 多机起飞
     * 1: 判断遥控器与飞机记录不足10米,则弹框提示;
     * 2: 判断飞机间距小于等于5米,弹框提示;
     */
    private fun multiControlTakeOff() {
        val remoteCoordinate = AutelPhoneLocationManager.locationLiveData.value ?: AutelLatLng()
        val devices = DeviceUtils.allControlDrones().filter { it.isConnected() }
        if (!remoteCoordinate.isInvalid()) {
            AutelLog.i(TAG, "remoteCoordinate : $remoteCoordinate")
            devices.forEach {
                val coordinate =
                    AutelLatLng(it.getDeviceStateData().flightControlData.droneLatitude, it.getDeviceStateData().flightControlData.droneLongitude)
                val distance = MapBoxUtils.getDistance(remoteCoordinate, coordinate)
                AutelLog.i(TAG, "remote to drone distance : $distance , min distance : ${DroneConst.REMOTE_TO_DRONE_DISTANCE}")
                if (distance < DroneConst.REMOTE_TO_DRONE_DISTANCE) {
                    // 提示遥控器与飞机间距不足10米
                    showDistanceWarnTips(
                        getString(
                            R.string.common_text_remote_to_drone_tips,
                            "${TransformUtils.getLengthWithoutUnit(DroneConst.REMOTE_TO_DRONE_DISTANCE.toDouble()).toInt()}",
                            TransformUtils.getLengthUnit()
                        )
                    )
                    return
                }
            }
        }
        multiTakeOff()
    }

    private fun showDistanceWarnTips(tip: String) {
        // 提示遥控器与飞机间距不足10米
        dismissAllowingStateLoss()
        val dialog = CommonTwoButtonDialog(activity ?: return)
        dialog.setMessage(tip)
        dialog.setRightBtnStr(getString(R.string.common_text_continue_takeoff))
        dialog.setLeftBtnStr(getString(R.string.common_text_cancel))
        dialog.setRightBtnListener {
            multiTakeOff()
        }
        dialog.show()
    }

    /**
     * 多机起飞接口
     */
    private fun multiTakeOff() {
        val key = FlightControlKey.KeyDroneSetMultiAutoTakeOff.create()
        val devices = DeviceUtils.allControlDrones().filter { it.isConnected() }
        AutelLog.i(TAG, "start multi take off...")
        devices.forEach {
            // 这里起飞直接传0
            it.getKeyManager().performAction(key, 0, callback = object : CommonCallbacks.CompletionCallbackWithParam<Void> {
                override fun onSuccess(t: Void?) {
                    AutelLog.i(TAG, "take off success : ${it.getDroneSn()}")
                }

                override fun onFailure(error: IAutelCode, msg: String?) {
                    AutelLog.i(TAG, "take off fail :$error ,msg :$msg : ${it.getDroneSn()}")
                }
            })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        removeAllListener()
    }

}