package com.external.uxdemo.function

import android.view.View
import com.autel.common.constant.AppTagConst
import com.autel.common.delegate.IDeviceSwitchListener
import com.autel.common.delegate.IMainProvider
import com.autel.common.delegate.function.AbsDelegateFunction
import com.autel.common.delegate.function.FunctionModel
import com.autel.common.delegate.function.FunctionType
import com.autel.common.delegate.function.FunctionViewType
import com.autel.common.manager.AppInfoManager
import com.autel.common.manager.MiddlewareManager
import com.autel.common.utils.DeviceUtils
import com.autel.common.widget.toast.AutelToast
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.manager.data.ControlMode
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.nest.enums.ModemModeEnum
import com.autel.log.AutelLog
import com.autel.widget.R
import com.autel.widget.function.inter.IFunctionManager
import com.autel.widget.function.inter.IFunctionOperate
import com.autel.widget.function.model.IFunctionModel
import com.autel.widget.function.model.SwitchFunctionModel


class FunctionManager(
    private val mainProvider: IMainProvider,
    private var functionViewType: FunctionViewType,
) : IFunctionManager,
    IDeviceSwitchListener {

    companion object {
        //工具栏最大数量，不含工具箱
        fun getFunctionBarMaxCount(): Int {
            return if (AppInfoManager.isSmallScreen()) {
                10
            } else {
                12
            }
        }

        //悬浮球最大数量，不含工具箱
        fun getFunctionBallMaxCount(): Int {
            return 8
        }
    }

    /** 编辑事件 */
    private val editListener: (isAdd: Boolean, functionItem: SwitchFunctionModel) -> Unit =
        { isAdd, functionItem ->
            if (isAdd) {
                switchPanelToBarList(functionItem)
            } else {
                switchBarToPanelList(functionItem)
            }
        }

    private val functionMore = MoreFunctionEntry(mainProvider)

    private val functionEntryList = MiddlewareManager.getFunctionEntryList(mainProvider)

    private val switchFunctionMore = switchFunctionEntryToUIModel(functionMore)

    /** 工具model map集合*/
    private val functionModelMap = functionEntryList.filter {
        it.getFunctionState().showEntry
    }.associate { delegateFunction ->
        val functionState = delegateFunction.getFunctionState()
        functionState.functionType to switchFunctionEntryToUIModel(
            delegateFunction
        )
    }.toMutableMap().apply {
        put(FunctionType.FunctionMore, switchFunctionMore)
    }

    private fun switchFunctionEntryToUIModel(delegateFunction: AbsDelegateFunction): SwitchFunctionModel {
        val function = delegateFunction.getFunctionState()
        return SwitchFunctionModel(
            functionModel = function,
            editListener = editListener,
            clickListener = { viewType, view ->
                if (!function.isOn) {
                    if (verifyFunctionEnable(function)) {
                        delegateFunction.onFunctionStart(viewType, view)
                        if (viewType == FunctionViewType.Panel) {
                            hidePanelIfNeeded(function.functionType)
                        }
                    }
                } else {
                    delegateFunction.onFunctionStop(viewType, view)
                }
                if (viewType == FunctionViewType.FloatBall) {
                    mainProvider.getMainHandler().hiddenFunctionFloatWindowPanel()
                }
            })
    }


    /** 工具delegate map集合*/
    private val functionDelegateMap = functionEntryList.associate {
        val function = it.getFunctionState()
        function.functionType to it
    }


    private fun initDefaultAllList(): List<FunctionType> {
        return if (DeviceUtils.isDockMode()) {
            listOf(
                FunctionType.ExitTakeOver,
                FunctionType.FunctionLine,
                FunctionType.AI,
                FunctionType.Track,
                FunctionType.Screenshot,
                FunctionType.ScreenRecording,
                FunctionType.Setting
            )
        } else if (DeviceUtils.isMainRC()) {
            AutelLog.i(AppTagConst.FunctionList, "is MainRC, init default all list")
            val list = mutableListOf(
                FunctionType.Track,
                FunctionType.Album,
                FunctionType.Personal,
                FunctionType.SingleMatch,
                FunctionType.NetMesh,
                FunctionType.NavigationLight,
                FunctionType.DrawArea,
                FunctionType.Setting,
                FunctionType.FlightRecord,
                FunctionType.DrawLine,
                FunctionType.Compass,
                FunctionType.MissionRectangle,
                FunctionType.MissionOblique,
                FunctionType.MissionPolygon,
                FunctionType.MissionPolyLine,
                FunctionType.MissionTaskRecord,
                FunctionType.MissionKml,
                FunctionType.FindDrone,
                FunctionType.DrawPointDrone,
                FunctionType.DrawPointRC,
                FunctionType.DrawPointFree,
                FunctionType.QuickTask,
                FunctionType.MissionWaypoint,
                FunctionType.MissionLib,
                FunctionType.CameraDemist,
                FunctionType.MissionSwarm,
                FunctionType.MarkHistory,
                FunctionType.QuickPuzzle,
                FunctionType.ProfessionalImaging,
                FunctionType.Surround,
                FunctionType.ObstacleAvoidance,
                FunctionType.CameraBrightness,
                FunctionType.FireStrike,
                FunctionType.Payload,
                FunctionType.DownFillLight,
                FunctionType.Detour,
                FunctionType.AI,
                FunctionType.LaserDistance,
                FunctionType.Screenshot,
                FunctionType.ScreenRecording,
                FunctionType.DataEncryption,
                FunctionType.AccurateReshoot,
                FunctionType.SwarmWaypoint,
                FunctionType.FENCE
            )
            if (AppInfoManager.isSupportRemoteControlCloud()) {
                list.add(FunctionType.CloudApiConfig)
            }
            /**GNSS开关*/
            if (AppInfoManager.isSupportGNSSShortCut()) {
                list.add(FunctionType.NoGpsFly)
            }
            /**超感光*/
            if (AppInfoManager.isSupportSuperLight()) {
                list.add(FunctionType.SuperLight)
            }
            /**直播*/
            if (AppInfoManager.isSupportLivePlay()) {
                list.add(FunctionType.LivePlay)
            }
            /**飞机推流*/
            if (AppInfoManager.isSupportDroneLivePlay()) {
                list.add(FunctionType.DroneLivePlay)
            }
            /**日志上传*/
            if (AppInfoManager.isSupportFlightLog()) {
                list.add(FunctionType.LogUpload)
            }
            /**公网群聊*/
            if (AppInfoManager.isSupportPNetChat()) {
                list.add(FunctionType.PNetChat)
            }
            /**隐蔽模式*/
            if (AppInfoManager.isSupportStealthMode()) {
                list.add(FunctionType.StealthMode)
            }

            /**
             * 是否支持热融合
             */
            if (AppInfoManager.isSupportIRFusion()) {
                list.add(FunctionType.IRFusion)
            }

            list.add(FunctionType.ManualFormat)
            list.add(FunctionType.UserManual)
            if (AppInfoManager.isSupport3DScan()){
                list.add(FunctionType.SCAN_3D)
            }
//            list.add(FunctionType.GIMBAL_FILL_LIGHT)
            if (AppInfoManager.isSupportUserManual()) {
                list.add(FunctionType.UserManual)
            }
            if (AppInfoManager.isModelH()) {
                list.add(FunctionType.Tracer)
            }
            list
        } else {
            AutelLog.i(AppTagConst.FunctionList, "is not MainRC, init default all list")
            val list = mutableListOf(
                FunctionType.Setting,
                FunctionType.SingleMatch,
                FunctionType.NetMesh,
                FunctionType.DrawLine,
                FunctionType.DrawArea,
                FunctionType.MarkHistory,
                FunctionType.DrawPointDrone,
                FunctionType.DrawPointRC,
                FunctionType.DrawPointFree,
                FunctionType.LaserDistance,
                FunctionType.Compass,
                FunctionType.ScreenRecording,
                FunctionType.Screenshot,
            )
            if (AppInfoManager.isSupportUserManual()) {
                list.add(FunctionType.UserManual)
            }
            if (AppInfoManager.isSupportRemoteControlCloud()) {
                list.add(FunctionType.CloudApiConfig)
            }
            list
        }
    }

    private val allFunctionList = initDefaultAllList()

    private val barStyle: FunctionStyleImpl =
        FunctionBarStyleImpl(mainProvider.getMainContext(), allFunctionList, functionModelMap)

    private val floatBallStyle: FunctionStyleImpl =
        FunctionFloatWindowStyleImpl(mainProvider.getMainContext(), allFunctionList, functionModelMap)

    private var delegateStyle = if (isBarStyleViewType()) {
        barStyle
    } else {
        floatBallStyle
    }

    override fun getFunctionBarList(): List<IFunctionModel> {
        return delegateStyle.getFunctionBarList()
    }

    override fun getMoreFunctionModel(): SwitchFunctionModel {
        return switchFunctionMore
    }


    fun onCreate() {
        functionMore.getPanelView().attachFunctionManager(this)
        allFunctionList.forEach {
            functionDelegateMap[it]?.onFunctionCreate()
        }
        functionMore.onFunctionCreate()
    }

    fun onDestroy() {
        allFunctionList.forEach {
            functionDelegateMap[it]?.onFunctionDestroy()
        }
        functionMore.onFunctionDestroy()
    }


    override fun onMainServiceValid(valid: Boolean, drone: IAutelDroneDevice) {
        allFunctionList.forEach {
            functionDelegateMap[it]?.onMainServiceValid(valid, drone)
        }
        functionMore.onMainServiceValid(valid, drone)
    }

    override fun onDroneChangedListener(
        connected: Boolean,
        drone: IAutelDroneDevice,
    ) {
        allFunctionList.forEach {
            functionDelegateMap[it]?.onDroneChangedListener(connected, drone)
        }
        functionMore.onDroneChangedListener(connected, drone)
    }

    override fun onModemModeChange(mode: ModemModeEnum) {
        allFunctionList.forEach {
            functionDelegateMap[it]?.onModemModeChange(mode)
        }
        functionMore.onModemModeChange(mode)
    }

    override fun onCameraAbilityFetchListener(
        localFetched: Boolean,
        remoteFetched: Boolean,
        drone: IAutelDroneDevice,
    ) {
        allFunctionList.forEach {
            functionDelegateMap[it]?.onCameraAbilityFetchListener(
                localFetched,
                remoteFetched,
                drone
            )
        }
        functionMore.onCameraAbilityFetchListener(localFetched, remoteFetched, drone)
    }

    override fun onControlChange(mode: ControlMode, droneList: List<IAutelDroneDevice>) {
        allFunctionList.forEach {
            functionDelegateMap[it]?.onControlChange(mode, droneList)
        }
        functionMore.onControlChange(mode, droneList)
    }

    override fun onDroneCreate(drone: IAutelDroneDevice) {
        allFunctionList.forEach {
            functionDelegateMap[it]?.onDroneCreate(drone)
        }
        functionMore.onDroneCreate(drone)
    }

    override fun onDroneDestroy(drone: IAutelDroneDevice) {
        allFunctionList.forEach {
            functionDelegateMap[it]?.onDroneDestroy(drone)
        }
        functionMore.onDroneDestroy(drone)
    }

    private fun switchBarToPanelList(functionItem: SwitchFunctionModel) {
        delegateStyle.switchBarToPanelList(functionItem)
    }

    private fun switchPanelToBarList(functionItem: SwitchFunctionModel) {
        delegateStyle.switchPanelToBarList(functionItem)
    }

    private fun verifyFunctionEnable(function: FunctionModel): Boolean {
        val type = function.functionType
        if (type.capacityList.isEmpty()) {
            return true
        } else {
            val needCloseCapacityList = mutableListOf<AbsDelegateFunction>()
            functionDelegateMap.forEach {
                var need = false
                if (it.value.getFunctionState().isOn) {
                    it.key.capacityList.forEach {
                        if (type.capacityList.contains(it)) {
                            need = true
                        }
                    }
                }
                if (need && !it.value.willBeChangedStatePassively(false, type)) {
                    needCloseCapacityList.add(it.value)
                }
            }
            if (needCloseCapacityList.isEmpty()) {
                return true
            }
            val strBuilder = StringBuilder()
            needCloseCapacityList.forEachIndexed { index, value ->
                if (index != needCloseCapacityList.size - 1) {
                    value.getFunctionDisplayTitle()?.let {
                        strBuilder.append(it).append("")
                    } ?: run {
                        strBuilder.append(value.getFunctionState().functionName).append("")
                    }
                } else {
                    value.getFunctionState().funDisplayTitle?.let {
                        strBuilder.append(value.getFunctionState().funDisplayTitle)
                    } ?: run {
                        strBuilder.append(value.getFunctionState().functionName)
                    }
                }
            }
            AutelToast.normalToast(
                mainProvider.getMainContext(),
                mainProvider.getMainContext().getString(
                    R.string.common_text_need_to_close_function,
                    strBuilder.toString(),
                    function.funDisplayTitle ?: run {
                        function.functionName
                    }
                )
            )
            return false
        }
    }

    /** 功能打开时需要关闭设置面板 */
    private fun hidePanelIfNeeded(functionType: FunctionType) {
        if (functionType == FunctionType.DataEncryption
            || functionType == FunctionType.MissionKml
            || functionType == FunctionType.Album
            || functionType == FunctionType.LivePlay
            || functionType == FunctionType.Personal
            || functionType == FunctionType.Setting
            || functionType == FunctionType.FlightRecord
            || functionType == FunctionType.LogUpload
            || functionType == FunctionType.NetMesh
        ) {
            mainProvider.getMainHandler().hiddenFunctionPanel()
        }
    }

    /**
     * 重置状态栏
     */
    override fun resetFunction() {
        delegateStyle.resetFunction()
    }

    override fun openFunctionPanelAndEnterEditMode(viewType: FunctionViewType, view: View) {
        if (!getMoreFunctionModel().functionModel.isOn) {
            functionMore.onFunctionStart(viewType, view)
        }
        functionMore.enterEditMode()
    }

    override fun saveBarToStorage() {
        delegateStyle.saveBarToStorage()
    }

    override fun savePanelToStorage() {
        delegateStyle.savePanelToStorage()
    }

    override fun bindPanelInBarStyleOperate(operate: IFunctionOperate) {
        barStyle.bindPanelOperate(operate)
    }

    override fun bindBarInBarStyleOperate(operate: IFunctionOperate) {
        barStyle.bindBarOperate(operate)
    }

    override fun bindPanelInFloatStyleOperate(operate: IFunctionOperate) {
        floatBallStyle.bindPanelOperate(operate)
    }

    override fun bindBarInFloatStyleOperate(operate: IFunctionOperate) {
        floatBallStyle.bindBarOperate(operate)
    }

    override fun getFunctionPanelList(): List<SwitchFunctionModel> {
        return delegateStyle.getFunctionPanelList()
    }

    fun updateFunction(functionModel: FunctionModel) {
        delegateStyle.updateFunction(functionModel)
    }

    fun hiddenFunctionPanel() {
        functionMore.hiddenFunctionPanel()
    }

    fun switchViewStyle(viewType: FunctionViewType) {
        functionViewType = viewType
        delegateStyle = if (isBarStyleViewType()) {
            barStyle
        } else {
            floatBallStyle
        }
        delegateStyle.refreshAllFunction()
    }

    private fun isBarStyleViewType(): Boolean {
        return functionViewType == FunctionViewType.Bar
    }
}