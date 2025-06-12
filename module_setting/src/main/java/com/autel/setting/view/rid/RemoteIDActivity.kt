package com.autel.setting.view.rid

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.alibaba.android.arouter.facade.annotation.Route
import com.autel.common.base.BaseAircraftActivity
import com.autel.common.feature.location.CountryManager
import com.autel.common.feature.route.RouterConst
import com.autel.setting.R
import com.autel.setting.databinding.SettingActivityRemoteIdBinding

@Route(path = RouterConst.PathConst.ACTIVITY_URL_RID)
class RemoteIDActivity : BaseAircraftActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = SettingActivityRemoteIdBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportFragmentManager.beginTransaction().replace(R.id.layout, getRIDFragment()).commit()
    }

    private fun getRIDFragment(): Fragment {
        return if (CountryManager.isEUZone()) {
            EURidFragment()
        } else {
            USRidFragment()
        }
    }
}