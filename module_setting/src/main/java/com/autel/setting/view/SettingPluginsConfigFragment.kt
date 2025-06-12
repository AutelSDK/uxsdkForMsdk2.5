package com.autel.setting.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.autel.common.base.BaseAircraftFragment
import com.autel.drone.sdk.vmodelx.module.payload.PayloadIndexType
import com.autel.drone.sdk.vmodelx.module.payload.data.PayloadWidgetInfo
import com.autel.drone.sdk.vmodelx.module.payload.widget.PayloadWidget
import com.autel.log.AutelLog
import com.autel.setting.databinding.SettingFragmentPluginsConfigBinding
import com.autel.setting.utils.payload.PluginsDataManager
import com.autel.setting.view.binder.SettingPluginsConfigAdapter

/**
 * Copyright: Autel Robotics
 * @author R24033 on 2025/4/24
 * 三方负载配置界面
 */
class SettingPluginsConfigFragment : BaseAircraftFragment() {

    private lateinit var binding: SettingFragmentPluginsConfigBinding
    private var payloadIndexType: PayloadIndexType = PayloadIndexType.UNKNOWN

    companion object {
        private const val TAG = "SettingPluginsConfigFragment"
        const val PARAMS_PAYLOAD_POSITION = "params_payload_position"

        fun newInstance(type: PayloadIndexType): SettingPluginsConfigFragment {
            val fragment = SettingPluginsConfigFragment()
            val args = Bundle()
            args.putInt(PARAMS_PAYLOAD_POSITION, type.value)
            fragment.arguments = args
            return fragment
        }
    }

    //列表
    private var adapter: SettingPluginsConfigAdapter? = null
    private val configList: MutableList<PayloadWidget> = ArrayList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = SettingFragmentPluginsConfigBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val type = arguments?.getInt(PARAMS_PAYLOAD_POSITION, -1) ?: -1
        payloadIndexType = PayloadIndexType.findType(type)

        //列表
        adapter = SettingPluginsConfigAdapter(payloadIndexType,onItemClick = { position, widget -> })

        binding.recyclerviewConfig.adapter = adapter
        binding.recyclerviewConfig.layoutManager = LinearLayoutManager(context)
    }

    override fun getData() {
        //widget信息
        PluginsDataManager.getInstance().widgetInfoLiveData
            .observe(viewLifecycleOwner) {
                parseConfigData(it)
            }
    }

    /**
     * 解析配置项数据
     */
    private fun parseConfigData(hashMap: HashMap<PayloadIndexType, PayloadWidgetInfo>) {
        if (payloadIndexType == PayloadIndexType.UNKNOWN) {
            AutelLog.e(TAG, "parseConfigData->payloadIndexType:$payloadIndexType")
            return
        }

        val widget = hashMap[payloadIndexType]
        val configInterfaceList = widget?.configInterfaceWidgetList
        if (hashMap.isEmpty() || configInterfaceList.isNullOrEmpty()) {
            AutelLog.e(TAG, "parseConfigData->widget data is empty")
            return
        }
        if (configList.isNotEmpty()) {
            configList.clear()
        }
        configList.addAll(configInterfaceList)
        adapter?.submitList(configList)
    }
}