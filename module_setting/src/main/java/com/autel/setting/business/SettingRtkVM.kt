package com.autel.setting.business

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.autel.common.base.BaseViewModel
import com.autel.common.constant.AppTagConst.RtkTag
import com.autel.common.extension.asLiveData
import com.autel.common.manager.AutelStorageManager
import com.autel.common.manager.StorageKey
import com.autel.common.sdk.KeyManagerCoroutineWrapper
import com.autel.common.utils.DeviceUtils
import com.autel.data.bean.entity.HistoricalAccountModel
import com.autel.data.database.DBManager
import com.autel.drone.sdk.libbase.error.IAutelCode
import com.autel.drone.sdk.vmodelx.interfaces.IRTKManager
import com.autel.drone.sdk.vmodelx.interfaces.RTKLoginStatusEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.callback.CommonCallbacks
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.RtkPropertKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.base.KeyTools
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.rtk.bean.RtkReportBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.rtk.enums.RTKSignalEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.rtk.enums.RTKSignalModeEnum
import com.autel.log.AutelLog
import com.autel.setting.intent.HistoricalAccountIntent
import com.autel.setting.state.HistoricalAccountState
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * com.autel.setting.business
 *
 * Copyright: Autel Robotics
 *
 * @author R22711 on 2023/4/5.
 */
class SettingRtkVM : BaseViewModel() {
    private val _rtkReportBean = MutableLiveData<RtkReportBean?>()
    val rtkReportBean = _rtkReportBean.asLiveData()

    val historicalAccountIntent = Channel<HistoricalAccountIntent>(Channel.UNLIMITED)

    private var _historicalAccountState = MutableStateFlow<HistoricalAccountState>(
        HistoricalAccountState.Loading
    )
    val historicalAccountState = _historicalAccountState

    companion object {
        const val TAG = "SettingRtkVM"
    }

    init {
        handleIntent()
    }

    override fun fixedFrequencyRefresh() {
        super.fixedFrequencyRefresh()
        _rtkReportBean.value = DeviceUtils.singleControlDrone()?.getDeviceStateData()?.rtkReportData
    }

    /**
     * 网络RTK授权
     * */
    fun authNetRtk(
        address: String,
        port: Int,
        account: String,
        password: String,
        mountPoint: String,
        isQianxun: Boolean,
        callback: IRTKManager.RTKAuthoCallback
    ) {
        DeviceUtils.singleControlDrone()?.getRtkManager()?.authNetRtk(
            address,
            port,
            account,
            password,
            mountPoint,
            isQianxun,
            callback
        )
    }

    /**
     * 设置RTK信号类型
     */
    fun switchRTKSignalEnum(rtkSignalEnum: RTKSignalEnum, callback: IRTKManager.ChangeRTKConfigCallback) {
        DeviceUtils.singleControlDrone()?.getRtkManager()?.switchRTKSignalEnum(rtkSignalEnum, callback)
    }

    /**
     * 机载RTK授权登录
     *
     * @param host
     * @param port
     * @param account
     * @param password
     * @param mountPoint
     * @param onSuccess
     * @param onError
     */
    fun authMobileServiceRtk(
        host: String, port: Int,
        account: String, password: String, mountPoint: String,
        onSuccess: () -> Unit, onError: (IAutelCode) -> Unit
    ) {
        AutelLog.i(RtkTag, "autherMobileServiceRtk host:$host, port:$port, account:$account, password:$password, mountPoint:$mountPoint")
        DeviceUtils.singleControlDrone()?.getRtkManager()?.autherMobileServiceRtk(
            host, port, account, password, mountPoint,
            object : IRTKManager.RTKAuthoCallback {
                override fun onRtkAuthorSuccess() {
                    AutelLog.i(RtkTag, "autherMobileServiceRtk onRtkAuthorSuccess")
                    onSuccess.invoke()
                }

                override fun onFailure(code: IAutelCode, msg: String?) {
                    AutelLog.i(RtkTag, "autherMobileServiceRtk onFailure code:$code, msg:$msg")
                    onError.invoke(code)
                }
            })
    }

    /**
     * 关闭rtk
     */
    fun closeRtk(onSuccess: () -> Unit, onError: () -> Unit) {
        DeviceUtils.singleControlDrone()?.getRtkManager()
            ?.closeCurrentRtk(object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    onSuccess.invoke()
                }

                override fun onFailure(code: IAutelCode, msg: String?) {
                    onError.invoke()
                }
            })
    }

    /**
     * 启用RTK定位
     */
    fun enableRTKLocation(rtkSwitch: Boolean, callback: IRTKManager.ChangeRTKConfigCallback) {
        AutelLog.i(RtkTag, "enableRTKLocation:$rtkSwitch")
        if (!rtkSwitch) {
            closeRtk({}, {})
        }
        DeviceUtils.singleControlDrone()?.getRtkManager()?.enableRTKLocation(rtkSwitch, callback)
    }

    /**
     * 是否启用RTK定位
     */
    fun isRTKLocationEnable(): Boolean {
        return DeviceUtils.singleControlDrone()?.getRtkManager()?.isenableRTKLocation() == true
    }

    /**
     * 获取RTK信号方式
     */
    fun rtkSignal(): RTKSignalEnum? {
        return DeviceUtils.singleControlDrone()?.getRtkManager()?.rtkSignalEnum
    }

    /**
     * 存储历史账号到数据库中
     */
    fun saveHistoricalAccount(deviceId: Int, serviceAddress: String, port: String, account: String, password: String, mountPoint: String) {
        AutelLog.i(
            RtkTag,
            "saveHistoricalAccount deviceId:$deviceId serviceAddress:$serviceAddress port:$port account:$account password:$password mountPoint:$mountPoint "
        )
        viewModelScope.launch(Dispatchers.IO) {
            val ham = HistoricalAccountModel()
            ham.deviceId = deviceId
            ham.serverAddr = serviceAddress
            ham.port = port
            ham.account = account
            ham.password = password
            ham.mountPoint = mountPoint
            ham.lastLoginTime = System.currentTimeMillis()
            ham.isLogining = true
            updateUsingToUnused()
            DBManager.insertHistoricalAccount(ham)
        }
    }

    /**
     * 把正在使用的账号改为未使用
     */
    private suspend fun updateUsingToUnused() {
        DBManager.updateUsingToUnuse()
    }

    private fun handleIntent() {
        viewModelScope.launch(CoroutineExceptionHandler { _, e ->
            historicalAccountState.value = HistoricalAccountState.OnError(e.message ?: "")
        } + Dispatchers.IO) {
            historicalAccountIntent.consumeAsFlow().collect {
                when (it) {
                    // 查询所有未使用账号
                    is HistoricalAccountIntent.QueryAllAccountUnused -> {
                        val list = mutableListOf<HistoricalAccountModel>()
                        _historicalAccountState.value = HistoricalAccountState.Loading
                        list.addAll(DBManager.queryAllHistoricalAccountUnused())
                        list.sortBy { accountModel -> accountModel.lastLoginTime }
                        withContext(Dispatchers.Main) {
                            if (list.isNotEmpty()) {
                                _historicalAccountState.value = HistoricalAccountState.AccountList(list)
                            } else {
                                _historicalAccountState.value = HistoricalAccountState.OnError("")
                            }
                        }
                    }

                    // 删除正在使用的账号
                    is HistoricalAccountIntent.DelAccountOnUsing -> {
                        DBManager.delAccountOnUsing()
                    }

                    // 删除一个账号
                    is HistoricalAccountIntent.DelOneAccount -> {
                        DBManager.delOneHistoricalAccount(it.ham)
                    }

                    // 把正在使用的账号改为未使用
                    is HistoricalAccountIntent.UpdateUsingToUnuse -> {
                        DBManager.updateUsingToUnuse()
                    }

                    // 存储一个账号
                    is HistoricalAccountIntent.SaveOneAccount -> {
                        saveHistoryAccountStringValue(StorageKey.PlainKey.KEY_RTK_SERVICE_ADDR, it.ham.serverAddr)
                        saveHistoryAccountStringValue(StorageKey.PlainKey.KEY_RTK_PORT, it.ham.port)
                        saveHistoryAccountStringValue(StorageKey.PlainKey.KEY_RTK_USER_NAME, it.ham.account)
                        saveHistoryAccountStringValue(StorageKey.PlainKey.KEY_RTK_PASSWORD, it.ham.password)
                        saveHistoryAccountStringValue(StorageKey.PlainKey.KEY_RTK_MOUNT_POINT, it.ham.mountPoint)

                        it.ham.lastLoginTime = System.currentTimeMillis()
                        it.ham.isLogining = true

                        updateUsingToUnused()
                        DBManager.insertHistoricalAccount(it.ham)
                    }
                }
            }
        }
    }

    /**
     * 针对组网多机下，不同飞机使用不同的账号问题,所以关联账号要绑定deviceID
     * 保存字符串型数据
     * */
    fun saveHistoryAccountStringValue(key: String, value: String) {
        val deviceId = DeviceUtils.singleControlDrone()?.getDeviceNumber()
        val deviceKey = "$deviceId" + key
        AutelLog.d(RtkTag, "saveHistoryAccountValue deviceKey:$deviceKey,value:$value")
        AutelStorageManager.getPlainStorage().setStringValue(deviceKey, value)
    }

    /**
     * 针对组网多机下，不同飞机使用不同的账号问题,所以关联账号要绑定deviceID
     * 读取字符串型数据
     * */
    fun loginHistoryAccountStringValue(key: String): String {
        val deviceId = DeviceUtils.singleControlDrone()?.getDeviceNumber()
        val deviceKey = "$deviceId" + key
        val value = AutelStorageManager.getPlainStorage().getStringValue(deviceKey, "") ?: ""
        AutelLog.d(RtkTag, "loginHistoryAccount deviceKey:$deviceKey,value:$value")
        return value
    }

    fun updateRtkAutoConnect(isAutoConnect: Boolean) {
        DeviceUtils.singleControlDrone()?.getRtkManager()?.setAutoConnectRTKAccount(isAutoConnect)
    }

    /**
     * 获取RTK登录状态
     */
    fun getRTKLoginState(): RTKLoginStatusEnum {
        return DeviceUtils.singleControlDrone()?.getRtkManager()?.loginState() ?: RTKLoginStatusEnum.NotLoggedIn
    }

    fun autoConnectRtkAccount(): Boolean {
        return DeviceUtils.singleControlDrone()?.getRtkManager()?.autoConnectRTKAccount() == true
    }

    /**
     * 查询当前GNSSModel
     */
    fun queryGNSSMode(onSuccess: (RTKSignalModeEnum) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            onError.invoke(throwable.toString())
        }) {
            getKeyManager()?.let {
                val mode = KeyManagerCoroutineWrapper.getValue(it, KeyTools.createKey(RtkPropertKey.KeyRTKSwitchMode))
                onSuccess.invoke(RTKSignalModeEnum.findEnum(mode))
            } ?: onError.invoke("keyManager is null")

        }
    }

    /**
     * 设置GNSS模式
     * @param mode RTKSignalModeEnum.ALL_SINGLE_MODE  , RTKSignalModeEnum.RTK_MODE_BDS
     */
    fun setGNSSMode(mode: RTKSignalModeEnum, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            onError.invoke(throwable.toString())
        }) {
            getKeyManager()?.let {
                KeyManagerCoroutineWrapper.setValue(it, KeyTools.createKey(RtkPropertKey.KeyRTKSwitchMode), mode.value)
                onSuccess.invoke()
            } ?: onError.invoke("keyManager is null")
        }
    }
}