package com.external.uxdemo

import com.autel.drone.sdk.vmodelx.interfaces.IAutelStorage
import com.autel.storage.AutelDefaultStorageUtil

/**
 * @date 2022/10/31.
 * @author maowei
 * @description app存储实现类
 */
class AppStorage : IAutelStorage {

    override fun setBooleanValue(key: String, value: Boolean) {
        AutelDefaultStorageUtil.getInstance().setBooleanValue(key, value)
    }

    override fun getBooleanValue(key: String, defaultValue: Boolean?): Boolean {
        defaultValue?.let {
            return AutelDefaultStorageUtil.getInstance().getBooleanValue(key, it)
        }
        return AutelDefaultStorageUtil.getInstance().getBooleanValue(key)
    }

    override fun setIntValue(key: String, value: Int) {
        AutelDefaultStorageUtil.getInstance().setIntValue(key, value)
    }

    override fun getIntValue(key: String, defaultValue: Int?): Int {
        defaultValue?.let {
            return AutelDefaultStorageUtil.getInstance().getIntValue(key, it)
        }
        return AutelDefaultStorageUtil.getInstance().getIntValue(key)
    }

    override fun setLongValue(key: String, value: Long) {
        AutelDefaultStorageUtil.getInstance().setLongValue(key, value)
    }

    override fun getLongValue(key: String, defaultValue: Long?): Long {
        defaultValue?.let {
            return AutelDefaultStorageUtil.getInstance().getLongValue(key, it)
        }
        return AutelDefaultStorageUtil.getInstance().getLongValue(key)
    }

    override fun setFloatValue(key: String, value: Float) {
        AutelDefaultStorageUtil.getInstance().setFloatValue(key, value)
    }

    override fun getFloatValue(key: String, defaultValue: Float?): Float {
        defaultValue?.let {
            return AutelDefaultStorageUtil.getInstance().getFloatValue(key, it)
        }
        return AutelDefaultStorageUtil.getInstance().getFloatValue(key)
    }

    override fun setStringValue(key: String, value: String?) {
        AutelDefaultStorageUtil.getInstance().setStringValue(key, value)
    }

    override fun getStringValue(key: String, defaultValue: String?): String? {
        defaultValue?.let {
            return AutelDefaultStorageUtil.getInstance().getStringValue(key, it)
        }
        return AutelDefaultStorageUtil.getInstance().getStringValue(key)
    }

    override fun removeValueForKey(key: String) {
        AutelDefaultStorageUtil.getInstance().removeValueForKey(key)
    }
}