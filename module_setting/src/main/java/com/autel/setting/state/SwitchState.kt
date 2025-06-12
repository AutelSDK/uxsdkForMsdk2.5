package com.autel.setting.state

import com.autel.common.base.BaseFragment

sealed class SwitchState {

    object Nothing : SwitchState()

    /**
     * fragment 切换
     *
     */
    data class switchFragment(var fragment: BaseFragment, var tag: String?) : SwitchState()

    /**
     * fragment 添加
     * @param tag 标题
     * @param boolean 是否有返回键
     */
    data class addFragment(
        var fragment: BaseFragment,
        var tag: String?,
        var boolean: Boolean? = true
    ) : SwitchState()

    /**
     * 设置顶部标题 直接传字符
     */
    data class setTitleText(var title: String) : SwitchState()

    /**
     * 设置顶部标题 字符ID
     */
    data class setTitle(var id: Int) : SwitchState()

    /**
     * 消失Dialog
     */
    data class dismiss(var boolean: Boolean? = true) : SwitchState()

    /**
     * 返回上一步
     */
    data class back(var boolean: Boolean? = true): SwitchState()
}