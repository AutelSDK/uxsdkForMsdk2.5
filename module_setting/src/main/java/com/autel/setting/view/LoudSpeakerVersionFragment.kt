package com.autel.setting.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.autel.common.base.BaseAircraftFragment
import com.autel.setting.R
import com.autel.setting.databinding.SettingLoudspeakerVersionFragmentBinding

class LoudSpeakerVersionFragment : BaseAircraftFragment() {

    private lateinit var binding: SettingLoudspeakerVersionFragmentBinding
    
    companion object {
        private const val KEY_LIGHT_VERSION = "light_version"
        private const val KEY_SPEAKER_VERSION = "speaker_version"
        
        fun newInstance(payloadVersion: String?, speakerVersion: String?): LoudSpeakerVersionFragment {
            return LoudSpeakerVersionFragment().apply {
                arguments = Bundle().apply {
                    putString(KEY_LIGHT_VERSION, payloadVersion)
                    putString(KEY_SPEAKER_VERSION, speakerVersion)
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = SettingLoudspeakerVersionFragmentBinding.inflate(LayoutInflater.from(context))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getData()
    }

    override fun getData() {
        val payloadVersion = arguments?.getString(KEY_LIGHT_VERSION)
        val speakerVersion = arguments?.getString(KEY_SPEAKER_VERSION)
        
        binding.citLightVersion.updateRightText(payloadVersion ?: getString(R.string.common_text_no_value))
        binding.citSpeakerVersion.updateRightText(speakerVersion ?: getString(R.string.common_text_no_value))
    }
} 