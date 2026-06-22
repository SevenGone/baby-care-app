package com.sevengone.babycare.data

import java.time.LocalDate
import java.time.LocalDateTime

enum class MeasurementMethod(val label: String, val normalLow: Float, val normalHigh: Float) {
    Armpit("腋温", 36.0f, 37.3f),
    Ear("耳温", 36.3f, 37.5f),
    Forehead("额温", 36.2f, 37.5f),
    Other("其他", 36.0f, 37.5f)
}

data class BabyProfile(
    val nickname: String,
    val ageLabel: String,
    val birthday: LocalDate
)

data class TemperatureRecord(
    val id: String,
    val temperatureCelsius: Float,
    val measuredAt: LocalDateTime,
    val method: MeasurementMethod,
    val note: String,
    val mood: String
)

data class MedicineRecord(
    val id: String,
    val medicineName: String,
    val dosage: String,
    val takenAt: LocalDateTime,
    val reason: String,
    val note: String
)

data class ReminderSettings(
    val medicineReminderEnabled: Boolean,
    val recheckReminderEnabled: Boolean,
    val defaultRecheckAfterMinutes: Int,
    val quietHours: String
)

data class DailyOverview(
    val latestTemperature: TemperatureRecord?,
    val latestMedicine: MedicineRecord?,
    val highestTemperatureToday: Float?,
    val temperatureCountToday: Int,
    val medicineCountToday: Int
)

sealed interface TimelineEvent {
    val happenedAt: LocalDateTime

    data class Temperature(val record: TemperatureRecord) : TimelineEvent {
        override val happenedAt: LocalDateTime = record.measuredAt
    }

    data class Medicine(val record: MedicineRecord) : TimelineEvent {
        override val happenedAt: LocalDateTime = record.takenAt
    }
}
