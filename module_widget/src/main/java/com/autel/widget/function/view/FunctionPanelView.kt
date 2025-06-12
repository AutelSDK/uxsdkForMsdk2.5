package com.autel.widget.function.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.autel.common.delegate.function.CAMERA_TOOL
import com.autel.common.delegate.function.FunctionViewType
import com.autel.common.delegate.function.MESSAGE_SHARE
import com.autel.common.delegate.function.MISSION_TOOL
import com.autel.common.delegate.function.SETTING
import com.autel.common.delegate.function.SHORTCUT_TOOL
import com.autel.common.delegate.layout.MainPanelView
import com.autel.widget.R
import com.autel.widget.databinding.WidgetLayoutClassificationFunctionItemBinding
import com.autel.widget.databinding.WidgetLayoutFunctionPanelBinding
import com.autel.widget.function.binder.FunctionItemViewBinder
import com.autel.widget.function.inter.IFunctionManager
import com.autel.widget.function.inter.IFunctionOperate
import com.autel.widget.function.inter.IFunctionPanelHandleListener
import com.autel.widget.function.model.ClassificationFunctionItem
import com.autel.widget.function.model.SwitchFunctionModel
import com.drakeet.multitype.MultiTypeAdapter


class FunctionPanelView @JvmOverloads constructor(context: Context, attr: AttributeSet? = null) : MainPanelView(context, attr),
    DefaultLifecycleObserver, IFunctionOperate {

    private val binding: WidgetLayoutFunctionPanelBinding = WidgetLayoutFunctionPanelBinding.inflate(LayoutInflater.from(context), this, true)
    private var panelHandleListener: IFunctionPanelHandleListener? = null
    private var funItems = mutableListOf<ClassificationFunctionItem>()

    private val adapterMap = mutableMapOf<Int, MultiTypeAdapter>()
    private var manager: IFunctionManager? = null

    init {
        binding.root.setOnClickListener {

        }
        binding.tvResetGroup.setOnClickListener {
            //重置
            manager?.resetFunction()
        }
        binding.ivFunctionEdit.setOnClickListener {
            //进入编辑
            clickEnterEditMode()
        }
        binding.tvFunctionSave.setOnClickListener {
            //序列化保存设置
            exitEditMode()
            panelHandleListener?.exitEditModel()
        }
        binding.viewEmpty.setOnClickListener {
            panelHandleListener?.hiddenPanel()
        }
    }

    fun setPanelHandlerListener(panelListener: IFunctionPanelHandleListener) {
        this.panelHandleListener = panelListener
    }


    /**
     * 分类
     */
    private fun classifyByType(lists: List<SwitchFunctionModel>): List<ClassificationFunctionItem> {
        val map = hashMapOf<Int, ClassificationFunctionItem>()
        lists.forEach {
            if (map[it.functionModel.functionType.group] == null) {
                map[it.functionModel.functionType.group] = ClassificationFunctionItem(it.functionModel.functionType.group, mutableListOf())
            }
            map[it.functionModel.functionType.group]?.functionItems?.add(it)
        }
        funItems.clear()
        val l = map.map { it.value }
        l.forEach {
            funItems += it
        }
        return funItems
    }

    fun attachFunctionManager(manager: IFunctionManager) {
        this.manager = manager
        manager.bindPanelInBarStyleOperate(this)
        manager.bindPanelInFloatStyleOperate(this)
        val targetList = manager.getFunctionPanelList()
        classifyByType(targetList)
        reloadFunView()
    }

    /**
     * 重置分类列表
     */
    private fun reloadFunView() {
        binding.llGroupContent.removeAllViews()
        adapterMap.clear()
        // 分类列表
        funItems.forEach {
            val binder = WidgetLayoutClassificationFunctionItemBinding.inflate(LayoutInflater.from(context), null, false)
            val layout = LinearLayout.LayoutParams(-1, -2)
            setFunTypeName(binder.classificationName, it.type)
            val adapter = MultiTypeAdapter().apply {
                register(SwitchFunctionModel::class.java, FunctionItemViewBinder(FunctionViewType.Panel))
            }
            binder.funs.setHasFixedSize(true)
            binder.funs.layoutManager = object : GridLayoutManager(context, 5) {
                override fun canScrollVertically(): Boolean {
                    return false
                }
            }
            adapter.items = it.functionItems
            binder.funs.adapter = adapter
            val change = FunctionItemTouchHelper(adapter, false)
            change.bindView(binder.funs, manager)
            (binder.funs.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
            adapterMap[it.type] = adapter
            binding.llGroupContent.addView(binder.root, layout)
        }
    }


    private fun setFunTypeName(tv: TextView, type: Int) {
        tv.setText(
            when (type) {
                SHORTCUT_TOOL -> {
                    R.string.common_text_shortcut_tool
                }

                MISSION_TOOL -> {
                    R.string.common_text_mission_tool
                }

                CAMERA_TOOL -> {
                    R.string.common_text_camera_image
                }

                MESSAGE_SHARE -> {
                    R.string.common_text_message_share
                }

                SETTING -> {
                    R.string.common_text_personal_and_setting
                }

                else -> {
                    R.string.common_text_no_value
                }
            }
        )
    }

    private fun enterEditMode() {
        changeDataEditState(true)
    }

    fun clickEnterEditMode() {
        enterEditMode()
        panelHandleListener?.enterEditModel()
        binding.rlFunctionEdit.visibility = View.GONE
        binding.rlFunctionSave.visibility = View.VISIBLE
    }

    fun exitEditMode() {
        changeDataEditState(false)
        binding.rlFunctionEdit.visibility = View.VISIBLE
        binding.rlFunctionSave.visibility = View.GONE
    }

    private fun changeDataEditState(isEdit: Boolean) {
        adapterMap.values.forEach {
            it.items.forEach { item ->
                if (item is SwitchFunctionModel) {
                    item.isEdit = isEdit
                } else if (item is ClassificationFunctionItem) {
                    item.functionItems.forEach { base ->
                        base.isEdit = isEdit
                    }
                }
            }
            it.notifyDataSetChanged()
        }
    }


    override fun refreshFunction(model: SwitchFunctionModel) {
        adapterMap.values.forEach {
            val position = it.items.indexOf(model)
            if (position != -1) {
                it.notifyItemChanged(position)
            }
        }
    }

    override fun refreshAllFunction() {
        adapterMap.values.forEach {
            it.notifyDataSetChanged()
        }
    }

    override fun notifyItemInserted(position: Int) {
        val targetList = manager?.getFunctionPanelList() ?: return
        classifyByType(targetList)
        reloadFunView()
    }

    override fun notifyItemRemoved(position: Int) {
        val targetList = manager?.getFunctionPanelList() ?: return
        classifyByType(targetList)
        reloadFunView()
    }

    override fun notifyItemChanged(position: Int) {
        val targetList = manager?.getFunctionPanelList() ?: return
        classifyByType(targetList)
        reloadFunView()
    }

    override fun resetFunction() {
        val targetList = manager?.getFunctionPanelList() ?: return
        classifyByType(targetList)
        reloadFunView()
    }

    override fun isShowMoreFunction(isShow: Boolean) {
        //
    }
}