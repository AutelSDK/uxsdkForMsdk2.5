package com.autel.setting.intent

import com.autel.data.bean.entity.HistoricalAccountModel

sealed class HistoricalAccountIntent {

    // 查询所有未使用的账号
    object QueryAllAccountUnused : HistoricalAccountIntent()

    // 把正在使用的账号改为未使用
    object UpdateUsingToUnuse : HistoricalAccountIntent()

    // 删除正在使用的账号
    object DelAccountOnUsing : HistoricalAccountIntent()

    // 删除一个账号
    data class DelOneAccount(var ham: HistoricalAccountModel) : HistoricalAccountIntent()

    // 存储一个账号
    data class SaveOneAccount(var ham: HistoricalAccountModel) : HistoricalAccountIntent()
}