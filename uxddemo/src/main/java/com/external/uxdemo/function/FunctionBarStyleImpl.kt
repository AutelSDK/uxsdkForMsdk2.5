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


class FunctionBarStyleImpl(
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
            if (AppInfoManager.isBuildTypeDebug() && this.size > FunctionManager.getFunctionBarMaxCount()) {
                throw IllegalStateException("defaultBarList size must less than ${FunctionManager.getFunctionBarMaxCount()}")
            }
        }
    }

    private fun initDefaultBarList(): List<FunctionType> {
        return if (DeviceUtils.isDockMode()) {
            val list = mutableListOf<FunctionType>()
            list += FunctionType.ExitTakeOver
            list += FunctionType.FunctionLine
            list += FunctionType.AI
            list += FunctionType.Track
            for (i in 0 until (FunctionManager.getFunctionBarMaxCount() - 7)) {
                list+=FunctionType.FunctionEmpty
            }
            list += FunctionType.Screenshot
            list += FunctionType.ScreenRecording
            list += FunctionType.Setting
            list
        } else if (DeviceUtils.isMainRC()) {
            AutelLog.i(AppTagConst.FunctionList, "isMainRC, init default bar list")
            val list = mutableListOf<FunctionType>()
            //如果支持GNSS，则把GNSS放第一个
            if (AppInfoManager.isSupportGNSSShortCut()){
                list += FunctionType.NoGpsFly
            }
            list += FunctionType.DownFillLight
            list += FunctionType.Detour
            list += FunctionType.AI
            list += FunctionType.LaserDistance
            list += FunctionType.Screenshot
            list += FunctionType.ScreenRecording
            list += FunctionType.DataEncryption
            list
        } else {
            AutelLog.i(AppTagConst.FunctionList, "is not MainRC, init default bar list")
            mutableListOf(
                FunctionType.DrawPointDrone,
                FunctionType.DrawPointRC,
                FunctionType.DrawPointFree,
                FunctionType.Compass,
                FunctionType.ScreenRecording,
                FunctionType.Screenshot,
            )
        }
    }

    private var functionBarList = initFunctionBarList()


    private fun initFunctionPanelList(): MutableList<SwitchFunctionModel> {
        val functionPanelCacheList = AutelStorageManager.getPlainStorage()
            .getStringValue(StorageKey.PlainKey.KEY_FUNCTION_PANEL_LIST)
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
            .getStringValue(StorageKey.PlainKey.KEY_FUNCTION_BAR_LIST)
        var result: List<FunctionType>? = null
        val gson = Gson()
        try {
            result = gson.fromJson<List<FunctionType>>(
                functionBarCacheList, object : TypeToken<List<FunctionType>>() {}.type
            )
        } catch (ex: Exception) {
            AutelLog.e(AppTagConst.FunctionList, "init function bar list failed")
        }
        if (DeviceUtils.isDockMode()) {
            result = defaultBarList
        } else if (result == null || result.size > FunctionManager.getFunctionBarMaxCount()) {
            AutelStorageManager.getPlainStorage()
                .setStringValue(StorageKey.PlainKey.KEY_FUNCTION_BAR_LIST, null)
            AutelStorageManager.getPlainStorage()
                .setStringValue(StorageKey.PlainKey.KEY_FUNCTION_PANEL_LIST, null)
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
        if (resultSize < FunctionManager.getFunctionBarMaxCount()) {
            for (i in 0 until FunctionManager.getFunctionBarMaxCount() - resultSize) {
                resultList.add(resultSize / 2 + (resultSize % 2) + i, EmptyFunctionModel())
            }
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
        if (toFunctionTypeList(functionBarList).count { it != FunctionType.FunctionEmpty } < FunctionManager.getFunctionBarMaxCount()) {
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
                    "${FunctionManager.getFunctionBarMaxCount()}"
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
        functionBarList.add(barPosition, EmptyFunctionModel())
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
            .setStringValue(StorageKey.PlainKey.KEY_FUNCTION_BAR_LIST, null)
        AutelStorageManager.getPlainStorage()
            .setStringValue(StorageKey.PlainKey.KEY_FUNCTION_PANEL_LIST, null)
        functionBarList = initFunctionBarList()
        functionPanelList = initFunctionPanelList()
        panelFunctionOperate?.resetFunction()
        barFunctionOperate?.resetFunction()
    }

    override fun saveBarToStorage() {
        val list = toFunctionTypeList(functionBarList)

        AutelStorageManager.getPlainStorage()
            .setStringValue(StorageKey.PlainKey.KEY_FUNCTION_BAR_LIST, Gson().toJson(list))
    }

    override fun savePanelToStorage() {
        val list = toFunctionTypeList(functionPanelList)
        AutelStorageManager.getPlainStorage()
            .setStringValue(StorageKey.PlainKey.KEY_FUNCTION_PANEL_LIST, Gson().toJson(list))
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