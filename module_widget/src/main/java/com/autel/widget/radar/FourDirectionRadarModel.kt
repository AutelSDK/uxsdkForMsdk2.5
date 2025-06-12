package com.autel.widget.radar

data class FourDirectionRadarModel(
    var radarGroupShow: Boolean? = null,

    var oneShow: Boolean? = null,
    var oneImageResource: Int? = null,
    var twoShow: Boolean? = null,
    var twoImageResource: Int? = null,
    var threeShow: Boolean? = null,
    var threeImageResource: Int? = null,
    var fourShow: Boolean? = null,
    var fourImageResource: Int? = null,

    var tvDistance: String? = null,
    var tvDistanceShow: Boolean? = null,
    var tvDistanceColor: Int? = null
)