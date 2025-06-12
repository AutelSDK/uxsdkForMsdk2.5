package com.autel.widget.widget.lenszoom

/**
 * Created by  2023/6/7
 * 变焦改变监听
 */
interface IZoomChangeListener {
    /** 变焦变化*/
    fun zoomChange(zoomMulti100: Int)

    /** 快速变焦*/
    fun quickZoom(zoomMulti100: Int)

    /**
     * 页面关闭
     */
    fun zoomDismiss()
}