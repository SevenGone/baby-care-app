package com.sevengone.babycare.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "宝宝提醒"
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "有一条新的宝宝记录提醒"
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)

        NotificationHelper.showReminderNotification(
            context = context,
            title = title,
            content = message,
            notificationId = notificationId
        )
    }

    companion object {
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_MESSAGE = "extra_message"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }
}
