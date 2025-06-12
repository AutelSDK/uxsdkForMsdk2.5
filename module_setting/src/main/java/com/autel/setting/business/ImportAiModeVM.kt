package com.autel.setting.business

import androidx.lifecycle.viewModelScope
import com.autel.common.base.BaseViewModel
import com.autel.drone.sdk.libbase.error.IAutelCode
import com.autel.drone.sdk.vmodelx.bean.OfflineAiModule
import com.autel.drone.sdk.vmodelx.enum.CopyFileEnum
import com.autel.drone.sdk.vmodelx.interfaces.IAiServiceManage
import com.autel.drone.sdk.vmodelx.manager.CloudServiceManager
import com.autel.log.AutelLog
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import java.io.File

/**
 * @author 
 * @date 2023/8/8
 * 模型导入
 */
class ImportAiModeVM : BaseViewModel() {

    /**
     * 导入离线模型
     */
    fun importAiMode(list: List<File>, isReplace: Boolean, onResult: (Boolean) -> Unit) {
        AutelLog.i(TAG, "importAiMode -> $list")
        viewModelScope.launch(CoroutineExceptionHandler { _, e ->
            onResult.invoke(false)
            AutelLog.i(TAG, "importAiMode -> e$e")
        }) {
            CloudServiceManager.getInstance().getAiServiceManager()
                .synOfflineModules(
                    list,
                    object : IAiServiceManage.CompletionFilesCallbackWithProgress<Double> {
                        override fun onProgressUpdate(progress: Double) {
                            AutelLog.i(TAG, "importAiMode -> onProgressUpdate progress=$progress")
                        }

                        override fun onFileExisted(files: List<File>) {

                        }

                        override fun onSuccess(result: List<String>) {
                            AutelLog.i(TAG, "importAiMode -> onSuccess result=$result")
                            onResult.invoke(true)
                        }

                        override fun onFailure(error: IAutelCode) {
                            AutelLog.i(TAG, "importAiMode -> onFailure error=$error")
                            onResult.invoke(false)
                        }
                    },
                    if (isReplace) CopyFileEnum.REPLACEMENT else CopyFileEnum.JUMP_OVER
                )
        }
    }

    /**
     * 检查本地模型是否存在
     */
    fun checkAiMode(checkList: List<File>, onCheckResult: (list: List<File>) -> Unit) {
        viewModelScope.launch(CoroutineExceptionHandler { _, e ->
            AutelLog.e(TAG, "checkAiMode -> e=$e")
        }) {
            try {
                //已存在的离线模型 名称，路径
                val result = ArrayList<File>()
                val apiResult: Pair<String?, List<OfflineAiModule>> =
                    CloudServiceManager.getInstance().getAiServiceManager()
                        .queryLocalModuleByType(true)
                AutelLog.i(
                    TAG,
                    "checkAiMode -> first=${apiResult.first} second=${apiResult.second}"
                )
                val list = apiResult.second
                if (list.isNotEmpty() && checkList.isNotEmpty()) {
                    for (x in list) {
                        for (y in checkList) {
                            if (y.name.contains(x.moduleName)) {
                                result.add(y)
                            }
                        }
                    }
                }
                onCheckResult.invoke(result)
            } catch (e: Exception) {
                AutelLog.e(TAG, "checkAiMode -> e=$e")
            }
        }
    }

}