package com.sevengone.babycare.data

fun BabyProfileEntity.toDomain(): BabyProfile = BabyProfile(
    nickname = nickname,
    ageLabel = ageLabel,
    birthday = birthday
)

fun BabyProfile.toEntity(): BabyProfileEntity = BabyProfileEntity(
    nickname = nickname,
    ageLabel = ageLabel,
    birthday = birthday
)

fun TemperatureRecordEntity.toDomain(): TemperatureRecord = TemperatureRecord(
    id = id,
    temperatureCelsius = temperatureCelsius,
    measuredAt = measuredAt,
    method = method,
    note = note,
    mood = mood
)

fun TemperatureRecord.toEntity(): TemperatureRecordEntity = TemperatureRecordEntity(
    id = id,
    temperatureCelsius = temperatureCelsius,
    measuredAt = measuredAt,
    method = method,
    note = note,
    mood = mood
)

fun MedicineRecordEntity.toDomain(): MedicineRecord = MedicineRecord(
    id = id,
    medicineName = medicineName,
    dosage = dosage,
    takenAt = takenAt,
    reason = reason,
    note = note
)

fun MedicineRecord.toEntity(): MedicineRecordEntity = MedicineRecordEntity(
    id = id,
    medicineName = medicineName,
    dosage = dosage,
    takenAt = takenAt,
    reason = reason,
    note = note
)

fun ReminderSettingsEntity.toDomain(): ReminderSettings = ReminderSettings(
    medicineReminderEnabled = medicineReminderEnabled,
    recheckReminderEnabled = recheckReminderEnabled,
    defaultRecheckAfterMinutes = defaultRecheckAfterMinutes,
    quietHours = quietHours
)

fun ReminderSettings.toEntity(): ReminderSettingsEntity = ReminderSettingsEntity(
    medicineReminderEnabled = medicineReminderEnabled,
    recheckReminderEnabled = recheckReminderEnabled,
    defaultRecheckAfterMinutes = defaultRecheckAfterMinutes,
    quietHours = quietHours
)
