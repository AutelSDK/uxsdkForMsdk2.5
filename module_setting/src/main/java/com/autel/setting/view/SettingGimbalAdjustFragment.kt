package com.autel.setting.view

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import com.autel.common.base.BaseAircraftFragment
import com.autel.common.lifecycle.LiveDataBus
import com.autel.common.lifecycle.event.GimbalAdjustEvent
import com.autel.common.lifecycle.event.ShowGimbalAdjustModel
import com.autel.common.utils.DeviceUtils
import com.autel.common.utils.NumberParseUtil
import com.autel.drone.sdk.vmodelx.interfaces.IValueChangeListener
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.GimbalKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.base.AutelKeyInfo
import com.autel.log.AutelLog
import com.autel.setting.R
import com.autel.setting.business.SettingGimbalAdjustVM
import com.autel.setting.databinding.SettingGimbalFineAdjustmentFragmentBinding
import kotlin.math.abs

/**
 * @Date 2022/9/12 10:34
 * 云台微调
 */
class SettingGimbalAdjustFragment : BaseAircraftFragment(), View.OnClickListener {
    private lateinit var gimbalBinding: SettingGimbalFineAdjustmentFragmentBinding
    private val settingGimbalAdjustVM: SettingGimbalAdjustVM by viewModels()

    //SDK接口都是100倍，所以显示需要除100
    private val step = 10f
    private var tvArray: ArrayList<TextView> = arrayListOf()
    private var yaw: Float = 0f
    private var pitch: Float = 0f
    private var roll: Float = 0f
    private val maxValue = 500f
    private val minValue = -500f

    private var curType: AdjustType = AdjustType.ROLL_TYPE

    private enum class AdjustType {
        ROLL_TYPE, //翻滚--水平
        PITCH_TYPE, //上下
        YAW_TYPE//左右
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        gimbalBinding = SettingGimbalFineAdjustmentFragmentBinding.inflate(inflater, container, false)
        return gimbalBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initView()
    }

    override fun onVisible() {
        super.onVisible()
        if (!DeviceUtils.isSingleControlDroneConnected()) {
            showToast(R.string.common_text_aircraft_disconnect)
            return
        }
    }

    private val interestedKeyList = listOf(
        GimbalKey.KeyYawAdjustAngle.keyName,
        GimbalKey.KeyRollAdjustAngle.keyName,
        GimbalKey.KeyPitchAngleRange.keyName,
    )

    /**
     * 当keyManager调用过这个key，会收到回调
     * */
    private val valueChangedListener = object : IValueChangeListener {
        override fun onValueChanged(list: List<AutelKeyInfo<*>>) {
            AutelLog.i("SettingGimbalAdjustFragment", "[onValueChanged]:${list.firstOrNull()?.keyName}")//几乎所有的list都只有一个元素
            val result = list.find { autelKeyInfo -> interestedKeyList.any { keyName -> autelKeyInfo.keyName == keyName } }
            if (result != null) {
                AutelLog.i("SettingGimbalAdjustFragment", "[onValueChangedOfInterestedKeyList]:${result.keyName}")
                if (result.keyName == GimbalKey.KeyYawAdjustAngle.keyName) {
                    settingGimbalAdjustVM.getGimbalYaw({
                        AutelLog.i("SettingGimbalAdjustFragment", "adjustGimbalPatch is Success${it.toString()}")
                        yaw = it.toFloat()
                        onClick(gimbalBinding.tvYaw)
                    }, { e ->
                        e.message?.let { msg ->
                            AutelLog.i("SettingGimbalAdjustFragment", "adjustGimbalPatch is Error $msg")
                        }
                    })
                } else if (result.keyName == GimbalKey.KeyRollAdjustAngle.keyName) {
                    settingGimbalAdjustVM.getGimbalRoll({
                        roll = it.toFloat()
                        onClick(gimbalBinding.tvRoll)
                    }, { e ->
                        e.message?.let { msg ->
                            AutelLog.i("SettingGimbalAdjustFragment", "adjustGimbalRoll is Error $msg")
                        }
                    })
                } else if (result.keyName == GimbalKey.KeyPitchAngleRange.keyName) {
                    settingGimbalAdjustVM.getGimbalPitch({
                        AutelLog.i("SettingGimbalAdjustFragment", "adjustGimbalPatch is Success${it.toString()}")
                        pitch = it.toFloat()
                        onClick(gimbalBinding.tvPitch)
                    }, { e ->
                        e.message?.let { msg ->
                            AutelLog.i("SettingGimbalAdjustFragment", "adjustGimbalPatch is Error $msg")
                        }
                    })
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AutelLog.i("SettingGimbalAdjustFragment", "onCreate")
        DeviceUtils.singleControlDrone()?.getKeyManager()?.setValueChangeListener(valueChangedListener)
    }
    override fun onDestroy() {
        super.onDestroy()
        AutelLog.i("SettingGimbalAdjustFragment", "onDestroy")
        DeviceUtils.singleControlDrone()?.getKeyManager()?.removeValueChangeListener(valueChangedListener)
    }

    fun initView() {
        tvArray.add(gimbalBinding.tvRoll)
        tvArray.add(gimbalBinding.tvYaw)
        tvArray.add(gimbalBinding.tvPitch)

        gimbalBinding.llGimbalMain.setOnClickListener(this)
        gimbalBinding.tvPitch.setOnClickListener(this)
        gimbalBinding.tvYaw.setOnClickListener(this)
        gimbalBinding.tvRoll.setOnClickListener(this)
        gimbalBinding.ivGimbalLeft.setOnClickListener(this)
        gimbalBinding.ivGimbalRight.setOnClickListener(this)
        gimbalBinding.adjustBtn.setOnClickListener(this)
        gimbalBinding.flGimbalRoot.setOnClickListener(this)

        checkSelectedItem(R.id.tv_roll)
    }

    /**
     * 刷新微调值
     * 需要除以100，并且负数校正
     */
    @SuppressLint("SetTextI18n")
    private fun refreshGimbalValue(value: Float) {
        val negative = if (value < 0) "-" else ""
        gimbalBinding.tvValue.text = "$negative${NumberParseUtil.formatFloat(abs(value) / 100, 1)}"
    }

    override fun getData() {
        settingGimbalAdjustVM.getGimbalRoll({
            roll = it.toFloat()
            refreshGimbalValue(roll)
        }, { e ->
            e.message?.let { msg ->
                AutelLog.i("SettingGimbalAdjustFragment", "adjustGimbalRoll is Error $msg")
            }
        })

        settingGimbalAdjustVM.getGimbalPitch({
            AutelLog.i("SettingGimbalAdjustFragment", "adjustGimbalPatch is Success${it.toString()}")
            pitch = it.toFloat()
        }, { e ->
            e.message?.let { msg ->
                AutelLog.i("SettingGimbalAdjustFragment", "adjustGimbalPatch is Error $msg")
            }
        })

        settingGimbalAdjustVM.getGimbalYaw({
            AutelLog.i("SettingGimbalAdjustFragment", "adjustGimbalPatch is Success${it.toString()}")
            yaw = it.toFloat()
        }, { e ->
            e.message?.let { msg ->
                AutelLog.i("SettingGimbalAdjustFragment", "adjustGimbalPatch is Error $msg")
            }
        })
    }

    override fun addListen() {

    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.tv_roll -> {
                checkSelectedItem(R.id.tv_roll)
                gimbalBinding.ivGimbalLeft.setImageResource(R.drawable.setting_selector_gimbal_roll_left)
                gimbalBinding.ivGimbalRight.setImageResource(R.drawable.setting_selector_gimbal_roll_right)
                curType = AdjustType.ROLL_TYPE
                refreshGimbalValue(roll)
            }
            R.id.tv_yaw -> {
                checkSelectedItem(R.id.tv_yaw)
                gimbalBinding.ivGimbalLeft.setImageResource(R.drawable.setting_selector_gimbal_yaw_left)
                gimbalBinding.ivGimbalRight.setImageResource(R.drawable.setting_selector_gimbal_yaw_right)
                curType = AdjustType.YAW_TYPE
                refreshGimbalValue(yaw)
            }
            R.id.tv_pitch -> {
                checkSelectedItem(R.id.tv_pitch)
                gimbalBinding.ivGimbalLeft.setImageResource(R.drawable.setting_selector_gimbal_pitch_left)
                gimbalBinding.ivGimbalRight.setImageResource(R.drawable.setting_selector_gimbal_pitch_right)
                curType = AdjustType.PITCH_TYPE
                refreshGimbalValue(pitch)
            }
            R.id.iv_gimbal_left, R.id.iv_gimbal_right -> {
                val isLeft = v.id == R.id.iv_gimbal_left
                when (curType) {
                    AdjustType.ROLL_TYPE -> {
                        val nv = addStep(roll, isLeft)
                        if (roll != nv) {
                            roll = nv
                            refreshGimbalValue(roll)
                            adjustRoll(roll)
                        }
                    }
                    AdjustType.PITCH_TYPE -> {
                        val nv = addStep(pitch, isLeft)
                        if (pitch != nv) {
                            pitch = nv
                            refreshGimbalValue(pitch)
                            adjustPitch(pitch)
                        }
                    }
                    AdjustType.YAW_TYPE -> {
                        val nv = addStep(yaw, isLeft)
                        if (yaw != nv) {
                            yaw = nv
                            refreshGimbalValue(yaw)
                            adjustYaw(yaw)
                        }
                    }
                }
            }
            R.id.ll_gimbal_main -> {
                //nothing,just don't exit
            }
            else -> {
                parentFragmentManager.beginTransaction().hide(this).commit()
                LiveDataBus.of(GimbalAdjustEvent::class.java).showGimbalAdjust().post(ShowGimbalAdjustModel(false))
            }
        }
    }

    private fun addStep(v: Float, isLeft: Boolean): Float {
        val rs = v + (if (isLeft) -step else step)
        return check(if (isLeft) rs.coerceAtLeast(minValue) else rs.coerceAtMost(maxValue))
    }

    private fun adjustRoll(roll: Float) {
        settingGimbalAdjustVM.adjustGimbalRoll(roll.toInt(), {
            AutelLog.i("SettingGimbalAdjustFragment", "adjustGimbalRoll is Success")

        }, { e ->
            e.message?.let { msg ->
                AutelLog.i("SettingGimbalAdjustFragment", "adjustGimbalRoll is Error $msg")
            }
        })
    }

    private fun adjustYaw(yaw: Float) {
        settingGimbalAdjustVM.adjustGimbalYaw(yaw.toInt(), {
            AutelLog.i("SettingGimbalAdjustFragment", "adjustGimbalYaw is Success")

        }, { e ->
            e.message?.let { msg ->
                AutelLog.i("SettingGimbalAdjustFragment", "adjustGimbalYaw is Error $msg")
            }
        })
    }

    private fun adjustPitch(pitch: Float) {
        settingGimbalAdjustVM.adjustGimbalPitch(pitch.toInt(), {
            AutelLog.i("SettingGimbalAdjustFragment", "adjustGimbalPatch is Success")

        }, { e ->
            e.message?.let { msg ->
                AutelLog.i("SettingGimbalAdjustFragment", "adjustGimbalPatch is Error $msg")
            }
        })
    }

    private fun checkSelectedItem(id: Int) {
        tvArray.forEach {
            if (it.id == id) {
                it.setTextColor(ContextCompat.getColor(requireContext(), R.color.common_color_FEE15D))
            } else {
                it.setTextColor(ContextCompat.getColor(requireContext(), R.color.common_color_e1))
            }
        }
    }

    private fun check(number: Float): Float {
        return ((number * 10).toInt().toFloat()) / 10
    }
}