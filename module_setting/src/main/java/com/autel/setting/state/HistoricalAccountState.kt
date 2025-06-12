package com.autel.setting.state

import com.autel.data.bean.entity.HistoricalAccountModel

sealed class HistoricalAccountState {

    object Loading : HistoricalAccountState()

    data class OnError(var string: String) : HistoricalAccountState()

    data class AccountList(var list: List<HistoricalAccountModel>) : HistoricalAccountState()
}