package com.autel.setting.bean

import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.enums.GearLevelEnum

/**
 * @author 
 * @date 2023/7/11
 * 档位模式
 */
data class GearModeBean(
    /**
     * 档位模式
     */
    var mode: GearLevelEnum = GearLevelEnum.UNKNOWN,

    /**
     * 名称
     */
    var name: String = ""
)
