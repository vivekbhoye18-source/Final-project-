package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.model.FilledFormRecord
import com.example.data.model.FormTemplate
import com.example.data.model.PatientRecord

@Database(
    entities = [
        PatientRecord::class,
        FormTemplate::class,
        FilledFormRecord::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(WardConverters::class)
abstract class WardDatabase : RoomDatabase() {
    abstract fun wardDao(): WardDao

    companion object {
        @Volatile
        private var INSTANCE: WardDatabase? = null

        fun getInstance(context: Context): WardDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    WardDatabase::class.java,
                    "wardforms_local.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
