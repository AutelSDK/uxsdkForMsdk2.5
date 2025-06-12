package com.autel.setting.view

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.text.TextUtils
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.autel.common.base.BaseAircraftActivity
import com.autel.common.manager.AiServiceManager
import com.autel.common.widget.dialog.CommonLoadingDialog
import com.autel.log.AutelLog
import com.autel.setting.R
import com.autel.setting.bean.FileInfoBean
import com.autel.setting.bean.FileTypeEnum
import com.autel.setting.business.ImportAiModeVM
import com.autel.setting.databinding.ActivityImportAiModeBinding
import com.autel.setting.view.binder.ImportAiModeBinder
import com.drakeet.multitype.MultiTypeAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

/**
 * @author
 * @date 2023/8/4
 * 导入AI模型
 */
class ImportAiModeActivity : BaseAircraftActivity() {
    private var TAG = "ImportAiModeActivity"
    private lateinit var binding: ActivityImportAiModeBinding
    private var mAdapter: MultiTypeAdapter? = null
    private val curFileList = ArrayList<FileInfoBean>()
    private var isEdit = false
    private var isChooseAll = false
    private var mUsbReceiver: BroadcastReceiver? = null
    private var curPath = ""//当前路径
    private val importAiModeVM: ImportAiModeVM by viewModels()
    private var loadingDialog: CommonLoadingDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImportAiModeBinding.inflate(layoutInflater)
        setContentView(binding.root)
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
                showToast(R.string.common_text_u_card_remove)
                finish()
            }
        }
        val usbDeviceStateFilter = IntentFilter()
        usbDeviceStateFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        registerReceiver(mUsbReceiver, usbDeviceStateFilter)
        AutelLog.i(TAG, "U card = ${getUsbPath()}")
        getUsbPath()?.let { curPath = it }
    }

    /**
     * 获取U盘路径
     * @return
     */
    private fun getUsbPath(): String? {
        return AiServiceManager.getUsbPath(this)
    }

    private fun initView() {
        loadingDialog = CommonLoadingDialog.Builder(this).setMessage("").builder()
        binding.ivBack.setOnClickListener {
            dealBack()
        }
        binding.tvImportOfflineMode.setOnClickListener {
            dealImportOfflineMode()
        }

        binding.tvChooseAll.setOnClickListener {
            isChooseAll = !isChooseAll
            refreshChooseAllView()
            refreshFileItem(isEdit = true, isSelectAll = isChooseAll)
            binding.tvImportOfflineMode.isVisible = hasChooseAiMode()
            refreshTitle()
        }

        binding.tvState.setOnClickListener {
            isEdit = !isEdit
            if (!isEdit) {
                binding.tvImportOfflineMode.isVisible = false
                isChooseAll = false
                refreshChooseAllView()
            }
            refreshTitle()
            binding.ivBack.isVisible = !isEdit
            binding.tvChooseAll.isVisible = isEdit
            val id = if (isEdit) R.string.common_text_cancel else R.string.common_text_select
            binding.tvState.text = getString(id)
            refreshFileItem(isEdit, isChooseAll)
        }
        initRecycleView()
    }

    private fun refreshChooseAllView() {
        val id = if (isChooseAll) R.string.common_text_cancel_all_select else R.string.common_text_all_select
        binding.tvChooseAll.text = getString(id)
    }

    private fun initRecycleView() {
        mAdapter = MultiTypeAdapter()
        mAdapter?.register(ImportAiModeBinder(dealItemClick = {
            AutelLog.i(TAG, "dealItemClick -> index =$it isEdit=$isEdit")
            refreshTitle()
            if (isEdit) {
                binding.tvImportOfflineMode.isVisible = hasChooseAiMode()
            } else {
                //当是文件夹，则进入
                if (curFileList[it].fileType == FileTypeEnum.FOLDER) {
                    curPath = curFileList[it].filePath
                    initUCardData()
                }
            }
        }))
        binding.rlChooseFile.layoutManager = LinearLayoutManager(this)
        binding.rlChooseFile.adapter = mAdapter
        refreshFileItem(false)
    }

    /**
     * 是否有选中的
     */
    private fun hasChooseAiMode(): Boolean {
        for (x in curFileList) {
            if (x.isChoose && x.fileType == FileTypeEnum.AI_MODE) return true
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
                if (x.isChoose && x.fileType == FileTypeEnum.AI_MODE) {
                    count++
                }
            }
            binding.tvTitle.text = String.format(Locale.ENGLISH, getString(R.string.common_text_mission_selector_num), count)
        } else {
            binding.tvTitle.text = getString(R.string.common_text_ai_mode_import)
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
        refreshFileItem()
    }

    /**
     * 解析路径-构造数据
     */
    private fun analysisFilePath() {
        try {
            if (!TextUtils.isEmpty(curPath)) {
                val folder = File(curPath)
                val fileList = folder.listFiles()
                for (x in fileList) {
                    val fileInfoBean = FileInfoBean()
                    fileInfoBean.fileName = x.name
                    fileInfoBean.fileType = if (x.isDirectory) {
                        FileTypeEnum.FOLDER
                    } else {
                        if (x.name.endsWith(FileTypeEnum.AI_MODE.tag)) FileTypeEnum.AI_MODE else FileTypeEnum.FILE
                    }
                    fileInfoBean.filePath = x.absolutePath
                    curFileList.add(fileInfoBean)
                }
            }

        } catch (e: Exception) {
            AutelLog.e(TAG, "e = $e")
        }
    }

    /**
     * 刷新历史记录
     */
    private fun refreshFileItem(isEdit: Boolean = false, isSelectAll: Boolean = false) {
        binding.rlItemContent.isVisible = curFileList.isNotEmpty()
        binding.rlEmpty.isVisible = curFileList.isEmpty()
        if (curFileList.isNotEmpty()) {
            for (x in curFileList.indices) {
                curFileList[x].showBottomLine = x != curFileList.size - 1
                curFileList[x].index = x
                curFileList[x].isEdit = isEdit
                if (isEdit) {
                    curFileList[x].isChoose = isSelectAll && curFileList[x].fileType == FileTypeEnum.AI_MODE
                } else {
                    curFileList[x].isChoose = false
                }
            }
            mAdapter?.items = curFileList
            mAdapter?.notifyDataSetChanged()
        }
    }

    private fun dealImportOfflineMode() {
        val importList = ArrayList<File>()
        for (x in curFileList) {
            if (x.fileType == FileTypeEnum.AI_MODE && x.isChoose) {
                importList.add(File(x.filePath))
            }
        }
        AutelLog.i(TAG, "importList=$importList")
        if (importList.isEmpty()) return

        importAiModeVM.checkAiMode(importList, onCheckResult = { existList ->
            if (existList.isEmpty()) {
                importAiMode(importList,false)
            } else {
                //有已存在的
                AutelLog.i(TAG, "已存在模型：$existList")
                var fileNames = ""
                for (x in existList) {
                    fileNames += "\"${x.name}\" "
                }
                val content = String.format(Locale.ENGLISH, getString(R.string.common_text_import_ai_mode_tips), fileNames)
                ImportAiModeDialog(this).apply {
                    setContentText(content)
                    setOnReplaceListener {
                        importAiMode(importList,true)
                    }
                    setOnJumpToImportListener {
                        for (x in existList) {
                            importList.remove(x)
                        }
                        importAiMode(importList,false)
                    }
                    show()
                }
            }
        })
    }

    private fun importAiMode(importList: ArrayList<File>,isReplace :Boolean) {
        loadingDialog?.show()
        //如果跳过之后为空，则直接成功
        if (importList.isEmpty()) {
            showToast(R.string.common_text_import_ai_mode_success)
            finish()
            return
        }
        importAiModeVM.importAiMode(importList,isReplace, onResult = {
            lifecycleScope.launch(Dispatchers.Main){
                loadingDialog?.dismiss()
                if (it) {
                    showToast(R.string.common_text_import_ai_mode_success)
                    finish()
                } else {
                    showToast(R.string.common_text_import_ai_mode_failure)
                }
            }
        })
    }

    /**
     * 处理返回时间
     */
    private fun dealBack() {
        if (curPath == getUsbPath()) {
            finish()
        } else {
            curPath = curPath.substring(0, curPath.lastIndexOf("/"))
            initUCardData()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mUsbReceiver)
    }
}