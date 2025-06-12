package com.autel.setting.business

import android.text.TextUtils
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.autel.cloud.aiservice.bean.CloudApiBean
import com.autel.cloud.aiservice.bean.ModelListResponse
import com.autel.common.base.BaseViewModel
import com.autel.common.bean.AiModelBean
import com.autel.common.bean.AiModelState
import com.autel.common.extension.asLiveData
import com.autel.common.manager.AiServiceManager
import com.autel.common.sdk.KeyManagerCoroutineWrapper
import com.autel.common.utils.DeviceUtils
import com.autel.drone.sdk.libbase.error.AutelStatusCode
import com.autel.drone.sdk.vmodelx.bean.OfflineAiModule
import com.autel.drone.sdk.vmodelx.device.DroneType
import com.autel.drone.sdk.vmodelx.manager.CloudServiceManager
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.drone.sdk.vmodelx.manager.keyvalue.callback.CommonCallbacks
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.AirLinkKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.base.KeyTools
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.alink.enums.HighSpeedEnum
import com.autel.log.AutelLog
import com.autel.setting.bean.FileTypeEnum
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import java.io.File

/**
 * @author
 * @date 2023/6/19
 * 设置，目标识别设置
 */
class SettingChooseTargetVM : BaseViewModel() {
    private val _highSpeedState = MutableLiveData<HighSpeedEnum?>()
    val highSpeedState = _highSpeedState.asLiveData()

    /**
     * 查询模型列表
     * @param onSuccess 模型列表
     * @param onError 错误码 true 表示token过期，false表示其他错误
     */
    fun getAiModelList(onSuccess: (List<AiModelBean>) -> Unit, onError: (Boolean) -> Unit) {
        viewModelScope.launch(CoroutineExceptionHandler { _, e ->
            onError.invoke(false)
            AutelLog.e(TAG, "getAiModelList -> e=$e")
        }) {
            val aiModeList = ArrayList<AiModelBean>()
            val droneType =DeviceUtils.singleControlDrone()?.getDroneType() ?: DroneType.UNKNOWN
            val apiResult: CloudApiBean<ModelListResponse> =
                CloudServiceManager.getInstance().getAiServiceManager().searchAiModelList(1, 30, droneType.value)
            AutelLog.i(TAG, "getAiModelList -> apiResult=$apiResult")
            if (apiResult.ret_code == 0 && apiResult.data != null) {
                apiResult.data?.models?.let { list ->
                    for (x in list.indices) {
                        val item = list[x]
                        val modelBean = AiModelBean()
                        modelBean.projectId = item.project_id ?: ""
                        modelBean.projectName = item.project_name ?: ""
                        modelBean.projectDesc = item.project_desc ?: ""
                        modelBean.modelId = item.id ?: ""
                        modelBean.isBest = item.is_best ?: false
                        modelBean.verCode = item.ver_code ?: 0
                        modelBean.md5 = item.md5 ?: ""
                        modelBean.size = item.size ?: ""
                        modelBean.url = item.url ?: ""
                        modelBean.index = x
                        //判断模型文件是否存在
                        val isExist = CloudServiceManager.getInstance().getAiServiceManager().queryModuleIsExist(modelBean.modelId, modelBean.md5)
                        modelBean.state = if (isExist) AiModelState.UPLOAD_WAIT else AiModelState.DOWNLOAD_WAIT
                        aiModeList.add(modelBean)
                    }
                }
                onSuccess.invoke(aiModeList)
            } else {
                //token过期
                if (apiResult.ret_code == AutelStatusCode.TOKEN_HAS_EXPIRE.code) {
                    AiServiceManager.resetAiService()
                    onError.invoke(true)
                } else {
                    onError.invoke(false)
                }
            }
        }
    }

    /**
     * 模型是否存在
     */
    fun isAiModeExit(bean: AiModelBean, onSuccess: (Boolean) -> Unit) {
        viewModelScope.launch(CoroutineExceptionHandler { _, e ->
            onSuccess.invoke(false)
            AutelLog.e(TAG, "isAiModeExit -> e=$e")
        }) {
            val result = CloudServiceManager.getInstance().getAiServiceManager().queryModuleIsExist(bean.modelId, bean.md5)
            onSuccess.invoke(result)
        }
    }

    /**
     * 下载模型
     */
    fun downloadModel(
        sourcePath: String,
        modelID: String,
        fileMd5: String,
        listener: CommonCallbacks.CompletionCallbackWithProgress<Int>?
    ) {
        CloudServiceManager.getInstance().getAiServiceManager().downloadModule(sourcePath, modelID, fileMd5, listener)
    }

    /**
     * 上传模型
     */
    fun uploadModule(
        modelID: String,
        fileMd5: String,
        listener: CommonCallbacks.UpLoadCallbackWithProgress<Int>?
    ) {
        CloudServiceManager.getInstance().getAiServiceManager().uploadModule(modelID, fileMd5, listener)
    }

    /**
     * 上传模型
     */
    fun uploadOfflineModule(
        file: File,
        listener: CommonCallbacks.UpLoadCallbackWithProgress<Int>
    ) {
        CloudServiceManager.getInstance().getAiServiceManager().uploadExtendedDetectFile(file, listener)
    }

    /**
     * 设置图传模式
     */
    fun setDownloadSeedMode(highSpeed: HighSpeedEnum) {
        AutelLog.i(TAG, "setDownloadSeedMode -> highSpeed=$highSpeed")
        val keyManager = getKeyManager()
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            AutelLog.i(TAG, "setDownloadSeedMode -> error=$throwable")
        }) {
            val key = KeyTools.createKey(AirLinkKey.KeyAirlinkControlHighSpeed)
            keyManager?.let { KeyManagerCoroutineWrapper.performAction(it, key, highSpeed) }
            AutelLog.i(TAG, "setDownloadSeedMode -> success")
        }
    }

    /**
     * 获取图传模式
     */
    fun getDownloadSeedMode() {
        AutelLog.i(TAG, "getDownloadSeedMode ->")
        val keyManager = getKeyManager()
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            AutelLog.i(TAG, "getDownloadSeedMode -> error=$throwable")
        }) {
            val key = KeyTools.createKey(AirLinkKey.KeyAirlinkGetHighSpeed)
            keyManager?.let {
                val result = KeyManagerCoroutineWrapper.performAction(it, key)
                AutelLog.i(TAG, "getDownloadSeedMode -> result=$result")
                if (_highSpeedState.value == null || _highSpeedState.value != result) {
                    _highSpeedState.value = result
                }
            }
        }
    }


    /**
     * 获取离线模型
     */
    fun getOfflineAiModelList(onSuccess: (List<AiModelBean>) -> Unit, onError: (Boolean) -> Unit) {
        viewModelScope.launch(CoroutineExceptionHandler { _, e ->
            onError.invoke(false)
            AutelLog.e(TAG, "getOfflineAiModelList -> e=$e")
        }) {
            try {
                val aiModeList = ArrayList<AiModelBean>()
                val apiResult: Pair<String?, List<OfflineAiModule>> =
                    CloudServiceManager.getInstance().getAiServiceManager().queryLocalModuleByType(true)
                AutelLog.i(TAG, "getOfflineAiModelList -> first=${apiResult.first} second=${apiResult.second}")
                val list = apiResult.second
                if (list.isNotEmpty()) {
                    for (x in list.indices) {
                        val item = list[x]
                        val modelBean = AiModelBean()
                        modelBean.projectName = item.moduleName
                        modelBean.modelId = getOfflineAiModeId(item.moduleName)
                        modelBean.url = item.filePath
                        modelBean.index = x
                        //这里都是存在的，因此全部是待上传的状态
                        modelBean.state = AiModelState.UPLOAD_WAIT
                        aiModeList.add(modelBean)
                    }
                }
                onSuccess.invoke(aiModeList)
            } catch (e: Exception) {
                AutelLog.e(TAG, "getOfflineAiModelList -> e=$e")
                onError.invoke(false)
            }
        }
    }

    /**
     * 解析模型id
     * 202308041614-V1-103.tar.gz 离线模型命名必须按照该格式
     */
    private fun getOfflineAiModeId(moduleName: String): String {
        if (TextUtils.isEmpty(moduleName)) return ""
        val name = moduleName.replace(FileTypeEnum.AI_MODE.tag, "")
        val list = name.split("-")
        if (list.isNotEmpty() && list.size == 3) {
            return list[2]
        }
        return ""
    }

    /**
     * 删除模型
     */
    fun deleteAiMode(path: String): Boolean {
        return try {
            val file = File(path)
            AutelLog.e(TAG, "deleteAiMode -> path$path exists=${file.exists()}")
            if (file.exists()) {
                file.delete()
            } else {
                true
            }
        } catch (e: Exception) {
            AutelLog.e(TAG, "deleteAiMode -> e$e")
            false
        }
    }

}