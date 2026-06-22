package com.sevengone.babycare.data

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.LocalDateTime

class TypeConverters {
    @TypeConverter
    fun localDateToString(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun stringToLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun localDateTimeToString(value: LocalDateTime?): String? = value?.toString()

    @TypeConverter
    fun stringToLocalDateTime(value: String?): LocalDateTime? = value?.let(LocalDateTime::parse)

    @TypeConverter
    fun methodToString(value: MeasurementMethod?): String? = value?.name

    @TypeConverter
    fun stringToMethod(value: String?): MeasurementMethod? = value?.let(MeasurementMethod::valueOf)
}
