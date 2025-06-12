package com.autel.widget.widget.statusbar.manager

import android.os.SystemClock
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.autel.common.constant.AppTagConst.WarningTag
import com.autel.common.sdk.isFly
import com.autel.common.sdk.isLanding
import com.autel.common.utils.AutelPlaySoundUtil
import com.autel.common.utils.DeviceUtils
import com.autel.common.utils.GoogleTextToSpeechManager
import com.autel.common.utils.TransformUtils
import com.autel.drone.sdk.vmodelx.SDKManager
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.interfaces.IBaseDevice
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.adsb.AirSenseWarningLevel
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.common.bean.WarningAtom
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.common.enums.WaringIdEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.enums.DroneWorkModeEnum
import com.autel.log.AutelLog
import com.autel.widget.R
import com.autel.widget.widget.statusbar.warn.CheckEntry
import com.autel.widget.widget.statusbar.warn.WarningBean
import com.autel.widget.widget.statusbar.wm.DeviceWarnAtomList

/**
 * Created by  2022/12/3
 */
object NewWarnModelManager {

    const val TAG = "NewWarnModelManager"

    /** 是否不需要检查手机音量 **/
    var mNotCheckVolume = false

    //飞机的检查内容
    private val mDroneChecker = DroneChecker()

    //谨慎/禁止/异常列表
    private val mWarnsMap = mutableMapOf<Int, ArrayList<WarningBean>>()

    private val mToastInfoWarnsMap = mutableMapOf<Int, ArrayList<WarningBean>>()

    private val mDialogInfoWarnsMap = mutableMapOf<Int, ArrayList<WarningBean>>()

    private val appendId: Int = 0

    val checkEntryLD: MutableLiveData<List<CheckEntry>> = MutableLiveData()

    //记录告警ID为返航的时机，此告警可能来源于遥控器，造成飞机告警随后来低电量告警的时候，在返航状态弹窗低电量返航的情况。所以记录一下时机，小于1秒间隔，不显示这个弹窗
    private var returnTimeInterval: Long = 0L

    fun observerWarnModel(lifecycleOwner: LifecycleOwner, observer: Observer<List<CheckEntry>>) {
        AutelLog.i(WarningTag, "observerWarnModel")
        checkEntryLD.observe(lifecycleOwner, observer)
    }

    fun observeForeverWarnModel(observer: Observer<List<CheckEntry>>) {
        AutelLog.i(WarningTag, "observeForeverWarnModel")
        checkEntryLD.observeForever(observer)
    }

    fun removeObserveForever(observer: Observer<List<CheckEntry>>) {
        AutelLog.i(WarningTag, "removeObserveForever")
        checkEntryLD.removeObserver(observer)
    }


    private fun buildCheckEntry(
        drone: IBaseDevice,
        connectInfo: WarningBean?,
        dialogs: ArrayList<WarningBean>,
        toasts: ArrayList<WarningBean>,
        warns: ArrayList<WarningBean>,
        permanent: ArrayList<WarningBean>,
    ): CheckEntry {
        val deviceId = drone.getDeviceNumber()
        val deviceName = DeviceUtils.droneDeviceName(drone)
        return CheckEntry(
            connectInfo,
            dialogs,
            toasts,
            warns,
            permanent,
            appendId,
            deviceName,
            deviceId
        )
    }

    private fun getWarns(drone: IBaseDevice): ArrayList<WarningBean> {
        val deviceId = drone.getDeviceNumber()
        var warns = mWarnsMap[deviceId]
        if (warns == null) warns = ArrayList()
        return warns
    }

    private fun getToastWarns(drone: IBaseDevice): ArrayList<WarningBean> {
        val deviceId = drone.getDeviceNumber()
        var warns = mToastInfoWarnsMap[deviceId]
        if (warns == null) warns = ArrayList()
        return warns
    }

    private fun getDialogInfoWarns(drone: IBaseDevice): ArrayList<WarningBean> {
        val deviceId = drone.getDeviceNumber()
        var warns = mDialogInfoWarnsMap[deviceId]
        if (warns == null) warns = ArrayList()
        return warns
    }

    fun clearCache() {
        AutelLog.i(WarningTag, "clearCache")
        mWarnsMap.clear()
        mDialogInfoWarnsMap.clear()
        mToastInfoWarnsMap.clear()
    }

    fun checkDroneWarningAtomList(atomLists: List<DeviceWarnAtomList>) {
        val entrys = mutableListOf<CheckEntry>()
        atomLists.forEach { warnAtomList ->
            val drone = warnAtomList.drone
            if (drone != null) {
                val warningAtomList = warnAtomList.warningAtomList
                if (warningAtomList != null) {
                    val entry = checkAtomLists(drone, warningAtomList)
                    entrys.add(entry)
                }
            } else {
                val warn = warnAtomList.warningAtomList?.find { it.warningId == WaringIdEnum.AIRCRAFT_DISCONNECT }
                if (warn != null) {
                    buildDisConnectCheckEntry()?.let { it1 -> entrys.add(it1) }
                }
            }
        }
        checkEntryLD.value = entrys
        playWarnVoice(entrys)
    }

    private fun buildDisConnectCheckEntry(): CheckEntry? {
        val warningBean = mDroneChecker.generate(WaringIdEnum.AIRCRAFT_DISCONNECT, false)
        warningBean?.deviceName = ""
        warningBean?.deviceId = 0
        return if (warningBean != null) {
            CheckEntry(null, arrayListOf(), arrayListOf(), arrayListOf(warningBean), arrayListOf(), appendId, "", 0)
        } else {
            null
        }
    }

    private fun checkAtomLists(drone: IBaseDevice, warningAtomList: List<WarningAtom>): CheckEntry {
        AutelLog.i(WarningTag, "checkAtomLists -------------start")
        val deviceId = DeviceUtils.droneDeviceId(drone)
        val deviceName = DeviceUtils.droneDeviceName(drone)
        var isFlying = false
        if (drone is IAutelDroneDevice) {
            val flyMode = drone.getDeviceStateData().flightControlData.flightMode
            val flyConnected = drone.isConnected()
            isFlying = flyConnected && flyMode.isFly()
            AutelLog.i(WarningTag, "处理飞机告警 $deviceId deviceName:$deviceName")
        } else {
            AutelLog.i(WarningTag, "处理遥控器告警 $deviceId deviceName:$deviceName")
        }


        mNotCheckVolume = mNotCheckVolume || isFlying

        /**
         * 获取本地缓存的toast告警、dialog告警
         * */
        val mTempWarns = ArrayList<WarningBean>(2)
        val mTempToastInfoWarns = ArrayList<WarningBean>(2)
        val mTempDialogInfoWarns = ArrayList<WarningBean>(2)
        val toastInfoWarns = getToastWarns(drone)
        val dialogInfoWarns = getDialogInfoWarns(drone)
        val mWarns = getWarns(drone)
        if (warningAtomList.isEmpty()) {
            if (mWarns.isNotEmpty()) {
                mWarns.clear()
                AutelLog.i(WarningTag, "mWarns clear")
                mWarnsMap.remove(deviceId)
            }
            if (toastInfoWarns.isNotEmpty()) {
                toastInfoWarns.clear()
                AutelLog.i(WarningTag, "toastInfoWarns clear")
                mToastInfoWarnsMap.remove(deviceId)
            }
            if (dialogInfoWarns.isNotEmpty()) {
                dialogInfoWarns.clear()
                AutelLog.i(WarningTag, "dialogInfoWarns clear")
                mDialogInfoWarnsMap.remove(deviceId)
            }
            return buildCheckEntry(drone, null, arrayListOf(), arrayListOf(), arrayListOf(), arrayListOf())
        }

        val permanents = arrayListOf<WarningBean>()

        mTempWarns.addAll(mWarns)
        mTempToastInfoWarns.addAll(toastInfoWarns)
        mTempDialogInfoWarns.addAll(dialogInfoWarns)
        AutelLog.i(
            WarningTag,
            "缓存数据：mTempWarns:${mTempWarns.size} mTempDialogInfoWarns:${mTempDialogInfoWarns.size} mTempToastInfoWarns:${mTempToastInfoWarns.size}"
        )
        mWarns.clear()
        toastInfoWarns.clear()
        dialogInfoWarns.clear()

        warningAtomList.forEachIndexed { _, it ->
            AutelLog.i(WarningTag, "处理收到的告警 warningAtomList:${it.warningId}")
            if (isAdsbWarn(it)) {
                val adsbWarnList = updateAdsbWarn(drone, it, isFlying, mTempWarns)
                val isNew = isNewWarn(it.warningId, mTempWarns, mTempToastInfoWarns, mTempDialogInfoWarns)
                adsbWarnList.forEach { adsbWarn ->
                    adsbWarn.voiceNew = isNew
                    adsbWarn.markedNew = isNew
                    adsbWarn.deviceId = deviceId
                    adsbWarn.deviceName = deviceName
                    mTempWarns.add(adsbWarn)
                }
                AutelLog.i(WarningTag, "ADS-B告警列表：${adsbWarnList.size}")
                mWarns.addAll(adsbWarnList)
            } else {
                val newWarningBean = mDroneChecker.generate(it.warningId, isFlying, drone)
                newWarningBean?.deviceName = deviceName
                newWarningBean?.deviceId = deviceId
                if (newWarningBean?.warnId == WaringIdEnum.UNKNOWN) {
                    AutelLog.i(WarningTag, "告警ID未支持，不处理：${it.warningId}")
                }
                if (isReturnHomeWarningId(it.warningId)) {//这个告警可能来源于遥控器，但是飞机的告警随后才处理，记录一下这个告警的时间，用于后续低电量弹窗处理
                    returnTimeInterval = SystemClock.elapsedRealtime()
                }

                val isNew = isNewWarn(newWarningBean!!.warnId, mTempWarns, mTempToastInfoWarns, mTempDialogInfoWarns)
                //当告警不为空且告警ID不是UNKNOWN时，将告警添加到列表中
                if (newWarningBean != null && newWarningBean.warnId != WaringIdEnum.UNKNOWN) {
                    newWarningBean.markedNew = isNew
                    newWarningBean.voiceNew = isNew
                    AutelLog.i(WarningTag, "告警:${newWarningBean.warnId} markedNew:${newWarningBean.markedNew} voiceNew:${newWarningBean.voiceNew}")
                    when (newWarningBean.tip) {
                        is WarningBean.TipType.TipToast -> {
                            toastInfoWarns.add(newWarningBean)
                        }

                        is WarningBean.TipType.TipDialog -> {//判断缓存弹窗告警是否存在，更新markedNew
                            dialogInfoWarns.add(newWarningBean)
                        }

                        is WarningBean.TipType.PermanentToastTip -> {
                            permanents.add(newWarningBean)
                        }

                        else -> {
                            mWarns.add(newWarningBean)
                        }
                    }
                }
            }
        }
        if (drone is IAutelDroneDevice) {
            this.mDialogInfoWarnsMap[deviceId] = dialogInfoWarns
            this.mToastInfoWarnsMap[deviceId] = toastInfoWarns
            this.mWarnsMap[deviceId] = mWarns
            setLowBatteryModel(drone, warningAtomList)
            mTempWarns.clear()
            mTempDialogInfoWarns.clear()
            mTempToastInfoWarns.clear()
            AutelLog.i(WarningTag, "飞机告警-------------end mWarns:$mWarns dialogInfoWarns:$dialogInfoWarns toastInfoWarns:$toastInfoWarns")
            return buildCheckEntry(drone, null, dialogInfoWarns, toastInfoWarns, mWarns, permanents)
        } else {
            this.mDialogInfoWarnsMap[deviceId] = dialogInfoWarns
            this.mToastInfoWarnsMap[deviceId] = toastInfoWarns
            this.mWarnsMap[deviceId] = mWarns
            mTempWarns.clear()
            mTempDialogInfoWarns.clear()
            mTempToastInfoWarns.clear()
            AutelLog.i(WarningTag, "遥控器告警-------------end mWarns:$mWarns dialogInfoWarns:$dialogInfoWarns toastInfoWarns:$toastInfoWarns")
            return buildCheckEntry(drone, null, dialogInfoWarns, toastInfoWarns, mWarns, permanents)
        }
    }

    private fun isNewWarn(
        warnId: WaringIdEnum,
        mTempWarns: ArrayList<WarningBean>,
        mTempToastInfoWarns: ArrayList<WarningBean>,
        mTempDialogInfoWarns: ArrayList<WarningBean>,
    ): Boolean {
        val isWindowWarnExist = mTempWarns.find { it.warnId == warnId } != null
        val isToastWarnExist = mTempToastInfoWarns.find { it.warnId == warnId } != null
        val isDialogWarnExist = mTempDialogInfoWarns.find { it.warnId == warnId } != null
        return !isWindowWarnExist && !isToastWarnExist && !isDialogWarnExist
    }

    private fun isReturnHomeWarningId(warningId: WaringIdEnum): Boolean {
        return warningId == WaringIdEnum.CANCEL_WAYPOINT_MISSION_AND_RETURN ||
                warningId == WaringIdEnum.REMOTE_LANDING ||
                warningId == WaringIdEnum.FROM_RC_BUTTON ||
                warningId == WaringIdEnum.FROM_NAV_CMD ||
                warningId == WaringIdEnum.CANCEL_WAYPOINT_MISSION_AND_RETURN ||
                warningId == WaringIdEnum.ABNORMAL_VOLTAGE
    }

    private fun setLowBatteryModel(drone: IBaseDevice, warningAtomList: List<WarningAtom>) {
        if (drone !is IAutelDroneDevice) {
            return
        }
        val flyMode = drone.getDeviceStateData().flightControlData.flightMode
        val workMode = drone.getDeviceStateData().flightControlData.droneWorkMode
        val flyConnected = drone.isConnected()
        val isFlying = flyConnected && flyMode.isFly()
        val isLand = flyConnected && (flyMode.isLanding() || workMode == DroneWorkModeEnum.LAND_MANUAL)
        var isReturn = flyConnected && workMode == DroneWorkModeEnum.RETURN
        val dialogInfoWarns = getDialogInfoWarns(drone)
        if (!isReturn) isReturn = warningAtomList.find {
            isReturnHomeWarningId(it.warningId)
        } != null
        if (!isReturn) {//如果前几个判断都不是返航，检查返航告警时间是否在1秒内出现过，出现过则认为目前处于返航中
            if (returnTimeInterval != 0L) {
                isReturn = if (SystemClock.elapsedRealtime() - returnTimeInterval <= 1000) {
                    AutelLog.i(WarningTag, "重置returnTimeInterval")
                    returnTimeInterval = 0
                    true
                } else {
                    false
                }
            }
        }
        dialogInfoWarns.forEach {
            when (it.warnId) {
                WaringIdEnum.CRITICAL_BATTERY -> {
                    if (it.markedNew) {
                        AutelLog.i(
                            WarningTag,
                            "更新markedNew CRITICAL_BATTERY isFlying:$isFlying isLand:$isLand isReturn:$isReturn warningAtomList:$warningAtomList"
                        )
                        it.markedNew = isFlying
                        it.voiceNew = isFlying
                    }
                }

                WaringIdEnum.LOW_BATTERY -> {
                    if (it.markedNew) {
                        AutelLog.i(
                            WarningTag,
                            "更新markedNew LOW_BATTERY isFlying:$isFlying isLand:$isLand isReturn:$isReturn warningAtomList:$warningAtomList"
                        )
                        it.markedNew = !isReturn
                        it.voiceNew = !isReturn
                    }
                }

                WaringIdEnum.INTELLIGENCE_LOW_BATTERY -> {
                    if (it.markedNew) {
                        it.markedNew = isFlying && !isLand && !isReturn
                        it.voiceNew = isFlying && !isLand && !isReturn
                        AutelLog.i(
                            WarningTag,
                            "更新markedNew INTELLIGENCE_LOW_BATTERY isFlying:$isFlying isLand:$isLand isReturn:$isReturn warningAtomList:$warningAtomList"
                        )
                        dialogInfoWarns.forEach { highBean ->
                            //如果异常告警中有严重低电量则不弹框。智能低电和低电量同级
                            if (highBean.warnId == WaringIdEnum.CRITICAL_BATTERY) {
                                AutelLog.i(
                                    WarningTag,
                                    "更新markedNew INTELLIGENCE_LOW_BATTERY dialogInfoWarns isFlying:$isFlying isLand:$isLand isReturn:$isReturn warningAtomList:$warningAtomList"
                                )
                                it.markedNew = false
                                it.voiceNew = false
                            }
                        }
                    }
                }

                WaringIdEnum.REMOTE_ID_INVALID -> {
                    // rid 也需要弹出框
                }

                WaringIdEnum.FLY_LOCK_UOM_UNACTIVATED -> {
                    // UOM未激活也需要弹出框
                }
                WaringIdEnum.MULTI_DRONE_TAKEOFF_TOO_CLOSE->{
                    // 多机起飞也需要弹框
                }

                else -> {
                    AutelLog.i(WarningTag, "更新markedNew  ${it.warnId}")
                    it.markedNew = false
                    it.voiceNew = false
                }
            }
        }
    }

    private fun updateAdsbWarn(
        drone: IBaseDevice,
        warningAtom: WarningAtom,
        isFlying: Boolean,
        cacheWarns: MutableList<WarningBean>,
    ): MutableList<WarningBean> {
        val adsbWarnList = mutableListOf<WarningBean>()
        if (drone is IAutelDroneDevice) {
            if (warningAtom.warningId == WaringIdEnum.ADSB_WARN_MIDDLE ||
                warningAtom.warningId == WaringIdEnum.ADSB_WARN_STRONG
            ) {

                val adsbReportData = drone.getDeviceStateData().adsbReportStateMap
                AutelLog.i(WarningTag, "更新ADS-B告警详情 adsbReportData:$adsbReportData")
                val cacheWarn = cacheWarns.find { it.warnId == warningAtom.warningId }

                adsbReportData.forEach { map ->
                    if (map.value.isTimeValid()) {
                        val newWarningBean = mDroneChecker.generate(warningAtom.warningId, isFlying, drone)
                        val adsbReportData = map.value
                        val adsbWarnLevel = adsbReportData.warningLevel
                        val relativeDistance = adsbReportData.relativeDistance
                        val altitude = adsbReportData.altitude
                        val relativeDistanceString = relativeDistance.let { distance ->
                            if (distance < 1000) {
                                TransformUtils.getDistanceValueWithm(distance.toDouble())
                            } else {
                                TransformUtils.getDistanceValueWithKm(distance.toFloat())
                            }
                        }
                        val altitudeString = altitude.let { altitude ->
                            if (altitude < 1000) {
                                TransformUtils.getDistanceValueWithm(altitude.toDouble())
                            } else {
                                TransformUtils.getDistanceValueWithKm(altitude.toFloat())
                            }
                        }
                        newWarningBean?.detailMsg =
                            SDKManager.get().sContext?.getString(R.string.common_text_adsb_warning_detail, relativeDistanceString, altitudeString)
                        newWarningBean?.markedNew = cacheWarn != null
                        if ((adsbWarnLevel == AirSenseWarningLevel.MIDDLE && newWarningBean?.warnId == WaringIdEnum.ADSB_WARN_MIDDLE) ||
                            (adsbWarnLevel == AirSenseWarningLevel.STRONG && newWarningBean?.warnId == WaringIdEnum.ADSB_WARN_STRONG)
                        ) {
                            adsbWarnList.add(newWarningBean)
                        }
                    }
                }
            }
        }
        return adsbWarnList
    }

    private fun isAdsbWarn(warningAtom: WarningAtom): Boolean {
        return warningAtom.warningId == WaringIdEnum.ADSB_WARN_MIDDLE || warningAtom.warningId == WaringIdEnum.ADSB_WARN_STRONG
    }

    private fun playWarnVoice(checkEntrys: MutableList<CheckEntry>) {
        // 存在多个飞机在线时，语音播报先播报飞机名称，再播报飞机的告警内容
        val voicePlayWarns = filterVoiceWarns(checkEntrys)
        var voicePlayedDroneName = ""
        for (x in voicePlayWarns) {
            if (needPlayDeviceName(checkEntrys)) {
                val deviceName = x.deviceName
                AutelLog.i(WarningTag, "playWarnVoice deviceName:$deviceName")
                if (!deviceName.isNullOrEmpty()) {
                    if (voicePlayedDroneName != deviceName) {
                        voicePlayedDroneName = deviceName
                        GoogleTextToSpeechManager.instance().speak(deviceName, false)
                    }
                }
            }
            voicePlay(x)
        }
    }

    // 是否需要播报设备名称，如果设备名称大于1个，就需要播报
    private fun needPlayDeviceName(checkEntrys: MutableList<CheckEntry>): Boolean {
        var playDeviceName = false
        val deviceWarnCount = checkEntrys.count {
            it.deviceName?.isNotEmpty() == true &&
                    (it.warns.isNotEmpty() || it.dialog.isNotEmpty() || it.toast.isNotEmpty())
        }
        if (deviceWarnCount > 1) {
            playDeviceName = true
        }
        return playDeviceName
    }

    /**
     * 将所有告警的内容整理成字符串数组，用于语音播报
     * */
    private fun filterVoiceWarns(checkEntrys: List<CheckEntry>): List<WarningBean> {
        val warningBeans = mutableListOf<WarningBean>()
        val addVoicePlayWarn = { warn: WarningBean ->
            if (warn.voiceNew && warn.voice) {
                val exist = warningBeans.find { filterWarn ->
                    filterWarn.warnId == warn.warnId && filterWarn.deviceId == warn.deviceId
                }
                if (exist == null) {
                    AutelLog.i(
                        WarningTag,
                        "filterVoiceWarns addVoicePlayWarn:${warn.deviceName} ${warn.warnId} voiceNew:${warn.voiceNew} voice:${warn.voice}"
                    )
                    warningBeans.add(warn)
                }
            }
        }
        checkEntrys.forEach {
            AutelLog.i(WarningTag, "filterVoiceWarns:${it.deviceName} ${it.deviceId}")
            it.warns.forEach { warn ->
                addVoicePlayWarn(warn)
            }
            it.dialog.forEach { warn ->
                addVoicePlayWarn(warn)
            }
            it.toast.forEach { warn ->
                addVoicePlayWarn(warn)
            }
        }
        return warningBeans
    }

    private fun voicePlay(msg: WarningBean) {
        AutelLog.i(WarningTag, "voicePlay:${msg.deviceName} ${msg.warnId} voiceNew:${msg.voiceNew} ${msg.tip.contentRes}")
        if (msg.warnId == WaringIdEnum.CRITICAL_BATTERY) {
            //低电量，播放声音
            AutelPlaySoundUtil.get().play(R.raw.battery_low)
        }
        if (msg.voice && msg.voiceNew) {
            if (msg.tip.contentRes > 0 || msg.tip.contentStr.isNotEmpty()) {
                val content = msg.content(SDKManager.get().sContext)
                AutelLog.i(WarningTag, "voicePlay content:$content")
                //语音播报
                GoogleTextToSpeechManager.instance().speak(content, false)
            }
        }
        msg.voiceNew = false
    }
}
