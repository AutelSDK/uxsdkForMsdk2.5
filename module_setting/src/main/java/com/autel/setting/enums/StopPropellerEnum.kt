package com.autel.setting.enums

import com.autel.common.R
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.mission.enums.FcsTmpDisarmAirEnum

/**
 * @author 
 * @date 2023/9/2
 * 紧急停桨
 */
enum class StopPropellerEnum(val value: FcsTmpDisarmAirEnum, val id: Int) {
    /**
     * 关闭
     */
    OFF(FcsTmpDisarmAirEnum.FORBID_DISARM, R.string.common_text_close),

    /**
     * 打开
     */
    ON(FcsTmpDisarmAirEnum.ALLOW_DISARM_ANYTIME, R.string.common_text_light_open),

    /**
     * 仅故障时
     */
    ONLY_FAULT(FcsTmpDisarmAirEnum.ALLOW_DISARM_WHEN_ERROR, R.string.common_text_only_failure_occurs);

    companion object {
        fun find(value: FcsTmpDisarmAirEnum): StopPropellerEnum {
            for (x in values()) {
                if (x.value == value) return x
            }
            return OFF
        }
    }
}