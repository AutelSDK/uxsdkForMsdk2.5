package com.autel.widget.widget

import android.content.Context
import android.text.Editable
import android.text.InputFilter
import android.text.Spanned
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import com.autel.common.R
import com.autel.widget.databinding.ViewCommonInputBinding

class CommonInputView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    private val binding: ViewCommonInputBinding = ViewCommonInputBinding.inflate(LayoutInflater.from(context), this, true)


    private val inputFilter: InputFilter = object : InputFilter {
        override fun filter(
            source: CharSequence, start: Int,
            end: Int, dest: Spanned, dstart: Int, dend: Int,
        ): CharSequence {
            for (index in start until end) {
                val type: Int = Character.getType(source[index])
                if (type == Character.SURROGATE.toInt()) {
                    return ""
                }
            }
            return source
        }
    }

    init {
        initListener()
    }

    fun setInputContent(name: String?) {
        binding.input.setText(name)
    }

    fun getInputContent(): String {
        return binding.input.text.toString()
    }

    fun setChangeListener(listener: ((String) -> Unit)) {

        binding.input.filters = arrayOf(inputFilter, InputFilter.LengthFilter(64))

        binding.input.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                listener.invoke(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })
    }

    fun initListener() {
        binding.ivClean.setOnClickListener {
            binding.input.setText("")
        }
    }

    fun clearInputFocus() {
        try {
            binding.input.clearFocus()
            val imm: InputMethodManager? = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
            imm?.hideSoftInputFromWindow(windowToken, 0)
        } catch (e: Exception) {
        }
    }

}