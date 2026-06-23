package com.sevengone.babycare.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sevengone.babycare.data.MeasurementMethod
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    viewModel: BabyCareViewModel,
    contentPadding: PaddingValues,
    onExportClick: () -> Unit
) {
    val targetDate = viewModel.focusDate()
    val overview = viewModel.overviewFor(targetDate)
    val displayMethod = overview.latestTemperature?.method ?: MeasurementMethod.Ear
    val records = viewModel.temperatureRecords
        .filter { it.measuredAt.toLocalDate() == targetDate && it.method == displayMethod }
        .sortedBy { it.measuredAt }
    val medicineRecords = viewModel.medicineRecords
        .filter { it.takenAt.toLocalDate() == targetDate }
        .sortedBy { it.takenAt }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 18.dp + contentPadding.calculateTopPadding(),
            bottom = 112.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            GlassCard {
                SectionHeader(
                    title = viewModel.babyProfile.nickname,
                    subtitle = targetDate.format(DateTimeFormatter.ofPattern("M 月 d 日"))
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    InlineStat(
                        title = "最新",
                        value = overview.latestTemperature?.let { "${it.temperatureCelsius}°C" } ?: "--",
                        icon = Icons.Rounded.Thermostat,
                        modifier = Modifier.weight(1f)
                    )
                    InlineStat(
                        title = "最高",
                        value = overview.highestTemperatureToday?.let { "${it}°C" } ?: "--",
                        icon = Icons.Rounded.TrendingUp,
                        modifier = Modifier.weight(1f)
                    )
                }
                overview.latestMedicine?.let { medicine ->
                    Text(
                        text = "最近给药 ${medicine.medicineName} ${medicine.takenAt.format(DateTimeFormatter.ofPattern("HH:mm"))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onExportClick
                ) {
                    Text("导出")
                }
            }
        }

        item {
            GlassCard {
                SectionHeader(
                    title = "趋势",
                    subtitle = displayMethod.label
                )
                TemperatureChart(
                    records = records,
                    medicineRecords = medicineRecords,
                    selectedMethod = displayMethod,
                    chartHeight = 280.dp,
                    minChartWidth = 880.dp,
                    showMedicineLabels = true,
                    showMethodLegend = false
                )
            }
        }
    }
}
