package com.autel.setting.business

import androidx.lifecycle.viewModelScope
import com.autel.common.base.BaseViewModel
import com.autel.common.feature.location.CountryManager
import com.autel.common.manager.AutelStorageManager
import com.autel.common.manager.StorageKey
import com.autel.common.sdk.KeyManagerCoroutineWrapper
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.CommonKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.RemoteIDKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.base.KeyTools
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.common.enums.DroneComponentEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.remoteid.bean.OperatorIdBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.remoteid.bean.RIDSetSelfIDBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.remoteid.enums.RegionEnum
import com.autel.log.AutelLog
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class RemoteIdVM : BaseViewModel() {

    private val _operatorId = MutableStateFlow<String>("")

    val operatorId: Flow<String> = _operatorId

    private val _purpose = MutableStateFlow<String>("")
    val purpose: Flow<String> = _purpose

    private val _droneSn = MutableStateFlow("")
    val droneSn: Flow<String> = _droneSn

    // 一个欧盟成员国的ISO 3166 Alpha-3代码列表
/*    private val euCountryAlpha3 = arrayOf(
        "AUT",  // 奥地利 (Austria)
        "BEL",  // 比利时 (Belgium)
        "BGR",  // 保加利亚 (Bulgaria)
        "HRV",  // 克罗地亚 (Croatia)
        "CYP",  // 塞浦路斯 (Cyprus)
        "CZE",  // 捷克共和国 (Czech Republic)
        "DNK",  // 丹麦 (Denmark)
        "EST",  // 爱沙尼亚 (Estonia)
        "FIN",  // 芬兰 (Finland)
        "FRA",  // 法国 (France)
        "DEU",  // 德国 (Germany)
        "GRC",  // 希腊 (Greece)
        "HUN",  // 匈牙利 (Hungary)
        "IRL",  // 爱尔兰 (Ireland)
        "ITA",  // 意大利 (Italy)
        "LVA",  // 拉脱维亚 (Latvia)
        "LTU",  // 立陶宛 (Lithuania)
        "LUX",  // 卢森堡 (Luxembourg)
        "MLT",  // 马耳他 (Malta)
        "NLD",  // 荷兰 (Netherlands)
        "POL",  // 波兰 (Poland)
        "PRT",  // 葡萄牙 (Portugal)
        "ROU",  // 罗马尼亚 (Romania)
        "SVK",  // 斯洛伐克 (Slovakia)
        "SVN",  // 斯洛文尼亚 (Slovenia)
        "ESP",  // 西班牙 (Spain)
        "SWE" // 瑞典 (Sweden)
    )*/

    /**
     * 设置飞手ID
     */
    fun setOperatorIdInfo(id: String, result: (Boolean) -> Unit) {
        val bean = OperatorIdBean()
        bean.operatorIdType = if (CountryManager.isJapanZone()) {
            RegionEnum.JAPAN
        } else if (CountryManager.isUsZone()) {
            RegionEnum.AMERICA
        } else {
            RegionEnum.AMERICA
        }
        bean.operatorId = id
        AutelLog.d(TAG, " set operator id info : $bean")
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            result.invoke(false)
        }) {
            val key = KeyTools.createKey(RemoteIDKey.KeyRidSetOperatorId)
            KeyManagerCoroutineWrapper.performAction(getKeyManager()!!, key, bean)
            AutelStorageManager.getPlainStorage().setStringValue(StorageKey.PlainKey.KEY_RID_PILOT_ID, id)
            _operatorId.value = id
            result.invoke(true)
        }
    }

    /**
     * 设置飞行目的
     */
    fun setPurposeOfFlight(purpose: String, result: (Boolean) -> Unit) {
        viewModelScope.launch(CoroutineExceptionHandler { _, _ ->
            result.invoke(false)
        }) {
            val key = KeyTools.createKey(RemoteIDKey.KeyRidSetSelfId)
            AutelLog.d(TAG, "set purpose of flight .")
            KeyManagerCoroutineWrapper.performAction(getKeyManager()!!, key, RIDSetSelfIDBean(selfIDMsg = purpose))
            AutelStorageManager.getPlainStorage().setStringValue(StorageKey.PlainKey.KEY_RID_PURPOSE, purpose)
            _purpose.value = purpose
            result.invoke(true)
        }
    }

    fun querySystemDevicesInfo() {
        val keyManager = getKeyManager()
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            throwable.printStackTrace()
        }) {
            val key = KeyTools.createKey(CommonKey.KeyGetDroneDevicesInfo)
            val success = keyManager?.let { KeyManagerCoroutineWrapper.performAction(it, key) }
            success?.forEach { itemBean ->
                if (itemBean.componentType == DroneComponentEnum.TARGET_FCS) {
                    _droneSn.value = itemBean.componentSN ?: ""
                }
            }
        }
    }

    fun checkEUFlyId(id: String): Int {

//        val regex = "^[A-Z]{3}[0-9a-z]{12}[a-z0-9]-[0-9a-z]{3}$"
        if (id.length < 3) {
            return -1
        }
        val country = id.substring(0, 3)
        if (!CountryManager.isInEUAlpha3(country)) {
            return -2
        }
        if (!Regex("[A-Z]{3}[0-9a-z]{12}[a-z0-9]-[0-9a-z]{3}").matches(id)) {
            AutelLog.i(TAG, "this id is not legitimate : $id")
            return -1
        }
        // 36进制对照表
        val mod36 =
            mapOf(
                "0" to 0, "1" to 1, "2" to 2, "3" to 3, "4" to 4, "5" to 5, "6" to 6, "7" to 7, "8" to 8, "9" to 9, "a" to 10,
                "b" to 11, "c" to 12, "d" to 13, "e" to 14, "f" to 15, "g" to 16, "h" to 17, "i" to 18, "j" to 19, "k" to 20,
                "l" to 21, "m" to 22, "n" to 23, "o" to 24, "p" to 25, "q" to 26, "r" to 27, "s" to 28, "t" to 29, "u" to 30,
                "v" to 31, "w" to 32, "x" to 33, "y" to 34, "z" to 35
            )
        val number = id.substring(3, 15)
        val ex = id.substring(id.length - 3)
        val checkSum = mod36[id.substring(15, 16)] ?: return -1
        val str = number + ex
        var sum = 0
        for (i in str.indices) {
            val c = str[i]
            val codePoint = mod36[c.toString()] ?: return -1
            sum += if (i % 2 == 0) {
                val doublePoint = codePoint * 2
                val first = doublePoint / 36
                val second = doublePoint % 36
                val value = first + second
                value
            } else {
                codePoint
            }
        }
        val result = (sum + checkSum) % 36
        return if (result == 0) 0 else -1
    }

}