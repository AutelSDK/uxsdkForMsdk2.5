package com.autel.widget.widget.compass

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.core.view.updateLayoutParams
import com.autel.aiso.AIJni
import com.autel.common.base.widget.ConstraintLayoutWidget
import com.autel.common.manager.unit.CoordinateUnitEnum
import com.autel.common.manager.unit.UnitManager
import com.autel.common.model.lens.ILens
import com.autel.common.utils.AnimateUtil
import com.autel.common.utils.LatLngUtil
import com.autel.common.utils.TransformUtils
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.module.camera.bean.GimbalTypeEnum
import com.autel.drone.sdk.vmodelx.module.camera.bean.LensTypeEnum
import com.autel.map.bean.AutelLatLng
import com.autel.map.util.MapBoxUtils
import com.autel.utils.ScreenUtils
import com.autel.widget.R
import com.autel.widget.databinding.NorthMissionShottingTargetViewBinding

/**
 * Created by  2022/11/24
 * 激光雷达测距控件
 */
class ShootingTargetInfoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
    ConstraintLayoutWidget(context, attrs, defStyleAttr), ILens {

    private var droneTargetVDistance =
        "${context.getString(R.string.common_text_dron_target_v_distance)} ${context.getString(R.string.common_text_no_value)}"
    private var droneTargetHDistance =
        "${context.getString(R.string.common_text_dron_target_h_distance)} ${context.getString(R.string.common_text_no_value)}"
    private var droneTargetX = "${context.getString(R.string.common_text_target_distance_x)} ${context.getString(R.string.common_text_no_value)}"
    private var droneTargetY = "${context.getString(R.string.common_text_target_distance_y)} ${context.getString(R.string.common_text_no_value)}"
    private var relativeAltitude = "${context.getString(R.string.common_text_abs_hight)} ${context.getString(R.string.common_text_no_value)}"
    private var hostTargetHDistance =
        "${context.getString(R.string.common_text_host_tareget_v_distance)} ${context.getString(R.string.common_text_no_value)}"


    private val binding: NorthMissionShottingTargetViewBinding =
        NorthMissionShottingTargetViewBinding.bind(
            LayoutInflater.from(context)
                .inflate(R.layout.north_mission_shotting_target_view, this, true)
        )


    private val widgetModel: ShootingTargetInfoVM by lazy {
        ShootingTargetInfoVM()
    }


    private fun updateTargetData(
        localDroneTargetVDistance: String,
        localDroneTargetHDistance: String,
        localTargetX: String,
        localTargetY: String,
        localRelativeHeight: String,
        localHostTargetHDistance: String
    ) {
        binding.otvDroneTargetVDistance.text = localDroneTargetVDistance
        binding.otvDroneTargetHDistance.text = localDroneTargetHDistance
        binding.otvDroneTargetX.text = localTargetX
        binding.otvDroneTargetY.text = localTargetY
        binding.otvDroneTargetAbsHigh.text = localRelativeHeight
        binding.otvDroneTargetHostTargetDistance.text = localHostTargetHDistance
    }

    fun updateDroneAndRemotePosition(start: Int, top: Int, end: Int, bottom: Int) {
        binding.root.updateLayoutParams<LayoutParams> {
            marginStart = start
            topMargin = top
            marginEnd = end
            bottomMargin = bottom
        }
    }

    private fun setTargetYVisible(isShow: Boolean) {
        if (isShow) {
            binding.otvDroneTargetY.visibility = VISIBLE
        } else {
            binding.otvDroneTargetY.visibility = GONE
        }
    }

    fun updatePosition(x: Int, y: Int) {
        AnimateUtil.animateProperty(this, "translationX", x.toFloat())
        AnimateUtil.animateProperty(this, "translationY", y.toFloat())
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
        }
        super.onDetachedFromWindow()
    }


    override fun reactToModelChanges() {
        super.reactToModelChanges()
        widgetModel.shootingTargetFlow.subscribe {
            val eul = getEul(it)
            val droneLocation = getRobotGeo(it)
            val homeLocation = getHomeLatLng(it)
            val planeDistance = AIJni.calcPlaneDistance(
                eul,
                getCamera(it),
                droneLocation,
                homeLocation,
                it.laserDistanceM
            )

            droneTargetVDistance =
                "${context.getString(R.string.common_text_dron_target_v_distance)} ${
                    TransformUtils.getDistanceValueWithm(planeDistance.vDist, 1)
                }"
            droneTargetHDistance =
                "${context.getString(R.string.common_text_dron_target_h_distance)} ${
                    TransformUtils.getDistanceValueWithm(planeDistance.hDist, 1)
                }"

            val targetFromScreen = AIJni.targetFromScreen(eul, droneLocation, it.droneAltitude.toDouble(), it.laserDistanceM)
            val targetLatLng = AutelLatLng()
            targetLatLng.altitude = targetFromScreen.ret_alt
            targetLatLng.latitude = targetFromScreen.ret_lat
            targetLatLng.longitude = targetFromScreen.ret_lon

            val autelLatLng = targetLatLng
            val targetXy = LatLngUtil.getLatLngWithUnit(
                autelLatLng.longitude,
                autelLatLng.latitude
            )
            if (UnitManager.getSelectCoordinateUnit() == CoordinateUnitEnum.WGS84_MGRS) {
                setTargetYVisible(false)
                droneTargetX =
                    "${context.getString(R.string.common_text_target_distance)} ${
                        targetXy[0]
                    }"

            } else {
                setTargetYVisible(true)
                droneTargetX =
                    "${context.getString(R.string.common_text_target_distance_x)} ${
                        if (!autelLatLng.isInvalid()) {
                            targetXy[1]
                        } else {
                            context.getString(R.string.common_text_no_value)
                        }

                    }"
                droneTargetY =
                    "${context.getString(R.string.common_text_target_distance_y)} ${
                        if (!autelLatLng.isInvalid()) {
                            targetXy[0]
                        } else {
                            context.getString(R.string.common_text_no_value)
                        }

                    }"
            }
            relativeAltitude = "${context.getString(R.string.common_text_abs_hight)} ${
                TransformUtils.getDistanceValueWithm(
                    it.droneAltitude - planeDistance.vDist,
                    1
                )
            }"


            MapBoxUtils.getDistance(
                AutelLatLng(
                    homeLocation[0],
                    homeLocation[1]
                ), autelLatLng
            )?.let {
                hostTargetHDistance =
                    "${context.getString(R.string.common_text_host_tareget_v_distance)} ${
                        TransformUtils.getDistanceValueWithm(
                            it,
                            1
                        )
                    }"
            }

            updateTargetData(
                droneTargetVDistance,
                droneTargetHDistance,
                droneTargetX,
                droneTargetY,
                relativeAltitude,
                hostTargetHDistance
            )
        }
    }

    private fun getEul(it: ShootingTargetModel): DoubleArray {
        val eul = DoubleArray(3)
        eul[0] = Math.toRadians(it.gimbalAttitudeBean.getYawDegree().toDouble())
        eul[1] = Math.toRadians(it.gimbalAttitudeBean.getPitchDegree().toDouble())
        eul[2] = Math.toRadians(it.gimbalAttitudeBean.getRollDegree().toDouble())
        return eul
    }

    private fun getRobotGeo(it: ShootingTargetModel): DoubleArray {
        val robotGeo = DoubleArray(3)
        robotGeo[0] = it.droneLatitude
        robotGeo[1] = it.droneLongitude
        robotGeo[2] = it.droneAltitude.toDouble()
        return robotGeo
    }

    private fun getCamera(it: ShootingTargetModel): DoubleArray {
        val cameras = DoubleArray(4)
        cameras[0] = it.fovH
        cameras[1] = it.fovV
        cameras[2] = ScreenUtils.getScreenWidth(context).toDouble()
        cameras[3] = ScreenUtils.getScreenHeight(context).toDouble()
        return cameras
    }

    private fun getHomeLatLng(it: ShootingTargetModel): DoubleArray {
        val homeLatLng = DoubleArray(3)
        homeLatLng[0] = it.homeLatitude
        homeLatLng[1] = it.homeLongitude
        homeLatLng[2] = 0.0
        return homeLatLng
    }

    override fun getDrone(): IAutelDroneDevice? {
        return widgetModel.getDrone()
    }

    override fun getGimbal(): GimbalTypeEnum? {
        return widgetModel.getGimbal()
    }

    override fun getLensTypeEnum(): LensTypeEnum? {
        return widgetModel.getLensTypeEnum()
    }

    override fun updateLensInfo(drone: IAutelDroneDevice?, gimbal: GimbalTypeEnum?, lensType: LensTypeEnum?) {
        widgetModel.updateLensInfo(drone, gimbal, lensType)
    }
}