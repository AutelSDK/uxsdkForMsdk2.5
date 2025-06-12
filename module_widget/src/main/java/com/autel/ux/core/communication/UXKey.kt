package com.autel.ux.core.communication

import com.autel.ux.core.communication.UXKeys.UpdateType
import kotlin.reflect.KClass

data class UXKey(
    val key: String,
    val valueType: KClass<*>,
    val keyPath: String,
    val updateType: UpdateType,
)