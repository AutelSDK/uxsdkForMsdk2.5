package com.autel.widget.widget.codectoolright

import android.content.Context
import android.content.res.Resources
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.SystemClock
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.commit
import androidx.lifecycle.DefaultLifecycleObserver
import com.autel.common.base.BaseFragment
import com.autel.common.base.widget.ConstraintLayoutWidget
import com.autel.common.constant.AppTagConst
import com.autel.common.constant.SharedParams
import com.autel.common.constant.StringConstants
import com.autel.common.delegate.IMainProvider
import com.autel.common.delegate.layout.DelegateLayoutType
import com.autel.common.delegate.layout.IMainPanelListener
import com.autel.common.delegate.layout.MainPanelView
import com.autel.common.manager.AutelStorageManager
import com.autel.common.manager.MiddlewareManager
import com.autel.common.manager.StorageKey
import com.autel.common.sdk.CheckStorageListener
import com.autel.common.sdk.CheckStorageResult
import com.autel.common.sdk.StorageCheckHelper
import com.autel.common.sdk.checkResult
import com.autel.common.utils.DateFormatUtils
import com.autel.common.utils.DeviceUtils
import com.autel.common.utils.GoogleTextToSpeechManager
import com.autel.common.widget.toast.AutelToast
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.camera.bean.RecordStatusBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.camera.enums.RecordStatusEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.camera.enums.StorageTypeEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.camera.enums.SystemStatusEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.camera.enums.TakePhotoStatusEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.remotecontrol.enums.RCButtonTypeEnum
import com.autel.drone.sdk.vmodelx.module.camera.bean.LensTypeEnum
import com.autel.drone.sdk.vmodelx.module.fileservice.FileConstants
import com.autel.log.AutelLog
import com.autel.player.VideoType
import com.autel.player.player.AutelPlayerManager
import com.autel.videorecord.IVideoRecordListener
import com.autel.widget.R
import com.autel.widget.databinding.MissionLayoutCodecToolRightBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean



class CodecToolRightWidget @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    ConstraintLayoutWidget(context, attrs, defStyleAttr), DefaultLifecycleObserver {

    companion object {
        private const val PROGRESS_TIME_OUT = 3_000L//转圈超时时间
        private const val VIDEO_TIME_OUT = 10_000L//录像超时时间

        private const val EVENT_PHOTO_TIME_OUT = 1001//拍照超时检查
        private const val EVENT_VIDEO_TIME_OUT = 1002//开始录像超时检查
        private const val EVENT_VIDEO_RECEIVE_TIME_OUT = 1003//录像文件信息上报检查
        private const val EVENT_STOP_VIDEO_TIME_OUT = 1004//停止录像超时检查
    }


    private var isStartTakePhoto = AtomicBoolean(false)//开始拍照转圈
    private var isStartTakeVideo = AtomicBoolean(false)//开始录像转圈
    private var isStopTakeVideo = AtomicBoolean(false)//停止录像转圈
    private var isVideoUpError = AtomicBoolean(false)//录像状态异常

    private val uiBinding: MissionLayoutCodecToolRightBinding

    private var mainProvider: IMainProvider? = null

    private val widgetModel: CodecToolRightWidgetModel by lazy {
        CodecToolRightWidgetModel()
    }

    private val cameraSettingLayout: MainPanelView by lazy {
        MainPanelView(context).apply {
            id = View.generateViewId()
        }
    }

    private val cameraSettingFragment: BaseFragment by lazy { MiddlewareManager.cameraModule.getCameraSetting() }

    //超时
    private val mHandler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            if (isStartTakePhoto.get() && msg.what == EVENT_PHOTO_TIME_OUT) {
                resetPhotoProgressBar()
            }
            if (isStartTakeVideo.get() && msg.what == EVENT_VIDEO_TIME_OUT) {
                resetVideoProgressBar()
            }
            if (isStopTakeVideo.get() && msg.what == EVENT_STOP_VIDEO_TIME_OUT) {
                resetVideoProgressBar()
            }
            if (msg.what == EVENT_VIDEO_RECEIVE_TIME_OUT && isVideoUpError.get()) {
                resetPhotoProgressBar()
                resetVideoProgressBar()
            }
        }
    }

    init {
        uiBinding = MissionLayoutCodecToolRightBinding.inflate(LayoutInflater.from(context), this)
        initListener()

    }

    fun setMainProvider(mainProvider: IMainProvider) {
        this.mainProvider = mainProvider
        mainProvider.getMainLayoutManager()
            .addPanelListener(DelegateLayoutType.CameraSettingType, object : IMainPanelListener {
                override fun panelShow() {

                }

                override fun panelRemove() {
                    mainProvider.getMainFragmentManager().commit {
                        remove(cameraSettingFragment)
                    }
                }
            })
    }

    private fun initListener() {
        uiBinding.ivCameraSetting.setOnClickListener { dealCameraSettingClick(it) }
        uiBinding.ivTakePhoto.setOnClickListener { dealTakePhotoClick() }
        uiBinding.ivTakeVideo.setOnClickListener { dealTakeVideoClick() }
        uiBinding.ivAlbum.setOnClickListener { dealAlbumClick() }
    }

    fun updateLensInfo(drone: IAutelDroneDevice?, isVisible: Boolean) {
        uiBinding.ivLinkageZoom.updateLensInfo(drone, isVisible)
    }

    private fun dealCameraSettingClick(it: View) {
        /*val cameraSettingPopupWindow =
            DeviceUtils.singleControlDrone()?.let {
                MiddlewareManager.cameraModule.getCameraSettingPopupWindow(context)
            }
        cameraSettingPopupWindow?.showAtLocation(
            it,
            Gravity.END or Gravity.TOP,
            0,
            0
        )*/
        if (cameraSettingLayout.parent == null) {
            mainProvider?.getMainLayoutManager()?.showViewToPanel(
                DelegateLayoutType.CameraSettingType,
                cameraSettingLayout,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                ).apply {
                    gravity = Gravity.END
                }
            )
        }
        mainProvider?.getMainFragmentManager()?.commit {
            cameraSettingFragment.arguments = args(DeviceUtils.singleControlDrone() ?: return, null)
            replace(cameraSettingLayout.id, cameraSettingFragment)
        }
    }


    private fun args(drone: IAutelDroneDevice, lensTypeEnum: LensTypeEnum?): Bundle {
        val args = Bundle()
        args.putInt(StringConstants.ARGS_DRONE_DEVICE_ID, drone.getDeviceNumber())
        args.putSerializable(StringConstants.ARGS_CAMERA_LENS_TYPE, lensTypeEnum)
        return args
    }

    private fun dealAlbumClick() {
        MiddlewareManager.albumModule.goAlbum(context)
    }


    private val listener = object : CheckStorageListener {
        override fun dealCheckResult(device: IAutelDroneDevice, event: CheckStorageResult) {
            when (event) {
                CheckStorageResult.CHECK_TO_EMMC -> {
                    scope?.launch(CoroutineExceptionHandler { _, throwable ->
                        AutelToast.normalToast(context, R.string.common_text_set_failed)
                    }) {
                        widgetModel.setStorageType(device, StorageTypeEnum.EMMC)
                        AutelToast.normalToast(context, R.string.common_text_setting_success)
                    }
                }

                CheckStorageResult.CHECK_TO_TF_CARD -> {
                    scope?.launch(CoroutineExceptionHandler { _, throwable ->
                        AutelToast.normalToast(context, R.string.common_text_set_failed)
                    }) {
                        widgetModel.setStorageType(device, StorageTypeEnum.SD)
                        AutelToast.normalToast(context, R.string.common_text_setting_success)
                    }
                }

                CheckStorageResult.FORMAT_TF_CARD -> {
                    scope?.launch(CoroutineExceptionHandler { _, throwable ->
                        AutelToast.normalToast(context, R.string.common_text_set_failed)
                    }) {
                        widgetModel.formatSDCard(device)
                        AutelToast.normalToast(context, R.string.common_text_setting_success)
                    }
                }

                CheckStorageResult.CLEAN_ALBUM -> {
                    MiddlewareManager.albumModule.goAlbum(context)
                }
            }
        }
    }

    private fun checkStorageEnable(drone: IAutelDroneDevice): Boolean {
        return StorageCheckHelper.isStorageEnable(context, drone, listener)
    }


    /**
     * 处理拍照按钮事件
     */
    private fun dealTakePhotoClick() {
        val controlDroneList = MiddlewareManager.codecModule.getSplitScreenDroneList()
        if (controlDroneList.isNullOrEmpty()) {
            return
        }
        val checkResultList = controlDroneList.map {
            checkStorageEnable(it)
        }
        if (checkResultList.all { it }) {
            uiBinding.psTakeLoading.isVisible = true
            uiBinding.psTakeLoading.progress = 0
            isStartTakePhoto.set(true)
            mHandler.removeMessages(EVENT_PHOTO_TIME_OUT)
            mHandler.sendEmptyMessageDelayed(EVENT_PHOTO_TIME_OUT, PROGRESS_TIME_OUT)

            startTakePhoto(controlDroneList)
        }

    }

    private fun startTakePhoto(controlDroneList: List<IAutelDroneDevice>) {
        widgetModel.startTakePhoto(controlDroneList)
    }

    private fun startRecord(controlDroneList: List<IAutelDroneDevice>) {
        scope?.launch(CoroutineExceptionHandler { _, throwable ->

        }) {
            val resultList = widgetModel.startRecord(controlDroneList)
            resultList.checkResult({
                AutelLog.i(AppTagConst.ToolRight, "startRecord -> success")
                mHandler.removeCallbacksAndMessages(null)
                mHandler.sendEmptyMessageDelayed(EVENT_VIDEO_RECEIVE_TIME_OUT, VIDEO_TIME_OUT)
                isVideoUpError.set(true)
            }, {
                AutelLog.e(AppTagConst.ToolRight, "startRecord -> failure")
                //开始失败则停止转圈
                resetVideoProgressBar()
                endRecord()
                AutelToast.normalToast(context, R.string.common_text_start_record_failed)
            })

        }
    }

    private fun stopRecord(controlDroneList: List<IAutelDroneDevice>) {
        scope?.launch(CoroutineExceptionHandler { _, throwable ->

        }) {
            val resultList = widgetModel.stopRecord(controlDroneList)
            resultList.checkResult({
                AutelLog.i(AppTagConst.ToolRight, "stopRecord -> success")
                mHandler.removeCallbacksAndMessages(null)
                mHandler.sendEmptyMessageDelayed(EVENT_VIDEO_RECEIVE_TIME_OUT, VIDEO_TIME_OUT)
                isVideoUpError.set(true)
            }, {
                AutelLog.e(AppTagConst.ToolRight, "stopRecord -> failure")
                resetVideoProgressBar()
                AutelToast.normalToast(context, R.string.common_text_stop_record_failed)
            })

        }
    }


    //当前执行录像事件的时间戳
    private var curTakeVideoTime = 0L

    override fun reactToModelChanges() {
        super.reactToModelChanges()
        widgetModel.photoFileData.subscribe {
            AutelLog.i(AppTagConst.ToolRight, "cameraTakePhotoData -> ${it.thumbnailPath}")
            it.thumbnailPath?.let {
                val strBaseUrl = DeviceManager.getDeviceManager().getActiveAlbumBaseUrl()
                AutelLog.i(AppTagConst.ToolRight, "cameraTakePhotoData -> $strBaseUrl")
                Glide.with(context)
                    .load(strBaseUrl + FileConstants.THUMBNAIL_PATH + it)
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .error(R.drawable.mission_ic_album)
                    .fallback(R.drawable.mission_ic_album)
                    .apply(RequestOptions.bitmapTransform(CircleCrop()))
                    .into(uiBinding.ivAlbum)
            }
        }
        widgetModel.recordFileData.subscribe {
            AutelLog.i(AppTagConst.ToolRight, "recordFileInfoData -> ${it.thumbnailPath}")
            //取消停止录像时间上报超时
            if (isVideoUpError.get()) {
                isVideoUpError.set(false)
                mHandler.removeMessages(EVENT_VIDEO_RECEIVE_TIME_OUT)
                resetVideoProgressBar(false)
            }

            it.thumbnailPath?.let {
                val strBaseUrl = DeviceManager.getDeviceManager().getActiveAlbumBaseUrl()
                AutelLog.i(AppTagConst.ToolRight, "recordFileInfoData -> $strBaseUrl")
                Glide.with(context)
                    .load(strBaseUrl + it)
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .error(R.drawable.mission_ic_album)
                    .fallback(R.drawable.mission_ic_album)
                    .apply(RequestOptions.bitmapTransform(CircleCrop()))
                    .into(uiBinding.ivAlbum)
            }
        }

        widgetModel.toolRightData.subscribe {
            uiBinding.ivCameraSetting.isEnabled = !it.isMissionMode && it.connected
            val record = it.recordStatus
            val recordStatus = record?.state
            uiBinding.ivCameraSetting.isInvisible = !it.isSingleControl ||
                    recordStatus == RecordStatusEnum.START ||
                    recordStatus == RecordStatusEnum.RECORDING ||
                    !it.isMainRc

            uiBinding.ivTakePhoto.isEnabled = !isStartTakeVideo.get() && !isStopTakeVideo.get() && it.connected && !it.isMissionMode
            //拍照转圈时，禁用录像按钮
            uiBinding.ivTakeVideo.isEnabled =
                !isStartTakePhoto.get() && it.connected && !it.isMissionMode
            uiBinding.flAlbum.isInvisible = !(it.isSingleControl && it.isMainRc)
            if (!it.connected) {
                isStartTakeVideo.set(false)
                isStopTakeVideo.set(false)
                isStartTakePhoto.set(false)
            }

            if (it.connected) {
                if (recordStatus == RecordStatusEnum.START) {
                    mHandler.removeMessages(EVENT_VIDEO_TIME_OUT)
                    mHandler.removeMessages(EVENT_STOP_VIDEO_TIME_OUT)
                    isStartTakeVideo.set(false)
                    isStopTakeVideo.set(false)
                    uiBinding.psTakeVideoLoading.isVisible = false
                    uiBinding.tvVideoRecordTime.isVisible = true
                    recording(record)
                } else if (recordStatus == RecordStatusEnum.RECORDING) {
                    if (isVideoUpError.get()) {
                        isVideoUpError.set(false)
                        mHandler.removeMessages(EVENT_VIDEO_RECEIVE_TIME_OUT)
                    }
                    recording(record)
                } else {
                    if (!isStartTakeVideo.get()) {
                        mHandler.removeMessages(EVENT_STOP_VIDEO_TIME_OUT)
                        isStopTakeVideo.set(false)
                        uiBinding.psTakeVideoLoading.isVisible = false
                        uiBinding.tvVideoRecordTime.isVisible = false
                        endRecord()
                    }
                }
            } else {
                endRecord()
            }
            val photoStatus = it.photoStatus
            if (photoStatus != TakePhotoStatusEnum.START) {
                isStartTakePhoto.set(false)
                uiBinding.psTakeLoading.isVisible = false
            }
        }

        widgetModel.hardwareButtonData.subscribe {
            if (DeviceUtils.isMainRC()) {
                dealRcEvent(it.buttonType)
            }
        }
    }

    /**
     * 处理录像按钮事件
     */
    private fun dealTakeVideoClick() {
        val controlDroneList = MiddlewareManager.codecModule.getSplitScreenDroneList()
        if (controlDroneList.isNullOrEmpty()) {
            return
        }
        val checkResultList = controlDroneList.map {
            checkStorageEnable(it)
        }
        if (checkResultList.all { it }) {
            if (SystemClock.elapsedRealtime() - curTakeVideoTime < 1000) {
                AutelLog.i(AppTagConst.ToolRight, "禁止点击频繁点击录像,防止测试同事手抽风一样的狂点")
                return
            }
            curTakeVideoTime = SystemClock.elapsedRealtime()
            val systemStatusList =
                controlDroneList.map { it.getDeviceStateData().gimbalDataMap[it.getGimbalDeviceType()]?.cameraData?.systemStatus }

            //预录制
            val preRecordSwitch = AutelStorageManager.getPlainStorage().getBooleanValue(StorageKey.PlainKey.KEY_PRE_RECORD_SWITCH)
            val list = SharedParams.lensInfoBeans.value ?: emptyList()
            AutelLog.i(AppTagConst.ToolRight, "preRecordSwitch -> preRecordSwitch=$preRecordSwitch lensInfoBeans -> $list")
            if (systemStatusList.all { it == SystemStatusEnum.IDLE }) {
                AutelLog.i(AppTagConst.ToolRight, "preRecordSwitch -> startRecord ")
                //所有飞机都为空闲状态，执行录像
                uiBinding.psTakeVideoLoading.isVisible = true
                isStartTakeVideo.set(true)
                uiBinding.tvVideoRecordTime.visibility = VISIBLE
                uiBinding.tvVideoRecordTime.text = DateFormatUtils.formatTimeStr(0)

                //3s超时转圈检查
                mHandler.removeMessages(EVENT_VIDEO_TIME_OUT)
                mHandler.sendEmptyMessageDelayed(EVENT_VIDEO_TIME_OUT, PROGRESS_TIME_OUT)
                GoogleTextToSpeechManager.instance().speak(context.getString(R.string.common_text_start_recording), true)
                startRecord(controlDroneList)
                if (preRecordSwitch) {
                    list.forEach { lensInfo ->
                        if (lensInfo.enable) {
                            val videoType = DeviceUtils.getVideoTypeByLens(lensInfo.type)
                            AutelLog.i(AppTagConst.ToolRight, "preRecordSwitch -> start type=${lensInfo.type} videoType=$videoType")
                            AutelPlayerManager.getInstance()
                                .startPreRecordVideo(videoType, object : IVideoRecordListener {
                                    override fun onError(errorCode: Int, detail: String?) {
                                        AutelLog.i(AppTagConst.ToolRight, "preRecordSwitch -> videoType=$videoType result, the errorCode = $errorCode, the detail = $detail")
                                    }
                                })
                        }
                    }
                }
            } else if (systemStatusList.firstOrNull { it == SystemStatusEnum.RECORDING } != null) {
                AutelLog.i(AppTagConst.ToolRight, "preRecordSwitch -> stopRecord ")
                //有飞机处于录像状态，执行停止录像
                uiBinding.psTakeVideoLoading.isVisible = true
                isStopTakeVideo.set(true)
                mHandler.removeMessages(EVENT_STOP_VIDEO_TIME_OUT)
                mHandler.sendEmptyMessageDelayed(EVENT_STOP_VIDEO_TIME_OUT, PROGRESS_TIME_OUT)
                GoogleTextToSpeechManager.instance().speak(context.getString(R.string.common_text_stop_recording), true)
                stopRecord(controlDroneList.filter { it.getDeviceStateData().gimbalDataMap[it.getGimbalDeviceType()]?.cameraData?.systemStatus == SystemStatusEnum.RECORDING })
                if (preRecordSwitch) {
                    val list = SharedParams.lensInfoBeans.value ?: emptyList()
                    list.forEach { lensInfo ->
                        if (lensInfo.enable) {
                            val videoType = DeviceUtils.getVideoTypeByLens(lensInfo.type)
                            AutelLog.i(AppTagConst.ToolRight, "preRecordSwitch -> stop type=${lensInfo.type} videoType=$videoType")
                            AutelPlayerManager.getInstance().stopPreRecordVideo(videoType)
                        }
                    }
                }
            } else {
                AutelLog.i(AppTagConst.ToolRight, "dealTakeVideoClick 相机状态异常 systemStatusList=${systemStatusList.joinToString { " " + it }}")
                AutelToast.normalToast(context, R.string.common_text_camera_busy_to_retry)
            }
        }
    }

    /**
     * 重置拍照转圈圈
     */
    private fun resetPhotoProgressBar() {
        isStartTakePhoto.set(false)

        uiBinding.psTakeLoading.isVisible = false

    }

    /**
     * 重置录像转圈圈
     */
    private fun resetVideoProgressBar(isShowTime: Boolean = false) {
        isStartTakeVideo.set(false)
        uiBinding.psTakeVideoLoading.isVisible = false
        uiBinding.tvVideoRecordTime.isVisible = isShowTime
    }

    private fun recording(it: RecordStatusBean) {
        uiBinding.ivTakeVideo.setBackgroundResource(R.drawable.mission_selector_right_camera_take_video_run)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            uiBinding.ivTakeVideo.setImageResource(Resources.ID_NULL)
        } else {
            uiBinding.ivTakeVideo.setImageResource(0)
        }
        AutelLog.i("RecordStatusBean", "app receive $it")
        uiBinding.tvVideoRecordTime.text = DateFormatUtils.formatTimeStr(it.currentRecordTime.toLong())
        uiBinding.tvVideoRecordTime.isVisible = true
    }

    private fun endRecord() {
        uiBinding.ivTakeVideo.setBackgroundResource(R.drawable.mission_selector_right_camera_take_video)
        uiBinding.ivTakeVideo.setImageResource(R.drawable.mission_selector_take_video)
        uiBinding.tvVideoRecordTime.isVisible = false
    }


    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isInEditMode) {
            widgetModel.setup()
        }
    }

    override fun onDetachedFromWindow() {
        if (!isInEditMode) {
            widgetModel.cleanup()
            mHandler.removeCallbacksAndMessages(null)
        }
        super.onDetachedFromWindow()
    }

    /**
     * 处理拍照录像键，物理按键
     */
    private fun dealRcEvent(button: RCButtonTypeEnum) {
        if (MiddlewareManager.codecModule.getSplitScreenDroneList()
                ?.all { it.getDeviceStateData().flightoperateData.bAircraftActivation == true } == true
        ) {
            if (button == RCButtonTypeEnum.PHOTO) {
                if (uiBinding.ivTakePhoto.isEnabled) {
                    dealTakePhotoClick()
                }
            } else if (button == RCButtonTypeEnum.PHOTO_RECORD) {
                if (uiBinding.ivTakePhoto.isEnabled) {
                    dealTakePhotoClick()
                }
            } else if (button == RCButtonTypeEnum.RECORD) {
                if (uiBinding.ivTakeVideo.isEnabled) {
                    dealTakeVideoClick()
                }
            }
        }
    }

    fun getCameraPosition(): Rect {
        return getViewLocation(uiBinding.ivCameraSetting)
    }

    fun getTakePhotoPosition(): Rect {
        return getViewLocation(uiBinding.ivTakePhoto)
    }

    fun getTakeVideoPosition(): Rect {
        return getViewLocation(uiBinding.ivTakeVideo)
    }

    fun getAlbumPosition(): Rect {
        return getViewLocation(uiBinding.ivAlbum)
    }

    private fun getViewLocation(view: View): Rect {
        val location = IntArray(2)
        val rect = Rect()
        view.getLocationOnScreen(location)
        rect.left = location[0]
        rect.top = location[1]
        rect.right = rect.left + view.width
        rect.bottom = rect.top + view.height
        return rect
    }

}