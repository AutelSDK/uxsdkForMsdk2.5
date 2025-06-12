package com.external.uxdemo.delegate

import android.view.View
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import com.autel.common.delegate.IMainLayoutManager
import com.autel.common.delegate.layout.DelegateLayoutType
import com.autel.common.delegate.layout.IMainPanelListener
import com.autel.common.delegate.layout.MainPanelView
import com.google.android.flexbox.FlexboxLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Created by  2023/5/11
 *  主页LayoutManager,用于动态向主页中添加UI
 */
class MainLayoutManager(
    private val mainLayout: ConstraintLayout,
    private val panelLayout: FrameLayout,
    private val functionContainer: FlexboxLayout,
) : IMainLayoutManager {

    private val mainLayoutMap = mutableMapOf<DelegateLayoutType, View>()
    private val panelLayoutMap = ConcurrentHashMap<DelegateLayoutType, MainPanelView>()

    private val panelRemoveListenerMap = mutableMapOf<DelegateLayoutType, MutableList<IMainPanelListener>>()

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var showViewJob: Job? = null
    override fun addViewToMainLayout(type: DelegateLayoutType, view: View, layoutParams: ConstraintLayout.LayoutParams?) {
        val cacheView = mainLayoutMap.get(type)
        if (cacheView != null) {
            mainLayout.removeView(cacheView)
        }
        mainLayoutMap.put(type, view)
        if (layoutParams != null) {
            mainLayout.addView(view, layoutParams)
        } else {
            mainLayout.addView(view)
        }
    }

    override fun addFunctionToMainLayout(
        type: DelegateLayoutType,
        view: View,
        layoutParams: FlexboxLayout.LayoutParams?,
    ) {
        val cacheView = mainLayoutMap.get(type)
        if (cacheView != null) {
            functionContainer.removeView(cacheView)
        }
        mainLayoutMap.put(type, view)
        if (layoutParams != null) {
            functionContainer.addView(view, layoutParams)
        } else {
            functionContainer.addView(view)
        }
    }

    override fun removeFunctionFromMainLayout(type: DelegateLayoutType) {
        (mainLayoutMap[type])?.let {
            functionContainer.removeView(it)
        }
        mainLayoutMap.remove(type)
    }

    override fun removeViewFromMainLayout(type: DelegateLayoutType) {
        (mainLayoutMap[type])?.let {
            mainLayout.removeView(it)
        }
        mainLayoutMap.remove(type)
    }

    override fun getMainView(): View {
        return mainLayout
    }

    override fun addPanelListener(watchType: DelegateLayoutType, removePanelList: IMainPanelListener) {
        val list = panelRemoveListenerMap.getOrPut(watchType) { mutableListOf<IMainPanelListener>() }
        list.add(removePanelList)
    }

    override fun removePanelRemoveListener(watchType: DelegateLayoutType, removePanelList: IMainPanelListener) {
        panelRemoveListenerMap.get(watchType)?.remove(removePanelList)
    }

    override fun showViewToPanel(type: DelegateLayoutType, view: MainPanelView, layoutParams: FrameLayout.LayoutParams?) {
        // 清空侧栏中的其他元素
        val iterator = panelLayoutMap.iterator()
        if (showViewJob?.isActive == true) {
            return
        }
        showViewJob = coroutineScope.launch {
            var allResult = true
            while (iterator.hasNext()) {
                val entry = iterator.next()
                val cacheView = entry.value

                // 等待 canReplace 结果
                val canReplaceResult = withContext(Dispatchers.Main) {
                    cacheView.canReplace().await()
                }

                if (canReplaceResult) {
                    cacheView.removeFromMainPanel()
                    panelLayout.removeView(cacheView)
                    panelRemoveListenerMap[entry.key]?.forEach {
                        it.panelRemove()
                    }
                    iterator.remove()
                } else {
                    allResult = false
                    break
                }
            }

            if (allResult) {
                // 添加新视图到 panelLayout
                panelLayoutMap.put(type, view)
                if (layoutParams != null) {
                    panelLayout.addView(view, layoutParams)
                    view.addToMainPanel()
                } else {
                    panelLayout.addView(view)
                    view.addToMainPanel()
                }
                panelRemoveListenerMap[type]?.forEach {
                    it.panelShow()
                }
            }
        }
    }

    override fun removeViewFromPanel(type: DelegateLayoutType) {
        val view = panelLayoutMap[type] ?: return
        view.let {
            panelLayout.removeView(it)
            it.removeFromMainPanel()
        }
        panelLayoutMap.remove(type)
        panelRemoveListenerMap[type]?.forEach {
            it.panelRemove()
        }
    }

}