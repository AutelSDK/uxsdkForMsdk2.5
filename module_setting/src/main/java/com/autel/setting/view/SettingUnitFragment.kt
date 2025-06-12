package com.autel.setting.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.autel.common.base.BaseFragment
import com.autel.common.lifecycle.LiveDataBus
import com.autel.common.lifecycle.event.ChangeUnitSuccessEvent
import com.autel.common.manager.unit.UnitManager
import com.autel.setting.R
import com.autel.setting.databinding.SettingUnitFragmentBinding

/**
 * @Author create by LJ
 * @Date 200/09/13 11:19
 * 单位设置页面
 */
class SettingUnitFragment : BaseFragment() {
    private lateinit var bindIng: SettingUnitFragmentBinding
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        bindIng = SettingUnitFragmentBinding.inflate(LayoutInflater.from(context))
        return bindIng.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(bindIng) {
            val speedUnitList = UnitManager.getSpeedUnitList()
            csisvSpeedUnit.updateSpinnerData(speedUnitList.map { requireContext().getString(it.stringRes) })
            csisvSpeedUnit.updateSpinnerTitleIndex(speedUnitList.indexOf(UnitManager.getSelectSpeedUnit()))
            csisvSpeedUnit.updateBottomLineVisible(true)
            csisvSpeedUnit.setSpinnerSelectedListener {
                UnitManager.saveSelectSpeedUnit(speedUnitList[it])
                LiveDataBus.of(ChangeUnitSuccessEvent::class.java).changeUnit().post(speedUnitList[it])
            }

            val areaUnitList = UnitManager.getAreaUnitList()
            csisvAreaUnit.updateSpinnerData(areaUnitList.map { requireContext().getString(it.stringRes) })
            csisvAreaUnit.updateSpinnerTitleIndex(areaUnitList.indexOf(UnitManager.getSelectAreaUnit()))
            csisvAreaUnit.updatePaddingTop(requireContext().resources.getDimensionPixelSize(R.dimen.common_10dp))
            csisvAreaUnit.updateBottomLineVisible(true)
            csisvAreaUnit.setSpinnerSelectedListener {
                UnitManager.saveSelectAreaUnit(areaUnitList[it])
                LiveDataBus.of(ChangeUnitSuccessEvent::class.java).changeUnit().post(areaUnitList[it])
            }

            val tempUnitList = UnitManager.getTempUnitList()
            csisvTempratureUnit.updateSpinnerData(tempUnitList.map { requireContext().getString(it.stringRes) })
            csisvTempratureUnit.updateSpinnerTitleIndex(tempUnitList.indexOf(UnitManager.getSelectTempUnit()))
            csisvTempratureUnit.updatePaddingTop(requireContext().resources.getDimensionPixelSize(R.dimen.common_10dp))
            csisvTempratureUnit.updateBottomLineVisible(true)
            csisvTempratureUnit.setSpinnerSelectedListener {
                UnitManager.saveSelectTempUnit(tempUnitList[it])
                LiveDataBus.of(ChangeUnitSuccessEvent::class.java).changeUnit().post(tempUnitList[it])
            }

            val coordinateUnitList = UnitManager.getCoordinateUnitList()
            csisvCoordinatesType.updateSpinnerData(coordinateUnitList.map { requireContext().getString(it.stringRes) })
            csisvCoordinatesType.updateSpinnerTitleIndex(coordinateUnitList.indexOf(UnitManager.getSelectCoordinateUnit()))
            csisvCoordinatesType.updatePaddingTop(requireContext().resources.getDimensionPixelSize(R.dimen.common_10dp))
            csisvCoordinatesType.setSpinnerSelectedListener {
                UnitManager.saveSelectCoordinateUnit(coordinateUnitList[it])
                LiveDataBus.of(ChangeUnitSuccessEvent::class.java).changeUnit().post(coordinateUnitList[it])
            }
        }

    }
}