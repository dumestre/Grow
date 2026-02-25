package com.daime.grow.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS plants (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                strain TEXT NOT NULL,
                stage TEXT NOT NULL,
                medium TEXT NOT NULL,
                days INTEGER NOT NULL,
                photoUri TEXT,
                nextWateringDate INTEGER,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS plant_events (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                plantId INTEGER NOT NULL,
                type TEXT NOT NULL,
                note TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                FOREIGN KEY(plantId) REFERENCES plants(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_plant_events_plantId ON plant_events(plantId)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS watering_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                plantId INTEGER NOT NULL,
                volumeMl INTEGER NOT NULL,
                intervalDays INTEGER NOT NULL,
                substrate TEXT NOT NULL,
                nextWateringDate INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                FOREIGN KEY(plantId) REFERENCES plants(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_watering_logs_plantId ON watering_logs(plantId)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS nutrient_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                plantId INTEGER NOT NULL,
                week INTEGER NOT NULL,
                ec REAL NOT NULL,
                ph REAL NOT NULL,
                createdAt INTEGER NOT NULL,
                FOREIGN KEY(plantId) REFERENCES plants(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_nutrient_logs_plantId ON nutrient_logs(plantId)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS checklist_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                plantId INTEGER NOT NULL,
                phase TEXT NOT NULL,
                task TEXT NOT NULL,
                done INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                FOREIGN KEY(plantId) REFERENCES plants(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_checklist_items_plantId ON checklist_items(plantId)")
    }
}

