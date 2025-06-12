package com.external.uxdemo.function

import android.content.Context
import com.autel.common.constant.AppTagConst
import com.autel.common.delegate.function.FunctionModel
import com.autel.common.delegate.function.FunctionType
import com.autel.common.manager.AppInfoManager
import com.autel.common.manager.AutelStorageManager
import com.autel.common.manager.StorageKey
import com.autel.common.utils.DeviceUtils
import com.autel.common.widget.toast.AutelToast
import com.autel.log.AutelLog
import com.autel.widget.R
import com.autel.widget.function.inter.IFunctionOperate
import com.autel.widget.function.model.EmptyFunctionModel
import com.autel.widget.function.model.IFunctionModel
import com.autel.widget.function.model.SwitchFunctionModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


class FunctionFloatWindowStyleImpl(
    private val context: Context,
    private val allFunctionList: List<FunctionType>,
    private val functionModelMap: Map<FunctionType, SwitchFunctionModel>
) :
    FunctionStyleImpl {

    private var panelFunctionOperate: IFunctionOperate? = null

    private var barFunctionOperate: IFunctionOperate? = null


    /** 不能超过 12个 */
    private val defaultBarList = initDefaultBarList().apply {
        apply {
            if (AppInfoManager.isBuildTypeDebug() && this.size > FunctionManager.getFunctionBallMaxCount()) {
                throw IllegalStateException("defaultBarList size must less than ${FunctionManager.getFunctionBallMaxCount()}")
            }
        }
    }

    private fun initDefaultBarList(): List<FunctionType> {
        return if (DeviceUtils.isMainRC()) {
            AutelLog.i(AppTagConst.FunctionList, "isMainRC, init default bar list")
            mutableListOf(
                FunctionType.DownFillLight,
                FunctionType.Detour,
                FunctionType.AI,
                FunctionType.Track,
                FunctionType.LaserDistance,
                FunctionType.Screenshot,
                FunctionType.ScreenRecording,
                FunctionType.MissionWaypoint
            )
        } else {
            AutelLog.i(AppTagConst.FunctionList, "is not MainRC, init default bar list")
            mutableListOf(
                FunctionType.DrawPointDrone,
                FunctionType.DrawPointRC,
                FunctionType.DrawPointFree,
                FunctionType.Compass,
                FunctionType.Screenshot,
                FunctionType.ScreenRecording
            )
        }
    }

    private var functionBarList = initFunctionBarList()


    private fun initFunctionPanelList(): MutableList<SwitchFunctionModel> {
        val functionPanelCacheList = AutelStorageManager.getPlainStorage()
            .getStringValue(StorageKey.PlainKey.KEY_FUNCTION_PANEL_LIST_IN_FLOAT)
        var result: List<FunctionType>? = null
        val gson = Gson()
        try {
            result = gson.fromJson<List<FunctionType>>(
                functionPanelCacheList, object : TypeToken<List<FunctionType>>() {}.type
            )
        } catch (ex: Exception) {
            AutelLog.e(AppTagConst.FunctionList, "init function panel list failed")
        }
        if (result == null) {
            result = allFunctionList
        } else {
            result = result.filter { allFunctionList.contains(it) }
        }

        val functionBarTypeList =
            functionBarList.mapNotNull { if (it is SwitchFunctionModel) it.functionModel.functionType else null }
        val panelList = result.filter { !functionBarTypeList.contains(it) }.toMutableList()

        allFunctionList.forEach {
            if ((!functionBarTypeList.contains(it)) && (!panelList.contains(it))) {
                panelList.add(it)
            }
        }

        return panelList.mapNotNull {
            if (panelList.contains(it)) {
                functionModelMap[it]
            } else {
                null
            }
        }.toMutableList()
    }

    private fun initFunctionBarList(): MutableList<IFunctionModel> {
        val functionBarCacheList = AutelStorageManager.getPlainStorage()
            .getStringValue(StorageKey.PlainKey.KEY_FUNCTION_BAR_LIST_IN_FLOAT)
        var result: List<FunctionType>? = null
        val gson = Gson()
        try {
            result = gson.fromJson<List<FunctionType>>(
                functionBarCacheList, object : TypeToken<List<FunctionType>>() {}.type
            )
        } catch (ex: Exception) {
            AutelLog.e(AppTagConst.FunctionList, "init function bar list failed")
        }
        if (result == null || result.size > FunctionManager.getFunctionBallMaxCount()) {
            AutelStorageManager.getPlainStorage()
                .setStringValue(StorageKey.PlainKey.KEY_FUNCTION_BAR_LIST_IN_FLOAT, null)
            AutelStorageManager.getPlainStorage()
                .setStringValue(StorageKey.PlainKey.KEY_FUNCTION_PANEL_LIST_IN_FLOAT, null)
            result = defaultBarList
        }

        val resultList = result.mapNotNull {
            if (it == FunctionType.FunctionEmpty) {
                EmptyFunctionModel()
            } else if (allFunctionList.contains(it)) {
                functionModelMap[it]
            } else {
                EmptyFunctionModel()
            }
        }.toMutableList()


        val resultSize = resultList.size
        if (resultSize < FunctionManager.getFunctionBallMaxCount()) {
            for (i in resultSize until FunctionManager.getFunctionBallMaxCount()) {
                resultList.add(i, EmptyFunctionModel())
            }
        }

        functionModelMap[FunctionType.FunctionMore]?.let {
            resultList.add(FunctionManager.getFunctionBallMaxCount(), it)
        }

        return resultList
    }


    private var functionPanelList = initFunctionPanelList()

    override fun getFunctionPanelList(): List<SwitchFunctionModel> {
        return functionPanelList
    }

    override fun getFunctionBarList(): List<IFunctionModel> {
        return functionBarList
    }


    override fun switchPanelToBarList(functionItem: SwitchFunctionModel) {
        if (toFunctionTypeList(functionBarList).count { it!=FunctionType.FunctionEmpty } < FunctionManager.getFunctionBallMaxCount()) {
            val targetIndex = functionBarList.indexOfFirst { it is EmptyFunctionModel }
            if (targetIndex == -1) {
                return
            }
            val panelPosition = functionPanelList.indexOf(functionItem)
            if (panelPosition == -1) {
                return
            }
            functionPanelList.remove(functionItem)
            functionBarList.removeAt(targetIndex)
            functionBarList.add(targetIndex, functionItem)
            panelFunctionOperate?.notifyItemRemoved(panelPosition)
            barFunctionOperate?.notifyItemChanged(targetIndex)
            saveBarToStorage()
            savePanelToStorage()
        } else {
            //达到最大数量，Toast提示
            AutelToast.normalToast(
                context,
                context.getString(
                    R.string.common_text_function_reach_max_count,
                    "${FunctionManager.getFunctionBallMaxCount()}"
                )
            )
        }
        AutelLog.i(AppTagConst.FunctionList, "switch panel to bar list success")
    }

    override fun switchBarToPanelList(functionItem: SwitchFunctionModel) {
        val barPosition = functionBarList.indexOf(functionItem)
        if (barPosition == -1) {
            return
        }
        functionBarList.remove(functionItem)
        functionBarList.add(barPosition, EmptyFunctionModel(true))
        functionPanelList.add(functionItem)
        val panelPosition = functionPanelList.indexOf(functionItem)
        panelFunctionOperate?.notifyItemInserted(panelPosition)
        barFunctionOperate?.notifyItemChanged(barPosition)
        saveBarToStorage()
        savePanelToStorage()
        AutelLog.i(AppTagConst.FunctionList, "switch bar to panel list success")
    }

    /**
     * 重置状态栏
     */
    override fun resetFunction() {
        AutelStorageManager.getPlainStorage()
            .setStringValue(StorageKey.PlainKey.KEY_FUNCTION_BAR_LIST_IN_FLOAT, null)
        AutelStorageManager.getPlainStorage()
            .setStringValue(StorageKey.PlainKey.KEY_FUNCTION_PANEL_LIST_IN_FLOAT, null)
        functionBarList = initFunctionBarList()
        functionPanelList = initFunctionPanelList()
        panelFunctionOperate?.resetFunction()
        barFunctionOperate?.resetFunction()
    }

    override fun saveBarToStorage() {
        val list =toFunctionTypeList(functionBarList)

        AutelStorageManager.getPlainStorage()
            .setStringValue(StorageKey.PlainKey.KEY_FUNCTION_BAR_LIST_IN_FLOAT, Gson().toJson(list))
    }

    override fun savePanelToStorage() {
        val list =toFunctionTypeList(functionPanelList)
        AutelStorageManager.getPlainStorage()
            .setStringValue(StorageKey.PlainKey.KEY_FUNCTION_PANEL_LIST_IN_FLOAT, Gson().toJson(list))
    }

    private fun toFunctionTypeList(functionTypeList: List<IFunctionModel>): List<FunctionType> {
        return functionTypeList.mapNotNull {
            if (it is SwitchFunctionModel) {
                if (it.functionModel.functionType != FunctionType.FunctionMore) {
                    it.functionModel.functionType
                } else {
                    null
                }
            } else if (it is EmptyFunctionModel) {
                FunctionType.FunctionEmpty
            } else {
                null
            }
        }
    }

    override fun bindPanelOperate(operate: IFunctionOperate) {
        panelFunctionOperate = operate
    }

    override fun bindBarOperate(operate: IFunctionOperate) {
        barFunctionOperate = operate
    }

    override fun updateFunction(functionModel: FunctionModel) {
        val function = functionModelMap[functionModel.functionType] ?: return
        if (function is SwitchFunctionModel) {
            barFunctionOperate?.refreshFunction(function)
            panelFunctionOperate?.refreshFunction(function)
        }
    }

    override fun refreshAllFunction() {
        panelFunctionOperate?.resetFunction()
        barFunctionOperate?.resetFunction()
    }
}