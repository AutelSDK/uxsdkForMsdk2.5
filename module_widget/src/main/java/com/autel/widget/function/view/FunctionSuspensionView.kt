package com.autel.widget.function.view

import android.content.Context
import android.graphics.drawable.TransitionDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.autel.common.manager.AutelStorageManager
import com.autel.common.manager.StorageKey
import com.autel.widget.R

/**
 * Created by  2023/12/18
 * 功能悬浮组件
 */
class FunctionSuspensionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : DragFloatView(context, attrs, defStyleAttr) {

    private var windowFocus = false
    private var transitionDrawable: TransitionDrawable? = null

    //按压抬起的操作
    private var actionUpRunnable = Runnable {
        val layers = arrayOf(
            resources.getDrawable(R.drawable.common_icon_float_press),
            resources.getDrawable(R.drawable.common_icon_float)
        )
        transitionDrawable = TransitionDrawable(layers)

        setImageDrawable(transitionDrawable)

        transitionDrawable?.startTransition(1000)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val result = super.onTouchEvent(event)
        if (event.action == MotionEvent.ACTION_DOWN) {
            //手指下按压
            setImageResource(R.drawable.common_icon_float_press)
            handler.removeCallbacks(actionUpRunnable)
        } else if (event.action == MotionEvent.ACTION_UP) {
            //手指抬起
            handler.postDelayed(actionUpRunnable, 5000L)
        }
        if (isDragging && event.action == MotionEvent.ACTION_UP) {
            AutelStorageManager.getPlainStorage().setFloatValue(StorageKey.PlainKey.KEY_FUNCTION_SUSPENSION_VIEW_X, getTargetX())
            AutelStorageManager.getPlainStorage().setFloatValue(StorageKey.PlainKey.KEY_FUNCTION_SUSPENSION_VIEW_Y, getTargetY())
        }
        return result
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (!windowFocus) {
            val parentWidth = (parent as View).width
            val parentHeight = (parent as View).height

            windowFocus = true

            val x = AutelStorageManager.getPlainStorage().getFloatValue(StorageKey.PlainKey.KEY_FUNCTION_SUSPENSION_VIEW_X, 0f)
            val y = AutelStorageManager.getPlainStorage().getFloatValue(StorageKey.PlainKey.KEY_FUNCTION_SUSPENSION_VIEW_Y, 0f)
            val translationX = x - width / 2
            val translationY = y - height / 2

            // 限制控件在屏幕内移动
            val newX = translationX.coerceIn(0f, (parentWidth - width).toFloat())
            val newY = translationY.coerceIn(0f, (parentHeight - height).toFloat())



            animate().x(newX).y(newY).setDuration(0).start()

        }
    }
}