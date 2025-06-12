package com.autel.setting.widget

import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.autel.common.utils.PackageUtil
import com.autel.common.widget.toast.AutelToast
import com.autel.setting.R
import com.autel.setting.databinding.SettingChooseMapDialogBinding
import com.autel.setting.enums.AutelMapType
import java.util.*

class ChooseMapDialog : DialogFragment() {
    var latitude: Double = 0.0
    var longitude: Double = 0.0

    companion object {
        private const val key_latitude = "key_latitude"
        private const val key_longitude = "key_longitude"

        fun newInstance(latitude: Double, longitude: Double): ChooseMapDialog {
            val dialog = ChooseMapDialog()
            val args = Bundle()
            args.putDouble(key_latitude, latitude)
            args.putDouble(key_longitude, longitude)
            dialog.arguments = args
            return dialog
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        latitude = arguments?.getDouble(key_latitude) ?: 0.0
        longitude = arguments?.getDouble(key_longitude) ?: 0.0
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {

        var binding = SettingChooseMapDialogBinding.inflate(inflater, container, false)

        val mapType: AutelMapType by lazy {
            val language = if (Build.VERSION.SDK_INT < 24) {
                Resources.getSystem().configuration.locale
            } else {
                Resources.getSystem().configuration.locales[0]
            }
            if (language.language == Locale.CHINA.language) {
                AutelMapType.GAODE
            } else {
                AutelMapType.MAPBOX
            }
        }
        if (mapType == AutelMapType.MAPBOX) {
            binding.chooseBaidu.isGone = true
            binding.chooseGaode.isGone = true
            binding.chooseGoogle.isVisible = true
        } else {
            binding.chooseBaidu.isVisible = true
            binding.chooseGaode.isVisible = true
            binding.chooseGoogle.isGone = true
        }

        binding.chooseGoogle.setOnClickListener {
            context?.let {
                if (PackageUtil.checkMapAppsIsExist(it, PackageUtil.GOOGLE_MAP_PKG_NAME)) {
                    PackageUtil.goGoogleMap(it, latitude, longitude)
                } else {
                    AutelToast.normalToast(it, R.string.common_text_google_map_not_install)
                }
            }
            dismiss()
        }

        binding.chooseGaode.setOnClickListener {
            context?.let {
                if (PackageUtil.checkMapAppsIsExist(it, PackageUtil.GAODE_MAP_PKG_NAME)) {
                    PackageUtil.goGaodeMap(it, latitude, longitude)
                } else {
                    AutelToast.normalToast(it, R.string.common_text_gaode_map_not_install)
                }
            }
            dismiss()
        }

        binding.chooseBaidu.setOnClickListener {
            context?.let {
                if (PackageUtil.checkMapAppsIsExist(it, PackageUtil.BAIDU_MAP_PKG_NAME)) {
                    PackageUtil.openBaiduMap(it, latitude, longitude)
                } else {
                    AutelToast.normalToast(it, R.string.common_text_baidu_map_not_install)

                }
            }
            dismiss()
        }

        return binding.root
    }


}