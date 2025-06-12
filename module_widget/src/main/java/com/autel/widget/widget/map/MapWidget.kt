package com.autel.widget.widget.map

import android.app.Activity
import android.content.Context
import android.graphics.BitmapFactory
import android.util.AttributeSet
import com.autel.common.base.widget.ConstraintLayoutWidget
import com.autel.common.manager.AutelMapTokenConstant
import com.autel.drone.sdk.log.SDKLog
import com.autel.map.MapManager
import com.autel.map.annotation.AutelPointAnnotation
import com.autel.map.annotation.AutelPolyLineAnnotation
import com.autel.map.bean.AutelLatLng
import com.autel.map.bean.AutelMapStyle
import com.autel.map.bean.LayerPriority
import com.autel.map.options.AutelPointAnnotationOptions
import com.autel.map.options.AutelPolylineAnnotationOptions
import com.autel.map.util.MapBoxUtils
import com.autel.widget.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 *@Author autel
 *@Date 2025/5/30
 *
 */
class MapWidget @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    ConstraintLayoutWidget(context, attrs, defStyleAttr) {
        companion object {
            private const val TAG = "MapWidget"
            private const val MAP_ICON_DRONE = "map_icon_drone"
            private const val MAP_ICON_HOME = "map_icon_home"
            private const val MAP_ICON_RC = "map_icon_rc"
            var MOVE_TO_DRONE = true
            private val DEFAULT_MAP_ZOOM = 16.0
            private val DEFAULT_MAP_DURATION = 200L
        }

    lateinit var mapManager: MapManager
    private val mapVM: MapWidgetVM by lazy {
        MapWidgetVM()
    }
    private val droneMarkers = mutableListOf<DroneMarker>()
    init {
        initMap()
    }

    fun initMap() {
        val MAPTILER_KEY = "" // Replace with your MapTiler API key
        MapManager.setMapToken(MAPTILER_KEY)

        mapManager = MapManager(context)
        mapManager.setDefaultMapSettings()
        addMapView()

        mapManager.loadStyle(context as Activity, AutelMapStyle.MIX, resources.configuration.locale) { isSuc, msg ->
            SDKLog.d(TAG, "loadStyle isSuc: $isSuc, msg: $msg")
            CoroutineScope(Dispatchers.Main).launch {
                if (isSuc) {
                    loadResources()
                    addDroneObserver()
                }
            }
        }
    }

    private fun addMapView() {
        addView(mapManager.getMapView()!!, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    private fun addDroneObserver() {
        mapVM.setup()
        mapVM.droneInfoChanged.subscribe {
            it?.let { droneInfoList ->
                updateDroneMarker(droneInfoList)
            }
        }
        mapVM.rcLocationChanged.subscribe { rcModel ->
            rcModel?.let { updateRcMarker(it) }
        }

    }

    private fun updateDroneMarker(list: List<DroneInfoModel>) {

        list.forEach { droneInfo ->
            val droneMarker = droneMarkers.find { it.id == droneInfo.id }
            if (droneMarker == null) {
                val options = AutelPointAnnotationOptions().apply {
                    withLatLng(droneInfo.longitude, droneInfo.latitude)
                    withIconImage(MAP_ICON_DRONE)
                    withIconRotate(droneInfo.heading.toDouble())
                    withTouchable(false)
                    withLayerPriority(LayerPriority.HIGH)
                    withSymbolSortKey(10.0)
                }
                val anno = mapManager.addPoint(options)
                val homeOptions = AutelPointAnnotationOptions().apply {
                    withLatLng(droneInfo.homeLongitude, droneInfo.homeLatitude)
                    withIconImage(MAP_ICON_HOME)
                    withTouchable(false)
                    withLayerPriority(LayerPriority.HIGH)
                    withSymbolSortKey(5.0)
                }
                val homeAnno = mapManager.addPoint(homeOptions)

                droneMarkers.add(DroneMarker().apply {
                    id = droneInfo.id
                    droneAnnotation = anno
                    homeAnnotation = homeAnno
                    points.add(AutelLatLng(droneInfo.latitude, droneInfo.longitude))
                })
            } else {
                droneMarker.droneAnnotation?.let { annotation ->
                    annotation.options.apply {
                        withLatLng(droneInfo.longitude, droneInfo.latitude)
                        withIconRotate(droneInfo.heading.toDouble())
                    }
                    mapManager.updatePoint(annotation)
                }
                droneMarker.homeAnnotation?.let { homeAnnotation ->
                    homeAnnotation.options.apply {
                        withLatLng(droneInfo.homeLongitude, droneInfo.homeLatitude)
                    }
                    mapManager.updatePoint(homeAnnotation)
                }
                val newLatlng = AutelLatLng(droneInfo.latitude, droneInfo.longitude)
                if (MapBoxUtils.getDistance(droneMarker.points.last(), newLatlng) > 1) {
                    droneMarker.points.add(newLatlng)
                    updateDroneLine(droneMarker)
                }

            }
        }

        moveToDrone()
    }

    private fun updateDroneLine(droneMarker: DroneMarker) {
        var line = droneMarker.lineAnnotation
        if (line == null) {
            val options = AutelPolylineAnnotationOptions().apply {
                withPoints(droneMarker.points)
                withLineColor(0xFFEEEEEE.toInt())
                withLineWidth(5.0)
                withLayerPriority(LayerPriority.HIGH)
            }
            line = mapManager.addPolyline(options)
            droneMarker.lineAnnotation = line
        } else {
            line.options.withPoints(droneMarker.points)
            mapManager.updatePolyline(line)
        }
    }
    private fun updateRcMarker(rcModel: DroneInfoModel) {
        val rcMarker = droneMarkers.firstOrNull { it.id == -1 } // RC ID is -1
        if (rcMarker == null) {
            val options = AutelPointAnnotationOptions().apply {
                withLatLng(rcModel.longitude, rcModel.latitude)
                withIconImage(MAP_ICON_RC)
                withIconRotate(rcModel.heading.toDouble())
                withTouchable(false)
                withLayerPriority(LayerPriority.HIGH)
            }
            val anno = mapManager.addPoint(options)
            droneMarkers.add(DroneMarker().apply {
                id = -1 // RC ID
                droneAnnotation = anno
            })
        } else {
            rcMarker.droneAnnotation?.let { annotation ->
                annotation.options.apply {
                    withLatLng(rcModel.longitude, rcModel.latitude)
                    withIconRotate(rcModel.heading.toDouble())
                }
                mapManager.updatePoint(annotation)
            }

        }
    }

    private fun moveToDrone() {
        if (!MOVE_TO_DRONE) return
        val droneMarker = droneMarkers.firstOrNull() ?: return
        mapManager.moveCameraTo(
            AutelLatLng(droneMarker.droneAnnotation?.options?.latLng?.latitude ?: 0.0,
                droneMarker.droneAnnotation?.options?.latLng?.longitude ?: 0.0),
            DEFAULT_MAP_ZOOM,
            DEFAULT_MAP_DURATION
        )
        MOVE_TO_DRONE = false
    }

    private fun loadResources() {
        mapManager.addImageIconToStyle(MAP_ICON_DRONE, BitmapFactory.decodeResource(resources, R.drawable.common_ic_ball_drone))
        mapManager.addImageIconToStyle(MAP_ICON_HOME, BitmapFactory.decodeResource(resources, R.drawable.common_ic_ball_home))
        mapManager.addImageIconToStyle(MAP_ICON_RC, BitmapFactory.decodeResource(resources, R.drawable.common_ic_stance_remote))
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

    }

    inner class DroneMarker {
        var id = 0
        var points = mutableListOf<AutelLatLng>()
        var droneAnnotation: AutelPointAnnotation? = null
        var homeAnnotation: AutelPointAnnotation? = null
        var lineAnnotation: AutelPolyLineAnnotation? = null
    }
}

