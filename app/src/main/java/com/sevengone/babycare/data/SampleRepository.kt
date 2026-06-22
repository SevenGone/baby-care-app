package com.sevengone.babycare.data

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month

object SampleRepository {
    private val sampleDay = LocalDate.of(2026, Month.JUNE, 22)

    fun babyProfile(): BabyProfile = BabyProfile(
        nickname = "小宝",
        ageLabel = "1 岁 1 个月",
        birthday = LocalDate.of(2025, Month.MAY, 18)
    )

    fun reminderSettings(): ReminderSettings = ReminderSettings(
        medicineReminderEnabled = true,
        recheckReminderEnabled = true,
        defaultRecheckAfterMinutes = 120,
        quietHours = "22:00 - 06:30"
    )

    fun temperatureRecords(): List<TemperatureRecord> = listOf(
        TemperatureRecord(
            id = "t1",
            temperatureCelsius = 37.3f,
            measuredAt = sampleDay.atTime(6, 0),
            method = MeasurementMethod.Ear,
            note = "刚睡醒，状态平稳",
            mood = "安静"
        ),
        TemperatureRecord(
            id = "t2",
            temperatureCelsius = 37.9f,
            measuredAt = sampleDay.atTime(8, 0),
            method = MeasurementMethod.Ear,
            note = "早餐胃口一般",
            mood = "有点黏人"
        ),
        TemperatureRecord(
            id = "t3",
            temperatureCelsius = 38.4f,
            measuredAt = sampleDay.atTime(10, 0),
            method = MeasurementMethod.Ear,
            note = "上午体温升高",
            mood = "哭闹"
        ),
        TemperatureRecord(
            id = "t4",
            temperatureCelsius = 38.0f,
            measuredAt = sampleDay.atTime(12, 0),
            method = MeasurementMethod.Ear,
            note = "给药后回落",
            mood = "可安抚"
        ),
        TemperatureRecord(
            id = "t5",
            temperatureCelsius = 38.1f,
            measuredAt = sampleDay.atTime(13, 30),
            method = MeasurementMethod.Ear,
            note = "中午后仍需观察",
            mood = "精神一般"
        ),
        TemperatureRecord(
            id = "t6",
            temperatureCelsius = 37.7f,
            measuredAt = sampleDay.atTime(15, 30),
            method = MeasurementMethod.Ear,
            note = "复测后继续回落",
            mood = "开始恢复"
        ),
        TemperatureRecord(
            id = "t7",
            temperatureCelsius = 37.4f,
            measuredAt = sampleDay.atTime(18, 0),
            method = MeasurementMethod.Ear,
            note = "傍晚相对平稳",
            mood = "精神好转"
        )
    )

    fun medicineRecords(): List<MedicineRecord> = listOf(
        MedicineRecord(
            id = "m1",
            medicineName = "布洛芬混悬液",
            dosage = "4 ml",
            takenAt = sampleDay.atTime(10, 0),
            reason = "退热",
            note = "服用后安排两小时复测"
        ),
        MedicineRecord(
            id = "m2",
            medicineName = "口服补液",
            dosage = "少量多次",
            takenAt = sampleDay.atTime(14, 0),
            reason = "补充水分",
            note = "避免脱水"
        )
    )
}
