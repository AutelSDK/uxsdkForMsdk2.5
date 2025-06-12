package com.autel.widget.widget.attitude.vm

import com.autel.common.base.widget.BaseWidgetModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * 姿态数据
 */
abstract class AttitudeWM : BaseWidgetModel() {

    val droneAttitudeData = MutableSharedFlow<AttitudeDataBean>(replay = 1, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
}