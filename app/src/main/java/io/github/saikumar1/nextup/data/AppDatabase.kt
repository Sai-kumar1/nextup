package io.github.saikumar1.nextup.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.LocalDate

@Database(entities = [Task::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        /** v1 → v2: tasks gain a planned date. Existing tasks are planned for the upgrade day. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val today = LocalDate.now().toEpochDay()
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `dueDate` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE `tasks` SET `dueDate` = $today")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_dueDate` ON `tasks` (`dueDate`)")
            }
        }

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "nextup.db")
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
