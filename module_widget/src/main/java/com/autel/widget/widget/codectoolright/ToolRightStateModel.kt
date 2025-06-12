package com.autel.widget.widget.codectoolright

import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.camera.bean.RecordStatusBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.camera.enums.TakePhotoStatusEnum


data class ToolRightStateModel(
    val connected: Boolean, //是否连接
    val isSingleControl: Boolean, //是否单控
    val isMainRc: Boolean, //是否为主遥控
    val photoStatus: TakePhotoStatusEnum?, //照片状态
    val recordStatus: RecordStatusBean?, //录像状态
    val isMissionMode: Boolean  //是否为任务模式
)