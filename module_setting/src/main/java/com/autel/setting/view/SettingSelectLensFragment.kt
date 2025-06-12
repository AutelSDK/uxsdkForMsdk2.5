package com.autel.setting.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.autel.common.base.BaseFragment
import com.autel.common.extension.SELECT_NULL_LENS
import com.autel.common.extension.getCastScreenMainLen
import com.autel.common.extension.getCastScreenSelectSideLens
import com.autel.common.extension.getSortValue
import com.autel.common.lifecycle.LiveDataBus
import com.autel.common.lifecycle.event.CastScreenEvent
import com.autel.common.manager.AutelStorageManager
import com.autel.common.manager.StorageKey
import com.autel.common.utils.DeviceUtils
import com.autel.drone.sdk.vmodelx.module.camera.bean.LensTypeEnum
import com.autel.setting.databinding.SettingLensSelectBinding
import com.autel.utils.ScreenUtils
import com.autel.widget.widget.AutelDividerItemDecoration
import com.autel.setting.view.binder.LensAdapter

class SettingSelectLensFragment : BaseFragment() {

    private var binding: SettingLensSelectBinding? = null

    private var lensInfoList: ArrayList<LensTypeEnum> = ArrayList()
    private val selectLens = mutableListOf<LensTypeEnum>()

    private val storageAdapter: LensAdapter by lazy { LensAdapter(lensInfoList, selectLens) }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return SettingLensSelectBinding.inflate(inflater).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding?.run {
            rvStore.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            rvStore.addItemDecoration(AutelDividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL).apply {
                setDividerMarginStart(ScreenUtils.dp2px(requireContext(), 10))
            })
            rvStore.adapter = storageAdapter
            storageAdapter.setOnItemClickListener { _, _, position ->
                if (selectLens.contains(lensInfoList[position])) {
                    selectLens.remove(lensInfoList[position])
                } else {
                    selectLens.add(lensInfoList[position])
                }
                storageAdapter.notifyDataSetChanged()
                if (selectLens.isEmpty()) {
                    AutelStorageManager.getPlainStorage().setStringValue(StorageKey.PlainKey.KEY_CAST_SIDE_SCREEN, SELECT_NULL_LENS)
                } else {
                    AutelStorageManager.getPlainStorage()
                        .setStringValue(StorageKey.PlainKey.KEY_CAST_SIDE_SCREEN, selectLens.joinToString(",") { it.value })
                }
                LiveDataBus.of(CastScreenEvent::class.java).updateSideCastScreen().post(selectLens)
            }

            val drone = DeviceUtils.singleControlDrone() ?: return
            val lens = drone.getCameraAbilitySetManger().getLensList()?.sortedBy { it.getSortValue() } ?: emptyList()
            lensInfoList.addAll(lens)
            lensInfoList.removeAll { it == getCastScreenMainLen() }

            selectLens.addAll(getCastScreenSelectSideLens())
            //移除selectLens中,不存在lensInfoList中的数据
            selectLens.removeAll { !lens.contains(it) }

            storageAdapter.notifyDataSetChanged()
        }
    }
}