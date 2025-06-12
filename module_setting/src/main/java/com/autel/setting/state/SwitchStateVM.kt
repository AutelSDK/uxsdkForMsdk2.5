package com.autel.setting.state


import com.autel.common.base.BaseFragment
import com.autel.common.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class SwitchStateVM : BaseViewModel() {

    private var _switchState = MutableStateFlow<SwitchState>(SwitchState.Nothing)
    val switchState = _switchState

    /**
     * fragment 切换
     *
     */
    fun switchFragment(fragment: BaseFragment, tag: String?) {
        _switchState.value = SwitchState.switchFragment(fragment, tag)
    }

    /**
     * fragment 添加
     * @param tag 标题
     * @param boolean 是否有返回键
     */
    fun addFragment(fragment: BaseFragment, tag: String?, boolean: Boolean? = true) {
        _switchState.value = SwitchState.addFragment(fragment, tag, boolean)
    }

    /**
     * 设置顶部标题 直接传字符
     */
    fun setTitleText(title: String) {
        _switchState.value = SwitchState.setTitleText(title)
    }

    /**
     * 设置顶部标题 字符ID
     */
    fun setTitle(id: Int) {
        _switchState.value = SwitchState.setTitle(id)
    }

    /**
     * 消失Dialog
     */
    fun dismiss() {
        _switchState.value = SwitchState.dismiss(true)
    }

    fun destroy() {
        _switchState.value = SwitchState.Nothing
    }

    fun back(){
        _switchState.value = SwitchState.back(true)
    }
}