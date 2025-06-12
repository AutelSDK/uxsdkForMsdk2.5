package com.autel.setting.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.addTextChangedListener
import com.autel.setting.databinding.SettingItemExpAdjustBinding

/**
 * @Author create by LJ
 * @Date 2022/10/20 09
 */
class ExpAdjustView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : ConstraintLayout(context, attrs, defStyleAttr),
    ExpView.OnExpChangeListener {

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0) {
        initView()
    }

    var rootView: SettingItemExpAdjustBinding? = null

    lateinit var listener: ((value: Float) -> Unit)

    open fun setOnExpAdjustViewListener(listener: ((value: Float) -> Unit)) {
        this.listener = listener
    }

    private fun initView() {
        rootView = SettingItemExpAdjustBinding.inflate(LayoutInflater.from(context))
        rootView?.settingExpImg?.setOnExpChangeListener(this)
        rootView?.settingEditExpNumber?.setAutelEditTextListener { value ->
            setExp(value)
            listener?.invoke(value)
        }
        addView(rootView?.root)
    }

    fun setTip(topText: String, bottomText: String) {
        rootView?.settingTvExpUp?.text = topText
        rootView?.settingTvExpDown?.text = bottomText
    }

    fun setExp(exp: Float) {
        rootView?.settingExpImg?.setExp(exp)
        rootView?.settingEditExpNumber?.setText("$exp")
    }

    fun getExp(): ExpView? {
        return rootView?.settingExpImg
    }

    override fun expChange(exp: Float, isSend: Boolean) {
        rootView?.settingEditExpNumber?.setText("$exp")
        listener?.let {
            if (isSend) {
                listener.invoke(exp)
            }
        }

    }

}