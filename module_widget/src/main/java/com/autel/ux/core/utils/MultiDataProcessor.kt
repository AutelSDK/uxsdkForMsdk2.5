package com.autel.ux.core.utils

import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class MultiDataProcessor<T> private constructor(private var defaultValue: T) {
    companion object {

        fun <T> create(defaultValue: T): MultiDataProcessor<T> {
            return MultiDataProcessor<T>(defaultValue)
        }
    }

    private var flow: MutableSharedFlow<Pair<IAutelDroneDevice, T>> = MutableSharedFlow(
        replay = 0, extraBufferCapacity = 0,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val dataMap = mutableMapOf<IAutelDroneDevice, T>()

    fun emit(data: Pair<IAutelDroneDevice, T>) {
        flow.tryEmit(data)
        dataMap[data.first] = data.second
    }

    fun getValue(device: IAutelDroneDevice): T {
        return dataMap.getOrElse(device) { defaultValue }
    }

    fun toFlow(): SharedFlow<Pair<IAutelDroneDevice, T>> = flow
}