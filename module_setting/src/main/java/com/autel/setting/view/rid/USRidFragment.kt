package com.autel.setting.view.rid

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.autel.common.base.BaseAircraftFragment
import com.autel.common.extension.launchAndCollectIn
import com.autel.common.manager.AutelStorageManager
import com.autel.common.manager.StorageKey
import com.autel.setting.R
import com.autel.setting.business.RemoteIdVM
import com.autel.setting.databinding.SettingActivityUsRidBinding

class USRidFragment : BaseAircraftFragment() {

    private lateinit var uiBinding: SettingActivityUsRidBinding

    private val viewModel: RemoteIdVM by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        uiBinding = SettingActivityUsRidBinding.inflate(inflater, container, false)
        return uiBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        uiBinding.layoutPilot.setOnClickListener {
            // 飞手ID
//            RouteManager.routeTo(requireContext(), RouterConst.PathConst.ACTIVITY_URL_RID_PILOT_ID)
            val dialog = PilotIdDialog()
            dialog.show(childFragmentManager, "")
        }
        uiBinding.layoutTarget.setOnClickListener {
            // 当前飞行目的
//            RouteManager.routeTo(requireContext(), RouterConst.PathConst.ACTIVITY_URL_RID_PURPOSE)
            val dialog = PurposeDialog()
            dialog.show(childFragmentManager, "")
        }
        uiBinding.ivClose.setOnClickListener { activity?.finish() }

        viewModel.operatorId.launchAndCollectIn(viewLifecycleOwner) {
            uiBinding.pilotId.text = it.ifEmpty { getString(R.string.common_text_unfilled) }
        }

        viewModel.purpose.launchAndCollectIn(viewLifecycleOwner) {
            uiBinding.purpose.text = it.ifEmpty { getString(R.string.common_text_input_pilot_id) }
        }

        viewModel.droneSn.launchAndCollectIn(viewLifecycleOwner) {
            uiBinding.droneSn.text = it
        }
    }

    override fun onResume() {
        super.onResume()
        val purpose =
            AutelStorageManager.getPlainStorage().getStringValue(StorageKey.PlainKey.KEY_RID_PURPOSE) ?: getString(R.string.common_text_unfilled)
        uiBinding.purpose.text = purpose
        val pilotId = AutelStorageManager.getPlainStorage().getStringValue(StorageKey.PlainKey.KEY_RID_PILOT_ID)
            ?: getString(R.string.common_text_input_pilot_id)
        uiBinding.pilotId.text = pilotId
        viewModel.querySystemDevicesInfo()
    }

    override fun getData() {
    }

    override fun addListen() {
    }
}