package com.autel.widget.function

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.autel.common.extension.asLiveData
import com.autel.common.manager.AutelStorageManager
import com.autel.common.manager.StorageKey
import com.autel.common.delegate.function.FunctionBarState

/**
 * Created by  2023/5/22
 *  工具栏VM
 */
class FunctionBarVM : ViewModel() {

    /** 工具栏的展开状态 */
    private val _functionBarLD = MutableLiveData<FunctionBarState>(FunctionBarState.Unfolded)
    val functionBarLD = _functionBarLD.asLiveData()

    /**
     * @return 更新functionBar状态
     */
    fun updateFunctionBarLD(state: FunctionBarState) {
        AutelStorageManager.getPlainStorage()
            .setBooleanValue(StorageKey.PlainKey.TOOLBAR_STATE_FOLDED, state == FunctionBarState.Folded)
        if (_functionBarLD.value == state) {
            return
        }
        _functionBarLD.value = state
    }

    fun isUnFoldedState(): Boolean {
        return functionBarLD.value == FunctionBarState.Unfolding || functionBarLD.value == FunctionBarState.Unfolded
    }
}