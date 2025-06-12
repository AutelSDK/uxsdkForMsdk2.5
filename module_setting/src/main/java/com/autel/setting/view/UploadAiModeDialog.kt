package com.autel.setting.view

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.isVisible
import com.autel.common.bean.AiModelBean
import com.autel.common.bean.AiModelState
import com.autel.common.widget.dialog.BaseAutelDialog
import com.autel.drone.sdk.libbase.error.IAutelCode
import com.autel.drone.sdk.vmodelx.manager.keyvalue.callback.CommonCallbacks
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.alink.enums.HighSpeedEnum
import com.autel.log.AutelLog
import com.autel.setting.R
import com.autel.setting.business.SettingChooseTargetVM
import com.autel.setting.databinding.DialogUploadAiModeBinding
import com.autel.setting.infc.UploadListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.*

/**
 * @author
 * @date 2023/8/10
 * 模型上传进度弹框
 */
@SuppressLint("SetTextI18n")
class UploadAiModeDialog(context: Context, val chooseTargetVM: SettingChooseTargetVM) : BaseAutelDialog(context) {
    private val TIME_OUT_LIMIT = 30_000L//超时限制
    private val TIME_REPET = 10_00L//重复时间
    private val NUM_1024 = 1024
    private val TAG = "UploadAiModeDialog"
    private val binding = DialogUploadAiModeBinding.inflate(LayoutInflater.from(context))
    private var isOnlineAiMode = true
    private var aiModelBean: AiModelBean? = null
    private var enterTimer: Timer? = Timer()
    private var exitTimer: Timer? = Timer()
    private var checkStartTime = 0L//开启独占模式的时间
    private var checkExitTime = 0L//退出独占模式的时间
    private var isRetryStart = false//是否重试开始 重试一次就好
    private var isRetryExit = false//是否重试退出 重试一次就好
    private var curProgress = 0 //模拟进度

    private var listener: UploadListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        window?.decorView?.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        setCanceledOnTouchOutside(false)
        binding.tvCancel.setOnClickListener {
            AutelLog.i(TAG, "tvCancel -> onClick ")
            binding.tvTitle.text = context.getString(R.string.common_text_exit_high_speed)
            exitExclusiveMode()
            listener?.uploadCancel()
        }
    }

    /**
     * 上传AI模型
     */
    fun uploadAiMode(isOnlineAiMode: Boolean, bean: AiModelBean, listener: UploadListener) {
        AutelLog.i(TAG, "uploadAiMode -> isOnlineAiMode=$isOnlineAiMode $bean")
        this.listener = listener
        binding.tvName.text = bean.projectName
        binding.tvTitle.text = context.getString(R.string.common_text_enter_high_speed)
        this.isOnlineAiMode = isOnlineAiMode
        aiModelBean = bean
        checkStartTime = SystemClock.elapsedRealtime()
        curProgress = 0
        enterExclusiveMode()
    }

    /**
     * 进入独占模式
     */
    private fun enterExclusiveMode() {
        AutelLog.i(TAG, "enterExclusiveMode -> start")
        //切换独占模式
        chooseTargetVM.setDownloadSeedMode(HighSpeedEnum.EXCLUSIVE)
        //开启定时器
        enterTimer?.schedule(object : TimerTask() {
            override fun run() {
                //如果已经切换到独占模式，则直接开始上传
                if (chooseTargetVM.highSpeedState.value == HighSpeedEnum.EXCLUSIVE) {
                    GlobalScope.launch(Dispatchers.Main) {
                        dealUploadAiMode()
                    }
                } else {
                    //如果30s都没有查询到切换成功，则重试一次
                    val time = SystemClock.elapsedRealtime() - checkStartTime
                    if (time > TIME_OUT_LIMIT) {
                        if (!isRetryStart) {
                            AutelLog.i(TAG, "enterExclusiveMode -> 重试1次")
                            isRetryStart = true
                            chooseTargetVM.setDownloadSeedMode(HighSpeedEnum.EXCLUSIVE)
                        }
                    }
                    //当60s没切换成功，则返回超时
                    if (time > TIME_OUT_LIMIT * 2) {
                        GlobalScope.launch(Dispatchers.Main) {
                            dealEnterExclusiveFailure()
                        }
                    } else {
                        chooseTargetVM.getDownloadSeedMode()
                    }
                    if (curProgress < 10) {
                        GlobalScope.launch(Dispatchers.Main) {
                            binding.progressBar.progress = curProgress++
                        }
                    }
                }
            }
        }, 0, TIME_REPET)
    }

    /**
     * 退出独占模式
     */
    private fun exitExclusiveMode() {
        AutelLog.i(TAG, "exitExclusiveMode -> start")
        curProgress = 90
        checkExitTime = SystemClock.elapsedRealtime()
        binding.tvSpeed.isVisible = false
        binding.tvProgress.isVisible = false
        //开启定时器
        exitTimer?.schedule(object : TimerTask() {
            override fun run() {
                //如果已经切换到普通模式，则表示完成上传流程
                if (chooseTargetVM.highSpeedState.value == HighSpeedEnum.NORMAL) {
                    GlobalScope.launch(Dispatchers.Main) {
                        dealExitExclusiveSuccess()
                    }
                } else {
                    //如果30s都没有查询到切换成功，则重试一次
                    val time = SystemClock.elapsedRealtime() - checkExitTime
                    if (time > TIME_OUT_LIMIT) {
                        if (!isRetryExit) {
                            AutelLog.i(TAG, "exitExclusiveMode -> 重试1次")
                            isRetryExit = true
                            chooseTargetVM.setDownloadSeedMode(HighSpeedEnum.NORMAL)
                        }
                    }
                    //当60s没切换成功，则返回超时
                    if (time > TIME_OUT_LIMIT * 2) {
                        GlobalScope.launch(Dispatchers.Main) {
                            dealExitExclusiveFailure()
                        }
                    } else {
                        chooseTargetVM.getDownloadSeedMode()
                    }
                    if (curProgress < 100) {
                        GlobalScope.launch(Dispatchers.Main) {
                            binding.progressBar.progress = curProgress++
                        }
                    }
                }
            }
        }, 0, TIME_REPET)
    }

    private fun release() {
        enterTimer = null
        exitTimer = null
        dismiss()
    }

    /**
     * 开启独占模式失败
     */
    private fun dealEnterExclusiveFailure() {
        AutelLog.i(TAG, "dealEnterExclusiveFailure ->")
        enterTimer?.cancel()
        listener?.enterFailure()
        release()
    }

    /**
     * 退出独占模式失败
     */
    private fun dealExitExclusiveFailure() {
        AutelLog.i(TAG, "dealExitExclusiveFailure ->")
        exitTimer?.cancel()
        listener?.exitFailure()
        release()
    }

    /**
     * 退出独占模式成功
     */
    private fun dealExitExclusiveSuccess() {
        AutelLog.i(TAG, "dealExitExclusiveSuccess ->")
        exitTimer?.cancel()
        release()
    }

    /**
     * 处理上传流程
     */
    private fun dealUploadAiMode() {
        AutelLog.i(TAG, "dealUploadAiMode -> ")
        enterTimer?.cancel()
        aiModelBean?.state = AiModelState.UPLOAD_EXECUTE
        listener?.notifyItemChanged()
        binding.tvTitle.text = context.getString(R.string.common_text_firmware_upgrade_uploading)
        binding.progressBar.progress = 10
        binding.tvProgress.isVisible = true
        binding.tvSpeed.isVisible = true
        if (isOnlineAiMode) {
            uploadOnlineAiMode()
        } else {
            uploadOfflineAiMode()
        }
    }

    /**
     * 上传在线模型
     */
    private fun uploadOnlineAiMode() {
        aiModelBean?.let { bean ->
            AutelLog.i(TAG, "uploadOnlineAiMode -> start $bean")
            chooseTargetVM.uploadModule(bean.modelId, bean.md5, object : CommonCallbacks.UpLoadCallbackWithProgress<Int> {
                override fun onProgressUpdate(progress: Int, speed: Double) {
                    if (bean.progress == progress) return
                    AutelLog.i(TAG, "uploadOnlineAiMode -> onProgressUpdate name=${bean.projectName} progress=$progress speed=$speed")
                    bean.progress = progress
                    GlobalScope.launch(Dispatchers.Main) {
                        dealUploading(progress, speed)
                    }
                }

                override fun onSuccess(file: File?) {
                    AutelLog.i(TAG, "uploadOnlineAiMode -> onSuccess name=${bean.projectName} result=${file?.absoluteFile}")
                    bean.state = AiModelState.UPLOAD_SUCCESS
                    GlobalScope.launch(Dispatchers.Main) {
                        dealUploadSuccess()
                    }
                }

                override fun onFailure(error: IAutelCode) {
                    AutelLog.i(TAG, "uploadOnlineAiMode -> onFailure name=${bean.projectName} error=$error")
                    bean.state = AiModelState.UPLOAD_WAIT
                    GlobalScope.launch(Dispatchers.Main) {
                        dealUploadFailure()
                    }
                }

            })
        } ?: AutelLog.i(TAG, "uploadOnlineAiMode -> bean is null")
    }

    /**
     * 上传离线模型
     */
    private fun uploadOfflineAiMode() {
        aiModelBean?.let { bean ->
            val file = File(bean.url)
            AutelLog.i(TAG, "uploadOfflineAiMode -> start isExist=${file.exists()} $bean")

            if (!file.exists()) {
                bean.state = AiModelState.UPLOAD_WAIT
                listener?.uploadFailure()
                listener?.notifyItemChanged()
                binding.tvTitle.text = context.getString(R.string.common_text_exit_high_speed)
                exitExclusiveMode()
                return
            }
            chooseTargetVM.uploadOfflineModule(file, object : CommonCallbacks.UpLoadCallbackWithProgress<Int> {
                override fun onProgressUpdate(progress: Int, speed: Double) {
                    if (bean.progress == progress) return
                    AutelLog.i(TAG, "uploadOfflineAiMode -> onProgressUpdate name=${bean.projectName} progress=$progress speed=$speed")
                    bean.progress = progress
                    GlobalScope.launch(Dispatchers.Main) {
                        dealUploading(progress, speed)
                    }
                }

                override fun onSuccess(file: File?) {
                    AutelLog.i(TAG, "uploadOfflineAiMode -> onSuccess name=${bean.projectName} result=${file?.absoluteFile}")
                    bean.state = AiModelState.UPLOAD_SUCCESS
                    GlobalScope.launch(Dispatchers.Main) {
                        dealUploadSuccess()
                    }
                }

                override fun onFailure(error: IAutelCode) {
                    AutelLog.i(TAG, "uploadOfflineAiMode -> onFailure name=${bean.projectName} error=$error")
                    bean.state = AiModelState.UPLOAD_WAIT
                    GlobalScope.launch(Dispatchers.Main) {
                        dealUploadFailure()
                    }
                }

            })
        } ?: AutelLog.i(TAG, "uploadOfflineAiMode -> bean is null")
    }

    /**
     * 处理上传成功
     */
    private fun dealUploadSuccess() {
        binding.progressBar.progress = 90
        binding.tvProgress.text = "100%"
        SystemClock.sleep(300)
        listener?.uploadSuccess()
        listener?.notifyItemChanged()
        binding.tvTitle.text = context.getString(R.string.common_text_ai_mode_working)
        exitExclusiveMode()
    }

    /**
     * 处理上传中
     */
    private fun dealUploading(progress: Int, speed: Double) {
        binding.tvSpeed.text = getSpeedStr(speed)
        binding.tvProgress.text = "$progress%"
        binding.progressBar.progress = 10 + (80 * progress / 100)
        listener?.notifyItemChanged()
    }

    /**
     * 处理上传失败
     */
    private fun dealUploadFailure() {
        listener?.uploadFailure()
        listener?.notifyItemChanged()
        exitExclusiveMode()
        binding.tvTitle.text = context.getString(R.string.common_text_exit_high_speed)
    }

    /**
     * 速度
     */
    private fun getSpeedStr(speed: Double): String {
        return if (speed < NUM_1024) {
            "${keepTwoDigits(speed)}B/S"
        } else if (speed >= NUM_1024 && speed < NUM_1024 * NUM_1024) {
            "${keepTwoDigits(speed / NUM_1024)}KB/S"
        } else {
            "${keepTwoDigits(speed / NUM_1024 / NUM_1024)}MB/S"
        }
    }

    /**
     * 保留两位小数
     */
    private fun keepTwoDigits(number: Double): String {
        val format = DecimalFormat.getInstance(Locale.ENGLISH) as DecimalFormat
        format.applyPattern("0.##")
        format.roundingMode = RoundingMode.HALF_UP
        return format.format(number)
    }
}
