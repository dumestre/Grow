package com.daime.grow.data.local

import android.content.Context
import android.util.Log
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
import com.daime.grow.data.local.migration.MIGRATION_8_9
import com.daime.grow.data.local.migration.MIGRATION_9_10

private const val TAG = "GrowDatabase"

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
    version = 10,
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
                    context.applicationContext,
                    GrowDatabase::class.java,
                    "grow.db"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        MIGRATION_9_10
                    )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also {
                        Log.d(TAG, "Banco de dados criado: ${context.getDatabasePath("grow.db").absolutePath}")
                        INSTANCE = it
                    }
            }
        }
    }
}
