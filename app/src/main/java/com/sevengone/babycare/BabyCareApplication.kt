package com.sevengone.babycare

import android.app.Application
import com.sevengone.babycare.data.BabyCareDatabase
import com.sevengone.babycare.data.BabyCareRepository
import com.sevengone.babycare.reminder.NotificationHelper
import com.sevengone.babycare.reminder.ReminderScheduler

class BabyCareApplication : Application() {
    lateinit var repository: BabyCareRepository
        private set

    lateinit var reminderScheduler: ReminderScheduler
        private set

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)

        val database = BabyCareDatabase.getInstance(this)
        repository = BabyCareRepository(database)
        reminderScheduler = ReminderScheduler(this)
    }
}
