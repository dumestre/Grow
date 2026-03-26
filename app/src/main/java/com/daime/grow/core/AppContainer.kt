package com.daime.grow.core

import android.app.Application
import android.content.Context
import com.daime.grow.data.backup.BackupManager
import com.daime.grow.data.local.GrowDatabase
import com.daime.grow.data.preferences.MuralPreferencesRepository
import com.daime.grow.data.preferences.SecurityPreferencesRepository
import com.daime.grow.data.reminder.ReminderScheduler

class AppContainer(context: Context) {
    val application: Application = context.applicationContext as Application
    val appContext: Context = context.applicationContext
    val database: GrowDatabase = GrowDatabase.getInstance(appContext)
    val preferencesRepository = SecurityPreferencesRepository(appContext)
    val muralPreferencesRepository = MuralPreferencesRepository(appContext)
    val reminderScheduler = ReminderScheduler(appContext)
    val backupManager = BackupManager(appContext, database)
    val muralDao = database.muralDao()
}
