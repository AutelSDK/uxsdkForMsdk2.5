package com.autel.ux.core.communication

import com.autel.common.manager.unit.DistanceSpeedUnitEnum

object GlobalKeys : UXKeys() {

    @UXParamKey(type = DistanceSpeedUnitEnum::class, updateType = UpdateType.ON_CHANGE)
    const val UNIT_TYPE = "UnitType"
}