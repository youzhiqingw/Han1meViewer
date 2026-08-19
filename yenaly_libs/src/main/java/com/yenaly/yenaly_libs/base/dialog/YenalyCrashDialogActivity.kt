package com.wuwei.yenaly_libs.base.dialog

import android.os.Bundle
import androidx.core.text.parseAsHtml
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.wuwei.yenaly_libs.ActivityManager
import com.wuwei.yenaly_libs.R
import com.wuwei.yenaly_libs.base.frame.FrameActivity
import com.wuwei.yenaly_libs.utils.copyToClipboard
import com.wuwei.yenaly_libs.utils.intentExtra
import com.wuwei.yenaly_libs.utils.sp

/**
 * @ProjectName : YenalyModule
 * @Author : Yenaly Liew
 * @Time : 2022/04/21 021 22:23
 * @Description : Description...
 */
class YenalyCrashDialogActivity : FrameActivity() {

    private val yenalyThrowable by intentExtra("yenaly_throwable", "null")

    override fun setUiStyle() {
        setTheme(R.style.YenalyDialog)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.yenaly_activity_crash_dialog)
        val info =
            """<span style="color: #FF0000; font-size: ${18.sp}px;">These errors occurred:</span><br><br>$yenalyThrowable""".parseAsHtml()
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.yenaly_error_title)
            .setMessage(info)
            .setCancelable(false)
            .setPositiveButton(R.string.yenaly_restart_app) { _, _ ->
                ActivityManager.restart(killProcess = true)
            }
            .setNegativeButton(R.string.yenaly_exit_app) { _, _ ->
                ActivityManager.exit(killProcess = true)
            }
            .setNeutralButton(R.string.yenaly_copy) { _, _ ->
                yenalyThrowable.copyToClipboard()
            }
            .show()
    }
}