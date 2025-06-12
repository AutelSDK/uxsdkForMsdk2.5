package com.autel.setting.dialog

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.autel.common.base.BaseFragment
import com.autel.common.lifecycle.LiveDataBus
import com.autel.common.lifecycle.event.RTKStatusEvent
import com.autel.common.manager.AppInfoManager
import com.autel.common.utils.BusinessType
import com.autel.common.utils.DeviceUtils
import com.autel.common.utils.TimerEventListener
import com.autel.common.utils.TimerManager
import com.autel.drone.sdk.vmodelx.device.IAutelDroneListener
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.interfaces.IControlDroneListener
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.drone.sdk.vmodelx.manager.data.ControlMode
import com.autel.log.AutelLog
import com.autel.setting.R
import com.autel.setting.databinding.SettingManagerDialogBinding
import com.autel.setting.state.SwitchState
import com.autel.setting.state.SwitchStateVM
import com.autel.setting.utils.payload.PluginsDataManager
import com.autel.setting.view.SettingAvoidingObstaclesFragment
import com.autel.setting.view.SettingControllerFragment
import com.autel.setting.view.SettingFlyBatteryFragment
import com.autel.setting.view.SettingFlyControllerFragment
import com.autel.setting.view.SettingGimbalFragment
import com.autel.setting.view.SettingHDFragment
import com.autel.setting.view.SettingMoreFragment
import com.autel.setting.view.SettingPayloadFragment
import com.autel.setting.view.SettingRTKAccountFragment
import com.autel.setting.view.SettingRTKFragment
import com.autel.setting.view.SettingThrowerFragment
import com.autel.setting.widget.RightRadioGroupView
import com.autel.setting.widget.ScaleRadioButton
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.launch

/**
 * @Author create by LJ
 * @Date 2022/9/1 13:50
 * 设置页面Dialog
 */
class SettingManagerDialog : DialogFragment() {

    private val switchVM: SwitchStateVM by activityViewModels()

    // 抛投参数
    private var throwCoroutine: Deferred<Unit>? = null

    /**
     * 抛投器状态上报超时时长
     * */
    private val throwerReportTimeOut = 2000

    private val controlDroneListener = object : IControlDroneListener {
        override fun onControlChange(mode: ControlMode, droneList: List<IAutelDroneDevice>) {
            if (mode != ControlMode.SINGLE /*&& mode != ControlMode.UNKNOWN*/) {
                dismiss()
            }
        }
    }

    private val droneListener = object : IAutelDroneListener {
        override fun onDroneChangedListener(connected: Boolean, drone: IAutelDroneDevice) {
            super.onDroneChangedListener(connected, drone)
            if (connected) {
                if (!DeviceUtils.isSingleControl()) {
                    dismiss()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (DeviceUtils.isBusinessTypeValid(BusinessType.NETMESH)) {
            DeviceManager.getMultiDeviceOperator().addControlChangeListener(controlDroneListener)
            DeviceManager.getDeviceManager().addDroneListener(droneListener)
        }
        PluginsDataManager.getInstance().init()
        setStyle(STYLE_NO_TITLE, 0)
    }

    private lateinit var rootView: SettingManagerDialogBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        rootView = SettingManagerDialogBinding.inflate(LayoutInflater.from(context))
        initView()
        lifecycleScope.launch { initObserver() }
        initData()
        LiveDataBus.of(RTKStatusEvent::class.java).isEnabled().observe(viewLifecycleOwner) {
            if (it) showRTK() else {
                if (rootView.settingTvTopTitle.text == getString(R.string.common_text_rtk_title)) {
                    clearLastFragment()
                    goToFlightControlParams()
                    rootView.settingRightRadiogroupview.check(R.id.setting_rb_fly_controll)
                    rootView.settingRightRadiogroupview.findViewById<ScaleRadioButton>(R.id.setting_rb_rtk).visibility =
                        View.GONE
                }
            }
        }
        return rootView.root
    }

    private suspend fun initObserver() {
        switchVM.switchState.collect {
            when (it) {
                is SwitchState.Nothing -> {}

                is SwitchState.addFragment -> {
                    addFragment(it.fragment, it.tag, it.boolean)
                }

                is SwitchState.switchFragment -> {
                    switchFragment(it.fragment, it.tag)
                }

                is SwitchState.setTitleText -> {
                    setTitleText(it.title)
                }

                is SwitchState.setTitle -> {
                    setTitle(it.id)
                }

                is SwitchState.dismiss -> {
                    dismiss()
                }

                is SwitchState.back -> {
                    dealBack()
                }
            }
        }
    }


    override fun onStart() {
        super.onStart()
        dialog?.let {
            it.window?.setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            it.setCanceledOnTouchOutside(true)
            it.window?.apply {
                setGravity(Gravity.END)
                setBackgroundDrawableResource(R.drawable.setting_trans_bg)
                setFlags(
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                )
                decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
            }
            iniPayload()
        }
    }

    /**
     * 初始化数据加载
     */
    private fun initData() {
        if (DeviceUtils.isDockMode()) {
            rootView.settingRightRadiogroupview.check(R.id.setting_rb_obstaclel)
            setTitle(R.string.common_text_rbobstaclel_title)
            switchFragment(
                SettingAvoidingObstaclesFragment(),
                resources.getString(R.string.common_text_rbobstaclel_title)
            )
        } else if (DeviceUtils.isMainRC()) {
            //默认选中“飞机参数控制”
            setTitle(R.string.common_text_fly_controll_title)
            switchFragment(
                SettingFlyControllerFragment(),
                context?.resources?.getString(R.string.common_text_fly_controll_title)
            )
            iniThrower()
            iniPayload()
        } else {
            //遥控器设置页面
            setTitle(R.string.common_text_remote_title)
            rootView.settingRightRadiogroupview.findViewById<ScaleRadioButton>(R.id.setting_rb_remote).isChecked =
                true
            switchFragment(
                SettingControllerFragment(),
                resources.getString(R.string.common_text_remote_title)
            )
        }

    }

    private fun showRTK() {
        val supportRTK = AppInfoManager.isSupportRtk() && DeviceUtils.isMainRC()
        val rtkConnected = DeviceUtils.singleControlDrone()?.getRtkManager()?.isConnected == true
        if (supportRTK && rtkConnected) {
            rootView.settingRightRadiogroupview.findViewById<ScaleRadioButton>(R.id.setting_rb_rtk).visibility =
                View.VISIBLE
        }
    }

    private fun dealBack() {
        childFragmentManager.popBackStackImmediate()
        if (childFragmentManager.backStackEntryCount == 1) {
            rootView.settingIvTopBack.visibility = View.GONE
        }
        if (childFragmentManager.fragments.isNotEmpty()) {
            childFragmentManager.fragments[0].tag?.let {
                setTitleText(it)
            }
        }
    }

    /**
     * 右侧table点击监听
     */
    private fun initView() {
        rootView.ivClose.setOnClickListener { dismiss() }
        rootView.clParent.setOnClickListener { dismiss() }
        rootView.settingIvTopBack.setOnClickListener { dealBack() }
        if (DeviceUtils.isDockMode()) {// 机巢模式,只保留避障
            for (view in rootView.settingRightRadiogroupview.getAllChildView()) {
                view.isVisible = view.id == R.id.setting_rb_obstaclel
            }
        } else if (DeviceUtils.isMainRC()) {
            rootView.settingRightRadiogroupview.findViewById<ScaleRadioButton>(R.id.setting_rb_thrower).isVisible =
                false
            rootView.settingRightRadiogroupview.findViewById<ScaleRadioButton>(R.id.setting_rb_obstaclel).isVisible =
                AppInfoManager.isSupportAvoidanceFunction()
        } else {
            rootView.settingRightRadiogroupview.findViewById<ScaleRadioButton>(R.id.setting_rb_fly_controll).visibility =
                View.GONE
            rootView.settingRightRadiogroupview.findViewById<ScaleRadioButton>(R.id.setting_rb_obstaclel).visibility =
                View.GONE
            rootView.settingRightRadiogroupview.findViewById<ScaleRadioButton>(R.id.setting_rb_battery).visibility =
                View.GONE
            rootView.settingRightRadiogroupview.findViewById<ScaleRadioButton>(R.id.setting_rb_camera).visibility =
                View.GONE
            rootView.settingRightRadiogroupview.findViewById<ScaleRadioButton>(R.id.setting_rb_rtk).visibility =
                View.GONE
            rootView.settingRightRadiogroupview.findViewById<ScaleRadioButton>(R.id.setting_rb_thrower).visibility =
                View.GONE
        }

        rootView.settingRightRadiogroupview.setRadioButtonClickListener(object :
            RightRadioGroupView.RadioButtonClickListener {
            override fun settingRbFlyControl() {
                //飞控设置页面
                goToFlightControlParams()
            }

            override fun settingRbObstacle() {
                //避障设置页面
                setTitle(R.string.common_text_rbobstaclel_title)
                switchFragment(
                    SettingAvoidingObstaclesFragment(),
                    resources.getString(R.string.common_text_rbobstaclel_title)
                )
            }

            override fun settingRbRemote() {
                //遥控器设置页面
                setTitle(R.string.common_text_remote_title)
                switchFragment(
                    SettingControllerFragment(),
                    resources.getString(R.string.common_text_remote_title)
                )
            }

            override fun settingRbHd() {
                //图传设置页面
                setTitle(R.string.common_text_HD_title)
                switchFragment(
                    SettingHDFragment(),
                    resources.getString(R.string.common_text_HD_title)
                )
            }

            override fun settingRbBattery() {
                //电池设置页面
                setTitle(R.string.common_text_battery_title)
                switchFragment(
                    SettingFlyBatteryFragment(),
                    resources.getString(R.string.common_text_battery_title)
                )
            }

            override fun settingRbGimbal() {
                //云台设置页面
                setTitle(R.string.common_text_gimbal_title)
                switchFragment(
                    SettingGimbalFragment(),
                    resources.getString(R.string.common_text_gimbal_title)
                )
            }

            override fun settingRbRtk() {
                //RTK设置页面
                setTitle(R.string.common_text_rtk_title)
                switchFragment(
                    SettingRTKFragment().apply {
                        callback = object : SettingRTKFragment.Callback {
                            override fun openAccountPage() {
                                settingRtkAccount()
                            }

                        }
                    },
                    resources.getString(R.string.common_text_rtk_title)
                )
            }

            /**
             * 抛投器设置
             */
            override fun settingThrower() {
                //抛投器设置页面
                setTitle(R.string.common_text_thrower_title)
                switchFragment(
                    SettingThrowerFragment(),
                    resources.getString(R.string.common_text_thrower_title)
                )
            }

            override fun settingRbMore() {
                //更多
                setTitle(R.string.common_setting_more_title)
                switchFragment(
                    SettingMoreFragment(),
                    resources.getString(R.string.common_setting_more_title)
                )
            }

            override fun settingPayload() {
                // 挂载设备
                setTitleText(
                    resources.getString(R.string.common_text_plugin_device)
                )
                switchFragment(
                    SettingPayloadFragment(),
                    resources.getString(R.string.common_text_plugin_device)
                )
            }


            fun settingRtkAccount() {
                //RTK设置页面
                setTitle(R.string.common_text_rtk_account)
                switchFragment(
                    SettingRTKAccountFragment(),
                    resources.getString(R.string.common_text_rtk_account)
                )
            }

        })
    }

    private fun goToFlightControlParams() {
        setTitle(R.string.common_text_fly_controll_title)
        switchFragment(
            SettingFlyControllerFragment(),
            resources.getString(R.string.common_text_fly_controll_title)
        )
    }

    /**
     * 切换fragment
     */
    fun switchFragment(fragment: BaseFragment, tag: String?) {
        childFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        tag?.let {
            addFragment(fragment, tag, false)
        }
    }

    /**
     * 清除之前所有的fragment
     */
    private fun clearLastFragment() {
        val count = childFragmentManager.backStackEntryCount
        if (count > 0) {
            //切换之前，清空之前所有Fragment
            for (index in 0 until count) {
                childFragmentManager.popBackStackImmediate()
            }
        }
    }

    /**
     * 添加二,三级Fragment页面
     * param:fragment 跳转到的fragment
     * tag：跳转的Fragment添加tag，注意：这个tag就是标题，请不要乱写
     * boolean：是否显示返回按钮
     */

    fun addFragment(fragment: BaseFragment, tag: String?, boolean: Boolean?) {
        boolean?.let {
            rootView.settingIvTopBack.visibility = if (it) View.VISIBLE else View.GONE
        }
        childFragmentManager.beginTransaction()
            .replace(R.id.setting_fragment_content_view, fragment, tag)
            .addToBackStack(null).commit()
        tag?.let {
            setTitleText(it)
        }
    }

    /**
     * 传字符更新标题
     */
    fun setTitleText(title: String) {
        rootView.settingTvTopTitle.text = title
    }

    /**
     * 传ID更新标题
     */
    fun setTitle(id: Int) {
        rootView.settingTvTopTitle.text = resources.getString(id)
    }

    /**
     *隐藏当前dialog
     */
    override fun dismiss() {
        dialog?.let {
            if (it.window != null) {
                it.dismiss()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (DeviceUtils.isBusinessTypeValid(BusinessType.NETMESH)) {
            DeviceManager.getMultiDeviceOperator().removeControlChangeListener(controlDroneListener)
            DeviceManager.getDeviceManager().removeDroneListener(droneListener)
        }
        switchVM.destroy()
        throwCoroutine?.cancel()
        throwCoroutine = null
        TimerManager.removeTimerEventListener(timerListener)
        PluginsDataManager.getInstance().onDestroy()
    }

    /**
     * 抛投器初始化
     */
    private fun iniThrower() {
        TimerManager.addTimerEventListener(timerListener)
    }

    private fun iniPayload() {
        val state = PluginsDataManager.getInstance().enablePSDKSetting()

        val view =
            rootView.settingRightRadiogroupview.findViewById<ScaleRadioButton>(R.id.setting_rb_payload)
        view.isVisible = state
        AutelLog.d("SettingManagerDialog", "Payload 显示状态：${state}")
    }

    private fun checkThrowerShow(isShow: Boolean) {
        val view =
            rootView.settingRightRadiogroupview.findViewById<ScaleRadioButton>(R.id.setting_rb_thrower)
        if (!isShow) {
            if (view.isChecked) {
                clearLastFragment()
                goToFlightControlParams()
                rootView.settingRightRadiogroupview.check(R.id.setting_rb_fly_controll)
                dismiss()
            }
        }
        view.isVisible = false
    }

    private var timerListener = object : TimerEventListener(this.javaClass.simpleName) {
        override fun onEventChanged() {
            if (DeviceUtils.isMainRC()) {
                val drone = DeviceUtils.singleControlDrone()
                if (drone != null) {
                    val timeInterval =
                        System.currentTimeMillis() - drone.getDeviceStateData().accessoriesData.jettisionGearStatus.timestamp

                    val timeStamp = drone.getDeviceStateData().throwMessageData?.timestamp ?: 0
                    val timeIntervalNew = System.currentTimeMillis() - timeStamp

                    if (timeInterval < throwerReportTimeOut || timeIntervalNew < throwerReportTimeOut) {
                        checkThrowerShow(true)
                    } else {
                        checkThrowerShow(false)
                    }
                } else {
                    checkThrowerShow(false)
                }
            } else {
                checkThrowerShow(false)
            }

        }
    }

}