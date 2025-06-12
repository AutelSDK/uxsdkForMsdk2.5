package com.autel.setting.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.autel.common.base.BaseAircraftFragment
import com.autel.common.constant.AppTagConst.RtkTag
import com.autel.common.manager.AppInfoManager
import com.autel.common.manager.StorageKey
import com.autel.common.utils.DeviceUtils
import com.autel.common.utils.TransformUtils
import com.autel.common.utils.UIUtils
import com.autel.common.widget.dialog.CommonTwoButtonDialog
import com.autel.drone.sdk.libbase.error.IAutelCode
import com.autel.drone.sdk.vmodelx.interfaces.IRTKManager
import com.autel.drone.sdk.vmodelx.interfaces.RTKLoginStatusEnum
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.enums.DroneGpsEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.rtk.enums.RTKPositionTypeEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.rtk.enums.RTKSignalEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.rtk.enums.RTKSignalModeEnum
import com.autel.log.AutelLog
import com.autel.setting.R
import com.autel.setting.business.SettingRtkVM
import com.autel.setting.databinding.SettingRtkFragmentNewBinding
import com.autel.setting.intent.HistoricalAccountIntent
import com.autel.setting.state.SwitchStateVM
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.util.*
import kotlin.collections.ArrayList

/**
 * @Author create by LJ
 * @Date 2022/9/1 10:38
 *
 * RTK设置
 */
class SettingRTKFragment : BaseAircraftFragment() {

    lateinit var rtkBinding: SettingRtkFragmentNewBinding

    var callback: Callback? = null

    private val settingRtkVM: SettingRtkVM by viewModels()

    private val switchVM: SwitchStateVM by activityViewModels()
    private val gnssModeStrList = ArrayList<String>()
    private var gnssModeIndex = 0//GNSS的默认选择
    private var isChangeGnssMode = false//是否切换GNSS模式

    // 更新RTK配置回调
    private var updateConfigCallback = object : IRTKManager.ChangeRTKConfigCallback {
        override fun onNeedAuterInfo(singnalEnum: RTKSignalEnum, isQianxun: Boolean) {
            when (singnalEnum) {
                RTKSignalEnum.NETWORK -> {
                    AutelLog.i(RtkTag, "need auth info, isQx:$isQianxun")
                    if (!isQianxun) {
                        activity?.runOnUiThread {
                            showRtkNotLoginStatus()
                        }
                    }
                }
                RTKSignalEnum.MOBILE_NETWORK_SERVICES -> {
                    activity?.runOnUiThread {
                        showRtkNotLoginStatus()
                    }
                }
                else -> {

                }
            }
        }

        override fun onUpdateConfigSuccess() {
            AutelLog.i(RtkTag, "onUpdateConfigSuccess")
            updateRTKLocationEnableStatus()
        }

        override fun onUpdateConfigFailure(error: IAutelCode, msg: String?) {
            updateRTKLocationEnableStatus()
            AutelLog.i(RtkTag, "onFailure:$error")
        }
    }

    /**
     * 根据当前rtk定位开关状态显示UI
     * */
    private fun updateRTKLocationEnableStatus() {
        rtkBinding.rtkCisLocation.isEnabled = true
        val isRtkLocationOpen = settingRtkVM.isRTKLocationEnable()
        AutelLog.i(RtkTag, "updateRTKLocationEnableStatus isRtkLocationOpen:$isRtkLocationOpen")
        rtkBinding.rtkCisLocation.setCheckedWithoutListener(isRtkLocationOpen)
        rtkBinding.clRtkInfo.isVisible = isRtkLocationOpen
        rtkBinding.clRtkSta.isVisible = isRtkLocationOpen
        rtkBinding.clRtkReportInf.isVisible = isRtkLocationOpen
        if (!isRtkLocationOpen) {
            setLoginButtonText(settingRtkVM.getRTKLoginState())
            hideRtkConnectStatus()
        } else {
            setLoginButtonText(settingRtkVM.getRTKLoginState())
            updateRTKConnectStatus(settingRtkVM.getRTKLoginState())
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        rtkBinding = SettingRtkFragmentNewBinding.inflate(LayoutInflater.from(context))
        initView()
        return rtkBinding.root
    }

    private fun initView() {
        AutelLog.i(RtkTag, "initView settingRtkVM.getRTKLoginState() = ${settingRtkVM.getRTKLoginState()}")
        val rtkLocationValid = settingRtkVM.isRTKLocationEnable()
        rtkBinding.rtkCisLocation.setCheckedWithoutListener(rtkLocationValid)
        if (rtkLocationValid && !settingRtkVM.isRTKLocationEnable()) {
            settingRtkVM.enableRTKLocation(rtkSwitch = rtkLocationValid, callback = updateConfigCallback)
        }

        rtkBinding.clRtkInfo.isVisible = rtkLocationValid
        rtkBinding.clRtkSta.isVisible = rtkLocationValid
        rtkBinding.clRtkReportInf.isVisible = rtkLocationValid

        rtkBinding.rtkCisLocation.setOnSwitchChangeListener {
            settingRtkVM.enableRTKLocation(it, updateConfigCallback)
            if (settingRtkVM.getRTKLoginState() == RTKLoginStatusEnum.NotLoggedIn) {
                if (rtkBinding.cisAutoConnect.isChecked() && it) {
                    rtkBinding.btLoginAccount.performClick()
                }
            }
        }

        val autoConnectRtkAccountValid = settingRtkVM.autoConnectRtkAccount()
        rtkBinding.cisAutoConnect.setCheckedWithoutListener(autoConnectRtkAccountValid)
        rtkBinding.cisAutoConnect.setOnSwitchChangeListener {
            settingRtkVM.updateRtkAutoConnect(it)
            AutelLog.i(RtkTag, "cisAutoConnect $it")
        }

        if (rtkLocationValid && autoConnectRtkAccountValid) {
            if (settingRtkVM.getRTKLoginState() == RTKLoginStatusEnum.NotLoggedIn) {
                rtkBinding.btLoginAccount.performClick()
            }
        }
        if (settingRtkVM.getRTKLoginState() == RTKLoginStatusEnum.LoggedIn) {
            updateRTKLocationEnableStatus()
        }
        setAuthInfo()
        initRtkServiceWay()

        AutelLog.i(RtkTag, "LoginState(${settingRtkVM.getRTKLoginState()})")
        updateRTKConnectStatus(settingRtkVM.getRTKLoginState())
        setLoginButtonText(settingRtkVM.getRTKLoginState())

        rtkBinding.btLoginAccount.setOnClickListener {
            AutelLog.i(RtkTag, "btLoginAccount Clicked LoginState:${settingRtkVM.getRTKLoginState()}")
            if (settingRtkVM.getRTKLoginState() == RTKLoginStatusEnum.NotLoggedIn) {
                if (rtkBinding.etServerAddress.text.toString().isEmpty() || rtkBinding.etPort.text.toString().isEmpty()) {
                    AutelLog.e(RtkTag, "rtk server address or port is empty.")
                    return@setOnClickListener
                }
                AutelLog.i(RtkTag, "btLoginAccount Clicked RTKSignalEnum：${settingRtkVM.rtkSignal()}")
                if (settingRtkVM.rtkSignal() == RTKSignalEnum.NETWORK) {
                    authNetRtk()
                    setLoginButtonText(RTKLoginStatusEnum.Logging)
                } else if (settingRtkVM.rtkSignal() == RTKSignalEnum.MOBILE_NETWORK_SERVICES) {
                    authMobileNetRtk()
                    setLoginButtonText(RTKLoginStatusEnum.Logging)
                } else {
                    AutelLog.e(RtkTag, "rtk login failure, rtkSignal is ${settingRtkVM.rtkSignal()}")
                }
            } else {
                // 断开连接
                AutelLog.i(RtkTag, "closeRtk")
                disableLoginButton(true)
                settingRtkVM.closeRtk(
                    onSuccess = {
                        AutelLog.i(RtkTag, "closeRtk onSuccess")
                        lifecycleScope.launch {
                            settingRtkVM.historicalAccountIntent.send(HistoricalAccountIntent.UpdateUsingToUnuse)
                        }
                        activity?.runOnUiThread {
                            setLoginButtonText(settingRtkVM.getRTKLoginState())
                            showRtkNotLoginStatus()
                        }
                    },
                    onError = {
                        AutelLog.e(RtkTag, "closeRtk onError")
                        activity?.runOnUiThread {
                            setLoginButtonText(settingRtkVM.getRTKLoginState())
                        }
                    })
            }

        }

        rtkBinding.tvHistoricalAccount.setOnClickListener {
            switchVM.addFragment(
                SettingRTKHistoricalAccountFragment(),
                context?.resources?.getString(R.string.common_text_rtk_historical_account),
                true
            )
        }

        rtkBinding.tvFixSta.text = getString(
            R.string.common_text_rtk_directional_location,
            getString(R.string.common_text_rtk_status_none)
        )

        initGnssListView()
    }

    /**
     * 设置GNSS系统
     */
    private fun initGnssListView() {
        rtkBinding.civGnssSwitch.getSwitchBtn().isVisible = false
        rtkBinding.civGnssSwitch.getRightText().isVisible = false
        gnssModeStrList.clear()
        gnssModeStrList.add(getString(R.string.common_text_auto))
        gnssModeStrList.add(DroneGpsEnum.BDS.name)
        rtkBinding.csvCheckGnssMode.dataList = gnssModeStrList
        rtkBinding.csvCheckGnssMode.setSpinnerViewListener {index ->
            if (!isDroneConnect()) {
                showToast(R.string.common_text_aircraft_disconnect)
                rtkBinding.csvCheckGnssMode.setDefaultText(gnssModeIndex)
                return@setSpinnerViewListener
            }

            //相等的话，就不设置了
            if (gnssModeIndex == index) return@setSpinnerViewListener

            context?.let { context ->
                isChangeGnssMode = false
                CommonTwoButtonDialog(context)
                    .apply {
                        setMessage(getString(R.string.common_text_change_gnss_tips))
                        setRightBtnStr(getString(R.string.common_text_exchange))
                        setRightBtnListener {
                            isChangeGnssMode = true
                            // GPS模式 AUTO
                            val mode = RTKSignalModeEnum.findEnum(index)
                            AutelLog.i(RtkTag,"initGnssListView -> setGNSSMode index=$index mode=$mode")
                            settingRtkVM.setGNSSMode(mode, onSuccess = {
                                AutelLog.i(RtkTag,"initGnssListView -> setGNSSMode success")
                                gnssModeIndex = index
                            }, onError = {
                                AutelLog.i(RtkTag,"initGnssListView -> setGNSSMode error=$it")
                                showToast(R.string.common_text_set_failed)
                                rtkBinding.csvCheckGnssMode.setDefaultText(gnssModeIndex)
                            })
                        }

                        setOnDismissListener {
                            if (!isChangeGnssMode) rtkBinding.csvCheckGnssMode.setDefaultText(gnssModeIndex)
                        }
                        show()
                    }
            }
        }
    }

    private fun authNetRtk() {
        if (!isAccountValid()) {
            AutelLog.e(RtkTag, "authNetRtk account is invalid.")
            return
        }
        settingRtkVM.authNetRtk(
            rtkBinding.etServerAddress.text.toString(),
            rtkBinding.etPort.text.toString().toInt(),
            rtkBinding.etAccount.text.toString(),
            rtkBinding.etPassword.text.toString(),
            rtkBinding.etMountPoint.text.toString(),
            false, object : IRTKManager.RTKAuthoCallback {
                override fun onRtkAuthorSuccess() {
                    AutelLog.i(RtkTag, "rtk login success.")
                    activity?.runOnUiThread {
                        showRtkConnectLocatingStatus()
                        // 登录成功，按钮改成断开连接
                        setLoginButtonText(RTKLoginStatusEnum.LoggedIn)
                        // 登录成功，存储账号
                        saveServiceInfo()
                        updateRTKLocationEnableStatus()
                    }
                }

                override fun onFailure(code: IAutelCode, msg: String?) {
                    AutelLog.e(RtkTag, "rtk login failure.$code msg:$msg")
                    activity?.runOnUiThread {
                        setLoginButtonText(settingRtkVM.getRTKLoginState())
                        showRtkConnectFailStatus()
                        updateRTKLocationEnableStatus()
                    }
                }
            })
    }

    private fun authMobileNetRtk() {
        if (!isAccountValid()) {
            AutelLog.e(RtkTag, "authMobileNetRtk account is invalid.")
            return
        }
        settingRtkVM.authMobileServiceRtk(
            rtkBinding.etServerAddress.text.toString(),
            rtkBinding.etPort.text.toString().toInt(),
            rtkBinding.etAccount.text.toString(),
            rtkBinding.etPassword.text.toString(),
            rtkBinding.etMountPoint.text.toString(), {
                AutelLog.i(RtkTag, "rtk login success.")
                activity?.runOnUiThread {
                    showRtkConnectLocatingStatus()
                    // 登录成功，按钮改成断开连接
                    setLoginButtonText(settingRtkVM.getRTKLoginState())
                    // 登录成功，存储账号
                    saveServiceInfo()
                    updateRTKLocationEnableStatus()
                }
            }, {
                AutelLog.e(RtkTag, "rtk login failure.$it")
                activity?.runOnUiThread {
                    setLoginButtonText(settingRtkVM.getRTKLoginState())
                    showRtkConnectFailStatus()
                    updateRTKLocationEnableStatus()
                }
            }
        )
    }

    /**
     * 初始化RTK机载服务方式
     */
    private fun initRtkServiceWay() {
        rtkBinding.llRtkSignalType.isVisible = AppInfoManager.isSupportOnboardRtk()
        if (AppInfoManager.isSupportOnboardRtk()) {
            services.clear()
            services.add(getString(R.string.common_text_right_radio_btn_remote_controller))
            services.add(getString(R.string.common_text_rtk_mode_4))
            rtkBinding.llRtkSignalType.updateSpinnerData(services)
            val defaultIndex = settingRtkVM.rtkSignal()?.let { getIndexByRtkService(it) }
            //如果存过则取缓存中，如果没存则取第一个
            if (defaultIndex != -1) {
                defaultIndex?.let { rtkBinding.llRtkSignalType.updateSpinnerTitleIndex(it) }
                rtkBinding.llRtkSignalType.setSpinnerSelectedListener { position ->
                    val type = getRtkServiceTypeByIndex(position)
                    AutelLog.i(RtkTag, "rtkTvService check position=$position type=$type ")
                    if (type != RTKSignalEnum.UNKNOWN) {
                        settingRtkVM.switchRTKSignalEnum(type, updateConfigCallback)
                    }
                }
            } else {
                AutelLog.e(RtkTag, "services is empty ")
            }
        }
    }

    private val services = ArrayList<String>() //RTK支持的模式


    /**
     * 通过RTK类型获取index
     */
    private fun getIndexByRtkService(type: RTKSignalEnum): Int {
        var rtkServiceTypeStr = ""
        when (type) {
            RTKSignalEnum.MOBILE_STATION -> {
                rtkServiceTypeStr = getString(R.string.common_text_rtk_mode_1)
            }

            RTKSignalEnum.NETWORK -> {
                rtkServiceTypeStr = getString(R.string.common_text_right_radio_btn_remote_controller)
            }

            RTKSignalEnum.SELF_NETWORK -> {
                rtkServiceTypeStr = getString(R.string.common_text_rtk_mode_3)
            }

            RTKSignalEnum.MOBILE_NETWORK_SERVICES -> {
                rtkServiceTypeStr = getString(R.string.common_text_rtk_mode_4)
            }
            else -> {
                rtkServiceTypeStr = ""
            }
        }
        for (x in services.indices) {
            if (services[x] == rtkServiceTypeStr) return x
        }
        return if (services.isEmpty()) -1 else 0
    }

    /**
     * 通过index获取RTK类型
     */
    private fun getRtkServiceTypeByIndex(position: Int): RTKSignalEnum {
        if (position >= services.size) return RTKSignalEnum.UNKNOWN
        return when (services[position]) {
            getString(R.string.common_text_rtk_mode_1) -> {
                RTKSignalEnum.MOBILE_STATION
            }

            getString(R.string.common_text_right_radio_btn_remote_controller) -> {
                RTKSignalEnum.NETWORK
            }

            getString(R.string.common_text_rtk_mode_3) -> {
                RTKSignalEnum.SELF_NETWORK
            }

            getString(R.string.common_text_rtk_mode_4) -> {
                RTKSignalEnum.MOBILE_NETWORK_SERVICES
            }

            else -> RTKSignalEnum.UNKNOWN
        }
    }

    /**
     * 显示上次登录的账号
     */
    private fun setAuthInfo() {
        val serviceAddress = settingRtkVM.loginHistoryAccountStringValue(StorageKey.PlainKey.KEY_RTK_SERVICE_ADDR)
        if (serviceAddress.isNotEmpty()) {
            rtkBinding.etServerAddress.setText(serviceAddress)
        }

        val port = settingRtkVM.loginHistoryAccountStringValue(StorageKey.PlainKey.KEY_RTK_PORT)
        if (port.isNotEmpty()) {
            rtkBinding.etPort.setText(port)
        }

        val userName = settingRtkVM.loginHistoryAccountStringValue(StorageKey.PlainKey.KEY_RTK_USER_NAME)
        if (userName.isNotEmpty()) {
            rtkBinding.etAccount.setText(userName)
        }

        val password = settingRtkVM.loginHistoryAccountStringValue(StorageKey.PlainKey.KEY_RTK_PASSWORD)
        if (password.isNotEmpty()) {
            rtkBinding.etPassword.setText(password)
        }

        val mountPoint = settingRtkVM.loginHistoryAccountStringValue(StorageKey.PlainKey.KEY_RTK_MOUNT_POINT)
        if (mountPoint.isNotEmpty()) {
            rtkBinding.etMountPoint.setText(mountPoint)
        }
    }

    /**
     * 账户信息是否可用
     * */
    private fun isAccountValid(): Boolean {
        val serviceAddress = rtkBinding.etServerAddress.text.toString()
        val port = rtkBinding.etPort.text.toString()
        val userName = rtkBinding.etAccount.text.toString()
        val password = rtkBinding.etPassword.text.toString()
        val mountPoint = rtkBinding.etMountPoint.text.toString()

        if (serviceAddress.isEmpty() ||
            port.isEmpty() ||
            userName.isEmpty() ||
            password.isEmpty() ||
            mountPoint.isEmpty()
        ) {
            return false
        }
        return true
    }

    /**
     * 存储服务器地址、账号、密码等信息
     */
    private fun saveServiceInfo() {
        if (isAccountValid()) {
            settingRtkVM.saveHistoricalAccount(
                DeviceUtils.singleControlDrone()?.getDeviceNumber() ?: 0,
                rtkBinding.etServerAddress.text.toString(),
                rtkBinding.etPort.text.toString(),
                rtkBinding.etAccount.text.toString(),
                rtkBinding.etPassword.text.toString(),
                rtkBinding.etMountPoint.text.toString()
            )
            settingRtkVM.saveHistoryAccountStringValue(StorageKey.PlainKey.KEY_RTK_SERVICE_ADDR, rtkBinding.etServerAddress.text.toString())
            settingRtkVM.saveHistoryAccountStringValue(StorageKey.PlainKey.KEY_RTK_PORT, rtkBinding.etPort.text.toString())
            settingRtkVM.saveHistoryAccountStringValue(StorageKey.PlainKey.KEY_RTK_USER_NAME, rtkBinding.etAccount.text.toString())
            settingRtkVM.saveHistoryAccountStringValue(StorageKey.PlainKey.KEY_RTK_PASSWORD, rtkBinding.etPassword.text.toString())
            settingRtkVM.saveHistoryAccountStringValue(StorageKey.PlainKey.KEY_RTK_MOUNT_POINT, rtkBinding.etMountPoint.text.toString())
        } else {
            AutelLog.e(
                RtkTag,
                "saveServiceInfo failed, account is invalid. etServerAddress:${rtkBinding.etServerAddress.text} etPort:${rtkBinding.etPort.text} etAccount:${rtkBinding.etAccount.text} etPassword:${rtkBinding.etPassword.text} etMountPoint:${rtkBinding.etMountPoint.text}"
            )
        }

    }


    override fun getData() {
        //如果是不支持基线版本，并且rtk为非网络RTK
        if (!AppInfoManager.isSupportOnboardRtk()) {
            val signal = settingRtkVM.rtkSignal()
            if (signal != RTKSignalEnum.NETWORK) {
                settingRtkVM.switchRTKSignalEnum(RTKSignalEnum.NETWORK, updateConfigCallback)
            }
        }

        AutelLog.i(RtkTag,"queryGNSSMode -> ")
        settingRtkVM.queryGNSSMode(onSuccess = {
            AutelLog.i(RtkTag,"queryGNSSMode -> result=$it")
            gnssModeIndex = it.value
            rtkBinding.csvCheckGnssMode.setDefaultText(gnssModeIndex)
        }, onError = {
            AutelLog.i(RtkTag,"queryGNSSMode -> onError=$it")
        })
    }

    /**
     * 初始化观察rtk上报信息
     */
    override fun addListen() {
        settingRtkVM.rtkReportBean.observe(this) {
            it?.let {
//                AutelLog.i(RtkTag, "rtkReportBean:${it}")
                val format = DecimalFormat.getInstance(Locale.ENGLISH) as DecimalFormat
                // 坐标系
                rtkBinding.tvCoordinateSystem.text = it.coordinateSys.toString()
                // 经度 纬度 高度
                format.applyPattern("0.0000000")
                val lon = format.format(it.lon / 10000000)
                rtkBinding.tvLon.text = lon
                val lat = format.format(it.lat / 10000000)
                rtkBinding.tvLat.text = lat
                format.applyPattern("0.000")
                val hgt = format.format(it.hgt / 10000000)
                rtkBinding.tvHgt.text = hgt

                // 星数
                rtkBinding.tvGps.text = "${it.gpsCnt}"
                rtkBinding.tvBaidou.text = "${it.beidouCnt}"
                rtkBinding.tvGlo.text = "${it.glonassCnt}"
                rtkBinding.tvGal.text = "${it.galileoCnt}"

                // 标准差
                rtkBinding.tvLatSd.text = getString(
                    R.string.common_text_rtk_lat_standard_deviation,
                    TransformUtils.getDistanceValueWithmNoRound(it.latSigma.toDouble())
                )
                rtkBinding.tvLngSd.text = getString(
                    R.string.common_text_rtk_lng_standard_deviation,
                    TransformUtils.getDistanceValueWithmNoRound(it.lonSigma.toDouble())
                )
                rtkBinding.tvAltSd.text = getString(
                    R.string.common_text_rtk_alt_standard_deviation,
                    TransformUtils.getDistanceValueWithmNoRound(it.hgtSigma.toDouble())
                )

                val fixText: String
                when (it.fixSta) {
                    // 0 -- 未fix
                    0 -> {
                        when (it.posType) {
                            RTKPositionTypeEnum.UNRECOGNIZED,
                            RTKPositionTypeEnum.UNKNOWN_POSITION -> {
                                fixText = getString(
                                    R.string.common_text_rtk_directional_location,
                                    getString(R.string.common_text_rtk_status_none)
                                )
                            }

                            RTKPositionTypeEnum.SINGLE_POINT -> {
                                fixText = getString(
                                    R.string.common_text_rtk_directional_location,
                                    getString(R.string.common_text_rtk_status_single)
                                )
                            }

                            RTKPositionTypeEnum.PSEUDORANGE,
                            RTKPositionTypeEnum.SBAS,
                            RTKPositionTypeEnum.L1_FLOAT,
                            RTKPositionTypeEnum.IONOSPHERIC_FLOAT,
                            RTKPositionTypeEnum.NARROW_FLOAT,
                            RTKPositionTypeEnum.INERTIAL_NAVIGATION,
                            RTKPositionTypeEnum.INERTIAL_SINGLE,
                            RTKPositionTypeEnum.INERTIAL_CARRIER_FLOAT,
                            RTKPositionTypeEnum.INERTIAL_CARRIER,
                            RTKPositionTypeEnum.PPP_FLOAT,
                            RTKPositionTypeEnum.PPP_FIX,-> {
                                fixText = getString(
                                    R.string.common_text_rtk_directional_location,
                                    getString(R.string.common_text_rtk_status_float)
                                )
                            }

                            else -> {
                                fixText = getString(
                                    R.string.common_text_rtk_directional_location,
                                    getString(R.string.common_text_rtk_status_fix)
                                )
                            }
                        }
                        showRtkConnectLocatingStatus()
                        setLoginButtonText(settingRtkVM.getRTKLoginState())
                    }
                    // 1 -- 已fix
                    1 -> {
                        fixText = getString(
                            R.string.common_text_rtk_directional_location,
                            getString(R.string.common_text_rtk_status_fix)
                        )
                        showRtkConnectLocationSuccessStatus()
                        setLoginButtonText(settingRtkVM.getRTKLoginState())
                    }

                    else -> {
                        fixText = getString(
                            R.string.common_text_rtk_directional_location,
                            getString(R.string.common_text_rtk_status_none)
                        )
                        showRtkConnectLocatingStatus()
                        setLoginButtonText(settingRtkVM.getRTKLoginState())
                    }
                }

                rtkBinding.tvFixSta.text = fixText
            }
        }
    }

    private fun updateRTKConnectStatus(status: RTKLoginStatusEnum) {
        when (status) {
            RTKLoginStatusEnum.NotLoggedIn -> {
                showRtkNotLoginStatus()
            }
            RTKLoginStatusEnum.LoggedIn -> {
                showRtkConnectLocatingStatus()
            }
            RTKLoginStatusEnum.Logging -> {
                showRtkConnectLoginStatus()
            }
        }
    }

    /**
     * 更新连接状态为已连接定位中
     * */
    private fun showRtkConnectLocatingStatus() {
        val rtkLoginState = settingRtkVM.getRTKLoginState()
        if (rtkLoginState == RTKLoginStatusEnum.LoggedIn) {
            if (isContextValid()) {
                rtkBinding.rtkStatus.visibility = View.VISIBLE
                rtkBinding.rtkStatus.setText(R.string.common_text_rtk_connect_locating)
                rtkBinding.rtkStatus.setTextColor(resources.getColor(R.color.common_color_FF6D00))
            }
        } else {
            showRtkNotLoginStatus()
        }
    }

    /**
     * 更新连接状态为已连接定位成功
     */
    private fun showRtkConnectLocationSuccessStatus() {
        val rtkLoginState = settingRtkVM.getRTKLoginState()
        if (rtkLoginState == RTKLoginStatusEnum.LoggedIn) {
            if (isContextValid()) {
                rtkBinding.rtkStatus.visibility = View.VISIBLE
                rtkBinding.rtkStatus.setText(R.string.common_text_rtk_connect_locate_succ)
                rtkBinding.rtkStatus.setTextColor(resources.getColor(R.color.common_battery_setting_safe))
            }
        }
    }

    /**
     * 更新连接状态为连接中
     */
    private fun showRtkConnectLoginStatus() {
        val rtkLoginState = settingRtkVM.getRTKLoginState()
        if (rtkLoginState == RTKLoginStatusEnum.LoggedIn) {
            if (isContextValid()) {
                rtkBinding.rtkStatus.visibility = View.VISIBLE
                rtkBinding.rtkStatus.setText(R.string.common_text_toast_loading_login)
                rtkBinding.rtkStatus.setTextColor(resources.getColor(R.color.white))
            }
        }
    }

    /**
     * 更新连接状态为连接失败
     */
    private fun showRtkConnectFailStatus() {
        AutelLog.i(RtkTag, "showRtkConnectFailStatus")
        if (isContextValid()) {
            rtkBinding.rtkStatus.visibility = View.VISIBLE
            rtkBinding.rtkStatus.setText(R.string.common_text_rtk_connect_failed)
            rtkBinding.rtkStatus.setTextColor(resources.getColor(R.color.common_battery_setting_critical))
        }
    }

    /**
     * 更新连接状态为未登录
     */
    private fun showRtkNotLoginStatus() {
        AutelLog.i(RtkTag, "showRtkNotLoginStatus")
        if (isContextValid()) {
            rtkBinding.rtkStatus.visibility = View.VISIBLE
            rtkBinding.rtkStatus.setText(R.string.common_text_not_logged_in)
            rtkBinding.rtkStatus.setTextColor(resources.getColor(R.color.white))
        }
    }

    /**
     * 更新连接状态为未连接
     */
    private fun hideRtkConnectStatus() {
        if (isContextValid()) {
            rtkBinding.rtkStatus.visibility = View.INVISIBLE
            rtkBinding.rtkStatus.setText(R.string.common_text_rtk_connect_failed)
            rtkBinding.rtkStatus.setTextColor(resources.getColor(R.color.common_battery_setting_critical))
        }
    }

    /**
     * fragment依附的context是否已销毁
     * */
    private fun isContextValid(): Boolean {
        return isAdded && !isDetached && !isRemoving
    }

    /**
     * 设置EditText是否可编辑
     */
    private fun setEditTextEnable(et: EditText, mode: Boolean) {
        et.isFocusable = mode
        et.isFocusableInTouchMode = mode
    }

    /**
     * 设置按钮按钮显示和EditText是否可编辑
     */
    private fun setLoginButtonText(loginStatus: RTKLoginStatusEnum) {
        if (isContextValid()) {
            when (loginStatus) {
                RTKLoginStatusEnum.LoggedIn -> {
                    // 登录成功时修改按钮显示
                    disableLoginButton(false)
                    AutelLog.i(RtkTag, "setLoginButtonText RTKLoginStatusEnum.LoggedIn")
                    rtkBinding.btLoginAccount.text = getString(R.string.common_text_rtk_disconnect)
                    rtkBinding.btLoginAccount.setTextColor(resources.getColor(R.color.common_color_cc_ff))
                    rtkBinding.btLoginAccount.background = UIUtils.getDrawable(R.drawable.setting_rtk_black_button_bg)
                    // 登录成功时设置editText不可编辑
                    setEditTextEnable(rtkBinding.etServerAddress, false)
                    setEditTextEnable(rtkBinding.etPort, false)
                    setEditTextEnable(rtkBinding.etAccount, false)
                    setEditTextEnable(rtkBinding.etPassword, false)
                    setEditTextEnable(rtkBinding.etMountPoint, false)
                }
                RTKLoginStatusEnum.Logging -> {
                    disableLoginButton(true)
                    AutelLog.i(RtkTag, "setLoginButtonText RTKLoginStatusEnum.Logging")
                    rtkBinding.btLoginAccount.text = getString(R.string.common_text_rtk_disconnect)
                    rtkBinding.btLoginAccount.setTextColor(resources.getColor(R.color.common_color_cc_ff))
                    rtkBinding.btLoginAccount.background = UIUtils.getDrawable(R.drawable.setting_rtk_black_button_bg)
                    // 登录成功时设置editText不可编辑
                    setEditTextEnable(rtkBinding.etServerAddress, false)
                    setEditTextEnable(rtkBinding.etPort, false)
                    setEditTextEnable(rtkBinding.etAccount, false)
                    setEditTextEnable(rtkBinding.etPassword, false)
                    setEditTextEnable(rtkBinding.etMountPoint, false)
                }
                else -> {
                    // 断开连接时调用
                    disableLoginButton(false)
                    AutelLog.i(RtkTag, "setLoginButtonText RTKLoginStatusEnum.NotLoggedIn")
                    rtkBinding.btLoginAccount.text = getString(R.string.common_text_rtk_login_account)
                    rtkBinding.btLoginAccount.setTextColor(resources.getColor(R.color.common_color_00_A1))
                    rtkBinding.btLoginAccount.background = UIUtils.getDrawable(R.drawable.setting_rtk_white_button_bg)
                    setEditTextEnable(rtkBinding.etServerAddress, true)
                    setEditTextEnable(rtkBinding.etPort, true)
                    setEditTextEnable(rtkBinding.etAccount, true)
                    setEditTextEnable(rtkBinding.etPassword, true)
                    setEditTextEnable(rtkBinding.etMountPoint, true)
                }
            }
        }
    }

    private fun disableLoginButton(disable: Boolean) {
        rtkBinding.btLoginAccount.isEnabled = !disable
        AutelLog.i(RtkTag, "disableLoginButton isEnabled:${!disable}")
    }

    /**
     * 接口回调
     */
    interface Callback {
        fun openAccountPage()
    }

    /**
     * 飞机是否连接
     */
    private fun isDroneConnect(): Boolean {
        return DeviceManager.getDeviceManager().getFirstDroneDevice()?.isConnected() ?: false
    }
}