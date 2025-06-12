package com.autel.ux.core.model

import com.autel.widget.R

enum class FlyModeEnum(val txtRes: Int) {

    /**
     * 未知
     */
    UNKNOWN(R.string.common_text_unknown),

    /**
     * 手动飞行
     */
    DEFAULT(R.string.common_text_manual_flight),

    /**
     * 目标锁定
     */
    LOCK_TARGET(R.string.common_text_track),

    /**
     * 目标识别
     */
    AI(R.string.common_text_ai),

    /**
     * 快速任务
     */
    QUICK_TASK(R.string.common_text_quick_task),

    /**
     * 航点任务
     */
    POINT_TASK(R.string.common_text_waypoint_task),

    /**
     * 矩形任务
     */
    RECT_TASK(R.string.common_text_rectangular_task),

    /**
     * 多边形任务
     */
    POLYGON_TASK(R.string.common_text_polygon_task),

    /**
     * 倾斜任务
     */
    OBLIQUE_TASK(R.string.common_text_tilt_photography),

    /**
     * 航带任务
     */
    BELT_TASK(R.string.common_text_belt_task),

    /*    */

    /**
     * 单体环绕，建模环绕
     */
    SINGLE_SURROUND(R.string.common_text_mapping_surround),

    /**
     * 返航
     */
    RETURN_HOME(R.string.common_text_go_home),

    /**
     * 降落
     */
    LANDING(R.string.common_text_land),

    /**
     * 任务录制
     */
    TASK_RECORD(R.string.common_text_task_recording),

    /**
     * 仿地飞行
     */
    EARTH_IMITATING(R.string.common_text_mission_earth_imitating_flight),

    /**
     * 航线任务
     */
    WAYLINE_MISSION(R.string.common_text_guide_route_task);

    companion object {
        fun findEnum(value: Int): FlyModeEnum {
            val array = values()
            for (type in array) {
                if (type.txtRes == value) {
                    return type
                }
            }
            return UNKNOWN
        }
    }
}