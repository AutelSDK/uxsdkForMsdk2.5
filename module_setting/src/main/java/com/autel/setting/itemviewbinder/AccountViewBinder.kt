package com.autel.setting.itemviewbinder

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.autel.common.feature.recyclerview.DefaultViewHolder
import com.autel.setting.databinding.SettingRtkHistoricalAccountItemBinding
import com.autel.data.bean.entity.HistoricalAccountModel
import com.drakeet.multitype.ItemViewBinder

class AccountViewBinder(
    private val context: Context,
    private val delClick: (HistoricalAccountModel) -> Unit,
    private val rootClick: (HistoricalAccountModel) -> Unit
) : ItemViewBinder<HistoricalAccountModel, DefaultViewHolder<SettingRtkHistoricalAccountItemBinding>>() {
    companion object {
        const val TAG = "AccountViewBinder"
    }

    override fun onCreateViewHolder(inflater: LayoutInflater, parent: ViewGroup): DefaultViewHolder<SettingRtkHistoricalAccountItemBinding> {
        return DefaultViewHolder(SettingRtkHistoricalAccountItemBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: DefaultViewHolder<SettingRtkHistoricalAccountItemBinding>, item: HistoricalAccountModel) {
        with(holder.dataBinding) {
            tvAccount.text = item.account
            tvServerAddr.text = item.serverAddr
            tvPort.text = item.port
            tvMountPoint.text = item.mountPoint

            root.setOnClickListener {
                rootClick.invoke(item)
            }

            ivDelete.setOnClickListener {
                delClick.invoke(item)
            }
        }
    }

}