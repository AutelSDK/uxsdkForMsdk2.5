package com.autel.widget.widget.statusbar.warn

import android.content.Context

/**
 * Created by  2022/12/3
 */
data class CheckEntry(
    /** 未连接飞机 **/
    var connected: WarningBean? = null,
    /** 是否有弹窗信息 **/
    var dialog: ArrayList<WarningBean>,
    /** 是否有Toast信息 **/
    var toast: ArrayList<WarningBean>,
    /** 告警信息列表 **/
    var warns: ArrayList<WarningBean>,
    /**
     * 常驻toast告警
     */
    var permanentToast : ArrayList<WarningBean>,
    /** 标题上是否展示指定信息 **/
    var appendId: Int = 0,
    /**关联飞机名称*/
    var deviceName: String? = null,
    /**关联飞机ID*/
    var deviceId: Int = 0
) {
    fun getSeriousWaringNum(): Int {
        var count = 0
        warns.forEach {
            if (it.warnLevel == WarningBean.WarnLevel.HIGH_TIP) {
                count++
            }
        }
        dialog.forEach {
            if (it.warnLevel == WarningBean.WarnLevel.HIGH_TIP) {
                count++
            }
        }
        return count
    }

    fun getGeneralWarningNum(): Int {
        var count = 0
        warns.forEach {
            if (it.warnLevel == WarningBean.WarnLevel.MIDDLE_TIP) {
                count++
            }
        }
        dialog.forEach {
            if (it.warnLevel == WarningBean.WarnLevel.MIDDLE_TIP) {
                count++
            }
        }
        return count
    }

    fun getContent(context: Context): String {
        val stringBuilder = StringBuilder()
        var warnIndex = 0
        warns.forEachIndexed { index, warningBean ->
            if (warningBean.warnLevel != WarningBean.WarnLevel.NO_FLY) {
                if (index == 0) {
                    stringBuilder.append(warningBean.content(context))
                } else {
                    stringBuilder.append("   ").append(warningBean.content(context))
                }
                warnIndex ++
            }
        }
        dialog.forEachIndexed { _, warningBean ->
            if ( warningBean.warnLevel != WarningBean.WarnLevel.NO_FLY) {
                if (warnIndex == 0) {
                    stringBuilder.append(warningBean.content(context))
                } else {
                    stringBuilder.append("   ").append(warningBean.content(context))
                }
            }
        }
        return stringBuilder.toString()
    }
}
