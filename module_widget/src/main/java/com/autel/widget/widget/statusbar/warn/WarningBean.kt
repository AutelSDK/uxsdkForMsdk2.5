package com.autel.widget.widget.statusbar.warn

import android.content.Context
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.common.enums.WaringIdEnum

/**
 * Created by  2022/12/3
 */
data class WarningBean(
    var warnId: WaringIdEnum = WaringIdEnum.UNKNOWN, //消息id
    val warnLevel: WarnLevel,
    var tip: TipType,       //展示形式或样式（dialog、toast、popupWindow）
    var voice: Boolean = true,          //语音播报
    var deviceName: String? = null,
    var deviceId: Int = 0,
    var detailMsg: String? = null, //用于描述告警详情的文案，有则在第二行显示
    val showDroneName :Boolean = true // 多机情况下,弹框告警是否显示飞机名称
) : Comparable<WarningBean> {
    var time: Long = System.currentTimeMillis()//时间戳

    //弹窗、toast、window是否已展示
    var markedNew: Boolean = false

    //用于判断是否需要播放语音
    var voiceNew: Boolean = false

    fun content(context: Context?): String {
        return if (tip.contentRes != 0) {
            context?.getString(tip.contentRes).orEmpty()
        } else if (tip.contentStr.isNotEmpty()) {
            tip.contentStr
        } else {
            detailMsg ?: ""
        }
    }

    override fun compareTo(other: WarningBean): Int {
        //不在列表显示的优先级最高
        if (tip.addInList != other.tip.addInList) {
            if (tip.addInList) -1 else 1
        }
        if (warnLevel.value != other.warnLevel.value) {
            return other.warnLevel.value - warnLevel.value
        }
        return tip.priority - other.tip.priority
    }

    open class TipType(
        var priority: Int = 0,
        var addInList: Boolean = true,
        var contentRes: Int,
        var contentStr: String
    ) {

        class TipDialog(
            priority: Int = 0,
            addInList: Boolean = true,
            contentRes: Int,
            contentStr: String = "",
            val msgRes: Int,
            val leftBtnRes: Int = 0,
            val rightBtnRes: Int,
            val rightBtnAction: Action? = null,
            val leftBtnAction: Action? = null,
            val imageRes: Int? = null,
            val msgTag: Int? = null
        ) : TipType(priority, addInList, contentRes, contentStr) {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                if (!super.equals(other)) return false

                other as TipDialog

                if (msgRes != other.msgRes) return false
                if (leftBtnRes != other.leftBtnRes) return false
                if (rightBtnRes != other.rightBtnRes) return false
                if (rightBtnAction != other.rightBtnAction) return false
                if (leftBtnAction != other.leftBtnAction) return false
                if (imageRes != other.imageRes) return false
                if (msgTag != other.msgTag) return false

                return true
            }

            override fun hashCode(): Int {
                var result = super.hashCode()
                result = 31 * result + msgRes
                result = 31 * result + leftBtnRes
                result = 31 * result + rightBtnRes
                result = 31 * result + rightBtnAction.hashCode()
                result = 31 * result + (leftBtnAction?.hashCode() ?: 0)
                result = 31 * result + (imageRes ?: 0)
                result = 31 * result + (msgTag ?: 0)
                return result
            }
        }

        class TipToast(
            priority: Int = 0,
            addInList: Boolean = true,
            contentRes: Int,
            contentStr: String = "",
        ) : TipType(priority, addInList, contentRes, contentStr) {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                if (!super.equals(other)) return false
                return true
            }

        }

        class PermanentToastTip(
            priority: Int = 0,
            addInList: Boolean = true,
            contentRes: Int,
            contentStr: String = "",
        ) : TipType(priority, addInList, contentRes,contentStr) {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                if (!super.equals(other)) return false
                return true
            }

            override fun hashCode(): Int = super.hashCode()
        }

        class TipWindow(
            priority: Int = 0,
            addInList: Boolean = true,
            contentRes: Int,
            contentStr: String = "",
            val action: Action? = null
        ) : TipType(priority, addInList, contentRes, contentStr) {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                if (!super.equals(other)) return false

                other as TipWindow

                if (action != other.action) return false

                return true
            }

            override fun hashCode(): Int {
                var result = super.hashCode()
                result = 31 * result + (action?.hashCode() ?: 0)
                return result
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as TipType

            if (priority != other.priority) return false
            if (addInList != other.addInList) return false
            if (contentRes != other.contentRes) return false

            return true
        }

        override fun hashCode(): Int {
            var result = priority
            result = 31 * result + addInList.hashCode()
            result = 31 * result + contentRes
            return result
        }
    }

    enum class WarnLevel(val value: Int) {
        HIGH_TIP(1),
        MIDDLE_TIP(2),
        NO_FLY(3),
    }

    enum class Action {
        IMU_CALI,       //IMU校准
        GOLanding,       //降落
        RETURNLAND,      //返航
        CANCEL_RETURNLAND, //取消返航
        COMPASS_CALI,//指南针校准
        CONNECTING_AIRCRAFT,//对频
        RC_CALI,//遥控器校准
        RID_MSG,//RID
        RC_COMPASS_CALL, //遥控器指南针校准
        UOM,//UOM
        ACTIVATE_DRONE,//激活飞机
        SHOW_ARM_UNFOLD_DIALOG, //显示机臂折叠
        SHOW_BATTERY_INSTALL_DIALOG//显示电池安装
    }

    override fun toString(): String {
        return "WarningBean(warnId=$warnId, warnLevel=$warnLevel, voice=$voice, deviceName=$deviceName, deviceId=$deviceId, detailMsg=$detailMsg, time=$time, markedNew=$markedNew, voiceNew=$voiceNew)"
    }
}