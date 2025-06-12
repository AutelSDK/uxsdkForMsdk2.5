package com.autel.setting.provider.function

import android.view.View
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import com.autel.common.delegate.IMainProvider
import com.autel.common.delegate.function.AbsDelegateFunction
import com.autel.common.delegate.function.FunctionBarState
import com.autel.common.delegate.function.FunctionType
import com.autel.common.delegate.function.FunctionViewType
import com.autel.common.lifecycle.LiveDataBus
import com.autel.common.lifecycle.event.RangingSwitchEvent
import com.autel.common.manager.AutelStorageManager
import com.autel.common.manager.MiddlewareManager
import com.autel.common.manager.StorageKey.PlainKey.KEY_NORTH_OPEN
import com.autel.common.utils.DeviceUtils
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.setting.R
import com.autel.setting.business.CompassViewModel

class CompassFunctionEntry(mainProvider: IMainProvider) : AbsDelegateFunction(mainProvider) {

    private var compassViewModel: CompassViewModel =
        ViewModelProvider(mainProvider.getMainContext() as ComponentActivity)[CompassViewModel::class.java]

    override fun getFunctionType(): FunctionType {
        return FunctionType.Compass
    }

    override fun getFunctionName(): String {
        return mainProvider.getMainContext().getString(R.string.common_text_compass)
    }

    override fun getFunctionIconRes(): Int {
        return R.drawable.common_selector_compass_selector
    }

    override fun onFunctionCreate() {
        super.onFunctionCreate()

        MiddlewareManager.codecModule.observerFullScreen(mainProvider.getMainLifecycleOwner()) {
            compassViewModel.changeScaleShootingPosition(it, mainProvider.getMainHandler().getScreenState())
        }
        //当前watch展示的设备，决定开关状态
        mainProvider.getMainHandler().observerScreenState {
            refreshCompassFunctionStatus()
            val drone = getScreenStateWatchDrone() ?: return@observerScreenState
            val compassStatus = compassViewModel.queryCompassStatus(drone)
            if (compassStatus) {
                compassViewModel.openCompass(mainProvider, drone)
            }
        }
    }

    override fun onFunctionStart(viewType: FunctionViewType, view: View) {
        super.onFunctionStart(viewType, view)
        val drone = getScreenStateWatchDrone() ?: return
        functionModel.isOn = true
        mainProvider.getMainHandler().updateFunctionState(functionModel)
        compassViewModel.openCompass(mainProvider, drone)
        AutelStorageManager.getPlainStorage().setBooleanValue(KEY_NORTH_OPEN, true)
        LiveDataBus.of(RangingSwitchEvent::class.java).switch().post(true)
        mainProvider.getMainHandler().updateFunctionBarLD(FunctionBarState.Folded)
    }

    override fun onFunctionStop(viewType: FunctionViewType, view: View) {
        super.onFunctionStop(viewType, view)
        val drone = getScreenStateWatchDrone() ?: return
        functionModel.isOn = false
        mainProvider.getMainHandler().updateFunctionState(functionModel)
        compassViewModel.closeCompass(mainProvider, drone)
        AutelStorageManager.getPlainStorage().setBooleanValue(KEY_NORTH_OPEN, false)
        LiveDataBus.of(RangingSwitchEvent::class.java).switch().post(false)
    }

    override fun onDroneChangedListener(connected: Boolean, drone: IAutelDroneDevice) {
        super.onDroneChangedListener(connected, drone)
        refreshCompassFunctionStatus()
    }

    private fun getScreenStateWatchDrone(): IAutelDroneDevice? {
        val state = mainProvider.getMainHandler().getScreenState()
        val drone = state.watchDroneNumber?.let {
            DeviceUtils.getDrone(it)
        }
        return drone
    }

    private fun refreshCompassFunctionStatus() {
        val drone = getScreenStateWatchDrone()
        if (drone == null) {
            functionModel.isOn = false
            functionModel.isEnabled = false
            mainProvider.getMainHandler().updateFunctionState(functionModel)
        } else {
            val compassStatus = compassViewModel.queryCompassStatus(drone)
            functionModel.isOn = compassStatus
            functionModel.isEnabled = drone.isConnected()
            mainProvider.getMainHandler().updateFunctionState(functionModel)
        }
    }
}