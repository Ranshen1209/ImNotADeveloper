package io.github.auag0.imnotadeveloper.common

import android.util.Log
import io.github.auag0.imnotadeveloper.BuildConfig

object Logger {
    private const val TAG = "ImNotADeveloper"

    fun logD(msg: Any?) {
        if (!BuildConfig.DEBUG) return
        Log.d(TAG, msg.toString())
    }

    fun logE(msg: Any?) {
        Log.e(TAG, msg.toString())
    }
}