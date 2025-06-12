package com.external.uxdemo.view

import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.autel.codec.splitscreen.business.ScreenStateManager
import com.autel.common.base.BaseAircraftActivity
import com.autel.common.bean.MainViewLocation
import com.autel.common.constant.MainLayoutViewsId
import com.autel.common.delegate.IMainHandler
import com.autel.common.delegate.IMainLayoutManager
import com.autel.common.delegate.IMainProvider
import com.autel.common.delegate.function.FunctionBarState
import com.autel.common.delegate.function.FunctionModel
import com.autel.common.delegate.function.FunctionViewType
import com.autel.common.feature.phone.StorageManager
import com.autel.common.feature.route.RouteManager
import com.autel.common.feature.route.RouterConst
import com.autel.common.manager.AppInfoManager
import com.autel.common.manager.AutelStorageManager
import com.autel.common.manager.StorageKey
import com.autel.common.model.splitscreen.AircraftScreenItem
import com.autel.common.model.splitscreen.AircraftScreenState
import com.autel.common.model.splitscreen.AircraftScreenStore
import com.autel.common.utils.BusinessType
import com.autel.common.utils.DeviceUtils
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.interfaces.IControlDroneListener
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.drone.sdk.vmodelx.manager.data.ControlMode
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.nest.enums.ModemModeEnum

import com.autel.widget.function.FunctionBarVM
import com.external.uxddemo.databinding.UxsdkdemoActivityBinding
import com.external.uxdemo.delegate.MainLayoutManager
import com.external.uxdemo.function.FunctionManager

/**
 * Created by  2023/5/11
 *  主页面基类
 */
open class BaseMainActivity : BaseAircraftActivity(), IMainProvider, IControlDroneListener {
    lateinit var uiBinding: UxsdkdemoActivityBinding

    lateinit var functionManager: FunctionManager
    private var mainLayoutManager: IMainLayoutManager? = null
    protected val functionBarVm: FunctionBarVM by viewModels()

    private var isDockMode: Boolean = false
    private var viewsMap: HashMap<String, View> = hashMapOf()

    private val mainHandler = object : IMainHandler {


        override fun addViewToRootView(view: View, layoutParams: ViewGroup.LayoutParams) {
            uiBinding.root.addView(view, layoutParams)
        }

        override fun updateFunctionState(functionModel: FunctionModel) {
            functionManager.updateFunction(functionModel)
        }

        override fun observerScreenState(observer: Observer<AircraftScreenState>) {
            ScreenStateManager.getInstance().observerScreenState(this@BaseMainActivity, observer)
        }

        override fun removeObserverScreenState(observer: Observer<AircraftScreenState>) {
            ScreenStateManager.getInstance().removeScreenStateObserver(observer)
        }

        override fun getScreenState(): AircraftScreenState {
            return ScreenStateManager.getInstance().getPageLocationState()
        }

        override fun getPageScreenStore(): AircraftScreenStore {
            return ScreenStateManager.getInstance().getPageScreenStore()
        }

        override fun enlargeAircraftScreenState(screenItem: AircraftScreenItem) {
            ScreenStateManager.getInstance().updatePageEnlargeItem(screenItem)
        }

        override fun enterFunctionEditModel() {
            if (FunctionViewType.find(
                    AutelStorageManager.getPlainStorage()
                        .getIntValue(StorageKey.PlainKey.KEY_FUNCTION_QUICK_ACTION)
                ) == FunctionViewType.FloatBall
            ) {
                uiBinding.functionFloatWindowView.enterEditMode()
                uiBinding.functionSuspensionView.visibility = View.GONE
                uiBinding.functionFloatWindowView.setClickDismiss(false)
                uiBinding.functionFloatWindowView.showCenter()
            } else {
                uiBinding.functionView.enterEditMode()
            }
        }

        override fun exitFunctionEditModel() {
            if (FunctionViewType.find(
                    AutelStorageManager.getPlainStorage()
                        .getIntValue(StorageKey.PlainKey.KEY_FUNCTION_QUICK_ACTION)
                ) == FunctionViewType.FloatBall
            ) {
                uiBinding.functionFloatWindowView.exitEditMode()
                uiBinding.functionFloatWindowView.visibility = View.GONE
                uiBinding.functionSuspensionView.visibility = View.VISIBLE
            } else {
                uiBinding.functionView.exitEditMode()
            }


        }

        override fun hiddenFunctionPanel() {
            functionManager.hiddenFunctionPanel()
        }

        override fun hiddenFunctionFloatWindowPanel() {
            uiBinding.functionFloatWindowView.visibility = View.GONE
            uiBinding.functionSuspensionView.visibility = View.VISIBLE
        }

        override fun getMainViewLocation(): MainViewLocation {
            return MainViewLocation(
                gimbalRectPosition = getViewLocation(uiBinding.flGimbal),
                cancelLandingPosition = getViewLocation(uiBinding.cancelLand),
                screenPosition = getViewLocation(uiBinding.screenShortcutView),
                cameraPosition = uiBinding.codecToolRight.getCameraPosition(),
                takePhotoPosition = uiBinding.codecToolRight.getTakePhotoPosition(),
                takeVideoPosition = uiBinding.codecToolRight.getTakeVideoPosition(),
                albumPosition = uiBinding.codecToolRight.getAlbumPosition(),
                aslPosition = getViewLocation(uiBinding.attitudeBall)
            )
        }

        override fun observeFunctionBarState(observer: Observer<FunctionBarState>) {
            functionBarVm.functionBarLD.observe(this@BaseMainActivity, observer)
        }

        override fun getFunctionBarState(): FunctionBarState {
            return functionBarVm.functionBarLD.value ?: FunctionBarState.Unfolded
        }

        override fun observeFullScreen(observer: Observer<Boolean>) {
            ScreenStateManager.getInstance().observerFullScreen(this@BaseMainActivity, observer)
        }

        override fun getVirtualFunctionView(): View {
            return uiBinding.virtualBar
        }

        override fun launchNewMainActivity() {
            RouteManager.routeTo(
                AppInfoManager.getApplicationContext(),
                RouterConst.PathConst.ACTIVITY_URL_SPLASH
            )
        }

        override fun getMainLayoutChildren(): HashMap<String, View> {
            return viewsMap
        }

        override fun isRunDockMode(): Boolean {
            return isDockMode
        }

        override fun updateFunctionBarLD(barState: FunctionBarState) {
            functionBarVm.updateFunctionBarLD(barState)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        uiBinding = UxsdkdemoActivityBinding.inflate(layoutInflater)
        ScreenStateManager.getInstance()
            .reInit(uiBinding.autelSplitScreenContainer::getOrCreateScreenView)
        functionManager = FunctionManager(
            this, FunctionViewType.find(
                AutelStorageManager.getPlainStorage()
                    .getIntValue(StorageKey.PlainKey.KEY_FUNCTION_QUICK_ACTION)
            )
        )
        uiBinding.functionView.setMaxCount(FunctionManager.getFunctionBarMaxCount())
        uiBinding.functionView.attachFunctionManager(functionManager)
        uiBinding.functionView.isShowMoreFunction(!DeviceUtils.isDockMode())
        uiBinding.functionFloatWindowView.attachFunctionManager(functionManager)
        setContentView(uiBinding.root)
        addChildrenToMap()
        if (DeviceUtils.isBusinessTypeValid(BusinessType.NETMESH)) {
            DeviceManager.getMultiDeviceOperator().addControlChangeListener(this)
        }
        isDockMode = DeviceUtils.isDockMode()
        functionManager.onCreate()
        StorageManager.getInstance().startMonitoring()
    }

    private fun addChildrenToMap() {
        viewsMap[MainLayoutViewsId.GIMBAL_PITCH_ID] = uiBinding.flGimbal
        viewsMap[MainLayoutViewsId.CANCEL_LANDING_ID] = uiBinding.cancelLand
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        functionManager.onDestroy()
        ScreenStateManager.getInstance().destroyAllView()
        if (DeviceUtils.isBusinessTypeValid(BusinessType.NETMESH)) {
            DeviceManager.getMultiDeviceOperator().removeControlChangeListener(this)
        }
    }

    override fun onDroneChangedListener(
        connected: Boolean,
        drone: IAutelDroneDevice
    ) {
        super.onDroneChangedListener(connected, drone)
        functionManager.onDroneChangedListener(connected, drone)
    }

    override fun onCameraAbilityFetchListener(
        localFetched: Boolean,
        remoteFetched: Boolean,
        drone: IAutelDroneDevice,
    ) {
        super.onCameraAbilityFetchListener(localFetched, remoteFetched, drone)
        functionManager.onCameraAbilityFetchListener(localFetched, remoteFetched, drone)
    }

    override fun onMainServiceValid(valid: Boolean, drone: IAutelDroneDevice) {
        super.onMainServiceValid(valid, drone)
        functionManager.onMainServiceValid(valid, drone)
    }


    override fun onControlChange(mode: ControlMode, droneList: List<IAutelDroneDevice>) {
        functionManager.onControlChange(mode, droneList)
    }

    override fun onModemModeChange(mode: ModemModeEnum) {
        functionManager.onModemModeChange(mode)
    }

    override fun getMainLayoutManager(): IMainLayoutManager {
        if (mainLayoutManager == null) {
            mainLayoutManager = MainLayoutManager(
                uiBinding.layoutContainer,
                uiBinding.layoutPanel,
                uiBinding.functionContainer
            )
        }
        return mainLayoutManager!!
    }

    override fun getMainLifecycleOwner(): LifecycleOwner {
        return this
    }

    override fun getMainContext(): Context {
        return this
    }

    override fun getMainFragmentManager(): FragmentManager {
        return supportFragmentManager
    }

    override fun getMainHandler(): IMainHandler {
        return mainHandler
    }


    private fun getViewLocation(view: View): Rect {
        val location = IntArray(2)
        val rect = Rect()
        view.getLocationOnScreen(location)
        rect.left = location[0]
        rect.top = location[1]
        rect.right = rect.left + view.width
        rect.bottom = rect.top + view.height
        return rect
    }

}