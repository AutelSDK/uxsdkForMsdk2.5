package com.autel.setting.infc

/**
 * @author 
 * @date 2023/8/10
 * 上传回调
 */
interface UploadListener {
    /**
     * 刷新当前状态
     */
    fun notifyItemChanged()

    /**
     * 上传取消
     */
    fun uploadCancel()

    /**
     * 上传
     */
    fun uploadFailure()

    /**
     * 上传成功
     */
    fun uploadSuccess()

    /**
     * 切换普通模式失败，需要重启飞机
     */
    fun exitFailure()

    /**
     * 切换告诉模式失败
     */
    fun enterFailure()
}