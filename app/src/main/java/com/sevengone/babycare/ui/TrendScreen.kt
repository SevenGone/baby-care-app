package com.sevengone.babycare.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sevengone.babycare.data.MeasurementMethod
import com.sevengone.babycare.data.MedicineRecord
import com.sevengone.babycare.data.TemperatureRecord
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun TrendScreen(
    viewModel: BabyCareViewModel,
    contentPadding: PaddingValues
) {
    var selectedMethod by rememberSaveable { mutableStateOf(MeasurementMethod.Ear) }
    val targetDate = LocalDate.of(2026, 6, 22)
    val records = viewModel.temperatureRecords
        .filter { it.measuredAt.toLocalDate() == targetDate && it.method == selectedMethod }
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
                    title = "体温趋势",
                    subtitle = "体温折线 + 正常范围带 + 给药时间点标记"
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MeasurementMethod.entries.forEach { method ->
                        FilterChip(
                            selected = selectedMethod == method,
                            onClick = { selectedMethod = method },
                            label = { Text(method.label) }
                        )
                    }
                }
                TemperatureChart(
                    records = records,
                    medicineRecords = medicineRecords,
                    selectedMethod = selectedMethod
                )
                Text(
                    text = "说明：当前正常范围为原型阶段参考值，正式上线前建议按测量方式和医学来源再次确认。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            GlassCard {
                SectionHeader(
                    title = "给药点位",
                    subtitle = "图上有标记，列表里再补充一次细节"
                )
            }
        }

        items(medicineRecords) { medicine ->
            GlassCard {
                Text(
                    text = medicine.medicineName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                InfoRow("时间", medicine.takenAt.format(DateTimeFormatter.ofPattern("HH:mm")))
                InfoRow("剂量", medicine.dosage)
                InfoRow("原因", medicine.reason)
                Text(
                    text = medicine.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TemperatureChart(
    records: List<TemperatureRecord>,
    medicineRecords: List<MedicineRecord>,
    selectedMethod: MeasurementMethod
) {
    if (records.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            Text(
                text = "当前测量方式下还没有记录。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val lineColor = MaterialTheme.colorScheme.error
    val primaryColor = MaterialTheme.colorScheme.primary
    val rangeColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    ) {
        val leftPadding = 72.dp.toPx()
        val rightPadding = 26.dp.toPx()
        val topPadding = 28.dp.toPx()
        val bottomPadding = 50.dp.toPx()
        val chartWidth = size.width - leftPadding - rightPadding
        val chartHeight = size.height - topPadding - bottomPadding

        val minTemp = 36f
        val maxTemp = 39.2f
        val yValues = listOf(36f, 36.5f, 37f, 37.5f, 38f, 38.5f, 39f)
        val dayStart = records.first().measuredAt.toLocalDate().atStartOfDay()
        val totalMinutes = 24f * 60f

        fun mapX(time: LocalDateTime): Float {
            val minutes = java.time.Duration.between(dayStart, time).toMinutes().toFloat()
            return leftPadding + (minutes / totalMinutes) * chartWidth
        }

        fun mapY(value: Float): Float {
            val ratio = (value - minTemp) / (maxTemp - minTemp)
            return topPadding + chartHeight - (ratio * chartHeight)
        }

        drawRoundRect(
            color = rangeColor,
            topLeft = Offset(
                x = leftPadding,
                y = mapY(selectedMethod.normalHigh)
            ),
            size = Size(
                width = chartWidth,
                height = mapY(selectedMethod.normalLow) - mapY(selectedMethod.normalHigh)
            ),
            cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx())
        )

        yValues.forEach { temp ->
            val y = mapY(temp)
            drawLine(
                color = gridColor,
                start = Offset(leftPadding, y),
                end = Offset(size.width - rightPadding, y),
                strokeWidth = 2f
            )

            drawContext.canvas.nativeCanvas.drawText(
                "${temp}°",
                leftPadding - 50.dp.toPx(),
                y + 8.dp.toPx(),
                Paint().apply {
                    color = textColor.toArgbCompat()
                    textSize = 12.dp.toPx()
                    isAntiAlias = true
                }
            )
        }

        val path = Path()
        records.forEachIndexed { index, record ->
            val point = Offset(mapX(record.measuredAt), mapY(record.temperatureCelsius))
            if (index == 0) {
                path.moveTo(point.x, point.y)
            } else {
                path.lineTo(point.x, point.y)
            }
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
        )

        records.forEach { record ->
            val center = Offset(mapX(record.measuredAt), mapY(record.temperatureCelsius))
            drawCircle(color = lineColor.copy(alpha = 0.15f), radius = 14.dp.toPx(), center = center)
            drawCircle(color = Color.White, radius = 6.dp.toPx(), center = center)
            drawCircle(color = lineColor, radius = 4.dp.toPx(), center = center)
        }

        // 先把给药点位按时间映射到同一条时间轴上，便于观察给药前后体温变化。
        medicineRecords.forEach { medicine ->
            val markerX = mapX(medicine.takenAt)
            drawLine(
                color = primaryColor.copy(alpha = 0.35f),
                start = Offset(markerX, topPadding),
                end = Offset(markerX, size.height - bottomPadding + 10.dp.toPx()),
                strokeWidth = 3f
            )

            val diamondCenter = Offset(markerX, topPadding + 28.dp.toPx())
            val diamondPath = Path().apply {
                moveTo(diamondCenter.x, diamondCenter.y - 10.dp.toPx())
                lineTo(diamondCenter.x + 10.dp.toPx(), diamondCenter.y)
                lineTo(diamondCenter.x, diamondCenter.y + 10.dp.toPx())
                lineTo(diamondCenter.x - 10.dp.toPx(), diamondCenter.y)
                close()
            }
            drawPath(
                path = diamondPath,
                color = primaryColor
            )
            drawContext.canvas.nativeCanvas.drawText(
                medicine.medicineName,
                markerX - 44.dp.toPx(),
                topPadding + 16.dp.toPx(),
                Paint().apply {
                    color = primaryColor.toArgbCompat()
                    textSize = 11.dp.toPx()
                    isAntiAlias = true
                    isFakeBoldText = true
                }
            )
        }

        records.forEach { record ->
            drawContext.canvas.nativeCanvas.drawText(
                record.measuredAt.format(DateTimeFormatter.ofPattern("HH:mm")),
                mapX(record.measuredAt) - 16.dp.toPx(),
                size.height - 12.dp.toPx(),
                Paint().apply {
                    color = textColor.toArgbCompat()
                    textSize = 12.dp.toPx()
                    isAntiAlias = true
                }
            )
        }
    }
}

private fun Color.toArgbCompat(): Int {
    return android.graphics.Color.argb(
        (alpha * 255).roundToInt(),
        (red * 255).roundToInt(),
        (green * 255).roundToInt(),
        (blue * 255).roundToInt()
    )
}
