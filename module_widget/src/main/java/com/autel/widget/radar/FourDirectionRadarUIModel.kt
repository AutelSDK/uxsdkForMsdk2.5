package com.autel.widget.radar

import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout

/**
 * 雷达图UI模型
 */
class FourDirectionRadarUIModel(
     val radarGroup: ConstraintLayout,
     val one: ImageView,
     val two: ImageView,
     val three: ImageView,
     val four: ImageView?,
     val tvDistance: TextView,
)