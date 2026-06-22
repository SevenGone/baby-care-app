package com.sevengone.babycare.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sevengone.babycare.BabyCareApplication
import com.sevengone.babycare.data.BabyProfile
import com.sevengone.babycare.data.DailyOverview
import com.sevengone.babycare.data.MeasurementMethod
import com.sevengone.babycare.data.MedicineRecord
import com.sevengone.babycare.data.ReminderSettings
import com.sevengone.babycare.data.TemperatureRecord
import com.sevengone.babycare.data.TimelineEvent
import com.sevengone.babycare.reminder.ReminderType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class BabyCareViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as BabyCareApplication
    private val repository = app.repository
    private val reminderScheduler = app.reminderScheduler

    var babyProfile by mutableStateOf(
        BabyProfile(
            nickname = "宝宝",
            ageLabel = "1 岁左右",
            birthday = LocalDate.now()
        )
    )
        private set

    var reminderSettings by mutableStateOf(
        ReminderSettings(
            medicineReminderEnabled = true,
            recheckReminderEnabled = true,
            defaultRecheckAfterMinutes = 120,
            quietHours = "22:00 - 06:30"
        )
    )
        private set

    val temperatureRecords = mutableStateListOf<TemperatureRecord>()
    val medicineRecords = mutableStateListOf<MedicineRecord>()

    init {
        viewModelScope.launch {
            repository.seedIfNeeded()
        }
        viewModelScope.launch {
            repository.babyProfile.collectLatest { profile ->
                babyProfile = profile
            }
        }
        viewModelScope.launch {
            repository.reminderSettings.collectLatest { settings ->
                reminderSettings = settings
            }
        }
        viewModelScope.launch {
            repository.temperatureRecords.collectLatest { records ->
                temperatureRecords.clear()
                temperatureRecords.addAll(records)
            }
        }
        viewModelScope.launch {
            repository.medicineRecords.collectLatest { records ->
                medicineRecords.clear()
                medicineRecords.addAll(records)
            }
        }
    }

    fun addTemperatureRecord(
        value: Float,
        method: MeasurementMethod,
        note: String,
        mood: String
    ) {
        viewModelScope.launch {
            repository.insertTemperature(
                TemperatureRecord(
                    id = UUID.randomUUID().toString(),
                    temperatureCelsius = value,
                    measuredAt = LocalDateTime.now(),
                    method = method,
                    note = note.ifBlank { "未填写备注" },
                    mood = mood.ifBlank { "未填写状态" }
                )
            )
        }
    }

    fun updateTemperatureRecord(
        id: String,
        value: Float,
        method: MeasurementMethod,
        note: String,
        mood: String,
        measuredAt: LocalDateTime
    ) {
        viewModelScope.launch {
            repository.updateTemperature(
                TemperatureRecord(
                    id = id,
                    temperatureCelsius = value,
                    measuredAt = measuredAt,
                    method = method,
                    note = note.ifBlank { "未填写备注" },
                    mood = mood.ifBlank { "未填写状态" }
                )
            )
        }
    }

    fun addMedicineRecord(
        medicineName: String,
        dosage: String,
        reason: String,
        note: String
    ) {
        viewModelScope.launch {
            val takenAt = LocalDateTime.now()
            val record = MedicineRecord(
                id = UUID.randomUUID().toString(),
                medicineName = medicineName,
                dosage = dosage,
                takenAt = takenAt,
                reason = reason.ifBlank { "未填写原因" },
                note = note.ifBlank { "未填写备注" }
            )
            repository.insertMedicine(record)

            if (reminderSettings.recheckReminderEnabled) {
                val recheckTime = takenAt.plusMinutes(reminderSettings.defaultRecheckAfterMinutes.toLong())
                reminderScheduler.scheduleReminder(
                    reminderType = ReminderType.Recheck,
                    title = "宝宝复测提醒",
                    message = "请复测体温，查看 ${medicineName} 用药后的变化。",
                    triggerAt = recheckTime,
                    uniqueKey = record.id
                )
            }

            if (reminderSettings.medicineReminderEnabled) {
                val medicineTime = takenAt.plusHours(6)
                reminderScheduler.scheduleReminder(
                    reminderType = ReminderType.Medicine,
                    title = "宝宝给药观察提醒",
                    message = "请确认是否需要继续观察或记录下一次给药信息。",
                    triggerAt = medicineTime,
                    uniqueKey = "${record.id}_medicine"
                )
            }
        }
    }

    fun updateMedicineRecord(
        id: String,
        medicineName: String,
        dosage: String,
        reason: String,
        note: String,
        takenAt: LocalDateTime
    ) {
        viewModelScope.launch {
            reminderScheduler.cancelReminder(ReminderType.Recheck, id)
            reminderScheduler.cancelReminder(ReminderType.Medicine, "${id}_medicine")

            val record = MedicineRecord(
                id = id,
                medicineName = medicineName,
                dosage = dosage,
                takenAt = takenAt,
                reason = reason.ifBlank { "未填写原因" },
                note = note.ifBlank { "未填写备注" }
            )
            repository.updateMedicine(record)

            if (reminderSettings.recheckReminderEnabled) {
                reminderScheduler.scheduleReminder(
                    reminderType = ReminderType.Recheck,
                    title = "宝宝复测提醒",
                    message = "请复测体温，查看 ${medicineName} 用药后的变化。",
                    triggerAt = takenAt.plusMinutes(reminderSettings.defaultRecheckAfterMinutes.toLong()),
                    uniqueKey = record.id
                )
            }

            if (reminderSettings.medicineReminderEnabled) {
                reminderScheduler.scheduleReminder(
                    reminderType = ReminderType.Medicine,
                    title = "宝宝给药观察提醒",
                    message = "请确认是否需要继续观察或记录下一次给药信息。",
                    triggerAt = takenAt.plusHours(6),
                    uniqueKey = "${record.id}_medicine"
                )
            }
        }
    }

    fun deleteTemperatureRecord(id: String) {
        viewModelScope.launch {
            repository.deleteTemperature(id)
        }
    }

    fun deleteMedicineRecord(id: String) {
        viewModelScope.launch {
            reminderScheduler.cancelReminder(ReminderType.Recheck, id)
            reminderScheduler.cancelReminder(ReminderType.Medicine, "${id}_medicine")
            repository.deleteMedicine(id)
        }
    }

    fun updateReminderSettings(
        medicineEnabled: Boolean = reminderSettings.medicineReminderEnabled,
        recheckEnabled: Boolean = reminderSettings.recheckReminderEnabled
    ) {
        viewModelScope.launch {
            repository.updateReminderSettings(
                reminderSettings.copy(
                    medicineReminderEnabled = medicineEnabled,
                    recheckReminderEnabled = recheckEnabled
                )
            )
        }
    }

    fun overviewFor(date: LocalDate = LocalDate.now()): DailyOverview {
        val dayTemps = temperatureRecords.filter { it.measuredAt.toLocalDate() == date }
        val dayMeds = medicineRecords.filter { it.takenAt.toLocalDate() == date }

        return DailyOverview(
            latestTemperature = dayTemps.maxByOrNull { it.measuredAt },
            latestMedicine = dayMeds.maxByOrNull { it.takenAt },
            highestTemperatureToday = dayTemps.maxOfOrNull { it.temperatureCelsius },
            temperatureCountToday = dayTemps.size,
            medicineCountToday = dayMeds.size
        )
    }

    fun timelineFor(date: LocalDate = LocalDate.now()): List<TimelineEvent> {
        val timeline = buildList {
            addAll(temperatureRecords.filter { it.measuredAt.toLocalDate() == date }.map(TimelineEvent::Temperature))
            addAll(medicineRecords.filter { it.takenAt.toLocalDate() == date }.map(TimelineEvent::Medicine))
        }

        return timeline.sortedByDescending { it.happenedAt }
    }
}
