package com.daime.grow

import android.app.Application
import com.daime.grow.core.AppContainer
import com.daime.grow.data.reminder.NotificationHelper
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class GrowApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)
        appContainer = AppContainer(this)
    }
}

