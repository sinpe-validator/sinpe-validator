package com.example.sinpe_validation_app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.sinpe_validation_app.data.local.dao.SmsDao
import com.example.sinpe_validation_app.data.local.entities.SmsEntity

@Database(
    entities = [SmsEntity::class],
    version = 2,
    exportSchema = false
)
abstract class SinpeDatabase : RoomDatabase() {

    abstract fun smsDao(): SmsDao

    companion object {
        @Volatile
        private var INSTANCE: SinpeDatabase? = null

        fun getDatabase(context: Context): SinpeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SinpeDatabase::class.java,
                    "sinpe_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}