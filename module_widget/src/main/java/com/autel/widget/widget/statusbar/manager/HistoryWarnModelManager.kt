package com.autel.widget.widget.statusbar.manager

import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.common.bean.WarningAtom
import com.autel.widget.widget.statusbar.warn.WarningBean
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Created by  2022/12/3
 */
class HistoryWarnModelManager {

    //飞机的检查内容
    private val mDroneChecker = DroneChecker()

    private val _warnMsg = MutableSharedFlow<List<WarningBean>>(1, 0, BufferOverflow.DROP_OLDEST)
    val warnMsg: Flow<List<WarningBean>> = _warnMsg

    fun checkAtomLists(warningAtomList: List<WarningAtom>) {
        val warns = mutableListOf<WarningBean>()
        for (warningAtom in warningAtomList) {
            mDroneChecker.generate(warningAtom.warningId, true)?.also { warns.add(it) }
        }
        _warnMsg.tryEmit(warns)
    }
}
