package com.autel.widget.radar

import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.vision.bean.VisionRadarInfoBean

data class RadarModel(
     val connected: Boolean,
     val isDroneFlying: Boolean,
     val radarInfoBeanList: List<VisionRadarInfoBean>,
     val isVisionValidate: Boolean
)
