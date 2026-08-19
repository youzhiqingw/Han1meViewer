package com.wuwei.han1meviewer

import com.wuwei.yenaly_libs.ActivityManager

object HCrashHandler : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(t: Thread, e: Throwable) {
        e.printStackTrace()
        ActivityManager.restart(killProcess = true)
    }
}