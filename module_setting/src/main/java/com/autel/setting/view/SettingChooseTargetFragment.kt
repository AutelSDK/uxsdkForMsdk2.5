package com.autel.setting.view

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.autel.common.base.BaseAircraftFragment
import com.autel.common.bean.AiCustomTargetBean
import com.autel.common.bean.AiModelBean
import com.autel.common.bean.AiModelState
import com.autel.common.bean.AiTargetBean
import com.autel.common.lifecycle.LiveDataBus
import com.autel.common.lifecycle.event.ChangeAISelectorEvent
import com.autel.common.listener.AiLoginListener
import com.autel.common.manager.AiRecognitionPlayer
import com.autel.common.manager.AiServiceManager
import com.autel.common.manager.AppInfoManager
import com.autel.common.manager.AutelStorageManager
import com.autel.common.manager.StorageKey
import com.autel.common.utils.DeviceUtils
import com.autel.common.widget.CommonItemCheckView
import com.autel.setting.dialog.AiLoginDialog
import com.autel.common.widget.dialog.CommonSingleButtonDialog
import com.autel.common.widget.dialog.CommonTwoButtonDialog
import com.autel.drone.sdk.libbase.error.IAutelCode
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.manager.keyvalue.callback.CommonCallbacks
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.aiservice.bean.ExtendedDetectBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.aiservice.enums.AiDetectSceneTypeEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.aiservice.enums.DetectTargetEnum
import com.autel.log.AutelLog
import com.autel.setting.R
import com.autel.setting.business.SettingChooseTargetVM
import com.autel.setting.databinding.SettingChooseTargetFragmentBinding
import com.autel.setting.infc.UploadListener
import com.autel.setting.itemviewbinder.AiModelViewBinder
import com.autel.common.utils.NetWorksUtils
import com.drakeet.multitype.MultiTypeAdapter
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

/**
 * @author 
 * @date 2023/2/15
 * 选择识别类型
 */
class SettingChooseTargetFragment : BaseAircraftFragment() {
    private val TAG = "SettingChooseTargetFragment"
    private lateinit var binding: SettingChooseTargetFragmentBinding
    private val aiTarget = AiTargetBean()
    private val peopleList = ArrayList<Int>()//人集合
    private val carList = ArrayList<Int>()//车集合
    private val shipList = ArrayList<Int>()//船集合
    private val aircraftList = ArrayList<Int>()//飞行器集合
    private val fireList = ArrayList<Int>()//烟雾火
    private val customList = ArrayList<Int>()//自定义类别，当前的
    private val gson = Gson()
    private var aiAdapter: MultiTypeAdapter? = null
    private val aiModelList = ArrayList<AiModelBean>()//Ai模型数据集合
    private val customTargetList = ArrayList<ExtendedDetectBean>()//当前自定义类别集合,总集合

    private val chooseTargetVM: SettingChooseTargetVM by viewModels()
    private var defaultBean: AiTargetBean? = null//默认的列表

    private var isOnlineAiMode = true//当前是否是在线Ai模型
    private var mUsbReceiver: BroadcastReceiver? = null

    private var offLineAiAdapter: MultiTypeAdapter? = null
    private val offlineAiModelList = ArrayList<AiModelBean>()//离线Ai模型数据集合

    private fun refreshBottomLine() {
        if (AppInfoManager.isSupportAiTargetAll()) {
            binding.cicvTargetAircraft.showBottomLine(true)
        } else {
            if (AppInfoManager.isSupportPublicAITarget()) {
                binding.cicvTargetAircraft.showBottomLine(false)
            }
        }
        binding.cicvTargetFire.showBottomLine(AppInfoManager.isSupportPublicAITarget() || AppInfoManager.isSupportAiTargetAll())
    }

    /**
     * 刷新自定义的target 不为空
     */
    private fun refreshCustomTarget() {
        refreshBottomLine()
        binding.llCustomModel.removeAllViews()
        for (x in customTargetList.indices) {
            context?.let {
                val customView = CommonItemCheckView(it)
                customView.setTitleName(customTargetList[x].modelName)
                customView.setChecked(getCustomDefaultCheck(customTargetList[x].modelId))
                customView.setOnClickListener {
                    val detectBean = getCustomTargetByName(customView.getTitleName())
                    detectBean?.let { detectBean ->
                        if (customList.contains(detectBean.modelId)) {
                            if (isOnlyOneSelected()) return@setOnClickListener
                            customList.remove(detectBean.modelId)
                            customView.setChecked(false)
                            saveTargetData()
                        } else {
                            customList.add(detectBean.modelId)
                            customView.setChecked(true)
                            saveTargetData()
                        }
                    }
                }
                //最后一个不显示底部分割线
                if (x == customTargetList.size - 1) {
                    customView.showBottomLine(false)
                }
                binding.llCustomModel.addView(customView)
            }
        }

        //如果id和列表id一样，则需要标记已上传
        dealUploadSuccessStatus()
    }

    /**
     * 处理上传成功的状态
     */
    @SuppressLint("NotifyDataSetChanged")
    private fun dealUploadSuccessStatus(isOffline: Boolean = false) {
        val customModelId = AiServiceManager.aiCustomTargetLD.value?.customModelId ?: ""
        //刷新离线模型，如果集合不为空就刷
        for (x in offlineAiModelList) {
            x.state = if (x.modelId == customModelId) AiModelState.UPLOAD_SUCCESS else AiModelState.UPLOAD_WAIT
        }
        offLineAiAdapter?.notifyDataSetChanged()

        //刷新在线模型
        for (x in aiModelList) {
            if (x.modelId == customModelId) {
                x.state = AiModelState.UPLOAD_SUCCESS
                aiAdapter?.notifyItemChanged(x.index)
            } else {
                //这里统一做成待下载状态
                chooseTargetVM.isAiModeExit(x) {
                    x.state = if (it) AiModelState.UPLOAD_WAIT else AiModelState.DOWNLOAD_WAIT
                    aiAdapter?.notifyItemChanged(x.index)
                }
            }
        }
    }

    /**
     * 通过name查询对应的对象
     *
     */
    private fun getCustomTargetByName(name: String): ExtendedDetectBean? {
        for (x in customTargetList) {
            if (x.modelName == name) return x
        }
        return null
    }

    /**
     * 查询自定义列表默认选中状态
     */
    private fun getCustomDefaultCheck(modelId: Int): Boolean {
        for (x in customList) {
            if (x == modelId) return true
        }
        return false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = SettingChooseTargetFragmentBinding.inflate(LayoutInflater.from(context))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecognitionSound()
    }

    private fun initRecognitionSound() {
        binding.cisTarget.setCheckedWithoutListener( AutelStorageManager.getPlainStorage().getBooleanValue(StorageKey.PlainKey.KEY_AI_RECOGNITION_SOUND_TIP, false))
        binding.cisTarget.setOnSwitchChangeListener {
            if(!it) {
                AiRecognitionPlayer.stopWarn()
            }
            AutelStorageManager.getPlainStorage().setBooleanValue(StorageKey.PlainKey.KEY_AI_RECOGNITION_SOUND_TIP, it)
        }
    }

    override fun getData() {

    }


    override fun onDroneChangedListener(connected: Boolean, drone: IAutelDroneDevice) {
       //只处理当前单控飞机
        if (DeviceUtils.singleControlDrone() == drone){
            if (connected) {
                val bean = AiServiceManager.aiCustomTargetLD.value
                AutelLog.i(TAG, "connected -> bean=$bean")
                bean?.let { dealAiCustomModel(bean) }
            } else {
                refreshBottomLine()
                binding.llCustomModel.removeAllViews()
            }
        }
    }

    /**
     * 注册U盘监听
     */
    private fun initUsbReceiver() {
        mUsbReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == UsbManager.ACTION_USB_DEVICE_DETACHED) {
                    binding.tvImportOfflineMode.isVisible = false
                } else {
                    val timer = Timer()
                    timer.schedule(object : TimerTask() {
                        override fun run() {
                            lifecycleScope.launch(Dispatchers.Main) {
                                binding.tvImportOfflineMode.isVisible = !isOnlineAiMode
                            }
                        }

                    }, 500)

                }
            }
        }
        val usbDeviceStateFilter = IntentFilter()
        usbDeviceStateFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        usbDeviceStateFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        activity?.registerReceiver(mUsbReceiver, usbDeviceStateFilter)
    }

    override fun addListen() {
        if (AppInfoManager.isSupportAiCloudService()) {
            initUsbReceiver()
        }
        val default = AutelStorageManager.getPlainStorage().getStringValue(StorageKey.PlainKey.KEY_AI_TARGET_LIST, "")
        AutelLog.i(TAG, "initObserver  -> default=$default")
        if (!default.isNullOrEmpty()) {
            defaultBean = gson.fromJson(default, AiTargetBean::class.java)
            try {
                //初始化固定类别
                aiTarget.aiTargetList.addAll(defaultBean?.aiTargetList ?: ArrayList())
                if (aiTarget.aiTargetList.isNotEmpty()) {
                    for (x in aiTarget.aiTargetList) {
                        when (x) {
                            //人
                            DetectTargetEnum.PERSON.value, DetectTargetEnum.SECURITY_CIVILIAN.value, DetectTargetEnum.SECURITY_SOLDIER.value -> {
                                peopleList.add(x)
                            }
                            //车
                            DetectTargetEnum.CAR.value, DetectTargetEnum.SECURITY_ARMORED_VEHICLE.value, DetectTargetEnum.VEHICLE.value -> {
                                carList.add(x)
                            }
                            //船
                            DetectTargetEnum.BOAT.value -> shipList.add(x)
                            //飞行器
                            DetectTargetEnum.SECURITY_AERIAL_VEHICLE.value -> aircraftList.add(x)

                            //烟雾火
                            DetectTargetEnum.SECURITY_SMOKE.value, DetectTargetEnum.SECURITY_FIRE.value -> fireList.add(x)
                            else -> {}
                        }
                    }
                }
                customList.clear()
                //用户的自定义类别
                aiTarget.customModelId = defaultBean?.customModelId ?: ""
                defaultBean?.customAiTargetList?.let { list ->
                    for (x in list) {
                        customList.add(x.modelId)
                    }
                }

            } catch (e: Exception) {
                AutelLog.e(TAG, "e: $e")
            }

        }
        refreshTarget()
        binding.cicvTargetPeople.setOnClickListener {
            if (isContainPeople()) {
                if (isOnlyOneSelected()) return@setOnClickListener
                peopleList.clear()
            } else {
                peopleList.add(DetectTargetEnum.PERSON.value)
                peopleList.add(DetectTargetEnum.SECURITY_CIVILIAN.value)
                peopleList.add(DetectTargetEnum.SECURITY_SOLDIER.value)
            }
            binding.cicvTargetPeople.setChecked(isContainPeople())
            saveTargetData()
        }

        binding.cicvTargetCar.setOnClickListener {
            if (isContainCar()) {
                if (isOnlyOneSelected()) return@setOnClickListener
                carList.clear()
            } else {
                carList.add(DetectTargetEnum.CAR.value)
                carList.add(DetectTargetEnum.VEHICLE.value)
                if (AppInfoManager.isSupportAiTargetAll()) {
                    carList.add(DetectTargetEnum.SECURITY_ARMORED_VEHICLE.value)
                }
            }
            binding.cicvTargetCar.setChecked(isContainCar())
            saveTargetData()
        }
        binding.cicvTargetShip.setOnClickListener {
            if (isContainShip()) {
                if (isOnlyOneSelected()) return@setOnClickListener
                shipList.clear()
            } else {
                shipList.add(DetectTargetEnum.BOAT.value)
            }
            binding.cicvTargetShip.setChecked(isContainShip())
            saveTargetData()
        }
        binding.cicvTargetAircraft.setOnClickListener {
            if (isContainAircraft()) {
                if (isOnlyOneSelected()) return@setOnClickListener
                aircraftList.clear()
            } else {
                aircraftList.add(DetectTargetEnum.SECURITY_AERIAL_VEHICLE.value)
            }
            binding.cicvTargetAircraft.setChecked(isContainAircraft())
            saveTargetData()
        }


        binding.cicvTargetFire.setOnClickListener {
            if (isContainSmokeFire()) {
                if (isOnlyOneSelected()) return@setOnClickListener
                fireList.clear()
            } else {
                fireList.add(DetectTargetEnum.SECURITY_SMOKE.value)
                fireList.add(DetectTargetEnum.SECURITY_FIRE.value)
            }
            binding.cicvTargetFire.setChecked(isContainSmokeFire())
            saveTargetData()
        }

        binding.cicvTargetAircraft.isVisible = AppInfoManager.isSupportAiTargetAll() || AppInfoManager.isSupportPublicAITarget()

        refreshBottomLine()

        //初始化Ai模型功能
        initAiMode()
    }

    /**
     * 初始化AI模型功能
     */
    private fun initAiMode() {
        //选择模式
        binding.rlAiChoose.isVisible = AppInfoManager.isSupportAiCloudService()
        binding.llOnlinePanel.isVisible = AppInfoManager.isSupportOnlineAiMode() && AppInfoManager.isSupportAiCloudService()
        binding.rlOfflinePanel.isVisible = AppInfoManager.isSupportAiCloudService()

        isOnlineAiMode = AppInfoManager.isSupportOnlineAiMode()
        binding.tvOnlineMode.isVisible = AppInfoManager.isSupportOnlineAiMode()

        binding.tvOnlineMode.setOnClickListener {
            if (!isOnlineAiMode) {
                isOnlineAiMode = true
                refreshAiChoose()
            }
        }
        binding.tvOfflineMode.setOnClickListener {
            if (isOnlineAiMode) {
                isOnlineAiMode = false
                refreshAiChoose()
            }
        }
        if (AppInfoManager.isSupportAiCloudService()) {
            refreshAiChoose()
            initOnlineAiModel()
            initOfflineAiModel()
        }
    }

    override fun onResume() {
        super.onResume()
        if (AppInfoManager.isSupportAiCloudService()) {
            getOfflineAiModeList()
        }
    }

    /**
     * 初始化离线AI模型
     */
    private fun initOfflineAiModel() {
        binding.tvImportOfflineMode.setOnClickListener {
            activity?.let { startActivity(Intent(activity, ImportAiModeActivity::class.java)) }
        }
        binding.tvImportOfflineMode.isVisible = !TextUtils.isEmpty(AiServiceManager.getUsbPath(activity)) && !isOnlineAiMode

        offLineAiAdapter = MultiTypeAdapter()
        offLineAiAdapter?.register(AiModelViewBinder(requireContext(), dealItemClick = {
            AutelLog.d(TAG, "dealItemClick -> $it")
            when (it.state) {
                AiModelState.UPLOAD_WAIT -> {
                    dealUploadWait(it)
                }

                AiModelState.UPLOAD_SUCCESS -> {
                    showToast(R.string.common_text_model_upload_success)
                }

                else -> {}
            }
        }, {
            activity?.let { context ->
                val msg = String.format(Locale.ENGLISH, getString(R.string.common_text_ai_mode_delete_tips), "\"${it.projectName}\"")
                CommonTwoButtonDialog(context)
                    .apply {
                        setMessage(msg)
                        setTitle(getString(R.string.common_text_delete))
                        setRightBtnListener {
                            //删除了，刷新下模型列表
                            chooseTargetVM.deleteAiMode(it.url)
                            getOfflineAiModeList()
                        }
                        show()
                    }

            }
        }, true))
        binding.rlOfflineAiModel.layoutManager = LinearLayoutManager(context)
        binding.rlOfflineAiModel.adapter = offLineAiAdapter
    }

    /**
     * 获取离线的AI模型
     */
    private fun getOfflineAiModeList() {
        chooseTargetVM.getOfflineAiModelList({
            if (it.isEmpty()) {
                showOfflineModeList(false)
            } else {
                offlineAiModelList.clear()
                offlineAiModelList.addAll(it)
                showOfflineModeList(true)
            }
        }, {
            showToast(R.string.common_text_model_load_failure_tips)
        })
    }

    /**
     * 展示离线模型View
     */
    private fun showOfflineModeList(hasData: Boolean) {
        binding.ivNoData.isVisible = !hasData
        binding.tvNoImportTips.isVisible = !hasData
        binding.rlOfflineAiModel.isVisible = hasData
        if (offlineAiModelList.isNotEmpty()) {
            offlineAiModelList.last().showBottomLine = false
        }
        dealUploadSuccessStatus(true)
        offLineAiAdapter?.items = offlineAiModelList
        offLineAiAdapter?.notifyDataSetChanged()
    }

    /**
     * 刷新AI模式选择
     */
    private fun refreshAiChoose() {
        context?.let { context ->
            binding.tvOnlineMode.setTextColor(context.getColor(getAiChooseColor(isOnlineAiMode)))
            binding.tvOfflineMode.setTextColor(context.getColor(getAiChooseColor(!isOnlineAiMode)))
        }
        binding.llOnlinePanel.isVisible = isOnlineAiMode
        binding.rlOfflinePanel.isVisible = !isOnlineAiMode
        binding.tvImportOfflineMode.isVisible = !TextUtils.isEmpty(AiServiceManager.getUsbPath(activity)) && !isOnlineAiMode
    }

    /**
     * 选中颜色
     */
    private fun getAiChooseColor(isChoose: Boolean): Int {
        return if (isChoose) R.color.common_color_FEE15D else R.color.common_color_BDBDBD_60
    }

    /**
     * 处理自定义模型
     */
    private fun dealAiCustomModel(bean: AiCustomTargetBean) {
        customTargetList.clear()
        customTargetList.addAll(bean.customAiTargetList)
        //如果配置的模型id和本地存储的不一样时
        AutelLog.i(TAG, "dealAiCustomModel aiTarget.customModelId=${aiTarget.customModelId} customModelId=${bean.customModelId}")
        if (bean.customModelId != aiTarget.customModelId) {
            aiTarget.customModelId = bean.customModelId
            aiTarget.customAiTargetList.clear()
            customList.clear()
            for (x in customTargetList) {
                customList.add(x.modelId)
            }
        }
        refreshCustomTarget()
    }

    /**
     * 初始化在线AI模型
     */
    private fun initOnlineAiModel() {
        //自定义类别变化，则需要更新列表
        AiServiceManager.aiCustomTargetLD.observe(viewLifecycleOwner) {
            AutelLog.i(TAG, "initAiModel -> customTarget=$it")
            //飞机连上才初始化自定义模型，否则不显示
            if (DeviceUtils.isSingleControlDroneConnected()) dealAiCustomModel(it)
        }

        binding.tvModelReload.setOnClickListener { getAiModelList() }

        binding.tvModelLogin.setOnClickListener {
            //判断网络
            if (!NetWorksUtils.isInternetAvailable(requireContext())) {
                showToast(R.string.common_text_internet_error)
                return@setOnClickListener
            }

            context?.let {
                AiLoginDialog(it).setAiLoginListener(object : AiLoginListener {
                    override fun loginSuccess() {
                        setAiModeLogin(true)
                        getAiModelList()
                    }
                }).show()
            }
        }

        aiAdapter = MultiTypeAdapter()
        aiAdapter?.register(AiModelViewBinder(requireContext(), dealItemClick = {
            AutelLog.d(TAG, "dealItemClick -> $it")
            when (it.state) {
                AiModelState.DOWNLOAD_WAIT -> {
                    dealDownloadWait(it)
                }

                AiModelState.DOWNLOAD_EXECUTE, AiModelState.UPLOAD_EXECUTE -> {}
                AiModelState.UPLOAD_WAIT -> {
                    dealUploadWait(it)
                }

                AiModelState.UPLOAD_SUCCESS -> {
                    showToast(R.string.common_text_model_upload_success)
                }
            }
        }, {}))
        binding.rlAiModel.layoutManager = LinearLayoutManager(context)
        binding.rlAiModel.adapter = aiAdapter

        if (AiServiceManager.isAiServiceLogin()) {
            setAiModeLogin(true)
            getAiModelList()
        } else {
            setAiModeLogin(false)
        }
    }


    /**
     * 加载AI模型list
     */
    private fun getAiModelList() {
        //判断网络
        if (!NetWorksUtils.isInternetAvailable(requireContext())) {
            showToast(R.string.common_text_internet_error)
            return
        }

        loadAiModelExecute()
        chooseTargetVM.getAiModelList(onSuccess = {
            aiModelList.clear()
            aiModelList.addAll(it)
            if (aiModelList.isEmpty()) {
                loadAiModelNone()
            } else {
                refreshAiModel()
            }
        }, {
            if (it) {
                setAiModeLogin(false)
                showToast(R.string.common_text_flight_record_user_info_error)
            } else {
                loadAiModelFailure()
            }
        })
    }

    /**
     * 处理点击上传事件
     */
    private fun dealUploadWait(bean: AiModelBean) {
        val drone = DeviceUtils.singleControlDrone()
        if (drone == null) {
            showToast(R.string.common_text_no_drone_controler)
            return
        }
        //未连接
        if (!DeviceUtils.isSingleControlDroneConnected()) {
            showToast(R.string.common_text_aircraft_disconnect)
            return
        }
        //飞机电机运转也不能上传
        if (DeviceUtils.isDroneFlying(drone)) {
            showToast(R.string.common_text_ai_upload_tips)
            return
        }

        var hasUploadExecute = false
        var hasUploadSuccess = false
        //这里在线和离线的都要判断
        for (x in aiModelList) {
            if (x.state == AiModelState.UPLOAD_EXECUTE) {
                hasUploadExecute = true
            }
            if (x.state == AiModelState.UPLOAD_SUCCESS) {
                hasUploadSuccess = true
            }
        }

        for (x in offlineAiModelList) {
            if (x.state == AiModelState.UPLOAD_EXECUTE) {
                hasUploadExecute = true
            }
            if (x.state == AiModelState.UPLOAD_SUCCESS) {
                hasUploadSuccess = true
            }
        }

        //已经有上传的，禁止再上传
        if (hasUploadExecute) {
            showToast(R.string.common_text_model_upload_tips)
            return
        }
        //如果已有上传过的，则需要二次弹框告诉用户是否替换模型
        if (hasUploadSuccess) {
            context?.let { context ->
                CommonTwoButtonDialog(context)
                    .apply {
                        setTitle(getString(R.string.common_text_model_replace_title))
                        setMessage(getString(R.string.common_text_model_replace_desc))
                        setRightBtnListener { realDealUploadWait(bean) }
                        show()
                    }
            }
            return
        }
        //开始上传逻辑
        realDealUploadWait(bean)
    }

    /**
     * 真正的上传流程
     */
    @SuppressLint("NotifyDataSetChanged")
    private fun realDealUploadWait(bean: AiModelBean) {
        context?.let {
            val dialog = UploadAiModeDialog(it, chooseTargetVM)
            dialog.uploadAiMode(isOnlineAiMode, bean, object : UploadListener {
                override fun notifyItemChanged() {
                    if (isOnlineAiMode) {
                        aiAdapter?.notifyItemChanged(bean.index)
                    } else {
                        offLineAiAdapter?.notifyItemChanged(bean.index)
                    }
                }

                override fun uploadCancel() {
                    showToast(R.string.common_text_ai_mode_cancel)
                }

                override fun uploadFailure() {
                    showToast(R.string.common_text_upload_fair)
                }

                override fun uploadSuccess() {
                    showToast(R.string.common_text_upload_sucess)
                }

                override fun exitFailure() {
                    CommonSingleButtonDialog(it).apply {
                        setDialogTitle(getString(R.string.common_text_tips_title))
                        setMessage(getString(R.string.common_text_ai_mode_exit_failure_tips))
                        setButtonText(getString(R.string.common_text_mission_got_known))
                        setCancelable(false)
                        show()
                    }
                }

                override fun enterFailure() {
                    showToast(R.string.common_text_ai_mode_enter_failure)
                }

            })
            dialog.show()
        }
    }

    /**
     * 处理点击的下载事件
     */
    @SuppressLint("NotifyDataSetChanged")
    private fun dealDownloadWait(bean: AiModelBean) {
        //判断网络
        if (!NetWorksUtils.isInternetAvailable(requireContext())) {
            showToast(R.string.common_text_internet_error)
            return
        }
        //开始下载逻辑
        if (bean.state == AiModelState.DOWNLOAD_EXECUTE) return
        bean.state = AiModelState.DOWNLOAD_EXECUTE
        aiAdapter?.notifyItemChanged(bean.index)
        AutelLog.i(TAG, "dealDownloadExecute -> start bean=$bean")
        chooseTargetVM.downloadModel(bean.url, bean.modelId, bean.md5, object : CommonCallbacks.CompletionCallbackWithProgress<Int> {
            override fun onProgressUpdate(progress: Int) {
                if (bean.progress == progress) return
                AutelLog.i(TAG, "dealDownloadExecute -> onProgressUpdate name=${bean.projectName} progress=$progress")
                bean.progress = progress
                lifecycleScope.launch(Dispatchers.Main) { aiAdapter?.notifyItemChanged(bean.index) }
            }

            override fun onSuccess(result: String?) {
                AutelLog.i(TAG, "dealDownloadExecute -> onSuccess name=${bean.projectName} result=$result")
                bean.state = AiModelState.UPLOAD_WAIT
                lifecycleScope.launch(Dispatchers.Main) {
                    showToast(R.string.common_text_download_success)
                    aiAdapter?.notifyItemChanged(bean.index)
                }
            }

            override fun onFailure(error: IAutelCode) {
                AutelLog.i(TAG, "dealDownloadExecute -> onFailure name=${bean.projectName} error=$error")
                bean.state = AiModelState.DOWNLOAD_WAIT
                lifecycleScope.launch(Dispatchers.Main) {
                    aiAdapter?.notifyItemChanged(bean.index)
                    showToast(R.string.common_text_firmware_download_fail)
                }
            }

        })
    }


    /**
     * 保存数据
     */
    private fun saveTargetData() {
        aiTarget.aiTargetList.clear()
        aiTarget.aiTargetList.addAll(peopleList)
        aiTarget.aiTargetList.addAll(carList)
        aiTarget.aiTargetList.addAll(shipList)
        aiTarget.aiTargetList.addAll(aircraftList)
        aiTarget.aiTargetList.addAll(fireList)

        aiTarget.customAiTargetList.clear()
        aiTarget.customAiTargetList.addAll(getCustomModelList())
        AutelStorageManager.getPlainStorage().setStringValue(StorageKey.PlainKey.KEY_AI_TARGET_LIST, gson.toJson(aiTarget))
        if (AppInfoManager.isSupportAiTargetAll() || AppInfoManager.isSupportPublicAITarget()) {
            AutelStorageManager.getPlainStorage()
                .setIntValue(StorageKey.PlainKey.KEY_AI_DETECT_SCENETYPE, AiDetectSceneTypeEnum.SCENE_TYPE_SECURITY.value)
        } else {
            AutelStorageManager.getPlainStorage()
                .setIntValue(StorageKey.PlainKey.KEY_AI_DETECT_SCENETYPE, AiDetectSceneTypeEnum.SCENE_TYPE_UNIVERSAL.value)
        }
        LiveDataBus.of(ChangeAISelectorEvent::class.java).changeAiSelect().post(aiTarget)
        AutelLog.i(TAG, "saveTargetData -> aiTarget=${gson.toJson(aiTarget)}")
    }

    /**
     * 获取当前用户选中的自定义类别集合
     */
    private fun getCustomModelList(): ArrayList<ExtendedDetectBean> {
        val list = ArrayList<ExtendedDetectBean>()
        for (x in customList) {
            getCustomModel(x)?.let { list.add(it) }
        }
        return list
    }

    /**
     * 通过id在自定义集合里面找
     */
    private fun getCustomModel(modelId: Int): ExtendedDetectBean? {
        for (x in customTargetList) {
            if (modelId == x.modelId) return x
        }
        return null
    }

    /**
     * 刷新选择识别类型
     */
    private fun refreshTarget() {
        binding.cicvTargetPeople.setChecked(isContainPeople())
        binding.cicvTargetCar.setChecked(isContainCar())
        binding.cicvTargetShip.setChecked(isContainShip())
        binding.cicvTargetAircraft.setChecked(isContainAircraft())
        binding.cicvTargetFire.setChecked(isContainSmokeFire())
    }

    /**
     * 仅有一项选中时候，不能再取消了
     */
    private fun isOnlyOneSelected(): Boolean {
        val selectResult = "${isContainSmokeFire()}${isContainArms()}${isContainAircraft()}${isContainShip()}${isContainCar()}${isContainPeople()}"
        val strs = selectResult.split("true")
        AutelLog.i(TAG, "selectResult=$selectResult strs=$strs")
        //当固定类别只有1个自定义为空，或者，固定类别没有自定义只有1个
        val result = (strs.size <= 2 && customList.isEmpty()) || strs.size <= 1 && customList.size == 1
        //val result = strs.size <= 2
        if (result) showToast(R.string.common_text_select_at_least_one)
        return result
    }

    /**
     * 是否包含烟雾、火
     */
    private fun isContainSmokeFire(): Boolean {
        return fireList.isNotEmpty()
    }

    /**
     * 是否包含武器设备
     */
    private fun isContainArms(): Boolean {
        return false
    }

    /**
     * 是否包括飞行器
     */
    private fun isContainAircraft(): Boolean {
        return aircraftList.isNotEmpty()
    }

    /**
     * 是否包含船
     */
    private fun isContainShip(): Boolean {
        return shipList.isNotEmpty()
    }

    /**
     * 是否包含车
     */
    private fun isContainCar(): Boolean {
        return carList.isNotEmpty()
    }

    /**
     * 是否包含人
     */
    private fun isContainPeople(): Boolean {
        return peopleList.isNotEmpty()
    }

    /**
     * 加载失败
     */
    private fun loadAiModelFailure() {
        binding.rlAiModel.isVisible = false
        binding.tvModelReload.isVisible = true
        binding.ivModelLoading.isVisible = false
        binding.ivModelTips.isVisible = true
        binding.tvModelTips.text = getString(R.string.common_text_model_load_failure_tips)
        binding.ivModelTips.setImageResource(R.drawable.setting_icon_cal_fail)
    }

    /**
     * 没有创建模型
     */
    private fun loadAiModelNone() {
        binding.rlAiModel.isVisible = false
        binding.tvModelReload.isVisible = false
        binding.ivModelLoading.isVisible = false
        binding.ivModelTips.isVisible = true
        binding.tvModelTips.text = getString(R.string.common_text_no_custom_model)
        binding.ivModelTips.setImageResource(R.drawable.icon_no_ai_model)
    }

    /**
     * 正在加载模型
     */
    private fun loadAiModelExecute() {
        binding.rlAiModel.isVisible = false
        binding.tvModelReload.isVisible = false
        binding.ivModelLoading.isVisible = true
        binding.ivModelTips.isVisible = false
        binding.tvModelTips.text = getString(R.string.common_text_model_loading)
    }

    /**
     * 设置ai账户登录状态
     */
    private fun setAiModeLogin(login: Boolean) {
        binding.rlAiModel.isVisible = false
        binding.llLogin.isVisible = !login
        binding.rlDataState.isVisible = login
    }

    /**
     * 刷新AI模型
     */
    @SuppressLint("NotifyDataSetChanged")
    private fun refreshAiModel() {
        binding.ivModelLoading.isVisible = false
        binding.rlAiModel.isVisible = true
        binding.rlDataState.isVisible = false
        binding.llLogin.isVisible = false
        if (aiModelList.isNotEmpty()) {
            aiModelList.last().showBottomLine = false
        }
        dealUploadSuccessStatus()
        aiAdapter?.items = aiModelList
        aiAdapter?.notifyDataSetChanged()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (activity != null && mUsbReceiver != null) {
            activity?.unregisterReceiver(mUsbReceiver)
        }
    }
}