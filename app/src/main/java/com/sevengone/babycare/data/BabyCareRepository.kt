package com.sevengone.babycare.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BabyCareRepository(
    private val database: BabyCareDatabase
) {
    private val dao = database.dao()

    val babyProfile: Flow<BabyProfile> = dao.observeProfile().map { entity ->
        (entity ?: SampleRepository.babyProfile().toEntity()).toDomain()
    }

    val reminderSettings: Flow<ReminderSettings> = dao.observeReminderSettings().map { entity ->
        (entity ?: SampleRepository.reminderSettings().toEntity()).toDomain()
    }

    val temperatureRecords: Flow<List<TemperatureRecord>> = dao.observeTemperatureRecords().map { list ->
        list.map(TemperatureRecordEntity::toDomain)
    }

    val medicineRecords: Flow<List<MedicineRecord>> = dao.observeMedicineRecords().map { list ->
        list.map(MedicineRecordEntity::toDomain)
    }

    suspend fun seedIfNeeded() {
        if (dao.profileCount() == 0) {
            dao.upsertProfile(SampleRepository.babyProfile().toEntity())
        }
        if (dao.reminderSettingsCount() == 0) {
            dao.upsertReminderSettings(SampleRepository.reminderSettings().toEntity())
        }
        if (dao.temperatureCount() == 0) {
            SampleRepository.temperatureRecords().forEach { dao.insertTemperature(it.toEntity()) }
        }
        if (dao.medicineCount() == 0) {
            SampleRepository.medicineRecords().forEach { dao.insertMedicine(it.toEntity()) }
        }
    }

    suspend fun insertTemperature(record: TemperatureRecord) {
        dao.insertTemperature(record.toEntity())
    }

    suspend fun updateTemperature(record: TemperatureRecord) {
        dao.insertTemperature(record.toEntity())
    }

    suspend fun deleteTemperature(id: String) {
        dao.deleteTemperatureById(id)
    }

    suspend fun insertMedicine(record: MedicineRecord) {
        dao.insertMedicine(record.toEntity())
    }

    suspend fun updateMedicine(record: MedicineRecord) {
        dao.insertMedicine(record.toEntity())
    }

    suspend fun deleteMedicine(id: String) {
        dao.deleteMedicineById(id)
    }

    suspend fun updateReminderSettings(settings: ReminderSettings) {
        dao.upsertReminderSettings(settings.toEntity())
    }
}
