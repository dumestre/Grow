package com.daime.grow.core.di

import android.content.Context
import com.daime.grow.data.local.GrowDatabase
import com.daime.grow.data.preferences.MuralPreferencesRepository
import com.daime.grow.data.preferences.SecurityPreferencesRepository
import com.daime.grow.data.repository.GrowRepositoryImpl
import com.daime.grow.data.reminder.ReminderScheduler
import com.daime.grow.data.backup.BackupManager
import com.daime.grow.domain.repository.GrowRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GrowDatabase {
        return GrowDatabase.getInstance(context)
    }

    @Provides
    fun providePlantDao(database: GrowDatabase) = database.plantDao()

    @Provides
    fun providePlantEventDao(database: GrowDatabase) = database.plantEventDao()

    @Provides
    fun provideWateringLogDao(database: GrowDatabase) = database.wateringLogDao()

    @Provides
    fun provideNutrientLogDao(database: GrowDatabase) = database.nutrientLogDao()

    @Provides
    fun provideChecklistDao(database: GrowDatabase) = database.checklistDao()

    @Provides
    fun provideMuralDao(database: GrowDatabase) = database.muralDao()

    @Provides
    fun provideHarvestDao(database: GrowDatabase) = database.harvestDao()

    @Provides
    @Singleton
    fun provideSecurityPreferencesRepository(@ApplicationContext context: Context): SecurityPreferencesRepository {
        return SecurityPreferencesRepository(context)
    }

    @Provides
    @Singleton
    fun provideMuralPreferencesRepository(@ApplicationContext context: Context): MuralPreferencesRepository {
        return MuralPreferencesRepository(context)
    }

    @Provides
    @Singleton
    fun provideReminderScheduler(@ApplicationContext context: Context): ReminderScheduler {
        return ReminderScheduler(context)
    }

    @Provides
    @Singleton
    fun provideBackupManager(@ApplicationContext context: Context, database: GrowDatabase): BackupManager {
        return BackupManager(context, database)
    }

    @Provides
    @Singleton
    fun provideGrowRepository(
        @ApplicationContext context: Context,
        database: GrowDatabase,
        scheduler: ReminderScheduler,
        backupManager: BackupManager,
        securityRepository: SecurityPreferencesRepository
    ): GrowRepository {
        return GrowRepositoryImpl(context, database, scheduler, backupManager, securityRepository)
    }
}
