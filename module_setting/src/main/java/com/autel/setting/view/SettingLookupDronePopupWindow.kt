package com.autel.setting.view

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import com.autel.common.feature.phone.AutelPhoneLocationManager
import com.autel.common.manager.unit.CoordinateUnitEnum
import com.autel.common.manager.unit.UnitManager
import com.autel.common.utils.*
import com.autel.common.widget.BasePopWindow
import com.autel.common.widget.toast.AutelToast
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.log.AutelLog
import com.autel.map.bean.AutelLatLng
import com.autel.map.util.MapBoxUtils
import com.autel.setting.R
import com.autel.setting.business.SettingLookupDroneVM
import com.autel.setting.databinding.SettingLookupDroneFragmentBinding
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import java.text.SimpleDateFormat
import java.util.*

/**
 * @Author create by LJ
 * @Date 2022/12/12 16
 * 找飞机
 */
class SettingLookupDronePopupWindow(context: Context, private val droneDevice: IAutelDroneDevice) : BasePopWindow(context) {
    private val TAG = "SettingLookupDroneFragment"
    private var latString: String? = null//纬度
    private var lngString: String? = null//经度
    private var isLookUp = false//是否开启闪灯鸣叫


    private val settingLookupDroneVm = ViewModelProvider(context as ComponentActivity)[SettingLookupDroneVM::class.java]
    private var player: ExoPlayer? = null
    private var VIDEO_PATH_URI = "/storage/emulated/0/Android/data/com.autel.enterprise/files/Album/vl_video.MP4"
    private var isRefreshLocation = false

    private val binding = SettingLookupDroneFragmentBinding.inflate(LayoutInflater.from(context))

    private val timerListener = object : TimerEventListener(this.javaClass.simpleName) {
        override fun onEventChanged() {
            if (droneDevice.isConnected()) {
                setLntLng(
                    droneDevice.getDeviceStateData().flightControlData.droneLatitude,
                    droneDevice.getDeviceStateData().flightControlData.droneLongitude
                )
            }
        }
    }

    init {
        contentView = binding.root
        width = context.resources.getDimensionPixelOffset(R.dimen.common_270dp)
        height = ViewGroup.LayoutParams.WRAP_CONTENT

        TimerManager.addTimer1sEventListener(timerListener)
        initData()
        initView()
        //SaveLastLatLngUtils.getLastLatLng(droneDevice)?.let { setLntLng(it.latitude, it.longitude) }
        getData()
    }


    override fun dismiss() {
        super.dismiss()
        releasePlayer()
        TimerManager.removeTimerEventListener(timerListener)
    }

    private fun initData() {
        VIDEO_PATH_URI = AutelDirPathUtils.getLookFlightVideoCachePath() + "/vl_video.MP4"
    }

    private fun getData() {
        settingLookupDroneVm.getFcsBuzzingStatus(droneDevice, onSuccess = {
            isLookUp = it
            refreshLookUpView()
        }, onError = {

        })
    }

    private fun refreshLookUpView() {
        val textId =
            if (isLookUp) R.string.common_text_light_drone_off else R.string.common_text_light_drone
        binding.tvStartTweet.text = context.getString(textId)
    }

    private fun initView() {
        if (droneDevice.isConnected()) {
            binding.videoView.visibility = View.GONE
        } else {
            binding.videoView.visibility = View.VISIBLE
            initializePlayer()
        }
        binding.tvStartTweet.setOnClickListener {
            if (!DeviceUtils.hasDroneConnected()) {
                AutelToast.normalToast(context, context.getString(R.string.common_text_loss_drone_loss))
                return@setOnClickListener
            }
            settingLookupDroneVm.setFcsBuzzingStatus(droneDevice, !isLookUp, onSuccess = {
                isLookUp = !isLookUp
                refreshLookUpView()
            }, onError = {

            })
        }

        binding.tvStartGuide.setOnClickListener {
            AutelToast.normalToast(context, R.string.common_text_fun_not_support)
        }
        binding.tvCopyLat.setOnClickListener {
            latString?.let {
                val clipboard =
                    context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Lat", it)
                clipboard.setPrimaryClip(clip)
                if (UnitManager.getSelectCoordinateUnit() == CoordinateUnitEnum.WGS84_MGRS) {
                    AutelToast.normalToast(context, R.string.common_text_copy_success)
                } else {
                    AutelToast.normalToast(context, R.string.common_text_copy_lat_success)
                }
            }
        }

        binding.tvCopyLng.setOnClickListener {
            lngString?.let {
                val clipboard =
                    context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Lng", it)
                clipboard.setPrimaryClip(clip)
                if (UnitManager.getSelectCoordinateUnit() == CoordinateUnitEnum.WGS84_MGRS) {
                    AutelToast.normalToast(context, R.string.common_text_copy_success)
                } else {
                    AutelToast.normalToast(context, R.string.common_text_copy_lng_success)
                }
            }
        }
        binding.ivClose.setOnClickListener {
            dismiss()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setLntLng(droneLatitude: Double, droneLongitude: Double) {
        var latLng = AutelLatLng(droneLatitude, droneLongitude)


        //经纬度非法的时候，直接显示NA
        if (latLng.isInvalid()) {
            binding.tvLoseCoordinateLat.text = context.getString(R.string.common_text_no_value)
            binding.tvLoseCoordinateLng.text = context.getString(R.string.common_text_no_value)
            binding.tvLoseLocation.text = context.getString(R.string.common_text_no_value)
            binding.tvLoseTime.text = context.getString(R.string.common_text_no_value)
            binding.tvLoseDistance.text = context.getString(R.string.common_text_no_value)
            AutelLog.i(TAG, "实时上报的数据非法，找飞机中的GPS 也非法，则显示NA ")
            return
        }

        val result = LatLngUtil.getLatLngWithUnit(droneLongitude, droneLatitude)
        latString = "${result[0]}"
        lngString = "${result[1]}"
        binding.tvLoseCoordinateLat.text = latString
        if (UnitManager.getSelectCoordinateUnit() == CoordinateUnitEnum.WGS84_MGRS) {
            binding.tvLoseCoordinateLat.text = "${result[0]}"
            binding.tvLoseCoordinateLng.visibility = View.GONE
            binding.tvCopyLng.visibility = View.GONE
        } else {
            binding.tvLoseCoordinateLng.text = lngString
            binding.tvLoseCoordinateLng.visibility = View.VISIBLE
            binding.tvCopyLng.visibility = View.VISIBLE
        }


        //显示时间
        if (droneDevice.isConnected()) {
            binding.tvLoseTime.text = getTimeStr(System.currentTimeMillis())
        } else {
            binding.tvLoseTime.text = context.getString(R.string.common_text_no_value)
        }
        //计算飞机坐标和遥控器坐标的距离
        val remoteLatLng = AutelPhoneLocationManager.locationLiveData.value
        if (remoteLatLng != null && !remoteLatLng.isInvalid()) {
            val distance =
                MapBoxUtils.getDistance(remoteLatLng, AutelLatLng(droneLatitude, droneLongitude))
            binding.tvLoseDistance.text = TransformUtils.getLengthWithUnit(distance)
        }

    }

    /**
     * 格式化时间
     */
    @SuppressLint("SimpleDateFormat")
    private fun getTimeStr(time: Long): String? {
        val date = Date(time)
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        return format.format(date)
    }

    private fun initializePlayer() {
        player = ExoPlayer.Builder(context)
            .build()
            .also { exoPlayer ->
                binding.videoView.player = exoPlayer
            }
        val mediaItem: MediaItem = MediaItem.fromUri(VIDEO_PATH_URI)
        player?.playWhenReady = true
        player?.seekTo(0, 0L)
        player?.setMediaItem(mediaItem)
        player?.repeatMode = Player.REPEAT_MODE_ONE
        player?.prepare()
    }

    private fun releasePlayer() {
        player?.let { exoPlayer ->
            exoPlayer.release()
        }
        player = null
    }
}