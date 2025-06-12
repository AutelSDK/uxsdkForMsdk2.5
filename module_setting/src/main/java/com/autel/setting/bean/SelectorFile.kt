package com.autel.setting.bean

import java.io.File

data class SelectorFile(val file: File, var isSelect: Boolean = false) {
    /**
     * 多选模式
     */
    var isMultipleSelectMode = false
}
