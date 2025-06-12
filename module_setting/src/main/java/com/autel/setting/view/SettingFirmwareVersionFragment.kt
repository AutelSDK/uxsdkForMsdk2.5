package com.autel.setting.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.autel.common.base.BaseAircraftFragment
import com.autel.common.model.serve.ResourceObserver
import com.autel.common.widget.CommonItemText
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.common.bean.DroneVersionItemBean
import com.autel.setting.R
import com.autel.setting.business.SettingAboutVM
import com.autel.setting.databinding.SettingFirmwareFragmentBinding

/**
 * @Author create by LJ
 * @Date 2022/9/1 15:16
 */
class SettingFirmwareVersionFragment : BaseAircraftFragment() {

    private lateinit var binding: SettingFirmwareFragmentBinding
    private val settingAboutVm: SettingAboutVM by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = SettingFirmwareFragmentBinding.inflate(LayoutInflater.from(context))
        return binding.root
    }

    override fun getData() {

    }

    override fun onResume() {
        super.onResume()
        settingAboutVm.querySystemDevicesInfo()
    }

    /**
     * 构造子项
     */
    private fun createChild(bean: DroneVersionItemBean): CommonItemText? {
        context?.let { it ->
            val view = CommonItemText(it)
            var title = bean.componentID.englishName
            //产品名显示
            bean.strComponentModel?.let { model ->
                if (model.contains(".")) {
                    val strs = model.split(".")
                    if (strs.size > 1) {
                        title = "$title ${strs[1]}"
                    }
                }
            }

            view.setTitle(title)
            view.updateRightText(bean.softwareVersion ?: getString(R.string.common_text_no_value))
            view.setBottomLineVisible(true)
            return view
        }
        return null
    }

    override fun addListen() {
        settingAboutVm.systemProfileLD.observe(viewLifecycleOwner, ResourceObserver<List<DroneVersionItemBean>?>().apply {
            success {
                binding.llParent.removeAllViews()
                data?.forEach {
                    createChild(it)?.let { childView -> binding.llParent.addView(childView) }
                }
            }
        })
    }

}