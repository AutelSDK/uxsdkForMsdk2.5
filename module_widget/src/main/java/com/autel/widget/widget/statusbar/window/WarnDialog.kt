package com.autel.widget.widget.statusbar.window

import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import com.autel.widget.databinding.DialogWarnBinding
import com.autel.widget.widget.statusbar.warn.WarningBean
import com.autel.common.widget.dialog.BaseAutelDialog

/**
 * Created by  2023/1/29
 * 告警弹窗
 */
class WarnDialog(context: Context) : BaseAutelDialog(context) {

    private val binding: DialogWarnBinding = DialogWarnBinding.inflate(LayoutInflater.from(context))
    private var countDownTimer: CountDownTimer? = null
    private var handleAction: IAction? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setCanceledOnTouchOutside(false)
    }


    fun setHandleAction(handleAction: IAction) {
        this.handleAction = handleAction
    }

    fun setWarnTip(warn: WarningBean, droneCount: Int) {
        when (val tip = warn.tip) {
            is WarningBean.TipType.TipDialog -> {
                if (tip.msgRes != 0) {
                    if (tip.msgTag != null) {
                        binding.tvMsg.text = context.getString(tip.msgRes, "${tip.msgTag}s")
                    } else {
                        binding.tvMsg.text = context.getString(tip.msgRes)
                    }
                    binding.tvMsg.visibility = View.VISIBLE
                } else {
                    binding.tvMsg.visibility = View.GONE
                }

                if (tip.contentRes != 0) {
                    if (warn.deviceName.isNullOrEmpty()) {
                        binding.tvTitle.text = context.getString(tip.contentRes)
                    } else {
                        if (droneCount > 1 && warn.showDroneName) {//多个飞机时，需要添加飞机名称
                            binding.tvTitle.text = "${'"'}${warn.deviceName}${'"'}" + context.getString(tip.contentRes, droneCount)
                        } else {
                            binding.tvTitle.text = context.getString(tip.contentRes)
                        }
                    }
                    binding.tvTitle.visibility = View.VISIBLE
                } else if (tip.contentStr.isNotEmpty()) {
                    binding.tvTitle.setText(tip.contentStr)
                    binding.tvTitle.visibility = View.VISIBLE
                } else {
                    binding.tvTitle.visibility = View.GONE
                }

                if (tip.imageRes != 0 && tip.imageRes != null) {
                    binding.ivTip.setImageResource(tip.imageRes)
                    binding.ivTip.visibility = View.VISIBLE
                } else {
                    binding.ivTip.visibility = View.GONE
                }

                if (tip.leftBtnRes != 0) {
                    binding.tvCancel.text = context.getString(tip.leftBtnRes)
                    binding.tvCancel.visibility = View.VISIBLE
                    binding.divideLine.visibility = View.VISIBLE
                } else {
                    binding.tvCancel.visibility = View.GONE
                    binding.divideLine.visibility = View.GONE
                }

                if (tip.rightBtnRes != 0) {
                    binding.tvConfirm.text = context.getString(tip.rightBtnRes)
                    binding.tvConfirm.visibility = View.VISIBLE
                } else {
                    binding.tvConfirm.visibility = View.GONE
                }

                if (tip.leftBtnRes == 0 && 0 == tip.rightBtnRes) {
                    binding.divideLine.visibility = View.GONE
                    binding.horizontalLine.visibility = View.GONE
                }

                if (tip.leftBtnAction != null) {
                    binding.tvCancel.setOnClickListener {
                        handleAction?.handleAction(tip.leftBtnAction)
                    }
                } else {
                    binding.tvCancel.setOnClickListener {
                        dismiss()
                    }
                }
                binding.tvConfirm.setOnClickListener {
                    if (tip.rightBtnAction != null){
                        handleAction?.handleAction(tip.rightBtnAction)
                    } else {
                        dismiss()
                    }
                }

                countDownTimer?.cancel()
                if (tip.msgTag != null) {
                    countDownTimer = object : CountDownTimer(tip.msgTag * 1000L, 500L) {
                        override fun onTick(millisUntilFinished: Long) {
                            val time = millisUntilFinished / 1000L
                            if (time != 0L) {
                                binding.tvMsg.text = context.getString(tip.msgRes, "${time}s")
                            }
                        }

                        override fun onFinish() {
                            dismiss()
                        }
                    }
                    countDownTimer?.start()
                }
            }
        }

    }

    interface IAction {
        fun handleAction(action: WarningBean.Action)
    }

}