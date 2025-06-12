package com.autel.setting.view

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.autel.common.base.BaseAircraftActivity
import com.autel.common.feature.route.RouterConst
import com.autel.common.widget.dialog.CommonLoadingDialog
import com.autel.log.AutelLog
import com.autel.setting.R
import com.autel.setting.bean.SelectorFile
import com.autel.setting.databinding.ActivityFileSelectorBinding
import com.autel.setting.view.binder.FileSelectorBinder
import com.drakeet.multitype.MultiTypeAdapter
import java.io.File
import java.util.ArrayList
import java.util.Locale

/**
 * @author 
 * @date 2023/8/4
 * 导入AI模型
 */
@Route(path = RouterConst.ModuleService.ACTIVITY_URL_FILE_SELECTOR)
class FileSelectorActivity : BaseAircraftActivity() {
    private var TAG = "FileSelectorActivity"
    private lateinit var binding: ActivityFileSelectorBinding
    private var mAdapter: MultiTypeAdapter? = null
    private val curFileList = ArrayList<SelectorFile>()
    private var isEdit = false
    private var isChooseAll = false
    private var mUsbReceiver: BroadcastReceiver? = null
    private var curPath = ""//当前路径
    private var loadingDialog: CommonLoadingDialog? = null

    @JvmField
    @Autowired(name = KEY_FILE_TYPE)
    var fileSuffix: String? = null

    @JvmField
    @Autowired(name = KEY_FILE_TYPES)
    var fileSuffixes: ArrayList<String>? = null

    /**
     * 是否支持多选
     */
    @JvmField
    @Autowired(name = KEY_MULTIPLE_CHOICE)
    var isMultipleChoice: Boolean = true

    @JvmField
    @Autowired(name = KEY_TITLE_RES)
    var titleRes: Int = 0

    @JvmField
    @Autowired(name = KEY_BASE_PATH)
    var basePath :String = ""

    @JvmField
    @Autowired(name = KEY_NEW_IMPORT_UI)
    var useNewImportUI: Boolean = false

    companion object {
        const val KEY_FILE_TYPE = "key_file_type"

        const val KEY_MULTIPLE_CHOICE = "key_multiple_choice"

        const val KEY_TITLE_RES = "key_title_res"

        const val KEY_BASE_PATH = "key_base_path"

        const val KEY_FILE_TYPES = "key_file_types"

        const val KEY_NEW_IMPORT_UI = "key_new_import_ui"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFileSelectorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ARouter.getInstance().inject(this)
        fileSuffix = intent.getStringExtra(KEY_FILE_TYPE)
        fileSuffixes = intent.getStringArrayListExtra(KEY_FILE_TYPES)
        useNewImportUI = intent.getBooleanExtra(KEY_NEW_IMPORT_UI, false)
        initUsbReceiver()
        initView()
        initUCardData()
    }

    /**
     * 注册U盘监听
     */
    private fun initUsbReceiver() {
        mUsbReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val action = intent?.action
                if (TextUtils.isEmpty(action)) return
                if (action == UsbManager.ACTION_USB_DEVICE_DETACHED) {
                    AutelLog.i(TAG, "U card remove")
                    showToast(R.string.common_text_u_card_remove)
                    finish()
                }
            }
        }
        val usbDeviceStateFilter = IntentFilter()
        usbDeviceStateFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        registerReceiver(mUsbReceiver, usbDeviceStateFilter)
        AutelLog.i(TAG, "U card = ${getUsbPath()}")
        getUsbPath()?.let { curPath = it }
        binding.rlEmpty.isVisible = curPath.isNullOrEmpty()
        binding.rlItemContent.isGone = curPath.isNullOrEmpty()
    }

    /**
     * 获取U盘路径
     * @return
     */
    private fun getUsbPath(): String {
        return basePath
    }

    private fun initView() {
        binding.tvState.isVisible = isMultipleChoice
        loadingDialog = CommonLoadingDialog.Builder(this).setMessage("").builder()
        binding.ivBack.setOnClickListener {
            dealBack()
        }

        binding.tvChooseAll.setOnClickListener {
            isChooseAll = !isChooseAll
            refreshChooseAllView()
            curFileList.forEach {
                if (!it.file.isDirectory) {
                    it.isSelect = isChooseAll
                }
            }
            if (!useNewImportUI) {
                binding.importFile.isVisible = hasSelectFile()
            }else{
                binding.bottomLayout.isVisible = hasSelectFile()
            }
            refreshTitle()
            mAdapter?.notifyDataSetChanged()
        }

        binding.tvState.setOnClickListener {
            curFileList.forEach { it.isSelect = false }
            binding.importFile.isVisible = false
            isEdit = !isEdit
            if (!isEdit) {
                binding.importFile.isVisible = false
                isChooseAll = false
                refreshChooseAllView()
            }
            curFileList.forEach {
                if (!it.file.isDirectory) {
                    it.isMultipleSelectMode = isEdit
                }
            }
            refreshTitle()
            binding.ivBack.isVisible = !isEdit
            binding.tvChooseAll.isVisible = isEdit
            val id = if (isEdit) R.string.common_text_cancel else R.string.common_text_select
            binding.tvState.text = getString(id)
            mAdapter?.notifyDataSetChanged()

        }
        if (useNewImportUI) {
            binding.bottomLayout.visibility = View.VISIBLE
            binding.importFile.visibility = View.GONE
        } else {
            binding.bottomLayout.visibility = View.GONE
            binding.importFile.visibility = View.VISIBLE
        }

        binding.importFileWithHint.setOnClickListener {
            handleImport()
        }

        binding.importFile.setOnClickListener {
            handleImport()
        }

        initRecycleView()

        binding.tvTitle.setText(titleRes)

        // 初始状态更新按钮
        updateImportButtonState(false)
    }

    private fun refreshChooseAllView() {
        val id = if (isChooseAll) R.string.common_text_cancel_all_select else R.string.common_text_all_select
        binding.tvChooseAll.text = getString(id)
    }

    private fun initRecycleView() {
        mAdapter = MultiTypeAdapter()
        mAdapter?.register(FileSelectorBinder(dealItemClick = {
            AutelLog.i(TAG, "dealItemClick -> index =$it isEdit=$isEdit")
            refreshTitle()
            if (isEdit) {
                val hasFiles = hasSelectFile()
                updateImportButtonState(hasFiles)
            } else {
                //当是文件夹，则进入
                if (it.file.isDirectory) {
                    curPath = it.file.absolutePath
                    initUCardData()
                } else {
                    curFileList.forEach { it.isSelect = false }
                    it.isSelect = true
                    mAdapter?.notifyDataSetChanged()
                    updateImportButtonState(true)
                }
            }
        }))
        binding.rlChooseFile.layoutManager = LinearLayoutManager(this)
        mAdapter?.items = curFileList
        binding.rlChooseFile.adapter = mAdapter
    }

    /**
     * 是否有选中的
     */
    private fun hasSelectFile(): Boolean {
        for (x in curFileList) {
            if (x.isSelect) return true
        }
        return false
    }

    /**
     * 刷新标题
     */
    private fun refreshTitle() {
        if (isEdit) {
            var count = 0
            for (x in curFileList) {
                if (x.isSelect) {
                    count++
                }
            }
            binding.tvTitle.text = String.format(Locale.ENGLISH, getString(R.string.common_text_mission_selector_num), count)
        } else {
            binding.tvTitle.text = getString(titleRes)
        }
    }

    /**
     * 构造数据
     */
    private fun initUCardData() {
        if (!TextUtils.isEmpty(curPath)) {
            binding.tvSourcePath.text = curPath
        }
        curFileList.clear()
        analysisFilePath()
    }

    /**
     * 解析路径-构造数据
     */
    private fun analysisFilePath() {
        try {
            if (!TextUtils.isEmpty(curPath)) {
                val folder = File(curPath)
                val fileList = folder.listFiles()
                fileList?.forEach {
                    val isDirectory = it.isDirectory
                    val shouldAddFile = when {
                        isDirectory -> true
                        fileSuffix.isNullOrEmpty() && fileSuffixes.isNullOrEmpty() -> true
                        fileSuffix != null && it.name.endsWith(fileSuffix!!) -> true
                        fileSuffixes != null && fileSuffixes!!.any { suffix -> it.name.endsWith(suffix, ignoreCase = true) } -> true
                        else -> false
                    }

                    if (shouldAddFile) {
                        val file = SelectorFile(it)
                        AutelLog.d(TAG, it.absolutePath)
                        curFileList.add(file)
                    }

                }
                mAdapter?.notifyDataSetChanged()
            }

        } catch (e: Exception) {
            AutelLog.e(TAG, "e = $e")
        }
    }

    /**
     * 处理返回时间
     */
    private fun dealBack() {
        AutelLog.i(TAG, "dealBack -> curPath=$curPath")
        if (curPath == getUsbPath()) {
            finish()
        } else {
            val index = curPath.lastIndexOf("/")
            if (index == -1) {
                finish()
            } else {
                curPath = curPath.substring(0, index)
                initUCardData()
            }
        }
    }

    private fun handleImport() {
        val intent = Intent()
        val selectedFiles = curFileList.filter { it.isSelect }
        val paths = selectedFiles.map { it.file.absolutePath }
        intent.putStringArrayListExtra("path", ArrayList(paths))
        setResult(RESULT_OK, intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mUsbReceiver)
    }

    // 在 hasSelectFile() 方法后添加更新UI状态的方法
    private fun updateImportButtonState(hasSelectedFiles: Boolean) {
        if (useNewImportUI) {
            binding.bottomLayout.isVisible = true  // 始终显示底部布局
            binding.importFileWithHint.isEnabled = hasSelectedFiles
            binding.importFileWithHint.alpha = if (hasSelectedFiles) 1.0f else 0.5f
            binding.tipTextView.alpha = if (hasSelectedFiles) 1.0f else 0.5f
        } else {
            binding.importFile.isVisible = hasSelectedFiles
        }
    }
}