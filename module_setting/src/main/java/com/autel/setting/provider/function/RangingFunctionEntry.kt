package com.autel.setting.provider.function

import android.view.View
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.activity.ComponentActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.autel.common.delegate.IMainProvider
import com.autel.common.delegate.function.AbsDelegateFunction
import com.autel.common.delegate.function.FunctionBarState
import com.autel.common.delegate.function.FunctionType
import com.autel.common.delegate.function.FunctionViewType
import com.autel.common.lifecycle.LiveDataBus
import com.autel.common.lifecycle.event.RangingSwitchEvent
import com.autel.common.model.lens.CodecLayoutType
import com.autel.common.model.splitscreen.AircraftScreenState
import com.autel.common.model.splitscreen.toLensTypeEnum
import com.autel.common.utils.DeviceUtils
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.setting.R
import com.autel.setting.business.RangingViewModel
import com.autel.widget.widget.ranging.RangingWidget

/**
 * 激光测距工具
 */
class RangingFunctionEntry(mainProvider: IMainProvider) : AbsDelegateFunction(mainProvider) {

    private var rangingViewModel: RangingViewModel =
        ViewModelProvider(mainProvider.getMainContext() as ComponentActivity)[RangingViewModel::class.java]

    private var lastWatcherNumber: Int? = null

    override fun getFunctionType(): FunctionType {
        return FunctionType.LaserDistance
    }

    override fun getFunctionName(): String {
        return mainProvider.getMainContext().getString(R.string.common_text_laser_distance)
    }

    override fun getFunctionIconRes(): Int {
        return R.drawable.mission_selector_laser_ranging
    }

    override fun isShowEntry(): Boolean = DeviceUtils.isMainRC()

    override fun functionEnableDependsOnAircraft(): Boolean {
        return true
    }

    override fun onFunctionCreate() {
        super.onFunctionCreate()
        rangingViewModel.rangingFunctionState.observe(mainProvider.getMainLifecycleOwner()) {
            if (it != functionModel.isOn) {
                functionModel.isOn = it
                mainProvider.getMainHandler().updateFunctionState(functionModel)
                updateRangingView(mainProvider, mainProvider.getMainHandler().getScreenState())
            }
        }
        mainProvider.getMainHandler().observerScreenState(Observer {
            val droneNumber = mainProvider.getMainHandler().getScreenState().watchDroneNumber
            val drone = droneNumber?.let {
                DeviceUtils.getDrone(it) ?: DeviceUtils.allControlDrones().firstOrNull()
            }
            if (droneNumber != null) {
                rangingViewModel.setWatchDroneNumber(droneNumber)
            }
            if (lastWatcherNumber != drone?.deviceNumber()) {
                if (drone != null) {
                    lastWatcherNumber = drone.deviceNumber()
                    rangingViewModel.queryRangingSwitch(drone)
                }
            }
            updateRangingView(mainProvider, it)
        })

        mainProvider.getMainHandler().observeFunctionBarState(Observer {
            when (it) {
                FunctionBarState.Unfolded,
                FunctionBarState.Folded -> {
                    updateRangingView(mainProvider, mainProvider.getMainHandler().getScreenState())
                }

                FunctionBarState.Unfolding,
                FunctionBarState.Folding -> {

                }
            }
        })

        LiveDataBus.of(RangingSwitchEvent::class.java).switch().observe(mainProvider.getMainLifecycleOwner(), Observer {
            if (it) {
                functionStart()
                updateRangingView(mainProvider, mainProvider.getMainHandler().getScreenState())
            } else {
                functionStop()
                updateRangingView(mainProvider, mainProvider.getMainHandler().getScreenState())
            }
        })
    }

    private fun updateRangingView(mainProvider: IMainProvider, screenState: AircraftScreenState) {
        if (rangingViewModel.rangingFunctionState.value == true) {
            screenState.getAllSplitWidgetList().forEach {
                val layoutManager = it.screen?.getCodecProvider()?.getCodecScreenLayoutManager()
                layoutManager ?: return@forEach
                if (!layoutManager.hasViewInCodecStreamLayout(CodecLayoutType.RANGING)) {
                    layoutManager.addViewToCodecStreamLayout(
                        CodecLayoutType.RANGING,
                        RangingWidget(mainProvider.getMainContext()).apply {
                            updateLensInfo(it.drone, it.getGimbalTypeEnum(), it.widgetType.toLensTypeEnum())
                        },
                        FrameLayout.LayoutParams(
                            RelativeLayout.LayoutParams.MATCH_PARENT,
                            RelativeLayout.LayoutParams.MATCH_PARENT
                        )
                    )
                }
                layoutManager.getViewInCodecStreamLayout(CodecLayoutType.RANGING)?.isVisible = screenState.bottomLeftScreenType != it
            }
        } else {
            screenState.getAllSplitWidgetList().forEach {
                val layoutManager = it.screen?.getCodecProvider()?.getCodecScreenLayoutManager()
                layoutManager ?: return@forEach
                layoutManager.removeViewFromCodecStreamLayout(CodecLayoutType.RANGING, null)
            }
        }
    }


    override fun onDroneChangedListener(connected: Boolean, drone: IAutelDroneDevice) {
        super.onDroneChangedListener(connected, drone)
        if (connected && drone.deviceNumber() == lastWatcherNumber) {
            rangingViewModel.queryRangingSwitch(drone)
        }
    }

    override fun onFunctionStart(viewType: FunctionViewType, view: View) {
        super.onFunctionStart(viewType, view)
        functionStart()
    }

    private fun functionStart() {
        if (functionModel.isOn) return
        val droneNumber = mainProvider.getMainHandler().getScreenState().watchDroneNumber
        val drone = droneNumber?.let {
            DeviceUtils.getDrone(it) ?: DeviceUtils.allControlDrones().firstOrNull()
        }
        if (drone != null) {
            rangingViewModel.setRangingSwitch(drone, true)
        }
    }

    override fun onFunctionStop(viewType: FunctionViewType, view: View) {
        super.onFunctionStop(viewType, view)
        functionStop()
    }

    private fun functionStop() {
        val droneNumber = mainProvider.getMainHandler().getScreenState().watchDroneNumber
        val drone = droneNumber?.let {
            DeviceUtils.getDrone(it) ?: DeviceUtils.allControlDrones().firstOrNull()
        }
        if (drone != null) {
            rangingViewModel.setRangingSwitch(drone, false)
        }
    }
}