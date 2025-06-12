package com.autel.setting.provider.delegate

import android.view.View
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import com.autel.common.delegate.IMainProvider
import com.autel.common.delegate.activity.AbsDelegateActivity
import com.autel.common.delegate.layout.DelegateLayoutType
import com.autel.common.lifecycle.LiveDataBus
import com.autel.common.lifecycle.event.GimbalAdjustEvent
import com.autel.log.AutelLog
import com.autel.setting.R
import com.autel.setting.view.SettingGimbalAdjustFragment

/**
 * Created by  2023/5/16
 *  云台微调代理Activity
 */
class GimbalAdjustDelegateActivity(delegateProvider: IMainProvider) : AbsDelegateActivity(delegateProvider) {
    private var fragLayout: FrameLayout? = null

    override fun onCreate() {
        LiveDataBus.of(GimbalAdjustEvent::class.java).showGimbalAdjust().observe(delegateProvider.getMainLifecycleOwner()) {
            if (it.showGimbalAdjust) {
                addGimbalAdjustFragment()
            } else {
                removeGimbalAdjustFragment()
            }
        }
    }

    private fun addGimbalAdjustFragment() {
        AutelLog.i("SettingGimbalAdjustFragment", "addGimbalAdjustFragment")
        val gimbalAdjustFragment = SettingGimbalAdjustFragment()
        if (fragLayout == null) {
            fragLayout = FrameLayout(delegateProvider.getMainContext()).apply {
                id = View.generateViewId()
            }
        }

        delegateProvider.getMainLayoutManager().addViewToMainLayout(
            DelegateLayoutType.GimbalAdjustLayoutType,
            fragLayout!!,
            ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT)
        )
        delegateProvider.getMainFragmentManager().beginTransaction().replace(
            fragLayout!!.id,
            gimbalAdjustFragment
        ).setCustomAnimations(R.anim.common_anim_slide_in_bottom, R.anim.common_anim_slide_in_bottom).commit()
    }

    private fun removeGimbalAdjustFragment() {
        AutelLog.i("SettingGimbalAdjustFragment", "removeGimbalAdjustFragment")
        if (fragLayout != null) {
            val fragment = delegateProvider.getMainFragmentManager().findFragmentById(fragLayout!!.id)
            if (fragment is SettingGimbalAdjustFragment) {
                delegateProvider.getMainFragmentManager().beginTransaction().remove(fragment).commit()
            }
            delegateProvider.getMainLayoutManager().removeViewFromMainLayout(DelegateLayoutType.GimbalAdjustLayoutType)
        }
    }
}