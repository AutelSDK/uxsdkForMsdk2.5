package com.autel.setting.bean

/**
 * @author
 * @date 2023/8/4
 * 文件类型
 */
enum class FileTypeEnum(val value: Int, val tag: String) {
    /**
     * 文件夹
     */
    FOLDER(0, "folder"),

    /**
     * AI模型文件
     */
    AI_MODE(1, ".tar.gz"),

    /**
     * 普通文件
     */
    FILE(2, "file"),
}