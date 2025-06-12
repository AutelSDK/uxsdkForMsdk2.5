package com.autel.widget.function.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.widget.AppCompatImageView


open class DragFloatView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {
    private var lastX: Float = 0f
    private var lastY: Float = 0f
    private var targetX: Float = 0f
    private var targetY: Float = 0f
    open var isDragging: Boolean = false
    private var canAdsorption: Boolean = false
    private var isClickHandled: Boolean = false

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val parentWidth = (parent as View).width
        val parentHeight = (parent as View).height
        val x = event.rawX
        val y = event.rawY

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastX = x
                lastY = y
                isDragging = false
                isClickHandled = false
            }

            MotionEvent.ACTION_MOVE -> {
                val deltaX = x - lastX
                val deltaY = y - lastY

                if (!isDragging && (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10)) {
                    isDragging = true
                }

                if (isDragging) {
                    val translationX = x - width / 2
                    val translationY = y - height / 2

                    // 限制控件在屏幕内移动
                    val newX = translationX.coerceIn(0f, (parentWidth - width).toFloat())
                    val newY = translationY.coerceIn(0f, (parentHeight - height).toFloat())

                    targetX = newX
                    targetY = newY

                    animate().x(newX).y(newY).setDuration(0).start()
                }

                lastX = x
                lastY = y
            }

            MotionEvent.ACTION_UP -> {
                if (isDragging) {
                    if (canAdsorption) {
                        val finalX = x - width / 2
                        if (finalX < parentWidth / 2) {
                            animate().x(0f).setDuration(200).start()
                        } else {
                            animate().x((parentWidth - width).toFloat()).setDuration(200).start()
                        }
                    }
                } else {
                    if (!isClickHandled) {
                        performClick()
                    }
                }
            }
        }
        return true
    }

    fun getTargetX(): Float {
        return targetX
    }

    fun getTargetY(): Float {
        return targetY
    }

    fun setCanAdsorption(canAdsorption: Boolean) {
        this.canAdsorption = canAdsorption
    }
}
