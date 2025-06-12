package com.autel.setting.business

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.autel.common.base.BaseViewModel
import com.autel.common.constant.AppTagConst.SettingTag
import com.autel.common.extension.asLiveData
import com.autel.common.model.serve.AutelResult
import com.autel.common.sdk.KeyManagerCoroutineWrapper
import com.autel.common.utils.DeviceUtils
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.CommonKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.RemoteControllerKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.base.KeyTools
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.common.bean.DroneVersionItemBean
import com.autel.log.AutelLog
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

/**
 * Created by  2022/10/24
 */
class SettingAboutVM : BaseViewModel() {
    private val _systemProfileLD = MutableLiveData<AutelResult<List<DroneVersionItemBean>?>>()
    val systemProfileLD = _systemProfileLD.asLiveData()

    fun querySystemDevicesInfo() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            AutelLog.i(SettingTag, "querySystemDevicesInfo error = ${throwable.message}")
            _systemProfileLD.value = AutelResult.Success(null)
        }) {
            //同步发起遥控器和飞机系统信息查询
            val rcDeviceInfo = async {
                kotlin.runCatching {
                    val keyRcDeviceInfo = KeyTools.createKey(RemoteControllerKey.KeyRcDeviceInfo)
                    getRemoteKeyManager()?.let { KeyManagerCoroutineWrapper.performAction(it, keyRcDeviceInfo) }
                }
            }



            val result = mutableListOf<DroneVersionItemBean>()
            var rcInfoList = mutableListOf<DroneVersionItemBean>()


            rcDeviceInfo.await().getOrNull()?.let {
                if (it.isNotEmpty()) {
                    rcInfoList = it.toMutableList()
                }
            }
            result.addAll(rcInfoList)
            if (DeviceUtils.isMainRC()) {//从遥控器请求飞机版本信息必然超时，所以不再请求
                if (DeviceUtils.singleControlDrone()?.isConnected() == true) {
                    val droneDeviceInfo = async {
                        kotlin.runCatching {
                            getKeyManager()?.let {
                                val keyGetDroneDevicesInfo = KeyTools.createKey(CommonKey.KeyGetDroneDevicesInfo)
                                KeyManagerCoroutineWrapper.performAction(it, keyGetDroneDevicesInfo) as ArrayList<DroneVersionItemBean>?
                            }
                        }
                    }
                    var droneInfoList = mutableListOf<DroneVersionItemBean>()
                    droneDeviceInfo.await().getOrNull()?.let {
                        if (it.isNotEmpty()) {
                            droneInfoList = it.toMutableList()
                            for (x in droneInfoList) {
                                x.dataSource = 1
                            }
                        }
                    }
                    result.addAll(droneInfoList)
                }
            }
            _systemProfileLD.value = AutelResult.Success(result)
        }
    }
}