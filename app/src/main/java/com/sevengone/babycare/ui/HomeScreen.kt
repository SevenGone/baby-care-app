package com.sevengone.babycare.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
                Text(
                    text = "${viewModel.babyProfile.nickname}的体温概览",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${targetDate.format(DateTimeFormatter.ofPattern("M 月 d 日"))} · 先看最关键的信息，再直接看趋势。",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val compact = maxWidth < 420.dp
                    if (compact) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatPill(
                                title = "最新体温",
                                value = overview.latestTemperature?.let { "${it.temperatureCelsius}°C" } ?: "--",
                                icon = Icons.Rounded.Thermostat,
                                modifier = Modifier.fillMaxWidth()
                            )
                            StatPill(
                                title = "今日最高",
                                value = overview.highestTemperatureToday?.let { "${it}°C" } ?: "--",
                                icon = Icons.Rounded.TrendingUp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatPill(
                                title = "最新体温",
                                value = overview.latestTemperature?.let { "${it.temperatureCelsius}°C" } ?: "--",
                                icon = Icons.Rounded.Thermostat,
                                modifier = Modifier.weight(1f)
                            )
                            StatPill(
                                title = "今日最高",
                                value = overview.highestTemperatureToday?.let { "${it}°C" } ?: "--",
                                icon = Icons.Rounded.TrendingUp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onExportClick
                ) {
                    Text("导出当前概览图片")
                }
            }
        }

        item {
            GlassCard {
                SectionHeader(
                    title = "趋势图",
                    subtitle = "${displayMethod.label} · 给药点位已在图中标记"
                )
                TemperatureChart(
                    records = records,
                    medicineRecords = medicineRecords,
                    selectedMethod = displayMethod,
                    chartHeight = 260.dp,
                    showMedicineLabels = false
                )
                overview.latestMedicine?.let { medicine ->
                    InfoRow(
                        label = "最近给药",
                        value = "${medicine.medicineName} · ${medicine.takenAt.format(DateTimeFormatter.ofPattern("HH:mm"))}"
                    )
                }
            }
        }
    }
}
