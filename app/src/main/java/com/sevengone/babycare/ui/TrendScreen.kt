package com.sevengone.babycare.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sevengone.babycare.data.MeasurementMethod
import com.sevengone.babycare.data.MedicineRecord
import com.sevengone.babycare.data.TemperatureRecord
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val trendDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("M 月 d 日")

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun TrendScreen(
    viewModel: BabyCareViewModel,
    contentPadding: PaddingValues
) {
    val availableDates = viewModel.availableDates()
    var selectedDateValue by rememberSaveable { mutableStateOf(availableDates.first().toString()) }
    var selectedMethod by rememberSaveable { mutableStateOf(MeasurementMethod.Ear) }
    val selectedDate = availableDates.firstOrNull { it.toString() == selectedDateValue } ?: availableDates.first()

    val records = viewModel.temperatureRecords
        .filter { it.measuredAt.toLocalDate() == selectedDate && it.method == selectedMethod }
        .sortedBy { it.measuredAt }
    val medicineRecords = viewModel.medicineRecords
        .filter { it.takenAt.toLocalDate() == selectedDate }
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
                SectionHeader(title = "趋势")
                DateFilterRow(
                    dates = availableDates,
                    selectedDate = selectedDate,
                    onDateSelected = { selectedDateValue = it.toString() }
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
                    selectedMethod = selectedMethod,
                    chartHeight = 320.dp,
                    minChartWidth = 980.dp,
                    showMedicineLabels = true,
                    showMethodLegend = true
                )
            }
        }

        item {
            SectionHeader(title = "给药")
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
                if (medicine.reason.isNotBlank()) {
                    InfoRow("原因", medicine.reason)
                }
            }
        }
    }
}

@Composable
private fun DateFilterRow(
    dates: List<LocalDate>,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        dates.forEach { date ->
            FilterChip(
                selected = selectedDate == date,
                onClick = { onDateSelected(date) },
                label = { Text(date.format(trendDateFormatter)) }
            )
        }
    }
}

@Composable
fun TemperatureChart(
    records: List<TemperatureRecord>,
    medicineRecords: List<MedicineRecord>,
    selectedMethod: MeasurementMethod,
    chartHeight: Dp = 300.dp,
    minChartWidth: Dp = 760.dp,
    showMedicineLabels: Boolean = true,
    showMethodLegend: Boolean = true
) {
    if (records.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight)
        ) {
            Text(
                text = "暂无记录",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val chartScrollState = rememberScrollState()
    val dynamicWidth = maxOf(minChartWidth.value, (records.size * 118f), (medicineRecords.size * 110f), 880f).dp
    val lineColor = MaterialTheme.colorScheme.error
    val primaryColor = MaterialTheme.colorScheme.primary
    val rangeColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f)
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val warningColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f)

    if (showMethodLegend) {
        Text(
            text = "可左右拖动查看更多时间点",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(chartScrollState)
    ) {
        Canvas(
            modifier = Modifier
                .width(dynamicWidth)
                .height(chartHeight)
        ) {
            val leftPadding = 68.dp.toPx()
            val rightPadding = 32.dp.toPx()
            val topPadding = 28.dp.toPx()
            val bottomPadding = 42.dp.toPx()
            val chartWidth = size.width - leftPadding - rightPadding
            val plotHeight = size.height - topPadding - bottomPadding

            val minTemp = 36f
            val maxTemp = 39.3f
            val yValues = listOf(36f, 36.5f, 37f, 37.5f, 38f, 38.5f, 39f)
            val warningTemp = 38.5f
            val dayStart = records.first().measuredAt.toLocalDate().atStartOfDay()
            val totalMinutes = 24f * 60f

            fun mapX(time: LocalDateTime): Float {
                val minutes = java.time.Duration.between(dayStart, time).toMinutes().toFloat()
                return leftPadding + (minutes / totalMinutes) * chartWidth
            }

            fun mapY(value: Float): Float {
                val ratio = (value - minTemp) / (maxTemp - minTemp)
                return topPadding + plotHeight - (ratio * plotHeight)
            }

            drawRoundRect(
                color = rangeColor,
                topLeft = Offset(leftPadding, mapY(selectedMethod.normalHigh)),
                size = Size(
                    width = chartWidth,
                    height = mapY(selectedMethod.normalLow) - mapY(selectedMethod.normalHigh)
                ),
                cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx())
            )

            val xAxisMinutes = listOf(0L, 3 * 60L, 6 * 60L, 9 * 60L, 12 * 60L, 15 * 60L, 18 * 60L, 21 * 60L, 24 * 60L)
            xAxisMinutes.forEach { minute ->
                val tickTime = dayStart.plusMinutes(minute.coerceAtMost(23 * 60L + 59))
                val x = if (minute >= 24 * 60L) leftPadding + chartWidth else mapX(tickTime)
                drawLine(
                    color = gridColor,
                    start = Offset(x, topPadding),
                    end = Offset(x, size.height - bottomPadding),
                    strokeWidth = 1.5f
                )
                drawContext.canvas.nativeCanvas.drawText(
                    if (minute >= 24 * 60L) "24:00" else tickTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                    x - 20.dp.toPx(),
                    size.height - 12.dp.toPx(),
                    Paint().apply {
                        color = textColor.toArgbCompat()
                        textSize = 11.dp.toPx()
                        isAntiAlias = true
                    }
                )
            }

            yValues.forEach { temp ->
                val y = mapY(temp)
                drawLine(
                    color = if (temp == warningTemp) warningColor else gridColor,
                    start = Offset(leftPadding, y),
                    end = Offset(size.width - rightPadding, y),
                    strokeWidth = if (temp == warningTemp) 3f else 2f
                )
                drawContext.canvas.nativeCanvas.drawText(
                    if (temp == warningTemp) "${temp}° 提醒" else "${temp}°",
                    leftPadding - 56.dp.toPx(),
                    y + 6.dp.toPx(),
                    Paint().apply {
                        color = if (temp == warningTemp) warningColor.toArgbCompat() else textColor.toArgbCompat()
                        textSize = 11.dp.toPx()
                        isAntiAlias = true
                    }
                )
            }

            val path = Path()
            records.forEachIndexed { index, record ->
                val point = Offset(mapX(record.measuredAt), mapY(record.temperatureCelsius))
                if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
            }

            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
            )

            records.forEach { record ->
                val center = Offset(mapX(record.measuredAt), mapY(record.temperatureCelsius))
                drawCircle(color = lineColor.copy(alpha = 0.15f), radius = 12.dp.toPx(), center = center)
                drawCircle(color = Color.White, radius = 5.dp.toPx(), center = center)
                drawCircle(color = lineColor, radius = 3.5.dp.toPx(), center = center)
            }

            medicineRecords.forEach { medicine ->
                val markerX = mapX(medicine.takenAt)
                drawLine(
                    color = primaryColor.copy(alpha = 0.22f),
                    start = Offset(markerX, topPadding),
                    end = Offset(markerX, size.height - bottomPadding + 8.dp.toPx()),
                    strokeWidth = 2f
                )
                val markerY = topPadding + 18.dp.toPx()
                drawCircle(
                    color = primaryColor.copy(alpha = 0.9f),
                    radius = 5.dp.toPx(),
                    center = Offset(markerX, markerY)
                )
                if (showMedicineLabels) {
                    drawContext.canvas.nativeCanvas.drawText(
                        medicine.medicineName,
                        markerX + 8.dp.toPx(),
                        markerY + 4.dp.toPx(),
                        Paint().apply {
                            color = primaryColor.copy(alpha = 0.58f).toArgbCompat()
                            textSize = 9.dp.toPx()
                            isAntiAlias = true
                        }
                    )
                }
            }
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
