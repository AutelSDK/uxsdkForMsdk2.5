package com.autel.widget.widget.statusbar.manager

import com.autel.common.DroneConst
import com.autel.common.base.BaseApp
import com.autel.common.feature.location.CountryManager
import com.autel.common.manager.AppInfoManager
import com.autel.common.manager.AutelStorageManager
import com.autel.common.manager.LawHeightDelegateManager
import com.autel.common.manager.StorageKey
import com.autel.common.manager.UomReportManager
import com.autel.common.sdk.ModelXDroneConst
import com.autel.common.utils.TransformUtils
import com.autel.drone.sdk.vmodelx.device.DroneType
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.interfaces.IBaseDevice
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.common.enums.WaringIdEnum
import com.autel.widget.R
import com.autel.widget.widget.statusbar.warn.WarningBean


/**
 * Created by gaojie 2022/02/17
 */
open class DroneChecker {
    companion object {
        val mUndefined = WarningBean(
            WaringIdEnum.UNKNOWN,
            WarningBean.WarnLevel.NO_FLY,
            WarningBean.TipType.TipWindow(contentRes = R.string.common_text_atflyremotecustom_item),
            false
        ).apply { markedNew = true }
    }

    fun generate(warningId: WaringIdEnum, isFlying: Boolean, device: IBaseDevice? = null): WarningBean? {
        val droneDevice = device as? IAutelDroneDevice
        return when (warningId) {
            WaringIdEnum.TILT_ANGLE_CONTROL_ABNORMAL -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.HIGH_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_text_tilt_angle_control_abnormal),
                    true
                )
            }

            WaringIdEnum.HEADING_ANGLE_CONTROL_ABNORMAL,
            WaringIdEnum.HORIZONTAL_VELOCITY_CONTROL_ABNORMAL,
            WaringIdEnum.VERTICAL_VELOCITY_CONTROL_ABNORMAL,
            WaringIdEnum.TILT_ANGLE_FUSION_ABNORMAL,
            WaringIdEnum.HEADING_ANGLE_FUSION_ABNORMAL,
            WaringIdEnum.HORIZONTAL_VELOCITY_FUSION_ABNORMAL,
            WaringIdEnum.VERTICAL_VELOCITY_FUSION_ABNORMAL,
                -> {
                WarningBean(
                    WaringIdEnum.HEADING_ANGLE_CONTROL_ABNORMAL,
                    WarningBean.WarnLevel.HIGH_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_text_control_abnormal),
                    true
                )
            }

            WaringIdEnum.BATTERY_ERROR -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.HIGH_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_text_battery_error),
                    true
                )
            }

            WaringIdEnum.BATTERY_DAMAGE,
            WaringIdEnum.BATTERY_INVALID,
                -> {
                val tipId = if (isFlying) R.string.common_text_battery_damage_flying else R.string.common_text_battery_damage
                WarningBean(
                    WaringIdEnum.BATTERY_DAMAGE,
                    WarningBean.WarnLevel.HIGH_TIP,
                    WarningBean.TipType.TipWindow(contentRes = tipId),
                    true
                )
            }

            WaringIdEnum.BATTERY_POWER_OFF -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_text_battery_power_off),
                    true
                )
            }

            WaringIdEnum.BATTERY_TEMPERATURE_LOW -> {
                val tipId = if (isFlying) R.string.common_text_battery_temperature_low_flying else R.string.common_text_battery_temperature_low
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = tipId),
                    true
                )
            }

            WaringIdEnum.BATTERY_TEMPERATURE_HIGH -> {
                val tipId = if (isFlying) R.string.common_text_battery_temperature_high_flying else R.string.common_text_battery_temperature_high
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = tipId),
                    true
                )
            }

            WaringIdEnum.CRITICAL_BATTERY -> {
                WarningBean(
                    warnId = warningId,
                    warnLevel = WarningBean.WarnLevel.HIGH_TIP,
                    tip = WarningBean.TipType.TipDialog(
                        contentRes = R.string.common_text_aircraft_power_seriously_low,
                        msgRes = R.string.common_text_power_seriously_low_auto_landing,
                        rightBtnRes = 0,
                        rightBtnAction = WarningBean.Action.GOLanding,
                        msgTag = ModelXDroneConst.DRONE_CRITICAL_BATTERY_AUTO_TIME,
                        imageRes = R.drawable.common_ic_dialog_waring
                    )
                )
            }

            WaringIdEnum.LOW_BATTERY -> {
                WarningBean(
                    warnId = warningId,
                    warnLevel = WarningBean.WarnLevel.MIDDLE_TIP,
                    tip = WarningBean.TipType.TipDialog(
                        contentRes = R.string.common_text_aircraft_power_low,
                        msgRes = R.string.common_text_aircraft_power_low_auto_go_home,
                        leftBtnRes = R.string.common_text_cancel,
                        leftBtnAction = WarningBean.Action.CANCEL_RETURNLAND,
                        rightBtnRes = R.string.common_text_go_home,
                        rightBtnAction = WarningBean.Action.RETURNLAND,
                        msgTag = ModelXDroneConst.DRONE_LOW_BATTERY_AUTO_TIME
                    )
                )
            }

            WaringIdEnum.INTELLIGENCE_LOW_BATTERY -> {
                WarningBean(
                    warnId = warningId,
                    warnLevel = WarningBean.WarnLevel.MIDDLE_TIP,
                    tip = WarningBean.TipType.TipDialog(
                        contentRes = R.string.common_text_aircraft_intelligent_low_power,
                        msgRes = R.string.common_text_aircraft_intelligent_low_power_auto_go_home,
                        leftBtnRes = R.string.common_text_cancel,
                        leftBtnAction = WarningBean.Action.CANCEL_RETURNLAND,
                        rightBtnRes = R.string.common_text_go_home,
                        rightBtnAction = WarningBean.Action.RETURNLAND,
                        msgTag = ModelXDroneConst.DRONE_LOW_BATTERY_AUTO_TIME
                    )
                )
            }

            WaringIdEnum.BATTERY_VOLTAGE_PRESSURE_DIFFERENTIAL -> {
                val tipId =
                    if (isFlying) R.string.common_text_battery_voltage_pressure_differential_flying else R.string.common_text_battery_voltage_pressure_differential
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.HIGH_TIP,
                    WarningBean.TipType.TipWindow(contentRes = tipId),
                    true
                )
            }

            WaringIdEnum.BATTERY_EXCESSIVE_DISCHARGE -> {
                val tipId =
                    if (isFlying) R.string.common_text_battery_excessive_discharge_flying else R.string.common_text_battery_excessive_discharge
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.HIGH_TIP,
                    WarningBean.TipType.TipWindow(contentRes = tipId),
                    true
                )
            }

            WaringIdEnum.BATTERY_VOLTAGE_NOTCHANGE -> {
                val tipId = if (isFlying) R.string.common_text_battery_voltage_notchange_flying else R.string.common_text_battery_voltage_notchange
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.HIGH_TIP,
                    WarningBean.TipType.TipWindow(contentRes = tipId),
                    true
                )
            }

            WaringIdEnum.ESC_ERROR -> {
                val tipId = if (isFlying) R.string.common_text_esc_exception_flying else R.string.common_text_esc_exception_nofly
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.HIGH_TIP,
                    WarningBean.TipType.TipWindow(contentRes = tipId),
                    true
                )
            }

            WaringIdEnum.ROTOR_PARTIALLY_MOUNTED -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_text_rotor_partially_mounted),
                    true
                )
            }

            WaringIdEnum.APPROACH_UPPER_LIMIT_SPEED -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_text_approach_upper_limit_speed),
                    true
                )
            }

            WaringIdEnum.APPROACH_LOWER_LIMIT_SPEED -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_text_approach_lower_limit_speed),
                    true
                )
            }

            WaringIdEnum.NO_ROTOR_MOUNTED -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_text_no_rotor_mounted),
                    true
                )
            }

            WaringIdEnum.IMU_ABNORMAL -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.HIGH_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_text_imu_error),
                    true
                )
            }

            WaringIdEnum.BAROMETER_ABNORMAL -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.HIGH_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_text_barometer_error),
                    true
                )
            }

            WaringIdEnum.MAGNETOMETER_ABNORMAL -> {
                if (AppInfoManager.isSupportCompassCal()) {
                    WarningBean(
                        warningId,
                        WarningBean.WarnLevel.HIGH_TIP,
                        WarningBean.TipType.TipWindow(contentRes = R.string.common_text_magnetometer_error, action = WarningBean.Action.COMPASS_CALI),
                        true
                    )
                } else {
                    mUndefined
                }
            }

            WaringIdEnum.BEGINNER_MODE_WITHOUT_GPS -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.HIGH_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_text_beginner_no_gps),
                    true
                )
            }

            WaringIdEnum.IMU_CALIBRATION_REQUIRED -> {
                if (AppInfoManager.isSupportIMUCal()) {
                    WarningBean(
                        warningId,
                        WarningBean.WarnLevel.HIGH_TIP,
                        WarningBean.TipType.TipWindow(contentRes = R.string.common_text_calibrate_imu, action = WarningBean.Action.IMU_CALI),
                        true
                    )
                } else {
                    mUndefined
                }
            }

            WaringIdEnum.RC_CALIBRATION_REQUIRED -> {
                WarningBean(
                    warnId = warningId,
                    warnLevel = WarningBean.WarnLevel.HIGH_TIP,
                    tip = WarningBean.TipType.TipDialog(
                        contentRes = R.string.common_text_rc_calibration_tips,
                        msgRes = R.string.common_text_rc_calibration_desc,
                        leftBtnRes = R.string.common_text_cancel,
                        rightBtnRes = R.string.common_text_to_calibrate,
                        rightBtnAction = WarningBean.Action.RC_CALI
                    ),
                    true
                )
            }

            WaringIdEnum.RC_RIGHT_ROCKER_EXCEPTION -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.HIGH_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_text_rc_right_rocker_excepiton, action = WarningBean.Action.RC_CALI),
                    true
                )
            }

            WaringIdEnum.RC_LEFT_ROCKER_EXCEPTION -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.HIGH_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_text_rc_left_rocker_excepiton, action = WarningBean.Action.RC_CALI),
                    true
                )
            }

            WaringIdEnum.RC_RIGHT_WHEEL_EXCEPTION -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.HIGH_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_text_rc_right_wheel_excepiton, action = WarningBean.Action.RC_CALI),
                    true
                )
            }

            WaringIdEnum.RC_LEFT_WHEEL_EXCEPTION -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.HIGH_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_text_rc_left_wheel_excepiton, action = WarningBean.Action.RC_CALI),
                    true
                )
            }

            WaringIdEnum.RC_BATTERY_TEMP_HOT -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.HIGH_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_text_rc_battery_temp_hot),
                    true
                )
            }

            WaringIdEnum.BOOM_NO_REIGN_1 -> {
                if (AppInfoManager.isSupportArmWarn()) {
                    val action = if (droneDevice?.getDroneType() == DroneType.MODEL_H) {
                        WarningBean.Action.SHOW_ARM_UNFOLD_DIALOG
                    } else {
                        null
                    }
                    WarningBean(
                        warningId,
                        WarningBean.WarnLevel.HIGH_TIP,
                        WarningBean.TipType.TipWindow(contentRes = R.string.common_text_boom_no_reign_1, action = action),
                        true
                    )
                } else {
                    mUndefined
                }
            }

            WaringIdEnum.BOOM_NO_REIGN_2 -> {
                if (AppInfoManager.isSupportArmWarn()) {
                    val action = if (droneDevice?.getDroneType() == DroneType.MODEL_H) {
                        WarningBean.Action.SHOW_ARM_UNFOLD_DIALOG
                    } else {
                        null
                    }
                    WarningBean(
                        warningId,
                        WarningBean.WarnLevel.HIGH_TIP,
                        WarningBean.TipType.TipWindow(contentRes = R.string.common_text_boom_no_reign_2, action = action),
                        true
                    )
                } else {
                    mUndefined
                }
            }

            WaringIdEnum.BOOM_NO_REIGN_3 -> {
                if (AppInfoManager.isSupportArmWarn()) {
                    val action = if (droneDevice?.getDroneType() == DroneType.MODEL_H) {
                        WarningBean.Action.SHOW_ARM_UNFOLD_DIALOG
                    } else {
                        null
                    }
                    WarningBean(
                        warningId,
                        WarningBean.WarnLevel.HIGH_TIP,
                        WarningBean.TipType.TipWindow(contentRes = R.string.common_text_boom_no_reign_3, action = action),
                        true
                    )
                } else {
                    mUndefined
                }
            }

            WaringIdEnum.BOOM_NO_REIGN_4 -> {
                if (AppInfoManager.isSupportArmWarn()) {
                    val action = if (droneDevice?.getDroneType() == DroneType.MODEL_H) {
                        WarningBean.Action.SHOW_ARM_UNFOLD_DIALOG
                    } else {
                        null
                    }
                    WarningBean(
                        warningId,
                        WarningBean.WarnLevel.HIGH_TIP,
                        WarningBean.TipType.TipWindow(contentRes = R.string.common_text_boom_no_reign_4, action = action),
                        true
                    )
                } else {
                    mUndefined
                }
            }

            WaringIdEnum.BATTERY_IS_INVALID -> {
                if (AppInfoManager.isSupportBatteryInvalidWarn() && !isFlying) {
                    WarningBean(
                        warningId,
                        WarningBean.WarnLevel.HIGH_TIP,
                        WarningBean.TipType.TipWindow(contentRes = R.string.common_text_battery_invalied),
                        true
                    )
                } else {
                    mUndefined
                }
            }

            WaringIdEnum.BATTERY_DATA_ABNORMAL -> {
                if (AppInfoManager.isSupportBatteryDataErrorWarn() && !isFlying) {
                    WarningBean(
                        warningId,
                        WarningBean.WarnLevel.HIGH_TIP,
                        WarningBean.TipType.TipWindow(contentRes = R.string.common_text_battery_data_exception),
                        true
                    )
                } else {
                    mUndefined
                }
            }

            WaringIdEnum.BATTERY_NOT_MATCHED -> {
                if (AppInfoManager.isSupportBatteryNotMatchWarn() && !isFlying) {
                    WarningBean(
                        warningId,
                        WarningBean.WarnLevel.HIGH_TIP,
                        WarningBean.TipType.TipWindow(contentRes = R.string.common_text_battery_not_match),
                        true
                    )
                } else {
                    mUndefined
                }
            }

            //IMU温升告警
            WaringIdEnum.IMU_HEATING -> {
                if (!isFlying) {
                    WarningBean(
                        warningId,
                        WarningBean.WarnLevel.HIGH_TIP,
                        WarningBean.TipType.TipWindow(contentRes = R.string.common_text_imu_heating),
                        true
                    )
                } else {
                    mUndefined
                }
            }

            WaringIdEnum.AIRCRAFT_DISCONNECT -> {
                val isShowPair = AutelStorageManager.getPlainStorage().getBooleanValue(StorageKey.PlainKey.KEY_FIRST_CONNECT_AIRCRAFT)
                val msgId = if (isShowPair) R.string.common_text_aircraft_disconnect else R.string.common_text_open_or_connect_aircraft
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = msgId, action = WarningBean.Action.CONNECTING_AIRCRAFT),
                    true
                )
            }

            WaringIdEnum.MAGNETOMETER_CALIBRATION_REQUIRED -> {
                if (AppInfoManager.isSupportCompassCal()) {
                    WarningBean(
                        warningId,
                        WarningBean.WarnLevel.HIGH_TIP,
                        WarningBean.TipType.TipWindow(
                            contentRes = R.string.common_text_calibrate_magnetometer,
                            action = WarningBean.Action.COMPASS_CALI
                        ),
                        true
                    )
                } else {
                    mUndefined
                }
            }

            WaringIdEnum.RTK_NOT_READY -> {
                if (isFlying) {
                    WarningBean(
                        warningId,
                        WarningBean.WarnLevel.MIDDLE_TIP,
                        WarningBean.TipType.TipWindow(contentRes = R.string.common_text_rtk_not_ready_flying),
                        true
                    )
                } else {
                    WarningBean(
                        warningId,
                        WarningBean.WarnLevel.MIDDLE_TIP,
                        WarningBean.TipType.TipWindow(contentRes = R.string.common_text_rtk_not_ready),
                        true
                    )
                }
            }

            WaringIdEnum.GIMBAL_NOT_READY -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_text_gimbal_not_ready),
                    true
                )
            }

            WaringIdEnum.MAGNETOMETER_CALIBRATING -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_text_magnetometer_calibrating),
                    true
                )
            }

            WaringIdEnum.TILT_OVER -> WarningBean(
                warningId,
                WarningBean.WarnLevel.HIGH_TIP,
                WarningBean.TipType.TipWindow(contentRes = R.string.common_text_tilt_over),
                true
            )

            WaringIdEnum.DRONE_LOST -> WarningBean(
                warningId,
                WarningBean.WarnLevel.HIGH_TIP,
                WarningBean.TipType.TipWindow(contentRes = R.string.common_text_drone_lost),
                true
            )

            WaringIdEnum.HIT_DETECTED -> WarningBean(
                warningId,
                WarningBean.WarnLevel.HIGH_TIP,
                WarningBean.TipType.TipWindow(contentRes = R.string.common_text_hit_detected),
                true
            )

            WaringIdEnum.NFZ -> WarningBean(
                warningId,
                WarningBean.WarnLevel.HIGH_TIP,
                WarningBean.TipType.TipWindow(contentRes = R.string.common_text_nfz),
                true
            )

            WaringIdEnum.REACH_MAX_HEIGHT_LIMIT_ZONE -> WarningBean(
                warningId,
                WarningBean.WarnLevel.MIDDLE_TIP,
                WarningBean.TipType.TipToast(contentRes = R.string.common_text_reach_max_height_limit_zone),
                false
            )

            WaringIdEnum.REACH_MAX_DISTANCE_OF_GEO_FENCE -> WarningBean(
                warningId,
                WarningBean.WarnLevel.MIDDLE_TIP,
                WarningBean.TipType.TipToast(contentRes = R.string.common_text_max_distance),
                false
            )

            WaringIdEnum.REACH_MAX_HEIGHT_OF_GEO_FENCE -> WarningBean(
                warningId,
                WarningBean.WarnLevel.MIDDLE_TIP,
                WarningBean.TipType.TipToast(contentRes = R.string.common_text_max_height),
                false
            )

            WaringIdEnum.UPGRADING -> WarningBean(
                warningId,
                WarningBean.WarnLevel.MIDDLE_TIP,
                WarningBean.TipType.TipToast(contentRes = R.string.common_text_upgrading),
                false
            )

            WaringIdEnum.ATTITUDE_INITIALIZING -> WarningBean(
                warningId,
                WarningBean.WarnLevel.MIDDLE_TIP,
                WarningBean.TipType.TipWindow(contentRes = R.string.common_text_attitude_initializing),
                true
            )

            WaringIdEnum.UAV_NOT_ACTIVATED -> WarningBean(
                warningId,
                WarningBean.WarnLevel.HIGH_TIP,
                WarningBean.TipType.TipWindow(contentRes = R.string.common_text_uav_not_activated),
                true
            )

            WaringIdEnum.LARGE_WIND_WARNING -> WarningBean(
                warningId,
                WarningBean.WarnLevel.HIGH_TIP,
                WarningBean.TipType.TipWindow(contentRes = R.string.common_text_large_wind_warning),
                true
            )

            WaringIdEnum.HOME_POINT_NOT_GOOD -> WarningBean(
                warningId,
                WarningBean.WarnLevel.MIDDLE_TIP,
                WarningBean.TipType.TipToast(contentRes = R.string.common_text_home_point_not_good),
                true
            )

            WaringIdEnum.GPS_WEAK -> WarningBean(
                warningId,
                WarningBean.WarnLevel.MIDDLE_TIP,
                WarningBean.TipType.TipToast(contentRes = R.string.common_text_gps_signal_weak_1),
                true
            )

            WaringIdEnum.GPS_ENSNARE -> WarningBean(
                warningId,
                WarningBean.WarnLevel.MIDDLE_TIP,
                WarningBean.TipType.TipWindow(contentRes = R.string.common_text_gnss_deception_prompt),
                true
            )

            WaringIdEnum.LOW_BATTERY_AND_RC_LOST,
            WaringIdEnum.RC_LOST,
                -> WarningBean(
                warningId,
                WarningBean.WarnLevel.HIGH_TIP,
                WarningBean.TipType.TipWindow(contentRes = R.string.common_text_aircraft_disconnect),
                true
            )

            WaringIdEnum.GO_HOME_LOW_BATTERY -> WarningBean(
                warningId,
                WarningBean.WarnLevel.MIDDLE_TIP,
                WarningBean.TipType.TipWindow(contentRes = R.string.common_text_low_battery_return),
                true
            )

            WaringIdEnum.ALTERNATIVE_LANDING -> WarningBean(
                warningId,
                WarningBean.WarnLevel.MIDDLE_TIP,
                WarningBean.TipType.TipWindow(contentRes = R.string.common_text_alternative_landing),
                true
            )

            WaringIdEnum.REMOTE_LANDING,
            WaringIdEnum.FROM_NAV_CMD,
            WaringIdEnum.CANCEL_WAYPOINT_MISSION_AND_RETURN,
            WaringIdEnum.ABNORMAL_VOLTAGE,
            WaringIdEnum.FROM_RC_BUTTON,
                -> WarningBean(
                WaringIdEnum.REMOTE_LANDING,
                WarningBean.WarnLevel.MIDDLE_TIP,
                WarningBean.TipType.TipToast(contentRes = R.string.common_text_gohome_button_rc),
                true
            )

            WaringIdEnum.NO_FLY_ZONE -> WarningBean(
                warningId,
                WarningBean.WarnLevel.MIDDLE_TIP,
                WarningBean.TipType.TipWindow(contentRes = R.string.common_text_gohome_nofly_zone),
                true
            )

            WaringIdEnum.WAYPOINT_MISSION_FINISHED -> WarningBean(
                warningId,
                WarningBean.WarnLevel.MIDDLE_TIP,
                WarningBean.TipType.TipToast(contentRes = R.string.common_text_gohome_waypoint_finish),
                true
            )

            /* WaringIdEnum.NO_REACTION -> WarningBean(
                 warningId,
                 WarningBean.WarnLevel.MIDDLE_TIP,
                 WarningBean.TipType.TipWindow(contentRes = R.string.common_text_no_reaction),
                 true
             )*/

            WaringIdEnum.EXIT_MISSION_COMPLETED -> WarningBean(
                warningId,
                WarningBean.WarnLevel.MIDDLE_TIP,
                WarningBean.TipType.TipToast(contentRes = R.string.common_text_route_task_has_been_completed),
                true
            )

            WaringIdEnum.EXIT_MISSION_OBJECT_LOSING -> WarningBean(
                warningId,
                WarningBean.WarnLevel.MIDDLE_TIP,
                WarningBean.TipType.TipToast(contentRes = R.string.common_text_exit_lost_obj),
                true
            )

            WaringIdEnum.USER_MANIPULATION_RC -> WarningBean(
                warningId,
                WarningBean.WarnLevel.MIDDLE_TIP,
                WarningBean.TipType.TipWindow(contentRes = R.string.common_text_exit_user_rc),
                true
            )

            WaringIdEnum.USER_MANIPULATION_APP -> WarningBean(
                warningId,
                WarningBean.WarnLevel.MIDDLE_TIP,
                WarningBean.TipType.TipWindow(contentRes = R.string.common_text_exit_user_app),
                true
            )

            WaringIdEnum.EXIT_MISSION_INTERRUPT,
            WaringIdEnum.EXIT_MISSION_SPECIAL_PAUSE,
            WaringIdEnum.EXIT_MISSION_NFZ,
            WaringIdEnum.EXIT_MISSION_GEOFENCING,
            WaringIdEnum.EXIT_MISSION_ROTOR_LOCKED,
            WaringIdEnum.EXIT_MISSION_OBSTACLE_AVOIDING,
            WaringIdEnum.EXIT_MISSION_VISION_MODE_TER,
            WaringIdEnum.EXIT_MISSION_STARPOINT_MODE,
            WaringIdEnum.EXIT_MISSION_ATTI_MODE,
            WaringIdEnum.EXIT_MISSION_NOT_TAKE_OFF,
            WaringIdEnum.EXIT_MISSION_TRACK_MODE,
                -> WarningBean(
                WaringIdEnum.EXIT_MISSION_INTERRUPT,
                WarningBean.WarnLevel.MIDDLE_TIP,
                WarningBean.TipType.TipToast(contentRes = R.string.common_text_exit_completed),
                true
            )

            WaringIdEnum.GIMBAL_STALLING -> WarningBean(
                warningId,
                WarningBean.WarnLevel.MIDDLE_TIP,
                WarningBean.TipType.TipWindow(contentRes = R.string.common_text_gim_joint_stalled),
                true
            )

            WaringIdEnum.GIMBAL_IMU_NOT_CALIBRATED -> {
                if (AppInfoManager.isSupportIMUCal()) {
                    WarningBean(
                        warningId,
                        WarningBean.WarnLevel.MIDDLE_TIP,
                        WarningBean.TipType.TipWindow(contentRes = R.string.common_text_calibrate_imu, action = WarningBean.Action.IMU_CALI),
                        true
                    )
                } else {
                    mUndefined
                }
            }

            WaringIdEnum.GIMBAL_IMU_FAULT -> WarningBean(
                warningId,
                WarningBean.WarnLevel.MIDDLE_TIP,
                WarningBean.TipType.TipWindow(contentRes = R.string.common_text_gim_imu_error),
                true
            )

            WaringIdEnum.GIMBAL_OVERHEAT -> WarningBean(
                warningId,
                WarningBean.WarnLevel.HIGH_TIP,
                WarningBean.TipType.TipWindow(contentRes = R.string.common_text_gim_over_temper),
                true
            )

            WaringIdEnum.GIMBAL_AND_UAV_COMMUNICATION_DISCONNECT -> WarningBean(
                warningId,
                WarningBean.WarnLevel.HIGH_TIP,
                WarningBean.TipType.TipWindow(contentRes = R.string.common_text_gimbal_and_uav_communication_disconnect),
                true
            )

            WaringIdEnum.GIMBAL_MOTOR_SELF_CHECKING_FAILURE -> WarningBean(
                warningId,
                WarningBean.WarnLevel.MIDDLE_TIP,
                WarningBean.TipType.TipWindow(contentRes = R.string.common_text_gim_cali_fail),
                true
            )

            WaringIdEnum.LASER_MODULE_FAILURE -> WarningBean(
                warningId,
                WarningBean.WarnLevel.MIDDLE_TIP,
                WarningBean.TipType.TipWindow(contentRes = R.string.common_text_gim_laser_error),
                true
            )

            WaringIdEnum.GIMBAL_MOTOR_REACH_LIMIT -> WarningBean(
                warningId,
                WarningBean.WarnLevel.MIDDLE_TIP,
                WarningBean.TipType.TipWindow(contentRes = R.string.common_text_gim_range_limit),
                true
            )

            WaringIdEnum.GIMBAL_MOTOR_NOT_CALIBRATED -> WarningBean(
                warningId,
                WarningBean.WarnLevel.HIGH_TIP,
                WarningBean.TipType.TipWindow(contentRes = R.string.common_text_gim_motor_no_cali),
                true
            )

            WaringIdEnum.RAW_IMAGE_ERROR,
            WaringIdEnum.DISTORD_IMAGE_ERROR,
            WaringIdEnum.RECTIFY_IMAGE_ERROR,
            WaringIdEnum.POINT_CLOUD_IMAGE_ERROR,
            WaringIdEnum.CAMERA_IMAGE_ERROR,
                -> WarningBean(
                WaringIdEnum.CAMERA_IMAGE_ERROR,
                WarningBean.WarnLevel.HIGH_TIP,
                WarningBean.TipType.TipWindow(contentRes = R.string.common_text_abnormal_visual_module),
                true
            )

            WaringIdEnum.VISION_INIT_ERROR -> WarningBean(
                warningId,
                WarningBean.WarnLevel.HIGH_TIP,
                WarningBean.TipType.TipWindow(contentRes = R.string.common_text_abnormal_visual_oa_slam),
                true
            )

            WaringIdEnum.DISTORDRESIZE_IMAGE_ERROR -> WarningBean(
                warningId,
                WarningBean.WarnLevel.HIGH_TIP,
                WarningBean.TipType.TipWindow(contentRes = R.string.common_text_abnormal_visual_slam),
                true
            )

            WaringIdEnum.BRIGHTNESS_LOWW_PERCEPTION_CLOSE -> {
                val id =
                    if (AppInfoManager.isSupport6RadarWarn()) {
                        R.string.common_text_brightness_loww_perception_close_6
                    } else if (AppInfoManager.isModelS()) {
                        R.string.common_text_brightness_loww_perception
                    } else {
                        R.string.common_text_brightness_loww_perception_close
                    }
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = id),
                    true
                )
            }

            WaringIdEnum.SD_CARD_ERROR -> WarningBean(
                warningId,
                WarningBean.WarnLevel.HIGH_TIP,
                WarningBean.TipType.TipWindow(contentRes = R.string.common_text_sd_card_error),
                true
            )

            WaringIdEnum.SD_CARD_FULL -> WarningBean(
                warningId,
                WarningBean.WarnLevel.HIGH_TIP,
                WarningBean.TipType.TipWindow(contentRes = R.string.common_text_sd_card_full),
                true
            )

            WaringIdEnum.UFS_FULL -> WarningBean(
                warningId,
                WarningBean.WarnLevel.HIGH_TIP,
                WarningBean.TipType.TipWindow(contentRes = R.string.common_text_ufs_full),
                true
            )

            WaringIdEnum.MAIN_CAMERA_DETECT_FAIL -> WarningBean(
                warningId,
                WarningBean.WarnLevel.MIDDLE_TIP,
                WarningBean.TipType.TipWindow(contentRes = R.string.common_text_wide_abnormality),
                true
            )

            WaringIdEnum.LONG_FOCAL_CAMERA_DETECT_FAIL -> WarningBean(
                warningId,
                WarningBean.WarnLevel.MIDDLE_TIP,
                WarningBean.TipType.TipWindow(contentRes = R.string.common_text_abnormal_telephoto_lens),
                true
            )

            WaringIdEnum.UPPER_AND_LOWER_VISION_DETECT_FAIL -> WarningBean(
                warningId,
                WarningBean.WarnLevel.HIGH_TIP,
                WarningBean.TipType.TipWindow(contentRes = R.string.common_text_upper_and_lower_vision_detect_fail),
                true
            )

            WaringIdEnum.FRONT_AND_BACK_VISION_DETECT_FAIL -> WarningBean(
                warningId,
                WarningBean.WarnLevel.HIGH_TIP,
                WarningBean.TipType.TipWindow(contentRes = R.string.common_text_front_and_back_vision_detect_fail),
                true
            )

            WaringIdEnum.F401_CHECK_VERSION_FAIL -> WarningBean(
                warningId,
                WarningBean.WarnLevel.MIDDLE_TIP,
                WarningBean.TipType.TipWindow(contentRes = R.string.common_text_zoom_abnormality),
                true
            )

            WaringIdEnum.ZOOM_LENS_ZOOM_ERROR -> WarningBean(
                warningId,
                WarningBean.WarnLevel.MIDDLE_TIP,
                WarningBean.TipType.TipWindow(contentRes = R.string.common_text_abnormal_telephoto_lens),
                true
            )

            WaringIdEnum.ZOOM_LENS_FOCUS_ERROR -> WarningBean(
                WaringIdEnum.ZOOM_LENS_FOCUS_ERROR,
                WarningBean.WarnLevel.MIDDLE_TIP,
                WarningBean.TipType.TipWindow(contentRes = R.string.common_text_zoom_lens_error),
                true
            )

            WaringIdEnum.F401_FIRMWARE_UPDATE_FAIL -> WarningBean(
                warningId,
                WarningBean.WarnLevel.MIDDLE_TIP,
                WarningBean.TipType.TipWindow(contentRes = R.string.common_text_f401_firmware_update_fail),
                true
            )

            WaringIdEnum.INFRARED_CAMERA_OPEN_FAIL -> WarningBean(
                warningId,
                WarningBean.WarnLevel.MIDDLE_TIP,
                WarningBean.TipType.TipWindow(contentRes = R.string.common_text_infrared_camera_open_fail),
                true
            )

            WaringIdEnum.RADAR_OVERHEATING -> WarningBean(
                warningId,
                WarningBean.WarnLevel.MIDDLE_TIP,
                WarningBean.TipType.TipWindow(contentRes = R.string.common_text_radar_overheating),
                true
            )

            WaringIdEnum.RADAR_SELF_CHECK_ERROR -> WarningBean(
                warningId,
                WarningBean.WarnLevel.HIGH_TIP,
                WarningBean.TipType.TipWindow(contentRes = R.string.common_text_radar_self_check_error),
                true
            )

            WaringIdEnum.RADAR_PLL_ERROR,
            WaringIdEnum.RADAR_COMMUNICATION_ERROR,
                -> WarningBean(
                WaringIdEnum.RADAR_PLL_ERROR,
                WarningBean.WarnLevel.MIDDLE_TIP,
                WarningBean.TipType.TipWindow(contentRes = R.string.common_text_radar_done_error),
                true
            )

            WaringIdEnum.RADAR_DONE_TIMEOUT -> WarningBean(
                WaringIdEnum.RADAR_DONE_TIMEOUT,
                WarningBean.WarnLevel.HIGH_TIP,
                WarningBean.TipType.TipWindow(contentRes = R.string.common_text_radar_pll_error),
                true
            )

            WaringIdEnum.PPS_SIGNAL_ERROR -> WarningBean(
                warningId,
                WarningBean.WarnLevel.MIDDLE_TIP,
                WarningBean.TipType.TipWindow(contentRes = R.string.common_text_pps_signal_error),
                true
            )

            WaringIdEnum.MISSION_MANAGER_ERROR -> WarningBean(
                warningId,
                WarningBean.WarnLevel.MIDDLE_TIP,
                WarningBean.TipType.TipToast(contentRes = R.string.common_text_mission_manager_error),
                true
            )

            WaringIdEnum.MISSION_MANAGER_ANGULAR_ANOMALY -> WarningBean(
                warningId,
                WarningBean.WarnLevel.MIDDLE_TIP,
                WarningBean.TipType.TipToast(contentRes = R.string.common_text_mission_manager_angular_anomaly),
                true
            )

            WaringIdEnum.MISSION_MANAGER_HEIGHT_ANOMALY -> WarningBean(
                warningId,
                WarningBean.WarnLevel.MIDDLE_TIP,
                WarningBean.TipType.TipToast(contentRes = R.string.common_text_mission_manager_height_anomaly),
                true
            )

            WaringIdEnum.MISSION_MANAGER_FLY_TO_NOFLY_ZONE -> WarningBean(
                warningId,
                WarningBean.WarnLevel.MIDDLE_TIP,
                WarningBean.TipType.TipToast(contentRes = R.string.common_text_mission_manager_fly_to_nofly_zone),
                true
            )

            WaringIdEnum.MISSION_MANAGER_LASER_ANOMALY -> WarningBean(
                warningId,
                WarningBean.WarnLevel.MIDDLE_TIP,
                WarningBean.TipType.TipToast(contentRes = R.string.common_text_mission_manager_laser_anomaly),
                true
            )

            WaringIdEnum.GPS_SUFFERS_INTERFERENCE,
            WaringIdEnum.RADIO_FREQUENCY_INTERFERENCE,
                -> WarningBean(
                WaringIdEnum.GPS_SUFFERS_INTERFERENCE,
                WarningBean.WarnLevel.MIDDLE_TIP,
                WarningBean.TipType.TipToast(contentRes = R.string.common_text_aircraft_signal_disturbed),
                true
            )

            WaringIdEnum.MISSION_FLIGHT_ANOMALY,
            WaringIdEnum.FLIGHT_CONTROL_GATEWAY_ABNORMAL,
                -> WarningBean(
                WaringIdEnum.MISSION_FLIGHT_ANOMALY,
                WarningBean.WarnLevel.MIDDLE_TIP,
                WarningBean.TipType.TipWindow(contentRes = R.string.common_text_the_flight_mission_is_abnormal),
                true
            )

            WaringIdEnum.MIF_VISUAL_LOCALIZATION_FAILURE -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_text_visual_position_off),
                    true
                )
            }

            WaringIdEnum.EMM_EXIT_RETURN_HOME_OBS -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_text_planner_error),
                    true
                )
            }

            WaringIdEnum.EMM_EXIT_SEMI_AUTO_OBS -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_text_visual_point_cloud_error),
                    false
                )
            }
            /*WaringIdEnum.CAMERA_DEMIST -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_text_rain_pollution_warn),
                    true
                )
            }*/
            /*WaringIdEnum.DIRTY_UPPER_LENS -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_text_upper_lens_dirty),
                    true
                )
            }*/
            WaringIdEnum.DIRTY_DOWN_LENS -> {
                if (AppInfoManager.isModelH()) {
                    WarningBean(
                        warningId,
                        WarningBean.WarnLevel.MIDDLE_TIP,
                        WarningBean.TipType.TipWindow(contentRes = R.string.common_text_down_lens_dirty),
                        true
                    )
                } else {
                    mUndefined
                }

            }

            WaringIdEnum.DIRTY_FRONT_LENS -> {
                if (AppInfoManager.isModelH()) {
                    WarningBean(
                        warningId,
                        WarningBean.WarnLevel.MIDDLE_TIP,
                        WarningBean.TipType.TipWindow(contentRes = R.string.common_text_front_lens_dirty),
                        true
                    )
                } else {
                    mUndefined
                }
            }

            WaringIdEnum.DIRTY_REAR_LENS -> {
                if (AppInfoManager.isModelH()) {
                    WarningBean(
                        warningId,
                        WarningBean.WarnLevel.MIDDLE_TIP,
                        WarningBean.TipType.TipWindow(contentRes = R.string.common_text_rear_lens_dirty),
                        true
                    )
                } else {
                    mUndefined
                }
            }

            WaringIdEnum.DIRTY_LEFT_LENS -> {
                if (AppInfoManager.isModelH()) {
                    WarningBean(
                        warningId,
                        WarningBean.WarnLevel.MIDDLE_TIP,
                        WarningBean.TipType.TipWindow(contentRes = R.string.common_text_left_lens_dirty),
                        true
                    )
                } else {
                    mUndefined
                }
            }

            WaringIdEnum.DIRTY_RIGHT_LENS -> {
                if (AppInfoManager.isModelH()) {
                    WarningBean(
                        warningId,
                        WarningBean.WarnLevel.MIDDLE_TIP,
                        WarningBean.TipType.TipWindow(contentRes = R.string.common_text_right_lens_dirty),
                        true
                    )
                } else {
                    mUndefined
                }
            }

            WaringIdEnum.RTK_NOT_FIX -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_text_rtk_signal_weak),
                    true
                )
            }

            WaringIdEnum.RADAR_NOT_CALIB -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_text_radar_not_calibrated_24),
                    true
                )
            }

            WaringIdEnum.RADAR_NOT_NIGHT -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_text_radar_not_night),
                    true
                )
            }

            WaringIdEnum.WARNING_AREA -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_text_warning_area),
                    true
                )
            }

            WaringIdEnum.ENHANCE_WARNING_AREA -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_text_enhance_warning_area),
                    true
                )
            }

            WaringIdEnum.APPROACHING_NO_FLY_ZONE_WARNING -> { // 飞机靠近禁飞区
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.HIGH_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_near_nofly_zone),
                    true
                )
            }

            WaringIdEnum.NEAR_NO_FLY_ZONE_BUFFER_WARNING -> {// 飞机靠近警示区
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_near_warning_area),
                    true
                )
            }

            WaringIdEnum.AIRCRAFT_APPROACHING_NO_FLY_ZONE_WARNING -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipToast(contentRes = R.string.common_text_flyable_area_edge),
                    true
                )
            }

            WaringIdEnum.AIRCRAFT_IN_NO_FLY_ZONE_WARNING -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.HIGH_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_text_flyable_area_outside),
                    true
                )
            }

            WaringIdEnum.AIRCRAFT_RETURN_TO_NO_FLY_ZONE_WARNING -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.HIGH_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_text_flyable_area_outside_return_home),
                    true
                )
            }

            WaringIdEnum.AIRCRAFT_RETURN_FROM_NO_FLY_ZONE_WARNING -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.HIGH_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_text_flyable_area_outside_return_home),
                    true
                )
            }
            // 各种锁桨原因
            /* 暂时不实现
            WaringIdEnum.BATTERY_NOT_IN_POSITION->{
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_text_enhance_warning_area),
                    true
                )
            }
            WaringIdEnum.LEFT_HEAD_ARM_NOT_IN_POSITION ->{
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_text_enhance_warning_area),
                    true
                )
            }
            WaringIdEnum.RIGHT_HEAD_ARM_NOT_IN_POSITION ->{
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_text_enhance_warning_area),
                    true
                )
            }
            WaringIdEnum.LEFT_TAIL_ARM_NOT_IN_POSITION->{
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_text_enhance_warning_area),
                    true
                )
            }
            WaringIdEnum.RIGHT_TAIL_ARM_NOT_IN_POSITION->{
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_text_enhance_warning_area),
                    true
                )
            }*/
            /*WaringIdEnum.BATTERY_IS_INVALID->{
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_text_enhance_warning_area),
                    true
                )
            }
            WaringIdEnum.BATTERY_DATA_ABNORMAL->{
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_text_enhance_warning_area),
                    true
                )
            }
            WaringIdEnum.BATTERY_NOT_MATCHED->{
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_text_enhance_warning_area),
                    true
                )
            }*/
            WaringIdEnum.REMOTE_ID_INVALID -> {
                WarningBean(
                    warnId = warningId,
                    warnLevel = WarningBean.WarnLevel.HIGH_TIP,
                    tip = WarningBean.TipType.TipDialog(
                        contentRes = R.string.common_text_input_rid_msg,
                        msgRes = R.string.common_text_input_rid_msg_content,
                        rightBtnRes = R.string.common_text_to_fill_in,
                        rightBtnAction = WarningBean.Action.RID_MSG,
                        leftBtnRes = R.string.common_text_cancel,
                        imageRes = R.drawable.common_ic_dialog_waring
                    )
                )
            }

            WaringIdEnum.HIGH_ESC_TEMPERATURE -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.HIGH_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_text_esc_board_is_too_high),
                    true
                )
            }

            WaringIdEnum.LOW_BATTERY_VOLTAGE -> {
                val id = if (AppInfoManager.isModelH()) {
                    R.string.common_text_battery_voltage_is_low_h
                } else if (AppInfoManager.isModelM()) {
                    R.string.common_text_battery_voltage_is_low_m
                } else {
                    R.string.common_text_battery_voltage_is_low
                }
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = id),
                    true
                )
            }

            WaringIdEnum.LOW_BATTERY_VOLTAGE_NEXT -> {
                if (AppInfoManager.isModelH()) {
                    WarningBean(
                        warningId,
                        WarningBean.WarnLevel.MIDDLE_TIP,
                        WarningBean.TipType.TipWindow(contentRes = R.string.common_text_battery_voltage_is_low_h_next),
                        true
                    )
                }else if (AppInfoManager.isModelM()) {
                    WarningBean(
                        warningId,
                        WarningBean.WarnLevel.MIDDLE_TIP,
                        WarningBean.TipType.TipWindow(contentRes = R.string.common_text_battery_voltage_is_low_m_next),
                        true
                    )
                } else {
                    mUndefined
                }
            }

            WaringIdEnum.HIGH_TAKEOFF_POWER -> {
                if (AppInfoManager.isSupportOverloadedWarn()) {
                    WarningBean(
                        warningId,
                        WarningBean.WarnLevel.HIGH_TIP,
                        WarningBean.TipType.TipWindow(contentRes = R.string.common_text_overloaded),
                        true
                    )
                } else {
                    mUndefined
                }
            }

            WaringIdEnum.HIGH_FLIGHT_POWER -> {
                if (AppInfoManager.isSupportOverloadedTipsWarn()) {
                    WarningBean(
                        warningId,
                        WarningBean.WarnLevel.MIDDLE_TIP,
                        WarningBean.TipType.TipWindow(contentRes = R.string.common_text_overloaded_tips),
                        true
                    )
                } else {
                    mUndefined
                }
            }

            WaringIdEnum.LOW_POWER_MODE_INFRARED_OFF -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_text_low_power_tips),
                    true
                )
            }

            WaringIdEnum.BATTERY_LF_TEMPERATURE_TOO_HIGH -> {
                if (AppInfoManager.isSupportBatteryDetailWarn()) {
                    val id = if (AppInfoManager.isModelM()) {
                        if (isFlying) R.string.common_text_battery_tem_too_high_mf_flying else R.string.common_text_battery_tem_too_high_mf
                    } else if (AppInfoManager.isModelH()) {
                        if (isFlying) R.string.common_text_battery_tem_too_high_hl_flying else R.string.common_text_battery_tem_too_high_hl
                    } else {
                        //走默认的，包括S
                        if (isFlying) R.string.common_text_battery_temperature_high_flying else R.string.common_text_battery_temperature_high
                    }
                    WarningBean(
                        warningId,
                        WarningBean.WarnLevel.HIGH_TIP,
                        WarningBean.TipType.TipWindow(contentRes = id),
                        true
                    )
                } else {
                    mUndefined
                }
            }

            WaringIdEnum.BATTERY_RB_TEMPERATURE_TOO_HIGH -> {
                if (AppInfoManager.isSupportBatteryDetailWarn()) {
                    val id = if (AppInfoManager.isModelM()) {
                        if (isFlying) R.string.common_text_battery_tem_too_high_mb_flying else R.string.common_text_battery_tem_too_high_mb
                    } else {
                        if (isFlying) R.string.common_text_battery_tem_too_high_hr_flying else R.string.common_text_battery_tem_too_high_hr
                    }
                    WarningBean(
                        warningId,
                        WarningBean.WarnLevel.HIGH_TIP,
                        WarningBean.TipType.TipWindow(contentRes = id),
                        true
                    )
                } else {
                    mUndefined
                }
            }

            WaringIdEnum.BATTERY_LF_LIFE_END -> {
                if (AppInfoManager.isSupportBatteryDetailWarn()) {
                    val id = if (AppInfoManager.isModelM()) {
                        R.string.common_text_battery_life_end_mf
                    } else {
                        R.string.common_text_battery_life_end_hl
                    }
                    WarningBean(
                        warningId,
                        WarningBean.WarnLevel.HIGH_TIP,
                        WarningBean.TipType.TipWindow(contentRes = id),
                        true
                    )
                } else {
                    mUndefined
                }
            }

            WaringIdEnum.BATTERY_RB_LIFE_END -> {
                if (AppInfoManager.isSupportBatteryDetailWarn()) {
                    val id = if (AppInfoManager.isModelM()) {
                        R.string.common_text_battery_life_end_mb
                    } else {
                        R.string.common_text_battery_life_end_hr
                    }
                    WarningBean(
                        warningId,
                        WarningBean.WarnLevel.HIGH_TIP,
                        WarningBean.TipType.TipWindow(contentRes = id),
                        true
                    )
                } else {
                    mUndefined
                }
            }

            WaringIdEnum.BATTERY_LF_TEMPERATURE_TOO_LOW,
            WaringIdEnum.BATTERY_LF_AUTO_HEATING,
                -> {
                if (AppInfoManager.isSupportBatteryDetailWarn()) {
                    val id = if (AppInfoManager.isModelM()) {
                        if (isFlying) R.string.common_text_battery_tem_too_low_mf_flying else R.string.common_text_battery_tem_too_low_mf
                    } else if (AppInfoManager.isModelH()) {
                        if (isFlying) R.string.common_text_battery_tem_too_low_hl_flying else R.string.common_text_battery_tem_too_low_hl
                    } else {
                        if (isFlying) R.string.common_text_battery_tem_too_low_s_flying else R.string.common_text_battery_tem_too_low_s
                    }
                    WarningBean(
                        warningId,
                        WarningBean.WarnLevel.HIGH_TIP,
                        WarningBean.TipType.TipWindow(contentRes = id),
                        true
                    )
                } else {
                    mUndefined
                }
            }

            WaringIdEnum.BATTERY_RB_TEMPERATURE_TOO_LOW,
            WaringIdEnum.BATTERY_RB_AUTO_HEATING,
                -> {
                if (AppInfoManager.isSupportBatteryDetailWarn()) {
                    val id = if (AppInfoManager.isModelM()) {
                        if (isFlying) R.string.common_text_battery_tem_too_low_mb_flying else R.string.common_text_battery_tem_too_low_mb
                    } else {
                        if (isFlying) R.string.common_text_battery_tem_too_low_hr_flying else R.string.common_text_battery_tem_too_low_hr
                    }
                    WarningBean(
                        warningId,
                        WarningBean.WarnLevel.HIGH_TIP,
                        WarningBean.TipType.TipWindow(contentRes = id),
                        true
                    )
                } else {
                    mUndefined
                }
            }

            WaringIdEnum.BATTERY_LF_TEMPERATURE_HIGH -> {
                if (AppInfoManager.isSupportBatteryDetailWarn() && isFlying) {
                    val id = if (AppInfoManager.isModelM()) {
                        R.string.common_text_battery_tem_high_mf_flying
                    } else if (AppInfoManager.isModelH()) {
                        R.string.common_text_battery_tem_high_hl_flying
                    } else {
                        R.string.common_text_battery_tem_high_s_flying
                    }
                    WarningBean(
                        warningId,
                        WarningBean.WarnLevel.HIGH_TIP,
                        WarningBean.TipType.TipWindow(contentRes = id),
                        true
                    )
                } else {
                    mUndefined
                }
            }

            WaringIdEnum.BATTERY_RB_TEMPERATURE_HIGH -> {
                if (AppInfoManager.isSupportBatteryDetailWarn() && isFlying) {
                    val id = if (AppInfoManager.isModelM()) {
                        R.string.common_text_battery_tem_high_mb_flying
                    } else {
                        R.string.common_text_battery_tem_high_hr_flying
                    }
                    WarningBean(
                        warningId,
                        WarningBean.WarnLevel.HIGH_TIP,
                        WarningBean.TipType.TipWindow(contentRes = id),
                        true
                    )
                } else {
                    mUndefined
                }
            }

            WaringIdEnum.BATTERY_LF_TEMPERATURE_LOW -> {
                if (AppInfoManager.isSupportBatteryDetailWarn() && isFlying) {
                    val id = if (AppInfoManager.isModelM()) {
                        R.string.common_text_battery_tem_low_mf_flying
                    } else if (AppInfoManager.isModelH()) {
                        R.string.common_text_battery_tem_low_hl_flying
                    } else {
                        R.string.common_text_battery_tem_low_s_flying
                    }
                    WarningBean(
                        warningId,
                        WarningBean.WarnLevel.HIGH_TIP,
                        WarningBean.TipType.TipWindow(contentRes = id),
                        true
                    )
                } else {
                    mUndefined
                }
            }

            WaringIdEnum.BATTERY_RB_TEMPERATURE_LOW -> {
                if (AppInfoManager.isSupportBatteryDetailWarn() && isFlying) {
                    val id = if (AppInfoManager.isModelM()) {
                        R.string.common_text_battery_tem_low_mb_flying
                    } else {
                        R.string.common_text_battery_tem_low_hr_flying
                    }
                    WarningBean(
                        warningId,
                        WarningBean.WarnLevel.HIGH_TIP,
                        WarningBean.TipType.TipWindow(contentRes = id),
                        true
                    )
                } else {
                    mUndefined
                }
            }

            WaringIdEnum.BATTERY_LF_LOW_TEMP_VOLTAGE -> {
                if (AppInfoManager.isSupportBatteryDetailWarn()) {
                    val id = if (AppInfoManager.isModelM()) {
                        if (isFlying) R.string.common_text_battery_tem_low_power_low_mf_flying else R.string.common_text_battery_tem_low_power_low_mf
                    } else {
                        if (isFlying) R.string.common_text_battery_tem_low_power_low_hl_flying else R.string.common_text_battery_tem_low_power_low_hl
                    }
                    WarningBean(
                        warningId,
                        WarningBean.WarnLevel.HIGH_TIP,
                        WarningBean.TipType.TipWindow(contentRes = id),
                        true
                    )
                } else {
                    mUndefined
                }
            }

            WaringIdEnum.BATTERY_RB_LOW_TEMP_VOLTAGE -> {
                if (AppInfoManager.isSupportBatteryDetailWarn()) {
                    val id = if (AppInfoManager.isModelM()) {
                        if (isFlying) R.string.common_text_battery_tem_low_power_low_mb_flying else R.string.common_text_battery_tem_low_power_low_mb
                    } else {
                        if (isFlying) R.string.common_text_battery_tem_low_power_low_hr_flying else R.string.common_text_battery_tem_low_power_low_hr
                    }
                    WarningBean(
                        warningId,
                        WarningBean.WarnLevel.HIGH_TIP,
                        WarningBean.TipType.TipWindow(contentRes = id),
                        true
                    )
                } else {
                    mUndefined
                }
            }

            WaringIdEnum.BATTERY_LF_VOLTAGE_DIFF_LARGE -> {
                if (AppInfoManager.isSupportBatteryDetailWarn()) {
                    val id = if (AppInfoManager.isModelM()) {
                        if (isFlying) R.string.common_text_battery_voltage_diff_more_mf_flying else R.string.common_text_battery_voltage_diff_more_mf
                    } else if (AppInfoManager.isModelH()) {
                        if (isFlying) R.string.common_text_battery_voltage_diff_more_hl_flying else R.string.common_text_battery_voltage_diff_more_hl
                    } else {
                        if (isFlying) R.string.common_text_battery_voltage_diff_more_s else R.string.common_text_battery_voltage_pressure_differential_flying
                    }
                    WarningBean(
                        warningId,
                        WarningBean.WarnLevel.HIGH_TIP,
                        WarningBean.TipType.TipWindow(contentRes = id),
                        true
                    )
                } else {
                    mUndefined
                }
            }

            WaringIdEnum.BATTERY_RB_VOLTAGE_DIFF_LARGE -> {
                if (AppInfoManager.isSupportBatteryDetailWarn()) {
                    val id = if (AppInfoManager.isModelM()) {
                        if (isFlying) R.string.common_text_battery_voltage_diff_more_mb_flying else R.string.common_text_battery_voltage_diff_more_mb
                    } else {
                        if (isFlying) R.string.common_text_battery_voltage_diff_more_hr_flying else R.string.common_text_battery_voltage_diff_more_hr
                    }
                    WarningBean(
                        warningId,
                        WarningBean.WarnLevel.HIGH_TIP,
                        WarningBean.TipType.TipWindow(contentRes = id),
                        true
                    )
                } else {
                    mUndefined
                }
            }

            WaringIdEnum.EMERGENCY_LANDING_MODE -> {
                if (AppInfoManager.isSupport3Plummeting() && isFlying) {
                    WarningBean(
                        warningId,
                        WarningBean.WarnLevel.HIGH_TIP,
                        WarningBean.TipType.TipWindow(contentRes = R.string.common_text_part_stop_propeller_warn),
                        true
                    )
                } else {
                    mUndefined
                }
            }

            WaringIdEnum.PARTIAL_ROTOR_FAILURE -> {
                if (AppInfoManager.isSupportPartStopPropeller() && isFlying) {
                    WarningBean(
                        warningId,
                        WarningBean.WarnLevel.HIGH_TIP,
                        WarningBean.TipType.TipWindow(contentRes = R.string.common_text_part_stop_propeller_warn),
                        true
                    )
                } else {
                    mUndefined
                }
            }

            WaringIdEnum.UOM_NOT_CERTIFICATION -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.HIGH_TIP,
                    WarningBean.TipType.TipDialog(
                        contentRes = R.string.common_text_drone_real_name_registration,
                        msgRes = R.string.common_text_drone_real_name_registration_tip,
                        leftBtnRes = R.string.common_text_cancel,
                        rightBtnRes = R.string.common_text_go_and_register,
                        rightBtnAction = WarningBean.Action.UOM,
                        imageRes = R.drawable.common_ic_dialog_waring
                    ),
                    true
                )
            }

            WaringIdEnum.REMOTE_COMPASS_NEED_CALIBRATION -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.HIGH_TIP,
                    WarningBean.TipType.TipWindow(
                        contentRes = R.string.common_text_remote_compass_need_calibration,
                        action = WarningBean.Action.RC_COMPASS_CALL
                    ),
                    true
                )
            }
            //需求：目前ads-b告警只显示强中告警，强中告警跟随设置走显示，弱告警始终不显示
            WaringIdEnum.ADSB_WARN_MIDDLE -> {
                return WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_text_adsb_warning_content),
                    true
                )
            }

            WaringIdEnum.ADSB_WARN_STRONG -> {
                return WarningBean(
                    warningId,
                    WarningBean.WarnLevel.HIGH_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_text_compass_interfere, action = WarningBean.Action.RC_COMPASS_CALL),
                    true
                )
            }

            WaringIdEnum.FLY_LOCK_MISSION_EXCEPTION -> {
                //现在分开显示，所有版本改夏冬冬、黄小珊、周德才
                val id = if (isFlying) R.string.common_text_drone_mission_error_flying else R.string.common_text_drone_mission_error
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.HIGH_TIP,
                    WarningBean.TipType.TipWindow(contentRes = id),
                    true
                )
            }

            WaringIdEnum.FLY_LOCK_UOM_UNACTIVATED -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipDialog(
                        contentRes = R.string.common_text_uom_not_register,
                        msgRes = R.string.common_text_drone_real_name_registration_tip,
                        leftBtnRes = R.string.common_text_cancel,
                        rightBtnRes = R.string.common_text_go_and_register,
                        rightBtnAction = WarningBean.Action.UOM,
                        imageRes = R.drawable.common_ic_dialog_waring
                    ),
                    true
                )
            }

            WaringIdEnum.DRONE_SHAKING_TOO_MUCH -> {
                if (isFlying) {
                    WarningBean(
                        warningId,
                        WarningBean.WarnLevel.HIGH_TIP,
                        WarningBean.TipType.TipWindow(contentRes = R.string.common_text_drone_shake_exception_flying),
                        true
                    )
                } else {
                    WarningBean(
                        warningId,
                        WarningBean.WarnLevel.HIGH_TIP,
                        WarningBean.TipType.TipWindow(contentRes = R.string.common_text_drone_shake_exception),
                        true
                    )
                }
            }

            WaringIdEnum.MISSION_EXIT_FOR_NFZ -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipToast(contentRes = R.string.common_text_hot_area_stop_mission),
                    true
                )
            }

            WaringIdEnum.MISSION_KML_UNZIP_ERROR -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipToast(contentRes = R.string.common_text_kml_analysis_failure),
                    true
                )
            }

            WaringIdEnum.MISSION_INVALID_GIMBAL_PITCH -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipToast(contentRes = R.string.common_text_gimbal_not_support_mission),
                    true
                )
            }

            WaringIdEnum.MISSION_STUCK_IN_FIRST_POINT -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipToast(contentRes = R.string.common_text_mission_failure_first_point),
                    true
                )
            }

            WaringIdEnum.STUCK_IN_WAYPOINT_PHOTO -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipToast(contentRes = R.string.common_text_mission_take_photo_advance),
                    true
                )
            }

            WaringIdEnum.MISSION_EXIT_FOR_NO_GPS -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipToast(contentRes = R.string.common_text_mission_is_exit_no_gps),
                    true
                )
            }

            WaringIdEnum.MISSION_EXIT_FOR_NO_FILE,
            WaringIdEnum.MISSION_EXIT_FOR_OPERATE_FILE_ERROR,
            WaringIdEnum.MISSION_FILE_NOT_FOUND,
            WaringIdEnum.MISSION_FILE_PARSER_ERROR,
            WaringIdEnum.MISSION_FILE_GUID_ERROR,
                -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipToast(contentRes = R.string.common_text_mission_upload_failure),
                    true
                )
            }

            WaringIdEnum.MISSION_EXIT_FOR_FLY_POSITION_ERROR,
            WaringIdEnum.MISSION_EXIT_FOR_GPS_TO_NED,
            WaringIdEnum.MISSION_EXIT_FOR_PARSER_ERROR,
                -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipToast(contentRes = R.string.common_text_mission_execute_failure),
                    true
                )
            }

            WaringIdEnum.AUTO_LAND_FOR_GEO_BARRIER -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_text_in_nfz_to_land),
                    true
                )
            }

            WaringIdEnum.MISSION_WAYPOINT_IN_NFZ -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipToast(contentRes = R.string.common_text_route_nfz_mission_failure),
                    true
                )
            }

            WaringIdEnum.MISSION_ALTITUDE_DISTANCE_EXCEED_MAX -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipToast(contentRes = R.string.common_text_mission_over_limit_failure),
                    true
                )
            }

            WaringIdEnum.MISSION_FLIGHT_DISTANCE_EXCEED_MAX -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipToast(contentRes = R.string.common_text_mission_over_ability_failure),
                    true
                )
            }

            WaringIdEnum.MISSION_START_POSITION_TOO_FAR -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipToast(contentRes = R.string.common_text_mission_to_far_failure),
                    true
                )
            }

            WaringIdEnum.MISSION_FLIGHT_TIME_TOO_LONG -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipToast(contentRes = R.string.common_text_mission_over_time_failure),
                    true
                )
            }

            WaringIdEnum.MISSION_FLY_CONTROL_NOT_RESPONSE -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipToast(contentRes = R.string.common_text_drone_not_response),
                    true
                )
            }

            WaringIdEnum.RID_CHECK_EXCEPTION -> {
                if (CountryManager.isChinaZone() || CountryManager.isJapanZone() || CountryManager.isEUZone()) {
                    WarningBean(
                        warningId,
                        WarningBean.WarnLevel.MIDDLE_TIP,
                        WarningBean.TipType.TipWindow(contentRes = R.string.common_text_rid_check_error),
                        true
                    )
                } else if (CountryManager.isUsZone()) {
                    val id = if (isFlying) R.string.common_text_rid_check_error_us_flying else R.string.common_text_rid_check_error_us
                    WarningBean(
                        warningId,
                        WarningBean.WarnLevel.MIDDLE_TIP,
                        WarningBean.TipType.TipWindow(contentRes = id),
                        true
                    )
                } else {
                    mUndefined
                }
            }

            WaringIdEnum.EMM_MANUAL_FORMATION_ALT_TOO_LOW -> {
                val limit = TransformUtils.getLengthWithUnit(30.0)
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipToast(contentRes = 0),
                    false,
                    detailMsg = String.format(BaseApp.getContext().getString(R.string.common_text_manual_format_height_low_tip), limit)
                )
            }

            WaringIdEnum.CAMERA_LOW_POWER_WARN -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_text_low_power_warn),
                    true
                )
            }

            WaringIdEnum.VISUAL_POSITIONING_INITIATED -> {
                if (AppInfoManager.isModelS()) {
                    WarningBean(
                        warningId,
                        WarningBean.WarnLevel.HIGH_TIP,
                        WarningBean.TipType.TipWindow(contentRes = R.string.common_text_visual_positioning_activating_warn),
                        true
                    )
                } else {
                    mUndefined
                }
            }

            WaringIdEnum.CHIP_TEMPERATURE_TOO_HIGH -> {
                if (AppInfoManager.isModelS()) {
                    WarningBean(
                        warningId,
                        WarningBean.WarnLevel.HIGH_TIP,
                        WarningBean.TipType.TipWindow(contentRes = R.string.common_text_chip_temperature_too_high),
                        true
                    )
                } else {
                    mUndefined
                }
            }

            //汪博拍板，云雾告警有问题，屏蔽掉
            /*WaringIdEnum.MISSION_CLOUD_WARN_FRONT,
            WaringIdEnum.MISSION_CLOUD_WARN_TAIL,
            WaringIdEnum.MISSION_CLOUD_WARN_BOTTOM,
            WaringIdEnum.MISSION_CLOUD_WARN_MULTIPLE -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_text_cloud_warn_tips),
                    true
                )
            }*/

            //SD卡写入慢
            WaringIdEnum.TF_CARD_WRITE_SPEED_TOO_SLOW -> {
                WarningBean(
                    warnId = warningId,
                    warnLevel = WarningBean.WarnLevel.HIGH_TIP,
                    tip = WarningBean.TipType.TipDialog(
                        contentRes = R.string.common_text_sd_write_slowly_title,
                        msgRes = R.string.common_text_sd_write_slowly_tips,
                        rightBtnRes = R.string.common_text_mission_got_known
                    ),
                    true
                )
            }

            //电机堵转
            WaringIdEnum.MOTOR_STALL -> {
                val tipId = if (isFlying) R.string.common_text_esc_stalled_flying else R.string.common_text_esc_stalled_nofly
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.HIGH_TIP,
                    WarningBean.TipType.TipWindow(contentRes = tipId),
                    true
                )
            }
            //电机未就绪
            WaringIdEnum.ESC_NOT_READY -> {
                val tipId = if (isFlying) R.string.common_text_esc_unready_flying else R.string.common_text_esc_unready_nofly
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.HIGH_TIP,
                    WarningBean.TipType.TipWindow(contentRes = tipId),
                    true
                )
            }
            //图传CP版本不统一（重刷版本）
            WaringIdEnum.TRANSMIT_CP_VERSION_NOT_MATCH -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_text_dsp_version_not_match),
                    true
                )
            }
            //飞机天线损坏，建议更换天线
            WaringIdEnum.TRANSMIT_DRONE_RSRP_BIG_DIFF -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.HIGH_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_text_drone_dsp_resp_big_diff),
                    true
                )
            }
            //遥控天线损坏，建议更换天线
            WaringIdEnum.TRANSMIT_RC_RSRP_BIG_DIFF -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.HIGH_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_text_rc_dsp_resp_big_diff),
                    true
                )
            }

            //指南针校准后未重启飞机
            WaringIdEnum.CALIBRATE_COMPASS_NOT_REBOOT -> {
                val tipId =
                    if (isFlying) R.string.common_text_compass_cal_not_restart_drone_flying else R.string.common_text_compass_cal_not_restart_drone
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.HIGH_TIP,
                    WarningBean.TipType.TipWindow(contentRes = tipId),
                    true
                )
            }

            //IMU校准后未重启飞机
            WaringIdEnum.CALIBRATE_IMU_NOT_REBOOT -> {
                val tipId =
                    if (isFlying) R.string.common_text_imu_cal_not_restart_drone_flying else R.string.common_text_imu_cal_not_restart_drone
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.HIGH_TIP,
                    WarningBean.TipType.TipWindow(contentRes = tipId),
                    true
                )
            }
            //进入低功耗模式，云台相机已关闭
            WaringIdEnum.CAMERA_LOW_POWER_WARN_GIMBAL -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_text_low_power_warn_gimbal),
                    true
                )
            }

            //没有网络，UOM告警
            WaringIdEnum.NO_INTERNET_FOR_UOM_REPORT -> {
                if (UomReportManager.isNeedWarnNoNetwork()) {
                    WarningBean(
                        warningId,
                        WarningBean.WarnLevel.MIDDLE_TIP,
                        WarningBean.TipType.TipWindow(contentRes = R.string.common_text_uom_report_no_network_tips),
                        true
                    )
                } else {
                    mUndefined
                }
            }

            WaringIdEnum.HEIGHT_EXCEEDS_LAW_HEIGHT -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(
                        contentRes = 0,
                        contentStr = BaseApp.getContext().getString(
                            R.string.common_text_more_than_height_limit_warn,
                            "" + TransformUtils.getLengthWithUnit(LawHeightDelegateManager.getLowHeight(CountryManager.currentCountry).toDouble())
                        )
                    ),
                    true
                )
            }

            //H:左电池缺失 M:前电池缺失
            WaringIdEnum.BATTERY_LACK_WARN_1 -> {
                if (AppInfoManager.isModelH() || AppInfoManager.isModelM()) {
                    val id: Int
                    val action: WarningBean.Action?
                    if (AppInfoManager.isModelH()) {
                        id = if (isFlying) R.string.common_text_battery_lack_warn_1_h_flying else R.string.common_text_battery_lack_warn_1_h
                        action = WarningBean.Action.SHOW_BATTERY_INSTALL_DIALOG
                    } else {
                        id = if (isFlying) R.string.common_text_battery_lack_warn_1_m_flying else R.string.common_text_battery_lack_warn_1_m
                        action = null
                    }
                    WarningBean(
                        warningId,
                        WarningBean.WarnLevel.HIGH_TIP,
                        WarningBean.TipType.TipWindow(contentRes = id, action = action),
                        true
                    )

                } else {
                    mUndefined
                }
            }

            //H:右电池缺失 M:后电池缺失
            WaringIdEnum.BATTERY_LACK_WARN_2 -> {
                if (AppInfoManager.isModelH() || AppInfoManager.isModelM()) {
                    val id: Int
                    val action: WarningBean.Action?
                    if (AppInfoManager.isModelH()) {
                        id = if (isFlying) R.string.common_text_battery_lack_warn_2_h_flying else R.string.common_text_battery_lack_warn_2_h
                        action = WarningBean.Action.SHOW_BATTERY_INSTALL_DIALOG
                    } else {
                        id = if (isFlying) R.string.common_text_battery_lack_warn_2_m_flying else R.string.common_text_battery_lack_warn_2_m
                        action = null
                    }

                    WarningBean(
                        warningId,
                        WarningBean.WarnLevel.HIGH_TIP,
                        WarningBean.TipType.TipWindow(contentRes = id, action = action),
                        true
                    )
                } else {
                    mUndefined
                }
            }

            //大雨告警
            WaringIdEnum.HEAVY_RAIN_WARN -> {
                val id = if (isFlying) R.string.common_text_heavy_rain_warn_flying else R.string.common_text_heavy_rain_warn
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.HIGH_TIP,
                    WarningBean.TipType.TipWindow(contentRes = id),
                    true
                )
            }

            //电池不在位 X H M 支持 电池1
            WaringIdEnum.BATTERY_NO_REIGN -> {
                if (!AppInfoManager.isModelS()) {
                    val id: Int
                    var action: WarningBean.Action? = null
                    if (AppInfoManager.isModelH()) {
                        id = if (isFlying) R.string.common_text_battery_no_reign_1_h_flying else R.string.common_text_battery_no_reign_1_h
                        action = WarningBean.Action.SHOW_BATTERY_INSTALL_DIALOG
                    } else if (AppInfoManager.isModelM()) {
                        id = if (isFlying) R.string.common_text_battery_no_reign_1_m_flying else R.string.common_text_battery_no_reign_1_m
                    } else {
                        id = if (isFlying) R.string.common_text_battery_no_reign_1_x_flying else R.string.common_text_battery_no_reign_1_x
                    }
                    WarningBean(
                        warningId,
                        WarningBean.WarnLevel.HIGH_TIP,
                        WarningBean.TipType.TipWindow(contentRes = id, action = action),
                        true
                    )
                } else {
                    mUndefined
                }
            }

            //电池不在位 H M 支持 电池2
            WaringIdEnum.BATTERY_NO_REIGN_2 -> {
                if (AppInfoManager.isModelH() || AppInfoManager.isModelM()) {
                    val id: Int
                    var action: WarningBean.Action? = null
                    if (AppInfoManager.isModelH()) {
                        id = if (isFlying) R.string.common_text_battery_no_reign_2_h_flying else R.string.common_text_battery_no_reign_2_h
                        action = WarningBean.Action.SHOW_BATTERY_INSTALL_DIALOG
                    } else {
                        id = if (isFlying) R.string.common_text_battery_no_reign_2_m_flying else R.string.common_text_battery_no_reign_2_m
                    }
                    WarningBean(
                        warningId,
                        WarningBean.WarnLevel.HIGH_TIP,
                        WarningBean.TipType.TipWindow(contentRes = id, action = action),
                        true
                    )
                } else {
                    mUndefined
                }
            }

            /**
             * 返航或降落，打杆只能往内飞
             * 触发条件：无人机在客制电子围栏可飞区外
             */
            WaringIdEnum.CUSTOM_GEO_FENCE_OUTSIDE_FLYABLE_ZONE -> {
                val tipType = if (isFlying) {
                    WarningBean.TipType.PermanentToastTip(contentRes = R.string.common_text_fly_out_of_fly_zone)
                } else {
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_text_out_of_fly_zone)
                }
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    tipType,
                    true,
                )
            }

            /**
             * 只能往内飞
             * 触发条件：无人机接近客制电子围栏可飞区边缘
             */
            WaringIdEnum.CUSTOM_GEO_FENCE_NEAR_FLYABLE_ZONE_EDGE -> {
                val tipType = if (isFlying) {
                    WarningBean.TipType.PermanentToastTip(contentRes = R.string.common_text_fly_near_fly_zone_buffer)
                } else {
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_text_in_buffer_zone)
                }

                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    tipType,
                    true,
                )
            }

            /**
             * 返航或降落，打杆只能往外飞
             * 触发条件：无人机位于客制电子围栏禁飞区内
             */
            WaringIdEnum.CUSTOM_GEO_FENCE_INSIDE_NO_FLY_ZONE -> {
                val tipType = if (isFlying) {
                    WarningBean.TipType.PermanentToastTip(contentRes = R.string.common_text_fly_in_nfz_fence)
                } else {
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_text_in_nfz_fence)
                }

                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    tipType,
                    true,
                )
            }

            /**
             * 只能往禁飞区外飞
             * 触发条件：无人机接近客制电子围栏禁飞区边缘
             */
            WaringIdEnum.CUSTOM_GEO_FENCE_NEAR_NO_FLY_ZONE_EDGE -> {
                val tipType = if (isFlying) {
                    WarningBean.TipType.PermanentToastTip(contentRes = R.string.common_text_near_nfz_fence)
                } else {
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_text_in_buffer_zone)
                }

                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    tipType,
                    true,
                )
            }

            WaringIdEnum.MULTI_DRONE_TAKEOFF_TOO_CLOSE -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.HIGH_TIP,
                    tip = WarningBean.TipType.TipDialog(
                        contentRes = 0,
                        contentStr = BaseApp.getContext().getString(
                            R.string.common_text_drone_to_drone_less_than_5_tips,
                            "${TransformUtils.getLengthWithoutUnit(DroneConst.DRONE_TO_DRONE_DISTANCE.toDouble()).toInt()}",
                            TransformUtils.getLengthUnit()
                        ),
                        msgRes = 0,
                        rightBtnRes = R.string.common_text_mission_got_known,
                    ),
                    true,
                    showDroneName = false
                )
            }

            //图传信号弱告警
            WaringIdEnum.TRANSMIT_SIGNAL_WEAK -> {
                val id = if (isFlying) R.string.common_text_warn_dsp_signal_weak_flying else R.string.common_text_warn_dsp_signal_weak
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = id),
                    true
                )
            }
            //图传信号丢失告警
            WaringIdEnum.TRANSMIT_SIGNAL_LOST -> {
                val id = if (isFlying) R.string.common_text_warn_dsp_signal_lost_flying else R.string.common_text_warn_dsp_signal_lost
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = id),
                    true
                )
            }
            //增强图传飞机信号丢失
            WaringIdEnum.LTE_DRONE_SIGNAL_LOST -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_text_drone_net_lost),
                    true
                )
            }
            // 增强图传飞机信号弱
            WaringIdEnum.LTE_DRONE_SIGNAL_WEAK -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_text_drone_net_weak),
                    true
                )
            }
            //增强图传遥控器信号丢失
            WaringIdEnum.LTE_RC_SIGNAL_LOST -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_text_remote_control_net_lost),
                    true
                )
            }
            //增强图传遥控器信号弱
            WaringIdEnum.LTE_RC_SIGNAL_WEAK -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_text_remote_control_weak),
                    true
                )
            }

            /**********************机巢告警**********************/
            //电机电源异常
            WaringIdEnum.MOTOR_POWER_UNUSUAL -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.HIGH_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_error_motor_power),
                    false
                )
            }
            //	舵机电源异常
            WaringIdEnum.STEER_POWER_UNUSUAL -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.HIGH_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_warn_servo_power_supply_abnormality),
                    false
                )
            }
            //	严重	空调1异常
            WaringIdEnum.AIR_ONE_UNUSUAL -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_error_air_1),
                    false
                )
            }
            //	严重	空调2异常
            WaringIdEnum.AIR_TWO_UNUSUAL -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_error_air_1),
                    false
                )
            }
            //  上舱温湿度传感器通讯异常
            WaringIdEnum.UP_TEMP_HUM_SENSOR_UNUSUAL -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_error_up_temp_hum),
                    false
                )
            }
            //   下舱水浸传感器异常
            WaringIdEnum.DOWN_TEMP_HUM_SENSOR_UNUSUAL -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_error_up_temp_hum),
                    false
                )
            }
            //   振动传感器异常
            WaringIdEnum.VIBRATE_SENSOR_UNUSUAL -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_error_up_temp_hum),
                    false
                )
            }
            //    备用电池通讯异常
            WaringIdEnum.BACKUP_BATTERY_COMMUNICATE_UNUSUAL -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_error_backup_battery),
                    false
                )
            }
            //     气象站通讯异常
            WaringIdEnum.METEOROLOGICAL_STATION_UNUSUAL -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_error_weather),
                    false
                )
            }
            //     充电主模块通讯异常
            WaringIdEnum.CHARGE_MAIN_MODULE_UNUSUAL -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_error_charge_main),
                    false
                )
            }
            //        充电副模块通讯异常
            WaringIdEnum.CHARGE_VICE_MODULE_UNUSUAL -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_error_charge_main),
                    false
                )
            }
            //      散热风扇1转速异常
            WaringIdEnum.FAN_ONE_UNUSUAL -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_error_fun1),
                    false
                )
            }
            //     散热风扇2转速异常
            WaringIdEnum.FAN_TWO_UNUSUAL -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_error_fun1),
                    false
                )
            }
            //    散热风扇3转速异常
            WaringIdEnum.FAN_THREE_UNUSUAL -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_error_fun1),
                    false
                )
            }
            //     散热风扇4转速异常
            WaringIdEnum.FAN_FOUR_UNUSUAL -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_error_fun1),
                    false
                )
            }
            //   空调1风扇5转速异常
            WaringIdEnum.AIR_ONE_FAN_FIVE_UNUSUAL -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_error_fun5),
                    false
                )
            }
            //      空调1风扇6转速异常
            WaringIdEnum.AIR_ONE_FAN_SIX_UNUSUAL -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_error_fun5),
                    false
                )
            }
            //     空调2风扇7转速异常
            WaringIdEnum.AIR_TWO_FAN_SEVEN_UNUSUAL -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_error_fun5),
                    false
                )
            }
            //      空调2风扇8转速异常
            WaringIdEnum.AIR_TWO_FAN_EIGHT_UNUSUAL -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_error_fun5),
                    false
                )
            }
            //       机巢-飞机 电池通讯异常
            WaringIdEnum.BATTERY_COMMUNICATE_UNUSUAL -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_error_battery_nest_fail),
                    false
                )
            }
            //    水浸告警
            WaringIdEnum.WATER_UNUSUAL -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.HIGH_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_error_water),
                    false
                )
            }
            //   机巢异常振动告警
            WaringIdEnum.VIBRATE_UNUSUAL -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.HIGH_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_error_vibrate),
                    false
                )
            }
            //    着陆灯异常
            WaringIdEnum.LAND_LIGHT_UNUSUAL -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_error_land_light),
                    false
                )
            }
            //   UPS电源通讯异常
            WaringIdEnum.UPS_POWER_COMMUNICATE -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_error_ups_power_communicate),
                    false
                )
            }
            //    UPS电源异常
            WaringIdEnum.UPS_POWER_UNUSUAL -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_error_ups_power),
                    false
                )
            }
            //   电源适配器通讯异常
            WaringIdEnum.POWER_ADAPTER_COMMUNICATE_UNUSUAL -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_error_power_adapter_communicate),
                    false
                )
            }
            //    电源适配器异常
            WaringIdEnum.POWER_ADAPTER_UNUSUAL -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_error_power_adapter),
                    false
                )
            }
            //    电机驱动板通讯失败
            WaringIdEnum.MOTOR_DRIVE_BOARD_COMMUNICATE_UNUSUAL -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_error_motor_driver_communicate),
                    false
                )
            }
            //    电机驱动板异常复位
            WaringIdEnum.MOTOR_DRIVE_BOARD_RESET_UNUSUAL -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.HIGH_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_error_motor_driver_reset),
                    false
                )
            }
            //   开门时舱门阻塞
            WaringIdEnum.CABIN_OPEN_BLOCK -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.HIGH_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_error_cabin_open_scram),
                    false
                )
            }
            //   关门时舱门阻塞
            WaringIdEnum.CABIN_CLOSE_BLOCK -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.HIGH_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_error_cabin_close_scram),
                    false
                )
            }
            //    开门异常
            WaringIdEnum.CABIN_OPEN_UNUSUAL -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.HIGH_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_error_cabin_open),
                    false
                )
            }
            //    关门异常
            WaringIdEnum.CABIN_CLOSE_UNUSUAL -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.HIGH_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_error_cabin_close),
                    false
                )
            }
            //   X杆归中时阻塞
            WaringIdEnum.X_ROD_PULL_BLOCK -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_error_x_rob_center_scram),
                    false
                )
            }
            //   X杆释放时阻塞
            WaringIdEnum.X_ROD_PUSH_BLOCK -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_error_x_rod_release_scram),
                    false
                )
            }
            //    X杆归中异常
            WaringIdEnum.X_ROD_PULL_UNUSUAL -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_error_x_rod_center),
                    false
                )
            }
            //    X杆释放异常
            WaringIdEnum.X_ROD_PUSH_UNUSUAL -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_error_x_rod_release),
                    false
                )
            }
            //   Y杆归中时阻塞
            WaringIdEnum.Y_ROD_PULL_BLOCK -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_error_y_rod_center_scram),
                    false
                )
            }
            //   Y杆释放时阻塞
            WaringIdEnum.Y_ROD_PUSH_BLOCK -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_error_y_rod_release_scram),
                    false
                )
            }
            //    Y杆归中异常
            WaringIdEnum.Y_ROD_PULL_UNUSUAL -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_error_y_rod_center),
                    false
                )
            }
            //    Y杆释放异常
            WaringIdEnum.Y_ROD_PUSH_UNUSUAL -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_error_y_rod_release),
                    false
                )
            }
            //       Z杆竖起异常
            WaringIdEnum.Z_ROD_PUSH_BLOCK -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_error_z_rod_release),
                    false
                )
            }
            //       Z杆倒下异常
            WaringIdEnum.Z_ROD_PULL_BLOCK -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_error_z_rod_center),
                    false
                )
            }
            //  限位开关异常
            WaringIdEnum.LIMIT_SWITCH_UNUSUAL -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.HIGH_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_error_limit_switch),
                    false
                )
            }
            //    舱门电机异常
            WaringIdEnum.CABIN_MOTOR_UNUSUAL -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.HIGH_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_error_cabin_motor),
                    false
                )
            }
            //    X杆电机异常
            WaringIdEnum.X_ROD_MOTOR_UNUSUAL -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_error_x_rod_motor),
                    false
                )
            }
            //    Y杆电机异常
            WaringIdEnum.Y_ROD_MOTOR_UNUSUAL -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_error_y_rod_motor),
                    false
                )
            }
            //   Z杆(拨桨杆)舵机1异常
            WaringIdEnum.Z_ROD_MOTOR_ONE_UNUSUAL -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_error_z_rod_1),
                    false
                )
            }
            //   Z杆(拨桨杆)舵机2异常
            WaringIdEnum.Z_ROD_MOTOR_TWO_UNUSUAL -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_error_z_rod_1),
                    false
                )
            }
            //   Z杆(拨桨杆)舵机3异常
            WaringIdEnum.Z_ROD_MOTOR_THREE_UNUSUAL -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_error_z_rod_1),
                    false
                )
            }
            //   Z杆(拨桨杆)舵机4异常
            WaringIdEnum.Z_ROD_MOTOR_FOUR_UNUSUAL -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_error_z_rod_1),
                    false
                )
            }
            //	严重	电机驱动板复位	电机驱动板复位（看门狗复位，断电复位）
            WaringIdEnum.MOTOR_DRIVER_RESET -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_motor_driver_reset),
                    false
                )
            }
            //  充电过温
            WaringIdEnum.CHARGE_OTC -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.HIGH_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_charging_over_temperature),
                    false
                )
            }
            //  充电欠温
            WaringIdEnum.CHARGE_UNDER_TEMP -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_low_temperature_of_charging),
                    false
                )
            }
            //    充电过流
            WaringIdEnum.CHARGE_OVER_CURRENT -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_over_current_of_charging),
                    false
                )
            }
            //   无人机充电异常
            WaringIdEnum.CHARGE_UNUSUAL -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_abnormal_of_drone_charging),
                    false
                )
            }
            //   U盘读写异常
            WaringIdEnum.U_BOARD_WRITE_READ_UNUSUAL -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_disk_abn),
                    false
                )
            }
            //   WIFI启动异常
            WaringIdEnum.WIFI_START_UNUSUAL -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_wifi_abn),
                    false
                )
            }
            //   RTK通信异常
            WaringIdEnum.RTK_COMMUNICATE_UNUSUAL -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_rtk_abn),
                    false
                )
            }
            //    电源板通信异常
            WaringIdEnum.POWER_BOARD_COMMUNICATE -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_power_board_abn),
                    false
                )
            }
            //   千兆网卡异常
            WaringIdEnum.GIGABIT_NETWORK_CARD_UNUSUAL -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_1000m_abn),
                    false
                )
            }
            //   百兆网卡异常
            WaringIdEnum.HUNDRED_NETWORK_CARD_UNUSUAL -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_100m_abn),
                    false
                )
            }
            //   监控摄像头异常(接百兆网卡)
            WaringIdEnum.SURVEILLANCE_CAMERA_UNUSUAL -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_camera_abn),
                    false
                )
            }
            //  系统异常重启
            WaringIdEnum.SYSTEM_REBOOT_UNUSUAL -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_system_reboot_abn),
                    false
                )
            }
            //   系统温度过高
            WaringIdEnum.SYSTEM_OVER_TEMP -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.HIGH_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_system_tmp_high),
                    false
                )
            }
            //   系统CPU负载过高
            WaringIdEnum.SYSTEM_OVER_SPU -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_system_cup_over),
                    false
                )
            }
            //   系统内存占用过高
            WaringIdEnum.SYSTEM_OVER_MEMORY_OCCUPY -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_system_memory_over),
                    false
                )
            }
            //  系统存储空间使用过高
            WaringIdEnum.SYSTEM_OVER_MEMORY_USE -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_system_storage_space),
                    false
                )
            }
            //  APK应用异常重启
            WaringIdEnum.APP_REBOOT_UNUSUAL -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_apk_abn),
                    false
                )
            }
            //    网络异常
            WaringIdEnum.NETWORK_UNUSUAL -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_network_abn),
                    false
                )
            }
            //  射频干扰
            WaringIdEnum.NEST_RADIO_FREQUENCY_INTERFERENCE -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_frequ_cy),
                    false
                )
            }
            //    图传CP通信异常
            WaringIdEnum.TRANSMIT_CP_COMMUNICATE_UNUSUAL -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_cp_abn),
                    false
                )
            }
            //  空口网络延时过高
            WaringIdEnum.NETWORK_DELAY_TOO_HIGH -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_network_delay),
                    false
                )
            }
            //  视频编解码异常
            WaringIdEnum.VIDEO_CODE_FAILED -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_video_codec_abn),
                    false
                )
            }
            //  从相机下载文件失败
            WaringIdEnum.DOWNLOAD_FAILED -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_download_pic_abn),
                    false
                )
            }
            //  上传文件到指挥中心失败
            WaringIdEnum.UPLOAD_FAILED -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_upload_abn),
                    false
                )
            }
            //  巢外摄像头登录异常
            WaringIdEnum.CAMERA_CONNECT_FAILED -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_outside_camera_abn),
                    false
                )
            }
            //  webrtc连接异常
            WaringIdEnum.WEBRTC_CONNECT_FAILED -> {
                WarningBean(
                    warningId,
                    WarningBean.WarnLevel.MIDDLE_TIP,
                    WarningBean.TipType.TipWindow(contentRes = R.string.common_nest_webrtc_abn),
                    false
                )
            }
            /**********************机巢告警 end**********************/

            else -> {
                mUndefined
            }
        }
    }
}