package com.autel.widget.widget.statusbar

import android.content.Context
import android.util.AttributeSet
import com.autel.common.widget.OutlineTextView
import com.autel.widget.widget.statusbar.bean.FscWightModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class FlightModeText @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : OutlineTextView(context, attrs, defStyleAttr) {

    private val flightMode: FscWightModel by lazy {
        FscWightModel()
    }

    private val scope = MainScope()

    private var job: Job? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        job = scope.launch {
            flightMode.flightMode.collect {
                text = it?.txtRes?.let { res ->
                    context.getString(res)
                } ?: ""
            }
        }

        flightMode.onAttached(context)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        flightMode.onDetached()
        job?.cancel()
    }

}