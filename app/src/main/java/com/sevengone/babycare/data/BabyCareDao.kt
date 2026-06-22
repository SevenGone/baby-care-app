package com.sevengone.babycare.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BabyCareDao {
    @Query("SELECT * FROM baby_profile WHERE id = 0")
    fun observeProfile(): Flow<BabyProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: BabyProfileEntity)

    @Query("SELECT * FROM reminder_settings WHERE id = 0")
    fun observeReminderSettings(): Flow<ReminderSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReminderSettings(settings: ReminderSettingsEntity)

    @Query("SELECT * FROM temperature_records ORDER BY measuredAt DESC")
    fun observeTemperatureRecords(): Flow<List<TemperatureRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemperature(record: TemperatureRecordEntity)

    @Query("DELETE FROM temperature_records WHERE id = :id")
    suspend fun deleteTemperatureById(id: String)

    @Query("SELECT COUNT(*) FROM temperature_records")
    suspend fun temperatureCount(): Int

    @Query("SELECT * FROM medicine_records ORDER BY takenAt DESC")
    fun observeMedicineRecords(): Flow<List<MedicineRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicine(record: MedicineRecordEntity)

    @Query("DELETE FROM medicine_records WHERE id = :id")
    suspend fun deleteMedicineById(id: String)

    @Query("SELECT COUNT(*) FROM medicine_records")
    suspend fun medicineCount(): Int

    @Query("SELECT COUNT(*) FROM baby_profile")
    suspend fun profileCount(): Int

    @Query("SELECT COUNT(*) FROM reminder_settings")
    suspend fun reminderSettingsCount(): Int
}
