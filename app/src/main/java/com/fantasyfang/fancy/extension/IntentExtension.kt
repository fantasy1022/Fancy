package com.fantasyfang.fancy.extension

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

fun Intent.asServicePendingIntent(
    context: Context,
    flag: Int = PendingIntent.FLAG_CANCEL_CURRENT
): PendingIntent {
    if (isOreo()) {
        return PendingIntent.getForegroundService(context, 0, this, flag)
    }
    return PendingIntent.getService(context, 0, this, flag)
}

fun isOreo(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
}