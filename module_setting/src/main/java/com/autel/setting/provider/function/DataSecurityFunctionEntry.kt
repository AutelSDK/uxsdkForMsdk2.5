package com.autel.setting.provider.function

import android.content.Intent
import android.view.View
import com.autel.common.delegate.IMainProvider
import com.autel.common.delegate.function.AbsDelegateFunction
import com.autel.common.delegate.function.FunctionType
import com.autel.common.delegate.function.FunctionViewType
import com.autel.setting.R
import com.autel.setting.view.DataSecurityActivity

/**
 * Created by  2023/12/14
 * 数据安全
 */
class DataSecurityFunctionEntry(mainProvider: IMainProvider) : AbsDelegateFunction(mainProvider) {
    override fun getFunctionType(): FunctionType {
        return FunctionType.DataSecurity
    }

    override fun getFunctionName(): String {
        return mainProvider.getMainContext().getString(R.string.common_text_data_security)
    }

    override fun getFunctionIconRes(): Int = R.drawable.common_selector_shortcuts_privacy_data

    override fun onFunctionStart(viewType: FunctionViewType, view: View) {
        super.onFunctionStart(viewType, view)
        mainProvider.getMainContext().startActivity(Intent(mainProvider.getMainContext(), DataSecurityActivity::class.java))
    }

    override fun onFunctionStop(viewType: FunctionViewType, view: View) {
        super.onFunctionStop(viewType, view)
        mainProvider.getMainContext().startActivity(Intent(mainProvider.getMainContext(), DataSecurityActivity::class.java))
    }
}