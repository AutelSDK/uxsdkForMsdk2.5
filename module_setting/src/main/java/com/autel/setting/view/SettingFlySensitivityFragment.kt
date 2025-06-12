package com.autel.setting.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.autel.common.base.BaseAircraftFragment
import com.autel.common.utils.DeviceUtils
import com.autel.log.AutelLog
import com.autel.setting.R
import com.autel.setting.business.SettingFlyControllerVM
import com.autel.setting.databinding.SettingFlySensitivityFragmentBinding

/**
 * @Author create by LJ
 * @Date 2022/10/20 20
 * 灵敏度设置
 */
class SettingFlySensitivityFragment : BaseAircraftFragment() {

    companion object {
        const val TAG = "SettingFlySensitivityFragment"
    }

    lateinit var rootView: SettingFlySensitivityFragmentBinding
    private val settingFlyControllerVM: SettingFlyControllerVM by viewModels()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        rootView = SettingFlySensitivityFragmentBinding.inflate(inflater, container, false)
        initView()
        return rootView.root
    }

    private fun initView() {
            rootView.cssvYawPosture.setProgressMargin(resources.getDimensionPixelSize(R.dimen.common_15dp),
                resources.getDimensionPixelSize(R.dimen.common_30dp),
                resources.getDimensionPixelSize(R.dimen.common_20dp),
               0)
            rootView.cssvYawTrip.setProgressMargin(resources.getDimensionPixelSize(R.dimen.common_15dp),
                resources.getDimensionPixelSize(R.dimen.common_20dp),
                resources.getDimensionPixelSize(R.dimen.common_20dp),
               0)
            rootView.cssvYawBrake.setProgressMargin(resources.getDimensionPixelSize(R.dimen.common_15dp),
                resources.getDimensionPixelSize(R.dimen.common_20dp),
                resources.getDimensionPixelSize(R.dimen.common_20dp),
               0)

    }

    override fun onVisible() {
        super.onVisible()
        if (!DeviceUtils.isSingleControlDroneConnected()) {
            showToast(R.string.common_text_aircraft_disconnect)
            return
        }
    }

    override fun getData() {
        settingFlyControllerVM.getSensitivityMsg(0, { value ->
            rootView.cssvYawTrip.setProgress(value.toInt())
        }, { AutelLog.i(TAG, "get YawTrip onError$it") })
        settingFlyControllerVM.getSensitivityMsg(1, { value ->
            rootView.cssvYawPosture.setProgress(value.toInt())
        }, {
            AutelLog.i(TAG, "setting Attitude onError$it")
        })
        settingFlyControllerVM.getSensitivityMsg(2, { value ->
            rootView.cssvYawBrake.setProgress(value.toInt())
        }, {
            AutelLog.i(TAG, "setting Brake onError$it")
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        var tripProgress = rootView.cssvYawTrip.curProgress
        var postureProgress = rootView.cssvYawPosture.curProgress
        var brakeProgress = rootView.cssvYawBrake.curProgress
        rootView.cssvYawTrip.setOnSeekBarStoppedListener {
            settingFlyControllerVM.setSensitivityMsg(0, it, {
                AutelLog.i(TAG, "setting Attitude onSuccess")
                tripProgress = rootView.cssvYawTrip.curProgress
            }, { e ->
                AutelLog.i(TAG, "setting Attitude onError$e")
                e.message?.let { msg ->
                    showToast(msg)
                }
                rootView.cssvYawTrip.setProgress(tripProgress)
            })

        }
        rootView.cssvYawPosture.setOnSeekBarStoppedListener {
            settingFlyControllerVM.setSensitivityMsg(1, it, {
                AutelLog.i(TAG, "setting YawTrip onSuccess")
                postureProgress = rootView.cssvYawPosture.curProgress
            }, { e ->
                AutelLog.i(TAG, "setting YawTrip onError$e")
                e.message?.let { msg ->
                    showToast(msg)
                }
                rootView.cssvYawPosture.setProgress(postureProgress)
            })
        }

        rootView.cssvYawBrake.setOnSeekBarStoppedListener {
            settingFlyControllerVM.setSensitivityMsg(2, it, {
                AutelLog.i(TAG, "setting Brake onSuccess")
                brakeProgress = rootView.cssvYawBrake.curProgress
            }, { e ->
                AutelLog.i(TAG, "setting Brake onError$e")
                e.message?.let { msg ->
                    showToast(msg)
                }
                rootView.cssvYawBrake.setProgress(brakeProgress)
            })
        }


    }


    override fun addListen() {

    }

}