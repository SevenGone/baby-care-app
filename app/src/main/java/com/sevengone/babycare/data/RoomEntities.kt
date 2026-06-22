package com.sevengone.babycare.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(tableName = "baby_profile")
data class BabyProfileEntity(
    @PrimaryKey val id: Int = 0,
    val nickname: String,
    val ageLabel: String,
    val birthday: LocalDate
)

@Entity(tableName = "temperature_records")
data class TemperatureRecordEntity(
    @PrimaryKey val id: String,
    val temperatureCelsius: Float,
    val measuredAt: LocalDateTime,
    val method: MeasurementMethod,
    val note: String,
    val mood: String
)

@Entity(tableName = "medicine_records")
data class MedicineRecordEntity(
    @PrimaryKey val id: String,
    val medicineName: String,
    val dosage: String,
    val takenAt: LocalDateTime,
    val reason: String,
    val note: String
)

@Entity(tableName = "reminder_settings")
data class ReminderSettingsEntity(
    @PrimaryKey val id: Int = 0,
    val medicineReminderEnabled: Boolean,
    val recheckReminderEnabled: Boolean,
    val defaultRecheckAfterMinutes: Int,
    val quietHours: String
)
