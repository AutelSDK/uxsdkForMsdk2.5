package com.autel.setting.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.children
import com.autel.setting.R
import com.autel.setting.databinding.SettingRightRadiogroupBinding

/**
 * create by longjian
 */
class RightRadioGroupView(context: Context?, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    val root = SettingRightRadiogroupBinding.inflate(LayoutInflater.from(context))
    lateinit var listener: RadioButtonClickListener

    init {
        removeAllViews()
        addView(root.root)
        root.settingRadiogroup.check(R.id.setting_rb_fly_controll)
        root.settingRadiogroup.setOnCheckedChangeListener { _, checkedId ->
            listener.let {
                when (checkedId) {
                    R.id.setting_rb_fly_controll -> {
                        it.settingRbFlyControl()
                    }

                    R.id.setting_rb_obstaclel -> {
                        it.settingRbObstacle()
                    }

                    R.id.setting_rb_remote -> {
                        it.settingRbRemote()
                    }

                    R.id.setting_rb_hd -> {
                        it.settingRbHd()
                    }

                    R.id.setting_rb_battery -> {
                        it.settingRbBattery()
                    }

                    R.id.setting_rb_camera -> {
                        it.settingRbGimbal()
                    }

                    R.id.setting_rb_rtk -> {
                        it.settingRbRtk()
                    }

                    R.id.setting_rb_thrower -> {
                        it.settingThrower()
                    }

                    R.id.setting_rb_more -> {
                        it.settingRbMore()
                    }

                    R.id.setting_rb_payload -> {
                        it.settingPayload()
                    }

                }
            }

        }
    }

    fun getAllChildView(): Sequence<View> {
        return root.settingRadiogroup.children
    }

    interface RadioButtonClickListener {
        fun settingRbFlyControl()
        fun settingRbObstacle()
        fun settingRbRemote()
        fun settingRbHd()
        fun settingRbBattery()
        fun settingRbGimbal()
        fun settingRbRtk()
        fun settingThrower()
        fun settingRbMore()

        fun settingPayload()

    }

    fun setRadioButtonClickListener(listener: RadioButtonClickListener) {
        this.listener = listener;
    }

    fun check(id: Int) {
        root.settingRadiogroup.check(id)
    }


}