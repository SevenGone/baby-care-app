package com.sevengone.babycare.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sevengone.babycare.data.TimelineEvent
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    viewModel: BabyCareViewModel,
    contentPadding: PaddingValues,
    onExportClick: () -> Unit
) {
    val overview = viewModel.overviewFor(LocalDate.of(2026, 6, 22))
    val timeline = viewModel.timelineFor(LocalDate.of(2026, 6, 22))

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
                    text = "${viewModel.babyProfile.nickname}的今日观察",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "先看今天的最新体温、最近给药和下一步动作，让夜间记录也能快速看懂。",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatPill(
                        title = "最新体温",
                        value = overview.latestTemperature?.let { "${it.temperatureCelsius}°C" } ?: "--",
                        modifier = Modifier.weight(1f)
                    )
                    StatPill(
                        title = "今日最高",
                        value = overview.highestTemperatureToday?.let { "${it}°C" } ?: "--",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = onExportClick
                    ) {
                        Text("导出今日概览")
                    }
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = {}
                    ) {
                        Text("下次复测 15:30")
                    }
                }
            }
        }

        item {
            GlassCard {
                SectionHeader(
                    title = "快速概览",
                    subtitle = "第一版先聚焦最常用的信息"
                )
                InfoRow("最近一次给药", overview.latestMedicine?.let { "${it.medicineName} ${it.dosage}" } ?: "暂无")
                InfoRow("今天测温次数", overview.temperatureCountToday.toString())
                InfoRow("今天给药次数", overview.medicineCountToday.toString())
                InfoRow(
                    "最近状态",
                    overview.latestTemperature?.mood ?: "暂无状态记录"
                )
            }
        }

        item {
            GlassCard {
                SectionHeader(
                    title = "今日时间线",
                    subtitle = "体温与给药按时间混排，方便回看整段过程"
                )
            }
        }

        items(timeline) { event ->
            when (event) {
                is TimelineEvent.Medicine -> TimelineTimelineCard(
                    title = event.record.medicineName,
                    meta = "${event.record.dosage} · ${event.record.reason}",
                    note = event.record.note,
                    time = event.record.takenAt.format(DateTimeFormatter.ofPattern("HH:mm")),
                    tag = "给药"
                )

                is TimelineEvent.Temperature -> TimelineTimelineCard(
                    title = "${event.record.temperatureCelsius}°C · ${event.record.method.label}",
                    meta = event.record.mood,
                    note = event.record.note,
                    time = event.record.measuredAt.format(DateTimeFormatter.ofPattern("HH:mm")),
                    tag = "体温"
                )
            }
        }
    }
}

@Composable
private fun TimelineTimelineCard(
    title: String,
    meta: String,
    note: String,
    time: String,
    tag: String
) {
    GlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(meta, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column {
                Text(time, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(tag, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
