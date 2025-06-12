package com.autel.ux.core.base.widget

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import androidx.annotation.CallSuper
import androidx.constraintlayout.widget.ConstraintLayout
import com.autel.log.AutelLog
import com.autel.ux.core.base.BaseWidget
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.launch

abstract class ConstraintLayoutWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr), BaseWidget {

    protected val TAG = this::class.java.simpleName

    private var widgetScope: CoroutineScope? = null

    protected val layoutInflater: LayoutInflater
        get() = LayoutInflater.from(context)

    init {
        initView(context, attrs, defStyleAttr)
    }

    @CallSuper
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        AutelLog.d(TAG, "<onAttachedToWindow>")
        if (isInEditMode) {
            return
        }
        widgetScope?.cancel()
        widgetScope = CoroutineScope(Dispatchers.Main.immediate + CoroutineExceptionHandler { _, t ->
            AutelLog.e(TAG, "widget scope error :${Log.getStackTraceString(t)}")
        })
        reactToModelChanges()
    }

    @CallSuper
    override fun onDetachedFromWindow() {
        AutelLog.d(TAG, "<onDetachedFromWindow>")
        widgetScope?.cancel()
        widgetScope = null
        super.onDetachedFromWindow()
    }

    protected abstract fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int)

    protected abstract fun reactToModelChanges()

    protected fun <T> Flow<T>.collectInWidget(collector: FlowCollector<T>) {
        widgetScope?.launch(Dispatchers.Main.immediate + CoroutineName(TAG) + CoroutineExceptionHandler { _, t ->
            AutelLog.e(TAG, "CoroutineExceptionHandler caught exception when collectInWidget: $t")
        }) {
            this@collectInWidget.collect(collector)
        }
    }

    protected fun logi(message: String) {
        AutelLog.i(TAG, message)
    }

    protected fun loge(message: String, e: Throwable? = null) {
        AutelLog.e(TAG, "$message  ${if (e == null) "" else Log.getStackTraceString(e)}")
    }

    protected fun logd(message: String) {
        AutelLog.d(TAG, message)
    }
}