package com.autel.setting.bean

import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.payload.bean.PayloadInfoBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.payload.enums.PayloadType
import com.autel.drone.sdk.vmodelx.module.payload.PayloadIndexType

/**
 * Copyright: Autel Robotics
 * @author R24033 on 2025/4/24
 */
class PluginBean {
    //负载位置
    var payloadIndexType: PayloadIndexType = PayloadIndexType.UNKNOWN
    var info: PayloadInfoBean? = null

    //负载类型
    var payloadType: PayloadType = PayloadType.PAYLOAD_UNKNOWN
    //负载名
    var pluginName: String = ""

    override fun toString(): String {
        return "PluginBean(payloadIndexType=$payloadIndexType, info=$info, payloadType=$payloadType, pluginName='$pluginName')"
    }

}