package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.FilledFormRecord
import com.example.data.model.FormTemplate
import com.example.data.model.PatientRecord

@Database(
    entities = [
        PatientRecord::class,
        FormTemplate::class,
        FilledFormRecord::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(WardConverters::class)
abstract class WardDatabase : RoomDatabase() {
    abstract fun wardDao(): WardDao

    companion object {
        @Volatile
        private var INSTANCE: WardDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE filled_forms ADD COLUMN patientUnit TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE filled_forms ADD COLUMN patientAddress TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE filled_forms ADD COLUMN patientMobile TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE filled_forms ADD COLUMN templateKey TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getInstance(context: Context): WardDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    WardDatabase::class.java,
                    "wardforms_local.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
