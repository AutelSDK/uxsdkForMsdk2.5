package com.autel.setting.provider.delegate

import android.annotation.SuppressLint
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.marginBottom
import androidx.core.view.marginLeft
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.autel.common.delegate.IMainProvider
import com.autel.common.delegate.activity.AbsDelegateActivity
import com.autel.common.delegate.layout.DelegateLayoutType
import com.autel.common.utils.DeviceUtils
import com.autel.setting.R
import com.autel.setting.business.RangingViewModel
import com.autel.widget.widget.remotelocation.RemotePlaneLocationWidget

class RemoteDroneLocationDelegateActivity(private val mainProvider: IMainProvider) : AbsDelegateActivity(mainProvider) {

    private var rangingViewModel: RangingViewModel =
        ViewModelProvider(mainProvider.getMainContext() as ComponentActivity)[RangingViewModel::class.java]

    private val remoteWidget = RemotePlaneLocationWidget(mainProvider.getMainContext())

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()
        rangingViewModel.rangingFunctionState.observe(mainProvider.getMainLifecycleOwner(), Observer<Boolean> {
            if (it) {
                mainProvider.getMainLayoutManager().removeViewFromMainLayout(DelegateLayoutType.RemoteDroneLocationType)
                val droneNumber = mainProvider.getMainHandler().getScreenState().watchDroneNumber
                val drone = droneNumber?.let {
                    DeviceUtils.getDrone(it)
                }
                remoteWidget.updateDrone(drone)

                mainProvider.getMainLayoutManager().addViewToMainLayout(
                    DelegateLayoutType.RemoteDroneLocationType,
                    remoteWidget,
                    ConstraintLayout.LayoutParams(
                        ConstraintLayout.LayoutParams.WRAP_CONTENT,
                        ConstraintLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                        startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                        marginStart = mainProvider.getMainContext().resources.getDimensionPixelOffset(R.dimen.variety_laser_margin_left)
                        bottomMargin = mainProvider.getMainContext().resources.getDimensionPixelOffset(R.dimen.variety_laser_margin_bottom)
                    })

                var orgX: Int = 0
                var orgY: Int = 0

                var touchX: Int = 0
                var touchY: Int = 0
                remoteWidget.setOnTouchListener { v, event ->
                    val rootView = mainProvider.getMainLayoutManager().getMainView()
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            orgX = v.marginLeft
                            orgY = v.marginBottom

                            touchX = event.rawX.toInt()
                            touchY = event.rawY.toInt()
                            return@setOnTouchListener true
                        }

                        MotionEvent.ACTION_MOVE -> {
                            var x = orgX + (event.rawX - touchX).toInt()
                            var y = orgY + (touchY - event.rawY).toInt()
                            val d = mainProvider.getMainContext().resources.getDimensionPixelOffset(R.dimen.common_20dp)
                            if (x < 0 || y < 0) {
                                return@setOnTouchListener true
                            }
                            if (x + remoteWidget.width + d > rootView.width) {
                                x = rootView.width - d - remoteWidget.width
                            }
                            if (y + remoteWidget.height + d > rootView.height) {
                                y = rootView.height - d - remoteWidget.height
                            }

                            remoteWidget.updateLayoutParams<ConstraintLayout.LayoutParams> {
                                leftMargin = x
                                bottomMargin = y
                            }
                        }
                    }
                    false
                }
            } else {
                mainProvider.getMainLayoutManager().removeViewFromMainLayout(DelegateLayoutType.RemoteDroneLocationType)
            }
        })

        mainProvider.getMainHandler().observerScreenState {
            val drone = it.watchDroneNumber?.let {
                DeviceUtils.getDrone(it)
            }
            remoteWidget.updateDrone(drone)
        }
    }
}