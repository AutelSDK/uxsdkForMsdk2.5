package com.autel.setting.bean

/**
 * @author 
 * @date 2023/8/4
 * 文件详情
 */
data class FileInfoBean(
    /**
     * 文件类型
     */
    var fileType: FileTypeEnum = FileTypeEnum.FILE,
    /**
     * 文件路径
     */
    var filePath: String = "",
    /**
     * 文件名
     */
    var fileName: String = "",

    /**
     * 序号
     */
    var index: Int = 0,

    /**
     * 是否显示下划线
     */
    var showBottomLine: Boolean = false,

    /**
     * 是否是编辑态
     */
    var isEdit: Boolean = false,

    /**
     * 是否选中
     */
    var isChoose: Boolean = false
)