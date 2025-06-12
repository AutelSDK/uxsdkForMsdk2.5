package com.autel.setting.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.autel.common.base.BaseAircraftFragment
import com.autel.common.lifecycle.LiveDataBus
import com.autel.common.lifecycle.event.PluginsFloatingInfoEvent
import com.autel.common.lifecycle.event.PluginsFloatingInfoModel
import com.autel.common.lifecycle.event.ShowMegaRecordEvent
import com.autel.common.lifecycle.event.ShowMegaRecordModel
import com.autel.common.manager.AutelStorageManager
import com.autel.common.manager.StorageKey
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.payload.bean.PayloadInfoBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.payload.enums.PayloadType
import com.autel.drone.sdk.vmodelx.module.payload.PayloadIndexType
import com.autel.log.AutelLog
import com.autel.setting.BuildConfig
import com.autel.setting.R
import com.autel.setting.bean.PluginBean
import com.autel.setting.databinding.SettingFragmentPayloadBinding
import com.autel.setting.state.SwitchStateVM
import com.autel.setting.utils.payload.PluginsDataManager
import com.autel.setting.view.binder.PayloadPluginsAdapter
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.listener.OnItemClickListener

class SettingPayloadFragment : BaseAircraftFragment() {
    private val TAG = "SettingPayloadFragment"

    private var binding: SettingFragmentPayloadBinding? = null

    //缓存负载信息
    private var preBasicInfoMap =
        HashMap<PayloadIndexType, Pair<IAutelDroneDevice, PayloadInfoBean>>()

    //负载信息
    private var adapter: PayloadPluginsAdapter? = null
    private var dataList: MutableList<PluginBean> = ArrayList()
    private val switchVM: SwitchStateVM by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = SettingFragmentPayloadBinding.inflate(inflater, container, false)
        AutelLog.d("zxm==", "onCreateView->context:${context?.javaClass?.simpleName}")
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //负载信息
        adapter = PayloadPluginsAdapter(dataList)

        binding?.recyclerviewPlugins?.adapter = adapter
        binding?.recyclerviewPlugins?.layoutManager = LinearLayoutManager(context)

        adapter?.setOnItemClickListener(object : OnItemClickListener {
            override fun onItemClick(p0: BaseQuickAdapter<*, *>, p1: View, p2: Int) {
                val item = (p0 as PayloadPluginsAdapter).getItem(p2)
                AutelLog.d(TAG, "setOnItemClickListener->item:${item}")
                switchVM.addFragment(
                    SettingPluginsConfigFragment.newInstance(type = item.payloadIndexType),
                    item.pluginName,
                    true
                )
            }
        })
        //实时弹窗权限
        val showWindow = AutelStorageManager.getPlainStorage()
            .getBooleanValue(StorageKey.PlainKey.KEY_PLUGINS_FLOATING_INFO, true)
        binding?.cisPluginInfoTurn?.getSwitchBtn()?.isChecked = showWindow
        binding?.cisPluginInfoTurn?.setOnSwitchChangeListener {
            AutelStorageManager.getPlainStorage()
                .setBooleanValue(StorageKey.PlainKey.KEY_PLUGINS_FLOATING_INFO, it)
            //通知状态
            LiveDataBus.of(PluginsFloatingInfoEvent::class.java).switchFloatingInfoWindow()
                .post(PluginsFloatingInfoModel(it))

        }
    }


    override fun getData() {
        //负载信息
        PluginsDataManager.getInstance().basicInfoLiveData
            .observe(viewLifecycleOwner) {
//            AutelLog.d(TAG, "receive basic info, data has changed ??:$state")
                updateBasicInfo(it)
            }

    }

    /**
     * 更新负载信息
     */
    private fun updateBasicInfo(list: MutableList<PluginBean>) {
        if (dataList.isNotEmpty()) {
            dataList.clear()
        }
        AutelLog.d(TAG, "convertBasicInfo->list:$list")
        if (list.isNotEmpty()) {
            for (index in list.indices) {
                val item = list[index]
                item.apply {
                    val payloadType = item.payloadType
                    this.pluginName = if (payloadType == PayloadType.PAYLOAD_LIGHT_SPEAKER) {
                        resources.getString(R.string.common_text_loud_speaker)
                    } else {
                        resources.getString(
                            R.string.common_text_show_plugins_title,
                            item.info?.payloadId ?: index.toString()
                        )
                    }
                }

                if (BuildConfig.DEBUG) {
                    dataList.add(item)
                } else {
                    val canAdd = item.payloadType.value != 6
                    if (canAdd) {
                        dataList.add(item)
                    }
                }

            }
        }

        AutelLog.d(TAG, "convertBasicInfo->dataList:$dataList")
        adapter?.setList(dataList)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}