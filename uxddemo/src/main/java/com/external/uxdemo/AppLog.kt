package com.external.uxdemo

import com.autel.drone.sdk.vmodelx.interfaces.IAutelLog
import com.autel.log.AutelLog


class AppLog : IAutelLog {

    override fun d(tag: String, msg: String, throwable: Throwable?) {
        AutelLog.d(tag, msg, throwable)
    }

    override fun i(tag: String, msg: String, throwable: Throwable?) {
        AutelLog.i(tag, msg, throwable)
    }

    override fun w(tag: String, msg: String, throwable: Throwable?) {
        AutelLog.w(tag, msg, throwable)
    }

    override fun e(tag: String, msg: String, throwable: Throwable?) {
        AutelLog.e(tag, msg, throwable)
    }
}