package com.daime.grow.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.daime.grow.data.local.dao.ChecklistDao
import com.daime.grow.data.local.dao.NutrientLogDao
import com.daime.grow.data.local.dao.PlantDao
import com.daime.grow.data.local.dao.PlantEventDao
import com.daime.grow.data.local.dao.WateringLogDao
import com.daime.grow.data.local.entity.ChecklistItemEntity
import com.daime.grow.data.local.entity.NutrientLogEntity
import com.daime.grow.data.local.entity.PlantEntity
import com.daime.grow.data.local.entity.PlantEventEntity
import com.daime.grow.data.local.entity.WateringLogEntity
import com.daime.grow.data.local.migration.MIGRATION_1_2
import com.daime.grow.data.local.migration.MIGRATION_2_3

@Database(
    entities = [
        PlantEntity::class,
        PlantEventEntity::class,
        WateringLogEntity::class,
        NutrientLogEntity::class,
        ChecklistItemEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class GrowDatabase : RoomDatabase() {
    abstract fun plantDao(): PlantDao
    abstract fun plantEventDao(): PlantEventDao
    abstract fun wateringLogDao(): WateringLogDao
    abstract fun nutrientLogDao(): NutrientLogDao
    abstract fun checklistDao(): ChecklistDao

    companion object {
        @Volatile
        private var INSTANCE: GrowDatabase? = null

        fun getInstance(context: Context): GrowDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context,
                    GrowDatabase::class.java,
                    "grow.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigrationOnDowngrade(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

