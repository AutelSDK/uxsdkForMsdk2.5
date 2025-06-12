package com.autel.widget.widget.colouratla

import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.camera.enums.ThermalColorEnum

/**
 * Created by  2023/9/16
 * 伪彩监听
 */
interface IColourAtlaSelectListner {

    fun onColourAtlaSelect(colorEnum: ThermalColorEnum)
}