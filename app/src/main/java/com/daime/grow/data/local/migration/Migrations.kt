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

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE plants ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE plants SET sortOrder = id")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE plants ADD COLUMN sharedOnMural INTEGER NOT NULL DEFAULT 0")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS mural_posts (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                plantId INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                FOREIGN KEY(plantId) REFERENCES plants(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_mural_posts_plantId ON mural_posts(plantId)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS mural_users (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                username TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS mural_comments (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                postId INTEGER NOT NULL,
                userId INTEGER NOT NULL,
                content TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                FOREIGN KEY(postId) REFERENCES mural_posts(id) ON DELETE CASCADE,
                FOREIGN KEY(userId) REFERENCES mural_users(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_mural_comments_postId ON mural_comments(postId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_mural_comments_userId ON mural_comments(userId)")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS harvest_batches (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                plantId INTEGER NOT NULL,
                plantName TEXT NOT NULL,
                strain TEXT NOT NULL,
                harvestDate INTEGER NOT NULL,
                status TEXT NOT NULL,
                FOREIGN KEY(plantId) REFERENCES plants(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS notifications (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                type TEXT NOT NULL,
                username TEXT NOT NULL,
                message TEXT NOT NULL,
                time INTEGER NOT NULL,
                relatedId INTEGER,
                isRead INTEGER NOT NULL DEFAULT 0,
                userId INTEGER
            )
            """.trimIndent()
        )
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE mural_comments ADD COLUMN parentId INTEGER")
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE plants ADD COLUMN isHydroponic INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE mural_posts ADD COLUMN remoteId TEXT")
        db.execSQL("ALTER TABLE mural_users ADD COLUMN remoteId TEXT")
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS mural_comments_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                remoteId TEXT,
                localPostId INTEGER NOT NULL,
                localUserId INTEGER NOT NULL,
                content TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                parentId TEXT,
                FOREIGN KEY(localPostId) REFERENCES mural_posts(id) ON DELETE CASCADE,
                FOREIGN KEY(localUserId) REFERENCES mural_users(id) ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("""
            INSERT INTO mural_comments_new (id, localPostId, localUserId, content, createdAt, parentId)
            SELECT id, postId, userId, content, createdAt, parentId FROM mural_comments
        """.trimIndent())
        db.execSQL("DROP TABLE mural_comments")
        db.execSQL("ALTER TABLE mural_comments_new RENAME TO mural_comments")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_mural_comments_localPostId ON mural_comments(localPostId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_mural_comments_localUserId ON mural_comments(localUserId)")
    }
}
