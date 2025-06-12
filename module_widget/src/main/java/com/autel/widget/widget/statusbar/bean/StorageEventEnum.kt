package com.autel.widget.widget.statusbar.bean

/**
 * @author 
 * @date 2023/2/11
 * 存储设备事件
 */
enum class StorageEventEnum {
    UNKNOWN,//未知
    SDCARD_OUT_TO_CHANGE_EMMC,//SD卡已拔出，切换到机载闪存
    DETECTED_SDCARD_TO_CHANGE,//检测到SD卡，切换到SD卡
    SDCARD_ERROR_TO_CHANGE_EMMC,//SD卡错误，切换到机载闪存 插卡和相机心跳上报
    SDCARD_FULL_TO_CHANGE_EMMC,//SD卡已满，切换到机载闪存
    SDCARD_UNKNOWN_FILESYSTEM_TO_CHANGE_EMMC,//未知的SD卡文件系统，提示切换机载闪存，或者格式化
    CLEAN_ALBUM,//清理相册
    EMMC_CHANGE_TO_TF,//机载闪存已满，切换到TF卡

}