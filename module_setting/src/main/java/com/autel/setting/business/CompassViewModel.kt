package com.autel.setting.business

import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.RelativeLayout.LayoutParams
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModel
import com.autel.common.delegate.IMainProvider
import com.autel.common.manager.AppInfoManager
import com.autel.common.model.lens.CodecLayoutType
import com.autel.common.model.splitscreen.AircraftScreenState
import com.autel.common.model.splitscreen.toLensTypeEnum
import com.autel.common.utils.DeviceUtils
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.setting.R
import com.autel.widget.widget.compass.Compass3DScaleView
import com.autel.widget.widget.compass.LoopScaleView
import com.autel.widget.widget.compass.ShootingTargetInfoView


class CompassViewModel : ViewModel() {

    private var compassSwitchStatus = hashMapOf<Int, Boolean>()

    /**
     * 查询指南针的开启状态
     */
    fun queryCompassStatus(drone: IAutelDroneDevice): Boolean {
        return compassSwitchStatus[drone.deviceNumber()] ?: false
    }

    fun openCompass(mainProvider: IMainProvider, drone: IAutelDroneDevice) {
        compassSwitchStatus[drone.deviceNumber()] = true
        val screenState = mainProvider.getMainHandler().getScreenState()
        screenState.getAllSplitWidgetList().forEach { item ->
            if (item.drone == drone) {
                //找到打开指北针的飞机
                val codecProvider = item.screen?.getCodecProvider() ?: return@forEach
                //3D指北针
                if (!codecProvider.getCodecScreenLayoutManager()
                        .hasViewInCodecStreamLayout(CodecLayoutType.COMPASS3D) && item != screenState.bottomLeftScreenType
                ) {
                    val compassView = Compass3DScaleView(
                        mainProvider.getMainContext()
                    )
                    compassView.updateLensInfo(drone, item.getGimbalTypeEnum(), item.widgetType.toLensTypeEnum())
                    codecProvider.getCodecScreenLayoutManager().addViewToCodecStreamLayout(
                        CodecLayoutType.COMPASS3D,
                        compassView,
                        FrameLayout.LayoutParams(
                            LayoutParams.MATCH_PARENT,
                            LayoutParams.MATCH_PARENT
                        )
                    )
                }

                (codecProvider.getCodecScreenLayoutManager()
                    .getViewInCodecStreamLayout(CodecLayoutType.COMPASS3D)
                        as? Compass3DScaleView)?.updateTouchEnable(AppInfoManager.isSupportPreciseCalibration() && screenState.fullScreenType == item)
                codecProvider.getCodecScreenLayoutManager().getViewInCodecStreamLayout(CodecLayoutType.COMPASS3D)?.let {
                    it.isVisible = item != screenState.bottomLeftScreenType
                }


                if (item == screenState.fullScreenType) {
                    //刻度尺
                    if (!codecProvider.getCodecScreenLayoutManager().hasViewInCodecLayout(CodecLayoutType.SCALE_VIEW)
                        && AppInfoManager.isSupportPreciseCalibration()
                    ) {
                        val scaleView = LoopScaleView(mainProvider.getMainContext())
                        scaleView.updateLensInfo(drone, item.getGimbalTypeEnum(), item.widgetType.toLensTypeEnum())
                        scaleView.let {
                            var params = LayoutParams(
                                mainProvider.getMainContext().resources.getDimensionPixelSize(R.dimen.common_400dp),
                                mainProvider.getMainContext().resources.getDimensionPixelSize(R.dimen.common_130dp)
                            ).apply {
                                topMargin = mainProvider.getMainContext().resources.getDimensionPixelSize(R.dimen.common_19_5dp)
                                addRule(RelativeLayout.BELOW, codecProvider.getVirtualFunctionView().id)
                                addRule(
                                    RelativeLayout.CENTER_HORIZONTAL,
                                    RelativeLayout.TRUE
                                )
                            }

                            scaleView.setShowItemSize(3)
                            scaleView.setCoursorBitmap(R.drawable.icon_triangle)
                            codecProvider.getCodecScreenLayoutManager().addViewToCodecLayout(
                                CodecLayoutType.SCALE_VIEW,
                                scaleView,
                                params
                            )
                        }
                    }
                    //精准校射信息view
                    if (!codecProvider.getCodecScreenLayoutManager().hasViewInCodecLayout(CodecLayoutType.SHOTTING_VIEW)
                        && AppInfoManager.isSupportPreciseCalibration()
                    ) {
                        val shootingTargetInfoView = ShootingTargetInfoView(mainProvider.getMainContext())
                        shootingTargetInfoView.updateLensInfo(drone, item.getGimbalTypeEnum(), item.widgetType.toLensTypeEnum())
                        shootingTargetInfoView?.let {
                            val shootingParams = LayoutParams(
                                LayoutParams.WRAP_CONTENT,
                                LayoutParams.WRAP_CONTENT
                            )
                            shootingParams.topMargin = mainProvider.getMainContext().resources.getDimensionPixelSize(R.dimen.common_10dp)
                            shootingParams.addRule(RelativeLayout.BELOW, codecProvider.getVirtualFunctionView().id)
                            shootingParams.leftMargin = mainProvider.getMainContext().resources.getDimensionPixelSize(R.dimen.common_80dp)
                            codecProvider.getCodecScreenLayoutManager().addViewToCodecLayout(
                                CodecLayoutType.SHOTTING_VIEW,
                                shootingTargetInfoView,
                                shootingParams
                            )
                        }
                    }
                }

                codecProvider.getCodecScreenLayoutManager().getViewInCodecLayout(CodecLayoutType.SCALE_VIEW)?.isVisible =
                    item == screenState.fullScreenType
                codecProvider.getCodecScreenLayoutManager().getViewInCodecLayout(CodecLayoutType.SHOTTING_VIEW)?.isVisible =
                    item == screenState.fullScreenType

            }
        }
    }

    /**
     * 当状态栏显示隐藏的时候，刻度尺和精准教射数据的位置也要跟着变化
     */
    fun changeScaleShootingPosition(isShow: Boolean, screenState: AircraftScreenState) {
        if (isShow) {
            screenState.getAllSplitWidgetList().forEach {
                val codecProvider = it.screen?.getCodecProvider()
                if (codecProvider == null) {
                    return@forEach
                }
                (codecProvider.getCodecScreenLayoutManager().getViewInCodecLayout(CodecLayoutType.SCALE_VIEW) as? LoopScaleView)?.let {
                    it.updatePosition(0, -it.context.resources.getDimensionPixelSize(R.dimen.common_70dp))
                }
                (codecProvider.getCodecScreenLayoutManager().getViewInCodecLayout(CodecLayoutType.SHOTTING_VIEW) as? ShootingTargetInfoView)?.let {
                    it.updatePosition(0, -it.context.resources.getDimensionPixelSize(R.dimen.common_70dp))
                }
            }
        } else {
            screenState.getAllSplitWidgetList().forEach {
                val codecProvider = it.screen?.getCodecProvider()
                if (codecProvider == null) {
                    return@forEach
                }
                (codecProvider.getCodecScreenLayoutManager().getViewInCodecLayout(CodecLayoutType.SCALE_VIEW) as? LoopScaleView)?.let {
                    it.updatePosition(0, 0)
                }
                (codecProvider.getCodecScreenLayoutManager().getViewInCodecLayout(CodecLayoutType.SHOTTING_VIEW) as? ShootingTargetInfoView)?.let {
                    it.updatePosition(0, 0)
                }
            }
        }
    }


    fun closeCompass(mainProvider: IMainProvider, drone: IAutelDroneDevice) {
        mainProvider.getMainHandler().getPageScreenStore().codecWidgetList.forEach {
            val codecProvider = it.screen?.getCodecProvider()
            val targetDrone = it.drone

            if (targetDrone == drone && codecProvider != null) {
                codecProvider.getCodecScreenLayoutManager().removeViewFromCodecStreamLayout(CodecLayoutType.COMPASS3D, null)
                codecProvider.getCodecScreenLayoutManager().removeViewFromCodecLayout(CodecLayoutType.SCALE_VIEW, null)
                codecProvider.getCodecScreenLayoutManager().removeViewFromCodecLayout(CodecLayoutType.SHOTTING_VIEW, null)
            }
        }
        compassSwitchStatus.put(drone.deviceNumber(), false)
    }

}