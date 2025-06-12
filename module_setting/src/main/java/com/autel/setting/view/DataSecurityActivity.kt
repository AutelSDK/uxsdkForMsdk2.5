package com.autel.setting.view

import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.viewModels
import com.autel.common.base.BaseAircraftActivity
import com.autel.common.constant.AppTagConst
import com.autel.common.constant.StringConstants
import com.autel.common.widget.dialog.CommonTwoButtonDialog
import com.autel.drone.sdk.libbase.error.IAutelCode
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.drone.sdk.vmodelx.manager.keyvalue.callback.CommonCallbacks
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.harddatasecurity.bean.UavDataDestroyListBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.harddatasecurity.bean.UavDataEncryptionBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.harddatasecurity.bean.UavDataGenerationLimitBean
import com.autel.log.AutelLog
import com.autel.setting.R
import com.autel.setting.business.DataSecurityVM
import com.autel.setting.databinding.SettingActivityDataSecurityBinding
import com.autel.setting.dialog.AircraftFrequencyPasswordDialog
import com.autel.setting.dialog.ClearDataProgressDialog

/**
 * Created by  2023/12/14
 * 数据安全
 */
class DataSecurityActivity : BaseAircraftActivity() {

    private lateinit var binding: SettingActivityDataSecurityBinding
    private val dataSecurityVM: DataSecurityVM by viewModels()
    private var droneDevice: IAutelDroneDevice? = null

    companion object {
        private const val DATA_SECURITY_CLOSE = 0 //关闭

        private const val DATA_SECURITY_OPEN = 1 //开启
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SettingActivityDataSecurityBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)
        val droneDeviceId = intent.getIntExtra(StringConstants.ARGS_DRONE_DEVICE_ID, -1)
        droneDevice = DeviceManager.getDeviceManager().getDroneDeviceById(droneDeviceId)
        AutelLog.i(AppTagConst.CompassCalibrateTag, "droneDeviceId -> $droneDeviceId droneDevice -> $droneDevice")
        queryData()
        initView()
    }

    private fun queryData() {
        droneDevice?.getKeyManager()?.let {
            dataSecurityVM.getKeyLimitDataGeneration(
                it,
                callbacks = object : CommonCallbacks.CompletionCallbackWithParam<UavDataGenerationLimitBean> {
                    override fun onSuccess(t: UavDataGenerationLimitBean?) {
                        t?.let {
                            binding.cisNotGenerateTask.setCheckedWithoutListener(it.enable == DATA_SECURITY_OPEN)
                            binding.cisNotGenerateFlyData.setCheckedWithoutListener(it.limitList.getOrNull(0) == DATA_SECURITY_OPEN)
                            binding.cisNotGenerateMedia.setCheckedWithoutListener(it.limitList.getOrNull(1) == DATA_SECURITY_OPEN)
                            binding.cisNotGenerateMediaInfo.setCheckedWithoutListener(it.limitList.getOrNull(2) == DATA_SECURITY_OPEN)
                        }
                    }

                    override fun onFailure(error: IAutelCode, msg: String?) {

                    }
                })
            dataSecurityVM.getKeyUavDataDestroy(it, callbacks = object : CommonCallbacks.CompletionCallbackWithParam<UavDataDestroyListBean> {
                override fun onSuccess(t: UavDataDestroyListBean?) {
                    t?.let {
                        binding.cisClearData.setCheckedWithoutListener(it.enable == DATA_SECURITY_OPEN)
                        binding.cisAutoClearDataDisconnect.setCheckedWithoutListener(it.disConnectDestroyMode == DATA_SECURITY_OPEN)
                        binding.cisAutoClearDataWithoutReturning.setCheckedWithoutListener(it.missionNoReturnDestroyMode == DATA_SECURITY_OPEN)
                        binding.cisAutoClearMediaFile.setCheckedWithoutListener(it.destroyList.getOrNull(0) == DATA_SECURITY_OPEN)
                        binding.cisAutoClearRouteFile.setCheckedWithoutListener(it.destroyList.getOrNull(1) == DATA_SECURITY_OPEN)
                        binding.cisAutoClearUpgradeFile.setCheckedWithoutListener(it.destroyList.getOrNull(2) == DATA_SECURITY_OPEN)
                    }
                }

                override fun onFailure(error: IAutelCode, msg: String?) {

                }
            })
            dataSecurityVM.getRemoteEncryption(it, callbacks = object : CommonCallbacks.CompletionCallbackWithParam<UavDataEncryptionBean> {
                override fun onSuccess(t: UavDataEncryptionBean?) {
                    t?.let {
                        binding.cisAircraftFrequency.setCheckedWithoutListener(it.enable == DATA_SECURITY_OPEN)
                    }
                }

                override fun onFailure(error: IAutelCode, msg: String?) {

                }
            })
        }
    }

    private fun initView() {
        binding.title.setLeftIconClickListener {
            finish()
        }
        val keyManager = droneDevice?.getKeyManager() ?: return
        binding.cisAircraftFrequency.setOnSwitchChangeListener { switch ->
            //飞机对频加密
            if (switch) {
                AircraftFrequencyPasswordDialog(this).apply {
                    setOnCancelListener {
                        binding.cisAircraftFrequency.setCheckedWithoutListener(false)
                    }
                    setOnConfirmListener {
                        dataSecurityVM.updateDataEncryptionBean(keyManager, UavDataEncryptionBean(DATA_SECURITY_OPEN, it, listOf(DATA_SECURITY_OPEN)))
                    }
                    show()
                }
            } else {
                dataSecurityVM.updateDataEncryptionBean(keyManager, UavDataEncryptionBean(DATA_SECURITY_CLOSE, "", emptyList()))
            }
        }


        binding.cisMediaFileEncryption.setOnSwitchChangeListener {
            //媒体文件加密

        }


        binding.cisNotGenerateTask.setOnSwitchChangeListener {
            //不生成任务数据

        }

        binding.cisNotGenerateMedia.setOnSwitchChangeListener {
            //照片和视频

        }

        binding.cisNotGenerateMediaInfo.setOnSwitchChangeListener {
            //照片和视频详细信息

        }

        binding.cisNotGenerateFlyData.setOnSwitchChangeListener {
            //飞行数据

        }

        binding.cisClearData.setOnSwitchChangeListener {
            //清除数据

        }

        binding.cisAutoClearDataDisconnect.setOnSwitchChangeListener {
            //失联后清除数据

        }

        binding.cisAutoClearDataWithoutReturning.setOnSwitchChangeListener {
            //不返航清除数据

        }

        binding.cisAutoClearMediaFile.setOnSwitchChangeListener {
            //清除媒体文件

        }

        binding.cisAutoClearRouteFile.setOnSwitchChangeListener {
            //清除航线文件

        }

        binding.cisAutoClearUpgradeFile.setOnSwitchChangeListener {
            //清除升级文件

        }

        binding.tvClearAllAircraftData.setOnClickListener {
            //清除所有数据
            CommonTwoButtonDialog(this).apply {
                setTitle(getString(R.string.common_text_clear_all_aircraft_data))
                setMessage(getString(R.string.common_text_clear_all_aircraft_data_tip))
                setLeftBtnStr(getString(R.string.common_text_cancel))
                setRightBtnStr(getString(R.string.common_text_clear))
                setRightBtnListener {
                    ClearDataProgressDialog(context).show()
                }
                show()
            }
        }

    }


}