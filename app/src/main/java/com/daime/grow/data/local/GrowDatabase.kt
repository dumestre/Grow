package com.daime.grow.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.daime.grow.data.local.dao.*
import com.daime.grow.data.local.entity.*
import com.daime.grow.data.local.migration.MIGRATION_1_2
import com.daime.grow.data.local.migration.MIGRATION_2_3
import com.daime.grow.data.local.migration.MIGRATION_3_4
import com.daime.grow.data.local.migration.MIGRATION_4_5
import com.daime.grow.data.local.migration.MIGRATION_5_6
import com.daime.grow.data.local.migration.MIGRATION_6_7
import com.daime.grow.data.local.migration.MIGRATION_7_8

@Database(
    entities = [
        PlantEntity::class,
        PlantEventEntity::class,
        WateringLogEntity::class,
        NutrientLogEntity::class,
        ChecklistItemEntity::class,
        MuralPostEntity::class,
        MuralUserEntity::class,
        MuralCommentEntity::class,
        NotificationEntity::class,
        HarvestBatchEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class GrowDatabase : RoomDatabase() {
    abstract fun plantDao(): PlantDao
    abstract fun plantEventDao(): PlantEventDao
    abstract fun wateringLogDao(): WateringLogDao
    abstract fun nutrientLogDao(): NutrientLogDao
    abstract fun checklistDao(): ChecklistDao
    abstract fun muralDao(): MuralDao
    abstract fun notificationDao(): NotificationDao
    abstract fun harvestDao(): HarvestDao

    companion object {
        @Volatile
        private var INSTANCE: GrowDatabase? = null

        fun getInstance(context: Context): GrowDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context,
                    GrowDatabase::class.java,
                    "grow.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
