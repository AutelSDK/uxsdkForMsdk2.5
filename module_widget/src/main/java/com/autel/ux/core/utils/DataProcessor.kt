package com.autel.ux.core.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class DataProcessor<T> private constructor(private var defaultValue: T) {

    companion object {

        fun <T> create(defaultValue: T): DataProcessor<T> {
            return DataProcessor<T>(defaultValue)
        }
    }

    private var flow: MutableStateFlow<T> = MutableStateFlow(defaultValue)

    fun emit(value: T?) {
        if (value != null) {
            flow.value = value
        }
    }

    fun getValue(): T {
        val value = flow.value
        if (value == null) {
            return defaultValue
        }
        return flow.value
    }

    fun toFlow(): StateFlow<T> = flow
}