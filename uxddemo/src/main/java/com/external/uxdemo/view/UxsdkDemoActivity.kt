package com.external.uxdemo.view

import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.view.marginBottom
import androidx.core.view.marginEnd
import androidx.core.view.marginStart
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Observer
import com.autel.codec.debug.TestSkyLinkFragment
import com.autel.codec.splitscreen.business.ScreenStateManager
import com.autel.common.delegate.function.FunctionBarState
import com.autel.common.delegate.function.FunctionViewType
import com.autel.common.lifecycle.LiveDataBus
import com.autel.common.lifecycle.event.FunctionViewStyleEvent
import com.autel.common.lifecycle.event.SplitScreenEffectEvent
import com.autel.common.manager.AutelStorageManager
import com.autel.common.manager.MiddlewareManager
import com.autel.common.manager.StorageKey
import com.autel.common.model.splitscreen.AircraftScreenItem
import com.autel.common.utils.AnimateUtil
import com.autel.common.utils.DeviceUtils
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.manager.SpeedModeManager
import com.autel.drone.sdk.vmodelx.manager.data.ControlMode
import com.autel.drone.sdk.vmodelx.utils.BroadcastUtils
import com.autel.log.AutelLog
import com.autel.player.player.AutelPlayerManager
import com.autel.player.player.autelplayer.AutelPlayer
import com.autel.widget.widget.map.MapWidget
import com.external.uxddemo.R
import android.os.Build
import android.os.Environment
import android.content.Intent
import android.provider.Settings
import android.net.Uri
import com.autel.map.MapManager


class UxsdkDemoActivity : BaseMainActivity() {

    private var skyLinkFragment: TestSkyLinkFragment? = null
    var mapWidget : MapWidget? = null

    init {
        AutelLog.i("LaunchCheck","MainActivity init")
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase)
        AutelLog.i("LaunchCheck", "MainActivity attachBaseContext")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val start = SystemClock.elapsedRealtime()
        AutelLog.i("LaunchCheck", "main onCreate start = $start")
        super.onCreate(savedInstanceState)
        BroadcastUtils.sendKillBroadcast(this)
        checkAndRequestAllFilesPermission()
        initView()
        AutelLog.i("LaunchCheck", "main onCreate end = ${SystemClock.elapsedRealtime() - start}")
    }

    private fun initView() {
        mapWidget = MapWidget(this)
        uiBinding.autelSplitScreenContainer.getMapContainer().addView(mapWidget, ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT)

        val mapToken = MapManager.getMapToken()
        if (mapToken.isNullOrEmpty()) {
            showMapKeyDialog()
        }

        uiBinding.codecTabView.setCodecTabSwitchListener {
            ScreenStateManager.getInstance().updateCodecTabSwitch(ScreenStateManager.getInstance().getPageLocationState().fullScreenType, it)
        }

        uiBinding.statusBar.visibility = if (DeviceUtils.isMainRC()) View.VISIBLE else View.GONE

        uiBinding.rsStatus.visibility = if (DeviceUtils.isMainRC()) View.GONE else View.VISIBLE

        uiBinding.statusBar.multipleClicks.observe(this) {
            if (skyLinkFragment == null) {
                addTestSkyLinkFragment()
            } else {
                removeTestSkyLinkFragment()
            }
        }
        uiBinding.ivBarCollapse.setOnClickListener {
            if (functionBarVm.functionBarLD.value == FunctionBarState.Unfolded) {
                functionBarVm.updateFunctionBarLD(FunctionBarState.Folding)
            } else if (functionBarVm.functionBarLD.value == FunctionBarState.Folded) {
                functionBarVm.updateFunctionBarLD(FunctionBarState.Unfolding)
            }
        }

        uiBinding.functionSuspensionView.setOnClickListener {
            val locationArray = IntArray(2)
            it.getLocationOnScreen(locationArray)
            uiBinding.functionFloatWindowView.show(locationArray)
            uiBinding.functionSuspensionView.visibility = View.GONE
            uiBinding.functionFloatWindowView.setClickDismiss(true)
            getMainHandler().hiddenFunctionPanel()
        }

        uiBinding.functionFloatWindowView.setDismissListener {
            uiBinding.functionSuspensionView.visibility = View.VISIBLE
        }

        uiBinding.functionFloatWindowView.setClickDismiss(true)

        refreshFunctionViewType(
            FunctionViewType.find(
                AutelStorageManager.getPlainStorage().getIntValue(StorageKey.PlainKey.KEY_FUNCTION_QUICK_ACTION)
            )
        )
        uiBinding.attitudeBall.isVisible = DeviceUtils.isSingleControl()

        uiBinding.codecToolRight.setMainProvider(this)
    }

    private fun addTestSkyLinkFragment() {
        val fragment = TestSkyLinkFragment()
        fragment.mAutelPlayer =
            AutelPlayer(16010)
        skyLinkFragment = fragment
        supportFragmentManager.beginTransaction().replace(R.id.fl_test_sky_link, fragment).commit()
        AutelPlayerManager.getInstance().openVideoInfoShow(true)
    }

    private fun removeTestSkyLinkFragment() {
        skyLinkFragment?.let {
            supportFragmentManager.beginTransaction().remove(it).commit()
            skyLinkFragment = null
        }
    }

    override fun initObserver() {
        super.initObserver()

        @Suppress("ObjectLiteralToLambda")
        LiveDataBus.of(SplitScreenEffectEvent::class.java).isUniformScale().observe(
            this,
            object : Observer<Boolean> {
                override fun onChanged(t: Boolean) {
                    t?.let { ScreenStateManager.getInstance().updateUniform(t) }
                }
            })

        LiveDataBus.of(SplitScreenEffectEvent::class.java).updateFullScreenStyle().observe(
            this,
            object : Observer<Int> {
                override fun onChanged(t: Int) {
                    t?.let { ScreenStateManager.getInstance().updateFullScreenStyle(t) }
                }
            })

        LiveDataBus.of(FunctionViewStyleEvent::class.java).switchViewStyle().observe(
            this,
            object : Observer<FunctionViewType> {
                override fun onChanged(t: FunctionViewType) {
                    refreshFunctionViewType(t)
                }
            })

        ScreenStateManager.getInstance().observerScreenState(this, Observer {
            refreshCodecToolLeft()
            refreshCodecToolRight()
            refreshCodecTab()
            refreshAttitudeBall()
        })

        ScreenStateManager.getInstance().observerFullScreen(this, Observer { full ->

            val barHeight = uiBinding.statusBar.height * 1.0f
            val barTranslateY = if (full) {
                -barHeight
            } else {
                0f
            }
            AnimateUtil.animateProperty(uiBinding.statusBar, "translationY", barTranslateY)

            val functionHeight = uiBinding.functionView.height
            val functionTranslateY = if (full) {
                -(barHeight + functionHeight)
            } else {
                0f
            }
            AnimateUtil.animateProperty(uiBinding.functionView, "translationY", functionTranslateY)

            val barCollapseHeight = uiBinding.ivBarCollapse.height
            val barCollapseTranslateY = if (full) {
                -(barHeight + functionHeight + barCollapseHeight)
            } else {
                0f
            }
            AnimateUtil.animateProperty(uiBinding.ivBarCollapse, "translationY", barCollapseTranslateY)

            val rcStatusTranslateY = if (full) {
                -(uiBinding.rsStatus.height + barHeight)
            } else {
                0f
            }
            AnimateUtil.animateProperty(uiBinding.rsStatus, "translationY", rcStatusTranslateY)

            val acvStanceBallTranslateY = if (full) {
                uiBinding.attitudeBall.height + uiBinding.attitudeBall.marginBottom + 0.0f
            } else {
                0f
            }
            AnimateUtil.animateProperty(uiBinding.attitudeBall, "translationY", acvStanceBallTranslateY)
            val codecTabSwitchTranslateX = if (full) {
                -(uiBinding.codecTabView.width + uiBinding.codecTabView.marginStart) * 1.0f
            } else {
                0f
            }
            AnimateUtil.animateProperty(uiBinding.codecTabView, "translationX", codecTabSwitchTranslateX)

            val codecLeftTranslateX = if (full) {
                -(uiBinding.flGimbal.width + uiBinding.flGimbal.marginStart) * 1.0f
            } else {
                0f
            }
            AnimateUtil.animateProperty(uiBinding.flGimbal, "translationX", codecLeftTranslateX)

            val codecRightTranslateX = if (full) {
                (uiBinding.codecToolRight.width + uiBinding.codecToolRight.marginEnd) * 1.0f
            } else {
                0f
            }
            AnimateUtil.animateProperty(uiBinding.codecToolRight, "translationX", codecRightTranslateX)

            val screenShortcutTranslateX = if (full) {
                (uiBinding.screenShortcutView.width + uiBinding.screenShortcutView.marginEnd) * 1.0f
            } else {
                0f
            }
            AnimateUtil.animateProperty(uiBinding.screenShortcutView, "translationX", screenShortcutTranslateX)

            AnimateUtil.animateAlphaProperty(uiBinding.functionSuspensionView, full)
        })

    }

    private fun refreshFunctionViewType(functionType: FunctionViewType) {
        if (functionType == FunctionViewType.Bar) { //工具栏样式
            functionBarVm.updateFunctionBarLD(FunctionBarState.Unfolded)
            uiBinding.ivBarCollapse.isVisible = true
            uiBinding.functionSuspensionView.isVisible = false
            uiBinding.codecTabView.updateLayoutParams<ConstraintLayout.LayoutParams> {
                topMargin = resources.getDimensionPixelOffset(R.dimen.common_15dp)
            }
        } else { //悬浮球样式
            functionBarVm.updateFunctionBarLD(FunctionBarState.Folded)
            uiBinding.ivBarCollapse.isVisible = false
            uiBinding.functionSuspensionView.isVisible = true
            uiBinding.codecTabView.updateLayoutParams<ConstraintLayout.LayoutParams> {
                topMargin = resources.getDimensionPixelOffset(R.dimen.common_102dp)
            }
        }
    }

    private fun refreshAttitudeBall() {
        val state = ScreenStateManager.getInstance().getPageLocationState()
        uiBinding.attitudeBall.isVisible = DeviceUtils.isSingleControl()
        if (state.deviceListShow) {
            uiBinding.attitudeBall.updateLayoutParams<ConstraintLayout.LayoutParams> {
                bottomMargin = resources.getDimensionPixelOffset(R.dimen.common_variety_device_list_height)
            }
        } else {
            uiBinding.attitudeBall.updateLayoutParams<ConstraintLayout.LayoutParams> {
                bottomMargin = 0
            }
        }
    }

    private fun refreshCodecToolLeft() {
        val state = ScreenStateManager.getInstance().getPageLocationState()
        if (!state.isMapWidgetFullScreen() &&
            DeviceUtils.isSingleControl() &&
            DeviceUtils.isMainRC()
        ) {
            uiBinding.flGimbal.visibility = View.VISIBLE
        } else {
            uiBinding.flGimbal.visibility = View.INVISIBLE
        }
    }

    private fun refreshCodecToolRight() {
        val state = ScreenStateManager.getInstance().getPageLocationState()
        if (!state.isMapWidgetFullScreen() && DeviceUtils.isMainRC()) {
            uiBinding.codecToolRight.updateLensInfo(state.getAllDroneSet().firstOrNull(), state.isShowLinkZoom())
            uiBinding.codecToolRight.visibility = View.VISIBLE
        } else {
            uiBinding.codecToolRight.visibility = View.GONE
        }
    }

    private fun refreshCodecTab() {
        val state = ScreenStateManager.getInstance().getPageLocationState()
        if (state.isCodecWidgetFullScreen()) {
            uiBinding.codecTabView.visibility = View.VISIBLE
            uiBinding.codecTabView.updateDronePageWidgetList(
                ScreenStateManager.getInstance().getCodecScreenItems(this, state.fullScreenType)
            )
        } else {
            uiBinding.codecTabView.visibility = View.INVISIBLE
            uiBinding.codecTabView.updateDronePageWidgetList(
                ScreenStateManager.getInstance().getCodecScreenItems(this, state.getFirstCodecWidget() ?: AircraftScreenItem.Empty)
            )
        }
    }

    override fun onDroneChangedListener(connected: Boolean, drone: IAutelDroneDevice) {
        super.onDroneChangedListener(connected, drone)
        refreshCodecToolLeft()
    }

    override fun onControlChange(mode: ControlMode, droneList: List<IAutelDroneDevice>) {
        super.onControlChange(mode, droneList)
        refreshCodecToolLeft()
        refreshAttitudeBall()
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ScreenStateManager.getInstance().checkInAircraftScreenAnimTime()) {
            return false
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onResume() {
        super.onResume()
        //回到图传界面，切回正常速度模式
        DeviceUtils.singleControlDrone()?.let { device ->
            SpeedModeManager.changeToNormalSpeed(device, 3) {
                AutelLog.i("MainActivity", "MainActivity onResume change speed normal:$it")
            }
        }
    }

    override fun onPause() {
        super.onPause()
    }

    private fun checkAndRequestAllFilesPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:" + packageName)
                startActivity(intent)
            }
        }
    }

    private fun showMapKeyDialog() {
        val messageCN = "地图加载失败，请在 MapWidget 的 fun initMap() { val MAPTILER_KEY = \"\" } 处，替换为你自己申请的 MapTiler API Key，否则地图无法显示。"
        val messageEN = "Map loading failed. Please go to MapWidget's fun initMap() { val MAPTILER_KEY = \"\" } and replace it with your own MapTiler API Key, otherwise the map cannot be displayed."
        val isChinese = java.util.Locale.getDefault().language == "zh"
        val message = if (isChinese) messageCN else messageEN
        val title = if (isChinese) "提示" else "Notice"
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(if (isChinese) "确定" else "OK", null)
            .show()
    }

}