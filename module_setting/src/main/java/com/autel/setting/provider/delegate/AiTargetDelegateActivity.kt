package com.autel.setting.provider.delegate

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.autel.common.delegate.IMainProvider
import com.autel.common.delegate.activity.AbsDelegateActivity
import com.autel.common.manager.AppInfoManager
import com.autel.common.sdk.business.DroneControlVM
import com.autel.common.widget.dialog.CommonLoadingDialog
import com.autel.common.widget.dialog.CommonTwoButtonDialog
import com.autel.common.widget.toast.AutelToast
import com.autel.log.AutelLog
import com.autel.setting.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.Timer
import java.util.TimerTask

/**
 *
 * <p> 功能描述：AI模型代理活动    <\br>

 * 详细描述：
 *
 * @Author: 龚寿生
 * @CreateDate: 2023/10/26 20:59
 * @UpdateUser: 最后一次更新者
 * @UpdateDate: 2023/10/26 20:59
 * @UpdateRemark: 最后一次更新说明
 * @Version: 1.0 版本
 */
class AiTargetDelegateActivity(delegateProvider: IMainProvider) :
    AbsDelegateActivity(delegateProvider) {

    private val TAG = "AiTargetDelegateActivity"

    private val droneControlVM: DroneControlVM =
        ViewModelProvider(delegateProvider.getMainContext() as ComponentActivity)[DroneControlVM::class.java]
    private var mUsbReceiver: BroadcastReceiver? = null
    private var loadingDialog: CommonLoadingDialog? = null
    private var aiModeDialog: CommonTwoButtonDialog? = null

    override fun onCreate() {
        AutelLog.i(TAG, "onCreate")
        if (AppInfoManager.isSupportAiCloudService()) initUsbReceiver()
    }

    /**
     * 注册U盘监听
     */
    private fun initUsbReceiver() {
        mUsbReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                AutelLog.i(TAG, "action=${intent?.action}")
                //移除
                if (intent?.action == UsbManager.ACTION_USB_DEVICE_DETACHED) {
                    aiModeDialog?.dismiss()
                } else {//检测到
                    val timer = Timer()
                    timer.schedule(object : TimerTask() {
                        override fun run() {
                            delegateProvider.getMainLifecycleOwner().lifecycleScope.launch(
                                Dispatchers.Main
                            ) {
                                dealAiModeInUCard()
                            }
                            timer.cancel()
                        }
                    }, 3000)

                }
            }
        }
        val usbDeviceStateFilter = IntentFilter()
        usbDeviceStateFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        usbDeviceStateFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        delegateProvider.getMainContext().registerReceiver(mUsbReceiver, usbDeviceStateFilter)
    }

    /**
     * 处理收到的U盘事件
     */
    private fun dealAiModeInUCard() {
        droneControlVM.checkOfflineAiMode(delegateProvider.getMainContext()) { fileList ->
            AutelLog.i(TAG, "fileList.size=${fileList?.size}")
            if (fileList?.isNotEmpty() == true) {
                val msg = String.format(
                    Locale.ENGLISH,
                    getString(R.string.common_text_ai_mode_file_tips),
                    "${fileList.size}"
                )
                aiModeDialog = CommonTwoButtonDialog(delegateProvider.getMainContext())
                    .apply {
                        setRightBtnStr(getString(R.string.common_text_import))
                        setTitle(getString(R.string.common_text_ai_mode_file_title))
                        setMessage(msg)
                        setAutoDismiss(false)
                        setLeftBtnListener {
                            dismiss()
                        }
                        setRightBtnListener {
                            loadingDialog =
                                CommonLoadingDialog.Builder(delegateProvider.getMainContext())
                                    .setMessage("")
                                    .builder()
                            loadingDialog?.show()
                            droneControlVM.importAiMode(fileList, true) {
                                delegateProvider.getMainLifecycleOwner().lifecycleScope.launch(
                                    Dispatchers.Main
                                ) {
                                    if (it) {
                                        AutelToast.normalToast(context, getString(R.string.common_text_import_ai_mode_success))
                                    } else {
                                        AutelToast.normalToast(context, getString(R.string.common_text_import_ai_mode_failure))
                                    }
                                    dismiss()
                                    loadingDialog?.dismiss()
                                }
                            }
                        }
                        show()
                    }
            }
        }
    }

    private fun getString(id: Int): String {
        return delegateProvider.getMainContext().getString(id)
    }

}