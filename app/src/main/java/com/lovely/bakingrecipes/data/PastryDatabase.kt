package com.lovely.bakingrecipes.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Pastry::class, Ingredient::class, Step::class, Tag::class, PastryTagCrossRef::class, ShoppingListItem::class, MediaItem::class],
    version = 7,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PastryDatabase : RoomDatabase() {

    abstract fun pastryDao(): PastryDao

    abstract fun shoppingListDao(): ShoppingListDao

    companion object {

        // v1 -> v2: introduced the ingredients table with a cascade FK to pastries.
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `ingredients` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`pastryId` INTEGER NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`amount` REAL NOT NULL, " +
                        "`unit` TEXT NOT NULL, " +
                        "FOREIGN KEY(`pastryId`) REFERENCES `pastries`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_ingredients_pastryId` " +
                        "ON `ingredients` (`pastryId`)"
                )
            }
        }

        // v2 -> v3: added created/updated timestamps to pastries.
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `pastries` ADD COLUMN `createdAt` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `pastries` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
            }
        }

        // v3 -> v4: introduced the steps table with a cascade FK to pastries.
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `steps` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`pastryId` INTEGER NOT NULL, " +
                        "`position` INTEGER NOT NULL, " +
                        "`instruction` TEXT NOT NULL, " +
                        "FOREIGN KEY(`pastryId`) REFERENCES `pastries`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_steps_pastryId` " +
                        "ON `steps` (`pastryId`)"
                )
            }
        }

        // v4 -> v5: added recipe metadata (servings, times, difficulty).
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `pastries` ADD COLUMN `servings` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `pastries` ADD COLUMN `prepMinutes` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `pastries` ADD COLUMN `cookMinutes` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `pastries` ADD COLUMN `difficulty` TEXT NOT NULL DEFAULT ''")
            }
        }

        // v5 -> v6: favorites flag, tags (many-to-many) and shopping list.
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `pastries` ADD COLUMN `isFavorite` INTEGER NOT NULL DEFAULT 0")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `tags` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL )"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_tags_name` ON `tags` (`name`)"
                )

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `pastry_tag` (" +
                        "`pastryId` INTEGER NOT NULL, " +
                        "`tagId` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`pastryId`, `tagId`), " +
                        "FOREIGN KEY(`pastryId`) REFERENCES `pastries`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE, " +
                        "FOREIGN KEY(`tagId`) REFERENCES `tags`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_pastry_tag_pastryId` ON `pastry_tag` (`pastryId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_pastry_tag_tagId` ON `pastry_tag` (`tagId`)"
                )

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `shopping_list_items` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`amount` REAL NOT NULL, " +
                        "`unit` TEXT NOT NULL, " +
                        "`isChecked` INTEGER NOT NULL DEFAULT 0, " +
                        "`sourcePastryId` INTEGER, " +
                        "`createdAt` INTEGER NOT NULL DEFAULT 0 )"
                )
            }
        }

        // v6 -> v7: added a media table for multiple photos and a procedure video.
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `media` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`pastryId` INTEGER NOT NULL, " +
                        "`uri` TEXT NOT NULL, " +
                        "`type` TEXT NOT NULL, " +
                        "`position` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`pastryId`) REFERENCES `pastries`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_media_pastryId` ON `media` (`pastryId`)"
                )
            }
        }

        @Volatile
        private var INSTANCE: PastryDatabase? = null

        fun getDatabase(context: Context): PastryDatabase {

            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PastryDatabase::class.java,
                    "baking_recipes_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .build()

                INSTANCE = instance

                instance
            }
        }
    }
}