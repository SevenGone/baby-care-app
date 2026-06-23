package com.sevengone.babycare.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    viewModel: BabyCareViewModel,
    contentPadding: PaddingValues,
    onExportClick: () -> Unit
) {
    val targetDate = viewModel.focusDate()
    val overview = viewModel.overviewFor(targetDate)
    val trendStartDate = targetDate.minusDays(6)
    val records = viewModel.temperatureRecords
        .filter { it.measuredAt.toLocalDate().isBetween(trendStartDate, targetDate) }
        .sortedBy { it.measuredAt }
    val medicineRecords = viewModel.medicineRecords
        .filter { it.takenAt.toLocalDate().isBetween(trendStartDate, targetDate) }
        .sortedBy { it.takenAt }
    var showNameSheet by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = topSafeContentPadding(),
            bottom = 126.dp + contentPadding.calculateBottomPadding()
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            GlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        SectionHeader(
                            title = viewModel.babyProfile.nickname,
                            subtitle = targetDate.format(DateTimeFormatter.ofPattern("M 月 d 日"))
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassIconButton(
                            icon = Icons.Rounded.Edit,
                            contentDescription = "修改宝宝名称",
                            tint = MaterialTheme.colorScheme.primary,
                            onClick = { showNameSheet = true }
                        )
                        GlassIconButton(
                            icon = Icons.Rounded.Share,
                            contentDescription = "导出图片",
                            tint = MaterialTheme.colorScheme.tertiary,
                            onClick = onExportClick
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
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
                        icon = Icons.AutoMirrored.Rounded.ShowChart,
                        modifier = Modifier.weight(1f)
                    )
                }
                overview.latestMedicine?.let { medicine ->
                    Text(
                        text = "最近给药 ${medicine.medicineName} · ${medicine.takenAt.format(DateTimeFormatter.ofPattern("HH:mm"))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            GlassCard {
                SectionHeader(
                    title = "近 7 天趋势",
                    subtitle = "体温与给药时间轴"
                )
                TemperatureChart(
                    records = records,
                    medicineRecords = medicineRecords,
                    chartHeight = 300.dp,
                    minChartWidth = 1120.dp,
                    showMedicineLabels = true
                )
            }
        }
    }

    if (showNameSheet) {
        BabyNameSheet(
            initialName = viewModel.babyProfile.nickname,
            onDismiss = { showNameSheet = false },
            onSubmit = {
                viewModel.updateBabyName(it)
                showNameSheet = false
            }
        )
    }
}

private fun LocalDate.isBetween(startDate: LocalDate, endDate: LocalDate): Boolean {
    return !isBefore(startDate) && !isAfter(endDate)
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun BabyNameSheet(
    initialName: String,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(initialName) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            paddingValues = PaddingValues(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Text(
                text = "宝宝名称",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("名称") }
            )
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onSubmit(name) }
            ) {
                Text("保存")
            }
        }
    }
}
