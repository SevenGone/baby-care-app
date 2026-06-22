package com.sevengone.babycare.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        BabyProfileEntity::class,
        TemperatureRecordEntity::class,
        MedicineRecordEntity::class,
        ReminderSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(TypeConverters::class)
abstract class BabyCareDatabase : RoomDatabase() {
    abstract fun dao(): BabyCareDao

    companion object {
        @Volatile
        private var INSTANCE: BabyCareDatabase? = null

        fun getInstance(context: Context): BabyCareDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BabyCareDatabase::class.java,
                    "baby-care.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
