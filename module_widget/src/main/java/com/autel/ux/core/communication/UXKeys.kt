package com.autel.ux.core.communication

import androidx.annotation.CheckResult
import com.autel.log.AutelLog
import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

open class UXKeys {

    private val TAG: String = "UXKeys"
    private val DEFAULT_INDEX: Int = 0
    private val keysPathMap: MutableMap<String?, UXKey?> = ConcurrentHashMap<String?, UXKey?>()
    private val keyValueMap: MutableMap<String?, KClass<*>?> = ConcurrentHashMap<String?, KClass<*>?>()
    private val keyUpdateTypeMap: MutableMap<String?, UpdateType?> = ConcurrentHashMap<String?, UpdateType?>()


    private fun initializeKeyValueTypes(clazz: Class<out UXKeys?>?) {
        if (clazz == null) return
        val fields = clazz.getFields()
        for (field in fields) {
            if (field.type == String::class.java && isStatic(field.modifiers) && (field.isAnnotationPresent(UXParamKey::class.java))) {
                try {
                    val paramKey = field.get(null) as String?
                    val paramKeyAnnotation = field.getAnnotation<UXParamKey?>(UXParamKey::class.java)
                    if (paramKey != null && paramKeyAnnotation != null) {
                        addKeyValueTypeToMap(paramKey, paramKeyAnnotation.type)
                        addKeyUpdateTypeToMap(paramKey, paramKeyAnnotation.updateType)
                    }
                } catch (e: Exception) {
                    AutelLog.e(TAG, "${e.message}")
                }
            }
        }
    }

    /**
     * Use this function to initialize any classes containing UXParamKeys
     *
     * @param componentClass Class which extends the `UXKeys` class and contains UXParamKeys
     */
    fun addNewKeyClass(componentClass: Class<out UXKeys?>) {
        initializeKeyValueTypes(componentClass)
    }

    /**
     * This functions allows creation of a UXKey using a param key (String)
     *
     * @param key String param key with UXParamKey annotation defined in class UXKeys or its children
     * @return UXKey if value-type of key has been registered - null otherwise
     */
    fun create(key: String): UXKey? {
        return create(key, DEFAULT_INDEX)
    }

    /**
     * This functions allows creation of a UXKey using a param key (String) and an index (int)
     *
     * @param key   String param key with UXParamKey annotation defined in class UXKeys or its children
     * @param index Index of the component the key is being created for - default is 0
     * @return UXKey if value-type of key has been registered - null otherwise
     */
    @CheckResult
    fun create(key: String, index: Int): UXKey? {
        val keyPath = producePathFromElements(key, index)
        var uxKey: UXKey? = getCache(keyPath)
        if (uxKey == null) {
            val valueType = keyValueMap[key]
            val updateType = keyUpdateTypeMap[key]
            if (valueType != null && updateType != null) {
                uxKey = UXKey(key, valueType, keyPath, updateType)
                putCache(keyPath, uxKey)
            }
        }
        return uxKey
    }

    /**
     * Use this function to initialize any custom keys created
     *
     * @param key       String key with UXParamKey annotation to be initialized
     * @param valueType Non-primitive class value-type of the key to be initialized (eg. Integer, Boolean etc)
     */
    private fun addKeyValueTypeToMap(key: String, valueType: KClass<*>) {
        keyValueMap.put(key, valueType)
    }

    private fun addKeyUpdateTypeToMap(key: String, updateType: UpdateType) {
        keyUpdateTypeMap.put(key, updateType)
    }

    private fun producePathFromElements(param: String, index: Int): String {
        return "$param/$index"
    }

    private fun getCache(keyStr: String?): UXKey? {
        return if (keyStr != null) {
            keysPathMap[keyStr]
        } else {
            null
        }
    }

    private fun putCache(keyStr: String?, key: UXKey?) {
        if (keyStr != null && key != null) {
            keysPathMap.put(keyStr, key)
        }
    }

    private fun isStatic(modifiers: Int): Boolean {
        return ((modifiers and Modifier.STATIC) != 0)
    }

    enum class UpdateType {
        /**
         * The key will update its listeners only when there is a change in the value
         */
        ON_CHANGE,

        /**
         * The key will update its listeners every time a value is received
         */
        ON_EVENT
    }

    @Retention(AnnotationRetention.RUNTIME)
    @Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
    annotation class UXParamKey(
        /**
         * The type of param that the method associated to this key will take or return
         *
         * @return The class type of the param
         */
        val type: KClass<*>,
        /**
         * The update type of this key.
         *
         * @return UpdateType type for this key.
         */
        val updateType: UpdateType,
    )
}