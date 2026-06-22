package com.sevengone.babycare.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // 第一版先不做开机后全量恢复提醒，仅保留接收入口，后续可结合提醒表进行重建。
    }
}
