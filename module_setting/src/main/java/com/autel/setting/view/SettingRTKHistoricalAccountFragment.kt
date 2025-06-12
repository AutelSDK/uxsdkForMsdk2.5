package com.autel.setting.view

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.autel.common.base.BaseAircraftFragment
import com.autel.common.manager.StorageKey
import com.autel.common.widget.dialog.CommonTwoButtonDialog
import com.autel.log.AutelLog
import com.autel.setting.business.SettingRtkVM
import com.autel.setting.databinding.SettingRtkHistoricalAccountFragmentBinding
import com.autel.data.bean.entity.HistoricalAccountModel
import com.autel.drone.sdk.libbase.error.IAutelCode
import com.autel.drone.sdk.vmodelx.interfaces.IRTKManager.*
import com.autel.drone.sdk.vmodelx.interfaces.RTKLoginStatusEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.rtk.enums.RTKSignalEnum
import com.autel.setting.R
import com.autel.setting.intent.HistoricalAccountIntent
import com.autel.setting.itemviewbinder.AccountViewBinder
import com.autel.setting.state.HistoricalAccountState
import com.autel.ui.decoration.LineDividerItemDecoration
import com.drakeet.multitype.MultiTypeAdapter
import kotlinx.coroutines.launch

/**
 * @author luxubin
 * @Date 2023 5/25
 *
 * RTK历史账号
 */
class SettingRTKHistoricalAccountFragment : BaseAircraftFragment() {

    companion object {
        const val TAG = "RTKHistoricalAccount"
    }

    lateinit var binding: SettingRtkHistoricalAccountFragmentBinding
    private lateinit var accountAdapter: MultiTypeAdapter
    private val settingRtkVM: SettingRtkVM by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = SettingRtkHistoricalAccountFragmentBinding.inflate(LayoutInflater.from(context))
        initView()
        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        accountAdapter = MultiTypeAdapter().apply {
            register(
                AccountViewBinder(
                    requireContext(),
                    { ham ->
                        showConfirmDeleteDialog(ham)
                    },
                    { ham ->
                        historicalAccountLogin(ham)
                    })
            )
        }

        binding.rvAccoutContent.apply {
            addItemDecoration(
                LineDividerItemDecoration(
                    lineSize = requireContext().resources.getDimensionPixelSize(R.dimen.common_line_width),
                    lineColor = context.getColor(R.color.common_color_e0),
                    marginStart = requireContext().resources.getDimensionPixelSize(R.dimen.common_50dp),
                    marginEnd = 0,
                    topLine = false,
                    bottomLine = false
                )
            )
            layoutManager = LinearLayoutManager(context)
            adapter = accountAdapter
        }

        lifecycleScope.launch {
            settingRtkVM.historicalAccountState.collect {
                when (it) {
                    HistoricalAccountState.Loading -> AutelLog.i(TAG, "Loading")
                    // 显示历史账号
                    is HistoricalAccountState.AccountList -> {
                        val items = mutableListOf<Any>()
                        items.addAll(it.list.reversed())
                        accountAdapter.items = items
                        accountAdapter.notifyDataSetChanged()
                    }

                    is HistoricalAccountState.OnError -> {
                        accountAdapter.items = mutableListOf<Any>()
                        accountAdapter.notifyDataSetChanged()
                    }
                }
            }
        }

        queryAccount()
    }

    private fun initView() {
        // 如果是登录状态，则设置正在登录的账号
        if (settingRtkVM.getRTKLoginState() == RTKLoginStatusEnum.LoggedIn) {
            setUsingAccount()

            binding.lyUsingAccount.ivDelete.setOnClickListener {
                showConfirmDeleteUsingDialog()
            }
        }
    }

    /**
     * 显示正在登录的账号
     */
    private fun setUsingAccount() {
        binding.lyUsingAccount.clHistoricalAccount.visibility = View.VISIBLE
        binding.lyUsingAccount.tvAccount.text = settingRtkVM.loginHistoryAccountStringValue(StorageKey.PlainKey.KEY_RTK_USER_NAME)
        binding.lyUsingAccount.tvServerAddr.text = settingRtkVM.loginHistoryAccountStringValue(StorageKey.PlainKey.KEY_RTK_SERVICE_ADDR)
        binding.lyUsingAccount.tvPort.text = settingRtkVM.loginHistoryAccountStringValue(StorageKey.PlainKey.KEY_RTK_PORT)
        binding.lyUsingAccount.tvMountPoint.text = settingRtkVM.loginHistoryAccountStringValue(StorageKey.PlainKey.KEY_RTK_MOUNT_POINT)
        binding.lyUsingAccount.ivSelected.visibility = View.VISIBLE
        binding.lyUsingAccount.ivDelete.isVisible = false
    }

    override fun getData() {

    }

    override fun addListen() {

    }

    /**
     * 查询所有未使用账号
     */
    private fun queryAccount() {
        if (!isAdded) return
        lifecycleScope.launch {
            settingRtkVM.historicalAccountIntent.send(HistoricalAccountIntent.QueryAllAccountUnused)
        }
    }

    /**
     * 删除账号
     */
    private fun delAccount(ham: HistoricalAccountModel) {
        if (!isAdded) return
        lifecycleScope.launch {
            settingRtkVM.historicalAccountIntent.send(HistoricalAccountIntent.DelOneAccount(ham))
        }
    }

    /**
     * 存储账号
     */
    private fun saveOneAccount(ham: HistoricalAccountModel) {
        if (!isAdded) return
        lifecycleScope.launch {
            settingRtkVM.historicalAccountIntent.send(HistoricalAccountIntent.SaveOneAccount(ham))
        }
    }

    /**
     * 展示删除账号对话框
     */
    private fun showConfirmDeleteDialog(ham: HistoricalAccountModel) {
        CommonTwoButtonDialog(requireContext()).apply {
            setTitle(getString(R.string.common_text_rtk_del_rtk_account))
            setMessage(getString(R.string.common_text_rtk_del_rtk_account_content))
            setLeftBtnStr(getString(R.string.common_text_cancel))
            setRightBtnStr(getString(R.string.common_text_confirm))
            setAutoDismiss(true)
            setLeftBtnListener {
            }
            setRightBtnListener {
                delAccount(ham)
                showToast(getString(R.string.common_text_delete_success))
                queryAccount()
            }
        }.show()
    }

    /**
     * 删除正在使用的账号
     */
    private fun showConfirmDeleteUsingDialog() {
        CommonTwoButtonDialog(requireContext()).apply {
            setTitle(getString(R.string.common_text_rtk_del_rtk_account))
            setMessage(getString(R.string.common_text_rtk_del_rtk_account_content))
            setLeftBtnStr(getString(R.string.common_text_cancel))
            setRightBtnStr(getString(R.string.common_text_confirm))
            setAutoDismiss(true)
            setLeftBtnListener {
            }
            setRightBtnListener {
                settingRtkVM.closeRtk(
                    onSuccess = {
                        AutelLog.i(TAG, "close rtk succcess")
                        lifecycleScope.launch {
                            settingRtkVM.historicalAccountIntent.send(HistoricalAccountIntent.DelAccountOnUsing)
                        }

                        binding.lyUsingAccount.clHistoricalAccount.visibility = View.GONE
                        showToast(getString(R.string.common_text_delete_success))
                    },
                    onError = {
                        AutelLog.i(TAG, "close rtk failed")
                        showToast(getString(R.string.common_text_dot_delete_fail))
                    })
            }
        }.show()
    }

    /**
     * 使用历史账号登录
     */
    private fun historicalAccountLogin(ham: HistoricalAccountModel) {
        val rtkSignal = settingRtkVM.rtkSignal()
        AutelLog.i(TAG, "historicalAccountLogin rtkSignal:$rtkSignal")
        when (rtkSignal) {
            RTKSignalEnum.NETWORK -> {
                settingRtkVM.authNetRtk(
                    ham.serverAddr,
                    ham.port.toInt(),
                    ham.account,
                    ham.password,
                    ham.mountPoint,
                    false,
                    object : RTKAuthoCallback {
                        override fun onRtkAuthorSuccess() {
                            AutelLog.i(TAG, "historicalAccountLogin success")
                            rtkLoginSuccess(ham)
                        }

                        override fun onFailure(code: IAutelCode, msg: String?) {
                            AutelLog.e(TAG, "historicalAccountLogin failed")
                            longinFailed(code)
                        }

                    }
                )
            }
            RTKSignalEnum.MOBILE_NETWORK_SERVICES -> {
                settingRtkVM.authMobileServiceRtk(ham.serverAddr, ham.port.toInt(), ham.account, ham.password, ham.mountPoint, {
                    AutelLog.i(TAG, "historicalAccountLogin success")
                    rtkLoginSuccess(ham)
                }, {
                    AutelLog.e(TAG, "historicalAccountLogin failed")
                    longinFailed(it)
                })
            }
            else -> {
                AutelLog.e(TAG, "historicalAccountLogin signal is not network or MOBILE_NETWORK_SERVICES")
            }
        }
    }

    private fun rtkLoginSuccess(ham: HistoricalAccountModel) {
        activity?.runOnUiThread {
            setUsingAccount(ham)
        }
        saveOneAccount(ham)
        refreshUnusedAccount()
    }

    private fun setUsingAccount(ham: HistoricalAccountModel) {
        binding.lyUsingAccount.clHistoricalAccount.visibility = View.VISIBLE
        binding.lyUsingAccount.tvAccount.text = ham.account
        binding.lyUsingAccount.tvServerAddr.text = ham.serverAddr
        binding.lyUsingAccount.tvPort.text = ham.port
        binding.lyUsingAccount.tvMountPoint.text = ham.mountPoint
    }

    /**
     * 刷新未使用账号
     */
    private fun refreshUnusedAccount() {
        queryAccount()
    }

    private fun longinFailed(code: IAutelCode) {
        AutelLog.i(TAG, "AutelCode (${code.code})")
    }
}